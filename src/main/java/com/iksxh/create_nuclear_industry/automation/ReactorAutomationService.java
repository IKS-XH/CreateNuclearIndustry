package com.iksxh.create_nuclear_industry.automation;
import com.iksxh.create_nuclear_industry.blockentity.*;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderService;
import com.iksxh.create_nuclear_industry.reactor.*;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import static com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.*;

/** 游戏线程权限边界：正式tick前提交目标、后冻结同龄观测再降权，不另执行核反应或伪造SCRAM。 */
public final class ReactorAutomationService {
    public enum Command { MODE, DEPTH, HEAT, RESET }
    public record Request(BlockPos burnerPos,BlockPos portPos,UUID ownerGeneration,long authorityEpoch,Command command,int value,long clientSequence) {}
    public record Response(boolean accepted,String reason,ReactorAutomationBinding binding,ReactorAutomationPortBlockEntity.Settings settings,long clientSequence) {}
    private static final Map<ServerPlayer,Long> SEQUENCES=new WeakHashMap<>();
    private ReactorAutomationService() {}
    /** 只读取所有者缓存列图及已加载接口；外侧燃烧室不计入结构体积。 */
    public static ReactorAutomationBinding candidate(ReactorInstrumentPortBlockEntity o) {
        if(o.getLevel()==null || o.getLevel().isClientSide || !o.structureValid() || o.structureOrigin()==null) return null;
        var slots=o.structureScan().ports().getOrDefault(ReactorStructureDefinition.PortType.AUTOMATION,List.of());
        if(slots.size()!=1) return null;
        var p=slots.getFirst(); BlockPos pos=o.structureOrigin().offset(p.x(),p.y(),p.z());
        if(!o.getLevel().hasChunkAt(pos) || !(o.getLevel().getBlockEntity(pos) instanceof ReactorAutomationPortBlockEntity port)) return null;
        port.bindOwner(o);
        var dimensions=o.structureScan().dimensions();
        Direction outward=p.x()==0?Direction.WEST:p.x()==dimensions.width()-1?Direction.EAST:p.z()==0?Direction.NORTH:Direction.SOUTH;
        BlockPos burner=pos.relative(outward);
        if(!o.getLevel().hasChunkAt(burner) || !(o.getLevel().getBlockEntity(burner) instanceof BlazeBurnerBlockEntity)) return null;
        return new ReactorAutomationBinding(o.getLevel().dimension().location().toString(),o.getBlockPos(),o.automationGeneration(),pos,burner,o.automationState().authorityEpoch());
    }
    /** 当前实例必须与已交接的接口和燃烧室同时相同；候选坐标匹配不授予控制权。 */
    public static boolean hasCurrentInstances(ReactorInstrumentPortBlockEntity o,ReactorAutomationBinding b) {
        return o!=null && b!=null && o.getLevel()!=null && !o.getLevel().isClientSide
                && o.getLevel().hasChunkAt(b.portPos()) && o.getLevel().hasChunkAt(b.burnerPos())
                && o.automationPort()!=null && o.automationPort()==o.getLevel().getBlockEntity(b.portPos())
                && o.automationBurner()!=null && o.automationBurner()==o.getLevel().getBlockEntity(b.burnerPos());
    }
    /** 已加载接口拆除或卸载立即撤权；重复生命周期回调不重复推进epoch。 */
    public static void onPortRemoved(ReactorInstrumentPortBlockEntity o,ReactorAutomationPortBlockEntity removed) {
        if(o==null || o.getLevel()==null || o.getLevel().isClientSide || o.automationPort()!=removed) return;
        ReactorAutomationHeatSupply.invalidate(o);
        var binding=o.automationBinding();
        o.setAutomationPort(null);o.setAutomationObservation(null);o.automationCurve().clear();
        if(o.automationState().claimed()&&!ReactorAutomationResume.normalShutdownActive(o)) {
            var stopped=o.automationState().stop("binding_lost",true);
            o.setAutomationState(stopped);
            if(binding!=null) o.setAutomationBinding(binding.withEpoch(stopped.authorityEpoch()));
            ControlRodSliderService.revokeOwnerSessions(o);submitDepth(o,100,true);
        }
        ReactorAutomationBlazeBridge.notifyPermission(o,binding==null?null:binding.burnerPos());
    }
    /** 安全持权状态在正式棒推进前幂等维持全插入目标，覆盖维修后新恢复的可动棒。 */
    private static void ensureSafetyBaseline(ReactorInstrumentPortBlockEntity o) {
        var state=o.automationState();
        if(state.claimed() && (state.phase()==Phase.STOPPED || state.phase()==Phase.FAULT || state.phase()==Phase.ARMING)) submitDepth(o,100,true);
    }
    public static void beforeReactorTick(ReactorInstrumentPortBlockEntity o,ReactorAutomationParameters p,long time) {
        // 待续接的ARMING仅是加载锁，不是原冷启动全插基准；首次当前付款后再恢复正式控制周期。
        if(ReactorAutomationResume.pending(o))return;
        // 安全钳位不受调节去重或绑定可用性影响，也不改变控制epoch。
        ensureSafetyBaseline(o);
        var next=candidate(o); var old=o.automationBinding();
        if(next==null) { if(o.automationState().claimed()) revoke(o,"binding_lost"); return; }
        if(old==null || !hasCurrentInstances(o,next) || !old.ownerGeneration().equals(next.ownerGeneration()) || !old.portPos().equals(next.portPos()) || !old.burnerPos().equals(next.burnerPos())) {
            var s=o.automationState();
            o.setAutomationState(new ReactorAutomationState(true,s.phase()==Phase.FAULT?Phase.FAULT:Phase.STOPPED,s.phase()==Phase.FAULT?s.reason():"stopped",100,s.authorityEpoch()+1,0,0,0,0,Mode.STOP,false));
            o.setAutomationBinding(next.withEpoch(o.automationState().authorityEpoch()));
            o.setAutomationBurner(o.getLevel().getBlockEntity(next.burnerPos()));
            o.setAutomationPort((ReactorAutomationPortBlockEntity)o.getLevel().getBlockEntity(next.portPos()));
            ControlRodSliderService.revokeOwnerSessions(o);
            submitDepth(o,100,true);
            o.automationCurve().clear();
            ReactorAutomationBlazeBridge.notifyPermission(o,next.burnerPos());
        }
        if(!o.beginAutomationTick(time)) return;
        var previous=o.automationObservation();
        if(previous==null) return;
        var observation=observe(o,previous.generatedHeatHuPerTick(),previous.convertedCoolantMb(),previous.newDamage(),previous.sampleTime(),time);
        if (observation.fuelCount() == 0) { revoke(o,"fuel_lost"); return; }
        var decision=ReactorAutomationController.decide(o.automationState(),observation,p);
        applyDecision(o,decision);
    }
    public static void afterReactorTick(ReactorInstrumentPortBlockEntity o,ReactorServerTick.Result result,ReactorAutomationParameters p,long time) {
        if(!o.automationState().claimed()) return;
        boolean damage=result.thermal().columns().values().stream().anyMatch(c->c.integrityDamage()>1e-12);
        var sample=observe(o,result.fission().generatedHeatHu(),result.coolant().settlement().convertedCoolantMb(),damage,time,time);
        if (sample.fuelCount()==0) revoke(o,"fuel_lost");
        o.setAutomationObservation(sample);
        o.recordAutomationDisplayHeat(sample.generatedHeatHuPerTick(),p.displayAverageWindowTicks());
        String trip=ReactorAutomationController.tripReason(sample,p);
        if(!trip.isEmpty()) revoke(o,trip);
        else if((o.automationState().phase()==Phase.MANUAL || o.automationState().phase()==Phase.CONSTANT)
                && !ReactorAutomationController.coolantCanCarrySnapshot(sample,ReactorAutomationController.warning(sample,p)?ReactorAutomationController.safetySnapshot(sample.snapshot(),0):sample.snapshot())) revoke(o,"cold_shortage");
    }
    public static void onTickFailure(ReactorInstrumentPortBlockEntity o,long time) {
        ReactorAutomationHeatSupply.invalidate(o);
        // 只有正常停服事件已到且服务器实际退出中，解绑才不覆盖最终保存意图；运行中卸载仍FAULT。
        if(ReactorAutomationResume.normalShutdownActive(o)) {o.setAutomationObservation(null);return;}
        o.setAutomationObservation(null);
        if(o.automationState().claimed()) revoke(o,"sample_lost");
        ensureSafetyBaseline(o);
    }
    public static ReactorAutomationObservation observe(ReactorInstrumentPortBlockEntity o,double heat,double flow,boolean damage,long sampleTime,long time) {
        var b=candidate(o); var port=b==null?null:(ReactorAutomationPortBlockEntity)o.getLevel().getBlockEntity(b.portPos());
        boolean duty=b!=null && o.getLevel().getBlockEntity(b.burnerPos()) instanceof BlazeBurnerBlockEntity burner && ReactorAutomationBlazeBridge.isDutyValid(burner);
        var params=o.automationSimulationParameters();
        return new ReactorAutomationObservation(o.snapshot(),params,o.automationCoolantInput(),heat,flow,damage,hasCurrentInstances(o,b),duty,
                o.getLevel().hasNeighborSignal(o.getBlockPos()),sampleTime,time,
                port==null?new ReactorAutomationPortBlockEntity.Settings(Mode.STOP,100,0):port.settings(),o.automationCurve().curve(o.snapshot(),params),com.iksxh.create_nuclear_industry.config.P1ServerConfig.VALUES.coolantAbsorptionHuPerMb.get()>0);
    }
    private static void applyDecision(ReactorInstrumentPortBlockEntity o,ReactorAutomationController.Decision d) {
        var old=o.automationState(); o.setAutomationState(d.nextState());
        if(o.automationBinding()!=null) o.setAutomationBinding(o.automationBinding().withEpoch(d.nextState().authorityEpoch()));
        if(old.authorityEpoch()!=d.nextState().authorityEpoch()) ControlRodSliderService.revokeOwnerSessions(o);
        // 预警各棒在自己的实际/目标安全基准上钳位与插入，整张列图一次提交。
        if(d.blockRodLift()) {
            var before=o.snapshot(); var protectedSnapshot=ReactorAutomationController.safetySnapshot(before,d.safeInsertPercent());
            if(!protectedSnapshot.equals(before)) o.setSnapshot(protectedSnapshot);
        }
        d.depthCommand().ifPresent(depth->submitDepth(o,depth,depth==100));
        if(old.runPermission()!=d.nextState().runPermission()) ReactorAutomationBlazeBridge.notifyPermission(o,o.automationBinding()==null?null:o.automationBinding().burnerPos());
    }
    /** 安全恢复基准先写100，再通知许可。卡死棒和库存/热账本保持，不能伪造完全停堆。 */
    public static void submitDepth(ReactorInstrumentPortBlockEntity o,int depth,boolean safeBaseline) {
        var before=o.snapshot(); var rods=new TreeMap<CoreColumnPosition,ControlRodColumnState>();
        before.controlRodColumns().forEach((pos,r)->rods.put(pos,ControlRodStateTransitions.requestTargetDepth(r,depth/100.)));
        var next=before.withColumns(before.fuelColumns(),rods);
        if(safeBaseline && before.scramRequested()) {
            var targets=new TreeMap<CoreColumnPosition,Double>(); rods.forEach((pos,r)->{ if(!r.jammed()) targets.put(pos,1.); });
            next=next.withScramState(targets,true);
        }
        if(!next.equals(before)) o.setSnapshot(next);
    }
    public static void revoke(ReactorInstrumentPortBlockEntity o,String reason) {
        var old=o.automationState(); if(!old.claimed()) return;
        if(old.phase()==Phase.FAULT && !old.runPermission()) {
            // 再次失效必须中断健康连续期；重复坏tick不能每tick推进会话代际。
            if(old.healthyAge()>0) {
                var reset=new ReactorAutomationState(true,Phase.FAULT,old.reason(),100,old.authorityEpoch()+1,0,0,0,0,Mode.STOP,false);
                o.setAutomationState(reset);
                if(o.automationBinding()!=null) o.setAutomationBinding(o.automationBinding().withEpoch(reset.authorityEpoch()));
                ControlRodSliderService.revokeOwnerSessions(o);
            }
            return;
        }
        applyDecision(o,new ReactorAutomationController.Decision(old.stop(reason,true),OptionalInt.of(100),0,reason));
    }
    /** 管理员手动逐棒确认使用当前真实各棒目标；预警只许更深，拟目标缺冷整次拒绝。 */
    public static boolean canSetManualRod(ReactorInstrumentPortBlockEntity o,CoreColumnPosition column,int percent) {
        if(o.automationState().phase()!=Phase.MANUAL || o.automationObservation()==null) return false;
        try {
            var p=com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.parameters(); var last=o.automationObservation();
            var sample=observe(o,last.generatedHeatHuPerTick(),last.convertedCoolantMb(),last.newDamage(),last.sampleTime(),o.getLevel().getGameTime());
            if(!ReactorAutomationController.tripReason(sample,p).isEmpty() || sample.signalHigh()) return false;
            var rod=o.snapshot().controlRodColumns().get(column);
            if(rod==null || rod.jammed() || percent<0 || percent>100) return false;
            if(ReactorAutomationController.warning(sample,p) && percent/100.<Math.max(rod.actualDepth(),rod.targetDepth())-1e-9) return false;
            var rods=new TreeMap<>(o.snapshot().controlRodColumns()); rods.put(column,ControlRodStateTransitions.requestTargetDepth(rod,percent/100.));
            return ReactorAutomationController.coolantCanCarrySnapshot(sample,o.snapshot().withColumns(o.snapshot().fuelColumns(),rods));
        } catch(IllegalArgumentException invalidConfig) {return false;}
    }

    /** 纯身份校验供真实入口复用，旧代际和两玩家竞态不得修改任何设定。 */
    public static boolean matches(Request r,ReactorAutomationBinding b) {
        return r!=null && b!=null && r.burnerPos()!=null && r.portPos()!=null && r.ownerGeneration()!=null
                && r.burnerPos().equals(b.burnerPos()) && r.portPos().equals(b.portPos())
                && r.ownerGeneration().equals(b.ownerGeneration()) && r.authorityEpoch()==b.authorityEpoch();
    }
    /** 旧世界控件入口已撤下；不接受没有当前GUI菜单会话的设置命令。 */
    public static Response applyCommand(ServerPlayer player,Request request) {
        return new Response(false,"gui_required",null,null,request==null?0:request.clientSequence());
    }
}
