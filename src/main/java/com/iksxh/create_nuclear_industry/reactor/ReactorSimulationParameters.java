package com.iksxh.create_nuclear_industry.reactor;

/** Validated, injectable numeric inputs for the loader-independent P1 simulation. */
public record ReactorSimulationParameters(
        double baseHeatPerFuelBlockHuPerTick,
        double burnHoursPerBlock,
        double damageHeatThresholdHuPerTick,
        double damageRatePerTickHuLoad,
        double damageTransferRate,
        double controlRodFailureThreshold,
        double meltdownTriggerFraction,
        int meltdownCountdownTicks,
        double controlResponseExponent,
        double overclockHeatMultiplier,
        double overclockBurnMultiplier,
        double overclockFeedbackGain,
        double overclockFeedbackExponent,
        double totalHeatMultiplierCap
) {
    public ReactorSimulationParameters {
        requireFiniteNonNegative("base heat", baseHeatPerFuelBlockHuPerTick);
        requireFinitePositive("burn hours", burnHoursPerBlock);
        requireFiniteNonNegative("damage threshold", damageHeatThresholdHuPerTick);
        requireFiniteNonNegative("damage rate", damageRatePerTickHuLoad);
        requireUnitInterval("damage transfer rate", damageTransferRate);
        requireUnitInterval("control rod failure threshold", controlRodFailureThreshold);
        requireUnitInterval("meltdown trigger fraction", meltdownTriggerFraction);
        if (meltdownCountdownTicks <= 0) {
            throw new IllegalArgumentException("meltdown countdown ticks must be positive");
        }
        requireFinitePositive("control response exponent", controlResponseExponent);
        requireFiniteAtLeastOne("overclock heat multiplier", overclockHeatMultiplier);
        requireFiniteAtLeastOne("overclock burn multiplier", overclockBurnMultiplier);
        requireFiniteNonNegative("overclock feedback gain", overclockFeedbackGain);
        requireFinitePositive("overclock feedback exponent", overclockFeedbackExponent);
        requireFinitePositive("total heat multiplier cap", totalHeatMultiplierCap);
    }

    public static ReactorSimulationParameters defaults() {
        return new ReactorSimulationParameters(
                1.0D,
                3.0D,
                0.25D,
                0.00125D,
                0.25D,
                0.0D,
                0.20D,
                900,
                1.0D,
                10.0D,
                10.0D,
                0.15D,
                0.5D,
                20.0D
        );
    }

    public double baseBurnPerFuelBlockPerTick() {
        return 1.0D / (burnHoursPerBlock * 3_600.0D * 20.0D);
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static void requireFinitePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    private static void requireFiniteAtLeastOne(String name, double value) {
        if (!Double.isFinite(value) || value < 1.0D) {
            throw new IllegalArgumentException(name + " must be finite and at least one");
        }
    }

    private static void requireUnitInterval(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]");
        }
    }
}
