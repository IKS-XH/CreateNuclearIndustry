package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证控制棒深度、损伤产热/燃耗倍率、四向燃料反馈、SCRAM 和热上限。 */
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
    void zeroIntegrityWithRemainingFuelUsesIndependentHeatAndBurnMultipliers() {
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

        assertEquals(1.5D, result.columns().get(damaged).damageHeatMultiplier(), 1.0E-12D);
        assertEquals(2.0D, result.columns().get(damaged).damageBurnMultiplier(), 1.0E-12D);
        assertEquals(1.5D,
                result.columns().get(damaged).generatedHeatHu() / result.columns().get(full).generatedHeatHu(),
                1.0E-12D);
        assertEquals(2.0D,
                result.columns().get(damaged).plannedFuelBurnUnits()
                        / result.columns().get(full).plannedFuelBurnUnits(),
                1.0E-12D);
        assertEquals(2.0D, result.columns().get(failed).damageHeatMultiplier(), 1.0E-12D);
        assertEquals(3.0D, result.columns().get(failed).damageBurnMultiplier(), 1.0E-12D);
        assertEquals(2.0D,
                result.columns().get(failed).generatedHeatHu() / result.columns().get(full).generatedHeatHu(),
                1.0E-12D);
        assertEquals(3.0D,
                result.columns().get(failed).plannedFuelBurnUnits()
                        / result.columns().get(full).plannedFuelBurnUnits(),
                1.0E-12D);
    }

    @Test
    void defaultDamageCurveMatchesAllFiveIntegritySamples() {
        double[] integrities = {1.0D, 0.75D, 0.5D, 0.25D, 0.0D};
        double[] expectedHeat = {1.0D, 1.25D, 1.5D, 1.75D, 2.0D};
        double[] expectedBurn = {1.0D, 1.5D, 2.0D, 2.5D, 3.0D};
        CoreColumnPosition position = new CoreColumnPosition(1, 1);

        for (int i = 0; i < integrities.length; i++) {
            FuelColumnFissionResult result = ReactorFissionCalculator.calculate(
                    ReactorSnapshot.singleFuelColumn(position, fuel(integrities[i])), PARAMETERS
            ).columns().get(position);
            assertEquals(expectedHeat[i], result.damageHeatMultiplier(), 1.0E-12D);
            assertEquals(expectedBurn[i], result.damageBurnMultiplier(), 1.0E-12D);
            assertEquals(3.0D * expectedHeat[i], result.generatedHeatHu(), 1.0E-12D);
            assertEquals(PARAMETERS.baseBurnPerFuelBlockPerTick() * 3.0D * expectedBurn[i],
                    result.plannedFuelBurnUnits(), 1.0E-15D);
            assertEquals(expectedHeat[i] / expectedBurn[i],
                    result.damageHeatMultiplier() / result.damageBurnMultiplier(), 1.0E-12D);
        }
    }

    @Test
    void customDamageEndpointsUseIndependentLinearCurves() {
        ReactorSimulationParameters parameters = parameters(1.5D, 2.5D);
        CoreColumnPosition position = new CoreColumnPosition(1, 1);

        FuelColumnFissionResult halfDamage = ReactorFissionCalculator.calculate(
                ReactorSnapshot.singleFuelColumn(position, fuel(0.5D)), parameters
        ).columns().get(position);
        FuelColumnFissionResult fullDamage = ReactorFissionCalculator.calculate(
                ReactorSnapshot.singleFuelColumn(position, fuel(0.0D)), parameters
        ).columns().get(position);

        assertEquals(1.25D, halfDamage.damageHeatMultiplier(), 1.0E-12D);
        assertEquals(1.75D, halfDamage.damageBurnMultiplier(), 1.0E-12D);
        assertEquals(1.5D, fullDamage.damageHeatMultiplier(), 1.0E-12D);
        assertEquals(2.5D, fullDamage.damageBurnMultiplier(), 1.0E-12D);
        assertTrue(halfDamage.damageHeatMultiplier() < halfDamage.damageBurnMultiplier());
        assertTrue(fullDamage.damageHeatMultiplier() < fullDamage.damageBurnMultiplier());
    }

    @Test
    void overclockAndDamageUseSeparateMultipliersBeforeHeatCap() {
        ReactorSimulationParameters parameters = parameters(3.0D, 5.0D);
        CoreColumnPosition first = new CoreColumnPosition(1, 1);
        CoreColumnPosition second = new CoreColumnPosition(2, 1);
        ReactorSnapshot fullSnapshot = new ReactorSnapshot(
                Map.of(first, fuel(1.0D), second, fuel(1.0D)), Map.of(), 0L, 0L, 0L, false);
        ReactorSnapshot damagedSnapshot = new ReactorSnapshot(
                Map.of(first, fuel(0.5D), second, fuel(0.5D)), Map.of(), 0L, 0L, 0L, false);

        FuelColumnFissionResult full = ReactorFissionCalculator.calculate(fullSnapshot, parameters)
                .columns().get(first);
        FuelColumnFissionResult damaged = ReactorFissionCalculator.calculate(damagedSnapshot, parameters)
                .columns().get(first);

        assertTrue(full.overclocked());
        assertTrue(damaged.overclocked());
        assertEquals(2.0D, damaged.generatedHeatHu() / full.generatedHeatHu(), 1.0E-12D);
        assertEquals(3.0D, damaged.plannedFuelBurnUnits() / full.plannedFuelBurnUnits(), 1.0E-12D);
    }

    @Test
    void heatCapReducesHeatOnlyAndDoesNotRefundPlannedFuelBurn() {
        CoreColumnPosition position = new CoreColumnPosition(1, 1);
        ReactorSimulationParameters capped = parameters(
                2.0D, 3.0D, 1.0D);
        FuelColumnFissionResult result = ReactorFissionCalculator.calculate(
                ReactorSnapshot.singleFuelColumn(position, fuel(0.0D)), capped
        ).columns().get(position);
        double expectedBurn = capped.baseBurnPerFuelBlockPerTick() * 3.0D * 3.0D;

        assertEquals(3.0D, result.generatedHeatHu(), 1.0E-12D);
        assertEquals(expectedBurn, result.plannedFuelBurnUnits(), 1.0E-15D);
    }

    @Test
    void adjacentFuelFeedbackUsesControlledPreviousRoundSignal() {
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
        assertEquals(0.0D, normal.columns().get(first).controlledIntensity(), 1.0E-12D);
        assertEquals(0.0D, normal.columns().get(first).heatIntensity(), 1.0E-12D);
        assertEquals(0.0D, normal.columns().get(first).burnIntensity(), 1.0E-12D);
        assertEquals(0.0D, normal.columns().get(first).generatedHeatHu(), 1.0E-12D);
        assertEquals(3.0D, normal.columns().get(second).generatedHeatHu(), 1.0E-12D);
        assertEquals(PARAMETERS.baseBurnPerFuelBlockPerTick() * 3.0D,
                normal.columns().get(second).plannedFuelBurnUnits(), 1.0E-15D);
        assertEquals(normal.columns().get(first).generatedHeatHu(),
                scrammed.columns().get(first).generatedHeatHu(), 1.0E-9D);
        assertEquals(normal.columns().get(second).generatedHeatHu(),
                scrammed.columns().get(second).generatedHeatHu(), 1.0E-9D);
    }

    @Test
    void threeRowFcfLayoutIsStrictlyMonotonicAndFullyInsertedStopsAllFission() {
        ReactorFissionResult withdrawn = ReactorFissionCalculator.calculate(fcfSnapshot(0.0D), PARAMETERS);
        ReactorFissionResult halfInserted = ReactorFissionCalculator.calculate(fcfSnapshot(0.5D), PARAMETERS);
        ReactorFissionResult fullyInserted = ReactorFissionCalculator.calculate(fcfSnapshot(1.0D), PARAMETERS);

        assertTrue(withdrawn.generatedHeatHu() > halfInserted.generatedHeatHu());
        assertTrue(halfInserted.generatedHeatHu() > fullyInserted.generatedHeatHu());
        assertTrue(withdrawn.plannedFuelBurnUnits() > halfInserted.plannedFuelBurnUnits());
        assertTrue(halfInserted.plannedFuelBurnUnits() > fullyInserted.plannedFuelBurnUnits());
        assertEquals(62.89770874243574D, withdrawn.generatedHeatHu(), 1.0E-12D);
        assertEquals(23.604377652134303D, halfInserted.generatedHeatHu(), 1.0E-12D);
        assertEquals(0.0002911930960297951D, withdrawn.plannedFuelBurnUnits(), 1.0E-15D);
        assertEquals(0.00010927952616728845D, halfInserted.plannedFuelBurnUnits(), 1.0E-15D);
        assertEquals(0.0D, fullyInserted.generatedHeatHu(), 1.0E-12D);
        assertEquals(0.0D, fullyInserted.plannedFuelBurnUnits(), 1.0E-15D);

        for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
            for (int x : new int[]{0, 2}) {
                CoreColumnPosition position = new CoreColumnPosition(x, z);
                FuelColumnFissionResult full = withdrawn.columns().get(position);
                FuelColumnFissionResult half = halfInserted.columns().get(position);
                FuelColumnFissionResult stopped = fullyInserted.columns().get(position);
                assertEquals(1.0D, full.controlledIntensity(), 1.0E-12D);
                assertEquals(0.5D, half.controlledIntensity(), 1.0E-12D);
                assertEquals(0.0D, stopped.controlledIntensity(), 1.0E-12D);
                assertTrue(full.generatedHeatHu() > half.generatedHeatHu());
                assertTrue(half.generatedHeatHu() > stopped.generatedHeatHu());
                assertTrue(full.plannedFuelBurnUnits() > half.plannedFuelBurnUnits());
                assertTrue(half.plannedFuelBurnUnits() > stopped.plannedFuelBurnUnits());
                assertEquals(0.0D, stopped.generatedHeatHu(), 1.0E-12D);
                assertEquals(0.0D, stopped.plannedFuelBurnUnits(), 1.0E-15D);
            }
        }
    }

    @Test
    void mixedFcfCoverageDoesNotCrossControlRodsOrEmptyCenter() {
        TreeMap<CoreColumnPosition, FuelColumnState> fuels = new TreeMap<>();
        TreeMap<CoreColumnPosition, ControlRodColumnState> controls = new TreeMap<>();
        for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
            fuels.put(new CoreColumnPosition(0, z), fuel(1.0D));
            fuels.put(new CoreColumnPosition(2, z), fuel(1.0D));
            double depth = z == 0 ? 1.0D : z == 1 ? 0.5D : 0.0D;
            controls.put(new CoreColumnPosition(1, z),
                    new ControlRodColumnState(1.0D, depth, depth, false, 0.0D));
        }
        ReactorFissionResult result = ReactorFissionCalculator.calculate(
                new ReactorSnapshot(fuels, controls, 0L, 0L, 0L, false), PARAMETERS);

        for (int x : new int[]{0, 2}) {
            assertEquals(0.0D,
                    result.columns().get(new CoreColumnPosition(x, 0)).generatedHeatHu(), 1.0E-12D);
            assertTrue(result.columns().get(new CoreColumnPosition(x, 1)).generatedHeatHu() > 0.0D);
            assertTrue(result.columns().get(new CoreColumnPosition(x, 2)).generatedHeatHu() > 0.0D);
        }
        assertEquals(result.columns().get(new CoreColumnPosition(0, 1)).generatedHeatHu(),
                result.columns().get(new CoreColumnPosition(2, 1)).generatedHeatHu(), 1.0E-12D);
        assertEquals(6, result.columns().size());
    }

    @Test
    void centerEmptyEightFuelAnchorUsesFourWayFeedbackWithoutControlState() {
        TreeMap<CoreColumnPosition, FuelColumnState> fuels = new TreeMap<>();
        for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
            for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
                if (x != 1 || z != 1) {
                    fuels.put(new CoreColumnPosition(x, z), fuel(1.0D));
                }
            }
        }
        ReactorFissionResult result = ReactorFissionCalculator.calculate(
                new ReactorSnapshot(fuels, Map.of(), 0L, 0L, 0L, false), PARAMETERS);

        assertEquals(8, result.columns().size());
        assertFalse(result.columns().containsKey(new CoreColumnPosition(1, 1)));
        assertTrue(result.generatedHeatHu() > 8.0D * 3.0D);
        assertTrue(result.plannedFuelBurnUnits()
                > 8.0D * PARAMETERS.baseBurnPerFuelBlockPerTick() * 3.0D);
    }

    @Test
    void emptyExhaustedAndUncontrolledColumnsFollowLocalControlOnly() {
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

        ReactorFissionResult result = ReactorFissionCalculator.calculate(snapshot, PARAMETERS);

        assertFalse(result.columns().get(empty).overclocked());
        assertEquals(0.0D, result.columns().get(empty).generatedHeatHu(), 1.0E-12D);
        assertEquals(0.0D, result.columns().get(exhausted).generatedHeatHu(), 1.0E-12D);
        assertEquals(3.0D, result.columns().get(isolated).generatedHeatHu(), 1.0E-12D);
        assertTrue(result.columns().get(isolated).plannedFuelBurnUnits() > 0.0D);
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

    private static ReactorSimulationParameters parameters(double heatMultiplier, double burnMultiplier) {
        return parameters(heatMultiplier, burnMultiplier, PARAMETERS.totalHeatMultiplierCap());
    }

    private static ReactorSimulationParameters parameters(
            double heatMultiplier,
            double burnMultiplier,
            double totalHeatMultiplierCap
    ) {
        return new ReactorSimulationParameters(
                PARAMETERS.baseHeatPerFuelBlockHuPerTick(),
                PARAMETERS.burnHoursPerBlock(),
                PARAMETERS.damageHeatThresholdHuPerTick(),
                PARAMETERS.damageRatePerTickHuLoad(),
                heatMultiplier,
                burnMultiplier,
                PARAMETERS.damageTransferRate(),
                PARAMETERS.controlRodFailureThreshold(),
                PARAMETERS.meltdownTriggerFraction(),
                PARAMETERS.meltdownCountdownTicks(),
                PARAMETERS.controlResponseExponent(),
                PARAMETERS.overclockHeatMultiplier(),
                PARAMETERS.overclockBurnMultiplier(),
                PARAMETERS.overclockFeedbackGain(),
                PARAMETERS.overclockFeedbackExponent(),
                totalHeatMultiplierCap
        );
    }

    /** 构造计划中固定的三行 F-C-F 堆芯：每行按 X 方向为燃料、控制棒、燃料。 */
    private static ReactorSnapshot fcfSnapshot(double rodDepth) {
        TreeMap<CoreColumnPosition, FuelColumnState> fuels = new TreeMap<>();
        TreeMap<CoreColumnPosition, ControlRodColumnState> controls = new TreeMap<>();
        for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
            fuels.put(new CoreColumnPosition(0, z), fuel(1.0D));
            fuels.put(new CoreColumnPosition(2, z), fuel(1.0D));
            controls.put(new CoreColumnPosition(1, z),
                    new ControlRodColumnState(1.0D, rodDepth, rodDepth, false, 0.0D));
        }
        return new ReactorSnapshot(fuels, controls, 0L, 0L, 0L, false);
    }
}
