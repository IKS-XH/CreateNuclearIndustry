package com.iksxh.create_nuclear_industry.accident;

import java.util.*;
import java.util.function.Supplier;
import com.iksxh.create_nuclear_industry.blockentity.*;
import com.iksxh.create_nuclear_industry.config.AccidentConfig;
import com.iksxh.create_nuclear_industry.content.AccidentContent;
import com.iksxh.create_nuclear_industry.production.*;
import com.iksxh.create_nuclear_industry.radiation.*;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 独立世界本体调度及局部写许可；所有自动块写由LevelChunk守卫记账，原生命令另走权限作用域。 */
public final class MoltenCoriumEffects {
    private MoltenCoriumEffects(){}
    private static final class Scope {
        final ServerLevel level;final Map<BlockPos,BlockState> targets;final Player actor;int writes;
        Scope(ServerLevel level,Map<BlockPos,BlockState> targets,Player actor){this.level=level;this.targets=targets;this.actor=actor;}
    }
    private record CommandScope(ServerLevel level,BlockPos pos){}
    private static final ThreadLocal<Scope> AUTO=new ThreadLocal<>(),PROBE=new ThreadLocal<>();
    private static final ThreadLocal<CommandScope> COMMAND=new ThreadLocal<>();
    public static boolean environmental(BlockEvent.BreakEvent event){var s=PROBE.get();return s!=null&&s.level==event.getLevel()&&s.actor==event.getPlayer()&&s.targets.containsKey(event.getPos());}
    public static boolean environmentRemoval(Level level,BlockPos pos){var s=AUTO.get();return s!=null&&s.level==level&&s.targets.containsKey(pos);}
    public static boolean environmentalSetBlock(Level level,BlockPos pos,BlockState state,int ordinaryFlags){
        return level.setBlock(pos,state,environmentRemoval(level,pos)?Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE:ordinaryFlags);
    }
    /** 只包围已通过原版命令权限检查的一个目标写，不以flags或FakePlayer名字放行。 */
    public static <T> T command(ServerLevel level,BlockPos pos,boolean permitted,Supplier<T> operation){
        if(!permitted)return operation.get();var previous=COMMAND.get();COMMAND.set(new CommandScope(level,pos.immutable()));
        try{return operation.get();}finally{if(previous==null)COMMAND.remove();else COMMAND.set(previous);}
    }
    /** LevelChunk写前调用；拒绝未经本体授权的替换，不在onRemove之后补写或重新造料。 */
    public static boolean beforeWrite(Level level,BlockPos pos,BlockState old,BlockState next){
        if(!(level instanceof ServerLevel server)||old==next)return true;
        if(AUTO.get()==null&&COMMAND.get()==null&&!(old.getBlock() instanceof MoltenCoriumBlock))return true;
        var data=ReactorAccidentSavedData.get(server);var body=data.moltenAt(pos);var command=COMMAND.get();
        if(command!=null&&command.level==server&&command.pos.equals(pos)){
            if(body!=null&&body.phase==MoltenCoriumState.Phase.MOLTEN&&!next.is(AccidentContent.MOLTEN_CORIUM.get())){
                body.adminRemoved();data.reindexMolten(body,pos);changed(server,pos);
            }return true;
        }
        var scope=AUTO.get();
        if(scope!=null&&scope.level==server){
            if(scope.targets.get(pos)!=old||data.availableBudget(server.getGameTime(),64)==0)return false;
            data.spendBudget(1);data.automaticBlockWrites++;scope.writes++;return true;
        }
        return body==null||body.phase!=MoltenCoriumState.Phase.MOLTEN;
    }
    public static void appeared(ServerLevel level,BlockPos pos){
        var data=ReactorAccidentSavedData.get(level);var previous=data.moltenAt(pos);
        if(previous!=null&&previous.phase!=MoltenCoriumState.Phase.MOLTEN){
            // HOT方块已由原生onRemove交出其唯一库存；新液格不能继承旧热引用、物料或时钟。
            for(var solid:previous.heat().activeSolids())previous.heat().endSolid(solid);
            previous.phase=MoltenCoriumState.Phase.DONE;data.reindexMolten(previous,pos);
        }
        if(data.moltenAt(pos)==null)data.addMolten(new MoltenCoriumState(UUID.randomUUID(),null,pos,ItemStack.EMPTY,AccidentConfig.moltenParameters()));
        var state=data.moltenAt(pos);if(state.phase==MoltenCoriumState.Phase.MOLTEN){state.placed=true;state.lastTick=level.getGameTime();data.setDirty();}
    }
    private static boolean water(ServerLevel level,BlockPos pos){
        for(var d:Direction.values()){var p=pos.relative(d);if(AccidentThermalEffects.loaded(level,p)&&level.getFluidState(p).is(FluidTags.WATER))return true;}return false;
    }
    public static void advance(ServerLevel level,ReactorAccidentSavedData data){
        for(var state:data.moltenWork()){
            if(!AccidentThermalEffects.loaded(level,state.pos))continue;
            if(state.phase==MoltenCoriumState.Phase.MOLTEN){if(state.placed)state.advance(level.getGameTime(),true,water(level,state.pos));}
            else if(state.phase==MoltenCoriumState.Phase.HOT){
                var heat=state.heat();for(var s:heat.activeSolids()){
                    if(!(level.getBlockEntity(s.pos) instanceof ReactorCoriumBlockEntity be)||!be.matches(state.id,s.pos)||be.payload().isEmpty())heat.endSolid(s);
                    else heat.advanceSolid(s,level.getGameTime(),true,water(level,s.pos));
                }
                if(!heat.active()){state.phase=MoltenCoriumState.Phase.DONE;data.reindexMolten(state,state.pos);}
            }
            data.setDirty();
        }
    }
    private static int write(ServerLevel level,Map<BlockPos,BlockState> targets,Runnable operation){
        var data=ReactorAccidentSavedData.get(level);if(data.availableBudget(level.getGameTime(),64)<targets.size())return 0;
        var scope=new Scope(level,targets,null);if(AUTO.get()!=null)throw new IllegalStateException("熔融自动写作用域嵌套");AUTO.set(scope);
        try{operation.run();return scope.writes;}finally{AUTO.remove();}
    }
    private static Map<BlockPos,BlockState> targets(ServerLevel level,List<BlockPos> positions){
        var result=new LinkedHashMap<BlockPos,BlockState>();for(var p:positions){if(!AccidentThermalEffects.loaded(level,p))return null;result.put(p.immutable(),level.getBlockState(p));}return result;
    }
    public static int one(ServerLevel level,ReactorAccidentSavedData data,MoltenCoriumState state){
        if(!state.active()||data.moltenAt(state.pos)!=state||!AccidentThermalEffects.loaded(level,state.pos))return 0;
        if(state.phase==MoltenCoriumState.Phase.HOT){
            var s=state.heat().solid(state.pos);if(s==null||s.terminal||s.remaining>0)return 0;
            var target=targets(level,List.of(state.pos));return write(level,target,()->{
                var block=level.getBlockState(state.pos);if(block.is(AccidentContent.REACTOR_CORIUM.get()))level.setBlock(state.pos,block.setValue(ReactorCoriumBlock.HOT,false),18);
                state.heat().endSolid(s);state.phase=MoltenCoriumState.Phase.DONE;data.reindexMolten(state,state.pos);
            });
        }
        if(!state.placed){
            if(!level.getBlockState(state.pos).isAir())return 0;
            return write(level,targets(level,List.of(state.pos)),()->{level.setBlock(state.pos,AccidentContent.MOLTEN_CORIUM.get().defaultBlockState(),18);changed(level,state.pos);});
        }
        if(state.remaining==0)return solidify(level,data,state);
        var below=state.pos.below();if(!level.isInWorldBounds(below)||!AccidentThermalEffects.loaded(level,below))return 0;
        var block=level.getBlockState(below);
        if(block.isAir())return state.moveProgress>=state.parameters.moveTicks()?move(level,data,state,below):0;
        if(block.is(AccidentContent.MOLTEN_CORIUM.get())){
            if(state.moveProgress<state.parameters.moveTicks())return 0;
            for(var d:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
                var next=state.pos.relative(d);if(!state.visited(next)&&AccidentThermalEffects.loaded(level,next)&&level.getBlockState(next).isAir())return move(level,data,state,next);
            }return 0;
        }
        if(state.corrosionProgress<state.parameters.corrosionTicks()||block.getDestroySpeed(level,below)<0||!block.getFluidState().isEmpty())return 0;
        return corrode(level,data,state,below);
    }
    private static int move(ServerLevel level,ReactorAccidentSavedData data,MoltenCoriumState state,BlockPos next){
        // 空气也可能已有另一Column交出的未投影本体；冲突先等待，不先改位置、修订号或处理进度。
        if(data.moltenAt(state.pos)!=state||data.moltenAt(next)!=null)return 0;
        var old=state.pos;var plan=targets(level,List.of(old,next));
        return write(level,plan,()->{
            // 先改唯一位置索引，onPlace只绑定现有记录；两格写已共同预留，不产生零载荷影子本体。
            state.moved(next);data.reindexMolten(state,old);
            if(!level.setBlock(old,Blocks.AIR.defaultBlockState(),18)||!level.setBlock(next,AccidentContent.MOLTEN_CORIUM.get().defaultBlockState(),18))throw new IllegalStateException("熔融搬移投影失败，唯一记录保留");
            changed(level,old);changed(level,next);
        });
    }
    private static int solidify(ServerLevel level,ReactorAccidentSavedData data,MoltenCoriumState state){
        return write(level,targets(level,List.of(state.pos)),()->{
            if(level.getBlockEntity(state.pos) instanceof ReactorCoriumBlockEntity existing&&existing.matches(state.id,state.pos)){state.handedOff();return;}
            if(!level.getBlockState(state.pos).is(AccidentContent.MOLTEN_CORIUM.get()))return;
            var stack=state.payload();
            if(!level.setBlock(state.pos,AccidentContent.REACTOR_CORIUM.get().defaultBlockState().setValue(ReactorCoriumBlock.HOT,!stack.isEmpty()),18))return;
            if(!(level.getBlockEntity(state.pos) instanceof ReactorCoriumBlockEntity be)||!be.acceptMolten(state.id,state.pos,stack))throw new IllegalStateException("凝固载荷交接失败");
            state.handedOff();data.setDirty();changed(level,state.pos);
        });
    }
    private static int corrode(ServerLevel level,ReactorAccidentSavedData data,MoltenCoriumState body,BlockPos pos){
        var primary=level.getBlockState(pos);var positions=ShieldedAssemblyStructure.corrosionTargets(level,pos,primary);if(positions==null)return 0;
        var plan=targets(level,positions);if(plan==null||!removalReadsLoaded(level,plan)||data.availableBudget(level.getGameTime(),64)<plan.size())return 0;
        var entities=new HashMap<BlockPos,net.minecraft.world.level.block.entity.BlockEntity>();plan.keySet().forEach(p->entities.put(p,level.getBlockEntity(p)));
        var actor=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("cni:molten_corrosion".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"CoriumCorrosion"));
        var probe=new Scope(level,plan,actor);PROBE.set(probe);
        try{
            // 所有连带目标先取得最终许可；监听异常/末位取消不能提前移料或产生普通手拆事故。
            for(var entry:plan.entrySet())if(NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,entry.getKey(),entry.getValue(),actor)).isCanceled()){body.corroded();data.setDirty();return 0;}
        }catch(RuntimeException failure){body.corroded();data.setDirty();return 0;}finally{PROBE.remove();}
        body.corroded();data.setDirty();
        for(var entry:plan.entrySet())if(!AccidentThermalEffects.loaded(level,entry.getKey())||level.getBlockState(entry.getKey())!=entry.getValue()||level.getBlockEntity(entry.getKey())!=entities.get(entry.getKey()))return 0;
        if(!removalReadsLoaded(level,plan))return 0;
        return write(level,plan,()->{
            if(level.getBlockEntity(pos) instanceof ReactorPortBlockEntity port){var fuel=port.takeForCorrosion();if(!fuel.isEmpty())Block.popResource(level,pos,fuel);}
            Block.dropResources(primary,level,pos,entities.get(pos),actor,ItemStack.EMPTY);
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);for(var p:plan.keySet())changed(level,p);
        });
    }
    /** 原生容器onRemove的输出通知可越过导体读取第二邻格；flags18不关闭该回调，先保守核全部两格读取域。 */
    private static boolean removalReadsLoaded(ServerLevel level,Map<BlockPos,BlockState> targets){
        for(var pos:targets.keySet())for(var direction:Direction.values())for(int distance=1;distance<=2;distance++){
            var read=pos.relative(direction,distance);
            // 高度界外原生getBlockState直接返回VOID_AIR；水平界外不能交给原生getChunk隐式读取。
            if(level.isOutsideBuildHeight(read))continue;
            if(!level.isInWorldBounds(read)||!AccidentThermalEffects.loaded(level,read))return false;
        }
        return true;
    }
    private static void changed(ServerLevel level,BlockPos pos){ReactorStructureLifecycle.scheduleRescanAround(level,pos);RadiationContainment.changed(level,pos);RadiationSourceIndex.changed(level,pos);RadiationSourceIndex.custodyChanged(level,pos);}
    public static void contact(Level level,BlockPos pos,Entity entity){
        if(!(level instanceof ServerLevel server)||entity.fireImmune())return;var state=ReactorAccidentSavedData.get(server).moltenAt(pos);
        if(state==null||!state.dangerous())return;var tag=entity.getPersistentData();long now=level.getGameTime();
        if(tag.contains("CniMoltenHeatNext")&&now<tag.getLong("CniMoltenHeatNext"))return;tag.putLong("CniMoltenHeatNext",now+20);
        entity.hurt(level.damageSources().lava(),(float)state.parameters.damageHp());entity.igniteForSeconds(state.parameters.igniteSeconds());
    }
}
