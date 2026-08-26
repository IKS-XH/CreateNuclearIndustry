package com.iksxh.create_nuclear_industry.p0probe.numeric;

import java.util.Map;
import java.util.TreeMap;

/** P0 服务端权威 SCRAM/滑块边界的纯状态机，仅供历史模型回归。 */
public final class ControlRodController {
    private final Map<ColumnKey, Double> savedDepths = new TreeMap<>();
    private boolean scram;

    /** 返回当前 P0 SCRAM 请求状态。 */
    public boolean isScram() {
        return scram;
    }

    /** 切换 SCRAM 电平并保存可动控制棒的目标深度。 */
    public boolean setScram(Map<ColumnKey, ColumnState> columns, boolean powered) {
        if (scram == powered) {
            return false;
        }
        scram = powered;
        if (powered) {
            savedDepths.clear();
            columns.values().stream()
                    .filter(column -> column.kind() == ColumnKind.CONTROL_ROD && !column.jammed())
                    .forEach(column -> savedDepths.put(column.key(), column.controlRodDepth()));
        }
        return true;
    }

    /** 在未 SCRAM 且控制棒可动时更新目标深度，深度单位为 [0,1]。 */
    public boolean setDepth(Map<ColumnKey, ColumnState> columns, ColumnKey key, double requestedDepth) {
        if (scram || !Double.isFinite(requestedDepth) || requestedDepth < 0 || requestedDepth > 1) {
            return false;
        }
        ColumnState column = columns.get(key);
        if (column == null || column.kind() != ColumnKind.CONTROL_ROD || column.jammed()) {
            return false;
        }
        columns.put(key, column.withControl(column.controlRodColumnIntegrity(), requestedDepth,
                column.cachedHeat(), false, column.jammedDepth()));
        return true;
    }

    /** 将 SCRAM 插入或保存目标恢复应用到列映射，并返回新映射。 */
    public Map<ColumnKey, ColumnState> apply(Map<ColumnKey, ColumnState> columns) {
        Map<ColumnKey, ColumnState> result = new TreeMap<>(columns);
        for (ColumnState column : columns.values()) {
            if (column.kind() != ColumnKind.CONTROL_ROD || column.jammed()) {
                continue;
            }
            if (scram) {
                result.put(column.key(), column.withControl(column.controlRodColumnIntegrity(), 1.0,
                        column.cachedHeat(), false, column.jammedDepth()));
            } else if (savedDepths.containsKey(column.key())) {
                result.put(column.key(), column.withControl(column.controlRodColumnIntegrity(),
                        savedDepths.get(column.key()), column.cachedHeat(), false, column.jammedDepth()));
            }
        }
        return result;
    }
}
