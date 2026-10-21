package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.accident.*;
import com.iksxh.create_nuclear_industry.blockentity.*;
import com.iksxh.create_nuclear_industry.config.*;
import com.iksxh.create_nuclear_industry.content.*;
import com.iksxh.create_nuclear_industry.production.*;
import com.iksxh.create_nuclear_industry.radiation.*;
import com.iksxh.create_nuclear_industry.reactor.*;
import com.iksxh.create_nuclear_industry.storage.*;
import com.iksxh.create_nuclear_industry.structure.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;

/** 第二批唯一隔离真实服域；所有库存为测试专用，绝不读取run/saves或用户世界。 */
@GameTestHolder("cni_accident_execution") @PrefixGameTestTemplate(false)
public final class AccidentExecutionGameTests {
    private static final String TEMPLATE="accident_empty";
    private static final BlockPos BASE=new BlockPos(4,2,4);
    private AccidentExecutionGameTests() {}
    private static boolean skipFixture(GameTestHelper h,String key) {
        var focus=System.getenv("CNI_ACCIDENT_FOCUS");
        if(!"1".equals(System.getenv("CNI_ACCIDENT_RESUME_ONLY"))&&(focus==null||focus.equals(key)
                ||("thermal_solid_fire".equals(focus)&&"shutdown".equals(key))
                ||("molten_corium".equals(focus)&&("shutdown".equals(key)||"thermal_solid_fire".equals(key)))))return false;
        System.out.println("[accident] focused validation: SKIP fixture "+key);h.succeed();return true;
    }
    private static boolean resumeOnly(GameTestHelper h) {return skipFixture(h,"business");}
    private static ItemStack fresh() {return new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());}
    private static ItemStack used() {return FuelUseHistory.prepareBurnCopy(fresh(),.001);}
    private static ReactorInstrumentPortBlockEntity build(GameTestHelper h,BlockPos base,int size) {
        for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++) {
            boolean edge=x==0||y==0||z==0||x==size-1||y==size-1||z==size-1;
            h.setBlock(base.offset(x,y,z),edge?P1Blocks.REACTOR_CASING.get():P1Blocks.REACTOR_FUEL_ROD.get());
        }
        for(int x=1;x<size-1;x++)for(int z=1;z<size-1;z++)h.setBlock(base.offset(x,size-1,z),P1Blocks.REACTOR_REFUELING_PORT.get());
        h.setBlock(base.offset(1,1,0),P1Blocks.REACTOR_INSTRUMENT_PORT.get());
        h.setBlock(base.offset(2,1,0),P1Blocks.REACTOR_COLD_PORT.get());h.setBlock(base.offset(3,1,0),P1Blocks.REACTOR_HOT_PORT.get());
        ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(base.offset(1,1,0)));
        var owner=(ReactorInstrumentPortBlockEntity)h.getBlockEntity(base.offset(1,1,0));
        h.assertTrue(owner.structureValid(),"真实反应堆未成型："+owner.structureScan().failureReason());return owner;
    }
    private static ReactorPortBlockEntity port(GameTestHelper h,BlockPos base,int size,int x,int z) {
        return (ReactorPortBlockEntity)h.getBlockEntity(base.offset(x+1,size-1,z+1));
    }
    private static ReactorAccidentLedger trigger(GameTestHelper h,ReactorInstrumentPortBlockEntity owner,ReactorMeltdownEvent.Reason reason) {
        return trigger(h,owner,reason,false);
    }
    /** 旧固体夹具显式用0份额；新增熔融业务才使用SERVER新默认50%，不以改变默认伪装通过。 */
    private static void solidOnly(Runnable action){
        try{
            var field=AccidentConfig.class.getDeclaredField("molten");field.setAccessible(true);var old=(MoltenCoriumParameters)field.get(null);
            field.set(null,new MoltenCoriumParameters(0,old.coolingTicks(),old.moveTicks(),old.corrosionTicks(),old.damageHp(),old.igniteSeconds(),old.solid()));
            try{action.run();}finally{field.set(null,old);}
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }
    private static ReactorAccidentLedger trigger(GameTestHelper h,ReactorInstrumentPortBlockEntity owner,ReactorMeltdownEvent.Reason reason,boolean molten) {
        var next=owner.snapshot().withMeltdown(P1ServerConfig.VALUES.meltdownCountdownTicks.get(),true).withMeltdownEventPublished(true);
        owner.setSnapshot(next);var e=new ReactorMeltdownEvent(reason,h.getLevel(),owner.structureOrigin(),owner.getBlockPos(),next);
        if(molten)NeoForge.EVENT_BUS.post(e);else solidOnly(()->NeoForge.EVENT_BUS.post(e));
        var ledger=ReactorAccidentSavedData.get(h.getLevel()).byGeneration(owner.accidentGeneration());
        h.assertTrue(ledger!=null,"正式事件没有事故任务");h.assertTrue(ReactorAccidentService.begin(e)==ledger,"事件重放另建任务");return ledger;
    }
    private static MoltenCoriumState molten(GameTestHelper h,BlockPos relative,ItemStack payload){
        var level=h.getLevel();var pos=h.absolutePos(relative);
        level.getChunkSource().getChunk(pos.getX()>>4,pos.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
        var state=new MoltenCoriumState(UUID.randomUUID(),null,pos,payload,MoltenCoriumParameters.DEFAULT);
        ReactorAccidentSavedData.get(level).addMolten(state);level.setBlock(pos,AccidentContent.MOLTEN_CORIUM.get().defaultBlockState(),18);return state;
    }
    private static void command(GameTestHelper h,String text){h.getLevel().getServer().getCommands().performPrefixedCommand(h.getLevel().getServer().createCommandSourceStack().withLevel(h.getLevel()).withSuppressedOutput(),text);}
    private static String coordinates(BlockPos p){return p.getX()+" "+p.getY()+" "+p.getZ();}
    private static void verifyHoseQueue(GameTestHelper h,BlockPos fluid){
        try{
            var at=new BlockPos(59,4,12);h.setBlock(at,com.simibubi.create.AllBlocks.HOSE_PULLEY.getDefaultState());
            var drainer=new com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour((com.simibubi.create.foundation.blockEntity.SmartBlockEntity)h.getBlockEntity(at));
            var type=drainer.getClass();var discover=type.getDeclaredMethod("canPullFluidsFrom",net.minecraft.world.level.block.state.BlockState.class,BlockPos.class);discover.setAccessible(true);
            for(int liquidLevel:new int[]{0,1}){
                command(h,"setblock "+coordinates(fluid)+" create_nuclear_industry:molten_corium[level="+liquidLevel+"]");
                h.assertTrue(discover.invoke(drainer,h.getLevel().getBlockState(fluid),fluid).toString().equals("NONE"),"软管仍发现熔融流体");
                var parent=type.getSuperclass();var root=parent.getDeclaredField("rootPos");root.setAccessible(true);root.set(drainer,fluid);
                var timer=parent.getDeclaredField("revalidateIn");timer.setAccessible(true);timer.setInt(drainer,1);
                var valid=type.getDeclaredField("isValid");valid.setAccessible(true);valid.setBoolean(drainer,true);
                var queueField=type.getDeclaredField("queue");queueField.setAccessible(true);
                @SuppressWarnings("unchecked") var queue=(it.unimi.dsi.fastutil.PriorityQueue<com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour.BlockPosEntry>)queueField.get(drainer);
                queue.enqueue(new com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour.BlockPosEntry(fluid,0));
                h.assertTrue(!drainer.pullNext(fluid,false)&&h.getLevel().getBlockState(fluid).is(AccidentContent.MOLTEN_CORIUM.get()),"软管已有队列抽取/删除了熔融格");
            }
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }

    /** 窄整改仅核原生箱子通知跨区块：真实FULL不可用时不移料，测试显式加载后再完成原生掉落。 */
    @GameTest(template=TEMPLATE,timeoutTicks=180)
    public static void moltenChestNotificationDomainWaitsForFull(GameTestHelper h){
        if(skipFixture(h,"molten_boundaries"))return;var level=h.getLevel();var origin=h.absolutePos(BlockPos.ZERO);
        var chunk=new net.minecraft.world.level.ChunkPos((origin.getX()>>4)+128,(origin.getZ()>>4)+128);
        var ticket=net.minecraft.server.level.TicketType.<net.minecraft.world.level.ChunkPos>create("cni_molten_boundary",Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
        level.getChunkSource().addRegionTicket(ticket,chunk,0,chunk);
        level.getChunkSource().getChunk(chunk.x,chunk.z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
        var chestPos=new BlockPos(chunk.getMinBlockX()+14,80,chunk.getMinBlockZ()+8);var second=chestPos.east(2);
        level.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),18);level.setBlock(chestPos.east(),Blocks.STONE.defaultBlockState(),18);
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chestPos);chest.setItem(0,new ItemStack(Items.DIAMOND,3));
        var data=ReactorAccidentSavedData.get(level);var body=new MoltenCoriumState(UUID.randomUUID(),null,chestPos.above(),ItemStack.EMPTY,MoltenCoriumParameters.DEFAULT);
        data.addMolten(body);level.setBlock(body.pos,AccidentContent.MOLTEN_CORIUM.get().defaultBlockState(),18);
        h.runAfterDelay(8,()->{
            var spawns=new java.util.concurrent.atomic.AtomicInteger();
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> listener=e->{if(e.getLevel()==level&&e.getEntity() instanceof ItemEntity item
                    &&item.getItem().is(Items.DIAMOND)&&item.position().distanceToSqr(Vec3.atCenterOf(chestPos))<9)spawns.addAndGet(item.getItem().getCount());};
            NeoForge.EVENT_BUS.addListener(listener);
            try{
                for(int i=0;i<3;i++)level.getChunkSource().tick(()->true,false);
                h.assertTrue(AccidentThermalEffects.loaded(level,chestPos)&&AccidentThermalEffects.loaded(level,chestPos.east())
                        &&!level.hasChunkAt(second)&&level.getChunkSource().getChunkNow(second.getX()>>4,second.getZ()>>4)==null,"箱子第二邻格FULL不可用前置没有成立");
                body.corrosionProgress=40;int spent=MoltenCoriumEffects.one(level,data,body);
                System.out.println("[molten-boundaries] chest missing FULL: spent="+spent+",inventory="+chest.getItem(0).getCount()+",spawns="+spawns.get()+",secondFull="+AccidentThermalEffects.loaded(level,second));
                h.assertTrue(spent==0&&level.getBlockEntity(chestPos)==chest&&chest.getItem(0).getCount()==3&&spawns.get()==0
                        &&!level.hasChunkAt(second)&&level.getChunkSource().getChunkNow(second.getX()>>4,second.getZ()>>4)==null,"箱子通知读取域未加载仍移料/删块/强载");
                level.getChunkSource().getChunk(second.getX()>>4,second.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
                h.assertTrue(AccidentThermalEffects.loaded(level,second),"测试显式加载第二邻格失败");
                h.assertTrue(MoltenCoriumEffects.one(level,data,body)==1&&level.getBlockState(chestPos).isAir()&&spawns.get()==3,"通知读取域齐备后没有成功原生掉落3钻石");
                System.out.println("[molten-boundaries] chest FULL wait -> explicit load -> native 3 diamonds passed");h.succeed();
            }finally{NeoForge.EVENT_BUS.unregister(listener);level.getChunkSource().removeRegionTicket(ticket,chunk,0,chunk);}
        });
    }
    /** 原位管理员重放或采掘后同tick出现，均须新空本体；旧HOT清理不能撤销新索引。 */
    @GameTest(template=TEMPLATE,timeoutTicks=80)
    public static void moltenReappearsOverHotWithNewIdentity(GameTestHelper h){
        if(skipFixture(h,"molten_boundaries"))return;var level=h.getLevel();var data=ReactorAccidentSavedData.get(level);
        for(int mode=0;mode<2;mode++){
            var old=molten(h,new BlockPos(20+8*mode,16,20),new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()));
            old.remaining=0;h.assertTrue(MoltenCoriumEffects.one(level,data,old)==1&&old.phase==MoltenCoriumState.Phase.HOT&&old.heat().active(),"HOT替换前置未凝固");
            if(mode==1)level.destroyBlock(old.pos,false);
            command(h,"setblock "+coordinates(old.pos)+" create_nuclear_industry:molten_corium");var next=data.moltenAt(old.pos);
            System.out.println("[molten-boundaries] HOT mode="+mode+",old="+old.phase+",next="+(next==null?"null":next.id+":"+next.phase));
            h.assertTrue(next!=null&&!next.id.equals(old.id)&&next.phase==MoltenCoriumState.Phase.MOLTEN&&next.placed&&next.payload().isEmpty()
                    &&next.remaining==6000&&old.phase==MoltenCoriumState.Phase.DONE&&!old.heat().active(),"HOT原位出现没有新空UUID/时钟或未终止旧引用");
            old.phase=MoltenCoriumState.Phase.DONE;data.reindexMolten(old,old.pos);MoltenCoriumEffects.one(level,data,old);
            h.assertTrue(data.moltenAt(next.pos)==next&&data.moltenWork().contains(next)&&!data.moltenWork().contains(old),"同tick旧引用终止撤掉新体索引");
            var pos=next.pos;next.moveProgress=20;next.advance(level.getGameTime()+1,true,false);
            h.assertTrue(MoltenCoriumEffects.one(level,data,next)==2&&next.pos.equals(pos.below())&&data.moltenAt(next.pos)==next&&next.remaining==5999
                    &&next.payload().isEmpty(),"新空本体没有继续独立计时/移动");
            int waste=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().filter(i->i.getItem().is(AccidentContent.ACCIDENT_FUEL_WASTE.get())).mapToInt(i->i.getItem().getCount()).sum();
            h.assertTrue(waste==1,"HOT真实废料未只走一次原生移除掉落");
        }
        System.out.println("[molten-boundaries] HOT replacement/mining same tick: fresh empty UUID + old cleanup isolation + move passed");h.succeed();
    }
    /** 下落及横移均不能抢占未投影记录；空气格不等于无物料所有者。 */
    @GameTest(template=TEMPLATE,timeoutTicks=80)
    public static void moltenPendingProjectionOwnsItsPosition(GameTestHelper h){
        if(skipFixture(h,"molten_boundaries"))return;var level=h.getLevel();var data=ReactorAccidentSavedData.get(level);
        for(int mode=0;mode<2;mode++){
            var a=molten(h,new BlockPos(24+10*mode,20,8),new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()));var destination=mode==0?a.pos.below():a.pos.north();
            if(mode==1){h.setBlock(new BlockPos(34,19,8),AccidentContent.MOLTEN_CORIUM.get());for(var direction:List.of(Direction.EAST,Direction.SOUTH,Direction.WEST))level.setBlock(a.pos.relative(direction),Blocks.BEDROCK.defaultBlockState(),18);}
            var b=new MoltenCoriumState(UUID.randomUUID(),null,destination,new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()),MoltenCoriumParameters.DEFAULT);data.addMolten(b);
            a.moveProgress=20;a.corrosionProgress=17;var before=a.pos;int revision=a.revision;int spent=MoltenCoriumEffects.one(level,data,a);
            System.out.println("[molten-boundaries] reserved mode="+mode+",spent="+spent+",a="+a.pos+",bOwner="+data.moltenAt(destination).id+",aMove="+a.moveProgress+",aRevision="+a.revision);
            h.assertTrue(spent==0&&a.pos.equals(before)&&a.revision==revision&&a.moveProgress==20&&a.corrosionProgress==17&&a.payload().getCount()==1
                    &&b.payload().getCount()==1&&!b.placed&&data.moltenAt(destination)==b&&data.moltenAt(before)==a,"移动抢占未投影本体，或冲突先改变进度/身份/载荷");
            boolean rejected=false;a.pos=destination;
            try{data.reindexMolten(a,before);}catch(IllegalStateException conflict){rejected=true;}finally{a.pos=before;}
            h.assertTrue(rejected&&data.moltenAt(destination)==b&&data.moltenAt(before)==a,"位置索引仍静默覆盖另一预留");
            h.assertTrue(MoltenCoriumEffects.one(level,data,b)==1&&b.placed&&data.moltenAt(destination)==b&&b.payload().getCount()==1&&a.payload().getCount()==1,"未投影本体未按原唯一所有权投影/保料");
        }
        System.out.println("[molten-boundaries] down/horizontal reservation collision, unchanged clocks/payloads, strict index + owner projection passed");h.succeed();
    }

    /** 新默认50%和独立世界本体的唯一真实服聚焦；原04A及shutdown夹具另以0份额复用。 */
    @GameTest(template=TEMPLATE,timeoutTicks=8600)
    public static void moltenWorldLifetimeProtectionAndContainers(GameTestHelper h){
        if(skipFixture(h,"molten_corium"))return;var level=h.getLevel();
        var owner=build(h,BASE,5);for(int i=0;i<3;i++)port(h,BASE,5,i,0).setFuelAssembly(used());port(h,BASE,5,0,1).setFuelAssembly(fresh());
        var ledger=trigger(h,owner,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE,true);
        h.assertTrue(ledger.moltenParameters.wasteFraction()==.5&&ledger.columns.stream().filter(c->c.molten).count()==2,"真实新默认不是3废料ceil50%=2熔融");
        var column=new BlockPos(40,180,4);for(int i=1;i<=80;i++)h.setBlock(column.below(i),Blocks.OBSIDIAN);h.setBlock(column.below(81),Blocks.BEDROCK);
        var deep=molten(h,column,ItemStack.EMPTY);var startDeep=deep.pos;
        var stable=new BlockPos(52,5,6);h.setBlock(stable.below(),Blocks.BEDROCK);var real=molten(h,stable,new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()));
        var wet=new BlockPos(56,5,6);h.setBlock(wet.below(),Blocks.BEDROCK);var cooled=molten(h,wet,new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()));
        h.setBlock(wet.west(),Blocks.OAK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true));
        var zeroAt=new BlockPos(52,5,12);h.setBlock(zeroAt.below(),Blocks.BEDROCK);h.setBlock(zeroAt,AccidentContent.MOLTEN_CORIUM.get());
        var zero=ReactorAccidentSavedData.get(level).moltenAt(h.absolutePos(zeroAt));h.assertTrue(zero!=null&&zero.source==null&&zero.payload().isEmpty()&&zero.dangerous(),"无事故世界入口没有独立空载荷本体");
        var denied=new BlockPos(8,12,20);h.setBlock(denied.below(),Blocks.OBSIDIAN);var protectedBody=molten(h,denied,ItemStack.EMPTY);var deniedPos=protectedBody.pos.below();
        java.util.function.Consumer<BlockEvent.BreakEvent> cancel=e->{if(e.getLevel()==level&&e.getPos().equals(deniedPos))e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,false,cancel);
        var throwingAt=new BlockPos(12,12,20);h.setBlock(throwingAt.below(),Blocks.CHEST);var throwingChest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getBlockEntity(throwingAt.below());throwingChest.setItem(0,new ItemStack(Items.EMERALD,5));
        var throwingBody=molten(h,throwingAt,ItemStack.EMPTY);
        var throwOnce=new java.util.concurrent.atomic.AtomicBoolean(true);
        java.util.function.Consumer<BlockEvent.BreakEvent> throwing=e->{if(e.getLevel()==level&&e.getPos().equals(throwingBody.pos.below())){if(throwOnce.getAndSet(false))throw new IllegalStateException("夹具保护异常");e.setCanceled(true);}};NeoForge.EVENT_BUS.addListener(throwing);
        var chestAt=new BlockPos(16,12,20);h.setBlock(chestAt.below(),Blocks.CHEST);((net.minecraft.world.level.block.entity.ChestBlockEntity)h.getBlockEntity(chestAt.below())).setItem(0,new ItemStack(Items.DIAMOND,3));
        var chestBody=molten(h,chestAt,ItemStack.EMPTY);
        var stationAt=new BlockPos(24,11,20);var stationBlock=FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get();
        h.setBlock(stationAt,stationBlock.defaultBlockState().setValue(ShieldedAssemblyBlock.EXPANDED,true));stationBlock.setPlacedBy(level,h.absolutePos(stationAt),h.getBlockState(stationAt),null,ItemStack.EMPTY);
        var station=(ShieldedAssemblyBlockEntity)h.getBlockEntity(stationAt);h.assertTrue(station.complete(),"装配台环境夹具未成型");station.insert(new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()));
        var stationInventory=station.savePortableData();var stationBody=molten(h,stationAt.above(2),ItemStack.EMPTY);
        var nuclearBase=new BlockPos(4,8,32);var nuclear=build(h,nuclearBase,5);var targetPort=port(h,nuclearBase,5,0,0);var named=used();named.set(DataComponents.CUSTOM_NAME,Component.literal("溶蚀原组件"));targetPort.setFuelAssembly(named);
        // 1.21.1的relativePos额外旋转180度，不能当作absolutePos的逆；沿建堆时的相对坐标放置。
        var nuclearBody=molten(h,nuclearBase.offset(1,5,1),ItemStack.EMPTY);
        h.assertTrue(nuclearBody.pos.equals(targetPort.getBlockPos().above()),"核燃料溶蚀夹具不在目标端口正上方");
        var accidentBodyIds=new ArrayList<UUID>();
        var collector=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CorrosionDropCollector"));
        var diamondSpawns=new java.util.concurrent.atomic.AtomicInteger();
        var expectedFuel=new java.util.concurrent.atomic.AtomicReference<>(named.copy());
        var nuclearAttempts=new java.util.concurrent.atomic.AtomicInteger();var nuclearCancelled=new java.util.concurrent.atomic.AtomicBoolean();
        java.util.function.Consumer<BlockEvent.BreakEvent> fuelProbe=e->{if(e.getLevel()==level&&e.getPos().equals(targetPort.getBlockPos())){nuclearAttempts.incrementAndGet();nuclearCancelled.set(e.isCanceled());if(MoltenCoriumEffects.environmental(e))expectedFuel.set(targetPort.fuelAssembly());}};
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,true,fuelProbe);
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> drops=e->{
            if(e.getLevel()!=level||!(e.getEntity() instanceof ItemEntity item))return;var stack=item.getItem();
            boolean diamond=stack.is(Items.DIAMOND)&&item.position().distanceToSqr(Vec3.atCenterOf(h.absolutePos(chestAt.below())))<9;
            boolean machine=stack.is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get())&&item.position().distanceToSqr(Vec3.atCenterOf(station.getBlockPos()))<16;
            boolean fuel=stack.is(named.getItem())&&named.get(DataComponents.CUSTOM_NAME).equals(stack.get(DataComponents.CUSTOM_NAME))&&item.position().distanceToSqr(Vec3.atCenterOf(targetPort.getBlockPos()))<9;
            if(diamond||machine||fuel){
                // 只在测试中立即领取真实原生掉落，隔离随后普通物品燃烧；不复制栈、不取消或重写生产掉落。
                if(diamond)diamondSpawns.addAndGet(stack.getCount());item.setNoPickUpDelay();item.playerTouch(collector);
            }
        };NeoForge.EVENT_BUS.addListener(drops);
        Runnable releaseProbes=()->{NeoForge.EVENT_BUS.unregister(cancel);NeoForge.EVENT_BUS.unregister(throwing);NeoForge.EVENT_BUS.unregister(drops);NeoForge.EVENT_BUS.unregister(fuelProbe);};
        var last=new java.util.concurrent.atomic.AtomicLong(ReactorAccidentSavedData.get(level).budgetActions);var peak=new java.util.concurrent.atomic.AtomicLong();
        h.onEachTick(()->{var now=ReactorAccidentSavedData.get(level).budgetActions;long delta=now-last.getAndSet(now);
            if(delta<0||delta>64)releaseProbes.run();h.assertTrue(delta>=0&&delta<=64,"原事故/火/热/独立本体共享预算越界："+delta);peak.accumulateAndGet(delta,Math::max);});
        h.runAfterDelay(30,()->{
            try{
            var data=ReactorAccidentSavedData.get(level);h.assertTrue(data.byId(ledger.id).complete(),"新相态延长原事故完成锁");
            var accidentBodies=data.moltenWork().stream().filter(s->ledger.id.equals(s.source)).toList();
            h.assertTrue(accidentBodies.size()==2&&accidentBodies.stream().mapToInt(s->s.payload().getCount()).sum()==2,"初始熔融载荷重复/丢失");
            var solid=ledger.columns.stream().filter(c->c.waste()&&!c.molten).findFirst().orElseThrow();h.assertTrue(((ReactorCoriumBlockEntity)level.getBlockEntity(solid.bottom)).payload().getCount()==1,"50%其余固体丢料");
            h.assertTrue(data.byId(real.id)==null,"独立世界夹具意外依赖ledger");
            var cow=net.minecraft.world.entity.EntityType.COW.create(level);float hp=cow.getHealth();level.getBlockState(real.pos).entityInside(level,real.pos,cow);
            h.assertTrue(cow.getHealth()==hp-4&&cow.getRemainingFireTicks()==100,"熔融物未4HP/5秒");cow.invulnerableTime=0;cow.clearFire();
            level.getBlockState(zero.pos).entityInside(level,zero.pos,cow);h.assertTrue(cow.getHealth()==hp-4&&cow.getRemainingFireTicks()<=0,"不同熔融格同类伤害叠加");
            var resistant=net.minecraft.world.entity.EntityType.COW.create(level);resistant.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,100));float safe=resistant.getHealth();
            level.getBlockState(real.pos).entityInside(level,real.pos,resistant);h.assertTrue(resistant.getHealth()==safe,"熔融伤害绕过火抗");
            var block=level.getBlockState(real.pos);h.assertTrue(((net.minecraft.world.level.block.BucketPickup)block.getBlock()).pickupBlock(null,level,real.pos,block).isEmpty(),"普通桶抽走熔融物");
            h.assertTrue(!((net.minecraft.world.item.BucketItem)Items.WATER_BUCKET).emptyContents(null,level,real.pos,null)
                    &&!level.setBlock(real.pos,Blocks.STONE.defaultBlockState(),3)&&!level.setBlock(real.pos,Blocks.WATER.defaultBlockState(),3),"普通放块/水覆盖无账删除本体");
            h.assertTrue(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,real.pos,Direction.UP)==null&&!level.getFluidState(real.pos).isSource(),"熔融物提供可抽capability/源液格");verifyHoseQueue(h,real.pos);
            var tag=data.save(new CompoundTag(),level.registryAccess());var restored=ReactorAccidentSavedData.load(tag,level.registryAccess());
            h.assertTrue(restored.molten(real.id).payload().getCount()==1&&restored.molten(real.id).remaining==data.molten(real.id).remaining,"本体当前格式恢复重置载荷/寿命");
            var admin=molten(h,new BlockPos(60,5,6),new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()));
            command(h,"setblock "+coordinates(admin.pos)+" minecraft:air");h.assertTrue(admin.phase==MoltenCoriumState.Phase.ADMIN_REMOVED&&admin.payload().isEmpty()&&level.getBlockState(admin.pos).isAir(),"权限setblock不能有意清除或重生本体");
            var fill=molten(h,new BlockPos(60,5,9),ItemStack.EMPTY);command(h,"fill "+coordinates(fill.pos)+" "+coordinates(fill.pos)+" minecraft:air");h.assertTrue(fill.phase==MoltenCoriumState.Phase.ADMIN_REMOVED&&level.getBlockState(fill.pos).isAir(),"权限fill未终止本体");
            var sample=RadiationSourceIndex.get(level).sampleAt(level,Vec3.atCenterOf(real.pos),RadiationParameters.DEFAULT);h.assertTrue(sample.complete()&&sample.fieldDosePerSecond()>=4,"独立真实熔融载荷没有进入原辐射栈预算");
            accidentBodies.forEach(body->accidentBodyIds.add(body.id));
            var withoutSource=data.save(new CompoundTag(),level.registryAccess());
            withoutSource.getList("Accidents",10).removeIf(entry->((CompoundTag)entry).getUUID("Id").equals(ledger.id));
            level.getDataStorage().set("create_nuclear_industry_accidents",ReactorAccidentSavedData.load(withoutSource,level.registryAccess()));
            h.assertTrue(ReactorAccidentSavedData.get(level).byId(ledger.id)==null,"夹具未删除来源ledger");
            System.out.println("[molten04b] native fluid + default 3 waste -> 2 molten/1 solid, independent zero payload, 4HP/5s/fire resistance, bucket/water/block/capability/hose queued guards, authorized setblock/fill, current NBT passed");
            }catch(RuntimeException|Error failure){releaseProbes.run();throw failure;}
        });
        h.runAfterDelay(48,()->{
            try{h.assertTrue(level.getBlockState(deniedPos).is(Blocks.OBSIDIAN)&&throwingChest.getItem(0).getCount()==5,"最终取消/监听异常仍删块或移料");}
            finally{releaseProbes.run();}
            h.assertTrue(diamondSpawns.get()==3&&collector.getInventory().countItem(Items.DIAMOND)==3,"普通箱子原生掉落/实收不是3钻石："+diamondSpawns.get());
            var portable=collector.getInventory().items.stream().filter(item->item.is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get())).toList();
            h.assertTrue(portable.size()==1&&portable.getFirst().getCount()==1&&portable.getFirst().get(DataComponents.CUSTOM_DATA).copyTag().getCompound("CniShieldedAssembly").equals(stationInventory),"装配台8格环境拆除没有单件完整携物");
            for(int i=0;i<8;i++)h.assertTrue(!level.getBlockState(ShieldedAssemblyLayout.position(station.getBlockPos(),Direction.NORTH,i)).hasBlockEntity(),"多格溶蚀漏了连带格");
            var returns=collector.getInventory().items.stream().filter(item->ItemStack.matches(item,expectedFuel.get())).toList();
            var liveNuclear=ReactorAccidentSavedData.get(level).molten(nuclearBody.id);
            h.assertTrue(returns.size()==1&&targetPort.fuelAssembly().isEmpty()&&!nuclear.structureValid()&&nuclear.accidentId()==null
                    &&ReactorAccidentSavedData.get(level).byGeneration(nuclear.accidentGeneration())==null,"核燃料环境交接状态：returned="+returns.size()+",port="+targetPort.fuelAssembly()
                    +",valid="+nuclear.structureValid()+",accident="+nuclear.accidentId()+",expected="+expectedFuel.get()+",attempts="+nuclearAttempts.get()+",cancel="+nuclearCancelled.get()
                    +",body="+liveNuclear.pos+",below="+level.getBlockState(liveNuclear.pos.below())+",remaining="+liveNuclear.remaining+",progress="+liveNuclear.corrosionProgress);
            System.out.println("[molten04b] final cancellation/exception retain inventories; chest native 3 diamonds, nuclear exact named fuel, assembly one portable inventory/eight actual block writes passed");
        });
        h.runAfterDelay(1510,()->{
            var current=ReactorAccidentSavedData.get(level).molten(cooled.id);h.assertTrue(current.phase!=MoltenCoriumState.Phase.MOLTEN&&level.getBlockEntity(current.pos) instanceof ReactorCoriumBlockEntity be&&be.payload().getCount()==1,"接水6000/4未凝固移交唯一废料");
        });
        h.runAfterDelay(4000,()->{
            var current=ReactorAccidentSavedData.get(level).molten(deep.id);h.assertTrue(current.pos.getY()<=startDeep.getY()-80&&level.getBlockState(startDeep.below(81)).is(Blocks.BEDROCK),"未越旧6格/64累计上限或基岩被熔穿");
            System.out.println("[molten04b] world body crossed 80 obsidian layers without source bounds; bedrock retained; total auto block writes="+ReactorAccidentSavedData.get(level).automaticBlockWrites);
        });
        h.runAfterDelay(8420,()->{
            var data=ReactorAccidentSavedData.get(level);var current=data.molten(real.id);
            h.assertTrue(current.phase==MoltenCoriumState.Phase.DONE&&level.getBlockEntity(current.pos) instanceof ReactorCoriumBlockEntity be&&be.payload().getCount()==1&&!level.getBlockState(current.pos).getValue(ReactorCoriumBlock.HOT),"6000->2400未独立凝固冷却且保留单件废料");
            var currentZero=data.molten(zero.id);h.assertTrue(level.getBlockEntity(currentZero.pos) instanceof ReactorCoriumBlockEntity empty&&empty.payload().isEmpty(),"指令零载荷凝固凭空造废料");
            for(var id:accidentBodyIds){var independent=data.molten(id);h.assertTrue(data.byId(ledger.id)==null&&independent.phase==MoltenCoriumState.Phase.DONE
                    &&level.getBlockEntity(independent.pos) instanceof ReactorCoriumBlockEntity kept&&kept.payload().getCount()==1,"来源ledger删除后本体未独立完成/保料");}
            h.assertTrue(peak.get()==64,"新/旧/无事故共享预算未实触64/t");System.out.println("[molten04b] 6000->2400 loaded ticks/water x4, empty cold safety and shared dimension peak64 passed; physical unload/real multiplayer not exercised");h.succeed();
        });
    }
    /** 本次冷态现场的六燃料/三全插棒布局；只补缺失方块，保留事故后实际幸存端口的BE身份。 */
    private static ReactorInstrumentPortBlockEntity shutdownReactor(GameTestHelper h) {
        var roles=new TreeMap<CoreColumnPosition,ReactorStructureDefinition.ColumnType>();
        for(int x=0;x<3;x++)for(int z=0;z<3;z++)roles.put(new CoreColumnPosition(x,z),z==1
                ?ReactorStructureDefinition.ColumnType.CONTROL_ROD:ReactorStructureDefinition.ColumnType.FUEL);
        for(var entry:ReactorStructureDefinition.templateFor(roles).entrySet()) {
            var local=entry.getKey();var pos=BASE.offset(local.x(),local.y(),local.z());
            var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(entry.getValue()));
            if(!h.getBlockState(pos).is(block))h.setBlock(pos,block);
        }
        var at=BASE.offset(2,2,0);ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(at));
        var owner=(ReactorInstrumentPortBlockEntity)h.getBlockEntity(at);
        h.assertTrue(owner.structureValid(),"事故残余重建没有有效几何");return owner;
    }

    /** 真实事故收据完成后，残余空口不能阻止后建冷堆的原生安全维护；旧锁或每次重扫换代都会使本例失败。 */
    @GameTest(template=TEMPLATE,timeoutTicks=160)
    public static void completedAccidentEmptyPortsRebindForColdShutdown(GameTestHelper h) {
        if(skipFixture(h,"shutdown"))return;
        var old=shutdownReactor(h);var ports=List.copyOf(old.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING));
        var cold=old.boundPorts(ReactorPortBlockEntity.BindingType.COLD_COOLANT).getFirst();
        var staleHandler=ReactorCoolantFluidHandler.forPort(cold);h.assertTrue(staleHandler!=null,"缺少真实旧冷端能力");
        var fuels=new TreeMap<CoreColumnPosition,FuelColumnState>();
        for(int i=0;i<ports.size();i++) {
            var item=i==0?used():fresh();item.set(DataComponents.CUSTOM_NAME,Component.literal("停机原件"+i));
            ports.get(i).setFuelAssembly(item);fuels.put(ports.get(i).boundColumn(),new FuelColumnState(FuelAssemblyState.empty(),0,4));
        }
        old.setSnapshot(old.snapshot().withColumns(fuels,old.snapshot().controlRodColumns())
                .withMeltdown(P1ServerConfig.VALUES.meltdownCountdownTicks.get()-1L,true));
        solidOnly(old::tickReactor);h.assertTrue(old.accidentId()!=null,"正式末tick未触发真实事故");
        var ledger=ReactorAccidentSavedData.get(h.getLevel()).byId(old.accidentId());
        h.runAfterDelay(30,()->{
            h.assertTrue(ledger.complete(),"正式事故尚未完成，不能作为过期锁用例");
            h.assertTrue(ledger.thermal.active(),"空口重绑必须发生在真实热阶段仍活跃时");
            var survivors=ports.stream().filter(p->!p.isRemoved()&&h.getLevel().getBlockEntity(p.getBlockPos())==p).toList();
            h.assertTrue(!survivors.isEmpty(),"32块壳上限未留下真实空换料口");
            var locked=survivors.getFirst();var lockedTag=locked.saveWithFullMetadata(h.getLevel().registryAccess());
            h.assertTrue(lockedTag.hasUUID("AccidentLock")&&ledger.id.equals(lockedTag.getUUID("AccidentLock")),"幸存口不是本事故原始锁");
            var originalGenerations=survivors.stream().map(ReactorPortBlockEntity::accidentGeneration).toList();
            var receipt=ledger.save(h.getLevel().registryAccess()).copy();receipt.remove("Thermal");
            var data=ReactorAccidentSavedData.get(h.getLevel());
            h.getLevel().getDataStorage().set("create_nuclear_industry_accidents",ReactorAccidentSavedData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess()));
            var waste=ledger.columns.stream().filter(ReactorAccidentLedger.Column::waste).findFirst().orElseThrow();
            var corium=(ReactorCoriumBlockEntity)h.getLevel().getBlockEntity(waste.bottom);
            var recovered=corium.take();h.assertTrue(recovered.is(AccidentContent.ACCIDENT_FUEL_WASTE.get())&&recovered.getCount()==1,"真实残骸未提供唯一回收载荷");
            var next=shutdownReactor(h);h.assertTrue(next!=old&&!next.accidentGeneration().equals(old.accidentGeneration()),"未建立后建仪表世代");
            next.setCoolantInventories(9000,0);
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ShutdownBreak"));
            var event=net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(),GameType.SURVIVAL,player,next.getBlockPos(),next.getBlockState());
            h.assertTrue(!event.isCanceled(),"已完成事故的空口使冷堆原生维护拆除仍被尚未完全停机取消");
            for(int i=0;i<survivors.size();i++) {
                var p=survivors.get(i);h.assertTrue(p.isBound()&&!p.accidentGeneration().equals(originalGenerations.get(i)),"真实残余空口未以新物理代际重绑");
            }
            h.assertTrue(next.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING).size()==6,"新堆漏绑残余列");
            var generation=locked.accidentGeneration();long binding=locked.bindingGeneration();
            var saved=locked.saveWithFullMetadata(h.getLevel().registryAccess());
            locked.loadWithComponents(saved,h.getLevel().registryAccess());ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),next.getBlockPos());
            h.assertTrue(locked.isBound()&&generation.equals(locked.accidentGeneration())&&binding==locked.bindingGeneration(),"当前格式保存/重扫重复换代或失效绑定");
            h.assertTrue(locked.tryInsertFuel(fresh()).success(),"已恢复残余口不能正式补料");
            h.assertTrue(locked.tryExtractFuel().success(),"冷态恢复口不能正式取料");
            h.assertTrue(staleHandler.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(),1),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)==0,"旧能力在新世代复活");
            verifyShutdownRebindGuards(h,locked,next,old,ledger,lockedTag,saved);
            var currentLedger=ReactorAccidentSavedData.get(h.getLevel()).byId(ledger.id);
            var currentReceipt=currentLedger.save(h.getLevel().registryAccess());currentReceipt.remove("Thermal");
            h.assertTrue(receipt.equals(currentReceipt)&&currentLedger.columns.stream().allMatch(c->c.payload().isEmpty()),"重绑改动已读回旧账本或再产托管");
            int returned=0;
            for(var c:ledger.columns)if(!c.waste()) {
                var id=UUID.nameUUIDFromBytes((ledger.id+":"+c.port.asLong()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                h.assertTrue(h.getLevel().getEntity(id) instanceof ItemEntity item&&ItemStack.matches(item.getItem(),c.original()),"已交接全新原件丢失/改变");returned++;
            }
            h.assertTrue(returned==5&&recovered.getCount()==1&&next.snapshot().coldCoolantMb()==9000&&next.snapshot().hotCoolantMb()==0,"旧5原件+1废料或新冷堆工质不守恒");
            ReactorAccidentExecutor.tick(h.getLevel());
            h.assertTrue(ledger.complete()&&ledger.columns.stream().allMatch(c->c.settled()&&c.payload().isEmpty())
                    &&ledger.actions.stream().allMatch(action->action.done),"完成事故重放重开了原接管/破裂动作");
            h.assertTrue(locked.tryInsertFuel(fresh()).success(),"安全清空前未装入真实组件");
            var shell=BASE;var worldShell=h.absolutePos(shell);
            var reset=net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(),GameType.SURVIVAL,player,worldShell,h.getBlockState(shell));
            h.assertTrue(!reset.isCanceled()&&next.snapshot().fuelColumns().isEmpty()&&next.snapshot().coldCoolantMb()==0
                    &&next.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING).stream().allMatch(p->p.fuelAssembly().isEmpty()),"正式外壳拆除未通过FULL_SHUTDOWN_RESET清空真实口");
            h.setBlock(shell,Blocks.AIR);ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),next.getBlockPos());
            h.assertTrue(!next.structureValid(),"原生安全事务后实际拆壳未使结构失效");
            System.out.println("[shutdown03] native final-tick accident -> retained "+survivors.size()+" empty ports -> new cold owner -> CommonHooks maintenance and FULL_SHUTDOWN_RESET allowed; receipts/5 originals/1 recovered waste, guards, save and idempotence passed");h.succeed();
        });
    }

    /** 单次聚焦覆盖两事故共预算、实体接触、有限火、实际加载冷却及当前格式真实BE/DataStorage恢复。 */
    @GameTest(template=TEMPLATE,timeoutTicks=2500)
    public static void thermalSolidAndFiniteFire(GameTestHelper h) {
        if(skipFixture(h,"thermal_solid_fire"))return;
        var level=h.getLevel();var first=build(h,BASE,5);int inserted=0;
        for(var p:first.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING))p.setFuelAssembly(++inserted==9?fresh():used());
        first.setCoolantInventories(123,456);var a=trigger(h,first,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);
        var second=build(h,BASE.offset(18,0,0),5);
        for(var p:second.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING))p.setFuelAssembly(used());
        var b=trigger(h,second,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);
        var previous=new HashMap<UUID,Integer>();ReactorAccidentSavedData.get(level).records().forEach(l->previous.put(l.id,l.worldActions));
        var peak=new java.util.concurrent.atomic.AtomicInteger();
        h.onEachTick(()->{
            int delta=0;for(var ledger:ReactorAccidentSavedData.get(level).records()) {
                var old=previous.put(ledger.id,ledger.worldActions);if(old!=null)delta+=ledger.worldActions-old;
            }
            h.assertTrue(delta>=0&&delta<=64,"同维度多个事故原动作/热火写超过64/t："+delta);peak.accumulateAndGet(delta,Math::max);
        });
        h.runAfterDelay(30,()->{
            var data=ReactorAccidentSavedData.get(level);var currentA=data.byId(a.id);var currentB=data.byId(b.id);
            h.assertTrue(currentA.complete()&&currentB.complete()&&currentA.thermal.active()&&currentB.thermal.active(),"原事故完成被热阶段延长");
            h.assertTrue(currentA.coldMb==123&&currentA.hotMb==456&&first.snapshot().coldCoolantMb()==0&&first.snapshot().hotCoolantMb()==0,"工质全损改变");
            int waste=0;for(var ledger:List.of(currentA,currentB))for(var c:ledger.columns)if(c.waste()) {
                var be=(ReactorCoriumBlockEntity)level.getBlockEntity(c.bottom);h.assertTrue(be!=null&&be.payload().getCount()==1&&c.payload().isEmpty(),"托管/BE双有或缺料");waste++;
            }
            h.assertTrue(waste==17&&currentA.columns.stream().filter(c->!c.waste()).count()==1,"已用/未用组件分类不守恒");
            var returned=currentA.columns.stream().filter(c->!c.waste()).findFirst().orElseThrow();
            var returnedId=UUID.nameUUIDFromBytes((currentA.id+":"+returned.port.asLong()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            h.assertTrue(level.getEntity(returnedId) instanceof ItemEntity item&&ItemStack.matches(item.getItem(),returned.original()),"未用原件没有唯一返还");
            var hot=currentA.columns.stream().filter(ReactorAccidentLedger.Column::waste).findFirst().orElseThrow().bottom;
            var hot2=currentB.columns.getLast().bottom;
            var standing=net.minecraft.world.entity.EntityType.COW.create(level);standing.setNoAi(true);standing.setPos(Vec3.atBottomCenterOf(hot2.above()));
            float standingHealth=standing.getHealth();level.addFreshEntity(standing);
            standing.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(.01,-.08,0));
            h.runAfterDelay(3,()->{
                h.assertTrue(standing.onGround()&&standing.getY()==hot2.getY()+1&&standing.getHealth()==standingHealth-2
                        &&standing.getRemainingFireTicks()>0,"真实顶面碰撞状态：ground="+standing.onGround()+",y="+standing.getY()+",expected="+(hot2.getY()+1)
                        +",hp="+standing.getHealth()+",before="+standingHealth+",fire="+standing.getRemainingFireTicks());standing.discard();
            });
            var cow=net.minecraft.world.entity.EntityType.COW.create(level);float health=cow.getHealth();
            level.getBlockState(hot).getBlock().stepOn(level,hot,level.getBlockState(hot),cow);
            h.assertTrue(cow.getHealth()==health-2&&cow.getRemainingFireTicks()==60,"默认接触不是2HP/3秒");
            cow.invulnerableTime=0;cow.clearFire();
            for(int i=0;i<20;i++)level.getBlockState(hot2).getBlock().stepOn(level,hot2,level.getBlockState(hot2),cow);
            h.assertTrue(cow.getHealth()==health-2&&cow.getRemainingFireTicks()<=0,"同实体同类多格绕过20tick节流");
            var resistant=net.minecraft.world.entity.EntityType.COW.create(level);resistant.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,200));
            float resistantHealth=resistant.getHealth();level.getBlockState(hot).getBlock().stepOn(level,hot,level.getBlockState(hot),resistant);
            h.assertTrue(resistant.getHealth()==resistantHealth,"普通火抗未抵挡直接热伤");
            var empty=h.absolutePos(BASE.offset(0,7,0));level.setBlock(empty,AccidentContent.REACTOR_CORIUM.get().defaultBlockState().setValue(ReactorCoriumBlock.HOT,true),18);
            var safe=net.minecraft.world.entity.EntityType.COW.create(level);float safeHealth=safe.getHealth();
            level.getBlockState(empty).getBlock().stepOn(level,empty,level.getBlockState(empty),safe);
            h.assertTrue(safe.getHealth()==safeHealth&&safe.getRemainingFireTicks()<=0,"空高温外观凭空灼烧");
            h.assertTrue(currentA.thermal.fires().size()==8&&currentA.thermal.fires().stream().filter(f->f.placed).count()==8,"同次事故没有8个独立有限火租约");
            var extinguished=currentA.thermal.fires().getFirst();
            var firefighter=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ThermalFirefighter"));
            firefighter.setPos(Vec3.atCenterOf(extinguished.pos).add(0,0,1));firefighter.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            firefighter.gameMode.handleBlockBreakAction(extinguished.pos,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.UP,level.getMaxBuildHeight(),1);
            h.assertTrue(extinguished.terminal&&level.getBlockState(extinguished.pos).isAir(),"原生玩家左键灭火未终止租约");
            var wetFire=currentA.thermal.fires().get(1);level.setBlock(wetFire.pos.above(),Blocks.WATER.defaultBlockState(),3);
            h.runAfterDelay(10,()->{
                var liveFire=ReactorAccidentSavedData.get(level).byId(a.id).thermal.fire(wetFire.id);
                h.assertTrue(liveFire.terminal&&level.getFluidState(wetFire.pos).is(net.minecraft.tags.FluidTags.WATER)
                        &&!level.getBlockState(wetFire.pos).is(AccidentContent.ACCIDENT_FIRE.get()),"真实向下水流没有到达并扑灭火点");
                // 仅清理夹具自行放出的水，避免改变远处自然冷却参照；保留水灭火终止收据。
                for(var pos:BlockPos.betweenClosed(h.absolutePos(BASE.offset(-2,0,-2)),h.absolutePos(BASE.offset(7,5,7))))
                    if(level.getBlockState(pos).is(Blocks.WATER))level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            });
            var expired=currentA.thermal.fires().get(2);expired.remaining=1;
            int available=data.availableBudget(level.getGameTime(),64);data.spendBudget(available);
            ReactorAccidentExecutor.tick(level);
            h.assertTrue(expired.terminal&&!((AccidentFireBlock.Lease)level.getBlockEntity(expired.pos)).dangerous()
                    &&level.getBlockState(expired.pos).is(AccidentContent.ACCIDENT_FIRE.get()),"预算耗尽时到期仍伤害或绕预算清块");
            level.setBlock(expired.pos,Blocks.STONE.defaultBlockState(),18);
            var be=(ReactorCoriumBlockEntity)level.getBlockEntity(hot);var beTag=be.saveWithFullMetadata(level.registryAccess());
            int remaining=currentA.thermal.solid(hot).remaining;var fireTag=currentA.thermal.save();
            be.loadWithComponents(beTag,level.registryAccess());
            level.getDataStorage().set("create_nuclear_industry_accidents",ReactorAccidentSavedData.load(data.save(new CompoundTag(),level.registryAccess()),level.registryAccess()));
            var restored=ReactorAccidentSavedData.get(level).byId(a.id);
            h.assertTrue(restored.thermal.solid(hot).remaining==remaining&&restored.thermal.save().equals(fireTag)&&be.payload().getCount()==1,"当前格式恢复重置热/火或物料");
            var wet=hot.west();level.setBlock(wet,Blocks.OAK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true),18);
            var dose=RadiationItemSources.describe(be.payload(),RadiationParameters.DEFAULT).dosePerSecond();
            var mined=currentB.columns.getFirst().bottom;level.destroyBlock(mined,true,firefighter);
            var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(mined).inflate(1)).stream()
                    .filter(item->item.getItem().is(AccidentContent.ACCIDENT_FUEL_WASTE.get())).toList();
            h.assertTrue(drops.stream().mapToInt(item->item.getItem().getCount()).sum()==1
                    &&ReactorAccidentSavedData.get(level).byId(b.id).thermal.solid(mined).terminal,"真实提前采掘未恰好掉一份废料或遗留热源");
            var wood=currentB.thermal.fires().getFirst().pos.above();level.setBlock(wood,Blocks.OAK_PLANKS.defaultBlockState(),18);
            h.runAfterDelay(20,()->{
                cow.invulnerableTime=0;level.getBlockState(hot).getBlock().stepOn(level,hot,level.getBlockState(hot),cow);
                h.assertTrue(cow.getHealth()==health-4,"20tick后同实体没有恢复接触伤害");
            });
            h.runAfterDelay(605,()->{
                var latest=ReactorAccidentSavedData.get(level).byId(a.id);
                h.assertTrue(latest.thermal.solid(hot).remaining==0&&!level.getBlockState(hot).getValue(ReactorCoriumBlock.HOT),"实际已加载接水600tick没有冷却投影");
                h.assertTrue(be.payload().getCount()==1&&RadiationItemSources.describe(be.payload(),RadiationParameters.DEFAULT).dosePerSecond()==dose,"冷却改变真实废料或辐射");
                h.assertTrue(latest.thermal.fires().stream().allMatch(f->f.terminal&&f.cleaned),"600tick后仍有火或灭后复燃");
                h.assertTrue(level.getBlockState(wood).is(Blocks.OAK_PLANKS),"有限火蔓延烧掉了邻接木块");
                h.assertTrue(level.getBlockState(expired.pos).is(Blocks.STONE)&&!level.getBlockState(extinguished.pos).is(AccidentContent.ACCIDENT_FIRE.get()),"到期清理删除后来方块或重燃");
                h.assertTrue(ReactorAccidentSavedData.get(level).byId(b.id).thermal.solid(hot2).remaining>0,"自然冷却被接水夹具污染");
                System.out.println("[thermal04a] 17 real waste + 1 exact fresh return; 2HP/3s/20tick, fire resistance, empty safety, 8 fire leases/600tick/extinguish/identity, water x4 and BE/SavedData restore passed");
            });
            h.runAfterDelay(2410,()->{
                var latest=ReactorAccidentSavedData.get(level).byId(b.id);
                h.assertTrue(latest.thermal.solid(hot2).remaining==0&&!level.getBlockState(hot2).getValue(ReactorCoriumBlock.HOT),"2400实际加载tick后自然残骸仍高温");
                h.assertTrue(ReactorAccidentSavedData.get(level).thermalWork().isEmpty(),"空闲仍保留已结束热事故索引");
                h.assertTrue(peak.get()==64,"双事故夹具未实际触达共享64/t上限");
                System.out.println("[thermal04a] real 2400 loaded tick cooling complete; all thermal active indexes drained; shared dimension peak="+peak.get()+"; physical unload and real multiplayer not exercised");h.succeed();
            });
        });
    }

    /** 只在隔离夹具恢复真实旧锁NBT来构造拒绝输入；每次失败必须保留身份、锁和库存。 */
    private static void rejectedShutdownRebind(GameTestHelper h,ReactorPortBlockEntity port,ReactorInstrumentPortBlockEntity owner,
            CompoundTag tag,ReactorPortBlockEntity.BindingType type,CoreColumnPosition column,String reason) {
        port.loadWithComponents(tag,h.getLevel().registryAccess());var before=port.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(!port.bindTo(owner,type,column)&&before.equals(port.saveWithFullMetadata(h.getLevel().registryAccess())),"无效重绑提前解锁/改身份："+reason);
    }

    private static void verifyShutdownRebindGuards(GameTestHelper h,ReactorPortBlockEntity port,ReactorInstrumentPortBlockEntity next,
            ReactorInstrumentPortBlockEntity old,ReactorAccidentLedger ledger,CompoundTag locked,CompoundTag restored) {
        var column=port.boundColumn();var type=ReactorPortBlockEntity.BindingType.REFUELING;
        var missing=locked.copy();missing.putUUID("AccidentLock",UUID.randomUUID());rejectedShutdownRebind(h,port,next,missing,type,column,"缺账本");
        var mismatch=locked.copy();mismatch.putUUID("AccidentGeneration",UUID.randomUUID());rejectedShutdownRebind(h,port,next,mismatch,type,column,"物理代际不匹配");
        var occupied=locked.copy();occupied.put("FuelAssembly",fresh().save(h.getLevel().registryAccess()));rejectedShutdownRebind(h,port,next,occupied,type,column,"当前口非空");
        rejectedShutdownRebind(h,port,old,locked,type,column,"原事故所有者");
        rejectedShutdownRebind(h,port,next,locked,ReactorPortBlockEntity.BindingType.COLD_COOLANT,null,"职责错误");
        rejectedShutdownRebind(h,port,next,locked,type,new CoreColumnPosition(column.x(),1),"结构列不占用该口");
        var unfinished=ledger.save(h.getLevel().registryAccess());var id=UUID.randomUUID();unfinished.putUUID("Id",id);unfinished.putUUID("Generation",UUID.randomUUID());unfinished.putBoolean("ShockComplete",false);
        var pendingReceipt=ReactorAccidentLedger.load(unfinished,h.getLevel().registryAccess());ReactorAccidentSavedData.get(h.getLevel()).add(pendingReceipt);
        var pending=locked.copy();pending.putUUID("AccidentLock",id);rejectedShutdownRebind(h,port,next,pending,type,column,"事故未完成");
        pendingReceipt.shockComplete=true;
        port.loadWithComponents(restored,h.getLevel().registryAccess());ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),next.getBlockPos());
        h.assertTrue(port.isBound(),"反例后未恢复新代际口");
    }

    @GameTest(template=TEMPLATE,timeoutTicks=120)
    public static void handoffStopsSameTickAndCanceledBreakDoesNotEnqueue(GameTestHelper h) {
        if(resumeOnly(h))return;
        var owner=build(h,BASE,5);var fuel=port(h,BASE,5,0,0);fuel.setFuelAssembly(used());
        var player=h.makeMockPlayer(GameType.SURVIVAL);var before=ReactorAccidentSavedData.get(h.getLevel()).records().size();
        var canceled=new BlockEvent.BreakEvent(h.getLevel(),owner.getBlockPos(),owner.getBlockState(),player);canceled.setCanceled(true);
        ReactorStructureLifecycle.onBlockBreak(canceled);h.assertTrue(before==ReactorAccidentSavedData.get(h.getLevel()).records().size(),"取消拆除留下事故");
        var fuels=new TreeMap<CoreColumnPosition,FuelColumnState>();
        owner.structureScan().columns().keySet().forEach(c->fuels.put(c,new FuelColumnState(FuelAssemblyState.empty(),0,0)));
        owner.setSnapshot(owner.snapshot().withColumns(fuels,Map.of()).withMeltdown(P1ServerConfig.VALUES.meltdownCountdownTicks.get()-1L,true));
        h.assertTrue(owner.tickReactor(),"正式倒计时末tick没有提交");
        h.assertTrue(owner.accidentId()!=null&&!owner.structureValid()&&owner.automationBinding()==null,"事故没有即时锁堆撤权");
        h.assertTrue(owner.snapshot().coldCoolantMb()==0&&owner.snapshot().hotCoolantMb()==0,"工质没有扣账");
        h.assertTrue(owner.runtimeDescriptor().isEmpty()||!owner.runtimeDescriptor().get().available(),"旧result恢复运行样本");
        h.assertTrue(!owner.tickReactor()&&!fuel.tryInsertFuel(fresh()).success(),"旧端口或旧tick重新运行");
        h.assertTrue(fuel.fuelAssembly().isEmpty(),"原口与托管同时持料");
        System.out.println("[accident] formal same-tick handoff and canceled-break assertions passed");h.succeed();
    }
    @GameTest(template=TEMPLATE,timeoutTicks=180)
    public static void custodyConservesPhysicalStacksAcrossReplay(GameTestHelper h) {
        if(resumeOnly(h))return;
        var owner=build(h,BASE,5);var p0=port(h,BASE,5,0,0);var p1=port(h,BASE,5,1,0);var p2=port(h,BASE,5,2,0);
        p0.setFuelAssembly(used());var named=fresh();named.set(DataComponents.CUSTOM_NAME,Component.literal("全新原件"));p1.setFuelAssembly(named);
        p2.setFuelAssembly(new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()));owner.setCoolantInventories(123,456);
        var ledger=trigger(h,owner,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);
        h.assertTrue(ledger.columns.size()==3&&ledger.columns.stream().filter(ReactorAccidentLedger.Column::waste).count()==2,"组件按高度/投影复制或漏掉小数历史");
        h.assertTrue(ledger.coldMb==123&&ledger.hotMb==456,"原工质未入账");
        var saved=ReactorAccidentLedger.load(ledger.save(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(saved.columns.size()==3&&saved.columns.stream().allMatch(ReactorAccidentLedger.Column::captured),"当前格式托管保存丢料");
        h.runAfterDelay(30,()->{
            h.assertTrue(ledger.complete(),"小堆事故未完成");int waste=0;
            for(var c:ledger.columns)if(c.waste()) {
                h.assertTrue(h.getLevel().getBlockEntity(c.bottom) instanceof ReactorCoriumBlockEntity,"列底没有残骸");
                var corium=(ReactorCoriumBlockEntity)h.getLevel().getBlockEntity(c.bottom);waste+=corium.payload().getCount();
            }
            h.assertTrue(waste==2&&ledger.columns.stream().allMatch(c->c.payload().isEmpty()),"托管/残骸载荷不守恒");
            var freshColumn=ledger.columns.stream().filter(c->!c.waste()).findFirst().orElseThrow();
            var dropId=UUID.nameUUIDFromBytes((ledger.id+":"+freshColumn.port.asLong()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            h.assertTrue(h.getLevel().getEntity(dropId) instanceof ItemEntity item&&ItemStack.matches(item.getItem(),named),"全新原件未完整返还一次");
            h.assertTrue(owner.prepareDisassemblyPlan(owner.getBlockPos(),owner.getBlockState())==null,"已结算残余仪表仍永久拒拆");
            System.out.println("[accident] physical used/fresh/spent/empty columns conserve 2 waste + 1 exact return; settled debris unlocked");h.succeed();
        });
    }
    @GameTest(template=TEMPLATE,timeoutTicks=230)
    public static void ledgerResumesWithoutInstrumentAndWithoutChunkLoads(GameTestHelper h) {
        if(resumeOnly(h))return;
        var owner=build(h,BASE,16);
        for(var port:owner.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING))port.setFuelAssembly(used());
        var ledger=trigger(h,owner,ReactorMeltdownEvent.Reason.DANGEROUS_DISASSEMBLY);
        var partial=ReactorAccidentSavedData.get(h.getLevel()).save(new CompoundTag(),h.getLevel().registryAccess());
        var restored=ReactorAccidentSavedData.load(partial,h.getLevel().registryAccess()).byId(ledger.id);
        h.assertTrue(restored.columns.size()==196&&!restored.complete(),"16³托管快照没有保存196份组件");
        h.setBlock(BASE.offset(1,1,0),Blocks.AIR);
        long[] forced=h.getLevel().getForcedChunks().toLongArray();
        h.runAfterDelay(120,()->{
            h.assertTrue(ledger.complete(),"仪表先毁后账本不能独立完成");
            h.assertTrue(ledger.columns.stream().allMatch(ReactorAccidentLedger.Column::settled),"16³有丢料/未结算列");
            h.assertTrue(ledger.externalBroken<=32&&ledger.peakActions<=64,"16³破坏/预算越界");
            h.assertTrue(java.util.Arrays.equals(forced,h.getLevel().getForcedChunks().toLongArray()),"事故强载区块");
            System.out.println("[accident] 16cube plan="+ledger.actions.size()+" payloads="+ledger.columns.size()+" actions="+ledger.worldActions+" external="+ledger.externalBroken+" peak="+ledger.peakActions);h.succeed();
        });
    }
    @GameTest(template=TEMPLATE,timeoutTicks=150)
    public static void sharedBudgetFrozenRulesAndBoundedDamage(GameTestHelper h) {
        if(resumeOnly(h))return;
        var a=build(h,BASE,5);var b=build(h,BASE.offset(12,0,0),5);port(h,BASE,5,0,0).setFuelAssembly(used());port(h,BASE.offset(12,0,0),5,0,0).setFuelAssembly(used());
        var la=trigger(h,a,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);var lb=trigger(h,b,ReactorMeltdownEvent.Reason.DANGEROUS_DISASSEMBLY);
        int one=ReactorAccidentExecutor.tick(h.getLevel()),two=ReactorAccidentExecutor.tick(h.getLevel());
        h.assertTrue(one+two<=64,"同维度多事故或同tick重入把预算相乘");
        h.assertTrue(la.parameters.equals(AccidentParameters.DEFAULT)&&lb.parameters.equals(AccidentParameters.DEFAULT),"事故没有冻结参数");
        h.runAfterDelay(30,()->{h.assertTrue(la.complete()&&lb.complete()&&la.externalBroken<=32&&lb.externalBroken<=32,"多事故未公平完成或外部超限");h.succeed();});
    }
    @GameTest(template=TEMPLATE,timeoutTicks=120)
    public static void nativeFinalCancellationAndMultiOwnerFailureDoNotEnqueue(GameTestHelper h) {
        if(resumeOnly(h))return;
        var a=build(h,BASE,5);var b=build(h,BASE.offset(4,0,0),5);
        ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),a.getBlockPos());
        var pa=port(h,BASE,5,0,0);var pb=port(h,BASE.offset(4,0,0),5,0,0);pa.setFuelAssembly(used());pb.setFuelAssembly(used());
        var broken=h.absolutePos(BASE.offset(4,2,2));var state=h.getLevel().getBlockState(broken);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AccidentBreak"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        int before=ReactorAccidentSavedData.get(h.getLevel()).records().size();
        java.util.function.Consumer<BlockEvent.BreakEvent> cancel=e->{if(e.getPos().equals(broken))e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,false,cancel);
        try {
            var event=net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(),GameType.SURVIVAL,player,broken,state);
            h.assertTrue(event.isCanceled()&&a.accidentId()==null&&b.accidentId()==null,"同LOWEST晚取消仍接管事故");
            h.assertTrue(ReactorAccidentSavedData.get(h.getLevel()).records().size()==before&&!pa.fuelAssembly().isEmpty()&&!pb.fuelAssembly().isEmpty(),"取消事件遗留任务/吞料");
        } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        java.util.function.Consumer<ReactorMeltdownEvent> failure=e->{if(e.instrumentPort().equals(b.getBlockPos()))throw new IllegalStateException("本批验证第二所有者监听失败");};
        NeoForge.EVENT_BUS.addListener(failure);
        try {
            var event=net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(),GameType.SURVIVAL,player,broken,state);
            h.assertTrue(event.isCanceled()&&ReactorAccidentSavedData.get(h.getLevel()).records().size()==before,"多所有者失败留下孤立事故");
            h.assertTrue(!pa.fuelAssembly().isEmpty()&&!pb.fuelAssembly().isEmpty()&&a.accidentId()==null&&b.accidentId()==null,"失败事务没有保留真实物料");
        } finally { NeoForge.EVENT_BUS.unregister(failure); }
        var success=net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(),GameType.SURVIVAL,player,broken,state);
        h.assertTrue(!success.isCanceled()&&a.accidentId()!=null&&b.accidentId()!=null,"原生危险拆除没有原子接管两个所有者");
        h.assertTrue(pa.fuelAssembly().isEmpty()&&pb.fuelAssembly().isEmpty(),"危险拆除同时保留原组件");
        System.out.println("[accident] native post final LOWEST cancellation, second-owner failure rollback, and successful dangerous handoff passed");h.succeed();
    }
    /** 夹具只解除FakePlayer固定免伤和出生保护，不建立真人连接，不称多人实测。 */
    private static final class ShockPlayer extends net.neoforged.neoforge.common.util.FakePlayer {
        ShockPlayer(net.minecraft.server.level.ServerLevel level) {
            super(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AccidentShock"));
            try {var f=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");f.setAccessible(true);f.setInt(this,0);}
            catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
    }
    @GameTest(template=TEMPLATE,timeoutTicks=130)
    public static void shockIsOnceAndDisabledDestructionFreezesAllRules(GameTestHelper h) {
        if(resumeOnly(h))return;
        var owner=build(h,BASE,5);port(h,BASE,5,0,0).setFuelAssembly(used());
        var player=new ShockPlayer(h.getLevel());player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(h.absolutePos(BASE.offset(2,2,2))));player.setHealth(20);
        h.getLevel().players().add(player);
        var l=trigger(h,owner,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);
        h.runAfterDelay(3,()->{
            try {
                h.assertTrue(player.getHealth()==12&&l.shocked.contains(player.getUUID()),"近距冲击未限8HP并记玩家收据：health="+player.getHealth()+" receipt="+l.shocked.size());
                ReactorAccidentExecutor.tick(h.getLevel());h.assertTrue(player.getHealth()==12,"同事故分批/重入重复伤害");
                var loaded=ReactorAccidentLedger.load(l.save(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(loaded.shocked.contains(player.getUUID()),"冲击收据没有保存");
            } finally {h.getLevel().players().remove(player);}
        });
        var nextBase=BASE.offset(16,0,0);var next=build(h,nextBase,5);port(h,nextBase,5,0,0).setFuelAssembly(used());next.setCoolantInventories(111,222);
        ReactorAccidentLedger disabled;
        try {
            var field=AccidentConfig.class.getDeclaredField("current");field.setAccessible(true);var original=field.get(null);
            try {field.set(null,new AccidentParameters(false,2,32,8,64));disabled=trigger(h,next,ReactorMeltdownEvent.Reason.DANGEROUS_DISASSEMBLY);}
            finally {field.set(null,original);}
        } catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        h.assertTrue(!disabled.parameters.destruction()&&disabled.actions.stream().noneMatch(a->a.external),"关闭破坏仍排壳/外部动作");
        h.runAfterDelay(30,()->{h.assertTrue(disabled.complete()&&disabled.externalBroken==0&&disabled.shocked.isEmpty()&&next.snapshot().coldCoolantMb()==0,"关闭外部破坏同时取消了内部报废/损失或仍有冲击");h.succeed();});
    }
    @GameTest(template=TEMPLATE,timeoutTicks=200)
    public static void actualKineticsPreserveWasteSpentAndManufactureRecipes(GameTestHelper h) {
        if(resumeOnly(h))return;
        var machines=new ArrayList<ShieldedAssemblyBlockEntity>();
        for(int mode=0;mode<3;mode++) {
            var pos=new BlockPos(12+mode*10,3,28);var block=FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get();
            h.setBlock(pos,block.defaultBlockState().setValue(ShieldedAssemblyBlock.EXPANDED,true));block.setPlacedBy(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),null,ItemStack.EMPTY);
            var machine=(ShieldedAssemblyBlockEntity)h.getBlockEntity(pos);machines.add(machine);
            if(mode<2) {
                h.assertTrue(machine.insert(new ItemStack(mode==0?AccidentContent.ACCIDENT_FUEL_WASTE.get():ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get())).isEmpty(),"真实机器拒收封装载荷");
                machine.insert(new ItemStack(SpentFuelStorageContent.VITRIFICATION_MEDIUM.get()));machine.insert(new ItemStack(SpentFuelStorageContent.LEAD_SHIELDING_CASK.get()));
            } else {
                machine.insert(new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(),8));machine.insert(new ItemStack(BasicMaterialContent.FUEL_CLADDING_TUBE.get(),4));
                machine.insert(new ItemStack(BasicMaterialContent.SOLDER_INGOT.get(),2));machine.insert(new ItemStack(BasicMaterialContent.STEEL_GRATE.get()));
            }
            h.setBlock(pos.below(),com.simibubi.create.AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(com.simibubi.create.content.kinetics.motor.CreativeMotorBlock.FACING,Direction.UP));
            ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)h.getBlockEntity(pos.below())).generatedSpeed.setValue(256);
        }
        h.runAfterDelay(25,()->{
            var m=machines.getFirst();int progress=m.state().progress();h.assertTrue(progress>0&&progress<12800,"真实Create动力没有推进新封装");
            var saved=m.savePortableData();m.loadPortableData(saved);h.assertTrue(m.recipeReady()&&m.state().progress()==progress,"新封装中途恢复串配方");
            m.updateFromNetwork(0,2048,2);m.tick();h.assertTrue(m.state().progress()==progress,"停机/过载仍计新封装工时");m.updateFromNetwork(100000,1024,2);
        });
        h.runAfterDelay(140,()->{
            for(int i=0;i<2;i++)h.assertTrue(SpentFuelPayload.isValid(machines.get(i).state().output()),"新/原封装真实动力没有完成");
            h.assertTrue(machines.get(2).state().output().is(ModItems.FRESH_FUEL_ASSEMBLY.get())&&machines.get(2).state().output().getDamageValue()==0,"原制造真实动力失效");
            for(var m:machines)for(int i=0;i<4;i++)h.assertTrue(m.state().input(i).isEmpty(),"真实完工未原子扣料");
            System.out.println("[accident] actual Create motor: waste/spent 12800 and manufacture 25600, pause/current-format resume passed");h.succeed();
        });
    }
    @GameTest(template=TEMPLATE,timeoutTicks=200)
    public static void wasteSealingAndSourceOwnershipStayAtomic(GameTestHelper h) {
        if(skipFixture(h,"ownership"))return;
        var pos=new BlockPos(28,3,8);var block=FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get();
        h.setBlock(pos,block.defaultBlockState().setValue(ShieldedAssemblyBlock.EXPANDED,true));block.setPlacedBy(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),null,ItemStack.EMPTY);
        var machine=(ShieldedAssemblyBlockEntity)h.getBlockEntity(pos);h.assertTrue(machine.complete(),"真实八格装配台未成型");
        var coriumPos=new BlockPos(24,3,8);h.setBlock(coriumPos,AccidentContent.REACTOR_CORIUM.get());var corium=(ReactorCoriumBlockEntity)h.getBlockEntity(coriumPos);
        var waste=new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get());h.assertTrue(corium.accept(UUID.randomUUID(),h.absolutePos(coriumPos),waste),"真实残骸拒绝单件载荷");
        double[] source={0};RadiationSourceAdapters.read(corium,s->source[0]+=RadiationItemSources.describe(s.stack(),RadiationParameters.DEFAULT).dosePerSecond());h.assertTrue(source[0]==4,"残骸真实源不是4");
        h.getLevel().destroyBlock(h.absolutePos(coriumPos),true);var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(coriumPos)).inflate(1));
        var drop=drops.stream().filter(e->e.getItem().is(AccidentContent.ACCIDENT_FUEL_WASTE.get())).findFirst().orElseThrow();var actual=drop.getItem().copy();drop.discard();
        var protection=new ItemEntity(h.getLevel(),h.absolutePos(coriumPos).getX(),h.absolutePos(coriumPos).getY(),h.absolutePos(coriumPos).getZ(),actual.copy());h.getLevel().addFreshEntity(protection);
        for(var damage:List.of(h.getLevel().damageSources().inFire(),h.getLevel().damageSources().lava(),h.getLevel().damageSources().cactus(),h.getLevel().damageSources().explosion(null,null)))
            h.assertTrue(!protection.hurt(damage,100)&&protection.isAlive(),"事故废料被原版伤害删除");
        h.assertTrue(protection.getAge()==-32768,"事故废料没有无限寿命保护");protection.discard();
        var input=machine.itemPort(h.absolutePos(pos.above()),Direction.UP);h.assertTrue(input!=null&&input.insertItem(0,actual,false).isEmpty(),"装配台拒绝真实废料");
        h.assertTrue(input.insertItem(1,new ItemStack(SpentFuelStorageContent.VITRIFICATION_MEDIUM.get()),false).isEmpty(),"固化材料拒绝");
        h.assertTrue(input.insertItem(2,new ItemStack(SpentFuelStorageContent.LEAD_SHIELDING_CASK.get()),false).isEmpty(),"空桶拒绝");
        h.assertTrue(machine.recipeReady()&&machine.state().work()==12800,"新封装没有12800工时");
        var recipe=(ShieldedAssemblyRecipe)h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("create_nuclear_industry:shielded_assembly/sealed_accident_fuel_waste")).orElseThrow().value();
        for(int i=0;i<50;i++)machine.state().advance(128,recipe.actualResult(machine.state().input(0)));
        var checkpoint=machine.savePortableData();machine.loadPortableData(checkpoint);h.assertTrue(machine.recipeReady()&&machine.state().progress()==6400,"封装中途正常格式丢进度");
        for(int i=0;i<50;i++)machine.state().advance(128,recipe.actualResult(machine.state().input(0)));
        var sealed=machine.state().extractOutput(1,false);h.assertTrue(SpentFuelPayload.isValid(sealed),"封装未产合法单件桶");
        h.assertTrue(Math.abs(RadiationItemSources.describe(sealed,RadiationParameters.DEFAULT).dosePerSecond()-.2)<1e-12,"桶重复/漏屏蔽");
        var rackPos=pos.offset(6,0,0);h.setBlock(rackPos,SpentFuelStorageContent.DRY_STORAGE_RACK.get());var rack=(DryStorageBlockEntity)h.getBlockEntity(rackPos);h.assertTrue(rack.insertOne(sealed),"架拒收新废料桶");
        source[0]=0;RadiationSourceAdapters.read(rack,s->source[0]+=RadiationItemSources.describe(s.stack(),RadiationParameters.DEFAULT).dosePerSecond()*(s.rackShielded()?0:1));h.assertTrue(source[0]==0,"完整架不屏蔽废料");
        h.assertTrue(SpentFuelPayload.isValid(SpentFuelPayload.seal(new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()))),"旧乏燃料封装失效");
        System.out.println("[accident] corium mine -> assembly 12800/save-resume -> cask .2 -> rack 0; original payload still valid");h.succeed();
    }
    @GameTest(template=TEMPLATE,timeoutTicks=130)
    public static void adjacentReactorKeepsItsOwnFuelAndReceivesStructureInvalidation(GameTestHelper h) {
        if(skipFixture(h,"ownership"))return;
        var a=build(h,BASE,5);var b=build(h,BASE.offset(4,0,0),5);
        ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),a.getBlockPos());
        port(h,BASE,5,0,0).setFuelAssembly(used());var otherPort=port(h,BASE.offset(4,0,0),5,0,0);var original=used();
        original.set(DataComponents.CUSTOM_NAME,Component.literal("邻堆唯一原件"));otherPort.setFuelAssembly(original);
        var ledger=trigger(h,a,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);
        var shared=h.absolutePos(BASE.offset(4,2,2));
        h.assertTrue(ledger.actions.stream().anyMatch(action->action.pos.equals(shared)&&action.external),"共享壳没有进入本次排定目标，不能要求邻堆损坏");
        h.assertTrue(ledger.knownOwners.contains(b.accidentGeneration()),"创建时邻堆世代没有冻结");
        h.assertTrue(ReactorAccidentLedger.load(ledger.save(h.getLevel().registryAccess()),h.getLevel().registryAccess()).knownOwners.contains(b.accidentGeneration()),"已存在邻堆世代保存丢失");
        var newBase=BASE.offset(18,0,0);var c=build(h,newBase,5);port(h,newBase,5,0,0).setFuelAssembly(used());
        var beforeNew=trigger(h,c,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);var d=build(h,newBase.offset(4,0,0),5);
        var newShared=h.absolutePos(newBase.offset(4,2,2));
        h.assertTrue(!beforeNew.knownOwners.contains(d.accidentGeneration()),"后建机组错误写入冻结邻堆名单");
        h.runAfterDelay(30,()->{
            h.assertTrue(b.accidentId()==null&&ReactorAccidentSavedData.get(h.getLevel()).byGeneration(b.accidentGeneration())==null,"邻堆被直接设为事故");
            h.assertTrue(!b.structureValid(),"flags18后没有向邻堆失效实际破损结构");
            h.assertTrue(h.getLevel().getBlockState(shared).isAir(),"排定共享壳没有实际删除");
            var retained=otherPort.fuelAssembly();
            // 共享壳真正破裂前邻堆可正常燃耗；只核对真实单件身份/原件标记，不把合法耐久变化当吞料。
            h.assertTrue(!retained.isEmpty()&&retained.getCount()==1&&retained.is(ModItems.FRESH_FUEL_ASSEMBLY.get())
                    &&original.get(DataComponents.CUSTOM_NAME).equals(retained.get(DataComponents.CUSTOM_NAME)),"本事故吞掉邻堆真实组件");
            h.assertTrue(RadiationItemSources.describe(otherPort.fuelAssembly(),RadiationParameters.DEFAULT).dosePerSecond()==1,"邻堆源被变成事故废料或漏掉");
            h.assertTrue(d.structureValid()&&d.accidentId()==null&&h.getLevel().getBlockState(newShared).is(P1Blocks.REACTOR_CASING.get()),"旧任务按位置破坏事故后新建机组");
            System.out.println("[accident] adjacent reactor own fuel preserved, shared wall damage invalidates structure without forcing meltdown");h.succeed();
        });
    }
    @GameTest(template=TEMPLATE,batch="accident_persistence",timeoutTicks=180)
    public static void normalStopPendingCustodyResumesFromSavedWorld(GameTestHelper h) {
        if("molten_corium".equals(System.getenv("CNI_ACCIDENT_FOCUS"))){moltenNativeResume(h);return;}
        if(System.getenv("CNI_ACCIDENT_FOCUS")!=null){System.out.println("[accident] focused validation: SKIP persistence fixture");h.succeed();return;}
        var data=ReactorAccidentSavedData.get(h.getLevel());
        var prior=data.records().stream().filter(l->l.coldMb==77711&&l.hotMb==1234&&!l.complete()).findFirst();
        if(prior.isPresent()) {
            var ledger=prior.get();var column=ledger.columns.getFirst();
            h.assertTrue(column.captured()&&!column.settled()&&column.payload().getCount()==1,"正常停服后托管丢失或错误结算");
            // 测试控制端模拟玩家重新到场，显式载入自己的独立测试区块；生产执行器不持有票据。
            h.getLevel().getChunkSource().getChunk(column.bottom.getX()>>4,column.bottom.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
            h.assertTrue(h.getLevel().getBlockState(column.bottom).is(Blocks.BEDROCK),"原生区块没有保存阻塞目标");
            var before=RadiationSourceIndex.get(h.getLevel()).sampleAt(h.getLevel(),Vec3.atCenterOf(column.bottom),RadiationParameters.DEFAULT);
            h.assertTrue(before.complete()&&before.fieldDosePerSecond()==4,"原生重进后托管源漏计");
            h.getLevel().setBlock(column.bottom,Blocks.AIR.defaultBlockState(),3);
            h.runAfterDelay(8,()->{
                h.assertTrue(ledger.complete()&&h.getLevel().getBlockEntity(column.bottom) instanceof ReactorCoriumBlockEntity,"原生重进后未续接残骸");
                var after=RadiationSourceIndex.get(h.getLevel()).sampleAt(h.getLevel(),Vec3.atCenterOf(column.bottom),RadiationParameters.DEFAULT);
                h.assertTrue(after.complete()&&after.fieldDosePerSecond()==4,"托管移交残骸源强不连续");
                System.out.println("[accident] NATIVE_RESTART_RESUME_OK id="+ledger.id+" before=4 after=4 one payload");h.succeed();
            });return;
        }
        h.assertTrue(!"1".equals(System.getenv("CNI_ACCIDENT_RESUME_ONLY")),"续接专轮没有读到前次正常停服的待办账本");
        var owner=build(h,BASE,5);port(h,BASE,5,0,0).setFuelAssembly(used());owner.setCoolantInventories(77711,1234);
        h.setBlock(BASE.offset(1,1,1),Blocks.BEDROCK);
        var ledger=trigger(h,owner,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);var column=ledger.columns.getFirst();
        h.runAfterDelay(30,()->{
            h.assertTrue(!ledger.complete()&&column.captured()&&!column.settled()&&column.payload().getCount()==1,"异常目标吞掉真实托管");
            h.assertTrue(h.getLevel().getBlockState(column.bottom).is(Blocks.BEDROCK),"事故修改不可破坏目标");
            System.out.println("[accident] NATIVE_SAVE_PENDING id="+ledger.id+" payload=1 blocked=bedrock");h.succeed();
        });
    }
    /** 原持久化批次在正常退出后原目录重进一次；审计source没有ledger，本体仍续时且只持一件。 */
    private static void moltenNativeResume(GameTestHelper h){
        var level=h.getLevel();var data=ReactorAccidentSavedData.get(level);
        var marker=UUID.nameUUIDFromBytes("cni:04b_native_resume".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var prior=data.moltenWork().stream().filter(s->marker.equals(s.source)).findFirst();
        if(prior.isPresent()){
            var state=prior.get();int remaining=state.remaining;
            h.assertTrue(data.byId(marker)==null&&state.phase==MoltenCoriumState.Phase.MOLTEN&&state.payload().getCount()==1&&remaining>4100&&remaining<=4242
                    &&state.moveProgress==7&&state.corrosionProgress==11,"原生正常重进本体状态/位置/进度/唯一载荷未保留");
            level.getChunkSource().getChunk(state.pos.getX()>>4,state.pos.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
            h.assertTrue(level.getBlockState(state.pos).is(AccidentContent.MOLTEN_CORIUM.get())&&level.getBlockState(state.pos.below()).is(Blocks.BEDROCK),"重进丢失实际液格/基岩");
            h.runAfterDelay(10,()->{
                h.assertTrue(state.remaining<remaining&&state.remaining>=remaining-12&&state.payload().getCount()==1&&state.phase==MoltenCoriumState.Phase.MOLTEN,"重进不续时或重复交出物料");
                System.out.println("[molten04b] NATIVE_RESTART_RESUME_OK id="+state.id+" remaining="+remaining+" -> "+state.remaining+" payload=1 no ledger at "+state.pos);
                command(h,"setblock "+coordinates(state.pos)+" minecraft:air");h.succeed();
            });return;
        }
        h.assertTrue(!"1".equals(System.getenv("CNI_ACCIDENT_RESUME_ONLY")),"原生重进专轮没有读到前次待办本体");
        var pos=h.absolutePos(new BlockPos(256,90,256));level.getChunkSource().getChunk(pos.getX()>>4,pos.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
        level.setBlock(pos.below(),Blocks.BEDROCK.defaultBlockState(),18);
        var state=new MoltenCoriumState(UUID.randomUUID(),marker,pos,new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()),MoltenCoriumParameters.DEFAULT);
        state.remaining=4242;state.moveProgress=7;state.corrosionProgress=11;data.addMolten(state);level.setBlock(pos,AccidentContent.MOLTEN_CORIUM.get().defaultBlockState(),18);
        // 记录在本批最后一个原生批次创建，立即成功并正常保存；离场后的生产推进仍以真实加载为准。
        System.out.println("[molten04b] NATIVE_SAVE_PENDING id="+state.id+" remaining="+state.remaining+" payload=1 no ledger at "+state.pos);h.succeed();
    }
    private static void awaitNativeUnload(GameTestHelper h,java.util.concurrent.atomic.AtomicInteger count,int attempts,Runnable ready) {
        h.getLevel().getChunkSource().tick(()->true,false);
        if(count.get()>0){ready.run();return;}
        if(attempts<=0){System.out.println("[accident] physical ChunkEvent.Unload NOT observed; this fixture proves actual FULL unavailability only");ready.run();return;}
        h.runAfterDelay(20,()->awaitNativeUnload(h,count,attempts-1,ready));
    }
    @GameTest(template=TEMPLATE,batch="accident_chunk",timeoutTicks=2600)
    public static void realFullChunkUnavailabilityRetainsCustodyThenResumes(GameTestHelper h) {
        if(skipFixture(h,"chunks"))return;
        var anchor=h.absolutePos(BlockPos.ZERO);
        var base=new BlockPos(24-(anchor.getX()&15),2,24-(anchor.getZ()&15));
        var owner=build(h,base,16);for(var p:owner.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING))p.setFuelAssembly(used());
        var ledger=trigger(h,owner,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);
        var ownerChunk=new net.minecraft.world.level.ChunkPos(owner.getBlockPos());var far=h.absolutePos(base.offset(14,1,14));
        var ticket=net.minecraft.server.level.TicketType.<net.minecraft.world.level.ChunkPos>create("cni_accident_probe",Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
        h.getLevel().getChunkSource().addRegionTicket(ticket,ownerChunk,0,ownerChunk);
        var unloaded=new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<net.neoforged.neoforge.event.level.ChunkEvent.Unload> listener=e->{
            if(e.getLevel()==h.getLevel()&&e.getChunk().getPos().x==(far.getX()>>4)&&e.getChunk().getPos().z==(far.getZ()>>4))unloaded.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(listener);
        for(long packed:h.getLevel().getForcedChunks().toLongArray()){var c=new net.minecraft.world.level.ChunkPos(packed);h.getLevel().setChunkForced(c.x,c.z,false);}
        for(int i=0;i<3;i++)h.getLevel().getChunkSource().tick(()->true,false);
        h.assertTrue(!h.getLevel().hasChunkAt(far),"真实远端FULL没有撤销，不能冒称卸载场景");
        h.runAfterDelay(355,()->awaitNativeUnload(h,unloaded,10,()->{
            try {
                h.assertTrue(!ledger.complete()&&ledger.columns.stream().anyMatch(c->!c.settled()),"不可用远端被跳过或吞掉");
                h.assertTrue(!h.getLevel().hasChunkAt(far),"生产事故执行器强载远端");
                var min=h.absolutePos(base);for(int x=min.getX()>>4;x<=(min.getX()+15)>>4;x++)for(int z=min.getZ()>>4;z<=(min.getZ()+15)>>4;z++)h.getLevel().setChunkForced(x,z,true);
                h.runAfterDelay(130,()->{
                    h.assertTrue(ledger.complete()&&ledger.columns.stream().allMatch(ReactorAccidentLedger.Column::settled),"真实卸载重载后事故不能续接");
                    h.getLevel().getChunkSource().removeRegionTicket(ticket,ownerChunk,0,ownerChunk);
                    System.out.println("[accident] FULL_CHUNK_AVAILABILITY_RESUME_OK physicalUnloadEvents="+unloaded.get()+" payloads=196 no executor chunk ticket");h.succeed();
                });
            } finally {NeoForge.EVENT_BUS.unregister(listener);}
        }));
    }
}
