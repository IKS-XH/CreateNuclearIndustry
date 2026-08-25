package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorControlRodTickTest {
    private static final ReactorSimulationParameters PARAMETERS = ReactorSimulationParameters.defaults();
    private static final CoreColumnPosition FUEL = new CoreColumnPosition(1, 1);
    private static final CoreColumnPosition MOVABLE = new CoreColumnPosition(1, 0);
    private static final CoreColumnPosition JAMMED = new CoreColumnPosition(0, 1);

    @Test
    void targetDepthBecomesActualDepthOnTheNextTickAndChangesAdjacentFission() {
        ReactorSnapshot before = new ReactorSnapshot(
                Map.of(FUEL, fuel()),
                Map.of(MOVABLE, new ControlRodColumnState(1.0D, 0.25D, 0.0D, false, 0.0D)),
                0L,
                0L,
                0L,
                false
        );

        assertEquals(3.0D,
                ReactorFissionCalculator.calculate(before, PARAMETERS, false)
                        .columns().get(FUEL).generatedHeatHu(),
                1.0E-12D);

        ReactorSnapshot after = ReactorControlRodTick.advance(before);
        assertEquals(0.25D, after.controlRodColumns().get(MOVABLE).targetDepth(), 1.0E-12D);
        assertEquals(0.25D, after.controlRodColumns().get(MOVABLE).actualDepth(), 1.0E-12D);
        assertEquals(2.25D,
                ReactorFissionCalculator.calculate(after, PARAMETERS, false)
                        .columns().get(FUEL).generatedHeatHu(),
                1.0E-12D);
    }

    @Test
    void scramLocksMovableRodsAndJammedRodsRemainUnchanged() {
        ControlRodColumnState jammed = new ControlRodColumnState(0.0D, 0.8D, 0.35D, true, 1.5D);
        ReactorSnapshot before = new ReactorSnapshot(
                Map.of(FUEL, fuel()),
                Map.of(
                        MOVABLE, new ControlRodColumnState(1.0D, 0.2D, 0.1D, false, 0.0D),
                        JAMMED, jammed
                ),
                0L,
                0L,
                0L,
                false,
                Map.of(MOVABLE, 0.2D),
                true
        );

        ReactorSnapshot after = ReactorControlRodTick.advance(before);

        ControlRodColumnState moved = after.controlRodColumns().get(MOVABLE);
        assertTrue(after.scramActive());
        assertEquals(1.0D, moved.targetDepth(), 1.0E-12D);
        assertEquals(1.0D, moved.actualDepth(), 1.0E-12D);
        assertSame(jammed, after.controlRodColumns().get(JAMMED));
        assertEquals(0.8D, after.controlRodColumns().get(JAMMED).targetDepth(), 1.0E-12D);
        assertEquals(0.35D, after.controlRodColumns().get(JAMMED).actualDepth(), 1.0E-12D);
    }

    @Test
    void stableSnapshotIsNotRewrittenWhenNoControlRodNeedsActuation() {
        ReactorSnapshot snapshot = ReactorSnapshot.singleControlRodColumn(
                MOVABLE, ControlRodColumnState.fullyInserted());

        assertSame(snapshot, ReactorControlRodTick.advance(snapshot));
    }

    private static FuelColumnState fuel() {
        return new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D);
    }
}
