package com.iksxh.create_nuclear_industry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 P1-GOGGLE-INSTRUMENT-04 的动态显示适配、只读边界、字段顺序和语言资源契约。 */
class P1GoggleInstrument04ContractTest {
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");
    private static final Path LANGUAGE_SOURCES =
            Path.of("src", "main", "resources", "assets", "create_nuclear_industry", "lang");

    @Test
    void callbackDelegatesToClientOnlyDynamicDisplayAdapter() throws IOException {
        String instrument = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java");
        String refuelingPort = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorPortBlockEntity.java");
        String controlRodDrive = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ControlRodDriveBlockEntity.java");
        String display = readSource(
                "com/iksxh/create_nuclear_industry/reactor/ReactorInstrumentGoggleDisplay.java");
        String summary = readSource(
                "com/iksxh/create_nuclear_industry/structure/ReactorInstrumentStructureSummary.java");

        assertTrue(instrument.contains("ReactorInstrumentGoggleDisplay.appendDynamicTooltip"));
        assertTrue(refuelingPort.contains("implements IHaveGoggleInformation"));
        assertTrue(refuelingPort.contains("appendFuelColumnTooltip"));
        assertTrue(controlRodDrive.contains("implements IHaveGoggleInformation"));
        assertTrue(controlRodDrive.contains("appendControlRodColumnTooltip"));
        assertTrue(display.contains("客户端"));
        assertTrue(display.contains("不读取世界") || display.contains("不触发"));
        assertTrue(summary.contains("coldInventoryCapacityMb"));
        assertTrue(summary.contains("hotInventoryCapacityMb"));
        assertFalse(display.contains("getBlockEntity"),
                "display adapter must not inspect neighboring block entities");
        assertFalse(display.contains("ReactorFissionCalculator"),
                "display adapter must not recompute fission heat");
        int dynamicStart = display.indexOf("appendDynamicTooltip(");
        int fuelDisplayStart = display.indexOf("appendFuelColumnTooltip(");
        String dynamicMethod = display.substring(dynamicStart, fuelDisplayStart);
        assertFalse(dynamicMethod.contains("fuelColumns()"),
                "instrument display must not enumerate fuel columns");
        assertFalse(dynamicMethod.contains("controlRodColumns()"),
                "instrument display must not enumerate control-rod columns");
    }

    @Test
    void dynamicLanguageKeysKeepDeterministicDisplayOrderAndUnits() throws IOException {
        String[] keys = {
                "goggle.create_nuclear_industry.reactor.structure_summary",
                "goggle.create_nuclear_industry.reactor.unavailable",
                "goggle.create_nuclear_industry.reactor.structure_size",
                "goggle.create_nuclear_industry.reactor.fuel_columns",
                "goggle.create_nuclear_industry.reactor.control_rod_columns",
                "goggle.create_nuclear_industry.reactor.cold_ports",
                "goggle.create_nuclear_industry.reactor.hot_ports",
                "goggle.create_nuclear_industry.reactor.fluid_capacity",
                "goggle.create_nuclear_industry.reactor.dynamic_summary",
                "goggle.create_nuclear_industry.reactor.fuel_column_summary",
                "goggle.create_nuclear_industry.reactor.control_rod_summary",
                "goggle.create_nuclear_industry.reactor.runtime_data_waiting",
                "goggle.create_nuclear_industry.reactor.cold_inventory",
                "goggle.create_nuclear_industry.reactor.hot_inventory",
                "goggle.create_nuclear_industry.reactor.total_fission_heat",
                "goggle.create_nuclear_industry.reactor.coolant_conversion",
                "goggle.create_nuclear_industry.reactor.fuel_column",
                "goggle.create_nuclear_industry.reactor.control_rod_column"
        };
        String chinese = readLanguage("zh_cn.json");
        String english = readLanguage("en_us.json");

        assertKeysInOrder(chinese, keys);
        assertKeysInOrder(english, keys);
        assertTrue(chinese.contains("HU/t") && chinese.contains("mB/t") && chinese.contains("完整度"));
        assertTrue(english.contains("HU/t") && english.contains("mB/t") && english.contains("integrity"));
        assertTrue(chinese.contains("等待首次成功运行数据"));
        assertTrue(english.contains("first successful runtime tick"));
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
