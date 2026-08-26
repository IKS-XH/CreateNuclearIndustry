package com.iksxh.create_nuclear_industry.reactor;

/** 将传播覆盖率与暂停条件应用到融毁倒计时后的结果；覆盖率保持在 [0,1]。 */
public record MeltdownUpdateResult(
        ReactorSnapshot snapshot,
        MeltdownStatus status,
        double propagationCoverageFraction,
        boolean dangerThresholdReached
) {
    public MeltdownUpdateResult {
        if (snapshot == null || status == null) {
            throw new IllegalArgumentException("meltdown snapshot and status are required");
        }
        if (!Double.isFinite(propagationCoverageFraction)
                || propagationCoverageFraction < 0.0D
                || propagationCoverageFraction > 1.0D) {
            throw new IllegalArgumentException("propagation coverage must be finite and in [0, 1]");
        }
    }
}
