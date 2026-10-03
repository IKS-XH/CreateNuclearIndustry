package com.iksxh.create_nuclear_industry.compat.create;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 单次 Create 管网调用内的临时接收计划，不持有或修改世界库存。
 * 服务端同步模拟使用，单位为 mB；一次调用结束后丢弃，不得按世界 tick 缓存复用。
 */
public final class SharedFluidFillPlan {
    private final Map<Object, Long> inventoryTotals = new IdentityHashMap<>();
    private final Map<Object, Long> flowTotals = new IdentityHashMap<>();
    private final Map<Object, Integer> handlerTotals = new IdentityHashMap<>();

    /**
     * 收窄原 handler 的累计模拟接受量，返回值仍是该 handler 的累计量，单位为 mB。
     * Create 会在重新分配时把此前已接受量加入请求；故须先排除当前 handler 的旧份额，
     * 不能将每次结果直接相加。不同物理口共享库存上限，但各自保留独立配额。
     * 调用方保证本计划期间同一 handler 的库存身份和配额身份不变。
     */
    public int limit(Object handler, SharedFluidReceiver.Limits limits, int simulatedAcceptedMb) {
        Objects.requireNonNull(handler, "流体处理器身份不能为空");
        Objects.requireNonNull(limits, "共享接收限制不能为空");
        if (simulatedAcceptedMb < 0) {
            throw new IllegalArgumentException("模拟接受量不能为负数");
        }
        int previous = handlerTotals.getOrDefault(handler, 0);
        long otherInventory = inventoryTotals.getOrDefault(limits.inventoryIdentity(), 0L) - previous;
        long otherFlow = flowTotals.getOrDefault(limits.flowIdentity(), 0L) - previous;
        long inventoryAvailable = Math.max(0L, limits.inventorySpaceMb() - otherInventory);
        long flowAvailable = Math.max(0L, limits.flowSpaceMb() - otherFlow);
        int accepted = (int) Math.min(simulatedAcceptedMb, Math.min(inventoryAvailable, flowAvailable));

        inventoryTotals.put(limits.inventoryIdentity(), otherInventory + accepted);
        flowTotals.put(limits.flowIdentity(), otherFlow + accepted);
        handlerTotals.put(handler, accepted);
        return accepted;
    }
}
