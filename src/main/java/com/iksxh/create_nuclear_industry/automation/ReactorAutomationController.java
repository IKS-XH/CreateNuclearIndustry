package com.iksxh.create_nuclear_industry.automation;

import com.iksxh.create_nuclear_industry.reactor.*;
import java.util.*;
import static com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.*;

/** 纯控制器仅给出目标和许可；不访问世界或提交快照。保护每tick，调节按配置周期。 */
public final class ReactorAutomationController {
    public record Decision(ReactorAutomationState nextState, OptionalInt depthCommand, int runPermission,String reason,boolean blockRodLift,int safeInsertPercent) {
        public Decision(ReactorAutomationState nextState,OptionalInt depthCommand,int runPermission,String reason) {
            this(nextState,depthCommand,runPermission,reason,false,0);
        }
    }
    /** 逐棒安全基准副本；世界提交由Service一次完成。 */
    public static ReactorSnapshot safetySnapshot(ReactorSnapshot s,int insertPercent) {
        var rods=new TreeMap<CoreColumnPosition,ControlRodColumnState>();
        s.controlRodColumns().forEach((pos,r)->rods.put(pos,ControlRodStateTransitions.requestTargetDepth(r,
                Math.min(1,Math.max(r.actualDepth(),r.targetDepth())+insertPercent/100.))));
        return s.withColumns(s.fuelColumns(),rods);
    }
    /** 当前各棒目标推进后的下一tick需求，不能替换成最后一次全堆值。 */
    public static boolean coolantCanCarryPendingTick(ReactorAutomationObservation o) { return coolantCanCarrySnapshot(o,o.snapshot()); }

    private ReactorAutomationController() {}
    /** 临时统一实际深度副本交给原裂变计算器；卡死棒保持实际深度，既不燃耗也不提交世界。 */
    public static double[] predictCurve(ReactorSnapshot snapshot,ReactorSimulationParameters parameters) {
        double[] curve=new double[101];
        for(int d=0;d<=100;d++) {
            var rods=new TreeMap<CoreColumnPosition,ControlRodColumnState>();
            for(var e:snapshot.controlRodColumns().entrySet()) {
                var r=e.getValue();
                rods.put(e.getKey(),r.jammed()?r:new ControlRodColumnState(r.integrity(),d/100.,d/100.,false,r.cachedHeatHu()));
            }
            curve[d]=ReactorFissionCalculator.calculate(snapshot.withColumns(snapshot.fuelColumns(),rods),parameters).generatedHeatHu();
        }
        return curve;
    }
    /** 遍历101档，不假定单调；相同安全功率优先更深棒。目标超过上限选最大可达功率。 */
    public static int chooseDepth(double[] curve,double targetHu) {
        if(curve==null || curve.length!=101 || !Double.isFinite(targetHu) || targetHu<0) throw new IllegalArgumentException("功率曲线或目标非法");
        int best=-1; double heat=-1;
        for(int d=0;d<=100;d++) {
            if(!Double.isFinite(curve[d]) || curve[d]<0) throw new IllegalArgumentException("功率曲线非法");
            if(curve[d]<=targetHu+1e-9 && curve[d]>=heat) { best=d; heat=curve[d]; }
        }
        return best<0?100:best;
    }
    /** 每个实例自己的预测缓存，只取计算器实际依赖；库存、缓存热和未耗尽的燃耗不制造失效。 */
    public static final class CurveCache {
        private Object key; private double[] curve;
        private record FuelKey(CoreColumnPosition position,boolean usable,double integrity) {}
        private record RodKey(CoreColumnPosition position,boolean jammed,double jammedDepth) {}
        public double[] curve(ReactorSnapshot s,ReactorSimulationParameters p) {
            var fuels=s.fuelColumns().entrySet().stream().map(e->new FuelKey(e.getKey(),e.getValue().hasUsableFuel(),e.getValue().integrity())).toList();
            var rods=s.controlRodColumns().entrySet().stream().map(e->new RodKey(e.getKey(),e.getValue().jammed(),e.getValue().jammed()?e.getValue().actualDepth():0)).toList();
            var next=List.of(s.dimensions(),p,fuels,rods);
            if(!next.equals(key)) { key=next; curve=predictCurve(s,p); }
            return curve;
        }
        public void clear() { key=null; curve=null; }
    }
    /** 以现行量化账本验证拟提交目标在下一tick推进后的热需求；真实库存优先于端口额定量。 */
    public static boolean coolantCanCarryNextTick(ReactorAutomationObservation o,int depth) {
        if(!o.coolingAvailable()) return false;
        var rods=new TreeMap<CoreColumnPosition,ControlRodColumnState>();
        o.snapshot().controlRodColumns().forEach((pos,r)->rods.put(pos,r.jammed()?r:
                ControlRodStateTransitions.requestTargetDepth(r,depth/100.)));
        return coolantCanCarrySnapshot(o,o.snapshot().withColumns(o.snapshot().fuelColumns(),rods));
    }
    /** 拟定各棒目标的完整副本进入原控制推进和量化冷却，不用统一深度掩盖手动差异。 */
    public static boolean coolantCanCarrySnapshot(ReactorAutomationObservation o,ReactorSnapshot pending) {
        if(!o.coolingAvailable()) return false;
        var projected=ReactorControlRodTick.advance(pending);
        double heat=ReactorFissionCalculator.calculate(projected,o.reactorParameters()).generatedHeatHu()
                +projected.fuelColumns().values().stream().mapToDouble(FuelColumnState::cachedHeatHu).sum();
        var settled=ReactorCoolantSimulationAdapter.settle(projected,new ReactorCoolantSimulationAdapter.TickInput(heat,
                o.coolantInput().ports(),0,0,o.coolantInput().coolantCapacityMb(),o.coolantInput().coolantAbsorptionHuPerMb()));
        return settled.settlement().remainingHeatHu()-settled.quantizedHeatRemainderHu()<=1e-9;
    }
    public static String tripReason(ReactorAutomationObservation o,ReactorAutomationParameters p) {
        if(o.settings().targetHeatHuPerTick()>p.maxTargetHeatHuPerTick()) return "invalid_value";
        if(o.settings().mode()==Mode.LOAD_FOLLOW&&o.settings().minimumHeatHuPerTick()>p.maxTargetHeatHuPerTick())return "invalid_value";
        if(!o.bindingValid() || o.gameTime()-o.sampleTime()>p.controlLeaseTicks() || o.gameTime()<o.sampleTime()) return "binding_lost";
        if(!o.dutyValid()) return "duty_lost";
        if(o.fuelCount()==0) return "fuel_lost";
        if(o.controlledFuelCount()!=o.fuelCount()) return "uncovered";
        if(o.jammedCount()>0) return "jammed";
        if(o.snapshot().meltdownCountdownStarted()) return "meltdown";
        if(o.newDamage()) return "damage";
        double threshold=o.reactorParameters().damageHeatThresholdHuPerTick();
        if(threshold==0?o.effectiveHeatHu()>1e-9:o.effectiveHeatHu()+1e-9>=threshold) return "heat_trip";
        if(!o.coolingAvailable()) return "cooling_unavailable";
        if(o.hotFraction()>p.tripHotFraction()) return "hot_trip";
        return "";
    }
    public static boolean startupHealthy(ReactorAutomationObservation o,ReactorAutomationParameters p) {
        return tripReason(o,p).isEmpty() && o.coldFraction()>=p.startupColdFraction()
                && o.hotFraction()<=p.startupMaxHotFraction() && o.fullyInserted();
    }
    public static boolean warning(ReactorAutomationObservation o,ReactorAutomationParameters p) {
        double threshold=o.reactorParameters().damageHeatThresholdHuPerTick();
        return o.coldFraction()<p.warningColdFraction() || o.hotFraction()>p.warningHotFraction()
                || (threshold>0 && o.effectiveHeatHu()+1e-9>=threshold*p.warningHeatThresholdFraction());
    }
    public static Decision decide(ReactorAutomationState state,ReactorAutomationObservation o,ReactorAutomationParameters p) {
        if(!state.claimed()) return result(state,null);
        String trip=tripReason(o,p);
        if(state.phase()==Phase.FAULT) {
            int healthy=startupHealthy(o,p)?Math.min(p.resetStableTicks(),state.healthyAge()+1):0;
            var next=new ReactorAutomationState(true,Phase.FAULT,state.reason(),100,state.authorityEpoch(),0,healthy,0,0,Mode.STOP,false);
            return result(next,null);
        }
        if(!trip.isEmpty()) return result(state.stop(trip,true),100);
        if(state.phase()==Phase.STOPPED) return result(state,null);
        if(state.phase()==Phase.ARMING) {
            if(o.signalHigh()) {
                if(state.armingAge()+1>=p.armingTimeoutTicks()) return result(state.stop("arming_timeout",true),100);
                return result(new ReactorAutomationState(true,Phase.ARMING,"arming",100,state.authorityEpoch(),0,0,0,state.armingAge()+1,state.requestedMode(),true),null);
            }
            var phase=state.requestedMode()==Mode.MANUAL?Phase.MANUAL:state.requestedMode()==Mode.LOAD_FOLLOW?Phase.LOAD_WARMUP:Phase.CONSTANT;
            if(phase==Phase.MANUAL) {
                // 降沿到达不解除库存/积热预警。逐棒保留安全基准与启动意图，解除预警后才提交已确认浅目标。
                if(warning(o,p)) {
                    if(!coolantCanCarrySnapshot(o,safetySnapshot(o.snapshot(),0))) return result(state.stop("cold_shortage",true),100);
                    return new Decision(with(state,100,0,"warning"),OptionalInt.empty(),15,"warning",true,0);
                }
                int depth=o.settings().manualDepthPercent();
                if(!coolantCanCarryNextTick(o,depth)) return result(state.stop("cold_shortage",true),100);
                return result(new ReactorAutomationState(true,phase,"manual",depth,state.authorityEpoch(),0,0,0,0,Mode.MANUAL,true),depth);
            }
            return result(new ReactorAutomationState(true,phase,"running",100,state.authorityEpoch(),0,0,0,0,state.requestedMode(),true),null);
        }
        if(o.signalHigh()) return result(state.stop("external_scram",true),100);
        if(state.phase()==Phase.CONSTANT && o.settings().targetHeatHuPerTick()==0) return result(state.stop("stopped",false),100);
        boolean warned=warning(o,p);
        var safePending=warned?safetySnapshot(o.snapshot(),0):o.snapshot();
        if(!coolantCanCarrySnapshot(o,safePending)) return result(state.stop("cold_shortage",true),100);
        int age=state.adjustmentAge()+1;
        if(warned) {
            int insert=age>=p.adjustIntervalTicks()?p.maxInsertPercentPerAdjustment():0;
            var protectedSnapshot=safetySnapshot(o.snapshot(),insert);
            if(!coolantCanCarrySnapshot(o,protectedSnapshot)) return result(state.stop("cold_shortage",true),100);
            int depth=state.phase()!=Phase.MANUAL?(int)Math.round(protectedSnapshot.controlRodColumns().values().stream()
                    .mapToDouble(ControlRodColumnState::targetDepth).average().orElse(1)*100):state.commandedDepthPercent();
            var next=with(state,depth,insert>0?0:age,"warning");
            return new Decision(next,OptionalInt.empty(),next.runPermission()?15:0,"warning",true,insert);
        }
        if(age<p.adjustIntervalTicks()) return result(with(state,state.commandedDepthPercent(),age,state.reason()),null);
        if(state.phase()==Phase.MANUAL) return result(with(state,state.commandedDepthPercent(),0,"manual"),null);
        // 闭环目标来自当前应力会话，纯保护控制器不把无关恒功率草稿0当成停堆请求。
        if(state.requestedMode()==Mode.LOAD_FOLLOW)return result(with(state,state.commandedDepthPercent(),0,"stress_waiting"),null);
        int desired=state.phase()==Phase.MANUAL?100:chooseDepth(o.curve(),o.settings().targetHeatHuPerTick());
        int current=state.commandedDepthPercent();
        String reason="regulating";
        if(Math.abs(o.curve()[current]-o.settings().targetHeatHuPerTick())<=Math.max(1,o.settings().targetHeatHuPerTick()*p.powerDeadbandFraction())) {
            return result(with(state,current,0,"deadband"),null);
        }
        else if(o.settings().targetHeatHuPerTick()>Arrays.stream(o.curve()).max().orElse(0)) reason="upper_limit";
        int next=desired<current?Math.max(desired,current-p.maxLiftPercentPerAdjustment()):Math.min(desired,current+p.maxInsertPercentPerAdjustment());
        if(!coolantCanCarryNextTick(o,next)) return result(state.stop("cold_shortage",true),100);
        return result(with(state,next,0,reason),next==current?null:next);
    }
    private static ReactorAutomationState with(ReactorAutomationState s,int depth,int age,String why) {
        return new ReactorAutomationState(true,s.phase(),why,depth,s.authorityEpoch(),age,s.healthyAge(),0,s.armingAge(),s.requestedMode(),s.runPermission());
    }
    private static Decision result(ReactorAutomationState s,Integer depth) {
        return new Decision(s,depth==null?OptionalInt.empty():OptionalInt.of(depth),s.runPermission()?15:0,s.reason());
    }
}
