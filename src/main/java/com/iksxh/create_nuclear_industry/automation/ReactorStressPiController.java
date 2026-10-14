package com.iksxh.create_nuclear_industry.automation;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/** 纯PI与101档正功率选择；只生成毛HU/t目标，真实保护/限速/世界提交属于仪表，不持久化积分。 */
public final class ReactorStressPiController {
    private ReactorStressPiController() { }
    public record Settings(int reservePercent,int minimumHeatHuPerTick) {
        public Settings {
            if(reservePercent<0||reservePercent>90||minimumHeatHuPerTick<=0)throw new IllegalArgumentException("应力目标或最低毛功率非法");
        }
    }
    /** integral/target单位HU/t；filteredReserve为比例，previousTick仅当前游戏时钟；限速标记不成为运行授权。 */
    public record State(double integralHuPerTick,double filteredReserve,double targetHeatHuPerTick,
            long previousTick,boolean initialized,boolean actuatorLimited) {
        public static State empty() {return new State(0,0,0,-1,false,false);}
    }
    /** 缺少目标表示本次不能调节，调用者按原因处理等待或保护，绝不把容量0换算为100%。 */
    public record Decision(State nextState,OptionalDouble targetHeat,String reason) { }
    public static Decision step(State previous,ReactorStressSample sample,Settings settings,ReactorLoadFollowParameters p,
            double attainableMaxHu,double actualHeatHu,boolean inventoryLimited,long gameTick) {
        if(previous==null||sample==null||settings==null||p==null)return unavailable(previous,"invalid_sample");
        if(sample.status()!=ReactorStressSample.Status.VALID)return unavailable(previous,
                sample.status()==ReactorStressSample.Status.NO_CAPACITY?"no_capacity":"sample_lost");
        if(sample.sampleTick()!=gameTick||gameTick<0||!Double.isFinite(attainableMaxHu)||!Double.isFinite(actualHeatHu)
                ||attainableMaxHu<settings.minimumHeatHuPerTick()||actualHeatHu<0
                ||!Double.isFinite(previous.integralHuPerTick())||!Double.isFinite(previous.filteredReserve())
                ||!Double.isFinite(previous.targetHeatHuPerTick())
                ||previous.initialized()&&gameTick<=previous.previousTick())return unavailable(previous,"invalid_time_or_power");
        double raw=sample.reserveFraction().orElseThrow();
        double desired=settings.reservePercent()/100.;
        double kp=p.stressPiProportionalFraction()*attainableMaxHu;
        if(!previous.initialized()) {
            // 初次接管按当前真实毛功率反算积分，避免有网后第一步突然改功率；没有保存的PI可恢复。
            double target=clamp(actualHeatHu,settings.minimumHeatHuPerTick(),attainableMaxHu);
            double integral=target-kp*clamp(desired-raw,-1,1);
            return new Decision(new State(integral,raw,target,gameTick,true,false),OptionalDouble.of(target),"initialized");
        }
        long elapsed=gameTick-previous.previousTick();
        double filtered=previous.filteredReserve()+(-Math.expm1(-(double)elapsed/p.stressFilterTimeTicks()))*(raw-previous.filteredReserve());
        double error=clamp(desired-filtered,-1,1);
        double integral=previous.integralHuPerTick();
        double target;
        if(Math.abs(error)<=p.stressReserveDeadbandFraction()) {
            // 死区保持上回合实际执行的档位，不积误差，滤波时钟仍继续推进。
            target=clamp(previous.targetHeatHuPerTick(),settings.minimumHeatHuPerTick(),attainableMaxHu);
        } else {
            double proposed=integral+p.stressPiIntegralFractionPerSecond()*attainableMaxHu*error*(elapsed/20.);
            double unlimited=kp*error+proposed;
            double previousError=clamp(desired-previous.filteredReserve(),-1,1);
            boolean pushesLimit=error>0&&unlimited>attainableMaxHu||error<0&&unlimited<settings.minimumHeatHuPerTick();
            boolean sameLimitedDirection=previous.actuatorLimited()&&Math.signum(error)==Math.signum(previousError);
            // 库存预警只阻止增功率；棒位限速/上下限阻止继续同向积分，反向误差仍可退出饱和。
            if(!pushesLimit&&!sameLimitedDirection&&!(inventoryLimited&&error>0))integral=proposed;
            target=clamp(kp*error+integral,settings.minimumHeatHuPerTick(),attainableMaxHu);
        }
        if(!Double.isFinite(integral)||!Double.isFinite(filtered)||!Double.isFinite(target))return unavailable(previous,"invalid_pi");
        return new Decision(new State(integral,filtered,target,gameTick,true,false),OptionalDouble.of(target),"regulating");
    }
    /** 仅跟踪实际选档HU与真实限速/保护标记；不能把未执行请求保留为下一回合实际目标。 */
    public static State trackApplied(State state,double appliedHeatHu,boolean constrained) {
        if(state==null||!Double.isFinite(appliedHeatHu)||appliedHeatHu<0)throw new IllegalArgumentException("实际功率非法");
        return new State(state.integralHuPerTick(),state.filteredReserve(),appliedHeatHu,state.previousTick(),state.initialized(),constrained);
    }
    /** 保护每tick推进同龄滤波/时钟并跟踪已提交档位HU，积分冻结；恢复不能补算保护窗口。 */
    public static State trackProtected(State state,ReactorStressSample sample,ReactorLoadFollowParameters p,double appliedHeatHu,long gameTick) {
        if(state==null||!state.initialized())return State.empty();
        if(sample==null||p==null||sample.status()!=ReactorStressSample.Status.VALID||sample.sampleTick()!=gameTick
                ||state.previousTick()<0||gameTick<state.previousTick()||!Double.isFinite(state.integralHuPerTick())
                ||!Double.isFinite(state.filteredReserve())||!Double.isFinite(appliedHeatHu)||appliedHeatHu<0)return State.empty();
        double raw=sample.reserveFraction().orElseThrow();
        double filtered=state.filteredReserve()+(-Math.expm1(-(double)(gameTick-state.previousTick())/p.stressFilterTimeTicks()))*(raw-state.filteredReserve());
        return new State(state.integralHuPerTick(),filtered,appliedHeatHu,gameTick,true,true);
    }
    /** 不假定曲线单调；只在正最低毛HU与上限之间选最近档，相同距离优先更深棒。 */
    public static OptionalInt chooseOperatingDepth(double[] curve,double targetHu,double minimumHu,double maximumHu) {
        if(curve==null||curve.length!=101||!Double.isFinite(targetHu)||!Double.isFinite(minimumHu)||!Double.isFinite(maximumHu)
                ||targetHu<0||minimumHu<=0||maximumHu<minimumHu)return OptionalInt.empty();
        int best=-1;double distance=Double.POSITIVE_INFINITY;
        double target=clamp(targetHu,minimumHu,maximumHu);
        for(int depth=0;depth<=100;depth++) {
            double heat=curve[depth];
            if(!Double.isFinite(heat)||heat<0)return OptionalInt.empty();
            if(heat<minimumHu||heat>maximumHu)continue;
            double next=Math.abs(heat-target);
            if(next<=distance) {distance=next;best=depth;}
        }
        return best<0?OptionalInt.empty():OptionalInt.of(best);
    }
    private static Decision unavailable(State state,String reason) {return new Decision(state,OptionalDouble.empty(),reason);}
    private static double clamp(double value,double low,double high) {return Math.max(low,Math.min(high,value));}
}
