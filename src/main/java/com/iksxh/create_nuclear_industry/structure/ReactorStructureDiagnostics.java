package com.iksxh.create_nuclear_industry.structure;

import net.minecraft.network.chat.Component;

/** Builds the player-facing, language-backed result of a server structure scan. */
public final class ReactorStructureDiagnostics {
    private ReactorStructureDiagnostics() {
    }

    public static Component message(ReactorStructureScanner.WorldScanResult result) {
        return message(result.contract());
    }

    public static Component message(ReactorStructureDefinition.ScanResult result) {
        if (!result.valid()) {
            return Component.translatable(result.diagnosticCode().translationKey());
        }

        int fuelColumns = countColumns(result, ReactorStructureDefinition.ColumnType.FUEL);
        int controlRodColumns = countColumns(result, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        int emptyColumns = countColumns(result, ReactorStructureDefinition.ColumnType.EMPTY);
        int coldPorts = result.ports().getOrDefault(
                ReactorStructureDefinition.PortType.COLD_COOLANT, java.util.List.of()).size();
        int hotPorts = result.ports().getOrDefault(
                ReactorStructureDefinition.PortType.HOT_COOLANT, java.util.List.of()).size();
        return Component.translatable(
                ReactorStructureDefinition.DiagnosticCode.VALID.translationKey(),
                fuelColumns, controlRodColumns, emptyColumns, coldPorts, hotPorts);
    }

    private static int countColumns(
            ReactorStructureDefinition.ScanResult result,
            ReactorStructureDefinition.ColumnType type
    ) {
        return (int) result.columns().values().stream()
                .filter(column -> column.type() == type)
                .count();
    }
}
