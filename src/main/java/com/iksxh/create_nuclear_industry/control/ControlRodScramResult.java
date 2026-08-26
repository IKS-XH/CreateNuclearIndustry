package com.iksxh.create_nuclear_industry.control;

/** 一次服务端红石 SCRAM 状态转换的结果，包含请求、激活状态和残余裂变热。 */
public record ControlRodScramResult(
        ControlRodScramStatus status,
        boolean scramRequested,
        boolean scramActive,
        double fissionHeatHu,
        String reason
) {
    /** 判断控制棒已进入 SCRAM 流程但仍有可观的裂变热。 */
    public boolean incomplete() {
        return status == ControlRodScramStatus.SCRAM_INCOMPLETE;
    }
}
