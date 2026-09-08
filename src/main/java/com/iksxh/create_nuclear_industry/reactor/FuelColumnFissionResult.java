package com.iksxh.create_nuclear_industry.reactor;

/** 一次模拟步中单根燃料列结算后的裂变热、燃耗需求和控制反馈结果。热量单位为 HU。 */
public record FuelColumnFissionResult(
        double controlledIntensity,
        double heatIntensity,
        double burnIntensity,
        double damageHeatMultiplier,
        double damageBurnMultiplier,
        double generatedHeatHu,
        double plannedFuelBurnUnits,
        boolean overclocked
) {
    public FuelColumnFissionResult {
        requireFiniteNonNegative("controlled intensity", controlledIntensity);
        requireFiniteNonNegative("heat intensity", heatIntensity);
        requireFiniteNonNegative("burn intensity", burnIntensity);
        requireFiniteNonNegative("damage heat multiplier", damageHeatMultiplier);
        requireFiniteNonNegative("damage burn multiplier", damageBurnMultiplier);
        requireFiniteNonNegative("generated heat", generatedHeatHu);
        requireFiniteNonNegative("planned fuel burn", plannedFuelBurnUnits);
    }

    public static FuelColumnFissionResult inactive() {
        return new FuelColumnFissionResult(0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D, 0.0D, false);
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
