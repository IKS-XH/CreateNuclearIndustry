package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证正式模拟流水线的顺序、确定性、SCRAM/冷却暂停和 NBT 续算。 */
class ReactorSimulationRegressionTest {
    private static final ReactorSimulationParameters PARAMETERS = ReactorSimulationParameters.defaults();
    private static final CoreColumnPosition SOURCE = new CoreColumnPosition(1, 2);
    private static final CoreColumnPosition TARGET = new CoreColumnPosition(1, 1);
    private static final CoreColumnPosition TARGET_CONTROL = new CoreColumnPosition(0, 1);
    private static final CoreColumnPosition SOURCE_CONTROL = new CoreColumnPosition(0, 2);

    @Test
    void fullPipelineIsStableAcrossColumnInsertionOrdersAndRepeatedRuns() {
        PipelineResult canonical = runPipeline(fixture(false), Map.of(), Map.of(), false, false);
        PipelineResult reversed = runPipeline(fixture(true), Map.of(), Map.of(), false, false);

        assertEquals(canonical, reversed);
        for (int repeat = 0; repeat < 8; repeat++) {
            assertEquals(canonical, runPipeline(fixture(repeat % 2 == 1), Map.of(), Map.of(), false, false));
        }
    }

    @Test
    void fullPipelineCarriesControlSuppressionHeatDamagePropagationAndMeltdown() {
        PipelineResult result = runPipeline(fixture(false), Map.of(), Map.of(), false, false);

        FuelColumnFissionResult fission = result.fission().columns().get(TARGET);
        assertEquals(0.65D, fission.controlledIntensity(), 1.0E-12D,
                "the jammed rod's frozen actual depth still suppresses isolated fuel");
        assertTrue(fission.generatedHeatHu() > 1.95D,
                "the failed neighbouring fuel column participates in bounded overclock feedback");
        assertTrue(result.fission().columns().get(SOURCE).generatedHeatHu() > 0.0D,
                "failed fuel with remaining assembly continues producing fission heat");
        assertTrue(result.fission().columns().get(SOURCE).plannedFuelBurnUnits() > 0.0D,
                "failed fuel with remaining assembly continues consuming fuel");
        assertEquals(fission.generatedHeatHu(), result.thermal().columns().get(TARGET).generatedHeatHu(),
                1.0E-12D);
        assertTrue(result.thermal().columns().get(TARGET).integrityDamage() > 0.0D);

        double sourceHeatBeforePropagation = result.thermal().snapshot().fuelColumns().get(SOURCE).cachedHeatHu();
        double expectedTransferredHeat = sourceHeatBeforePropagation * PARAMETERS.damageTransferRate();
        assertEquals(expectedTransferredHeat, result.propagation().totalTransferredHeatHu(), 1.0E-12D);
        assertEquals(expectedTransferredHeat / 2.0D, result.propagation().receivedHeatHu().get(TARGET), 1.0E-12D);
        assertEquals(expectedTransferredHeat / 2.0D,
                result.propagation().receivedHeatHu().get(SOURCE_CONTROL), 1.0E-12D);
        assertEquals(sourceHeatBeforePropagation - expectedTransferredHeat,
                result.propagation().snapshot().fuelColumns().get(SOURCE).cachedHeatHu(),
                1.0E-12D);
        assertTrue(result.propagation().coveredEffectiveFuelColumns().contains(TARGET));
        assertEquals(1.0D, result.meltdown().propagationCoverageFraction(), 1.0E-12D);
        assertTrue(result.meltdown().dangerThresholdReached());
        assertEquals(MeltdownStatus.RUNNING, result.meltdown().status());
        assertEquals(1L, result.meltdown().snapshot().meltdownProgressTicks());

        ControlRodColumnState jammed = result.meltdown().snapshot().controlRodColumns().get(TARGET_CONTROL);
        assertTrue(jammed.jammed());
        assertEquals(0.35D, jammed.actualDepth(), 1.0E-12D);
        assertSame(jammed, ControlRodStateTransitions.requestScram(jammed));
    }

    @Test
    void scramCoolingAndClearedDangerPauseThenResumeWithoutRewinding() {
        PipelineResult running = runPipeline(fixture(false), Map.of(), Map.of(), false, false);
        PipelineResult scrammed = runPipeline(running.meltdown().snapshot(), Map.of(), Map.of(), true, false);
        PipelineResult cooled = runPipeline(
                scrammed.meltdown().snapshot(),
                Map.of(TARGET, 100.0D),
                Map.of(TARGET, 100.0D),
                false,
                true
        );
        PipelineResult resumed = runPipeline(
                cooled.meltdown().snapshot(),
                Map.of(TARGET, 100.0D),
                Map.of(),
                false,
                false
        );

        assertEquals(MeltdownStatus.RUNNING, running.meltdown().status());
        assertEquals(MeltdownStatus.PAUSED, scrammed.meltdown().status());
        assertEquals(MeltdownStatus.PAUSED, cooled.meltdown().status());
        assertEquals(1L, scrammed.meltdown().snapshot().meltdownProgressTicks());
        assertEquals(1L, cooled.meltdown().snapshot().meltdownProgressTicks());
        assertEquals(MeltdownStatus.RUNNING, resumed.meltdown().status());
        assertEquals(2L, resumed.meltdown().snapshot().meltdownProgressTicks());
    }

    @Test
    void nbtRoundTripPreservesEveryPipelineContinuationResult() {
        ReactorSnapshot initial = persistedFixture();
        ReactorSnapshot restoredInitial = ReactorSnapshotNbtCodec.decode(ReactorSnapshotNbtCodec.encode(initial));
        assertEquals(initial, restoredInitial);

        ReactorFissionResult fission = ReactorFissionCalculator.calculate(initial, PARAMETERS, false);
        ReactorThermalResult thermal = ReactorThermalCalculator.settleFissionHeat(
                initial,
                fission,
                Map.of(),
                PARAMETERS
        );
        ReactorSnapshot restoredThermal = ReactorSnapshotNbtCodec.decode(
                ReactorSnapshotNbtCodec.encode(thermal.snapshot())
        );
        assertEquals(thermal.snapshot(), restoredThermal);

        HeatPropagationResult propagation = ReactorHeatPropagation.propagate(
                thermal.snapshot(),
                Map.of(),
                PARAMETERS
        );
        HeatPropagationResult restoredPropagation = ReactorHeatPropagation.propagate(
                restoredThermal,
                Map.of(),
                PARAMETERS
        );
        assertEquals(propagation, restoredPropagation);

        MeltdownUpdateResult meltdown = ReactorMeltdownStateMachine.update(
                thermal.snapshot(),
                propagation,
                false,
                false,
                PARAMETERS
        );
        MeltdownUpdateResult restoredMeltdown = ReactorMeltdownStateMachine.update(
                restoredThermal,
                restoredPropagation,
                false,
                false,
                PARAMETERS
        );
        assertEquals(meltdown, restoredMeltdown);
    }

    private static PipelineResult runPipeline(
            ReactorSnapshot snapshot,
            Map<CoreColumnPosition, Double> localCooling,
            Map<CoreColumnPosition, Double> propagationCooling,
            boolean scramActive,
            boolean effectiveCooling
    ) {
        ReactorFissionResult fission = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, scramActive);
        ReactorThermalResult thermal = ReactorThermalCalculator.settleFissionHeat(
                snapshot,
                fission,
                localCooling,
                PARAMETERS
        );
        HeatPropagationResult propagation = ReactorHeatPropagation.propagate(
                thermal.snapshot(),
                propagationCooling,
                PARAMETERS
        );
        MeltdownUpdateResult meltdown = ReactorMeltdownStateMachine.update(
                thermal.snapshot(),
                propagation,
                scramActive,
                effectiveCooling,
                PARAMETERS
        );
        return new PipelineResult(fission, thermal, propagation, meltdown);
    }

    private static ReactorSnapshot fixture(boolean reverseInsertionOrder) {
        Map<CoreColumnPosition, FuelColumnState> fuels = new LinkedHashMap<>();
        Map<CoreColumnPosition, ControlRodColumnState> controls = new LinkedHashMap<>();
        if (reverseInsertionOrder) {
            fuels.put(TARGET, liveFuel(1.0D, 0.0D));
            fuels.put(SOURCE, liveFuel(0.0D, 4.0D));
            controls.put(SOURCE_CONTROL, jammedControl(0.0D, 0.8D, 0.35D));
            controls.put(TARGET_CONTROL, jammedControl(0.4D, 0.8D, 0.35D));
        } else {
            fuels.put(SOURCE, liveFuel(0.0D, 4.0D));
            fuels.put(TARGET, liveFuel(1.0D, 0.0D));
            controls.put(TARGET_CONTROL, jammedControl(0.4D, 0.8D, 0.35D));
            controls.put(SOURCE_CONTROL, jammedControl(0.0D, 0.8D, 0.35D));
        }
        return new ReactorSnapshot(fuels, controls, 1_200L, 300L, 0L, false);
    }

    private static ReactorSnapshot persistedFixture() {
        ReactorSnapshot base = fixture(false);
        return new ReactorSnapshot(
                base.fuelColumns(),
                base.controlRodColumns(),
                base.coldCoolantMb(),
                base.hotCoolantMb(),
                7L,
                true
        );
    }

    private static FuelColumnState liveFuel(double integrity, double cachedHeat) {
        return new FuelColumnState(FuelAssemblyState.installed(216_000, 0), integrity, cachedHeat);
    }

    private static ControlRodColumnState jammedControl(double integrity, double targetDepth, double actualDepth) {
        return new ControlRodColumnState(integrity, targetDepth, actualDepth, true, 0.0D);
    }

    private record PipelineResult(
            ReactorFissionResult fission,
            ReactorThermalResult thermal,
            HeatPropagationResult propagation,
            MeltdownUpdateResult meltdown
    ) {
    }
}
