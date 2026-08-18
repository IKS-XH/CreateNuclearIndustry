package com.iksxh.create_nuclear_industry.reactor;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Immutable aggregate of all per-column fission calculations for one step. */
public record ReactorFissionResult(
        Map<CoreColumnPosition, FuelColumnFissionResult> columns,
        double rawHeatHu,
        double generatedHeatHu,
        double plannedFuelBurnUnits
) {
    public ReactorFissionResult {
        if (columns == null) {
            throw new IllegalArgumentException("fission result columns are required");
        }
        columns = Collections.unmodifiableMap(new TreeMap<>(columns));
        requireFiniteNonNegative("raw heat", rawHeatHu);
        requireFiniteNonNegative("generated heat", generatedHeatHu);
        requireFiniteNonNegative("planned fuel burn", plannedFuelBurnUnits);
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
