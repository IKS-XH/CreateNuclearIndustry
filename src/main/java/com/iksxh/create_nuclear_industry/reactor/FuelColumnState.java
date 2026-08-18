package com.iksxh.create_nuclear_industry.reactor;

import java.util.Objects;

/** Immutable state owned by one fuel column in the server-authoritative reactor snapshot. */
public record FuelColumnState(
        FuelAssemblyState fuelAssembly,
        double integrity,
        double cachedHeatHu
) {
    public FuelColumnState {
        Objects.requireNonNull(fuelAssembly, "fuelAssembly");
        requireUnitInterval("fuel column integrity", integrity);
        requireFiniteNonNegative("fuel column cached heat", cachedHeatHu);
    }

    public static FuelColumnState empty() {
        return new FuelColumnState(FuelAssemblyState.empty(), 1.0D, 0.0D);
    }

    public boolean hasUsableFuel() {
        return fuelAssembly.present() && !fuelAssembly.exhausted();
    }

    public boolean isEffectiveFuel() {
        return hasUsableFuel() && integrity > 0.0D;
    }

    private static void requireUnitInterval(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]");
        }
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
