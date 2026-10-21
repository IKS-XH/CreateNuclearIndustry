package com.iksxh.create_nuclear_industry.accident;

import com.iksxh.create_nuclear_industry.content.AccidentContent;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;

/** 只推进已加载热引用；逻辑计时先行，世界投影由Executor逐项领取同维度预算。 */
public final class AccidentThermalEffects {
    private AccidentThermalEffects(){}
    public static boolean loaded(ServerLevel level,BlockPos pos) {
        return level.hasChunkAt(pos)&&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    private static boolean water(ServerLevel level,BlockPos pos) {
        for(var direction:Direction.values()){var next=pos.relative(direction);if(loaded(level,next)&&level.getFluidState(next).is(FluidTags.WATER))return true;}
        return false;
    }
    public static void advance(ServerLevel level,ReactorAccidentSavedData data) {
        for(var ledger:data.thermalWork()) {
            var thermal=ledger.thermal;boolean changed=false;
            for(var s:thermal.activeSolids()) {
                if(!loaded(level,s.pos))continue;
                if(!(level.getBlockEntity(s.pos) instanceof ReactorCoriumBlockEntity be)||!be.matches(ledger.id,s.pos)||be.payload().isEmpty())thermal.endSolid(s);
                else thermal.advanceSolid(s,level.getGameTime(),true,water(level,s.pos));changed=true;
            }
            for(var f:thermal.activeFires()) {
                if(!f.attempted||!loaded(level,f.pos))continue;
                if(!(level.getBlockEntity(f.pos) instanceof AccidentFireBlock.Lease lease)||!lease.matches(ledger.id,f.id)) {
                    thermal.endFire(f);thermal.cleaned(f);
                } else if(water(level,f.pos))thermal.endFire(f);
                else thermal.advanceFire(f,level.getGameTime(),true);
                changed=true;
            }
            data.refreshWork(ledger);if(changed)data.setDirty();
        }
    }
    /** 返回至多一次真实世界写；到期先无危险，即使本tick预算耗尽也不延长伤害。 */
    public static boolean one(ServerLevel level,ReactorAccidentLedger ledger) {
        var thermal=ledger.thermal;
        for(var s:thermal.activeSolids()) {
            if(!loaded(level,s.pos))continue;
            if(!(level.getBlockEntity(s.pos) instanceof ReactorCoriumBlockEntity be)||!be.matches(ledger.id,s.pos)||be.payload().isEmpty()) {thermal.endSolid(s);continue;}
            var state=level.getBlockState(s.pos);boolean hot=s.remaining>0;
            if(state.getValue(ReactorCoriumBlock.HOT)!=hot) {
                if(level.setBlock(s.pos,state.setValue(ReactorCoriumBlock.HOT,hot),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE)) {
                    if(!hot)thermal.endSolid(s);return true;
                }
            }else if(!hot)thermal.endSolid(s);
        }
        // 不让新增火点阻挡或延长原complete收据，也不在原破裂动作完成前点火。
        if(!ledger.complete())return false;
        for(var f:thermal.activeFires()) {
            if(!loaded(level,f.pos))continue;
            if(!f.attempted) {
                var below=f.pos.below();if(!loaded(level,below))continue;
                f.attempted=true;
                if(!level.getBlockState(f.pos).isAir()||ReactorAccidentExecutor.newOwnerAt(level,ledger,f.pos)
                        ||!level.getBlockState(below).isFaceSturdy(level,below,Direction.UP)) {
                    thermal.endFire(f);thermal.cleaned(f);continue;
                }
                if(level.setBlock(f.pos,AccidentContent.ACCIDENT_FIRE.get().defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE)) {
                    if(!(level.getBlockEntity(f.pos) instanceof AccidentFireBlock.Lease lease))throw new IllegalStateException("火点身份实体未创建");
                    lease.bind(ledger.id,f.id);f.placed=true;f.lastTick=level.getGameTime();return true;
                }
                thermal.endFire(f);thermal.cleaned(f);
            } else if(f.terminal) {
                if(level.getBlockEntity(f.pos) instanceof AccidentFireBlock.Lease lease&&lease.matches(ledger.id,f.id)) {
                    if(level.setBlock(f.pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE)){thermal.cleaned(f);return true;}
                }else thermal.cleaned(f);
            }
        }
        return false;
    }
    /** 接触不信任HOT外观，必须同时有本事故热引用与真实单件废料；同实体同类20tick最多一次。 */
    public static void contactSolid(Level level,BlockPos pos,Entity entity) {
        if(!(level instanceof ServerLevel server)||entity.fireImmune()||!loaded(server,pos))return;
        if(!(level.getBlockEntity(pos) instanceof ReactorCoriumBlockEntity be)||be.payload().isEmpty()||be.accident()==null)return;
        var ledger=ReactorAccidentSavedData.get(server).byId(be.accident());var solid=ledger==null?null:ledger.thermal.solid(pos);
        if(solid==null||solid.terminal||solid.remaining<=0)return;
        var tag=entity.getPersistentData();long now=level.getGameTime();
        if(tag.contains("CniSolidHeatNext")&&now<tag.getLong("CniSolidHeatNext"))return;
        var parameters=ledger.thermal.parameters;
        tag.putLong("CniSolidHeatNext",now+20);entity.hurt(level.damageSources().hotFloor(),(float)parameters.solidDamageHp());entity.igniteForSeconds(parameters.solidIgniteSeconds());
    }
}
