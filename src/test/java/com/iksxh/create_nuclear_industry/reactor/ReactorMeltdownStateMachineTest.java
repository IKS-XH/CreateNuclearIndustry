package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证融毁覆盖集合口径、19/20/21% 边界、暂停、完成、单调进度和完整修复复位。 */
class ReactorMeltdownStateMachineTest {
    private static final ReactorSimulationParameters PARAMETERS = ReactorSimulationParameters.defaults();

    @Test
    void isolatedFailedFuelColumnStartsCountdownAtFullCoverage() {
        CoreColumnPosition position = new CoreColumnPosition(0, 0);
        ReactorSnapshot snapshot = ReactorSnapshot.singleFuelColumn(
                position,
                fuel(0.0D)
        );

        MeltdownUpdateResult result = update(snapshot, Set.of(position), false, false);

        assertEquals(1.0D, result.propagationCoverageFraction(), 1.0E-12D);
        assertTrue(result.dangerThresholdReached());
        assertEquals(MeltdownStatus.RUNNING, result.status());
        assertEquals(1L, result.snapshot().meltdownProgressTicks());
    }

    @Test
    void sourceAndTargetUnionCountsEachOfEightUsableFuelColumnsOnce() {
        Map<CoreColumnPosition, FuelColumnState> fuels = fuelMap(8);
        CoreColumnPosition source = fuels.keySet().iterator().next();
        CoreColumnPosition target = fuels.keySet().stream().skip(1).findFirst().orElseThrow();
        fuels.put(source, fuel(0.0D));
        ReactorSnapshot snapshot = new ReactorSnapshot(fuels, Map.of(), 0L, 0L, 0L, false);

        MeltdownUpdateResult below = update(snapshot, Set.of(source), false, false);
        MeltdownUpdateResult triggered = update(
                snapshot,
                new LinkedHashSet<>(java.util.List.of(source, target, source)),
                false,
                false
        );

        assertEquals(1.0D / 8.0D, below.propagationCoverageFraction(), 1.0E-12D);
        assertFalse(below.dangerThresholdReached());
        assertEquals(2.0D / 8.0D, triggered.propagationCoverageFraction(), 1.0E-12D);
        assertTrue(triggered.dangerThresholdReached());
        assertEquals(1L, triggered.snapshot().meltdownProgressTicks());
    }

    @Test
    void exhaustedFuelAndControlRodColumnsDoNotEnterCoverageDenominator() {
        CoreColumnPosition source = new CoreColumnPosition(0, 0);
        CoreColumnPosition exhausted = new CoreColumnPosition(0, 1);
        CoreColumnPosition control = new CoreColumnPosition(1, 0);
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(
                        source, fuel(0.0D),
                        exhausted, new FuelColumnState(
                                FuelAssemblyState.installed(216_000, 216_000), 0.0D, 0.0D)
                ),
                Map.of(control, ControlRodColumnState.fullyInserted()),
                0L,
                0L,
                0L,
                false
        );

        MeltdownUpdateResult result = update(snapshot, Set.of(source, exhausted, control), false, false);

        assertEquals(1.0D, result.propagationCoverageFraction(), 1.0E-12D);
        assertTrue(result.dangerThresholdReached());
    }

    @Test
    void coverageBoundaryCountsOnlyEffectiveFuelColumns() {
        MeltdownUpdateResult below = update(snapshotWithFuelCount(6, 0L, false), covered(1), false, false);
        MeltdownUpdateResult exact = update(snapshotWithFuelCount(5, 0L, false), covered(1), false, false);
        MeltdownUpdateResult above = update(snapshotWithFuelCount(9, 0L, false), covered(2), false, false);

        assertEquals(1.0D / 6.0D, below.propagationCoverageFraction(), 1.0E-12D);
        assertFalse(below.dangerThresholdReached());
        assertEquals(0.20D, exact.propagationCoverageFraction(), 1.0E-12D);
        assertTrue(exact.dangerThresholdReached());
        assertTrue(above.dangerThresholdReached());
        assertEquals(MeltdownStatus.RUNNING, exact.status());
        assertEquals(1L, exact.snapshot().meltdownProgressTicks());
    }

    @Test
    void scramAndEffectiveCoolingPauseWithoutRewinding() {
        ReactorSnapshot running = snapshotWithFuelCount(5, 20L, true);

        MeltdownUpdateResult scrammed = update(running, covered(1), true, false);
        MeltdownUpdateResult cooled = update(scrammed.snapshot(), covered(1), false, true);

        assertEquals(MeltdownStatus.PAUSED, scrammed.status());
        assertEquals(20L, scrammed.snapshot().meltdownProgressTicks());
        assertEquals(MeltdownStatus.PAUSED, cooled.status());
        assertEquals(20L, cooled.snapshot().meltdownProgressTicks());
    }

    @Test
    void clearedDangerPausesAndLaterDangerResumesFromStoredProgress() {
        ReactorSnapshot running = snapshotWithFuelCount(5, 40L, true);

        MeltdownUpdateResult cleared = update(running, Set.of(), false, false);
        MeltdownUpdateResult resumed = update(cleared.snapshot(), covered(1), false, false);

        assertEquals(MeltdownStatus.PAUSED, cleared.status());
        assertEquals(40L, cleared.snapshot().meltdownProgressTicks());
        assertEquals(MeltdownStatus.RUNNING, resumed.status());
        assertEquals(41L, resumed.snapshot().meltdownProgressTicks());
    }

    @Test
    void countdownCompletesAndNeverExceedsConfiguredDuration() {
        ReactorSnapshot almostComplete = snapshotWithFuelCount(
                5,
                PARAMETERS.meltdownCountdownTicks() - 1L,
                true
        );

        MeltdownUpdateResult completed = update(almostComplete, covered(1), false, false);
        MeltdownUpdateResult repeated = update(completed.snapshot(), covered(1), false, false);

        assertEquals(MeltdownStatus.COMPLETE, completed.status());
        assertEquals(PARAMETERS.meltdownCountdownTicks(), completed.snapshot().meltdownProgressTicks());
        assertEquals(PARAMETERS.meltdownCountdownTicks(), repeated.snapshot().meltdownProgressTicks());
    }

    @Test
    void onlyFullRepairOfFuelAndControlColumnsResetsCountdown() {
        CoreColumnPosition fuelPosition = new CoreColumnPosition(0, 0);
        CoreColumnPosition controlPosition = new CoreColumnPosition(1, 0);
        ReactorSnapshot partlyRepaired = new ReactorSnapshot(
                Map.of(fuelPosition, fuel(1.0D)),
                Map.of(controlPosition, new ControlRodColumnState(0.9D, 0.4D, 0.4D, false, 0.0D)),
                0L,
                0L,
                50L,
                true
        );
        ReactorSnapshot fullyRepaired = new ReactorSnapshot(
                partlyRepaired.fuelColumns(),
                Map.of(controlPosition, new ControlRodColumnState(1.0D, 0.4D, 0.4D, false, 0.0D)),
                0L,
                0L,
                50L,
                true
        );

        MeltdownUpdateResult partial = update(partlyRepaired, Set.of(), false, false);
        MeltdownUpdateResult reset = update(fullyRepaired, Set.of(), false, false);

        assertTrue(partial.snapshot().meltdownCountdownStarted());
        assertEquals(50L, partial.snapshot().meltdownProgressTicks());
        assertFalse(reset.snapshot().meltdownCountdownStarted());
        assertEquals(0L, reset.snapshot().meltdownProgressTicks());
        assertEquals(MeltdownStatus.INACTIVE, reset.status());
    }

    private static MeltdownUpdateResult update(
            ReactorSnapshot snapshot,
            Set<CoreColumnPosition> covered,
            boolean scram,
            boolean cooling
    ) {
        HeatPropagationResult propagation = new HeatPropagationResult(
                snapshot,
                Map.of(),
                Map.of(),
                Map.of(),
                covered,
                0.0D
        );
        return ReactorMeltdownStateMachine.update(snapshot, propagation, scram, cooling, PARAMETERS);
    }

    private static Set<CoreColumnPosition> covered(int count) {
        Map<CoreColumnPosition, FuelColumnState> positions = fuelMap(count);
        return Set.copyOf(positions.keySet());
    }

    private static ReactorSnapshot snapshotWithFuelCount(int count, long progress, boolean started) {
        Map<CoreColumnPosition, FuelColumnState> fuels = fuelMap(count);
        CoreColumnPosition first = fuels.keySet().iterator().next();
        fuels.put(first, fuel(0.9D));
        return new ReactorSnapshot(fuels, Map.of(), 0L, 0L, progress, started);
    }

    private static Map<CoreColumnPosition, FuelColumnState> fuelMap(int count) {
        Map<CoreColumnPosition, FuelColumnState> fuels = new LinkedHashMap<>();
        int added = 0;
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE && added < count; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE && added < count; z++) {
                fuels.put(new CoreColumnPosition(x, z), fuel(1.0D));
                added++;
            }
        }
        return fuels;
    }

    private static FuelColumnState fuel(double integrity) {
        return new FuelColumnState(FuelAssemblyState.installed(216_000, 0), integrity, 0.0D);
    }
}
