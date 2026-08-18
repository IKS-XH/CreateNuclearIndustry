package com.iksxh.create_nuclear_industry.reactor;

/** Immutable target, physical and damage state for one control-rod column. */
public record ControlRodColumnState(
        double integrity,
        double targetDepth,
        double actualDepth,
        boolean jammed,
        double cachedHeatHu
) {
    public ControlRodColumnState {
        requireUnitInterval("control rod column integrity", integrity);
        requireUnitInterval("control rod target depth", targetDepth);
        requireUnitInterval("control rod actual depth", actualDepth);
        requireFiniteNonNegative("control rod column cached heat", cachedHeatHu);
    }

    public static ControlRodColumnState fullyInserted() {
        return new ControlRodColumnState(1.0D, 1.0D, 1.0D, false, 0.0D);
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
