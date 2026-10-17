package com.iksxh.create_nuclear_industry.radiation;

/** 单份栈的只读摘要：容器屏蔽前/后D/s，真实载荷标志与预算完成状态。 */
public record ItemSourceSummary(double unshieldedDosePerSecond, double dosePerSecond,
        boolean radioactivePayload, boolean complete, int visitedStacks) {}
