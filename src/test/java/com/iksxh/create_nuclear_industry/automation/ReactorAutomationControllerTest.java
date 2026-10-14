package com.iksxh.create_nuclear_industry.automation;
import com.iksxh.create_nuclear_industry.reactor.*;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
/** 真实计算器和决策边界回归，单位HU/t、mB和tick。 */
class ReactorAutomationControllerTest {
    @Test void armingWarningHoldsEachTickAndKeepsManualIntentUntilWarningClears() {
        var snapshot=snapshot(0,0).withCoolantInventories(149,100);
        var ports=ReactorCoolantLedger.summarizePorts(java.util.List.of(ReactorCoolantLedger.Port.cold("c",2048),ReactorCoolantLedger.Port.hot("h",2048)),2048);
        var sample=new ReactorAutomationObservation(snapshot,ReactorSimulationParameters.defaults(),new ReactorServerTick.CoolantInput(ports,1000,1),0,0,false,true,true,false,0,0,
                new ReactorAutomationPortBlockEntity.Settings(ReactorAutomationState.Mode.MANUAL,40,250),curve());
        assertTrue(ReactorAutomationController.warning(sample,P));assertTrue(ReactorAutomationController.coolantCanCarryNextTick(sample,40));
        var state=new ReactorAutomationState(true,ReactorAutomationState.Phase.ARMING,"arming",100,1,0,0,0,0,ReactorAutomationState.Mode.MANUAL,true);
        for(int tick=0;tick<5;tick++) {
            var decision=ReactorAutomationController.decide(state,sample,P);
            assertEquals(ReactorAutomationState.Phase.ARMING,decision.nextState().phase());assertTrue(decision.depthCommand().isEmpty());assertTrue(decision.blockRodLift());
            assertEquals(1,ReactorAutomationController.safetySnapshot(snapshot,decision.safeInsertPercent()).controlRodColumns().values().iterator().next().targetDepth());
            state=decision.nextState();
        }
        var healthy=new ReactorAutomationObservation(snapshot.withCoolantInventories(500,100),sample.reactorParameters(),sample.coolantInput(),0,0,false,true,true,false,0,0,sample.settings(),sample.curve());
        var release=ReactorAutomationController.decide(state,healthy,P);
        assertEquals(ReactorAutomationState.Phase.MANUAL,release.nextState().phase());assertEquals(40,release.depthCommand().orElseThrow());
    }
    @Test void partiallyExhaustedCoreKeepsDutyButNoUsableFuelTrips() {
        var s=snapshot(0,0);
        var spent=new CoreColumnPosition(2,1);
        s=s.withFuelColumn(spent,new FuelColumnState(FuelAssemblyState.installed(100,100),1,0));
        assertEquals("",ReactorAutomationController.tripReason(observation(s,250),ReactorAutomationParameters.defaults()));
        var usable=new CoreColumnPosition(1,1);
        s=s.withFuelColumn(usable,new FuelColumnState(FuelAssemblyState.installed(100,100),1,0));
        assertEquals("fuel_lost",ReactorAutomationController.tripReason(observation(s,250),ReactorAutomationParameters.defaults()));
    }
    private static final ReactorAutomationParameters P=ReactorAutomationParameters.defaults();
    static ReactorSnapshot snapshot(double cached,double remainder) {
        return new ReactorSnapshot(Map.of(new CoreColumnPosition(1,1),new FuelColumnState(FuelAssemblyState.installed(216000,0),1,cached,0,remainder)),
            Map.of(new CoreColumnPosition(1,0),ControlRodColumnState.fullyInserted()),500,100,0,false);
    }
    static double[] curve() { var c=new double[101]; for(int d=0;d<=100;d++) c[d]=Math.ceil(1000*Math.pow(1-d/100.0,2)); return c; }
    static ReactorAutomationObservation observation(ReactorSnapshot s,int target) {
        return new ReactorAutomationObservation(s,ReactorSimulationParameters.defaults(),new ReactorServerTick.CoolantInput(ReactorCoolantLedger.summarizePorts(java.util.List.of(),0),1000,1),0,0,false,true,true,false,0,0,
            new ReactorAutomationPortBlockEntity.Settings(ReactorAutomationState.Mode.CONSTANT,100,target),curve());
    }
    static ReactorAutomationState running(int depth,int age) { return new ReactorAutomationState(true,ReactorAutomationState.Phase.CONSTANT,"running",depth,1,age,0,0,0,ReactorAutomationState.Mode.CONSTANT,true); }
    @Test void choosesDiscreteNonlinearNearestAndSafeTie() {
        assertEquals(50,ReactorAutomationController.chooseDepth(curve(),250));
        assertEquals(0,ReactorAutomationController.chooseDepth(curve(),2000));
        var tied=curve(); tied[49]=250; assertEquals(50,ReactorAutomationController.chooseDepth(tied,250));
    }
    @Test void realPredictionDoesNotBurnOrMutateAndUsesOriginalCeil() {
        var s=snapshot(0,0); var before=s;
        var c=ReactorAutomationController.predictCurve(s,ReactorSimulationParameters.defaults());
        assertEquals(3,c[0]); assertEquals(0,c[100]); assertEquals(2,c[50]); assertSame(before,s);
        assertEquals(0,s.fuelColumns().values().iterator().next().fuelAssembly().damage());
    }
    @Test void ninthTickHoldsTenthTickLiftsOneAndInsertsFive() {
        var o=observation(snapshot(0,0),250);
        assertTrue(ReactorAutomationController.decide(running(100,8),o,P).depthCommand().isEmpty());
        assertEquals(99,ReactorAutomationController.decide(running(100,9),o,P).depthCommand().orElseThrow());
        assertEquals(55,ReactorAutomationController.decide(running(50,9),observation(snapshot(0,0),100),P).depthCommand().orElseThrow());
    }
    @Test void quantizedRemainderIsSafeButEffectiveThresholdTripsEveryTick() {
        assertNotEquals(ReactorAutomationState.Phase.FAULT,ReactorAutomationController.decide(running(100,0),observation(snapshot(.24,.24),250),P).nextState().phase());
        assertEquals(ReactorAutomationState.Phase.FAULT,ReactorAutomationController.decide(running(50,0),observation(snapshot(.25,0),250),P).nextState().phase());
    }
    @Test void missingColdForProposedNextDepthTripsBeforeSubmission() {
        var s=snapshot(0,0).withCoolantInventories(1,0);
        var base=observation(s,250);
        var o=new ReactorAutomationObservation(s,base.reactorParameters(),new ReactorServerTick.CoolantInput(base.coolantInput().ports(),2,.001),0,0,false,true,true,false,0,0,base.settings(),curve());
        assertEquals(ReactorAutomationState.Phase.FAULT,ReactorAutomationController.decide(running(100,9),o,P).nextState().phase());
    }
    @Test void startupUsesInventoryAndDoesNotRequireFlow() {
        var o=observation(snapshot(0,0),250);
        assertTrue(ReactorAutomationController.startupHealthy(o,P)); assertEquals(0,o.convertedCoolantMb());
        assertFalse(ReactorAutomationController.startupHealthy(observation(snapshot(0,0).withCoolantInventories(249,100),250),P));
        assertFalse(ReactorAutomationController.startupHealthy(observation(snapshot(0,0).withCoolantInventories(500,751),250),P));
    }
    @Test void effectiveHalfThresholdAndInventoryWarningsOnlyInsert() {
        var result=ReactorAutomationController.decide(running(50,9),observation(snapshot(.125,0),250),P);
        assertEquals(5,result.safeInsertPercent()); assertTrue(result.blockRodLift()); assertEquals("warning",result.reason());
        assertEquals(5,ReactorAutomationController.decide(running(50,9),observation(snapshot(0,0).withCoolantInventories(149,100),250),P).safeInsertPercent());
        assertEquals(ReactorAutomationState.Phase.FAULT,ReactorAutomationController.decide(running(50,0),observation(snapshot(0,0).withCoolantInventories(500,951),250),P).nextState().phase());
    }
    @Test void faultHealthNeedsStableTicksAndNeverRestartsByItself() {
        var state=running(100,0).stop("duty_lost",true);
        for(int i=0;i<99;i++) state=ReactorAutomationController.decide(state,observation(snapshot(0,0),250),P).nextState();
        assertEquals(99,state.healthyAge());assertEquals(ReactorAutomationState.Phase.FAULT,state.phase());assertFalse(state.runPermission());
        state=ReactorAutomationController.decide(state,observation(snapshot(0,0),250),P).nextState();assertEquals(100,state.healthyAge());
        state=ReactorAutomationController.decide(state,observation(snapshot(0,0).withCoolantInventories(1,0),250),P).nextState();assertEquals(0,state.healthyAge());
    }
    @Test void armingChecksRedstoneAndTimesOutWithoutRodLift() {
        var s=new ReactorAutomationState(true,ReactorAutomationState.Phase.ARMING,"arming",100,1,0,0,0,0,ReactorAutomationState.Mode.CONSTANT,true);
        var o=observation(snapshot(0,0),250);
        var high=new ReactorAutomationObservation(o.snapshot(),o.reactorParameters(),o.coolantInput(),0,0,false,true,true,true,0,0,o.settings(),o.curve());
        for(int i=0;i<39;i++) s=ReactorAutomationController.decide(s,high,P).nextState();
        assertEquals(ReactorAutomationState.Phase.ARMING,s.phase());assertEquals(100,s.commandedDepthPercent());
        assertEquals(ReactorAutomationState.Phase.FAULT,ReactorAutomationController.decide(s,high,P).nextState().phase());
        assertEquals(ReactorAutomationState.Phase.CONSTANT,ReactorAutomationController.decide(s,o,P).nextState().phase());
    }
    @Test void cacheIgnoresInventoryAndBurnNumbersButTracksActualFissionInputs() {
        var cache=new ReactorAutomationController.CurveCache();var s=snapshot(0,0);var params=ReactorSimulationParameters.defaults();
        var first=cache.curve(s,params);assertSame(first,cache.curve(s.withCoolantInventories(300,200),params));
        var pos=new CoreColumnPosition(1,1);var fuel=s.fuelColumns().get(pos);
        var changedFuel=new FuelColumnState(FuelAssemblyState.installed(216000,1),1,.24,.5,.24);
        assertSame(first,cache.curve(s.withFuelColumn(pos,changedFuel),params));
        assertNotSame(first,cache.curve(s.withFuelColumn(pos,fuel.withIntegrity(.9)),params));
        var exhausted=s.withFuelColumn(pos,new FuelColumnState(FuelAssemblyState.installed(216000,216000),1,0));
        assertEquals(0,cache.curve(exhausted,params)[0]);
    }
    @Test void realOverclockDamageAndExponentUseOriginalCalculator() {
        var params=ReactorSimulationParameters.defaults();
        var nonlinear=new ReactorSimulationParameters(1000,params.burnHoursPerBlock(),params.damageHeatThresholdHuPerTick(),params.damageRatePerTickHuLoad(),2,3,params.damageTransferRate(),0,params.meltdownTriggerFraction(),900,2,10,10,.15,.5,20);
        var s=snapshot(0,0);var fuel=new CoreColumnPosition(2,1);var rods=new java.util.TreeMap<>(s.controlRodColumns());rods.put(new CoreColumnPosition(2,0),ControlRodColumnState.fullyInserted());
        s=s.withColumns(s.fuelColumns(),rods).withFuelColumn(fuel,new FuelColumnState(FuelAssemblyState.installed(1000,0),.9,0));
        var c=ReactorAutomationController.predictCurve(s,nonlinear);
        assertTrue(c[50]<c[0]*.5); assertEquals(0,c[100]); assertTrue(c[0]>6000);
        var originalRods=new java.util.TreeMap<>(s.controlRodColumns());originalRods.replaceAll((p,r)->new ControlRodColumnState(r.integrity(),.5,.5,false,0));
        assertEquals(ReactorFissionCalculator.calculate(s.withColumns(s.fuelColumns(),originalRods),nonlinear).generatedHeatHu(),c[50]);
    }
    @Test void zeroThresholdNeverDividesAndZeroCapacityCannotStart() {
        var base=observation(snapshot(.001,0),250);var d=ReactorSimulationParameters.defaults();
        var zero=new ReactorSimulationParameters(d.baseHeatPerFuelBlockHuPerTick(),d.burnHoursPerBlock(),0,d.damageRatePerTickHuLoad(),2,3,d.damageTransferRate(),0,d.meltdownTriggerFraction(),900,1,10,10,.15,.5,20);
        var o=new ReactorAutomationObservation(base.snapshot(),zero,base.coolantInput(),0,0,false,true,true,false,0,0,base.settings(),base.curve());
        assertEquals("heat_trip",ReactorAutomationController.tripReason(o,P));
        var noCapacity=new ReactorAutomationObservation(snapshot(0,0),d,ReactorServerTick.CoolantInput.none(),0,0,false,true,true,false,0,0,base.settings(),base.curve());
        assertFalse(ReactorAutomationController.startupHealthy(noCapacity,P));
    }
    @Test void protectionTripsOnLostDutyCoverageJamsDamageMeltdownAndLease() {
        var base=observation(snapshot(0,0),250);
        assertEquals("duty_lost",ReactorAutomationController.tripReason(new ReactorAutomationObservation(base.snapshot(),base.reactorParameters(),base.coolantInput(),0,0,false,true,false,false,0,0,base.settings(),base.curve()),P));
        assertEquals("binding_lost",ReactorAutomationController.tripReason(new ReactorAutomationObservation(base.snapshot(),base.reactorParameters(),base.coolantInput(),0,0,false,true,true,false,0,21,base.settings(),base.curve()),P));
        assertEquals("damage",ReactorAutomationController.tripReason(new ReactorAutomationObservation(base.snapshot(),base.reactorParameters(),base.coolantInput(),0,0,true,true,true,false,0,0,base.settings(),base.curve()),P));
        assertEquals("meltdown",ReactorAutomationController.tripReason(observation(base.snapshot().withMeltdown(1,true),250),P));
        assertEquals("uncovered",ReactorAutomationController.tripReason(observation(base.snapshot().withColumns(base.snapshot().fuelColumns(),Map.of()),250),P));
        var rods=Map.of(new CoreColumnPosition(1,0),new ControlRodColumnState(0,.5,.5,true,0));
        assertEquals("jammed",ReactorAutomationController.tripReason(observation(base.snapshot().withColumns(base.snapshot().fuelColumns(),rods),250),P));
    }
    @Test void validatesAllSixteenDefaultsAndRejectsCrossFieldMisordering() {
        assertEquals(16,ReactorAutomationParameters.class.getRecordComponents().length);
        assertEquals(1,P.burnerMaintenanceHuPerTick());
        for(double cost:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY,1000001})
            assertThrows(IllegalArgumentException.class,()->new ReactorAutomationParameters(10,40,1,5,.02,1000000,.25,.75,.15,.85,.95,.5,100,20,40,cost));
        assertEquals(10,P.adjustIntervalTicks());assertEquals(40,P.displayAverageWindowTicks());assertEquals(1,P.maxLiftPercentPerAdjustment());assertEquals(5,P.maxInsertPercentPerAdjustment());
        assertEquals(1000000,P.maxTargetHeatHuPerTick());assertEquals(.02,P.powerDeadbandFraction());assertEquals(.25,P.startupColdFraction());assertEquals(.75,P.startupMaxHotFraction());assertEquals(.15,P.warningColdFraction());assertEquals(.85,P.warningHotFraction());assertEquals(.95,P.tripHotFraction());assertEquals(.5,P.warningHeatThresholdFraction());assertEquals(100,P.resetStableTicks());assertEquals(20,P.controlLeaseTicks());assertEquals(40,P.armingTimeoutTicks());
        assertThrows(IllegalArgumentException.class,()->new ReactorAutomationParameters(0,40,1,5,.02,1000000,.25,.75,.15,.85,.95,.5,100,20,40));
        assertThrows(IllegalArgumentException.class,()->new ReactorAutomationParameters(10,40,1,5,.02,1000000,.25,.75,.3,.85,.95,.5,100,20,40));
    }
    @Test void warningClampsPendingLiftEveryTickBeforeAdjustmentPeriod() {
        var base=snapshot(.125,0);var rods=new java.util.TreeMap<>(base.controlRodColumns());
        rods.put(new CoreColumnPosition(1,0),new ControlRodColumnState(1,0,.4,false,0));
        rods.put(new CoreColumnPosition(2,0),new ControlRodColumnState(1,.9,.8,false,0));
        var sample=base.withColumns(base.fuelColumns(),rods);
        var d=ReactorAutomationController.decide(running(50,8),observation(sample,250),P);
        assertTrue(d.blockRodLift());assertEquals(0,d.safeInsertPercent());assertTrue(d.depthCommand().isEmpty());
        var clamped=ReactorAutomationController.safetySnapshot(sample,0);
        assertEquals(.4,clamped.controlRodColumns().get(new CoreColumnPosition(1,0)).targetDepth());
        assertEquals(.9,clamped.controlRodColumns().get(new CoreColumnPosition(2,0)).targetDepth());
        var tenth=ReactorAutomationController.decide(running(50,9),observation(sample,250),P);
        assertEquals(5,tenth.safeInsertPercent());assertTrue(tenth.depthCommand().isEmpty());
        var inserted=ReactorAutomationController.safetySnapshot(sample,5);
        assertEquals(.45,inserted.controlRodColumns().get(new CoreColumnPosition(1,0)).targetDepth(),1e-9);
        assertEquals(.95,inserted.controlRodColumns().get(new CoreColumnPosition(2,0)).targetDepth(),1e-9);
    }
    @Test void manualNextTickCoolingUsesPendingTargetsInsteadOfLastFullDepth() {
        var base=observation(snapshot(0,0).withCoolantInventories(1,0),250);
        var rods=new java.util.TreeMap<>(base.snapshot().controlRodColumns());
        rods.replaceAll((p,r)->ControlRodStateTransitions.requestTargetDepth(r,0));
        var snapshot=base.snapshot().withColumns(base.snapshot().fuelColumns(),rods);
        var o=new ReactorAutomationObservation(snapshot,base.reactorParameters(),new ReactorServerTick.CoolantInput(base.coolantInput().ports(),2,.001),0,0,false,true,true,false,0,0,base.settings(),base.curve());
        assertTrue(ReactorAutomationController.coolantCanCarryNextTick(o,100));
        assertFalse(ReactorAutomationController.coolantCanCarryPendingTick(o));
        var state=new ReactorAutomationState(true,ReactorAutomationState.Phase.MANUAL,"manual",100,1,0,0,0,0,ReactorAutomationState.Mode.MANUAL,true);
        assertEquals(ReactorAutomationState.Phase.FAULT,ReactorAutomationController.decide(state,o,P).nextState().phase());
    }
}
