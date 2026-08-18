package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlRodStateTransitionsTest {
    private static final double FAILURE_THRESHOLD = 0.0D;

    @Test
    void intactRodAcceptsSliderMovementAndScramTargets() {
        ControlRodColumnState initial = new ControlRodColumnState(1.0D, 0.2D, 0.2D, false, 0.0D);
        ControlRodColumnState slider = ControlRodStateTransitions.requestTargetDepth(initial, 0.6D);
        ControlRodColumnState moved = ControlRodStateTransitions.moveActualDepth(slider, 0.45D);
        ControlRodColumnState scrammed = ControlRodStateTransitions.requestScram(moved);

        assertEquals(0.6D, slider.targetDepth(), 1.0E-12D);
        assertEquals(0.2D, slider.actualDepth(), 1.0E-12D);
        assertEquals(0.45D, moved.actualDepth(), 1.0E-12D);
        assertEquals(1.0D, scrammed.targetDepth(), 1.0E-12D);
        assertFalse(scrammed.jammed());
    }

    @Test
    void integrityFailureJamsAtThePreviousActualDepth() {
        ControlRodColumnState initial = new ControlRodColumnState(0.1D, 0.8D, 0.35D, false, 2.0D);

        ControlRodColumnState jammed = ControlRodStateTransitions.applyIntegrityDamage(
                initial,
                0.2D,
                FAILURE_THRESHOLD
        );

        assertEquals(0.0D, jammed.integrity(), 1.0E-12D);
        assertEquals(0.35D, jammed.actualDepth(), 1.0E-12D);
        assertEquals(0.8D, jammed.targetDepth(), 1.0E-12D);
        assertTrue(jammed.jammed());
    }

    @Test
    void jammedRodRejectsSliderScramAndPhysicalMovement() {
        ControlRodColumnState jammed = new ControlRodColumnState(0.0D, 0.8D, 0.35D, true, 2.0D);

        assertSame(jammed, ControlRodStateTransitions.requestTargetDepth(jammed, 0.1D));
        assertSame(jammed, ControlRodStateTransitions.requestScram(jammed));
        assertSame(jammed, ControlRodStateTransitions.moveActualDepth(jammed, 1.0D));
    }

    @Test
    void jammedActualDepthStillSuppressesAdjacentIsolatedFuel() {
        CoreColumnPosition fuelPosition = new CoreColumnPosition(1, 1);
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(fuelPosition, new FuelColumnState(
                        FuelAssemblyState.installed(216_000, 0),
                        1.0D,
                        0.0D
                )),
                Map.of(new CoreColumnPosition(1, 0),
                        new ControlRodColumnState(0.0D, 1.0D, 0.35D, true, 0.0D)),
                0L,
                0L,
                0L,
                false
        );

        FuelColumnFissionResult fission = ReactorFissionCalculator.calculate(
                snapshot,
                ReactorSimulationParameters.defaults(),
                false
        ).columns().get(fuelPosition);

        assertEquals(0.65D, fission.controlledIntensity(), 1.0E-12D);
    }

    @Test
    void repairMustExceedFailureThresholdBeforeRodUnjams() {
        ControlRodColumnState jammed = new ControlRodColumnState(0.0D, 0.8D, 0.35D, true, 0.0D);
        ControlRodColumnState unchanged = ControlRodStateTransitions.repairIntegrity(
                jammed,
                0.0D,
                FAILURE_THRESHOLD
        );
        ControlRodColumnState repaired = ControlRodStateTransitions.repairIntegrity(
                unchanged,
                0.1D,
                FAILURE_THRESHOLD
        );

        assertTrue(unchanged.jammed());
        assertFalse(repaired.jammed());
        assertEquals(0.1D, repaired.integrity(), 1.0E-12D);
        assertEquals(0.35D, repaired.actualDepth(), 1.0E-12D);
    }
}
