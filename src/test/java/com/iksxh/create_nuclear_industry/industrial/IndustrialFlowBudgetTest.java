package com.iksxh.create_nuclear_industry.industrial;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证服务端共享额度；模拟、停转和超速都不能制造额外 mB/t。 */
class IndustrialFlowBudgetTest {
    @Test void speedScalesAndCapsOnePump() {
        assertEquals(16384, IndustrialFlowBudget.pumpLimit(16384, 256, 256));
        assertEquals(8192, IndustrialFlowBudget.pumpLimit(16384, 256, 128));
        assertEquals(8192, IndustrialFlowBudget.pumpLimit(16384, 256, -128));
        assertEquals(0, IndustrialFlowBudget.pumpLimit(16384, 256, 0));
        assertEquals(16384, IndustrialFlowBudget.pumpLimit(16384, 256, 1024));
        assertEquals(0, IndustrialFlowBudget.pumpLimit(16384, 256, Double.NaN));
    }
    @Test void sharedEdgeCapsMultiplePumpsAndSimulationIsReadOnly() {
        var budget = new IndustrialFlowBudget();
        var path = List.of(new IndustrialFlowBudget.Limit(12L, 8192), new IndustrialFlowBudget.Limit(13L, 32768));
        assertEquals(8192, budget.reserve(1, path, 16384, false));
        assertEquals(8192, budget.reserve(1, path, 16384, false));
        assertEquals(6000, budget.reserve(1, path, 6000, true));
        assertEquals(2192, budget.reserve(1, path, 10000, true));
        assertEquals(0, budget.reserve(1, path, 1, true));
        assertEquals(8192, budget.reserve(2, path, 16384, true));
    }
    @Test void independentPathsAddButSeriesPumpDoesNot() {
        var budget = new IndustrialFlowBudget();
        assertEquals(10000, budget.reserve(7, List.of(new IndustrialFlowBudget.Limit(1, 16384)), 10000, true));
        assertEquals(10000, budget.reserve(7, List.of(new IndustrialFlowBudget.Limit(2, 16384)), 10000, true));
        assertEquals(6384, budget.reserve(7, List.of(new IndustrialFlowBudget.Limit(1, 16384), new IndustrialFlowBudget.Limit(2, 16384)), 16384, true));
    }
    @Test void duplicateNodesAndInvalidLimitsCannotExpandQuota() {
        var budget = new IndustrialFlowBudget();
        assertEquals(8192, budget.reserve(1, List.of(new IndustrialFlowBudget.Limit(1, 32768), new IndustrialFlowBudget.Limit(1, 8192)), 20000, true));
        assertEquals(0, budget.reserve(1, List.of(new IndustrialFlowBudget.Limit(9, -1)), 1, true));
    }
}
