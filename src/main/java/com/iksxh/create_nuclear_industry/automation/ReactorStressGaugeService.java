package com.iksxh.create_nuclear_industry.automation;

import java.util.*;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** 当前服务端选择与动力网调节权；全部实例登记暂态，持权不构造或强载Create网络。 */
public final class ReactorStressGaugeService {
    private ReactorStressGaugeService() { }
    private static final Registry CLAIMS=new Registry();
    private static final Map<ServerPlayer,Selection> SELECTIONS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> SEQUENCES=new WeakHashMap<>();
    private record Selection(Level level,BlockPos position,StressGaugeBlockEntity instance,long tick) { }
    /** 客户端只报Ctrl选择/绑定意图；只有绑定携带现有只读view的戳，不能提交任何设置。 */
    public record Intent(boolean select,BlockPos position,BlockPos port,UUID generation,long epoch,long sequence) { }
    /** 当前维度已加载真实表；拒绝移除实例和跨维度，不使用getOrCreateNetwork或区块强载。 */
    public static StressGaugeBlockEntity gauge(ReactorInstrumentPortBlockEntity owner) {
        if(owner==null||owner.getLevel()==null||owner.automationPort()==null)return null;
        var link=owner.automationPort().gaugeLink();var level=owner.getLevel();
        return link!=null&&link.dimension().equals(level.dimension().location().toString())&&level.hasChunkAt(link.position())
                &&level.getBlockEntity(link.position()) instanceof StressGaugeBlockEntity g&&!g.isRemoved()?g:null;
    }
    public static ReactorStressSample sample(ReactorInstrumentPortBlockEntity owner,long time) {
        var g=gauge(owner);
        return g==null?new ReactorStressSample(ReactorStressSample.Status.UNAVAILABLE,0,0,null,time):
                new ReactorStressSample(ReactorStressSample.Status.VALID,g.getNetworkCapacity(),g.getNetworkStress(),g.network,time);
    }
    public static boolean claim(ReactorInstrumentPortBlockEntity owner,Long network) {return CLAIMS.claim(owner.getLevel(),network,owner);}
    public static boolean available(ReactorInstrumentPortBlockEntity owner,Long network) {return CLAIMS.available(owner.getLevel(),network,owner);}
    /** 原网络变更若并入另一持权网，同时撤销该当前管理员，不私选一个继续闭环的主机。 */
    public static void revokeMergedNetwork(ReactorInstrumentPortBlockEntity owner,Long network) {
        var other=CLAIMS.owner(owner.getLevel(),network);
        if(other instanceof ReactorInstrumentPortBlockEntity instrument&&instrument!=owner)ReactorAutomationService.revoke(instrument,"stress_network_changed");
    }
    public static void release(ReactorInstrumentPortBlockEntity owner) {CLAIMS.release(owner);}
    /** 真实服务器线程、持物、可达性及同一选择实例全部核验后才提交位置，停堆外改绑整次拒绝。 */
    public static String apply(ServerPlayer player,Intent intent) {
        String reason="accepted";
        try {
            if(player==null||intent==null||intent.position()==null||!player.getServer().isSameThread()||player.isSpectator()
                    ||!AllItems.WRENCH.isIn(player.getMainHandItem())||!player.canInteractWithBlock(intent.position(),0)
                    ||!player.level().hasChunkAt(intent.position())||intent.sequence()<=SEQUENCES.getOrDefault(player,0L))throw new IllegalArgumentException("binding_invalid");
            SEQUENCES.put(player,intent.sequence());var level=player.level();long now=level.getGameTime();
            if(intent.select()) {
                if(!(level.getBlockEntity(intent.position()) instanceof StressGaugeBlockEntity gauge))throw new IllegalArgumentException("binding_invalid");
                SELECTIONS.put(player,new Selection(level,intent.position().immutable(),gauge,now));reason="gauge_selected";
            } else {
                var binding=ReactorAutomationBlazeBridge.findBinding(level,intent.position()).orElse(null);var owner=ReactorAutomationBlazeBridge.owner(level,binding);
                if(owner==null||!ReactorAutomationService.hasCurrentInstances(owner,binding)||owner.automationBinding()==null
                        ||!owner.automationBinding().equals(binding)||intent.port()==null||intent.generation()==null
                        ||!binding.portPos().equals(intent.port())||!binding.ownerGeneration().equals(intent.generation())||binding.authorityEpoch()!=intent.epoch())throw new IllegalArgumentException("stale_settings");
                if(owner.automationState().phase()!=ReactorAutomationState.Phase.STOPPED||ReactorAutomationResume.pending(owner))throw new IllegalArgumentException("binding_running");
                var selected=SELECTIONS.get(player);
                if(selected==null||!selectionValid(selected.tick(),now,level.hasChunkAt(selected.position())&&level.getBlockEntity(selected.position())==selected.instance()&&!selected.instance().isRemoved(),selected.level()==level))throw new IllegalArgumentException("selection_expired");
                var state=owner.automationState();long epoch=Math.addExact(state.authorityEpoch(),1);
                owner.automationPort().applyGaugeLink(new ReactorStressGaugeLink(level.dimension().location().toString(),selected.position()));
                owner.setAutomationState(new ReactorAutomationState(state.claimed(),state.phase(),state.reason(),state.commandedDepthPercent(),epoch,state.adjustmentAge(),state.healthyAge(),state.leaseAge(),state.armingAge(),state.requestedMode(),state.runPermission()));
                owner.setAutomationBinding(binding.withEpoch(epoch));SELECTIONS.remove(player);
                com.iksxh.create_nuclear_industry.control.ControlRodSliderService.revokeOwnerSessions(owner);
                ReactorAutomationBlazeBridge.tick((com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity)owner.automationBurner());reason="gauge_bound";
            }
        } catch(IllegalArgumentException|ArithmeticException invalid) {reason=invalid.getMessage()==null?"binding_invalid":invalid.getMessage();}
        if(player!=null)player.displayClientMessage(Component.translatable("automation.create_nuclear_industry.gui.result_"+reason),true);
        return reason;
    }
    /** 选择年龄按游戏tick，1200tick寿命不依赖墙钟；实例/维度必须仍为选择时身份。 */
    public static boolean selectionValid(long selected,long now,boolean currentInstance,boolean sameDimension) {
        return selected>=0&&now>=selected&&now-selected<=1200&&currentInstance&&sameDimension;
    }
    /** 单游戏线程的同网互斥；世界对象身份隔离，释放不会移除另一个所有者的登记。 */
    public static final class Registry {
        private final Map<Object,Map<Long,Object>> worlds=new WeakHashMap<>();
        public Object owner(Object world,Long network) {return network==null?null:worlds.getOrDefault(world,Map.of()).get(network);}
        public boolean available(Object world,Long network,Object owner) {var current=owner(world,network);return network!=null&&(current==null||current==owner);}
        public boolean claim(Object world,Long network,Object owner) {
            if(world==null||network==null||owner==null)return false;
            var claims=worlds.computeIfAbsent(world,w->new HashMap<>());var existing=claims.get(network);
            if(existing!=null&&existing!=owner)return false;
            claims.put(network,owner);return true;
        }
        public void release(Object owner) {worlds.values().forEach(claims->claims.values().removeIf(value->value==owner));}
    }
}
