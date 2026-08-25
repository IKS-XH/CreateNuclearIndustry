package com.iksxh.create_nuclear_industry.control;

/** Stable server-side outcomes for the instrument-port redstone SCRAM edge. */
public enum ControlRodScramStatus {
    SCRAM_ACTIVE,
    SCRAM_INCOMPLETE,
    SCRAM_ALREADY_ACTIVE,
    SCRAM_RELEASED,
    SCRAM_NOT_ACTIVE,
    SCRAM_UNAVAILABLE_NO_CONTROL_RODS,
    SCRAM_INVALID_STRUCTURE
}
