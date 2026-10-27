package com.iksxh.create_nuclear_industry.boiler;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.iksxh.create_nuclear_industry.accident.MoltenCoriumEffects;
import com.iksxh.create_nuclear_industry.config.BoilerConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 服务端事故提交边界；普通工况在Level Post运输之后判定，手拆只在最终许可后提交。 */
public final class BoilerAccidentLifecycle {
    private record Observation(WeakReference<BoilerControllerBlockEntity> owner,long tick){}
    private static final Map<ServerLevel,Map<BlockPos,Observation>> OBSERVED=new WeakHashMap<>();
    private BoilerAccidentLifecycle(){}
    public static void observe(BoilerControllerBlockEntity owner){
        if(owner.getLevel() instanceof ServerLevel level)OBSERVED.computeIfAbsent(level,k->new LinkedHashMap<>()).put(owner.getBlockPos(),new Observation(new WeakReference<>(owner),level.getGameTime()));
    }
    /** Server Post晚于工业主动出汽的Level Post；不能在实际送汽前按过早炉压引爆。 */
    public static void finishObserved(ServerLevel level){
        var pending=OBSERVED.remove(level);if(pending==null)return;
        for(var entry:pending.values()){
            var owner=entry.owner().get();if(owner==null||entry.tick()!=level.getGameTime()||!owner.current())continue;
            var form=owner.currentForm();if(form==null)continue;var p=BoilerConfig.accidentParameters();
            var cause=owner.accident().advance(level.getGameTime(),true,owner.ledger().pressure(),owner.dryFiring(),p);
            if(cause!=BoilerAccidentState.Cause.NONE)commit(level,owner,form,cause,p);
            owner.setChanged();
        }
    }
    /** 已有反应堆最终处理仍可能取消；仅最终未取消且原位置/状态仍一致时检查锅炉危险。 */
    public static void finishBlockBreak(BlockEvent.BreakEvent event){
        if(event.isCanceled()||!(event.getLevel() instanceof ServerLevel level)||MoltenCoriumEffects.environmental(event)
                ||!loaded(level,event.getPos())||level.getBlockState(event.getPos())!=event.getState())return;
        var owner=BoilerStructure.owner(level,event.getPos());if(owner==null)return;var form=owner.currentForm();if(form==null)return;
        var p=BoilerConfig.accidentParameters();var cause=owner.accident().dangerous(owner.ledger().pressure(),owner.dryFiring(),p);
        if(cause!=BoilerAccidentState.Cause.NONE)commit(level,owner,form,cause,p);
    }
    public static boolean loaded(ServerLevel level,BlockPos pos){return level.isInWorldBounds(pos)&&level.hasChunkAt(pos)&&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    /** 建完只读计划才交出原炉工质/能力；事故ID先入独立账本，重复入口返回同一收据。 */
    public static BoilerAccidentLedger commit(ServerLevel level,BoilerControllerBlockEntity owner,BoilerStructure.Form form,
            BoilerAccidentState.Cause cause,BoilerAccidentState.Parameters p){
        var data=BoilerAccidentSavedData.get(level);var old=data.activeGeneration(owner.boilerGeneration());if(old!=null)return old;
        if(form==null||!p.valid()||cause==BoilerAccidentState.Cause.NONE)return null;
        boolean burst=cause==BoilerAccidentState.Cause.BURST;Direction direction=Direction.values()[level.random.nextInt(6)];
        BlockPos origin=faceCenter(form,direction);double pressure=owner.ledger().pressure();var targets=new ArrayList<BoilerAccidentLedger.Target>();
        UUID id=UUID.randomUUID();
        if(burst){
            var shape=new BoilerAccidentLedger(id,owner.boilerGeneration(),owner.getBlockPos(),origin,direction,true,pressure,p,List.of());
            int radius=(int)Math.ceil(shape.radius);
            for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-radius,-radius,-radius),origin.offset(radius,radius,radius))){
                if(!loaded(level,pos)||!shape.inJet(Vec3.atCenterOf(pos)))continue;var state=level.getBlockState(pos);
                double distance=Vec3.atCenterOf(pos).distanceTo(Vec3.atCenterOf(origin));
                if(!state.isAir()&&state.getDestroySpeed(level,pos)>=0&&state.getBlock().getExplosionResistance()<=(shape.radius-distance+.5)*4)
                    targets.add(new BoilerAccidentLedger.Target(level,pos));
            }
            targets.sort(Comparator.comparingDouble(t->t.pos.distSqr(origin)));
        }else{
            var candidates=new ArrayList<BlockPos>();candidates.addAll(form.exchangers());candidates.addAll(form.sections());
            candidates.removeIf(pos->!loaded(level,pos)||!(level.getBlockState(pos).is(HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get())||level.getBlockState(pos).is(BoilerContent.HEAT_SECTION.get())));
            if(candidates.isEmpty())return null;targets.add(new BoilerAccidentLedger.Target(level,candidates.get(level.random.nextInt(candidates.size()))));
        }
        var plan=new BoilerAccidentLedger(id,owner.boilerGeneration(),owner.getBlockPos(),origin,direction,burst,pressure,p,targets);
        data.add(plan);owner.commitAccident(plan);
        data.setDirty();return plan;
    }
    public static BlockPos faceCenter(BoilerStructure.Form f,Direction d){
        int x=(f.min().getX()+f.max().getX())>>1,y=(f.min().getY()+f.max().getY())>>1,z=(f.min().getZ()+f.max().getZ())>>1;
        return new BlockPos(d==Direction.WEST?f.min().getX():d==Direction.EAST?f.max().getX():x,
                d==Direction.DOWN?f.min().getY():d==Direction.UP?f.max().getY():y,d==Direction.NORTH?f.min().getZ():d==Direction.SOUTH?f.max().getZ():z);
    }
    /** 干烧已经领取的唯一目标不能由紧随其后的玩家正常掉落再回收完整设备。 */
    public static boolean consumedComponent(Level level,BlockPos pos,BlockState state,net.minecraft.world.level.block.entity.BlockEntity original){
        if(!(level instanceof ServerLevel server))return false;
        for(var l:BoilerAccidentSavedData.get(server).active())if(!l.burst){
            if(loaded(server,l.owner)&&server.getBlockEntity(l.owner) instanceof BoilerControllerBlockEntity next&&!next.boilerGeneration().equals(l.generation))continue;
            for(var t:l.targets)if(t.pos.equals(pos)&&t.ownsDrop(state,original))return true;
        }
        return false;
    }
    /** 仅本机真实新放置撤销同址旧目标；属性更新/当前版本正常区块重载不经过这个分支。 */
    public static void componentPlaced(Level level,BlockPos pos,BlockState state,BlockState previous){
        if(!(level instanceof ServerLevel server)||previous.is(state.getBlock()))return;
        var data=BoilerAccidentSavedData.get(server);
        for(var l:data.active())for(var target:l.targets)if(target.pos.equals(pos)){target.replaced();data.setDirty();}
    }
    public static net.minecraft.world.entity.player.Player environmentActor(ServerLevel level){return net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
            new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes("cni:boiler_accident".getBytes(StandardCharsets.UTF_8)),"BoilerAccident"));}
}
