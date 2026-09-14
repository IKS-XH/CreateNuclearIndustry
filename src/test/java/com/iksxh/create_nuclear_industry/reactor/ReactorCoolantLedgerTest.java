package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证冷却账本的守恒、热端背压、容量边界和非法输入拒绝。 */
class ReactorCoolantLedgerTest {
    private static final double EPSILON = 1.0E-12D;

    @Test
    void coldInputIsTheLimitingConstraintAndInventoryChangesAreConservative() {
        ReactorCoolantLedger.Settlement result = ReactorCoolantLedger.settle(
                new ReactorCoolantLedger.Inventory(0.0D, 0.0D),
                new ReactorCoolantLedger.Input(20.0D, 3.0D, 10.0D, 0.0D, 100.0D, 2.0D)
        );

        assertEquals(3.0D, result.convertedCoolantMb(), EPSILON);
        assertEquals(0.0D, result.hotOutActualMb(), EPSILON);
        assertEquals(6.0D, result.removedHeatHu(), EPSILON);
        assertEquals(14.0D, result.remainingHeatHu(), EPSILON);
        assertEquals(0.0D, result.nextInventory().coldCoolantMb(), EPSILON);
        assertEquals(3.0D, result.nextInventory().hotCoolantMb(), EPSILON);
    }

    @Test
    void fullSharedCapacityStillAllowsColdToHotStateConversion() {
        ReactorCoolantLedger.Settlement result = ReactorCoolantLedger.settle(
                new ReactorCoolantLedger.Inventory(10.0D, 0.0D),
                new ReactorCoolantLedger.Input(100.0D, 0.0D, 0.0D, 0.0D, 10.0D, 0.5D)
        );

        assertEquals(10.0D, result.convertedCoolantMb(), EPSILON);
        assertEquals(0.0D, result.hotOutActualMb(), EPSILON);
        assertEquals(5.0D, result.removedHeatHu(), EPSILON);
        assertEquals(0.0D, result.nextInventory().coldCoolantMb(), EPSILON);
        assertEquals(10.0D, result.nextInventory().hotCoolantMb(), EPSILON);
    }

    @Test
    void fullMixedSharedCapacityConvertsTheRemainingColdVolume() {
        ReactorCoolantLedger.Settlement result = ReactorCoolantLedger.settle(
                new ReactorCoolantLedger.Inventory(5.0D, 5.0D),
                new ReactorCoolantLedger.Input(100.0D, 0.0D, 0.0D, 0.0D, 10.0D, 0.5D)
        );

        assertEquals(5.0D, result.convertedCoolantMb(), EPSILON);
        assertEquals(2.5D, result.removedHeatHu(), EPSILON);
        assertEquals(0.0D, result.nextInventory().coldCoolantMb(), EPSILON);
        assertEquals(10.0D, result.nextInventory().hotCoolantMb(), EPSILON);
    }

    @Test
    void actualHotOutputIsDeductedAndFluidMassIsConserved() {
        ReactorCoolantLedger.Settlement result = ReactorCoolantLedger.settle(
                new ReactorCoolantLedger.Inventory(0.0D, 0.0D),
                new ReactorCoolantLedger.Input(10.0D, 10.0D, 4.0D, 4.0D, 10.0D, 1.0D)
        );

        assertEquals(10.0D, result.convertedCoolantMb(), EPSILON);
        assertEquals(4.0D, result.hotOutActualMb(), EPSILON);
        assertEquals(10.0D, result.removedHeatHu(), EPSILON);
        assertEquals(0.0D, result.remainingHeatHu(), EPSILON);
        assertEquals(0.0D, result.nextInventory().coldCoolantMb(), EPSILON);
        assertEquals(6.0D, result.nextInventory().hotCoolantMb(), EPSILON);
        assertEquals(10.0D, result.nextInventory().coldCoolantMb()
                + result.nextInventory().hotCoolantMb() + result.hotOutActualMb(), EPSILON);
    }

    @Test
    void existingColdInventoryConvertsWithoutIndependentHotBackpressure() {
        ReactorCoolantLedger.Settlement result = ReactorCoolantLedger.settle(
                new ReactorCoolantLedger.Inventory(5.0D, 0.0D),
                new ReactorCoolantLedger.Input(100.0D, 0.0D, 0.0D, 0.0D, 10.0D, 1.0D)
        );

        assertEquals(5.0D, result.convertedCoolantMb(), EPSILON);
        assertEquals(0.0D, result.hotOutActualMb(), EPSILON);
        assertEquals(5.0D, result.removedHeatHu(), EPSILON);
        assertEquals(0.0D, result.nextInventory().coldCoolantMb(), EPSILON);
        assertEquals(5.0D, result.nextInventory().hotCoolantMb(), EPSILON);
    }

    @Test
    void overCapacityStateIsPreservedAndCanBeReducedByConversionAndOutput() {
        ReactorCoolantLedger.Settlement result = ReactorCoolantLedger.settle(
                new ReactorCoolantLedger.Inventory(5.0D, 7.0D),
                new ReactorCoolantLedger.Input(100.0D, 2.0D, 7.0D, 2.0D, 10.0D, 0.5D)
        );

        assertEquals(0.0D, result.coldInAcceptedMb(), EPSILON);
        assertEquals(5.0D, result.convertedCoolantMb(), EPSILON);
        assertEquals(2.0D, result.hotOutActualMb(), EPSILON);
        assertEquals(0.0D, result.nextInventory().coldCoolantMb(), EPSILON);
        assertEquals(10.0D, result.nextInventory().hotCoolantMb(), EPSILON);
    }

    @Test
    void invalidInputsAreRejectedAtTheLedgerBoundary() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReactorCoolantLedger.Inventory(-1.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () ->
                new ReactorCoolantLedger.Input(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () ->
                new ReactorCoolantLedger.Input(Double.NaN, 0.0D, 0.0D, 0.0D, 0.0D, 0.5D));
        assertThrows(IllegalArgumentException.class, () ->
                ReactorCoolantLedger.settle(null,
                        new ReactorCoolantLedger.Input(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.5D)));
        assertThrows(IllegalArgumentException.class, () ->
                new ReactorCoolantLedger.Input(0.0D, 0.0D, 1.0D, 2.0D, 10.0D, 0.5D));
        assertThrows(IllegalArgumentException.class, () ->
                ReactorCoolantLedger.settle(
                        new ReactorCoolantLedger.Inventory(0.0D, 11.0D),
                        new ReactorCoolantLedger.Input(0.0D, 0.0D, 1.0D, 2.0D, 10.0D, 0.5D)));
    }
}
