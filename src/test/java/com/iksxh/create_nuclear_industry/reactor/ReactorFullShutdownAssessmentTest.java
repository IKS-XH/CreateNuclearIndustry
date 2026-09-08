package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证完全停机四项条件、余热口径、失败关闭和输入无副作用。 */
class ReactorFullShutdownAssessmentTest {
    private static final CoreColumnPosition CENTER = new CoreColumnPosition(1, 1);
    private static final CoreColumnPosition CONTROL = new CoreColumnPosition(1, 0);
    private static final int MAX_DAMAGE = 216_000;

    @Test
    void emptyHeapMeetsAllFourConditions() {
        ReactorFullShutdownResult result = assess(ReactorSnapshot.empty());

        assertTrue(result.inputValid());
        assertTrue(result.fullyStopped());
        assertTrue(result.generatedHeatSafe());
        assertTrue(result.plannedFuelBurnSafe());
        assertTrue(result.activeResidualHeatSafe());
        assertTrue(result.meltdownCountdownInactive());
        assertEquals(0.0D, result.generatedHeatHu(), 0.0D);
        assertEquals(0.0D, result.plannedFuelBurnUnits(), 0.0D);
        assertEquals(0.0D, result.activeResidualHeatHu(), 0.0D);
    }

    @Test
    void nonZeroGeneratedHeatRejectsEvenWhenPlannedBurnIsWithinEpsilon() {
        ReactorSimulationParameters parameters = parameters(1.0D, 1.0E15D);

        ReactorFullShutdownResult result = assess(isolatedFuelSnapshot(), parameters);

        assertFalse(result.fullyStopped());
        assertFalse(result.generatedHeatSafe());
        assertTrue(result.plannedFuelBurnSafe());
        assertTrue(result.activeResidualHeatSafe());
        assertTrue(result.generatedHeatHu() > ReactorFullShutdownAssessment.SAFETY_EPSILON);
        assertTrue(result.plannedFuelBurnUnits() <= ReactorFullShutdownAssessment.SAFETY_EPSILON);
    }

    @Test
    void nonZeroPlannedFuelBurnRejectsEvenWhenGeneratedHeatIsWithinEpsilon() {
        ReactorSimulationParameters parameters = parameters(0.0D, 3.0D);

        ReactorFullShutdownResult result = assess(isolatedFuelSnapshot(), parameters);

        assertFalse(result.fullyStopped());
        assertTrue(result.generatedHeatSafe());
        assertFalse(result.plannedFuelBurnSafe());
        assertTrue(result.activeResidualHeatSafe());
        assertEquals(0.0D, result.generatedHeatHu(), 0.0D);
        assertTrue(result.plannedFuelBurnUnits() > ReactorFullShutdownAssessment.SAFETY_EPSILON);
    }

    @Test
    void activeCachedHeatRejects() {
        ReactorFullShutdownResult result = assess(snapshotWithHeat(2.0D, 0.0D));

        assertFalse(result.fullyStopped());
        assertFalse(result.activeResidualHeatSafe());
        assertEquals(2.0D, result.activeResidualHeatHu(), 0.0D);
        assertEquals(0.0D, result.safeQuantizedHeatRemainderHu(), 0.0D);
    }

    @Test
    void safeQuantizedRemainderDoesNotBlockShutdown() {
        ReactorFullShutdownResult result = assess(snapshotWithHeat(2.0D, 2.0D));

        assertTrue(result.fullyStopped());
        assertTrue(result.activeResidualHeatSafe());
        assertEquals(0.0D, result.activeResidualHeatHu(), 0.0D);
        assertEquals(2.0D, result.safeQuantizedHeatRemainderHu(), 0.0D);
    }

    @Test
    void partialSafeQuantizedRemainderLeavesActiveHeat() {
        ReactorFullShutdownResult result = assess(snapshotWithHeat(2.0D, 1.5D));

        assertFalse(result.fullyStopped());
        assertFalse(result.activeResidualHeatSafe());
        assertEquals(0.5D, result.activeResidualHeatHu(), 0.0D);
        assertEquals(1.5D, result.safeQuantizedHeatRemainderHu(), 0.0D);
    }

    @Test
    void newlyStartedRunningScramPausedAndCoolingPausedCountdownsReject() {
        ReactorSnapshot stopped = stoppedOperationalSnapshot();
        ReactorSnapshot scramPaused = stopped.withMeltdown(40L, true);
        ReactorSnapshot coolingPaused = stopped
                .withCoolantInventories(12_345L, 6_789L)
                .withMeltdown(80L, true);

        assertCountdownRejects(stopped.withMeltdown(0L, true));
        assertCountdownRejects(stopped.withMeltdown(1L, true));
        assertCountdownRejects(scramPaused);
        assertCountdownRejects(coolingPaused);
    }

    @Test
    void completedAndPublishedCountdownRejects() {
        ReactorSnapshot completed = stoppedOperationalSnapshot()
                .withMeltdown(ReactorSimulationParameters.defaults().meltdownCountdownTicks(), true);
        ReactorSnapshot published = completed.withMeltdownEventPublished(true);

        assertCountdownRejects(completed);
        assertCountdownRejects(published);
        assertTrue(published.meltdownEventPublished());
    }

    @Test
    void fuelInventoriesRemaindersDamageDepthJammedAndScramDoNotFalseReject() {
        ReactorFullShutdownResult result = assess(stoppedOperationalSnapshot());

        assertTrue(result.fullyStopped());
        assertEquals(0.0D, result.generatedHeatHu(), 0.0D);
        assertEquals(0.0D, result.plannedFuelBurnUnits(), 0.0D);
        assertEquals(0.0D, result.activeResidualHeatHu(), 0.0D);
    }

    @Test
    void controlRodHistoricalCachedHeatDoesNotBlockShutdown() {
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(CENTER, new FuelColumnState(
                        FuelAssemblyState.installed(MAX_DAMAGE, 0),
                        1.0D,
                        0.0D
                )),
                Map.of(CONTROL, new ControlRodColumnState(
                        1.0D,
                        1.0D,
                        1.0D,
                        false,
                        100.0D
                )),
                0L,
                0L,
                0L,
                false
        );

        ReactorFullShutdownResult result = assess(snapshot);

        assertTrue(result.fullyStopped());
        assertEquals(0.0D, result.activeResidualHeatHu(), 0.0D);
        assertEquals(0.0D, result.safeQuantizedHeatRemainderHu(), 0.0D);
    }

    @Test
    void exactEpsilonActiveHeatPassesAndAboveEpsilonRejects() {
        ReactorFullShutdownResult exact = assess(
                snapshotWithHeat(ReactorFullShutdownAssessment.SAFETY_EPSILON, 0.0D)
        );
        ReactorFullShutdownResult above = assess(
                snapshotWithHeat(Math.nextUp(ReactorFullShutdownAssessment.SAFETY_EPSILON), 0.0D)
        );

        assertTrue(exact.fullyStopped());
        assertTrue(exact.activeResidualHeatSafe());
        assertFalse(above.fullyStopped());
        assertFalse(above.activeResidualHeatSafe());
    }

    @Test
    void inputSnapshotIsUnchangedAndRepeatedAssessmentIsEqual() {
        ReactorSnapshot snapshot = stoppedOperationalSnapshot();
        ReactorSnapshot before = new ReactorSnapshot(
                snapshot.fuelColumns(),
                snapshot.controlRodColumns(),
                snapshot.coldCoolantMb(),
                snapshot.hotCoolantMb(),
                snapshot.meltdownProgressTicks(),
                snapshot.meltdownCountdownStarted(),
                snapshot.scramSavedTargetDepths(),
                snapshot.scramRequested(),
                snapshot.meltdownEventPublished()
        );

        ReactorFullShutdownResult first = assess(snapshot);
        ReactorFullShutdownResult second = assess(snapshot);

        assertEquals(before, snapshot);
        assertEquals(first, second);
        assertTrue(first.fullyStopped());
    }

    @Test
    void nullInputsFailClosed() {
        ReactorFullShutdownResult nullSnapshot = assess(null, ReactorSimulationParameters.defaults());
        ReactorFullShutdownResult nullParameters = assess(ReactorSnapshot.empty(), null);

        assertFalse(nullSnapshot.inputValid());
        assertFalse(nullSnapshot.fullyStopped());
        assertFalse(nullParameters.inputValid());
        assertFalse(nullParameters.fullyStopped());
    }

    private static ReactorFullShutdownResult assess(ReactorSnapshot snapshot) {
        return assess(snapshot, ReactorSimulationParameters.defaults());
    }

    private static ReactorFullShutdownResult assess(
            ReactorSnapshot snapshot,
            ReactorSimulationParameters parameters
    ) {
        return ReactorFullShutdownAssessment.assess(snapshot, parameters);
    }

    private static ReactorSnapshot isolatedFuelSnapshot() {
        return ReactorSnapshot.singleFuelColumn(
                CENTER,
                new FuelColumnState(FuelAssemblyState.installed(MAX_DAMAGE, 0), 1.0D, 0.0D)
        );
    }

    private static ReactorSnapshot snapshotWithHeat(double cachedHeatHu, double quantizedHeatRemainderHu) {
        return ReactorSnapshot.singleFuelColumn(
                CENTER,
                new FuelColumnState(
                        FuelAssemblyState.empty(),
                        1.0D,
                        cachedHeatHu,
                        0.0D,
                        quantizedHeatRemainderHu
                )
        );
    }

    private static ReactorSnapshot stoppedOperationalSnapshot() {
        return new ReactorSnapshot(
                Map.of(CENTER, new FuelColumnState(
                        FuelAssemblyState.installed(MAX_DAMAGE, 12_345),
                        0.2D,
                        0.0D,
                        0.75D,
                        0.0D
                )),
                Map.of(CONTROL, new ControlRodColumnState(
                        0.2D,
                        1.0D,
                        1.0D,
                        true,
                        0.0D
                )),
                1_234L,
                5_678L,
                0L,
                false,
                Map.of(CONTROL, 0.25D),
                true,
                false
        );
    }

    private static void assertCountdownRejects(ReactorSnapshot snapshot) {
        ReactorFullShutdownResult result = assess(snapshot);

        assertFalse(result.fullyStopped());
        assertTrue(result.generatedHeatSafe());
        assertTrue(result.plannedFuelBurnSafe());
        assertTrue(result.activeResidualHeatSafe());
        assertFalse(result.meltdownCountdownInactive());
    }

    private static ReactorSimulationParameters parameters(double baseHeat, double burnHours) {
        ReactorSimulationParameters defaults = ReactorSimulationParameters.defaults();
        return new ReactorSimulationParameters(
                baseHeat,
                burnHours,
                defaults.damageHeatThresholdHuPerTick(),
                defaults.damageRatePerTickHuLoad(),
                defaults.fuelColumnDamageHeatMultiplier(),
                defaults.fuelColumnDamageBurnMultiplier(),
                defaults.damageTransferRate(),
                defaults.controlRodFailureThreshold(),
                defaults.meltdownTriggerFraction(),
                defaults.meltdownCountdownTicks(),
                defaults.controlResponseExponent(),
                defaults.overclockHeatMultiplier(),
                defaults.overclockBurnMultiplier(),
                defaults.overclockFeedbackGain(),
                defaults.overclockFeedbackExponent(),
                defaults.totalHeatMultiplierCap()
        );
    }
}
