package com.iksxh.create_nuclear_industry.industrial;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 服务端单世界逐 tick 额度账本；节点身份共用额度，模拟绝不修改账本，单位 mB/t。 */
public final class IndustrialFlowBudget {
    private long tick = Long.MIN_VALUE;
    private final Map<Long, Integer> used = new HashMap<>();

    /** 同一世界节点的本次有效上限；同路重复节点取最小值，只收费一次。 */
    public record Limit(long node, int capacity) {}

    /** 转速只影响单泵输送额，正反转共用额定值；非法配置或非有限转速拒绝运输。 */
    public static int pumpLimit(int rated, double ratedRpm, double rpm) {
        if (rated <= 0 || !Double.isFinite(ratedRpm) || ratedRpm <= 0 || !Double.isFinite(rpm)) return 0;
        return (int) Math.floor(rated * Math.min(1, Math.abs(rpm) / ratedRpm));
    }

    /** 校验整条路径后按实际执行量一次收费；模拟跨 tick 也不清除旧账本。 */
    public int reserve(long now, List<Limit> path, int requested, boolean execute) {
        if (requested <= 0 || path.isEmpty()) return 0;
        Map<Long, Integer> capacities = new HashMap<>();
        for (Limit limit : path) {
            if (limit.capacity() <= 0) return 0;
            capacities.merge(limit.node(), limit.capacity(), Math::min);
        }
        int amount = requested;
        for (var entry : capacities.entrySet()) {
            int previous = now == tick ? used.getOrDefault(entry.getKey(), 0) : 0;
            amount = Math.min(amount, Math.max(0, entry.getValue() - previous));
        }
        if (execute && amount > 0) {
            if (now != tick) { used.clear(); tick = now; }
            for (long node : capacities.keySet()) used.merge(node, amount, Math::addExact);
        }
        return amount;
    }
}
