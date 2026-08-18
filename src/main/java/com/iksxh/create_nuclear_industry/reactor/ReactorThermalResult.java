package com.iksxh.create_nuclear_industry.reactor;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Immutable result of settling fission heat and local cooling for every fuel column. */
public record ReactorThermalResult(
        ReactorSnapshot snapshot,
        Map<CoreColumnPosition, FuelColumnThermalResult> columns
) {
    public ReactorThermalResult {
        if (snapshot == null || columns == null) {
            throw new IllegalArgumentException("thermal snapshot and column results are required");
        }
        columns = Collections.unmodifiableMap(new TreeMap<>(columns));
    }
}
