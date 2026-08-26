package com.iksxh.create_nuclear_industry.p0probe.numeric;

/**
 * 可注入的 P0 默认参数；本类中的值尚不是正式服务端配置。
 * 修复物品成本与修复量故意不放在这里，以避免把历史原型当成 P1 玩法契约。
 */
public record ReactorParameters(
        double baseHeatPerFuel,
        double burnHoursPerBlock,
        double coolantAbsorptionHuPerMb,
        double coolantMaxFlowPerPort,
        double fuelColumnDamageHeatThreshold,
        double fuelColumnDamageRate,
        double fuelColumnDamageTransferRate,
        double controlRodColumnFailureThreshold,
        double meltdownTriggerFraction,
        int meltdownCountdownTicks,
        double controlResponseExponent,
        double overclockHeatMultiplier,
        double overclockBurnMultiplier,
        double overclockFeedbackGain,
        double overclockFeedbackExponent,
        double totalHeatMultiplierCap,
        double fuelCapacityPerBlock
) {
    public ReactorParameters {
        requireFiniteNonNegative("baseHeatPerFuel", baseHeatPerFuel);
        requireFinitePositive("burnHoursPerBlock", burnHoursPerBlock);
        requireFinitePositive("coolantAbsorptionHuPerMb", coolantAbsorptionHuPerMb);
        requireFiniteNonNegative("coolantMaxFlowPerPort", coolantMaxFlowPerPort);
        requireFiniteNonNegative("fuelColumnDamageHeatThreshold", fuelColumnDamageHeatThreshold);
        requireFiniteNonNegative("fuelColumnDamageRate", fuelColumnDamageRate);
        requireFiniteNonNegative("fuelColumnDamageTransferRate", fuelColumnDamageTransferRate);
        requireFiniteNonNegative("controlRodColumnFailureThreshold", controlRodColumnFailureThreshold);
        if (meltdownTriggerFraction < 0 || meltdownTriggerFraction > 1 || !Double.isFinite(meltdownTriggerFraction)) {
            throw new IllegalArgumentException("meltdownTriggerFraction must be in [0, 1]");
        }
        if (meltdownCountdownTicks <= 0) {
            throw new IllegalArgumentException("meltdownCountdownTicks must be positive");
        }
        requireFinitePositive("controlResponseExponent", controlResponseExponent);
        requireFiniteNonNegative("overclockHeatMultiplier", overclockHeatMultiplier);
        requireFiniteNonNegative("overclockBurnMultiplier", overclockBurnMultiplier);
        requireFiniteNonNegative("overclockFeedbackGain", overclockFeedbackGain);
        requireFinitePositive("overclockFeedbackExponent", overclockFeedbackExponent);
        requireFinitePositive("totalHeatMultiplierCap", totalHeatMultiplierCap);
        requireFinitePositive("fuelCapacityPerBlock", fuelCapacityPerBlock);
    }

    /** 返回 P0 历史回归使用的默认参数。 */
    public static ReactorParameters defaults() {
        return new ReactorParameters(
                1.0,
                3.0,
                0.5,
                128.0,
                0.25,
                0.0000005,
                0.25,
                0.0,
                0.20,
                900,
                1.0,
                10.0,
                10.0,
                0.15,
                0.5,
                20.0,
                1.0
        );
    }

    /** 在满功率且无邻列反馈时每 tick 消耗的燃料块等效量。 */
    public double baseBurnPerFuel() {
        return fuelCapacityPerBlock / (burnHoursPerBlock * 3600.0 * 20.0);
    }

    /** 创建可修改数值后重新校验的构建器。 */
    public Builder toBuilder() {
        return new Builder(this);
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static void requireFinitePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    /** P0 参数的受控修改器；build 时重新执行全部数值校验。 */
    public static final class Builder {
        private double baseHeatPerFuel;
        private double burnHoursPerBlock;
        private double coolantAbsorptionHuPerMb;
        private double coolantMaxFlowPerPort;
        private double fuelColumnDamageHeatThreshold;
        private double fuelColumnDamageRate;
        private double fuelColumnDamageTransferRate;
        private double controlRodColumnFailureThreshold;
        private double meltdownTriggerFraction;
        private int meltdownCountdownTicks;
        private double controlResponseExponent;
        private double overclockHeatMultiplier;
        private double overclockBurnMultiplier;
        private double overclockFeedbackGain;
        private double overclockFeedbackExponent;
        private double totalHeatMultiplierCap;
        private double fuelCapacityPerBlock;

        private Builder(ReactorParameters source) {
            baseHeatPerFuel = source.baseHeatPerFuel;
            burnHoursPerBlock = source.burnHoursPerBlock;
            coolantAbsorptionHuPerMb = source.coolantAbsorptionHuPerMb;
            coolantMaxFlowPerPort = source.coolantMaxFlowPerPort;
            fuelColumnDamageHeatThreshold = source.fuelColumnDamageHeatThreshold;
            fuelColumnDamageRate = source.fuelColumnDamageRate;
            fuelColumnDamageTransferRate = source.fuelColumnDamageTransferRate;
            controlRodColumnFailureThreshold = source.controlRodColumnFailureThreshold;
            meltdownTriggerFraction = source.meltdownTriggerFraction;
            meltdownCountdownTicks = source.meltdownCountdownTicks;
            controlResponseExponent = source.controlResponseExponent;
            overclockHeatMultiplier = source.overclockHeatMultiplier;
            overclockBurnMultiplier = source.overclockBurnMultiplier;
            overclockFeedbackGain = source.overclockFeedbackGain;
            overclockFeedbackExponent = source.overclockFeedbackExponent;
            totalHeatMultiplierCap = source.totalHeatMultiplierCap;
            fuelCapacityPerBlock = source.fuelCapacityPerBlock;
        }

        public Builder baseHeatPerFuel(double value) { baseHeatPerFuel = value; return this; }
        public Builder burnHoursPerBlock(double value) { burnHoursPerBlock = value; return this; }
        public Builder fuelColumnDamageRate(double value) { fuelColumnDamageRate = value; return this; }
        public Builder fuelColumnDamageTransferRate(double value) { fuelColumnDamageTransferRate = value; return this; }
        public Builder meltdownTriggerFraction(double value) { meltdownTriggerFraction = value; return this; }
        public Builder meltdownCountdownTicks(int value) { meltdownCountdownTicks = value; return this; }

        public ReactorParameters build() {
            return new ReactorParameters(baseHeatPerFuel, burnHoursPerBlock, coolantAbsorptionHuPerMb,
                    coolantMaxFlowPerPort, fuelColumnDamageHeatThreshold,
                    fuelColumnDamageRate, fuelColumnDamageTransferRate, controlRodColumnFailureThreshold,
                    meltdownTriggerFraction, meltdownCountdownTicks, controlResponseExponent,
                    overclockHeatMultiplier, overclockBurnMultiplier, overclockFeedbackGain,
                    overclockFeedbackExponent, totalHeatMultiplierCap, fuelCapacityPerBlock);
        }
    }
}
