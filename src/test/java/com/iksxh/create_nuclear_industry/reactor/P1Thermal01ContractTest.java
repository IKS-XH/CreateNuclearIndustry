package com.iksxh.create_nuclear_industry.reactor;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 P1-THERMAL-01 的量化余数、真实短缺、传播隔离、控制棒停热和 NBT 守恒。 */
class P1Thermal01ContractTest {
    private static final double EPSILON = 1.0E-12D;
    private static final CoreColumnPosition CENTER = new CoreColumnPosition(1, 1);
    private static final CoreColumnPosition NORTH = new CoreColumnPosition(1, 0);
    private static final ReactorSimulationParameters DEFAULTS =
            ReactorSimulationParameters.defaults();

    @Test
    void quarterHuIsStoredAsQuantizedRemainderWithoutDamageWhenCoolingExists() {
        ReactorServerTick.Result result = advance(0.25D, snapshot(fuel(1.0D, 0.0D, 0.0D), 1L, 0L),
                1_000L);

        FuelColumnThermalResult thermal = result.thermal().columns().get(CENTER);
        FuelColumnState next = result.snapshot().fuelColumns().get(CENTER);
        assertEquals(0.25D, thermal.generatedHeatHu(), EPSILON);
        assertEquals(0.0D, thermal.removedHeatHu(), EPSILON);
        assertEquals(0.25D, thermal.quantizedHeatRemainderHu(), EPSILON);
        assertEquals(0.0D, thermal.integrityDamage(), EPSILON);
        assertEquals(0.25D, next.cachedHeatHu(), EPSILON);
        assertEquals(0.25D, next.quantizedHeatRemainderHu(), EPSILON);
        assertEquals(1.0D, next.integrity(), EPSILON);
    }

    @Test
    void fortyNineHundredthsHuIsNotDamageWhenOneWholeMbCanBeStored() {
        ReactorServerTick.Result result = advance(0.49D, snapshot(fuel(1.0D, 0.0D, 0.0D), 1L, 0L),
                1_000L);

        assertEquals(0.49D, result.coolant().settlement().remainingHeatHu(), EPSILON);
        assertEquals(0.49D, result.coolant().quantizedHeatRemainderHu(), EPSILON);
        assertEquals(0.0D, result.thermal().columns().get(CENTER).integrityDamage(), EPSILON);
        assertEquals(0.49D, result.snapshot().fuelColumns().get(CENTER).cachedHeatHu(), EPSILON);
    }

    @Test
    void exactlyHalfHuConsumesOneWholeMbAndLeavesNoRemainder() {
        ReactorServerTick.Result result = advance(0.5D, snapshot(fuel(1.0D, 0.0D, 0.0D), 1L, 0L),
                1_000L);

        assertEquals(1.0D, result.coolant().settlement().convertedCoolantMb(), EPSILON);
        assertEquals(0.5D, result.coolant().settlement().removedHeatHu(), EPSILON);
        assertEquals(0.0D, result.coolant().quantizedHeatRemainderHu(), EPSILON);
        assertEquals(0.0D, result.snapshot().fuelColumns().get(CENTER).cachedHeatHu(), EPSILON);
        assertEquals(0.0D,
                result.snapshot().fuelColumns().get(CENTER).quantizedHeatRemainderHu(), EPSILON);
    }

    @Test
    void fractionalHeatAccumulatesAcrossTicksUntilOneWholeMbIsConverted() {
        ReactorSimulationParameters parameters = parametersForGeneratedHeat(0.25D);
        ReactorSnapshot before = snapshot(fuel(1.0D, 0.0D, 0.0D), 1L, 0L);

        ReactorServerTick.Result first = advance(before, parameters, 1_000L);
        ReactorServerTick.Result second = advance(first.snapshot(), parameters, 1_000L);

        assertEquals(0.25D, first.snapshot().fuelColumns().get(CENTER).cachedHeatHu(), EPSILON);
        assertEquals(0.25D,
                first.snapshot().fuelColumns().get(CENTER).quantizedHeatRemainderHu(), EPSILON);
        assertEquals(0.5D, second.coolant().settlement().removedHeatHu(), EPSILON);
        assertEquals(1.0D, second.coolant().settlement().convertedCoolantMb(), EPSILON);
        assertEquals(0.0D, second.snapshot().fuelColumns().get(CENTER).cachedHeatHu(), EPSILON);
        assertEquals(0.0D,
                second.snapshot().fuelColumns().get(CENTER).quantizedHeatRemainderHu(), EPSILON);
        assertEquals(1.0D, second.snapshot().fuelColumns().get(CENTER).integrity(), EPSILON);
    }

    @Test
    void noCoolingTreatsFortyNineHundredthsHuAsRealShortage() {
        ReactorServerTick.Result result = advance(
                snapshot(fuel(1.0D, 0.0D, 0.0D), 0L, 0L),
                parametersForGeneratedHeat(0.49D),
                1_000L
        );
        double expectedDamage = (0.49D - DEFAULTS.damageHeatThresholdHuPerTick())
                * DEFAULTS.damageRatePerTickHuLoad();

        assertEquals(0.0D, result.coolant().quantizedHeatRemainderHu(), EPSILON);
        assertEquals(expectedDamage,
                result.thermal().columns().get(CENTER).integrityDamage(), EPSILON);
        assertEquals(0.49D, result.snapshot().fuelColumns().get(CENTER).cachedHeatHu(), EPSILON);
        assertEquals(0.0D,
                result.snapshot().fuelColumns().get(CENTER).quantizedHeatRemainderHu(), EPSILON);
    }

    @Test
    void partialCoolingDoesNotChargeTheFractionalRemainderTwiceAsDamage() {
        ReactorServerTick.Result result = advance(
                snapshot(fuel(1.0D, 0.0D, 0.0D), 1L, 0L),
                parametersForGeneratedHeat(1.25D),
                1_000L
        );
        double expectedDamage = (1.25D - 0.5D - 0.25D
                - DEFAULTS.damageHeatThresholdHuPerTick())
                * DEFAULTS.damageRatePerTickHuLoad();

        assertEquals(1.0D, result.coolant().settlement().convertedCoolantMb(), EPSILON);
        assertEquals(0.25D, result.coolant().quantizedHeatRemainderHu(), EPSILON);
        assertEquals(0.75D, result.thermal().columns().get(CENTER).netHeatLoadHu(), EPSILON);
        assertEquals(0.25D,
                result.thermal().columns().get(CENTER).quantizedHeatRemainderHu(), EPSILON);
        assertEquals(expectedDamage,
                result.thermal().columns().get(CENTER).integrityDamage(), EPSILON);
    }

    @Test
    void blockedHotEndDisablesQuantizedProtectionAndKeepsHeatInFuelColumn() {
        ReactorServerTick.Result result = advance(
                snapshot(fuel(1.0D, 0.0D, 0.0D), 1L, 0L),
                parametersForGeneratedHeat(0.49D),
                0L
        );
        double expectedDamage = (0.49D - DEFAULTS.damageHeatThresholdHuPerTick())
                * DEFAULTS.damageRatePerTickHuLoad();

        assertEquals(0.0D, result.coolant().settlement().convertedCoolantMb(), EPSILON);
        assertEquals(0.0D, result.coolant().quantizedHeatRemainderHu(), EPSILON);
        assertEquals(expectedDamage,
                result.thermal().columns().get(CENTER).integrityDamage(), EPSILON);
        assertEquals(0.49D, result.snapshot().fuelColumns().get(CENTER).cachedHeatHu(), EPSILON);
    }

    @Test
    void multipleColumnsShareRemovalAndQuantizedRemainderInTheSameRatio() {
        CoreColumnPosition east = new CoreColumnPosition(2, 2);
        ReactorSnapshot before = new ReactorSnapshot(
                Map.of(
                        CENTER, fuel(1.0D, 0.0D, 0.0D),
                        east, fuel(0.5D, 0.0D, 0.0D)
                ),
                Map.of(),
                1L,
                0L,
                0L,
                false
        );
        ReactorServerTick.Result result = advance(before,
                parametersForGeneratedHeat(0.3D), 1_000L);
        FuelColumnThermalResult center = result.thermal().columns().get(CENTER);
        FuelColumnThermalResult eastResult = result.thermal().columns().get(east);

        assertEquals(0.25D,
                center.quantizedHeatRemainderHu() + eastResult.quantizedHeatRemainderHu(), EPSILON);
        assertEquals(center.generatedHeatHu() / eastResult.generatedHeatHu(),
                center.removedHeatHu() / eastResult.removedHeatHu(), 1.0E-10D);
        assertEquals(center.netHeatLoadHu() / eastResult.netHeatLoadHu(),
                center.quantizedHeatRemainderHu() / eastResult.quantizedHeatRemainderHu(),
                1.0E-10D);
        assertEquals(0.0D, center.integrityDamage(), EPSILON);
        assertEquals(0.0D, eastResult.integrityDamage(), EPSILON);
    }

    @Test
    void failedColumnQuantizedRemainderCannotStartPropagationOrMeltdown() {
        ReactorSnapshot before = new ReactorSnapshot(
                Map.of(
                        CENTER, fuel(0.0D, 0.49D, 0.49D),
                        NORTH, fuel(1.0D, 0.0D, 0.0D)
                ),
                Map.of(),
                1L,
                0L,
                0L,
                false
        );
        ReactorServerTick.Result result = advance(before,
                parametersForGeneratedHeat(0.0D), 1_000L);

        assertTrue(result.fission().columns().get(CENTER).generatedHeatHu() == 0.0D);
        assertEquals(0.49D,
                result.snapshot().fuelColumns().get(CENTER).quantizedHeatRemainderHu(), EPSILON);
        assertTrue(result.propagation().receivedHeatHu().isEmpty());
        assertEquals(1.0D, result.snapshot().fuelColumns().get(NORTH).integrity(), EPSILON);
        assertFalse(result.snapshot().meltdownCountdownStarted());
    }

    @Test
    void fullyInsertedControlRodsStopNewHeatButAllowSafeCachedHeatToRemain() {
        ReactorSnapshot before = new ReactorSnapshot(
                Map.of(CENTER, fuel(1.0D, 0.49D, 0.49D)),
                Map.of(NORTH, ControlRodColumnState.fullyInserted()),
                1L,
                0L,
                0L,
                false
        );
        ReactorServerTick.Result result = advance(before,
                parametersForGeneratedHeat(0.49D), 1_000L);

        FuelColumnFissionResult fission = result.fission().columns().get(CENTER);
        assertEquals(0.0D, fission.generatedHeatHu(), EPSILON);
        assertEquals(0.0D, fission.plannedFuelBurnUnits(), EPSILON);
        assertEquals(0.49D, result.thermal().columns().get(CENTER).netHeatLoadHu(), EPSILON);
        assertEquals(0.49D,
                result.thermal().columns().get(CENTER).quantizedHeatRemainderHu(), EPSILON);
        assertEquals(0.0D, result.thermal().columns().get(CENTER).integrityDamage(), EPSILON);
        assertEquals(0.49D, result.coolant().settlement().remainingHeatHu(), EPSILON);
    }

    @Test
    void quantizedRemainderSurvivesSnapshotNbtReloadAndLegacyFormatMigratesToZero() {
        ReactorServerTick.Result result = advance(
                snapshot(fuel(1.0D, 0.0D, 0.0D), 1L, 0L),
                parametersForGeneratedHeat(0.49D),
                1_000L
        );
        ReactorSnapshot reloaded = ReactorSnapshotNbtCodec.decode(
                ReactorSnapshotNbtCodec.encode(result.snapshot()));

        assertEquals(result.snapshot(), reloaded);
        CompoundTag encodedFuel = ReactorSnapshotNbtCodec.encode(result.snapshot())
                .getList("FuelColumns", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0);
        assertEquals(0.49D, encodedFuel.getDouble("QuantizedHeatRemainderHu"), EPSILON);

        CompoundTag legacy = new CompoundTag();
        legacy.putInt("FormatVersion", 2);
        net.minecraft.nbt.ListTag fuels = new net.minecraft.nbt.ListTag();
        CompoundTag fuel = new CompoundTag();
        fuel.putInt("X", CENTER.x());
        fuel.putInt("Z", CENTER.z());
        fuel.putDouble("CachedHeatHu", 0.49D);
        fuel.putDouble("QuantizedHeatRemainderHu", 0.49D);
        fuels.add(fuel);
        legacy.put("FuelColumns", fuels);
        ReactorSnapshot migrated = ReactorSnapshotNbtCodec.decode(legacy);
        assertEquals(0.0D,
                migrated.fuelColumns().get(CENTER).quantizedHeatRemainderHu(), EPSILON);
    }

    private static ReactorServerTick.Result advance(
            double generatedHeat,
            ReactorSnapshot snapshot,
            long hotCapacityMb
    ) {
        return advance(snapshot, parametersForGeneratedHeat(generatedHeat), hotCapacityMb);
    }

    private static ReactorServerTick.Result advance(
            ReactorSnapshot snapshot,
            ReactorSimulationParameters parameters,
            long hotCapacityMb
    ) {
        return ReactorServerTick.advance(snapshot, parameters, new ReactorServerTick.CoolantInput(
                ReactorCoolantLedger.summarizePorts(List.of(
                        ReactorCoolantLedger.Port.cold("cold", 128.0D),
                        ReactorCoolantLedger.Port.hot("hot", hotCapacityMb > 0L ? 128.0D : 0.0D)
                )),
                hotCapacityMb,
                0.5D
        ));
    }

    private static ReactorSimulationParameters parametersForGeneratedHeat(double generatedHeat) {
        return new ReactorSimulationParameters(
                generatedHeat / ReactorSnapshot.INTERNAL_HEIGHT,
                DEFAULTS.burnHoursPerBlock(),
                DEFAULTS.damageHeatThresholdHuPerTick(),
                DEFAULTS.damageRatePerTickHuLoad(),
                DEFAULTS.damageTransferRate(),
                DEFAULTS.controlRodFailureThreshold(),
                DEFAULTS.meltdownTriggerFraction(),
                DEFAULTS.meltdownCountdownTicks(),
                DEFAULTS.controlResponseExponent(),
                DEFAULTS.overclockHeatMultiplier(),
                DEFAULTS.overclockBurnMultiplier(),
                DEFAULTS.overclockFeedbackGain(),
                DEFAULTS.overclockFeedbackExponent(),
                DEFAULTS.totalHeatMultiplierCap()
        );
    }

    private static ReactorSnapshot snapshot(
            FuelColumnState fuel,
            long coldCoolantMb,
            long hotCoolantMb
    ) {
        return new ReactorSnapshot(
                Map.of(CENTER, fuel),
                Map.of(),
                coldCoolantMb,
                hotCoolantMb,
                0L,
                false
        );
    }

    private static FuelColumnState fuel(
            double integrity,
            double cachedHeatHu,
            double quantizedHeatRemainderHu
    ) {
        return new FuelColumnState(
                FuelAssemblyState.installed(216_000, 0),
                integrity,
                cachedHeatHu,
                0.0D,
                quantizedHeatRemainderHu
        );
    }
}
