package com.iksxh.create_nuclear_industry.radiation;

import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;

/** 每维度只读源索引；跨帧任务显式拥有目标/投影/实体/射线/累积游标，不拥有真实物料。 */
public final class RadiationSourceIndex {
    public static final TagKey<Block> SHIELDING=TagKey.create(Registries.BLOCK,ResourceLocation.parse("create_nuclear_industry:radiation_shielding"));
    public static final int STACK_BUDGET=32768, BLOCK_ENTITY_BUDGET=4096, RAY_CELL_BUDGET=32768;
    private static final Map<ServerLevel,RadiationSourceIndex> LEVELS=new IdentityHashMap<>();
    private static final Map<ServerLevel,Set<RadiationSourceIndex>> ACTIVE=new IdentityHashMap<>();
    private static Set<RadiationSourceIndex> active(ServerLevel level){return ACTIVE.getOrDefault(level,Set.of());}
    /** 正式入口固定默认技术上限；可注入较小上限验证同一生产算法的活性。 */
    public record WorkLimits(int stacks,int blockEntities,int rayCells) {
        public static final WorkLimits DEFAULT=new WorkLimits(STACK_BUDGET,BLOCK_ENTITY_BUDGET,RAY_CELL_BUDGET);
        public WorkLimits {if(stacks<2||blockEntities<1||rayCells<1)throw new IllegalArgumentException("invalid sampling work limit");}
    }
    public record Counters(int stacks,int blockEntities,int rayCells,boolean complete) {}
    private final WorkLimits limits;
    private final RadiationContainment.ChunkVisibility visibility;
    private final Map<Object,Job> jobs=new LinkedHashMap<>();
    private final Map<ItemStack,CachedItem> itemCache=new WeakHashMap<>();
    private long time=Long.MIN_VALUE;
    private int stacks,blockEntities,rayCells;
    private boolean complete=true;
    private static final class BudgetExceeded extends RuntimeException {}
    private static final class CachedItem {
        final Object container,bundle,history;final int count;final RadiationParameters parameters;
        final RadiationItemSources.Cursor cursor;ItemSourceSummary result;
        CachedItem(ItemStack stack,RadiationParameters p) {
            container=stack.get(net.minecraft.core.component.DataComponents.CONTAINER);
            bundle=stack.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS);
            history=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);count=stack.getCount();parameters=p;
            cursor=new RadiationItemSources.Cursor(stack,p);
        }
        boolean matches(ItemStack stack,RadiationParameters p) {return count==stack.getCount()&&parameters.equals(p)
                &&container==stack.get(net.minecraft.core.component.DataComponents.CONTAINER)
                &&bundle==stack.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS)
                &&history==stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);}
    }
    private static final class Source {
        final Group group;final double strength;final BooleanSupplier stillPresent;final ItemStack stack;final CachedItem fingerprint;
        final BlockPos origin;final Entity entity;boolean invalid;
        Source(Group group,double strength,BooleanSupplier stillPresent,ItemStack stack,RadiationParameters p,BlockPos origin,Entity entity) {
            this.group=group;this.strength=strength;this.stillPresent=stillPresent;this.stack=stack;this.origin=origin;this.entity=entity;
            fingerprint=new CachedItem(stack,p);
        }
        boolean valid(RadiationParameters p) {return !invalid&&stillPresent.getAsBoolean()&&fingerprint.matches(stack,p);}
    }
    private static final class Group {
        final Vec3 position;Ray ray;double pathTransmission,containment,strength;boolean queued;
        int strengthGeneration=-1;
        Group(Vec3 position){this.position=position;}
    }
    private static final class Job {
        final ServerLevel level;final Vec3 target;final RadiationParameters p;
        final List<BlockEntity> owners=new ArrayList<>();final List<ServerPlayer> carriers;
        final List<ItemEntity> entities;final List<Source> sources=new ArrayList<>();
        final Map<Vec3,Group> groups=new LinkedHashMap<>();
        final Map<BlockPos,List<Source>> originSources=new HashMap<>();
        final Map<Long,List<Source>> chunkSources=new HashMap<>();
        final Map<Entity,List<Source>> entitySources=new IdentityHashMap<>();
        final Set<Object> transports=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,Set<Group>> pathDependencies=new HashMap<>(),frameDependencies=new HashMap<>();
        final Map<Long,Set<Group>> pathChunkDependencies=new HashMap<>();
        final Deque<Group> rays=new ArrayDeque<>();
        final Map<ServerPlayer,Integer> carrierRevisions=new IdentityHashMap<>();
        List<Group> orderedGroups;int stage,ownerCursor,slotCursor,carrierCursor,carrierSlot,entityCursor,validationCursor,sumCursor,generation;
        double field;long lastRequested;
        Job(ServerLevel level,Vec3 target,RadiationParameters p,long now) {
            this.level=level;this.target=target;this.p=p;lastRequested=now;int radius=p.detectionRadiusBlocks();
            for(int x=((int)Math.floor(target.x)-radius)>>4;x<=((int)Math.floor(target.x)+radius)>>4;x++)
                for(int z=((int)Math.floor(target.z)-radius)>>4;z<=((int)Math.floor(target.z)+radius)>>4;z++) {
                    var chunk=level.getChunkSource().getChunkNow(x,z);if(chunk!=null)owners.addAll(chunk.getBlockEntities().values());
                }
            carriers=level.players().stream().filter(player->player.position().distanceToSqr(target)<=radius*radius).toList();
            entities=List.copyOf(level.getEntitiesOfClass(ItemEntity.class,new AABB(target,target).inflate(radius),Entity::isAlive));
        }
        void add(Vec3 position,ItemStack stack,double strength,BooleanSupplier present,BlockPos origin,Entity entity,Object transport) {
            if(strength<=0||position.distanceToSqr(target)>(double)p.detectionRadiusBlocks()*p.detectionRadiusBlocks())return;
            if(transport!=null&&!transports.add(transport))return;
            Group group=groups.computeIfAbsent(position,Group::new);
            Source source=new Source(group,strength,present,stack,p,origin,entity);sources.add(source);
            if(origin!=null){originSources.computeIfAbsent(origin,k->new ArrayList<>()).add(source);chunkSources.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(origin.getX()>>4,origin.getZ()>>4),k->new ArrayList<>()).add(source);}
            if(entity!=null)entitySources.computeIfAbsent(entity,k->new ArrayList<>()).add(source);
        }
        void invalidateSources(List<Source> candidates) {
            boolean changed=false;
            for(Source source:candidates)if(!source.invalid){source.invalid=true;changed=true;}
            if(changed){generation++;validationCursor=0;sumCursor=0;field=0;if(stage>4)stage=4;}
        }
        void dirty(Group group) {
            group.ray=null;
            if(!group.queued){group.queued=true;rays.add(group);}
            if(stage>3)stage=3;
            sumCursor=0;field=0;
        }
    }
    /** 一条体素DDA的真实跨tick游标；预算可小于单条路径长度。 */
    private static final class Ray {
        final int ex,ey,ez,sx,sy,sz;final double dx,dy,dz;int x,y,z,remaining;
        double tx,ty,tz,transmission=1;
        Ray(Vec3 start,Vec3 end,int radius) {
            x=(int)Math.floor(start.x);y=(int)Math.floor(start.y);z=(int)Math.floor(start.z);
            ex=(int)Math.floor(end.x);ey=(int)Math.floor(end.y);ez=(int)Math.floor(end.z);
            dx=end.x-start.x;dy=end.y-start.y;dz=end.z-start.z;
            sx=Double.compare(dx,0);sy=Double.compare(dy,0);sz=Double.compare(dz,0);
            tx=first(start.x,x,dx,sx);ty=first(start.y,y,dy,sy);tz=first(start.z,z,dz,sz);remaining=3*radius+4;
        }
        boolean step() {
            if(x==ex&&y==ey&&z==ez)return true;
            if(--remaining<0)throw new IllegalStateException("radiation ray exceeded bounded path");
            double next=Math.min(tx,Math.min(ty,tz));
            if(tx==next){x+=sx;tx+=1/Math.abs(dx);}if(ty==next){y+=sy;ty+=1/Math.abs(dy);}if(tz==next){z+=sz;tz+=1/Math.abs(dz);}return false;
        }
    }
    public RadiationSourceIndex(){this(WorkLimits.DEFAULT);}
    public RadiationSourceIndex(WorkLimits limits){this(limits,RadiationContainment.LOADED_CHUNKS);}
    /** 只注入屏蔽可见性；真实源/owner仍须实际加载，不能凭查询边界强载或制造物料。 */
    public RadiationSourceIndex(WorkLimits limits,RadiationContainment.ChunkVisibility visibility){this.limits=Objects.requireNonNull(limits);this.visibility=Objects.requireNonNull(visibility);}
    public static RadiationSourceIndex get(ServerLevel level){return LEVELS.computeIfAbsent(level,k->new RadiationSourceIndex());}
    public static void forget(ServerLevel level){var index=LEVELS.remove(level);if(index!=null)index.jobs.clear();}
    /** 世界真正关闭时撤销所有未完成任务，包括使用注入预算的独立执行实例。 */
    public static void close(ServerLevel level){forget(level);for(var index:active(level))index.jobs.clear();ACTIVE.remove(level);}
    public static void cancel(UUID player){for(var indices:ACTIVE.values())for(var index:indices)index.jobs.remove(player);}
    public Counters counters(){return new Counters(stacks,blockEntities,rayCells,complete);}
    /** 只使变化格拥有的源投影失效；同区块无关库存不会撤销其他源或射线任务。 */
    public static void changed(ServerLevel level,BlockPos pos) {
        for(var index:active(level))index.originChanged(pos);
    }
    private void originChanged(BlockPos pos){for(Job job:jobs.values())job.invalidateSources(job.originSources.getOrDefault(pos,List.of()));}
    public static void entityChanged(Entity entity) {
        if(entity.level() instanceof ServerLevel level){for(var index:active(level))for(Job job:index.jobs.values())job.invalidateSources(job.entitySources.getOrDefault(entity,List.of()));}
    }
    /** 只重算穿过实际变化格的射线和依赖该壳边界的组；无关方块不抹除维度进度。 */
    public static void shieldingChanged(ServerLevel level,BlockPos pos) {
        Set<BlockPos> frames=RadiationContainment.get(level).boundaryFrames(pos);
        for(var index:active(level))for(Job job:index.jobs.values()) {
            Set<Group> affected=Collections.newSetFromMap(new IdentityHashMap<>());
            affected.addAll(job.pathDependencies.getOrDefault(pos,Set.of()));
            for(BlockPos frame:frames)affected.addAll(job.frameDependencies.getOrDefault(frame,Set.of()));
            for(Group group:affected)job.dirty(group);
        }
    }
    /** 加载/卸载只重算已处理路径区块及已知壳范围命中的组，不撤销无关任务进度。 */
    public static void chunkChanged(ServerLevel level,int x,int z) {
        Set<BlockPos> frames=RadiationContainment.get(level).chunkFrames(x,z);
        long chunk=net.minecraft.world.level.ChunkPos.asLong(x,z);
        for(var index:active(level))for(Job job:index.jobs.values()) {
            Set<Group> affected=Collections.newSetFromMap(new IdentityHashMap<>());
            affected.addAll(job.pathChunkDependencies.getOrDefault(chunk,Set.of()));
            for(BlockPos frame:frames)affected.addAll(job.frameDependencies.getOrDefault(frame,Set.of()));
            for(Group group:affected)job.dirty(group);
        }
    }
    public static void unload(ServerLevel level,int x,int z) {
        for(var index:active(level)){
        for(Job job:index.jobs.values())job.invalidateSources(job.chunkSources.getOrDefault(net.minecraft.world.level.ChunkPos.asLong(x,z),List.of()));
        // 目标区块离开时不再交付该坐标的历史任务；源区块离开则只撤销其载荷。
        index.jobs.entrySet().removeIf(entry->BlockPos.containing(entry.getValue().target).getX()>>4==x&&BlockPos.containing(entry.getValue().target).getZ()>>4==z);}
        chunkChanged(level,x,z);
    }
    private void begin(ServerLevel level) {
        long now=level.getGameTime();if(time==now)return;time=now;stacks=0;blockEntities=0;rayCells=0;complete=true;
        jobs.entrySet().removeIf(entry->now-entry.getValue().lastRequested>40);
        RadiationContainment.get(level).beginSampling(now);
    }
    public RadiationFieldSample sample(ServerPlayer player,RadiationParameters p){return sample(player.serverLevel(),player.getUUID(),player.position(),p);}
    /** 直接场强查询与玩家共用算法；玩家入口按身份续接，目标行走不新建任务。 */
    public RadiationFieldSample sampleAt(ServerLevel level,Vec3 target,RadiationParameters p){return sample(level,target,target,p);}
    private RadiationFieldSample sample(ServerLevel level,Object key,Vec3 target,RadiationParameters p) {
        ACTIVE.computeIfAbsent(level,k->Collections.newSetFromMap(new WeakHashMap<>())).add(this);
        begin(level);Job job=jobs.get(key);
        if(job==null||job.level!=level||!job.p.equals(p)||job.target.distanceToSqr(target)>(double)p.detectionRadiusBlocks()*p.detectionRadiusBlocks()) {
            job=new Job(level,target,p,time);jobs.put(key,job);
        }
        job.lastRequested=time;
        for(var carrier:job.carrierRevisions.entrySet())if(carrier.getKey().getInventory().getTimesChanged()!=carrier.getValue()) {
            job.invalidateSources(job.entitySources.getOrDefault(carrier.getKey(),List.of()));carrier.setValue(carrier.getKey().getInventory().getTimesChanged());
        }
        try {
            advance(job);jobs.remove(key);return new RadiationFieldSample(Math.min(p.maxFieldDosePerSecond(),job.field),true,time);
        }catch(BudgetExceeded waiting){complete=false;return new RadiationFieldSample(0,false,time);}
    }
    private void advance(Job job) {
        while(true) switch(job.stage) {
            case 0 -> {
                while(job.ownerCursor<job.owners.size()) {
                    if(blockEntities>=limits.blockEntities())throw new BudgetExceeded();blockEntities++;
                    var owner=job.owners.get(job.ownerCursor);
                    if(!current(job.level,owner)){job.ownerCursor++;job.slotCursor=0;continue;}
                    int[] slot={0};RadiationSourceAdapters.read(owner,located->{
                        if(slot[0]++<job.slotCursor)return;
                        double strength=read(located.stack(),job.p)*(located.rackShielded()?job.p.rackTransmission():1);
                        BlockEntity inventoryOwner=owner;
                        if(owner instanceof com.simibubi.create.content.kinetics.belt.BeltBlockEntity belt&&belt.getControllerBE()!=null)inventoryOwner=belt.getControllerBE();
                        BlockEntity actualOwner=inventoryOwner;
                        // 带分段发现的载荷仍归控制端；跨帧提走后旧运输实例不再是有效源。
                        job.add(located.position(),located.stack(),strength,()->current(job.level,owner)&&current(job.level,actualOwner)
                                &&liveTransport(actualOwner,located.transportIdentity()),actualOwner.getBlockPos(),null,located.transportIdentity());job.slotCursor++;
                    });job.ownerCursor++;job.slotCursor=0;
                }job.stage=1;
            }
            case 1 -> {
                while(job.carrierCursor<job.carriers.size()) {
                    var carrier=job.carriers.get(job.carrierCursor);
                    job.carrierRevisions.putIfAbsent(carrier,carrier.getInventory().getTimesChanged());
                    while(job.carrierSlot<carrier.getInventory().getContainerSize()) {
                        int slot=job.carrierSlot;ItemStack stack=carrier.getInventory().getItem(slot);double strength=read(stack,job.p);
                        job.add(carrier.position(),stack,strength,()->carrier.serverLevel()==job.level&&!carrier.isRemoved()&&carrier.getInventory().getItem(slot)==stack,null,carrier,null);job.carrierSlot++;
                    }job.carrierCursor++;job.carrierSlot=0;
                }job.stage=2;
            }
            case 2 -> {
                while(job.entityCursor<job.entities.size()) {
                    var entity=job.entities.get(job.entityCursor);
                    if(!entity.isAlive()||!job.level.hasChunkAt(entity.blockPosition())){job.entityCursor++;continue;}
                    ItemStack stack=entity.getItem();double strength=read(stack,job.p);
                    job.add(entity.position(),stack,strength,()->entity.isAlive()&&entity.level()==job.level&&entity.getItem()==stack&&entity.position().distanceToSqr(job.target)<=(double)job.p.detectionRadiusBlocks()*job.p.detectionRadiusBlocks()&&job.level.hasChunkAt(entity.blockPosition()),entity.blockPosition(),entity,null);
                    job.entityCursor++;
                }
                job.orderedGroups=new ArrayList<>(job.groups.values());
                for(Group group:job.orderedGroups){job.dirty(group);for(BlockPos frame:RadiationContainment.get(job.level).sourceFrames(group.position))job.frameDependencies.computeIfAbsent(frame,k->Collections.newSetFromMap(new IdentityHashMap<>())).add(group);}
                job.stage=3;
            }
            case 3 -> {
                while(!job.rays.isEmpty()) {
                    Group group=job.rays.peek();
                    if(group.ray==null) {
                        group.containment=RadiationContainment.get(job.level).transmission(job.level,group.position,job.target,job.p,visibility);
                        if(!RadiationContainment.get(job.level).workComplete())throw new BudgetExceeded();
                        if(group.containment==0){group.pathTransmission=1;group.queued=false;job.rays.remove();continue;}
                        group.ray=new Ray(group.position,job.target,job.p.detectionRadiusBlocks());
                    }
                    while(true) {
                        if(rayCells>=limits.rayCells())throw new BudgetExceeded();rayCells++;
                        Ray ray=group.ray;BlockPos pos=new BlockPos(ray.x,ray.y,ray.z);
                        job.pathDependencies.computeIfAbsent(pos,k->Collections.newSetFromMap(new IdentityHashMap<>())).add(group);
                        // 未知区块也记录依赖，后来从磁盘载入的墙同样必须重算，不能只依赖setBlock通知。
                        job.pathChunkDependencies.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(ray.x>>4,ray.z>>4),k->Collections.newSetFromMap(new IdentityHashMap<>())).add(group);
                        // 未加载路径不读取，也不凭未知方块授予屏蔽；真实源本身始终要求已加载。
                        if(RadiationContainment.LOADED_CHUNKS.loaded(job.level,ray.x>>4,ray.z>>4)&&visibility.loaded(job.level,ray.x>>4,ray.z>>4)&&job.level.getBlockState(pos).is(SHIELDING))ray.transmission*=job.p.blockTransmission();
                        if(ray.step()){group.pathTransmission=ray.transmission;group.queued=false;job.rays.remove();break;}
                    }
                }job.stage=4;
            }
            case 4 -> {
                while(job.validationCursor<job.sources.size()) {
                    takeStack();Source source=job.sources.get(job.validationCursor);Group group=source.group;
                    if(group.strengthGeneration!=job.generation){group.strength=0;group.strengthGeneration=job.generation;}
                    if(source.valid(job.p))group.strength=RadiationItemSources.addFinite(group.strength,source.strength);
                    job.validationCursor++;
                }job.stage=5;
            }
            case 5 -> {
                while(job.sumCursor<job.orderedGroups.size()) {
                    takeStack();Group group=job.orderedGroups.get(job.sumCursor);
                    double strength=group.strengthGeneration==job.generation?group.strength:0;
                    double contribution=strength*job.p.distanceFactor(group.position.distanceToSqr(job.target))*group.containment*group.pathTransmission;
                    job.field=RadiationItemSources.addFinite(job.field,contribution);job.sumCursor++;
                }return;
            }
            default -> throw new IllegalStateException("unknown radiation sampling stage");
        }
    }
    private static boolean current(ServerLevel level,BlockEntity owner){return !owner.isRemoved()&&level.hasChunkAt(owner.getBlockPos())&&level.getBlockEntity(owner.getBlockPos())==owner;}
    private static boolean liveTransport(BlockEntity owner,Object identity) {
        if(identity==null)return true;
        if(!(owner instanceof com.simibubi.create.content.kinetics.belt.BeltBlockEntity belt))return false;
        var inventory=belt.getInventory();var queues=(com.iksxh.create_nuclear_industry.mixin.RadiationBeltAccessor)inventory;
        if(queues.radiation$getToRemove().stream().anyMatch(item->item==identity))return false;
        return inventory.getTransportedItems().stream().anyMatch(item->item==identity)||queues.radiation$getToInsert().stream().anyMatch(item->item==identity);
    }
    private void takeStack(){if(stacks>=limits.stacks())throw new BudgetExceeded();stacks++;}
    private double read(ItemStack stack,RadiationParameters p) {
        takeStack();CachedItem cache=itemCache.get(stack);
        if(cache==null||!cache.matches(stack,p)){cache=new CachedItem(stack,p);itemCache.put(stack,cache);}
        if(cache.result!=null)return cache.result.dosePerSecond();int before=cache.cursor.summary().visitedStacks();
        cache.cursor.advance(limits.stacks()-stacks);var summary=cache.cursor.summary();stacks+=summary.visitedStacks()-before;
        if(!summary.complete())throw new BudgetExceeded();cache.result=summary;return summary.dosePerSecond();
    }
    private static double first(double origin,int cell,double delta,int direction){return direction==0?Double.POSITIVE_INFINITY:((direction>0?cell+1:cell)-origin)/delta;}
}
