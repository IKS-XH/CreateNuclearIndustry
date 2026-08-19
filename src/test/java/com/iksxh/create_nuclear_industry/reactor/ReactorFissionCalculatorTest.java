package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorFissionCalculatorTest {
    private static final ReactorSimulationParameters PARAMETERS = ReactorSimulationParameters.defaults();
    private static final int MAX_DAMAGE = 216_000;

    @Test
    void fullIntegrityIsolatedFuelUsesControlRodDepthForHeatAndBurn() {
        CoreColumnPosition fuelPosition = new CoreColumnPosition(1, 1);
        CoreColumnPosition controlPosition = new CoreColumnPosition(1, 0);
        ReactorSnapshot withdrawn = snapshot(
                fuelPosition,
                fuel(1.0D),
                controlPosition,
                new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D)
        );
        ReactorSnapshot inserted = snapshot(
                fuelPosition,
                fuel(1.0D),
                controlPosition,
                ControlRodColumnState.fullyInserted()
        );

        FuelColumnFissionResult fullPower = result(withdrawn, fuelPosition, false);
        FuelColumnFissionResult stopped = result(inserted, fuelPosition, false);

        assertEquals(1.0D, fullPower.controlledIntensity(), 1.0E-12D);
        assertEquals(3.0D, fullPower.generatedHeatHu(), 1.0E-12D);
        assertEquals(PARAMETERS.baseBurnPerFuelBlockPerTick() * 3.0D,
                fullPower.plannedFuelBurnUnits(), 1.0E-15D);
        assertEquals(0.0D, stopped.generatedHeatHu(), 1.0E-12D);
        assertEquals(0.0D, stopped.plannedFuelBurnUnits(), 1.0E-15D);
    }

    @Test
    void twoControlRodsUseMeanActualDepth() {
        CoreColumnPosition fuelPosition = new CoreColumnPosition(1, 1);
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(fuelPosition, fuel(1.0D)),
                Map.of(
                        new CoreColumnPosition(1, 0),
                        new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D),
                        new CoreColumnPosition(1, 2),
                        new ControlRodColumnState(1.0D, 1.0D, 1.0D, false, 0.0D)
                ),
                0L,
                0L,
                0L,
                false
        );

        FuelColumnFissionResult result = result(snapshot, fuelPosition, false);

        assertEquals(0.5D, result.controlledIntensity(), 1.0E-12D);
        assertEquals(1.5D, result.generatedHeatHu(), 1.0E-12D);
    }

    @Test
    void zeroIntegrityWithRemainingFuelKeepsTwoTimesHeatAndBurnMultiplier() {
        CoreColumnPosition full = new CoreColumnPosition(0, 0);
        CoreColumnPosition damaged = new CoreColumnPosition(2, 0);
        CoreColumnPosition failed = new CoreColumnPosition(1, 2);
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(full, fuel(1.0D), damaged, fuel(0.5D), failed, fuel(0.0D)),
                Map.of(),
                0L,
                0L,
                0L,
                false
        );
        ReactorFissionResult result = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, false);

        assertEquals(1.5D, result.columns().get(damaged).damageMultiplier(), 1.0E-12D);
        assertEquals(1.5D,
                result.columns().get(damaged).generatedHeatHu() / result.columns().get(full).generatedHeatHu(),
                1.0E-12D);
        assertEquals(1.5D,
                result.columns().get(damaged).plannedFuelBurnUnits()
                        / result.columns().get(full).plannedFuelBurnUnits(),
                1.0E-12D);
        assertEquals(2.0D, result.columns().get(failed).damageMultiplier(), 1.0E-12D);
        assertEquals(2.0D,
                result.columns().get(failed).generatedHeatHu() / result.columns().get(full).generatedHeatHu(),
                1.0E-12D);
        assertEquals(2.0D,
                result.columns().get(failed).plannedFuelBurnUnits()
                        / result.columns().get(full).plannedFuelBurnUnits(),
                1.0E-12D);
    }

    @Test
    void adjacentFuelUsesBoundedFeedbackAndIgnoresControlAndScram() {
        CoreColumnPosition first = new CoreColumnPosition(1, 1);
        CoreColumnPosition second = new CoreColumnPosition(2, 1);
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(first, fuel(1.0D), second, fuel(1.0D)),
                Map.of(new CoreColumnPosition(1, 0), ControlRodColumnState.fullyInserted()),
                0L,
                0L,
                0L,
                false
        );

        ReactorFissionResult normal = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, false);
        ReactorFissionResult scrammed = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, true);

        assertTrue(normal.columns().get(first).overclocked());
        assertTrue(normal.columns().get(first).heatIntensity() > 1.0D);
        assertTrue(normal.columns().get(first).heatIntensity() <= PARAMETERS.overclockHeatMultiplier());
        assertEquals(normal.columns().get(first).generatedHeatHu(),
                scrammed.columns().get(first).generatedHeatHu(), 1.0E-9D);
    }

    @Test
    void emptyExhaustedAndScrammedIsolatedColumnsDoNotGenerate() {
        CoreColumnPosition empty = new CoreColumnPosition(0, 0);
        CoreColumnPosition exhausted = new CoreColumnPosition(2, 0);
        CoreColumnPosition isolated = new CoreColumnPosition(1, 2);
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(
                        empty, FuelColumnState.empty(),
                        exhausted, new FuelColumnState(FuelAssemblyState.installed(MAX_DAMAGE, MAX_DAMAGE), 1.0D, 0.0D),
                        isolated, fuel(1.0D)
                ),
                Map.of(),
                0L,
                0L,
                0L,
                false
        );

        ReactorFissionResult result = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, true);

        assertFalse(result.columns().get(empty).overclocked());
        assertEquals(0.0D, result.columns().get(empty).generatedHeatHu(), 1.0E-12D);
        assertEquals(0.0D, result.columns().get(exhausted).generatedHeatHu(), 1.0E-12D);
        assertEquals(0.0D, result.columns().get(isolated).generatedHeatHu(), 1.0E-12D);
    }

    private static FuelColumnFissionResult result(
            ReactorSnapshot snapshot,
            CoreColumnPosition position,
            boolean scram
    ) {
        return ReactorFissionCalculator.calculate(snapshot, PARAMETERS, scram).columns().get(position);
    }

    private static ReactorSnapshot snapshot(
            CoreColumnPosition fuelPosition,
            FuelColumnState fuel,
            CoreColumnPosition controlPosition,
            ControlRodColumnState control
    ) {
        return new ReactorSnapshot(
                Map.of(fuelPosition, fuel),
                Map.of(controlPosition, control),
                0L,
                0L,
                0L,
                false
        );
    }

    private static FuelColumnState fuel(double integrity) {
        return new FuelColumnState(FuelAssemblyState.installed(MAX_DAMAGE, 0), integrity, 0.0D);
    }
}
