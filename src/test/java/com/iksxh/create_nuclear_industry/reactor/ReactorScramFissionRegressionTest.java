package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorScramFissionRegressionTest {
    private static final ReactorSimulationParameters PARAMETERS = ReactorSimulationParameters.defaults();
    private static final CoreColumnPosition CENTER = new CoreColumnPosition(1, 1);

    @Test
    void scramFullyInsertsAdjacentMovableRodAndStopsAdjacentHeat() {
        CoreColumnPosition controlPosition = new CoreColumnPosition(1, 0);
        ReactorServerTick.Result result = ReactorServerTick.advance(
                scrammedSnapshot(
                        CENTER,
                        controlPosition,
                        new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D)
                ),
                PARAMETERS,
                ReactorServerTick.CoolantInput.none()
        );

        assertEquals(1.0D,
                result.controlSnapshot().controlRodColumns().get(controlPosition).actualDepth(),
                1.0E-12D);
        assertEquals(0.0D, result.fission().columns().get(CENTER).generatedHeatHu(), 1.0E-12D);
        assertEquals(0.0D,
                result.fission().columns().get(CENTER).plannedFuelBurnUnits(),
                1.0E-15D);
    }

    @Test
    void scramKeepsPartiallyInsertedJammedAdjacentRodFission() {
        CoreColumnPosition controlPosition = new CoreColumnPosition(1, 0);
        ReactorServerTick.Result result = ReactorServerTick.advance(
                scrammedSnapshot(
                        CENTER,
                        controlPosition,
                        new ControlRodColumnState(0.0D, 0.35D, 0.35D, true, 0.0D)
                ),
                PARAMETERS,
                ReactorServerTick.CoolantInput.none()
        );

        assertTrue(result.controlSnapshot().controlRodColumns().get(controlPosition).jammed());
        assertEquals(0.35D,
                result.controlSnapshot().controlRodColumns().get(controlPosition).actualDepth(),
                1.0E-12D);
        assertEquals(1.95D, result.fission().columns().get(CENTER).generatedHeatHu(), 1.0E-12D);
        assertTrue(result.fission().columns().get(CENTER).plannedFuelBurnUnits() > 0.0D);
    }

    @Test
    void scramControlRodDoesNotAffectNonAdjacentFuel() {
        CoreColumnPosition fuelPosition = new CoreColumnPosition(0, 0);
        CoreColumnPosition controlPosition = new CoreColumnPosition(2, 2);
        ReactorServerTick.Result result = ReactorServerTick.advance(
                scrammedSnapshot(
                        fuelPosition,
                        controlPosition,
                        new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D)
                ),
                PARAMETERS,
                ReactorServerTick.CoolantInput.none()
        );

        assertEquals(1.0D,
                result.controlSnapshot().controlRodColumns().get(controlPosition).actualDepth(),
                1.0E-12D);
        assertEquals(3.0D, result.fission().columns().get(fuelPosition).generatedHeatHu(), 1.0E-12D);
        assertTrue(result.fission().columns().get(fuelPosition).plannedFuelBurnUnits() > 0.0D);
    }

    private static ReactorSnapshot scrammedSnapshot(
            CoreColumnPosition fuelPosition,
            CoreColumnPosition controlPosition,
            ControlRodColumnState control
    ) {
        Map<CoreColumnPosition, Double> savedTargets = control.jammed()
                ? Map.of()
                : Map.of(controlPosition, control.targetDepth());
        return new ReactorSnapshot(
                Map.of(fuelPosition,
                        new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)),
                Map.of(controlPosition, control),
                0L,
                0L,
                0L,
                false,
                savedTargets,
                true
        );
    }
}
