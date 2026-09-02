package com.iksxh.create_nuclear_industry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 GOGGLE-INSTRUMENT-01 的服务端缓存边界和唯一状态所有权。 */
class P1GoggleInstrument01ContractTest {
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");

    @Test
    void instrumentPortExposesAStaticSummaryFromCacheAndServerConfig() throws IOException {
        String instrument = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java");
        String summary = readSource(
                "com/iksxh/create_nuclear_industry/structure/ReactorInstrumentStructureSummary.java");

        assertTrue(instrument.contains("ReactorInstrumentStructureSummary structureSummary()"));
        assertTrue(instrument.contains("P1ServerConfig.VALUES.coldInventoryCapacityMb.get()"));
        assertTrue(instrument.contains("P1ServerConfig.VALUES.hotInventoryCapacityMb.get()"));
        assertTrue(instrument.contains("ReactorInstrumentStructureSummary.from("));
        assertTrue(summary.contains("totalFluidCapacityMb"));
        assertTrue(summary.contains("distinct()"));
        assertFalse(summary.contains("ReactorSnapshot snapshot"),
                "static structure summary must not duplicate the authoritative runtime snapshot");
    }

    @Test
    void staticSummaryDoesNotAddAWorldScanLoopOrDynamicRuntimeFields() throws IOException {
        String instrument = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java");
        String summary = readSource(
                "com/iksxh/create_nuclear_industry/structure/ReactorInstrumentStructureSummary.java");

        assertFalse(summary.contains("coldCoolantMb"));
        assertFalse(summary.contains("hotCoolantMb"));
        assertFalse(summary.contains("generatedHeat"));
        assertFalse(summary.contains("ReactorServerTick"));
        assertFalse(instrument.contains("scanInstrumentPort(level"),
                "reading the summary must not invoke the world scanner");
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(JAVA_SOURCES.resolve(relativePath));
    }
}
