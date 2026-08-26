package com.iksxh.create_nuclear_industry.control;

/** 仪表端口红石 SCRAM 边沿的稳定服务端结果类别。 */
public enum ControlRodScramStatus {
    SCRAM_ACTIVE,
    SCRAM_INCOMPLETE,
    SCRAM_ALREADY_ACTIVE,
    SCRAM_RELEASED,
    SCRAM_NOT_ACTIVE,
    SCRAM_UNAVAILABLE_NO_CONTROL_RODS,
    SCRAM_INVALID_STRUCTURE
}
