package com.iksxh.create_nuclear_industry.reactor;

/**
 * Mutable per-physical-port flow budget for one server tick.
 *
 * <p>Simulation calls only inspect the budget. Execute calls reserve the
 * returned amount, so repeated capability calls cannot bypass the configured
 * per-port limit.</p>
 */
public final class ReactorCoolantPortFlowBudget {
    private long activeServerTick = Long.MIN_VALUE;
    private int usedMb;

    /** Returns the remaining quota without consuming it. */
    public int available(long serverTick, int configuredLimitMbPerTick) {
        validateLimit(configuredLimitMbPerTick);
        resetIfNeeded(serverTick);
        return Math.max(0, configuredLimitMbPerTick - usedMb);
    }

    /**
     * Returns the amount allowed by the current quota. Only an execute call
     * consumes quota; a simulated call is observational.
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

    /** Visible for deterministic unit tests and diagnostics. */
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
