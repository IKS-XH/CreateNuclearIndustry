package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证单物理端口 mB/t 预算在模拟、执行和服务端 tick 边界上的行为。 */
class ReactorCoolantPortFlowBudgetTest {
    @Test
    void repeatedExecuteCallsShareOnePhysicalPortQuota() {
        ReactorCoolantPortFlowBudget budget = new ReactorCoolantPortFlowBudget();

        assertEquals(80, budget.reserve(10L, 80, 128, true));
        assertEquals(48, budget.reserve(10L, 80, 128, true));
        assertEquals(0, budget.reserve(10L, 1, 128, true));
        assertEquals(128, budget.used(10L, 128));
    }

    @Test
    void simulateDoesNotConsumeTheCurrentTickQuota() {
        ReactorCoolantPortFlowBudget budget = new ReactorCoolantPortFlowBudget();

        assertEquals(128, budget.reserve(20L, 256, 128, false));
        assertEquals(0, budget.used(20L, 128));
        assertEquals(128, budget.reserve(20L, 256, 128, true));
        assertEquals(0, budget.reserve(20L, 1, 128, true));
    }

    @Test
    void quotaResetsOnNextServerTick() {
        ReactorCoolantPortFlowBudget budget = new ReactorCoolantPortFlowBudget();

        assertEquals(128, budget.reserve(30L, 128, 128, true));
        assertEquals(128, budget.reserve(31L, 128, 128, true));
    }

    @Test
    void aServerConfigOverrideChangesTheLimitWithoutChangingTheBudgetContract() {
        ReactorCoolantPortFlowBudget budget = new ReactorCoolantPortFlowBudget();

        assertEquals(64, budget.reserve(40L, 128, 64, true));
        assertEquals(0, budget.reserve(40L, 128, 32, true));
        assertEquals(32, budget.reserve(41L, 128, 32, true));
        assertEquals(0, budget.reserve(41L, 1, 32, true));
    }

    @Test
    void invalidBudgetInputsAreRejected() {
        ReactorCoolantPortFlowBudget budget = new ReactorCoolantPortFlowBudget();

        assertThrows(IllegalArgumentException.class, () -> budget.available(0L, -1));
        assertThrows(IllegalArgumentException.class, () -> budget.reserve(0L, -1, 128, true));
    }
}
