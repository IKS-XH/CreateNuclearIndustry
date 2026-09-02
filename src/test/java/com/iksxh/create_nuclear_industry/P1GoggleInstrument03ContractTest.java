package com.iksxh.create_nuclear_industry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 P1-GOGGLE-INSTRUMENT-03 的服务端来源、只读同步和节流边界。 */
class P1GoggleInstrument03ContractTest {
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");

    @Test
    void telemetryIsDerivedAfterFormalTickAndSyncedOnlyAsClientUpdateData() throws IOException {
        String instrument = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java");
        String telemetry = readSource(
                "com/iksxh/create_nuclear_industry/reactor/ReactorInstrumentTelemetry.java");
        String codec = readSource(
                "com/iksxh/create_nuclear_industry/reactor/ReactorInstrumentTelemetryNbtCodec.java");

        assertTrue(instrument.contains("ReactorInstrumentTelemetry.from(result)"));
        assertTrue(instrument.contains("clientTelemetry"));
        assertTrue(instrument.contains("TELEMETRY_SYNC_INTERVAL_TICKS = 5"));
        assertTrue(instrument.contains("if (clientPacket)"));
        assertTrue(instrument.contains("TELEMETRY_KEY"));
        assertTrue(instrument.contains("tag.put(TELEMETRY_KEY"));
        assertTrue(telemetry.contains("convertedCoolantMbPerTick"));
        assertTrue(telemetry.contains("generatedFissionHeatHuPerTick"));
        assertTrue(telemetry.contains("cachedHeatHu"));
        assertTrue(codec.contains("FORMAT_VERSION = 1"));
        assertFalse(codec.contains("ReactorSnapshotNbtCodec.FORMAT_VERSION"));
    }

    @Test
    void telemetryDoesNotPersistAsASecondReactorSnapshotState() throws IOException {
        String telemetry = readSource(
                "com/iksxh/create_nuclear_industry/reactor/ReactorInstrumentTelemetry.java");
        String codec = readSource(
                "com/iksxh/create_nuclear_industry/reactor/ReactorInstrumentTelemetryNbtCodec.java");

        assertTrue(telemetry.contains("不属于 {@link ReactorSnapshot} 的第二份权威状态"));
        assertTrue(codec.contains("不参与方块实体持久化"));
        assertFalse(telemetry.contains("write"));
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(JAVA_SOURCES.resolve(relativePath));
    }
}
