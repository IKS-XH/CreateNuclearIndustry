package com.iksxh.create_nuclear_industry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 P1-GOGGLE-INSTRUMENT-02 的护目镜显示、同步边界、字段顺序和语言资源契约。 */
class P1GoggleInstrument02ContractTest {
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");
    private static final Path LANGUAGE_SOURCES =
            Path.of("src", "main", "resources", "assets", "create_nuclear_industry", "lang");

    @Test
    void instrumentPortUsesCreateGoggleApiAndClientOnlySummaryCopy() throws IOException {
        String instrument = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java");

        assertTrue(instrument.contains("implements IHaveGoggleInformation"));
        assertTrue(instrument.contains("addToGoggleTooltip(List<Component> tooltip"));
        assertTrue(instrument.contains("private ReactorInstrumentStructureSummary clientStructureSummary"));
        assertTrue(instrument.contains("if (clientPacket)"));
        assertTrue(instrument.contains("STRUCTURE_SUMMARY_KEY"));
        assertTrue(instrument.contains("level.sendBlockUpdated"));

        int callbackStart = instrument.indexOf("addToGoggleTooltip(List<Component> tooltip");
        int callbackEnd = instrument.indexOf("\n    @Override", callbackStart + 1);
        String callback = instrument.substring(callbackStart, callbackEnd);
        assertFalse(callback.contains("structureSummary()"),
                "goggle rendering must not read server structure cache or trigger a scan");
        assertFalse(callback.contains("getBlockEntity"),
                "goggle rendering must not scan or inspect neighboring block entities");
        assertFalse(callback.contains("send"),
                "goggle rendering must not issue a network request");
    }

    @Test
    void goggleLanguageKeysKeepSummaryOrderAndDoNotTouchPonderKeys() throws IOException {
        String[] keys = {
                "goggle.create_nuclear_industry.reactor.structure_summary",
                "goggle.create_nuclear_industry.reactor.unavailable",
                "goggle.create_nuclear_industry.reactor.structure_size",
                "goggle.create_nuclear_industry.reactor.fuel_columns",
                "goggle.create_nuclear_industry.reactor.control_rod_columns",
                "goggle.create_nuclear_industry.reactor.cold_ports",
                "goggle.create_nuclear_industry.reactor.hot_ports",
                "goggle.create_nuclear_industry.reactor.fluid_capacity"
        };
        String chinese = readLanguage("zh_cn.json");
        String english = readLanguage("en_us.json");

        assertKeysInOrder(chinese, keys);
        assertKeysInOrder(english, keys);
        assertTrue(chinese.contains("mB"));
        assertTrue(english.contains("mB"));
        assertTrue(chinese.contains("不是当前存量"));
        assertTrue(english.contains("not current stock"));
        assertFalse(chinese.contains("goggle.create_nuclear_industry.ponder"));
        assertFalse(english.contains("goggle.create_nuclear_industry.ponder"));
    }

    private static void assertKeysInOrder(String language, String[] keys) {
        int previous = -1;
        for (String key : keys) {
            int current = language.indexOf('"' + key + '"');
            assertTrue(current > previous, "missing or out-of-order language key: " + key);
            previous = current;
        }
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(JAVA_SOURCES.resolve(relativePath));
    }

    private static String readLanguage(String fileName) throws IOException {
        return Files.readString(LANGUAGE_SOURCES.resolve(fileName));
    }
}
