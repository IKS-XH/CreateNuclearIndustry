package com.iksxh.create_nuclear_industry.reactor;

/**
 * 评估反应堆是否达到完全停机条件的纯逻辑入口。
 *
 * <p>调用方应传入同一次服务端权威 tick 使用的 {@link ReactorSnapshot} 和
 * {@link ReactorSimulationParameters}。本类只读取输入并调用一次裂变计算器，不接触
 * 世界、方块实体、流体事务或控制状态，因此可由后续结构重组成型逻辑复用。</p>
 */
public final class ReactorFullShutdownAssessment {
    /** 新生热、计划燃耗和活动余热共用的安全 epsilon。 */
    public static final double SAFETY_EPSILON = 1.0E-12D;

    private ReactorFullShutdownAssessment() {
    }

    /**
     * 使用一次裂变计算判断快照是否完全停机；空、无效或计算异常输入均失败关闭。
     *
     * <p>活动余热按燃料列快照的确定性顺序求和。量化余数先限制在该列缓存热量以内，
     * 之后从缓存热量中扣除；控制棒列的历史缓存热量不在本判定中再次作为热源，
     * 因为现有热传播模型已将其结算为控制棒完整度损失。</p>
     */
    public static ReactorFullShutdownResult assess(
            ReactorSnapshot snapshot,
            ReactorSimulationParameters parameters
    ) {
        if (snapshot == null || parameters == null) {
            return ReactorFullShutdownResult.invalid();
        }

        try {
            ReactorFissionResult fission = ReactorFissionCalculator.calculate(snapshot, parameters);
            requireFiniteNonNegative("generated heat", fission.generatedHeatHu());
            requireFiniteNonNegative("planned fuel burn", fission.plannedFuelBurnUnits());
            HeatTotals heatTotals = calculateHeatTotals(snapshot);

            boolean generatedHeatSafe = fission.generatedHeatHu() <= SAFETY_EPSILON;
            boolean plannedFuelBurnSafe = fission.plannedFuelBurnUnits() <= SAFETY_EPSILON;
            boolean activeResidualHeatSafe = heatTotals.activeResidualHeatHu() <= SAFETY_EPSILON;
            boolean meltdownCountdownInactive = !snapshot.meltdownCountdownStarted();
            boolean fullyStopped = generatedHeatSafe
                    && plannedFuelBurnSafe
                    && activeResidualHeatSafe
                    && meltdownCountdownInactive;
            return new ReactorFullShutdownResult(
                    fullyStopped,
                    true,
                    generatedHeatSafe,
                    plannedFuelBurnSafe,
                    activeResidualHeatSafe,
                    meltdownCountdownInactive,
                    fission.generatedHeatHu(),
                    fission.plannedFuelBurnUnits(),
                    heatTotals.activeResidualHeatHu(),
                    heatTotals.safeQuantizedHeatRemainderHu()
            );
        } catch (RuntimeException exception) {
            return ReactorFullShutdownResult.invalid();
        }
    }

    private static HeatTotals calculateHeatTotals(ReactorSnapshot snapshot) {
        double activeResidualHeatHu = 0.0D;
        double safeQuantizedHeatRemainderHu = 0.0D;
        for (FuelColumnState fuelColumn : snapshot.fuelColumns().values()) {
            double cachedHeatHu = fuelColumn.cachedHeatHu();
            double safeRemainderHu = Math.max(
                    0.0D,
                    Math.min(cachedHeatHu, fuelColumn.quantizedHeatRemainderHu())
            );
            double activeHeatHu = Math.max(0.0D, cachedHeatHu - safeRemainderHu);
            activeResidualHeatHu = addFiniteNonNegative(activeResidualHeatHu, activeHeatHu);
            safeQuantizedHeatRemainderHu = addFiniteNonNegative(
                    safeQuantizedHeatRemainderHu,
                    safeRemainderHu
            );
        }
        return new HeatTotals(activeResidualHeatHu, safeQuantizedHeatRemainderHu);
    }

    private static double addFiniteNonNegative(double first, double second) {
        requireFiniteNonNegative("heat component", second);
        double sum = first + second;
        requireFiniteNonNegative("heat total", sum);
        return sum;
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private record HeatTotals(
            double activeResidualHeatHu,
            double safeQuantizedHeatRemainderHu
    ) {
    }
}
