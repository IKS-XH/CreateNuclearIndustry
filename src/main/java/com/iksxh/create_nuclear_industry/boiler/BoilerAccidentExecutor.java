package com.iksxh.create_nuclear_industry.boiler;

import java.util.*;
import com.iksxh.create_nuclear_industry.accident.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 服务端有预算的定向事故执行器；不用原版球形爆炸，不强载，单次收据先于每项伤害/破坏。 */
@EventBusSubscriber(modid="create_nuclear_industry")
public final class BoilerAccidentExecutor {
    private BoilerAccidentExecutor(){}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void onTick(ServerTickEvent.Post event){for(var level:event.getServer().getAllLevels()){BoilerAccidentLifecycle.finishObserved(level);tick(level);}}
    /** 与反应堆当前活跃任务的冻结上限共同取最小值，不借事件顺序抢先花掉另一个64额度。 */
    public static int sharedCap(ReactorAccidentSavedData shared){
        var work=new LinkedHashSet<ReactorAccidentLedger>();work.addAll(shared.initialWork());work.addAll(shared.thermalWork());
        return work.stream().mapToInt(l->l.parameters.worldActionsPerTick()).min().orElse(64);
    }
    public static int tick(ServerLevel level){
        var data=BoilerAccidentSavedData.get(level);var work=data.active();if(work.isEmpty())return 0;
        var shared=ReactorAccidentSavedData.get(level);int before=shared.availableBudget(level.getGameTime(),sharedCap(shared));
        int attempts=0,idle=0;
        while(attempts<64&&idle<work.size()&&shared.availableBudget(level.getGameTime(),sharedCap(shared))>0){
            var l=work.get(attempts++%work.size());if(l.complete()){idle++;continue;}
            int used=one(level,data,shared,l);if(used==0)idle++;else {idle=0;shared.setDirty();}
            data.refresh(l);
            if(l.complete()&&BoilerAccidentLifecycle.loaded(level,l.owner)&&level.getBlockEntity(l.owner) instanceof BoilerControllerBlockEntity owner
                    &&owner.boilerGeneration().equals(l.generation))owner.finishAccident(l);
        }
        return before-shared.availableBudget(level.getGameTime(),sharedCap(shared));
    }
    private static int one(ServerLevel level,BoilerAccidentSavedData data,ReactorAccidentSavedData shared,BoilerAccidentLedger l){
        if(!BoilerAccidentLifecycle.loaded(level,l.owner))return 0;
        if(level.getBlockEntity(l.owner) instanceof BoilerControllerBlockEntity replacement&&!replacement.boilerGeneration().equals(l.generation)){
            l.cursor=l.targets.size();l.shockComplete=true;shared.spendBudget(1);return 1;
        }
        if(!l.shockComplete){
            if(!BoilerAccidentLifecycle.loaded(level,l.origin))return 0;
            for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(l.origin).inflate(l.radius))){
                if(!entity.isAlive()||!l.inJet(entity.position())||l.damaged.contains(entity.getUUID()))continue;
                l.damaged.add(entity.getUUID());data.setDirty();shared.spendBudget(1);
                double scale=Math.max(0,1-entity.position().distanceTo(Vec3.atCenterOf(l.origin))/l.radius);
                if(!(entity instanceof Player player)||!player.isCreative()&&!player.isSpectator())entity.hurt(level.damageSources().generic(),(float)(l.parameters.damageHp()*scale));
                return 1;
            }
            l.shockComplete=true;data.setDirty();shared.spendBudget(1);
            level.playSound(null,l.origin,SoundEvents.GENERIC_EXPLODE.value(),SoundSource.BLOCKS,1,1);
            level.sendParticles(ParticleTypes.EXPLOSION,l.origin.getX()+.5,l.origin.getY()+.5,l.origin.getZ()+.5,1,0,0,0,0);return 1;
        }
        if(l.cursor>=l.targets.size())return 0;var target=l.targets.get(l.cursor);
        if(!BoilerAccidentLifecycle.loaded(level,target.pos))return 0;
        var owner=BoilerStructure.owner(level,target.pos);
        boolean newOwner=owner!=null&&!owner.boilerGeneration().equals(l.generation);
        if(newOwner||!target.matches(level)){l.cursor++;data.setDirty();shared.spendBudget(1);return 1;}
        var receipt=MoltenCoriumEffects.removeEnvironmentBlock(level,target.pos,BoilerAccidentLifecycle.environmentActor(level),l.burst,()->target.matches(level));
        if(receipt.status()==MoltenCoriumEffects.RemovalStatus.DEFER)return 0;
        // 真正提交或最终拒绝才领取；额度/读取域暂缺保留原游标、目标及身份，下tick原项重试。
        l.cursor++;data.setDirty();
        if(receipt.writes()==0){shared.spendBudget(1);return 1;}return receipt.writes();
    }
}
