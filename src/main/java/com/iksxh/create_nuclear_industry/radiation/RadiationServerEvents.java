package com.iksxh.create_nuclear_industry.radiation;

import com.iksxh.create_nuclear_industry.config.RadiationConfig;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.network.PacketDistributor;

/** 服务端在线玩家剂量、短时症状与伤害；不读客户端剂量，不修改其他来源效果。 */
public final class RadiationServerEvents {
    public static final ResourceKey<DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("create_nuclear_industry:radiation"));
    private static final Map<UUID,Boolean> EXPOSED=new HashMap<>();
    private static final Map<UUID,Integer> ONLINE_TICKS=new HashMap<>();
    private static long packets;
    private static final int DISPLAY_INTERVAL_TICKS=20;
    private static final Map<UUID,DisplayState> DISPLAYS=new HashMap<>();
    private static final class DisplayState {
        long lastSent=Long.MIN_VALUE;net.minecraft.resources.ResourceLocation dimension;
        RadiationExposurePayload latest,unsentComplete;
    }
    private RadiationServerEvents() {}
    public static RadiationDoseState state(ServerPlayer player) {return RadiationDoseState.load(player.getPersistentData().getCompound(RadiationDoseState.KEY),RadiationConfig.parameters());}
    public static void save(ServerPlayer player,RadiationDoseState state) {player.getPersistentData().put(RadiationDoseState.KEY,state.save());}
    public static long packetsSent() {return packets;}
    /** 在所有世界/玩家物流tick之后统一采样，同tick共享投影不会跨越两名玩家的库存移动。 */
    @SubscribeEvent public static void serverTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        for(ServerLevel level:event.getServer().getAllLevels())for(ServerPlayer player:List.copyOf(level.players()))
            tick(new PlayerTickEvent.Post(player));
    }
    public static void tick(PlayerTickEvent.Post event) {tick(event,null);}
    /** 注入的索引只改变技术工作预算；实际剂量/伤害/发送仍走同一在线服务端控制流。 */
    public static void tick(PlayerTickEvent.Post event,RadiationSourceIndex samplingIndex) {
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var p=RadiationConfig.parameters();var before=state(player);boolean affected=!player.isCreative()&&!player.isSpectator();
        int elapsed=ONLINE_TICKS.merge(player.getUUID(),1,Integer::sum);
        // 伤害时钟独立于采样队列，incomplete不能变成高剂量免伤手段。
        int damageTicks=p.enabled()&&affected&&before.dose()>=p.damageDose()?before.damageTicks()+1:0;
        if(damageTicks>=p.damageIntervalTicks()) {
            player.hurt(new DamageSource(player.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE)),(float)p.damageHealthPoints());
            damageTicks=0;
        }
        var state=new RadiationDoseState(before.dose(),before.safeTicks(),damageTicks);
        if(elapsed>=p.sampleIntervalTicks()) {
            var sample=(samplingIndex==null?RadiationSourceIndex.get(player.serverLevel()):samplingIndex).sample(player,p);
            double absorbed=p.enabled()&&affected?sample.fieldDosePerSecond()*(1-RadiationEquipment.reduction(player,p)):0;
            if(sample.complete()||!p.enabled()) {
                ONLINE_TICKS.put(player.getUUID(),0);
                var next=RadiationDoseCalculator.advance(state,affected?absorbed:0,elapsed,p);
                state=new RadiationDoseState(next.dose(),next.safeTicks(),next.dose()>=p.damageDose()?damageTicks:0);
                boolean exposure=p.enabled()&&affected&&absorbed>p.safeAbsorptionPerSecond();
                if(exposure&&!EXPOSED.getOrDefault(player.getUUID(),false))player.displayClientMessage(Component.translatable("radiation.create_nuclear_industry.exposed"),false);
                EXPOSED.put(player.getUUID(),exposure);
                for(double threshold:new double[]{p.warningDose(),p.symptomDose(),p.damageDose()})
                    if(before.dose()<threshold&&state.dose()>=threshold)player.displayClientMessage(Component.translatable("radiation.create_nuclear_industry.warning",(int)threshold),false);
            }
            offer(player,new RadiationExposurePayload(player.level().dimension().location(),sample.gameTime(),sample.fieldDosePerSecond(),state.dose(),absorbed,sample.complete()));
        }
        save(player,state);applySymptoms(player,state,p,affected);
        if(!(player instanceof net.neoforged.neoforge.common.util.FakePlayer))
            flush(player,player.level().getGameTime(),packet->PacketDistributor.sendToPlayer(player,packet));
    }
    /** 一次完成结果先入显示队列；采样重试、剂量推进与发送时钟互不复用。 */
    private static void offer(ServerPlayer player,RadiationExposurePayload payload) {
        var display=DISPLAYS.computeIfAbsent(player.getUUID(),k->new DisplayState());
        if(!payload.dimension().equals(display.dimension)){display.dimension=payload.dimension();display.lastSent=Long.MIN_VALUE;display.unsentComplete=null;}
        display.latest=payload;if(payload.complete())display.unsentComplete=payload;
    }
    /** 正式网络和计数传输接收器共用同一节拍；pending也至多每20在线tick一次。 */
    public static void deliver(ServerPlayer player,RadiationExposurePayload payload,long now,java.util.function.Consumer<RadiationExposurePayload> transport) {
        offer(player,payload);flush(player,now,transport);
    }
    private static void flush(ServerPlayer player,long now,java.util.function.Consumer<RadiationExposurePayload> transport) {
        DisplayState display=DISPLAYS.get(player.getUUID());
        if(display==null||!RadiationEquipment.holdingMeter(player)||display.latest==null)return;
        if(display.lastSent!=Long.MIN_VALUE&&now>=display.lastSent&&now-display.lastSent<DISPLAY_INTERVAL_TICKS)return;
        // 已完成结果不会被后续pending覆盖；gameTime仍是该结果完成时刻，客户端允许它接替pending。
        RadiationExposurePayload payload=display.unsentComplete!=null?display.unsentComplete:display.latest;
        transport.accept(payload);packets++;display.lastSent=now;display.unsentComplete=null;
    }
    /** 只短时刷新I级症状，剂量下降后自然到期；不移除任何其他来源的同种药水。 */
    public static void applySymptoms(ServerPlayer player,RadiationDoseState state,RadiationParameters p,boolean affected) {
        if(p.enabled()&&affected&&state.dose()>=p.symptomDose()&&player.tickCount%20==0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,40,0,false,true,true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,0,false,true,true));
        }
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if(event.getEntity() instanceof ServerPlayer next) {
            RadiationSourceIndex.cancel(next.getUUID());DISPLAYS.remove(next.getUUID());ONLINE_TICKS.remove(next.getUUID());
            var state=event.isWasDeath()?RadiationDoseState.ZERO:RadiationDoseState.load(event.getOriginal().getPersistentData().getCompound(RadiationDoseState.KEY),RadiationConfig.parameters());save(next,state);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {ONLINE_TICKS.remove(event.getEntity().getUUID());EXPOSED.remove(event.getEntity().getUUID());DISPLAYS.remove(event.getEntity().getUUID());RadiationSourceIndex.cancel(event.getEntity().getUUID());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        UUID id=event.getEntity().getUUID();ONLINE_TICKS.remove(id);DISPLAYS.remove(id);EXPOSED.remove(id);RadiationSourceIndex.cancel(id);
    }
    @SubscribeEvent public static void leave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {RadiationSourceIndex.entityChanged(event.getEntity());}
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if(!event.getLevel().isClientSide && event.getEntity() instanceof ItemEntity item)protectExpiry(item);
    }
    @SubscribeEvent public static void expire(ItemExpireEvent event) {protectExpiry(event.getEntity());}
    private static boolean protectExpiry(ItemEntity item) {
        var summary=RadiationItemSources.describe(item.getItem(),RadiationConfig.parameters());
        if(summary.radioactivePayload()||!summary.complete()){item.setUnlimitedLifetime();return true;}return false;
    }
    @SubscribeEvent public static void load(ChunkEvent.Load event) {
        if(event.getLevel() instanceof ServerLevel level) {
            int x=event.getChunk().getPos().x,z=event.getChunk().getPos().z;
            // 此事件可能早于FULL；只失效几何/任务，不读取新载入方块或等待区块提升。
            RadiationContainment.chunkChanged(level,x,z);RadiationSourceIndex.chunkChanged(level,x,z);
        }
    }
    @SubscribeEvent public static void unload(ChunkEvent.Unload event) {
        if(event.getLevel() instanceof ServerLevel level){RadiationSourceIndex.unload(level,event.getChunk().getPos().x,event.getChunk().getPos().z);RadiationContainment.chunkChanged(level,event.getChunk().getPos().x,event.getChunk().getPos().z);}
    }
    @SubscribeEvent public static void close(LevelEvent.Unload event) {if(event.getLevel() instanceof ServerLevel level)RadiationSourceIndex.close(level);}
}
