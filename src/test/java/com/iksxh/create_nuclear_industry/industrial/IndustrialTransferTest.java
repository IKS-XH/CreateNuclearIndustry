package com.iksxh.create_nuclear_industry.industrial;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** 实际短收必须保留在同泵缓存；零写模拟不能消耗源、目标或已有余量。 */
class IndustrialTransferTest {
    private static final class Store implements IndustrialTransfer.Store {
        int amount, capacity, executionLimit = Integer.MAX_VALUE;
        Store(int amount, int capacity) { this.amount = amount; this.capacity = capacity; }
        public int remove(int request, boolean execute) { int n = Math.min(amount, request); if (execute) amount -= n; return n; }
        public int add(int request, boolean execute) {
            int n = Math.min(request, Math.max(0, capacity - amount));
            if (execute) { n = Math.min(n, executionLimit); amount += n; }
            return n;
        }
    }
    @Test void shortReceiptRemainsCachedAndBlocksFurtherExtraction() {
        var source = new Store(50000, 50000); var target = new Store(0, 50000);
        var transfer = new IndustrialTransfer(); target.executionLimit = 2000;
        assertEquals(2000, transfer.move(source, target, 16384, 16384, true));
        assertEquals(33616, source.amount); assertEquals(14384, transfer.cached());
        assertEquals(50000, source.amount + target.amount + transfer.cached());
        assertEquals(2000, transfer.move(source, target, 16384, 16384, true));
        assertEquals(33616, source.amount); assertEquals(12384, transfer.cached());
    }
    @Test void simulateAndBlockedTargetAreZeroWrite() {
        var source = new Store(10000, 10000); var target = new Store(0, 8000); var transfer = new IndustrialTransfer();
        assertEquals(8000, transfer.move(source, target, 16384, 16384, false));
        assertEquals(10000, source.amount); assertEquals(0, target.amount); assertEquals(0, transfer.cached());
        target.capacity = 0; assertEquals(0, transfer.move(source, target, 16384, 16384, true));
        assertEquals(10000, source.amount);
    }
    @Test void shrinkingCapacityRetainsExistingCache() {
        var source = new Store(20000, 20000); var target = new Store(0, 20000); var transfer = new IndustrialTransfer();
        target.executionLimit = 0; transfer.move(source, target, 16384, 16384, true);
        assertEquals(16384, transfer.cached()); target.executionLimit = 1000;
        assertEquals(1000, transfer.move(source, target, 1000, 1000, true));
        assertEquals(15384, transfer.cached()); assertEquals(3616, source.amount);
    }
}
