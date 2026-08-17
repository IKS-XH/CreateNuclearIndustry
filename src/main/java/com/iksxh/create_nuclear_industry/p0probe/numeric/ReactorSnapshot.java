package com.iksxh.create_nuclear_industry.p0probe.numeric;

import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;

public record ReactorSnapshot(
        Map<ColumnKey, ColumnState> columns,
        double meltdownProgress,
        boolean meltdownTriggered,
        long tick
) {
    public ReactorSnapshot {
        TreeMap<ColumnKey, ColumnState> ordered = new TreeMap<>();
        if (columns != null) {
            columns.forEach((key, value) -> {
                if (key == null || value == null || !key.equals(value.key())) {
                    throw new IllegalArgumentException("column map must contain matching non-null keys");
                }
                ordered.put(key, value);
            });
        }
        columns = Collections.unmodifiableMap(ordered);
        if (!Double.isFinite(meltdownProgress) || meltdownProgress < 0) {
            throw new IllegalArgumentException("meltdownProgress must be finite and non-negative");
        }
        if (tick < 0) {
            throw new IllegalArgumentException("tick must be non-negative");
        }
    }

    public static ReactorSnapshot empty() {
        return new ReactorSnapshot(Map.of(), 0, false, 0);
    }

    public boolean allColumnIntegrityIsFull() {
        return columns.values().stream().allMatch(column ->
                (column.kind() != ColumnKind.FUEL || column.fuelColumnIntegrity() >= 1.0)
                        && (column.kind() != ColumnKind.CONTROL_ROD || column.controlRodColumnIntegrity() >= 1.0));
    }

    public ReactorSnapshot resetMeltdownIfAllRepaired() {
        if (!allColumnIntegrityIsFull()) {
            return this;
        }
        return new ReactorSnapshot(columns, 0, false, tick);
    }
}
