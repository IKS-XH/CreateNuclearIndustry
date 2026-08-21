package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorStructureDiagnosticsContractTest {
    private static final Path ENGLISH = Path.of(
            "src", "main", "resources", "assets", "create_nuclear_industry", "lang", "en_us.json");
    private static final Path CHINESE = Path.of(
            "src", "main", "resources", "assets", "create_nuclear_industry", "lang", "zh_cn.json");

    @Test
    void mapsAllApprovedFailureClassesToStableCodes() {
        Map<ReactorStructureDefinition.LocalPosition, String> canonical =
                ReactorStructureDefinition.canonicalTemplate();

        Map<ReactorStructureDefinition.LocalPosition, String> structureBlocks = new HashMap<>(canonical);
        structureBlocks.put(new ReactorStructureDefinition.LocalPosition(0, 1, 1),
                ReactorStructureDefinition.AIR_ID);
        assertCode(structureBlocks, ReactorStructureDefinition.DiagnosticCode.STRUCTURE_BLOCKS);

        Map<ReactorStructureDefinition.LocalPosition, String> instrumentPosition = new HashMap<>(canonical);
        instrumentPosition.put(ReactorStructureDefinition.DEFAULT_INSTRUMENT_PORT_POSITION,
                "create_nuclear_industry:reactor_casing");
        instrumentPosition.put(new ReactorStructureDefinition.LocalPosition(0, 0, 0),
                "create_nuclear_industry:reactor_instrument_port");
        assertCode(instrumentPosition, ReactorStructureDefinition.DiagnosticCode.INSTRUMENT_PORT);

        Map<ReactorStructureDefinition.LocalPosition, String> missingCold = new HashMap<>(canonical);
        missingCold.put(ReactorStructureDefinition.DEFAULT_COLD_PORT_POSITION,
                "create_nuclear_industry:reactor_casing");
        assertCode(missingCold, ReactorStructureDefinition.DiagnosticCode.MISSING_COLD_PORT);

        Map<ReactorStructureDefinition.LocalPosition, String> missingHot = new HashMap<>(canonical);
        missingHot.put(ReactorStructureDefinition.DEFAULT_HOT_PORT_POSITION,
                "create_nuclear_industry:reactor_casing");
        assertCode(missingHot, ReactorStructureDefinition.DiagnosticCode.MISSING_HOT_PORT);

        Map<ReactorStructureDefinition.LocalPosition, String> columnLayout = new HashMap<>(canonical);
        columnLayout.put(new ReactorStructureDefinition.LocalPosition(2, 1, 2),
                "create_nuclear_industry:reactor_casing");
        assertCode(columnLayout, ReactorStructureDefinition.DiagnosticCode.COLUMN_LAYOUT);

        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> noFuelLayout = new HashMap<>();
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                noFuelLayout.put(new CoreColumnPosition(x, z),
                        ReactorStructureDefinition.ColumnType.EMPTY);
            }
        }
        assertCode(ReactorStructureDefinition.templateFor(noFuelLayout),
                ReactorStructureDefinition.DiagnosticCode.NO_FUEL);
    }

    @Test
    void everyDiagnosticCodeHasEnglishAndChineseLanguageKeys() throws IOException {
        String english = Files.readString(ENGLISH);
        String chinese = Files.readString(CHINESE);
        for (ReactorStructureDefinition.DiagnosticCode code : ReactorStructureDefinition.DiagnosticCode.values()) {
            assertTrue(english.contains("\"" + code.translationKey() + "\":"),
                    "missing English language key for " + code);
            assertTrue(chinese.contains("\"" + code.translationKey() + "\":"),
                    "missing Chinese language key for " + code);
        }
    }

    @Test
    void successfulDiagnosticCountsFuelControlEmptyAndPorts() {
        ReactorStructureDefinition.ScanResult result = ReactorStructureDefinition.scan(
                ReactorStructureDefinition.canonicalTemplate());
        assertEquals(ReactorStructureDefinition.DiagnosticCode.VALID, result.diagnosticCode());
        assertTrue(ReactorStructureDiagnostics.message(result) != null);
    }

    private static void assertCode(
            Map<ReactorStructureDefinition.LocalPosition, String> blocks,
            ReactorStructureDefinition.DiagnosticCode expected
    ) {
        assertEquals(expected, ReactorStructureDefinition.scan(blocks).diagnosticCode());
    }
}
