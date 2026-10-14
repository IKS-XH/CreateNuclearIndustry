package com.iksxh.create_nuclear_industry.automation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

/** 纯PI、离散正功率档及明确时滞模型；模型不能替代实际热—汽—动力机组验收。 */
class ReactorStressPiControllerTest {
    private static final ReactorLoadFollowParameters P=new ReactorLoadFollowParameters(20,2,.03,40,.25,.005,1200);
    private static final ReactorStressPiController.Settings SETTINGS=new ReactorStressPiController.Settings(20,2);
    private static ReactorStressSample sample(double capacity,double load,long tick) {
        return new ReactorStressSample(ReactorStressSample.Status.VALID,capacity,load,1L,tick);
    }
    @Test void idleRetainsMinimumPositivePower() {
        double[] curve=new double[101];
        for(int depth=0;depth<=100;depth++)curve[depth]=(100-depth)*7;
        assertEquals(99,ReactorStressPiController.chooseOperatingDepth(curve,1,1,700).orElseThrow(),
                "目标低于最小正档7HU时仍须选正档，健康空载不能以100%全插结束");
    }
    @Test void reserveDefinitionAndOverload() {
        assertEquals(.2,sample(10000,8000,10).reserveFraction().orElseThrow(),1e-12);
        assertEquals(-.2,sample(10000,12000,10).reserveFraction().orElseThrow(),1e-12);
        assertEquals(ReactorStressSample.Status.UNAVAILABLE,sample(Double.NaN,10,10).status());
        assertEquals(ReactorStressSample.Status.UNAVAILABLE,sample(10,-1,10).status());
        assertEquals(ReactorStressSample.Status.UNAVAILABLE,sample(Double.MIN_VALUE,Double.MAX_VALUE,10).status(),"派生余量溢出也不能进入显示或PI");
    }
    @Test void zeroCapacityIsNotFreeReserve() {
        var noCapacity=sample(0,0,10);
        assertTrue(noCapacity.reserveFraction().isEmpty());
        assertTrue(ReactorStressPiController.step(ReactorStressPiController.State.empty(),noCapacity,SETTINGS,P,1000,10,false,10).targetHeat().isEmpty());
    }
    @Test void noOperatingBandRejectsStartup() {
        assertTrue(ReactorStressPiController.chooseOperatingDepth(new double[101],2,2,1000).isEmpty());
        double[] curve=new double[101];curve[40]=7;curve[60]=7;
        assertEquals(60,ReactorStressPiController.chooseOperatingDepth(curve,1,1,1000).orElseThrow());
        assertTrue(ReactorStressPiController.chooseOperatingDepth(curve,8,8,1000).isEmpty());
    }
    @Test void proportionalAndIntegralUseHuAndRealTickSeconds() {
        var previous=new ReactorStressPiController.State(0,.1,25,0,true,false);
        var result=ReactorStressPiController.step(previous,sample(10000,9000,10),new ReactorStressPiController.Settings(20,1),P,1000,25,false,10);
        assertEquals(.25,result.nextState().integralHuPerTick(),1e-10);
        assertEquals(25.25,result.targetHeat().orElseThrow(),1e-10);
    }
    @Test void initializationIsBumplessAndDeadbandHoldsAppliedTarget() {
        var initial=ReactorStressPiController.step(ReactorStressPiController.State.empty(),sample(10000,9000,10),SETTINGS,P,1000,500,false,10);
        assertEquals(500,initial.targetHeat().orElseThrow(),1e-10);
        assertEquals(475,initial.nextState().integralHuPerTick(),1e-10);
        var previous=new ReactorStressPiController.State(400,.205,400,0,true,false);
        var held=ReactorStressPiController.step(previous,sample(10000,8000,10),SETTINGS,P,1000,400,false,10);
        assertEquals(400,held.targetHeat().orElseThrow(),1e-10);
        assertEquals(400,held.nextState().integralHuPerTick(),1e-10);
    }
    @Test void saturationAndInventoryDoNotWindUpAndReverseCanRecover() {
        var high=new ReactorStressPiController.State(900,-.8,1000,0,true,false);
        var saturated=ReactorStressPiController.step(high,sample(10000,18000,10),SETTINGS,P,1000,1000,false,10);
        assertEquals(900,saturated.nextState().integralHuPerTick(),1e-10);
        assertEquals(1000,saturated.targetHeat().orElseThrow(),1e-10);
        var low=new ReactorStressPiController.State(0,.9,2,0,true,false);
        var minimum=ReactorStressPiController.step(low,sample(10000,1000,10),SETTINGS,P,1000,2,false,10);
        assertEquals(0,minimum.nextState().integralHuPerTick(),1e-10);
        assertEquals(2,minimum.targetHeat().orElseThrow(),1e-10);
        var limited=new ReactorStressPiController.State(250,.1,300,0,true,true);
        assertEquals(250,ReactorStressPiController.step(limited,sample(10000,9000,10),SETTINGS,P,1000,300,false,10).nextState().integralHuPerTick(),1e-10);
        assertEquals(250,ReactorStressPiController.step(new ReactorStressPiController.State(250,.1,300,0,true,false),sample(10000,9000,10),SETTINGS,P,1000,300,true,10).nextState().integralHuPerTick(),1e-10);
        var fastFilter=new ReactorLoadFollowParameters(20,2,.03,1,.25,.005,1200);
        assertTrue(ReactorStressPiController.step(limited,sample(10000,4000,10),SETTINGS,fastFilter,1000,300,false,10).nextState().integralHuPerTick()<250);
    }
    @Test void trackingUsesExecutedHeatAndInvalidTimeProducesNoCommand() {
        var previous=new ReactorStressPiController.State(475,.1,500,10,true,false);
        var tracked=ReactorStressPiController.trackApplied(previous,350,true);
        assertEquals(350,tracked.targetHeatHuPerTick());assertTrue(tracked.actuatorLimited());
        assertTrue(ReactorStressPiController.step(previous,sample(10000,9000,10),SETTINGS,P,1000,500,false,10).targetHeat().isEmpty());
        assertTrue(ReactorStressPiController.step(previous,sample(10000,9000,9),SETTINGS,P,1000,500,false,9).targetHeat().isEmpty());
        assertTrue(ReactorStressPiController.step(previous,sample(10000,9000,20),SETTINGS,P,1000,Double.NaN,false,20).targetHeat().isEmpty());
    }
    @Test void warningAcrossErrorSignCannotIntegrateProtectedTimeOnRecovery() {
        var tuned=new ReactorLoadFollowParameters(20,2,.03,40,.25,.01,1200);
        var protectedState=new ReactorStressPiController.State(100,.3,100,0,true,false);
        for(long time=1;time<=200;time++)protectedState=ReactorStressPiController.trackProtected(protectedState,sample(10000,15000,time),tuned,100,time);
        assertEquals(100,protectedState.integralHuPerTick(),1e-9,"保护期不得积提棒误差");
        var resumed=ReactorStressPiController.step(protectedState,sample(10000,15000,210),SETTINGS,tuned,1000,100,false,210);
        assertTrue(resumed.nextState().integralHuPerTick()<=105,"恢复第一周期只能计最多0.5秒，不能补10秒保护积分");
        assertEquals(200,protectedState.previousTick());
    }
    /** 明确的单阶热力模型：SU/HU比例10、热惯性5秒、额外0/5/20秒时滞，控制限速沿原1/5百分点。 */
    @ParameterizedTest @ValueSource(ints={0,5,20})
    void delayedLoadStepsStayFiniteAndSettleWithoutIdleShutdown(int delaySeconds) throws Exception {
        var tuned=new ReactorLoadFollowParameters(20,2,.03,40,.25,.01,1200);
        double[] curve=new double[101];for(int d=0;d<=100;d++)curve[d]=(100-d)*10;
        var history=new ArrayDeque<Double>();for(int i=0;i<=delaySeconds*2;i++)history.add(400.);
        var state=ReactorStressPiController.State.empty();int depth=60;
        double plant=400,steadyReserve=0;int steadyCount=0,changes=0,reverse=0,lastDirection=0;boolean idleSeen=false;
        var rows=new StringBuilder("seconds,load_su,capacity_su,heat_hu,depth,integral_hu,reserve,limited\n");
        for(int index=0;index<=1800;index++) {
            double seconds=index*.5;long tick=index*10L;
            double load=seconds<300?4000:seconds<500?0:6000;
            double delayed=history.removeFirst();history.add(curve[depth]);
            plant+=(-Math.expm1(-.5/5))*(delayed-plant);
            var measured=sample(plant*10,load,tick);
            var decision=ReactorStressPiController.step(state,measured,SETTINGS,tuned,1000,curve[depth],false,tick);
            assertTrue(decision.targetHeat().isPresent(),"有效容量必须生成目标");
            int desired=ReactorStressPiController.chooseOperatingDepth(curve,decision.targetHeat().orElseThrow(),2,1000).orElseThrow();
            int bounded=desired<depth?Math.max(desired,depth-1):Math.min(desired,depth+5);
            int direction=Integer.signum(bounded-depth);
            if(direction!=0) {changes++;if(lastDirection!=0&&direction!=lastDirection)reverse++;lastDirection=direction;}
            depth=bounded;
            state=ReactorStressPiController.trackApplied(decision.nextState(),curve[depth],bounded!=desired);
            assertTrue(Double.isFinite(state.integralHuPerTick()));assertTrue(depth<100,"健康空载不得完全停堆");
            if(seconds>=480&&seconds<500) {idleSeen=true;assertEquals(99,depth,"空载应达到最小正档10HU");}
            if(seconds>=850) {steadyReserve+=measured.reserveFraction().orElseThrow();steadyCount++;}
            rows.append(String.format(Locale.ROOT,"%.1f,%.1f,%.3f,%.1f,%d,%.6f,%.6f,%b%n",seconds,load,plant*10,curve[depth],depth,state.integralHuPerTick(),measured.reserveFraction().orElseThrow(),bounded!=desired));
        }
        var folder=Path.of("build/reports/reactor-load-follow-02");Files.createDirectories(folder);
        Files.writeString(folder.resolve("pi-delay-"+delaySeconds+"s.csv"),rows);
        Files.writeString(folder.resolve("pi-delay-"+delaySeconds+"s-summary.txt"),"Kp="+tuned.stressPiProportionalFraction()+" Ki="+tuned.stressPiIntegralFractionPerSecond()+" delay="+delaySeconds+" changes="+changes+" reversals="+reverse+" finalReserve="+steadyReserve/steadyCount+" depth="+depth);
        assertTrue(idleSeen);assertEquals(.2,steadyReserve/steadyCount,.04,"长时滞后的稳态余量须落入目标附近");
        assertTrue(reverse<40,"不应持续来回换向");
    }
}
