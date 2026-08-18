package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorHeatPropagationTest {
    private static final ReactorSimulationParameters PARAMETERS = ReactorSimulationParameters.defaults();
    private static final CoreColumnPosition SOURCE = new CoreColumnPosition(1, 1);
    private static final CoreColumnPosition NORTH = new CoreColumnPosition(1, 0);
    private static final CoreColumnPosition EAST = new CoreColumnPosition(2, 1);
    private static final CoreColumnPosition DIAGONAL = new CoreColumnPosition(2, 0);

    @Test
    void failedFuelSplitsTransferEquallyAcrossFourWayFuelAndControlTargets() {
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(
                        SOURCE, fuel(0.0D, 4.0D),
                        NORTH, fuel(1.0D, 0.0D),
                        DIAGONAL, fuel(1.0D, 0.0D)
                ),
                Map.of(EAST, new ControlRodColumnState(1.0D, 0.35D, 0.35D, false, 0.0D)),
                0L,
                0L,
                0L,
                false
        );

        HeatPropagationResult result = ReactorHeatPropagation.propagate(snapshot, Map.of(), PARAMETERS);

        assertEquals(1.0D, result.totalTransferredHeatHu(), 1.0E-12D);
        assertEquals(0.5D, result.receivedHeatHu().get(NORTH), 1.0E-12D);
        assertEquals(0.5D, result.receivedHeatHu().get(EAST), 1.0E-12D);
        assertFalse(result.receivedHeatHu().containsKey(DIAGONAL));
        assertEquals(3.0D, result.snapshot().fuelColumns().get(SOURCE).cachedHeatHu(), 1.0E-12D);
        assertEquals(0.5D, result.snapshot().fuelColumns().get(NORTH).cachedHeatHu(), 1.0E-12D);
        assertEquals(0.5D, result.snapshot().controlRodColumns().get(EAST).cachedHeatHu(), 1.0E-12D);
    }

    @Test
    void targetCoolingBlocksPropagationDamageAndCachedHeat() {
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(SOURCE, fuel(0.0D, 4.0D), NORTH, fuel(1.0D, 0.0D)),
                Map.of(),
                0L,
                0L,
                0L,
                false
        );

        HeatPropagationResult result = ReactorHeatPropagation.propagate(
                snapshot,
                Map.of(NORTH, 1.0D),
                PARAMETERS
        );

        assertEquals(1.0D, result.receivedHeatHu().get(NORTH), 1.0E-12D);
        assertEquals(1.0D, result.removedHeatHu().get(NORTH), 1.0E-12D);
        assertEquals(0.0D, result.netReceivedHeatHu().get(NORTH), 1.0E-12D);
        assertEquals(1.0D, result.snapshot().fuelColumns().get(NORTH).integrity(), 1.0E-12D);
        assertEquals(0.0D, result.snapshot().fuelColumns().get(NORTH).cachedHeatHu(), 1.0E-12D);
        assertTrue(result.coveredEffectiveFuelColumns().isEmpty());
    }

    @Test
    void gapsAndDiagonalColumnsDoNotReceiveHeat() {
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(SOURCE, fuel(0.0D, 4.0D), DIAGONAL, fuel(1.0D, 0.0D)),
                Map.of(),
                0L,
                0L,
                0L,
                false
        );

        HeatPropagationResult result = ReactorHeatPropagation.propagate(snapshot, Map.of(), PARAMETERS);

        assertEquals(0.0D, result.totalTransferredHeatHu(), 1.0E-12D);
        assertTrue(result.receivedHeatHu().isEmpty());
        assertEquals(4.0D, result.snapshot().fuelColumns().get(SOURCE).cachedHeatHu(), 1.0E-12D);
    }

    @Test
    void controlRodColumnsArePropagationEndpoints() {
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(EAST, fuel(1.0D, 0.0D)),
                Map.of(SOURCE, new ControlRodColumnState(0.0D, 0.4D, 0.4D, false, 4.0D)),
                0L,
                0L,
                0L,
                false
        );

        HeatPropagationResult result = ReactorHeatPropagation.propagate(snapshot, Map.of(), PARAMETERS);

        assertTrue(result.receivedHeatHu().isEmpty());
        assertEquals(0.0D, result.snapshot().fuelColumns().get(EAST).cachedHeatHu(), 1.0E-12D);
    }

    private static FuelColumnState fuel(double integrity, double cachedHeat) {
        return new FuelColumnState(
                FuelAssemblyState.installed(216_000, 0),
                integrity,
                cachedHeat
        );
    }
}
