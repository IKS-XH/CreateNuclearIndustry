package com.iksxh.create_nuclear_industry.accident;

import java.nio.charset.StandardCharsets;
import java.util.*;
import com.iksxh.create_nuclear_industry.blockentity.*;
import com.iksxh.create_nuclear_industry.content.AccidentContent;
import com.iksxh.create_nuclear_industry.radiation.*;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 服务端分批世界动作；每维度共享最多64项，轮转等待区块，不调用强载或原版爆炸。 */
public final class ReactorAccidentExecutor {
    private ReactorAccidentExecutor() {}
    @SubscribeEvent public static void onTick(ServerTickEvent.Post event) {
        for(var level:event.getServer().getAllLevels())tick(level);
    }
    /** 返回本tick实际执行/收据动作数；同维度各事故按自己的冻结上限共同限额。 */
    public static int tick(ServerLevel level) {
        var data=ReactorAccidentSavedData.get(level);var active=data.records().stream().filter(l->!l.complete()).toList();
        if(active.isEmpty())return 0;
        int budget=active.stream().mapToInt(l->l.parameters.worldActionsPerTick()).min().orElse(64);
        if(data.budgetTime!=level.getGameTime()){data.budgetTime=level.getGameTime();data.usedThisTick=0;}
        budget=Math.max(0,budget-data.usedThisTick);
        int used=0,idle=0;var counts=new HashMap<UUID,Integer>();long start=System.nanoTime();
        while(used<budget&&idle<active.size()) {
            var l=active.get(Math.floorMod(data.rotation++,active.size()));
            if(one(level,l)) { used++;idle=0;l.worldActions++;counts.merge(l.id,1,Integer::sum);data.setDirty(); }
            else idle++;
        }
        counts.forEach((id,count)->{var l=data.byId(id);l.peakActions=Math.max(l.peakActions,count);});
        data.usedThisTick+=used;
        if(used>0&&active.stream().anyMatch(ReactorAccidentLedger::complete))
            com.mojang.logging.LogUtils.getLogger().info("事故动作收据：维度={} 本tick={} 耗时微秒={} 任务={}",level.dimension().location(),used,(System.nanoTime()-start)/1000,
                    active.stream().map(l->l.id+":plan="+l.actions.size()+",payloads="+l.columns.size()+",actions="+l.worldActions+",external="+l.externalBroken+",peak="+l.peakActions+",complete="+l.complete()).toList());
        return used;
    }
    private static boolean loaded(ServerLevel level,BlockPos pos) { return level.hasChunkAt(pos)&&level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null; }
    private static boolean one(ServerLevel level,ReactorAccidentLedger l) {
        if(!l.shockComplete) {
            if(!l.parameters.destruction()||l.parameters.shockHealthPoints()==0){l.shockComplete=true;return true;}
            else {
                var area=new AABB(Vec3.atLowerCornerOf(l.min),Vec3.atLowerCornerOf(l.max.offset(1,1,1))).inflate(l.parameters.extension());
                for(var player:level.players())if(area.contains(player.position())&&!l.shocked.contains(player.getUUID())) {
                    l.shocked.add(player.getUUID());
                    double dx=Math.max(l.min.getX()-player.getX(),Math.max(0,player.getX()-l.max.getX()-1));
                    double dy=Math.max(l.min.getY()-player.getY(),Math.max(0,player.getY()-l.max.getY()-1));
                    double dz=Math.max(l.min.getZ()-player.getZ(),Math.max(0,player.getZ()-l.max.getZ()-1));
                    double scale=Math.max(0,1-Math.sqrt(dx*dx+dy*dy+dz*dz)/(l.parameters.extension()+1D));
                    if(!player.isCreative()&&!player.isSpectator())player.hurt(level.damageSources().generic(),(float)(l.parameters.shockHealthPoints()*scale));
                    return true;
                }
                l.shockComplete=true;
                return true;
            }
        }
        // 所有可用动作轮转；一个未加载目标不会阻塞另一已加载区块或其它事故。
        for(int checked=0;checked<l.actions.size();checked++) {
            var a=l.actions.get(Math.floorMod(l.cursor++,l.actions.size()));
            if(a.done||!loaded(level,a.pos))continue;
            var state=level.getBlockState(a.pos);
            if(Block.getId(state)!=a.expected||state.getDestroySpeed(level,a.pos)<0||newOwnerAt(level,l,a.pos)) {a.done=true;return true;}
            var be=level.getBlockEntity(a.pos);
            if(be!=null&&!ownedEntity(be,a.entityGeneration,l)) {a.done=true;return true;}
            // 不产生原方块掉落或世界流体，避免事故复制原库存/可回收工质。
            // 事故显式失效本模组结构/源；禁止原版形状/邻居递归读取远端格而暗中强载区块。
            if(level.setBlock(a.pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE)) {
                a.done=true;if(a.external)l.externalBroken++;
                ReactorStructureLifecycle.scheduleRescanAround(level,a.pos); RadiationContainment.changed(level,a.pos); RadiationSourceIndex.changed(level,a.pos);
                return true;
            }
        }
        for(var c:l.columns) {
            if(c.settled()||!c.captured()||!loaded(level,c.bottom))continue;
            if(c.payload().isEmpty()){finish(level,c);return true;}
            if(c.waste()) {
                if(level.getBlockEntity(c.bottom) instanceof ReactorCoriumBlockEntity existing&&existing.matches(l.id,c.bottom)) {
                    finish(level,c);return true;
                }
                if(!level.getBlockState(c.bottom).isAir()||newOwnerAt(level,l,c.bottom))continue;
                if(!level.setBlock(c.bottom,AccidentContent.REACTOR_CORIUM.get().defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE))continue;
                if(!(level.getBlockEntity(c.bottom) instanceof ReactorCoriumBlockEntity corium)||!corium.accept(l.id,c.bottom,c.payload()))
                    throw new IllegalStateException("残骸载荷交接失败，托管保留");
            } else {
                UUID entityId=UUID.nameUUIDFromBytes((l.id+":"+c.port.asLong()).getBytes(StandardCharsets.UTF_8));
                if(level.getEntity(entityId)==null) {
                    var item=new ItemEntity(level,c.bottom.getX()+.5,c.bottom.getY()+.5,c.bottom.getZ()+.5,c.payload());
                    item.setUUID(entityId);item.setDeltaMovement(Vec3.ZERO);item.setUnlimitedLifetime();
                    if(!level.addFreshEntity(item))continue;
                }
            }
            finish(level,c); return true;
        }
        return false;
    }
    private static void finish(ServerLevel level,ReactorAccidentLedger.Column column) {
        column.finish();ReactorAccidentSavedData.get(level).payloadChanged(column);RadiationSourceIndex.changed(level,column.bottom);
        RadiationSourceIndex.custodyChanged(level,column.bottom);
    }
    private static boolean ownedEntity(net.minecraft.world.level.block.entity.BlockEntity be,UUID generation,ReactorAccidentLedger l) {
        if(generation==null)return false;
        if(be instanceof ReactorPortBlockEntity port)return port.accidentGeneration().equals(generation)&&port.fuelAssembly().isEmpty();
        return be instanceof ReactorInstrumentPortBlockEntity owner&&owner.accidentGeneration().equals(generation)&&l.id.equals(owner.accidentId());
    }
    /** 新成型的不同世代有自己的库存，旧任务即使坐标/外观相同也不能把它作为原目标。 */
    private static boolean newOwnerAt(ServerLevel level,ReactorAccidentLedger l,BlockPos pos) {
        for(var owner:ReactorStructureLifecycle.loadedOwnersNear(level,pos)) {
            if(!owner.structureValid()||owner.accidentGeneration().equals(l.generation)||l.knownOwners.contains(owner.accidentGeneration())||owner.structureOrigin()==null)continue;
            var min=owner.structureOrigin();var d=owner.structureScan().dimensions();
            if(pos.getX()>=min.getX()&&pos.getX()<min.getX()+d.width()&&pos.getY()>=min.getY()&&pos.getY()<min.getY()+d.height()
                    &&pos.getZ()>=min.getZ()&&pos.getZ()<min.getZ()+d.depth())return true;
        }
        return false;
    }
}
