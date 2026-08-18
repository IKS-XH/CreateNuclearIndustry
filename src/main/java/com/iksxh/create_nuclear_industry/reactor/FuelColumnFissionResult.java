package com.iksxh.create_nuclear_industry.reactor;

/** Settled fission heat and fuel demand for one fuel column during one simulation step. */
public record FuelColumnFissionResult(
        double controlledIntensity,
        double heatIntensity,
        double burnIntensity,
        double damageMultiplier,
        double generatedHeatHu,
        double plannedFuelBurnUnits,
        boolean overclocked
) {
    public FuelColumnFissionResult {
        requireFiniteNonNegative("controlled intensity", controlledIntensity);
        requireFiniteNonNegative("heat intensity", heatIntensity);
        requireFiniteNonNegative("burn intensity", burnIntensity);
        requireFiniteNonNegative("damage multiplier", damageMultiplier);
        requireFiniteNonNegative("generated heat", generatedHeatHu);
        requireFiniteNonNegative("planned fuel burn", plannedFuelBurnUnits);
    }

    public static FuelColumnFissionResult inactive() {
        return new FuelColumnFissionResult(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false);
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
