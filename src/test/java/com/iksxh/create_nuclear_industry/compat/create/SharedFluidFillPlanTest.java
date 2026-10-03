package com.iksxh.create_nuclear_industry.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** 验证一次分流模拟的共享容量与物理口配额；真实泵管守恒另由 GameTest 证明。 */
final class SharedFluidFillPlanTest {
    @Test
    void threePortsCannotPromiseTheSameNearFullSpaceThreeTimes() {
        var plan = new SharedFluidFillPlan();
        Object owner = new Object();
        int accepted = 0;
        for (int port = 0; port < 3; port++) {
            accepted += plan.limit(new Object(), limits(owner, 36, new Object(), 128), 36);
        }
        assertEquals(36, accepted);
    }

    @Test
    void cumulativeRedistributionReplacesTheHandlersPreviousShare() {
        var plan = new SharedFluidFillPlan();
        Object owner = new Object();
        Object first = new Object();
        Object second = new Object();
        var firstLimits = limits(owner, 100, new Object(), 128);
        var secondLimits = limits(owner, 100, new Object(), 128);

        assertEquals(32, plan.limit(first, firstLimits, 32));
        assertEquals(16, plan.limit(second, secondLimits, 16));
        assertEquals(64, plan.limit(first, firstLimits, 64));
        assertEquals(36, plan.limit(second, secondLimits, 52));
        assertEquals(64, plan.limit(first, firstLimits, 64));
        assertEquals(36, plan.limit(second, secondLimits, 52));
    }

    @Test
    void multipleFacesOfOnePhysicalPortShareItsRemainingBudget() {
        var plan = new SharedFluidFillPlan();
        var limits = limits(new Object(), 1000, new Object(), 128);
        assertEquals(96, plan.limit(new Object(), limits, 96));
        assertEquals(32, plan.limit(new Object(), limits, 96));
    }

    @Test
    void differentPhysicalPortsCanTogetherAcceptMoreThan128() {
        var plan = new SharedFluidFillPlan();
        Object owner = new Object();
        int first = plan.limit(new Object(), limits(owner, 1000, new Object(), 128), 128);
        int second = plan.limit(new Object(), limits(owner, 1000, new Object(), 128), 128);
        assertEquals(256, first + second);
    }

    @Test
    void equalButDistinctOwnersBudgetsAndHandlersRemainIndependent() {
        var plan = new SharedFluidFillPlan();
        var first = limits(new EqualKey(1), 64, new EqualKey(2), 128);
        var second = limits(new EqualKey(1), 64, new EqualKey(2), 128);
        assertEquals(64, plan.limit(new EqualKey(3), first, 64));
        assertEquals(64, plan.limit(new EqualKey(3), second, 64));
    }

    @Test
    void nextNetworkInvocationStartsWithoutReservations() {
        var limits = limits(new Object(), 64, new Object(), 128);
        Object handler = new Object();
        var previous = new SharedFluidFillPlan();
        assertEquals(64, previous.limit(handler, limits, 64));
        assertEquals(0, previous.limit(new Object(), limits, 64));
        assertEquals(64, new SharedFluidFillPlan().limit(handler, limits, 64));
    }

    @Test
    void exchangerFacesShareHotSpaceWithoutIncreasingTheOriginalResult() {
        var plan = new SharedFluidFillPlan();
        Object ledger = new Object();
        var limits = limits(ledger, 64, ledger, 64);
        Object first = new Object();
        assertEquals(20, plan.limit(first, limits, 20));
        assertEquals(44, plan.limit(new Object(), limits, 64));
        assertEquals(20, plan.limit(first, limits, 64));
    }

    private static SharedFluidReceiver.Limits limits(
            Object owner, long space, Object port, int budget) {
        return new SharedFluidReceiver.Limits(owner, space, port, budget);
    }

    /** 故意相等但不同身份，防止普通 HashMap 合并不同机器或接口。 */
    private record EqualKey(int value) {}
}
