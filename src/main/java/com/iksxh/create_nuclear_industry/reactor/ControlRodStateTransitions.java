package com.iksxh.create_nuclear_industry.reactor;

/** 集中维护控制棒状态转换，并保证 P1 的卡死棒不再移动约束。 */
public final class ControlRodStateTransitions {
    private ControlRodStateTransitions() {
    }

    /** 请求新的目标深度；卡死棒保持完整状态不变，深度单位为 [0,1]。 */
    public static ControlRodColumnState requestTargetDepth(ControlRodColumnState state, double targetDepth) {
        requireState(state);
        requireUnitInterval("requested target depth", targetDepth);
        if (state.jammed()) {
            return state;
        }
        return new ControlRodColumnState(
                state.integrity(),
                targetDepth,
                state.actualDepth(),
                false,
                state.cachedHeatHu()
        );
    }

    /** 将可动控制棒的目标深度请求为完全插入。 */
    public static ControlRodColumnState requestScram(ControlRodColumnState state) {
        return requestTargetDepth(state, 1.0D);
    }

    /** 紧急插入同时更新物理深度，保证 SCRAM 边界立即反映完全插入。 */
    public static ControlRodColumnState scramInsert(ControlRodColumnState state) {
        requireState(state);
        if (state.jammed()) {
            return state;
        }
        return new ControlRodColumnState(
                state.integrity(),
                1.0D,
                1.0D,
                false,
                state.cachedHeatHu()
        );
    }

    /** 释放 SCRAM 后恢复保存的目标深度；卡死棒不恢复。 */
    public static ControlRodColumnState restoreTargetDepth(
            ControlRodColumnState state,
            double targetDepth
    ) {
        requireState(state);
        requireUnitInterval("SCRAM restore target depth", targetDepth);
        if (state.jammed()) {
            return state;
        }
        return new ControlRodColumnState(
                state.integrity(),
                targetDepth,
                state.actualDepth(),
                false,
                state.cachedHeatHu()
        );
    }

    /** 更新控制棒实际物理深度；卡死棒保持原值。 */
    public static ControlRodColumnState moveActualDepth(ControlRodColumnState state, double actualDepth) {
        requireState(state);
        requireUnitInterval("requested actual depth", actualDepth);
        if (state.jammed()) {
            return state;
        }
        return new ControlRodColumnState(
                state.integrity(),
                state.targetDepth(),
                actualDepth,
                false,
                state.cachedHeatHu()
        );
    }

    /**
     * 应用一次服务端控制棒 tick。首版契约没有独立棒速参数，因此可动棒在下一 tick
     * 直接到达权威目标；SCRAM 将目标锁定为完全插入，卡死棒逐字段保持不变。
     */
    public static ControlRodColumnState applyServerTick(
            ControlRodColumnState state,
            boolean scramActive
    ) {
        requireState(state);
        if (state.jammed()) {
            return state;
        }
        double nextTarget = scramActive ? 1.0D : state.targetDepth();
        if (state.targetDepth() == nextTarget && state.actualDepth() == nextTarget) {
            return state;
        }
        return new ControlRodColumnState(
                state.integrity(),
                nextTarget,
                nextTarget,
                false,
                state.cachedHeatHu()
        );
    }

    /** 扣减控制棒完整度；低于失效阈值后卡死状态单调成立。 */
    public static ControlRodColumnState applyIntegrityDamage(
            ControlRodColumnState state,
            double damage,
            double failureThreshold
    ) {
        requireState(state);
        requireFiniteNonNegative("control rod integrity damage", damage);
        requireUnitInterval("control rod failure threshold", failureThreshold);
        double nextIntegrity = clamp01(state.integrity() - damage);
        boolean nextJammed = state.jammed() || nextIntegrity <= failureThreshold;
        return new ControlRodColumnState(
                nextIntegrity,
                state.targetDepth(),
                state.actualDepth(),
                nextJammed,
                state.cachedHeatHu()
        );
    }

    /** 修复控制棒完整度；只有超过失效阈值才解除既有卡死状态。 */
    public static ControlRodColumnState repairIntegrity(
            ControlRodColumnState state,
            double repairAmount,
            double failureThreshold
    ) {
        requireState(state);
        requireFiniteNonNegative("control rod integrity repair", repairAmount);
        requireUnitInterval("control rod failure threshold", failureThreshold);
        double nextIntegrity = clamp01(state.integrity() + repairAmount);
        boolean nextJammed = state.jammed() && nextIntegrity <= failureThreshold;
        return new ControlRodColumnState(
                nextIntegrity,
                state.targetDepth(),
                state.actualDepth(),
                nextJammed,
                state.cachedHeatHu()
        );
    }

    private static void requireState(ControlRodColumnState state) {
        if (state == null) {
            throw new IllegalArgumentException("control rod state is required");
        }
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static void requireUnitInterval(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]");
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
