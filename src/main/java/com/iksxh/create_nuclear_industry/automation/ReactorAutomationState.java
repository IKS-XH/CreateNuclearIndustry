package com.iksxh.create_nuclear_industry.automation;

/** 仪表唯一持有的控制状态；不包含燃料、冷却库存或客户端显示授权。 */
public record ReactorAutomationState(boolean claimed, Phase phase, String reason, int commandedDepthPercent,
        long authorityEpoch, int adjustmentAge, int healthyAge, int leaseAge, int armingAge,
        Mode requestedMode, boolean runPermission) {
    public ReactorAutomationState {
        if (phase==null || requestedMode==null || reason==null || commandedDepthPercent<0 || commandedDepthPercent>100
                || authorityEpoch<0 || adjustmentAge<0 || healthyAge<0 || leaseAge<0 || armingAge<0)
            throw new IllegalArgumentException("管理员状态非法");
    }
    public enum Mode { STOP, MANUAL, CONSTANT, LOAD_FOLLOW }
    public enum Phase { STOPPED, ARMING, MANUAL, CONSTANT, FAULT, LOAD_WARMUP, LOAD_FOLLOW }
    public static ReactorAutomationState initial() {
        return new ReactorAutomationState(false, Phase.STOPPED, "stopped",100,0,0,0,0,0,Mode.STOP,false);
    }
    /** 恒定功率及安全状态锁定逐棒；手动运行允许玩家精修。 */
    public boolean rodsLocked() { return claimed && phase != Phase.MANUAL; }
    public ReactorAutomationState stop(String why, boolean fault) {
        return new ReactorAutomationState(claimed, fault ? Phase.FAULT : Phase.STOPPED,why,100,
                authorityEpoch+1,0,0,0,0,Mode.STOP,false);
    }
}
