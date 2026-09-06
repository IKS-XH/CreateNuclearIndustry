package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.content.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证燃料列钢板维修的消耗、完整度封顶、停热边界和融毁状态保留。 */
class FuelColumnRepairTransactionTest {
    private static final double EPSILON = 1.0E-12D;
    private static final int MAX_DAMAGE = ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY;
    private static final CoreColumnPosition COLUMN = new CoreColumnPosition(0, 0);

    /** 每次维修只消耗一块钢板，并按固定三格有效高度恢复完整度。 */
    @Test
    void repairConsumesOneSteelPlateAndRestoresFixedFraction() {
        FuelColumnState current = fuel(0.5D, 4.0D, 0.4D, 1.0D);
        ItemStack input = new ItemStack(ModItems.STEEL_PLATE.get(), 3);

        FuelColumnRepairTransaction.Result result = FuelColumnRepairTransaction.repair(
                current, input, 0.0D);

        assertEquals(FuelColumnRepairTransaction.Status.REPAIRED, result.status());
        assertTrue(result.success());
        assertEquals(3, input.getCount(), "事务不得预先修改调用方输入栈");
        assertEquals(2, result.remainingInput().getCount());
        assertEquals(0.5D + 0.25D / ReactorSnapshot.INTERNAL_HEIGHT,
                result.nextColumn().integrity(), EPSILON);
        assertEquals(current.fuelAssembly(), result.nextColumn().fuelAssembly());
        assertEquals(current.cachedHeatHu(), result.nextColumn().cachedHeatHu(), EPSILON);
        assertEquals(current.fuelBurnRemainder(), result.nextColumn().fuelBurnRemainder(), EPSILON);
        assertEquals(current.quantizedHeatRemainderHu(),
                result.nextColumn().quantizedHeatRemainderHu(), EPSILON);
    }

    /** 最后一次维修只能封顶到 1.0，不能溢出或多消耗钢板。 */
    @Test
    void repairCapsIntegrityAtOne() {
        ItemStack input = new ItemStack(ModItems.STEEL_PLATE.get(), 2);
        FuelColumnRepairTransaction.Result result = FuelColumnRepairTransaction.repair(
                fuel(0.95D, 0.0D, 0.0D, 0.0D), input, 0.0D);

        assertEquals(FuelColumnRepairTransaction.Status.REPAIRED, result.status());
        assertEquals(1.0D, result.nextColumn().integrity(), EPSILON);
        assertEquals(1, result.remainingInput().getCount());

        FuelColumnRepairTransaction.Result full = FuelColumnRepairTransaction.repair(
                result.nextColumn(), result.remainingInput(), 0.0D);
        assertEquals(FuelColumnRepairTransaction.Status.COLUMN_FULL, full.status());
        assertEquals(1, full.remainingInput().getCount());
        assertEquals(result.nextColumn(), full.nextColumn());
    }

    /** 当前列仍有新生裂变热时拒绝维修，输入和列状态都保持不变。 */
    @Test
    void activeColumnRejectsRepairWithoutConsumingPlate() {
        FuelColumnState current = fuel(0.5D, 0.0D, 0.0D, 0.0D);
        ItemStack input = new ItemStack(ModItems.STEEL_PLATE.get(), 2);

        FuelColumnRepairTransaction.Result result = FuelColumnRepairTransaction.repair(
                current, input, 0.001D);

        assertEquals(FuelColumnRepairTransaction.Status.COLUMN_ACTIVE, result.status());
        assertEquals(current, result.nextColumn());
        assertEquals(2, result.remainingInput().getCount());
    }

    /** 缓存余热不等于仍在裂变放热；允许维修但必须保留余热账本。 */
    @Test
    void residualHeatAllowsRepairAndRemainsUntouched() {
        FuelColumnState current = fuel(0.5D, 8.0D, 0.4D, 2.0D);
        FuelColumnRepairTransaction.Result result = FuelColumnRepairTransaction.repair(
                current,
                new ItemStack(ModItems.STEEL_PLATE.get()),
                0.0D);

        assertTrue(result.success());
        assertEquals(8.0D, result.nextColumn().cachedHeatHu(), EPSILON);
        assertEquals(2.0D, result.nextColumn().quantizedHeatRemainderHu(), EPSILON);
        assertEquals(0.4D, result.nextColumn().fuelBurnRemainder(), EPSILON);
    }

    /** 错误物品和空输入不得改变列或输入；空燃料列维修不会补入组件。 */
    @Test
    void invalidInputsAreRejectedWithoutMutation() {
        FuelColumnState current = fuel(0.5D, 0.0D, 0.0D, 0.0D);

        FuelColumnRepairTransaction.Result wrongItem = FuelColumnRepairTransaction.repair(
                current, new ItemStack(Items.IRON_INGOT), 0.0D);
        FuelColumnRepairTransaction.Result emptyInput = FuelColumnRepairTransaction.repair(
                current, ItemStack.EMPTY, 0.0D);
        FuelColumnRepairTransaction.Result emptyColumn = FuelColumnRepairTransaction.repair(
                new FuelColumnState(FuelAssemblyState.empty(), 0.5D, 0.0D),
                new ItemStack(ModItems.STEEL_PLATE.get()), 0.0D);

        assertEquals(FuelColumnRepairTransaction.Status.WRONG_REPAIR_ITEM, wrongItem.status());
        assertEquals(FuelColumnRepairTransaction.Status.EMPTY_INPUT, emptyInput.status());
        assertEquals(FuelColumnRepairTransaction.Status.REPAIRED, emptyColumn.status());
        assertEquals(current, wrongItem.nextColumn());
        assertEquals(current, emptyInput.nextColumn());
        assertEquals(1, wrongItem.remainingInput().getCount());
        assertTrue(emptyInput.remainingInput().isEmpty());
        assertEquals(0.5D + 0.25D / ReactorSnapshot.INTERNAL_HEIGHT,
                emptyColumn.nextColumn().integrity(), EPSILON);
        assertFalse(emptyColumn.nextColumn().fuelAssembly().present(),
                "空燃料列维修不得补入燃料组件");
    }

    /** 维修只改变目标列完整度，不得回退融毁进度或清除启动标志。 */
    @Test
    void repairDoesNotRollbackMeltdownProgress() {
        FuelColumnState current = fuel(0.5D, 3.0D, 0.2D, 1.0D);
        ReactorSnapshot before = new ReactorSnapshot(
                Map.of(COLUMN, current), Map.of(), 17L, 23L, 41L, true);
        FuelColumnRepairTransaction.Result result = FuelColumnRepairTransaction.repair(
                current, new ItemStack(ModItems.STEEL_PLATE.get()), 0.0D);

        ReactorSnapshot after = before.withFuelColumn(COLUMN, result.nextColumn());

        assertFalse(result.nextColumn().integrity() == current.integrity());
        assertEquals(41L, after.meltdownProgressTicks());
        assertTrue(after.meltdownCountdownStarted());
        assertEquals(17L, after.coldCoolantMb());
        assertEquals(23L, after.hotCoolantMb());
    }

    private static FuelColumnState fuel(
            double integrity,
            double cachedHeatHu,
            double fuelBurnRemainder,
            double quantizedHeatRemainderHu
    ) {
        return new FuelColumnState(
                FuelAssemblyState.installed(MAX_DAMAGE, 12_345),
                integrity,
                cachedHeatHu,
                fuelBurnRemainder,
                quantizedHeatRemainderHu
        );
    }
}
