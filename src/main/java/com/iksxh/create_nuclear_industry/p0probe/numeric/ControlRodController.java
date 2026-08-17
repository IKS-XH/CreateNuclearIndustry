package com.iksxh.create_nuclear_industry.p0probe.numeric;

import java.util.Map;
import java.util.TreeMap;

/** Pure state machine for the server-authoritative SCRAM/slider boundary. */
public final class ControlRodController {
    private final Map<ColumnKey, Double> savedDepths = new TreeMap<>();
    private boolean scram;

    public boolean isScram() {
        return scram;
    }

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
