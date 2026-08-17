package com.iksxh.create_nuclear_industry.p0probe.numeric;

/** Immutable previous-tick state for one horizontal column. */
public record ColumnState(
        ColumnKey key,
        ColumnKind kind,
        double fuelRemaining,
        double fuelColumnIntegrity,
        double controlRodColumnIntegrity,
        double controlRodDepth,
        double jammedDepth,
        double cachedHeat,
        boolean jammed
) {
    public ColumnState {
        if (key == null || kind == null) {
            throw new IllegalArgumentException("column key and kind are required");
        }
        requireFiniteNonNegative("fuelRemaining", fuelRemaining);
        requireUnit("fuelColumnIntegrity", fuelColumnIntegrity);
        requireUnit("controlRodColumnIntegrity", controlRodColumnIntegrity);
        requireUnit("controlRodDepth", controlRodDepth);
        requireUnit("jammedDepth", jammedDepth);
        requireFiniteNonNegative("cachedHeat", cachedHeat);
        if (kind != ColumnKind.FUEL && fuelRemaining != 0) {
            throw new IllegalArgumentException("only fuel columns may contain fuel");
        }
        if (kind != ColumnKind.CONTROL_ROD && (controlRodColumnIntegrity != 0 || controlRodDepth != 0
                || jammedDepth != 0 || jammed)) {
            throw new IllegalArgumentException("only control columns may contain control state");
        }
    }

    public static ColumnState fuel(ColumnKey key, double remaining, double integrity, double cachedHeat) {
        return new ColumnState(key, ColumnKind.FUEL, remaining, integrity, 0, 0, 0, cachedHeat, false);
    }

    public static ColumnState controlRod(ColumnKey key, double integrity, double depth) {
        return new ColumnState(key, ColumnKind.CONTROL_ROD, 0, 0, integrity, depth, depth, 0, false);
    }

    public static ColumnState controlRodWithHeat(ColumnKey key, double integrity, double depth, double cachedHeat) {
        return new ColumnState(key, ColumnKind.CONTROL_ROD, 0, 0, integrity, depth, depth, cachedHeat, false);
    }

    public static ColumnState empty(ColumnKey key) {
        return new ColumnState(key, ColumnKind.EMPTY, 0, 0, 0, 0, 0, 0, false);
    }

    public boolean effectiveFuel() {
        return kind == ColumnKind.FUEL && fuelRemaining > 0 && fuelColumnIntegrity > 0;
    }

    public double effectiveControlDepth(ReactorParameters parameters) {
        if (kind != ColumnKind.CONTROL_ROD) {
            return 0;
        }
        return jammed || controlRodColumnIntegrity <= parameters.controlRodColumnFailureThreshold()
                ? jammedDepth : controlRodDepth;
    }

    public ColumnState withFuel(double remaining, double integrity, double nextCachedHeat) {
        return fuel(key, remaining, integrity, nextCachedHeat);
    }

    public ColumnState withControl(double integrity, double depth, double nextCachedHeat, boolean nextJammed,
                                   double nextJammedDepth) {
        return new ColumnState(key, ColumnKind.CONTROL_ROD, 0, 0, integrity, depth, nextJammedDepth,
                nextCachedHeat, nextJammed);
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static void requireUnit(String name, double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " must be in [0, 1]");
        }
    }
}
