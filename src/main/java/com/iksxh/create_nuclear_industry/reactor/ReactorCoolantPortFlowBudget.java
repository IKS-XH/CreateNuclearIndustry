package com.iksxh.create_nuclear_industry.reactor;

/**
 * 单个物理端口在一个服务端 tick 内可用的可变流量预算。
 *
 * <p>模拟调用只观察预算，EXECUTE 调用才预留已返回量，因此重复 capability 调用
 * 不能绕过配置的单端口 mB/t 上限。</p>
 */
public final class ReactorCoolantPortFlowBudget {
    private long activeServerTick = Long.MIN_VALUE;
    private int usedMb;

    /** 返回当前 tick 剩余配额但不消耗它。 */
    public int available(long serverTick, int configuredLimitMbPerTick) {
        validateLimit(configuredLimitMbPerTick);
        resetIfNeeded(serverTick);
        return Math.max(0, configuredLimitMbPerTick - usedMb);
    }

    /**
     * 返回当前配额允许的量；只有执行调用消耗配额，模拟调用只做观察。
     */
    public int reserve(
            long serverTick,
            int requestedMb,
            int configuredLimitMbPerTick,
            boolean execute
    ) {
        if (requestedMb < 0) {
            throw new IllegalArgumentException("requested flow must be non-negative");
        }
        int accepted = Math.min(requestedMb, available(serverTick, configuredLimitMbPerTick));
        if (execute) {
            usedMb += accepted;
        }
        return accepted;
    }

    /** 为确定性单元测试和诊断暴露已使用量。 */
    public int used(long serverTick, int configuredLimitMbPerTick) {
        validateLimit(configuredLimitMbPerTick);
        resetIfNeeded(serverTick);
        return usedMb;
    }

    private void resetIfNeeded(long serverTick) {
        if (serverTick != activeServerTick) {
            activeServerTick = serverTick;
            usedMb = 0;
        }
    }

    private static void validateLimit(int configuredLimitMbPerTick) {
        if (configuredLimitMbPerTick < 0) {
            throw new IllegalArgumentException("configured flow limit must be non-negative");
        }
    }
}
