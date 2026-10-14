package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** 以独立数值检查新生优先、缓存比例和不足额原子性，不镜像实现。 */
class ReactorMaintenanceHeatTest {
    @Test void controlRodCacheIsRealMaintenanceHeatAndKeepsMechanicalState() {
        var rod=new CoreColumnPosition(1,1);
        var s=new ReactorSnapshot(Map.of(),Map.of(rod,new ControlRodColumnState(.7,.8,.4,true,2)),0,0,0,false);
        var paid=ReactorMaintenanceHeat.settle(s,fission(0,0),1);assertEquals(1,paid.paidHeatHu());assertEquals(1,paid.cachedExpenseHu());
        var tick=ReactorServerTick.advance(s,ReactorSimulationParameters.defaults(),ReactorServerTick.CoolantInput.none(),1);
        assertEquals(new ControlRodColumnState(.7,.8,.4,true,1),tick.snapshot().controlRodColumns().get(rod));
        assertEquals(0,tick.effectiveCoolingHeatHu());
    }
    @Test void mixedCachesShareExpenseAndInsufficientMixedHeatIsAtomic() {
        var rod=new CoreColumnPosition(1,1);var s=state(3,9).withColumns(state(3,9).fuelColumns(),Map.of(rod,new ControlRodColumnState(1,1,1,false,12)));
        var paid=ReactorMaintenanceHeat.settle(s,fission(0,0),6);
        assertEquals(6,paid.cachedExpenseHu());assertEquals(.75,paid.removedByColumn().get(A));assertEquals(2.25,paid.removedByColumn().get(B));
        assertEquals(3,paid.removedFromControlRods().get(rod));
        var shortState=state(.1,.1).withColumns(state(.1,.1).fuelColumns(),Map.of(rod,new ControlRodColumnState(1,1,1,false,.4)));
        assertEquals(0,ReactorMaintenanceHeat.settle(shortState,fission(0,0),1).paidHeatHu());
    }
    @Test void rodExpenseCannotDisplaceFuelCoolingOrPretendToPauseAccident() {
        var rod=new CoreColumnPosition(1,1);
        var s=new ReactorSnapshot(Map.of(A,new FuelColumnState(FuelAssemblyState.empty(),1,2)),Map.of(rod,new ControlRodColumnState(1,1,1,false,6)),100,0,0,false);
        var ports=ReactorCoolantLedger.summarizePorts(java.util.List.of(ReactorCoolantLedger.Port.cold("c",128),ReactorCoolantLedger.Port.hot("h",128)),128);
        var tick=ReactorServerTick.advance(s,ReactorSimulationParameters.defaults(),new ReactorServerTick.CoolantInput(ports,1000,1),4);
        assertEquals(4,tick.maintenance().paidHeatHu());assertEquals(1,tick.effectiveCoolingHeatHu());assertEquals(1,tick.snapshot().hotCoolantMb());assertEquals(3,tick.snapshot().controlRodColumns().get(rod).cachedHeatHu());
        var unsafe=new ReactorSnapshot(Map.of(A,new FuelColumnState(FuelAssemblyState.installed(216000,0),0,0)),Map.of(rod,new ControlRodColumnState(1,0,0,false,6)),0,0,0,true);
        var accident=ReactorServerTick.advance(unsafe,ReactorSimulationParameters.defaults(),ReactorServerTick.CoolantInput.none(),1);
        assertEquals(1,accident.maintenance().paidHeatHu());assertEquals(0,accident.effectiveCoolingHeatHu());assertEquals(MeltdownStatus.RUNNING,accident.meltdown().status());
    }
    private static final CoreColumnPosition A = new CoreColumnPosition(0, 0), B = new CoreColumnPosition(2, 2);
    private static ReactorSnapshot state(double a, double b) {
        return new ReactorSnapshot(Map.of(A, new FuelColumnState(FuelAssemblyState.empty(), 1, a),
                B, new FuelColumnState(FuelAssemblyState.empty(), 1, b)), Map.of(), 0, 0, 0, false);
    }
    private static ReactorFissionResult fission(double a, double b) {
        return new ReactorFissionResult(Map.of(A, new FuelColumnFissionResult(0,0,0,0,0,a,0,false),
                B, new FuelColumnFissionResult(0,0,0,0,0,b,0,false)),a+b,a+b,0);
    }
    @Test void newbornHeatPaysFirstWithoutTouchingOldCache() {
        var p = ReactorMaintenanceHeat.settle(state(10, 20), fission(2, 6), 4);
        assertEquals(4, p.paidHeatHu()); assertEquals(4, p.generatedExpenseHu()); assertEquals(0, p.cachedExpenseHu());
        assertEquals(1, p.removedByColumn().get(A)); assertEquals(3, p.removedByColumn().get(B));
    }
    @Test void cachePaysProportionallyAfterNewbornIsUsedUp() {
        var p = ReactorMaintenanceHeat.settle(state(3, 9), fission(.2, .8), 5);
        assertEquals(1,p.generatedExpenseHu()); assertEquals(4,p.cachedExpenseHu());
        assertEquals(1.2,p.removedByColumn().get(A),1e-12); assertEquals(3.8,p.removedByColumn().get(B),1e-12);
        assertEquals(5,p.removedByColumn().values().stream().mapToDouble(Double::doubleValue).sum(),1e-12);
    }
    @Test void insufficientHeatNeverPartiallyChargesOrGrantsDuty() {
        var p = ReactorMaintenanceHeat.settle(state(.1,.2),fission(.2,.3),1);
        assertEquals(0,p.paidHeatHu()); assertTrue(p.removedByColumn().isEmpty());
    }
    @Test void noRequestLeavesTheNativeHeatLedgerUntouched() {
        assertTrue(ReactorMaintenanceHeat.settle(state(3,9),fission(2,4),0).removedByColumn().isEmpty());
    }
    @Test void invalidRequestsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->ReactorMaintenanceHeat.settle(state(0,0),fission(0,0),Double.NaN));
        assertThrows(IllegalArgumentException.class,()->ReactorMaintenanceHeat.settle(state(0,0),fission(0,0),-1));
    }
    @Test void formalTickKeepsGrossHeatAndBurnButRemovesMaintenanceBeforeCooling() {
        var fuel = new CoreColumnPosition(1,1);
        var s = ReactorSnapshot.singleFuelColumn(fuel,new FuelColumnState(FuelAssemblyState.installed(216000,0),1,0))
                .withCoolantInventories(100,0);
        var p = ReactorSimulationParameters.defaults();
        var ports=ReactorCoolantLedger.summarizePorts(java.util.List.of(ReactorCoolantLedger.Port.cold("c",128),ReactorCoolantLedger.Port.hot("h",128)),128);
        var input=new ReactorServerTick.CoolantInput(ports,1000,1);
        var nativeTick=ReactorServerTick.advance(s,p,input);
        assertEquals(nativeTick,ReactorServerTick.advance(s,p,input,0));
        var tick=ReactorServerTick.advance(s,p,input,1);
        assertEquals(3,tick.fission().generatedHeatHu()); assertEquals(1,tick.maintenance().paidHeatHu());
        assertEquals(2,tick.effectiveCoolingHeatHu()); assertEquals(2,tick.snapshot().hotCoolantMb());
        assertEquals(nativeTick.snapshot().fuelColumns().get(fuel).fuelAssembly(),tick.snapshot().fuelColumns().get(fuel).fuelAssembly());
        assertEquals(0,tick.snapshot().fuelColumns().get(fuel).cachedHeatHu());
    }
    @Test void smallMaintenanceIsNotAccidentCooling() {
        var a=new CoreColumnPosition(1,1); var b=new CoreColumnPosition(1,2);
        var s=new ReactorSnapshot(Map.of(a,new FuelColumnState(FuelAssemblyState.installed(216000,0),0,100),
                b,new FuelColumnState(FuelAssemblyState.installed(216000,0),1,0)),Map.of(),0,0,0,false);
        var tick=ReactorServerTick.advance(s,ReactorSimulationParameters.defaults(),ReactorServerTick.CoolantInput.none(),1);
        assertEquals(1,tick.maintenance().paidHeatHu()); assertEquals(0,tick.effectiveCoolingHeatHu());
        assertEquals(MeltdownStatus.RUNNING,tick.meltdown().status()); assertEquals(1,tick.snapshot().meltdownProgressTicks());
    }
}
