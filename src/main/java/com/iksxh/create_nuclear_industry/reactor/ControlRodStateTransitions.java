package com.iksxh.create_nuclear_industry.reactor;

/** Central state transitions that preserve the P1 control-rod jamming contract. */
public final class ControlRodStateTransitions {
    private ControlRodStateTransitions() {
    }

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

    public static ControlRodColumnState requestScram(ControlRodColumnState state) {
        return requestTargetDepth(state, 1.0D);
    }

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
