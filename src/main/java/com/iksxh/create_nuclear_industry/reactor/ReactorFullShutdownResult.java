package com.iksxh.create_nuclear_industry.reactor;

/**
 * 完全停机判定的不可变结果。
 *
 * <p>结果只描述服务端权威快照在一次求值时的数值状态，不携带世界对象，也不承担
 * 快照提交、方块修改或资源消耗职责。热量单位为 HU，计划燃耗单位为燃料组件比例。</p>
 */
public record ReactorFullShutdownResult(
        boolean fullyStopped,
        boolean inputValid,
        boolean generatedHeatSafe,
        boolean plannedFuelBurnSafe,
        boolean activeResidualHeatSafe,
        boolean meltdownCountdownInactive,
        double generatedHeatHu,
        double plannedFuelBurnUnits,
        double activeResidualHeatHu,
        double safeQuantizedHeatRemainderHu
) {
    public ReactorFullShutdownResult {
        requireFiniteNonNegative("generated heat", generatedHeatHu);
        requireFiniteNonNegative("planned fuel burn", plannedFuelBurnUnits);
        requireFiniteNonNegative("active residual heat", activeResidualHeatHu);
        requireFiniteNonNegative("safe quantized heat remainder", safeQuantizedHeatRemainderHu);
        boolean expectedFullyStopped = inputValid
                && generatedHeatSafe
                && plannedFuelBurnSafe
                && activeResidualHeatSafe
                && meltdownCountdownInactive;
        if (fullyStopped != expectedFullyStopped) {
            throw new IllegalArgumentException("fully stopped result does not match its conditions");
        }
    }

    /** 返回输入无效或计算异常时的失败关闭结果。 */
    public static ReactorFullShutdownResult invalid() {
        return new ReactorFullShutdownResult(
                false,
                false,
                false,
                false,
                false,
                false,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
