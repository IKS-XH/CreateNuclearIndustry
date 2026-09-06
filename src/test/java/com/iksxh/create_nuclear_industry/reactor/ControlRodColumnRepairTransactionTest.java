package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.content.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证控制棒列钢板维修的固定恢复量、卡死解除条件和全堆状态保留。 */
class ControlRodColumnRepairTransactionTest {
    private static final double EPSILON = 1.0E-12D;
    private static final double FAILURE_THRESHOLD = 0.20D;
    private static final CoreColumnPosition COLUMN = new CoreColumnPosition(1, 1);

    /** 低于失效阈值的卡死列可以部分维修，但仍保持卡死和原有深度。 */
    @Test
    void partialRepairKeepsJammedRodJammed() {
        ControlRodColumnState current = new ControlRodColumnState(
                0.0D, 0.8D, 0.35D, true, 2.0D);
        ItemStack input = new ItemStack(ModItems.STEEL_PLATE.get(), 3);

        ControlRodColumnRepairTransaction.Result result =
                ControlRodColumnRepairTransaction.repair(current, input, FAILURE_THRESHOLD);

        assertEquals(ControlRodColumnRepairTransaction.Status.REPAIRED, result.status());
        assertTrue(result.success());
        assertFalse(result.jammedCleared());
        assertTrue(result.nextColumn().jammed());
        assertEquals(0.25D / ReactorSnapshot.INTERNAL_HEIGHT,
                result.nextColumn().integrity(), EPSILON);
        assertEquals(0.8D, result.nextColumn().targetDepth(), EPSILON);
        assertEquals(0.35D, result.nextColumn().actualDepth(), EPSILON);
        assertEquals(2.0D, result.nextColumn().cachedHeatHu(), EPSILON);
        assertEquals(3, input.getCount());
        assertEquals(2, result.remainingInput().getCount());
    }

    /** 完整度越过失效阈值后解除卡死，但不重置目标深度或实际深度。 */
    @Test
    void repairPastFailureThresholdUnjamsRod() {
        ControlRodColumnState current = new ControlRodColumnState(
                FAILURE_THRESHOLD, 0.8D, 0.35D, true, 2.0D);

        ControlRodColumnRepairTransaction.Result result =
                ControlRodColumnRepairTransaction.repair(
                        current,
                        new ItemStack(ModItems.STEEL_PLATE.get()),
                        FAILURE_THRESHOLD);

        assertTrue(result.success());
        assertTrue(result.jammedCleared());
        assertFalse(result.nextColumn().jammed());
        assertTrue(result.nextColumn().integrity() > FAILURE_THRESHOLD);
        assertEquals(0.8D, result.nextColumn().targetDepth(), EPSILON);
        assertEquals(0.35D, result.nextColumn().actualDepth(), EPSILON);
        assertEquals(2.0D, result.nextColumn().cachedHeatHu(), EPSILON);
    }

    /** 未卡死的受损列按同一固定量恢复，最后一次维修封顶到 1.0。 */
    @Test
    void nonJammedRepairUsesFixedAmountAndCapsAtOne() {
        ControlRodColumnState current = new ControlRodColumnState(
                0.95D, 0.4D, 0.2D, false, 0.0D);
        ControlRodColumnRepairTransaction.Result result =
                ControlRodColumnRepairTransaction.repair(
                        current,
                        new ItemStack(ModItems.STEEL_PLATE.get(), 2),
                        FAILURE_THRESHOLD);

        assertTrue(result.success());
        assertFalse(result.nextColumn().jammed());
        assertEquals(1.0D, result.nextColumn().integrity(), EPSILON);
        assertEquals(1, result.remainingInput().getCount());
    }

    /** 空输入、错误物品、满列和无效驱动器均不得消耗输入或产生列状态。 */
    @Test
    void invalidInputsAndFullColumnDoNotMutate() {
        ControlRodColumnState current = new ControlRodColumnState(
                0.5D, 0.4D, 0.2D, false, 0.0D);
        ControlRodColumnRepairTransaction.Result empty =
                ControlRodColumnRepairTransaction.repair(current, ItemStack.EMPTY, FAILURE_THRESHOLD);
        ControlRodColumnRepairTransaction.Result wrong =
                ControlRodColumnRepairTransaction.repair(
                        current, new ItemStack(Items.IRON_INGOT), FAILURE_THRESHOLD);
        ControlRodColumnState fullState = new ControlRodColumnState(
                1.0D, 0.4D, 0.2D, false, 0.0D);
        ControlRodColumnRepairTransaction.Result full =
                ControlRodColumnRepairTransaction.repair(
                        fullState,
                        new ItemStack(ModItems.STEEL_PLATE.get()),
                        FAILURE_THRESHOLD);
        ControlRodColumnRepairTransaction.Result invalid =
                ControlRodColumnRepairTransaction.invalidDrive(
                        new ItemStack(ModItems.STEEL_PLATE.get(), 2));

        assertEquals(ControlRodColumnRepairTransaction.Status.EMPTY_INPUT, empty.status());
        assertEquals(ControlRodColumnRepairTransaction.Status.WRONG_REPAIR_ITEM, wrong.status());
        assertEquals(ControlRodColumnRepairTransaction.Status.COLUMN_FULL, full.status());
        assertEquals(ControlRodColumnRepairTransaction.Status.INVALID_DRIVE, invalid.status());
        assertSame(current, empty.nextColumn());
        assertSame(current, wrong.nextColumn());
        assertSame(fullState, full.nextColumn());
        assertNull(invalid.nextColumn());
        assertTrue(empty.remainingInput().isEmpty());
        assertEquals(1, wrong.remainingInput().getCount());
        assertEquals(1, full.remainingInput().getCount());
        assertEquals(2, invalid.remainingInput().getCount());
    }

    /** 维修提交到快照时只替换目标控制棒列，不回退融毁进度和冷却剂账本。 */
    @Test
    void snapshotCommitPreservesOtherColumnsAndMeltdownState() {
        ControlRodColumnState current = new ControlRodColumnState(
                0.0D, 0.8D, 0.35D, true, 3.0D);
        CoreColumnPosition other = new CoreColumnPosition(0, 0);
        ControlRodColumnState otherState = ControlRodColumnState.fullyInserted();
        ReactorSnapshot before = new ReactorSnapshot(
                Map.of(),
                Map.of(COLUMN, current, other, otherState),
                17L,
                23L,
                41L,
                true);
        ControlRodColumnRepairTransaction.Result result =
                ControlRodColumnRepairTransaction.repair(
                        current,
                        new ItemStack(ModItems.STEEL_PLATE.get()),
                        0.0D);
        ReactorSnapshot after = before.withColumns(
                before.fuelColumns(),
                Map.of(COLUMN, result.nextColumn(), other, otherState));

        assertEquals(17L, after.coldCoolantMb());
        assertEquals(23L, after.hotCoolantMb());
        assertEquals(41L, after.meltdownProgressTicks());
        assertTrue(after.meltdownCountdownStarted());
        assertEquals(otherState, after.controlRodColumns().get(other));
        assertEquals(result.nextColumn(), after.controlRodColumns().get(COLUMN));
    }
}
