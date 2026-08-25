package com.iksxh.create_nuclear_industry.control;

/** Result of one server-side redstone SCRAM state transition. */
public record ControlRodScramResult(
        ControlRodScramStatus status,
        boolean scramRequested,
        boolean scramActive,
        double fissionHeatHu,
        String reason
) {
    public boolean incomplete() {
        return status == ControlRodScramStatus.SCRAM_INCOMPLETE;
    }
}
