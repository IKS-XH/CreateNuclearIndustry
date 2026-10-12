package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;
import java.util.Map;
import com.iksxh.create_nuclear_industry.structure.ReactorDimensions;
import com.iksxh.create_nuclear_industry.content.ModItems;
import net.minecraft.world.item.ItemStack;
import static org.junit.jupiter.api.Assertions.*;

/** 验证实际高度贯穿NBT和纯状态转换，产热与燃耗只线性缩放一次。 */
class ReactorVariableThermalTest {
    @Test void savedActualHeightDrivesFissionAndFixedComponentBurn() {
        var fuel = new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1, 0);
        var original = ReactorSnapshot.singleFuelColumn(new CoreColumnPosition(0, 0), fuel);
        var defaultHeat = ReactorFissionCalculator.calculate(original, ReactorSimulationParameters.defaults());
        assertEquals(3, defaultHeat.generatedHeatHu(), 1e-12);
        assertEquals(3.0 / 216_000, defaultHeat.plannedFuelBurnUnits(), 1e-12);
        var tag = ReactorSnapshotNbtCodec.encode(original);
        tag.putInt("Width", 6); tag.putInt("Height", 16); tag.putInt("Depth", 7);
        var tall = ReactorSnapshotNbtCodec.decode(tag).withFuelColumn(new CoreColumnPosition(0, 0), fuel);
        var tallHeat = ReactorFissionCalculator.calculate(tall, ReactorSimulationParameters.defaults());
        assertEquals(14, tallHeat.generatedHeatHu(), 1e-12);
        assertEquals(14.0 / 216_000, tallHeat.plannedFuelBurnUnits(), 1e-12);
        assertEquals(1, tall.fuelColumns().size());
        assertEquals(16, ReactorSnapshotNbtCodec.encode(tall.withCoolantInventories(11, 22)
                .withColumns(tall.fuelColumns(), Map.of()).withoutFuelAssemblies()
                .withMeltdown(2, true).withMeltdownEventPublished(true).withScramState(Map.of(), false)).getInt("Height"));
    }

    @Test void maximumColumnCoordinateSurvivesCurrentNbtRoundTrip() {
        var tag = ReactorSnapshotNbtCodec.encode(ReactorSnapshot.empty());
        tag.putInt("Width", 16); tag.putInt("Height", 16); tag.putInt("Depth", 16);
        var tall = ReactorSnapshotNbtCodec.decode(tag).withFuelColumn(new CoreColumnPosition(13, 13), FuelColumnState.empty());
        assertEquals(tall, ReactorSnapshotNbtCodec.decode(ReactorSnapshotNbtCodec.encode(tall)));
        assertEquals(16, ReactorSnapshotNbtCodec.encode(tall).getInt("Width"));
    }

    @Test void bothRepairTransactionsScaleInverselyWithHeightAndConsumeOnePlate() {
        var fuel=FuelColumnState.empty().withIntegrity(.5);
        var rod=new ControlRodColumnState(.5,.3,.3,false,0);
        var plates=new ItemStack(ModItems.STEEL_PLATE.get(),2);
        assertEquals(.5+.25/3,FuelColumnRepairTransaction.repair(fuel,plates,0,3).nextColumn().integrity(),1e-12);
        assertEquals(.5+.25/3,ControlRodColumnRepairTransaction.repair(rod,plates,0,3).nextColumn().integrity(),1e-12);
        var fuelTall=FuelColumnRepairTransaction.repair(fuel,plates,0,14);
        var rodTall=ControlRodColumnRepairTransaction.repair(rod,plates,0,14);
        assertEquals(.5+.25/14,fuelTall.nextColumn().integrity(),1e-12);
        assertEquals(.5+.25/14,rodTall.nextColumn().integrity(),1e-12);
        assertEquals(1,fuelTall.remainingInput().getCount());
        assertEquals(1,rodTall.remainingInput().getCount());
        assertEquals(2,plates.getCount());
    }

    @Test void separatedFuelClustersAndRectangularNeighboursRoundWholeHeatOnce() {
        var fuel=new FuelColumnState(FuelAssemblyState.installed(216_000,0),1,0);
        var state=new ReactorSnapshot(Map.of(new CoreColumnPosition(0,0),fuel,new CoreColumnPosition(2,0),fuel),
                Map.of(),0,0,0,false,Map.of(),false,false,new ReactorDimensions(6,16,7));
        var parameters=new ReactorSimulationParameters(.1,3,.25,.0000005,2,3,.25,0,.2,900,1,10,10,.15,.5,20);
        var result=ReactorFissionCalculator.calculate(state,parameters);
        assertEquals(2.8,result.rawHeatHu(),1e-12);
        assertEquals(3,result.generatedHeatHu(),1e-12);
        assertEquals(3,result.columns().values().stream().mapToDouble(FuelColumnFissionResult::generatedHeatHu).sum(),1e-12);
        assertEquals(28.0/216_000,result.plannedFuelBurnUnits(),1e-12);
        assertFalse(result.columns().values().stream().anyMatch(FuelColumnFissionResult::overclocked));
        assertEquals(3,new CoreColumnPosition(3,3).cardinalNeighbours(new ReactorDimensions(6,8,7)).size());
    }

    /** 最大实际高度仍按整数mB结算，未取走的小数热保留且冷、热总量守恒。 */
    @Test void actualRectangleTickPreservesDimensionsCoolantAndFractionalHeat() {
        var position = new CoreColumnPosition(3, 4);
        var dimensions = new ReactorDimensions(6, 16, 7);
        var before = new ReactorSnapshot(Map.of(position,
                new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1, .75)),
                Map.of(new CoreColumnPosition(2, 4), ControlRodColumnState.fullyInserted()),
                128, 11, 0, false, Map.of(), false, false, dimensions);
        var input = new ReactorServerTick.CoolantInput(ReactorCoolantLedger.summarizePorts(
                java.util.List.of(ReactorCoolantLedger.Port.cold("cold", 128),
                        ReactorCoolantLedger.Port.hot("hot", 128)), 128), 1_000, .5);
        var result = ReactorServerTick.advance(before, ReactorSimulationParameters.defaults(), input);
        assertEquals(dimensions, result.snapshot().dimensions());
        assertEquals(0, result.fission().generatedHeatHu());
        assertEquals(1, result.coolant().settlement().convertedCoolantMb());
        assertEquals(127, result.snapshot().coldCoolantMb());
        assertEquals(12, result.snapshot().hotCoolantMb());
        assertEquals(139, result.snapshot().coldCoolantMb() + result.snapshot().hotCoolantMb());
        assertEquals(.25, result.snapshot().fuelColumns().get(position).cachedHeatHu(), 1e-12);
        assertEquals(.25, result.snapshot().fuelColumns().get(position).quantizedHeatRemainderHu(), 1e-12);
        assertEquals(1, result.snapshot().fuelColumns().get(position).integrity());
    }

    /** 最大截面远端的水平邻接进入控制和传播，不能退回默认3×3列图。 */
    @Test void farColumnsUseActualNeighboursForControlPropagationAndMeltdown() {
        var source = new CoreColumnPosition(12, 13);
        var target = new CoreColumnPosition(13, 13);
        var fuel = new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1, 0);
        var state = new ReactorSnapshot(Map.of(target, fuel),
                Map.of(source, ControlRodColumnState.fullyInserted()),
                0, 0, 0, false, Map.of(), false, false, new ReactorDimensions(16, 16, 16));
        assertEquals(0, ReactorFissionCalculator.calculate(state, ReactorSimulationParameters.defaults()).generatedHeatHu());
        var damaged = state.withColumns(Map.of(source, new FuelColumnState(fuel.fuelAssembly(), 0, 4), target, fuel), Map.of());
        var result = ReactorServerTick.advance(damaged, ReactorSimulationParameters.defaults(), ReactorServerTick.CoolantInput.none());
        assertTrue(result.propagation().coveredEffectiveFuelColumns().contains(target));
        assertTrue(result.snapshot().fuelColumns().get(target).integrity() < 1);
        assertTrue(result.snapshot().meltdownCountdownStarted());
        assertEquals(state.dimensions(), result.snapshot().dimensions());
    }
}
