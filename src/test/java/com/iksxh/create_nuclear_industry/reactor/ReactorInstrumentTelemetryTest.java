package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证动态遥测的字段来源、新生热语义、稳定坐标顺序和更新标签边界。 */
class ReactorInstrumentTelemetryTest {
    private static final ReactorSimulationParameters PARAMETERS =
            ReactorSimulationParameters.defaults();

    @Test
    void derivesPostTickInventoriesAndActualConversionWithoutPersistingTelemetryInSnapshot() {
        CoreColumnPosition firstFuel = new CoreColumnPosition(2, 0);
        CoreColumnPosition secondFuel = new CoreColumnPosition(0, 1);
        CoreColumnPosition control = new CoreColumnPosition(1, 0);
        ReactorSnapshot previous = new ReactorSnapshot(
                Map.of(
                        firstFuel, fuel(1.0D, 4.5D),
                        secondFuel, fuel(0.75D, 0.0D)
                ),
                Map.of(control, new ControlRodColumnState(0.8D, 0.0D, 0.0D, false, 0.0D)),
                64L,
                0L,
                0L,
                false
        );
        ReactorCoolantLedger.PortSummary ports = ReactorCoolantLedger.summarizePorts(
                List.of(
                        ReactorCoolantLedger.Port.cold("cold", 128.0D),
                        ReactorCoolantLedger.Port.hot("hot", 128.0D)
                ),
                128.0D
        );
        ReactorServerTick.Result result = ReactorServerTick.advance(
                previous,
                PARAMETERS,
                new ReactorServerTick.CoolantInput(ports, 1_000L, 0.5D)
        );

        ReactorInstrumentTelemetry telemetry = ReactorInstrumentTelemetry.from(result);

        assertTrue(telemetry.available());
        assertEquals(result.snapshot().coldCoolantMb(), telemetry.coldCoolantMb());
        assertEquals(result.snapshot().hotCoolantMb(), telemetry.hotCoolantMb());
        assertEquals(result.coolant().settlement().convertedCoolantMb(),
                telemetry.convertedCoolantMbPerTick(), 1.0E-12D);
        assertEquals(result.fission().generatedHeatHu(),
                telemetry.totalGeneratedFissionHeatHuPerTick(), 1.0E-12D);
        assertNotEquals(result.fission().generatedHeatHu() + 4.5D,
                telemetry.totalGeneratedFissionHeatHuPerTick(), 1.0E-12D);
        assertEquals(List.of(firstFuel, secondFuel), telemetry.fuelColumns().stream()
                .map(ReactorInstrumentTelemetry.FuelColumnTelemetry::position).toList());
        assertEquals(List.of(control), telemetry.controlRodColumns().stream()
                .map(ReactorInstrumentTelemetry.ControlRodColumnTelemetry::position).toList());
        assertEquals(result.snapshot().fuelColumns().get(firstFuel).integrity(),
                telemetry.fuelColumns().get(0).fuelColumnIntegrity(), 1.0E-12D);
        assertEquals(result.snapshot().controlRodColumns().get(control).integrity(),
                telemetry.controlRodColumns().get(0).controlRodColumnIntegrity(), 1.0E-12D);
        assertTrue(telemetry.coldCoolantMb() > 0L,
                "non-empty cold inventory was not reported explicitly");
        assertTrue(telemetry.hotCoolantMb() > 0L,
                "non-empty hot inventory was not reported explicitly");
        assertTrue(telemetry.convertedCoolantMbPerTick() > 0.0D,
                "non-empty inventory did not produce an actual coolant conversion");
    }

    @Test
    void emptyInventoriesReportZeroStockAndZeroActualConversion() {
        ReactorInstrumentTelemetry telemetry = ReactorInstrumentTelemetry.from(
                ReactorServerTick.advance(
                        ReactorSnapshot.empty(), PARAMETERS, ReactorServerTick.CoolantInput.none()));

        assertTrue(telemetry.available());
        assertEquals(0L, telemetry.coldCoolantMb());
        assertEquals(0L, telemetry.hotCoolantMb());
        assertEquals(0.0D, telemetry.convertedCoolantMbPerTick(), 1.0E-12D);
        assertTrue(telemetry.fuelColumns().isEmpty());
        assertTrue(telemetry.controlRodColumns().isEmpty());
        assertEquals(0.0D, telemetry.totalGeneratedFissionHeatHuPerTick(), 1.0E-12D);
    }

    @Test
    void controlDepthChangesAffectNewHeatReportedByTheNextTelemetry() {
        CoreColumnPosition fuelPosition = new CoreColumnPosition(1, 0);
        CoreColumnPosition controlPosition = new CoreColumnPosition(1, 1);
        ReactorSnapshot withdrawn = snapshotWithControlDepth(fuelPosition, controlPosition, 0.0D);
        ReactorSnapshot inserted = snapshotWithControlDepth(fuelPosition, controlPosition, 1.0D);
        ReactorServerTick.CoolantInput noCoolant = ReactorServerTick.CoolantInput.none();

        ReactorInstrumentTelemetry withdrawnTelemetry = ReactorInstrumentTelemetry.from(
                ReactorServerTick.advance(withdrawn, PARAMETERS, noCoolant));
        ReactorInstrumentTelemetry insertedTelemetry = ReactorInstrumentTelemetry.from(
                ReactorServerTick.advance(inserted, PARAMETERS, noCoolant));

        assertTrue(withdrawnTelemetry.totalGeneratedFissionHeatHuPerTick()
                > insertedTelemetry.totalGeneratedFissionHeatHuPerTick());
        assertEquals(1, withdrawnTelemetry.fuelColumns().size());
        assertEquals(1, insertedTelemetry.fuelColumns().size());
        assertTrue(withdrawnTelemetry.fuelColumns().get(0).generatedFissionHeatHuPerTick()
                > insertedTelemetry.fuelColumns().get(0).generatedFissionHeatHuPerTick());
        assertEquals(withdrawnTelemetry.fuelColumns().get(0).generatedFissionHeatHuPerTick(),
                withdrawnTelemetry.totalGeneratedFissionHeatHuPerTick(), 1.0E-12D);
        assertEquals(insertedTelemetry.fuelColumns().get(0).generatedFissionHeatHuPerTick(),
                insertedTelemetry.totalGeneratedFissionHeatHuPerTick(), 1.0E-12D);
        assertEquals(withdrawnTelemetry.totalGeneratedFissionHeatHuPerTick(),
                withdrawnTelemetry.fuelColumns().stream()
                        .mapToDouble(ReactorInstrumentTelemetry.FuelColumnTelemetry::generatedFissionHeatHuPerTick)
                        .sum(), 1.0E-12D);
    }

    @Test
    void telemetryUpdateTagRoundTripsAndUnavailableStateCarriesNoStaleValues() {
        ReactorSnapshot snapshot = ReactorSnapshot.singleFuelColumn(
                new CoreColumnPosition(0, 0), fuel(1.0D, 0.0D));
        ReactorInstrumentTelemetry telemetry = ReactorInstrumentTelemetry.from(
                ReactorServerTick.advance(snapshot, PARAMETERS, ReactorServerTick.CoolantInput.none()));

        ReactorInstrumentTelemetry decoded = ReactorInstrumentTelemetryNbtCodec.decode(
                ReactorInstrumentTelemetryNbtCodec.encode(telemetry));
        assertEquals(telemetry, decoded);

        ReactorInstrumentTelemetry unavailable = ReactorInstrumentTelemetry.unavailable("no successful tick");
        ReactorInstrumentTelemetry decodedUnavailable = ReactorInstrumentTelemetryNbtCodec.decode(
                ReactorInstrumentTelemetryNbtCodec.encode(unavailable));
        assertFalse(decodedUnavailable.available());
        assertTrue(decodedUnavailable.fuelColumns().isEmpty());
        assertEquals(0L, decodedUnavailable.coldCoolantMb());
        assertEquals(0.0D, decodedUnavailable.convertedCoolantMbPerTick());
    }

    @Test
    void malformedAvailableTelemetryIsUnavailableInsteadOfAnEmptyValidSnapshot() {
        CompoundTag malformed = new CompoundTag();
        malformed.putInt("FormatVersion", ReactorInstrumentTelemetryNbtCodec.FORMAT_VERSION);
        malformed.putBoolean("Available", true);
        malformed.putLong("ColdCoolantMb", 0L);
        malformed.putLong("HotCoolantMb", 0L);
        malformed.putDouble("TotalGeneratedFissionHeatHuPerTick", 0.0D);
        malformed.putDouble("ConvertedCoolantMbPerTick", 0.0D);

        assertFalse(ReactorInstrumentTelemetryNbtCodec.decode(malformed).available());
    }

    private static ReactorSnapshot snapshotWithControlDepth(
            CoreColumnPosition fuelPosition,
            CoreColumnPosition controlPosition,
            double depth
    ) {
        return new ReactorSnapshot(
                Map.of(fuelPosition, fuel(1.0D, 0.0D)),
                Map.of(controlPosition, new ControlRodColumnState(1.0D, depth, depth, false, 0.0D)),
                0L,
                0L,
                0L,
                false
        );
    }

    private static FuelColumnState fuel(double integrity, double cachedHeat) {
        return new FuelColumnState(
                FuelAssemblyState.installed(216_000, 0), integrity, cachedHeat);
    }
}
