package com.iksxh.create_nuclear_industry.structure;

import net.minecraft.network.chat.Component;

/** 将服务端结构扫描结果转换为使用语言键的玩家可见消息。 */
public final class ReactorStructureDiagnostics {
    private ReactorStructureDiagnostics() {
    }

    /** 展开世界扫描包装结果；扫描契约本身负责有效性和诊断代码。 */
    public static Component message(ReactorStructureScanner.WorldScanResult result) {
        return message(result.contract());
    }

    /** 根据结构有效性返回失败原因，或返回列与冷却端口统计摘要。 */
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
