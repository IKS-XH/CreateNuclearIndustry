package com.iksxh.create_nuclear_industry.industrial;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** 耗尽首节点的路径应直接拒绝；仍保持重复节点取最小值及模拟跨tick不清账。 */
class IndustrialExhaustedBudgetTest {
    @Test void exhaustedHeadRejectsWithoutWalkingTheRemainingPath() {
        var budget = new IndustrialFlowBudget();var head = new IndustrialFlowBudget.Limit(1, 10);
        assertEquals(10, budget.reserve(1, List.of(head), 10, true));
        int[] tailReads = {0};
        var path = new AbstractList<IndustrialFlowBudget.Limit>() {
            @Override public int size() { return 80; }
            @Override public IndustrialFlowBudget.Limit get(int index) {
                if (index == 0) return head;
                tailReads[0]++;return new IndustrialFlowBudget.Limit(index + 1, 100);
            }
        };
        assertEquals(0, budget.reserve(1, path, 1, false));assertEquals(0, tailReads[0]);
        assertEquals(0, budget.reserve(1, path, 1, true));assertEquals(0, tailReads[0]);
    }
    @Test void nextTickSimulationDoesNotEraseTheOldTickCharge() {
        var budget = new IndustrialFlowBudget();var path = List.of(new IndustrialFlowBudget.Limit(1, 10));
        assertEquals(10, budget.reserve(1, path, 10, true));assertEquals(10, budget.reserve(2, path, 10, false));
        assertEquals(0, budget.reserve(1, path, 1, false));assertEquals(10, budget.reserve(2, path, 10, true));
    }
    @Test void repeatedHeadStillUsesTheSmallestCapacityAndChargesOnlyOnce() {
        var budget = new IndustrialFlowBudget();
        var path = List.of(new IndustrialFlowBudget.Limit(1, 20), new IndustrialFlowBudget.Limit(1, 10));
        assertEquals(7, budget.reserve(1, path, 7, true));assertEquals(3, budget.reserve(1, path, 20, false));
        assertEquals(3, budget.reserve(1, path, 20, true));assertEquals(0, budget.reserve(1, path, 1, true));
    }
}
