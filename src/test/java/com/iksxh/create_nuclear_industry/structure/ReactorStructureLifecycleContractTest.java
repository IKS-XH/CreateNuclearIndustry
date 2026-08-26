package com.iksxh.create_nuclear_industry.structure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证方块事件重扫、服务端 tick 末端合并和扳手入口，禁止 per-tick 全量扫描。 */
class ReactorStructureLifecycleContractTest {
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");

    @Test
    void registersLifecycleEventsAndWrenchRescan() throws IOException {
        String main = readSource("com/iksxh/create_nuclear_industry/CreateNuclearIndustry.java");
        String lifecycle = readSource("com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java");
        String instrumentBlock = readSource("com/iksxh/create_nuclear_industry/block/ReactorInstrumentPortBlock.java");

        assertTrue(main.contains("NeoForge.EVENT_BUS.register(ReactorStructureLifecycle.class)"));
        assertTrue(lifecycle.contains("onBlockBreak"));
        assertTrue(lifecycle.contains("onBlockPlace"));
        assertTrue(lifecycle.contains("scheduleRescanAround"));
        assertTrue(instrumentBlock.contains("IWrenchable"));
        assertTrue(instrumentBlock.contains("onWrenched"));
    }

    @Test
    void scannerAndLifecycleDoNotIntroduceAFullTickScanLoop() throws IOException {
        String scanner = readSource("com/iksxh/create_nuclear_industry/structure/ReactorStructureScanner.java");
        String lifecycle = readSource("com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java");
        String instrument = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java");

        for (String source : new String[]{scanner, lifecycle, instrument}) {
            assertFalse(source.contains("tick("), "structure scanning must not be implemented as a ticker");
        }
        assertTrue(scanner.contains("ReactorStructureDefinition.scan"));
        assertTrue(instrument.contains("updateStructureCache"));
        assertTrue(instrument.contains("structureScanCount"));
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(JAVA_SOURCES.resolve(relativePath));
    }
}
