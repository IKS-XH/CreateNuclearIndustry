package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.content.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证燃料物品耐久、单列取放与快捷替换事务、耗尽产物和多列隔离。 */
class FuelRefuelingTransactionTest {
    private static final int MAX_DAMAGE = ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY;
    private static final CoreColumnPosition FIRST = new CoreColumnPosition(0, 0);
    private static final CoreColumnPosition SECOND = new CoreColumnPosition(0, 1);

    @Test
    void emptyColumnLoadsOneFreshAssemblyAndConsumesOnlyOneStackEntry() {
        FuelColumnState empty = FuelColumnState.empty();
        ItemStack input = freshFuel(12_345, 2);

        FuelRefuelingTransaction.Result result = FuelRefuelingTransaction.insert(empty, input, 0.0D);

        assertEquals(FuelRefuelingTransaction.Status.INSERTED, result.status());
        assertTrue(result.success());
        assertEquals(2, input.getCount(), "事务不得预先修改调用方输入栈");
        assertEquals(1, result.remainingInput().getCount());
        assertTrue(result.output().isEmpty());
        assertTrue(result.nextColumn().fuelAssembly().present());
        assertEquals(12_345, result.nextColumn().fuelAssembly().damage());
        assertEquals(MAX_DAMAGE, result.nextColumn().fuelAssembly().maxDamage());
    }

    @Test
    void runningColumnRejectsBothInsertionAndExtractionWithoutMutation() {
        FuelColumnState loaded = new FuelColumnState(
                FuelAssemblyState.installed(MAX_DAMAGE, 321), 0.72D, 6.0D);
        ItemStack input = freshFuel(99, 1);

        FuelRefuelingTransaction.Result insertion = FuelRefuelingTransaction.insert(loaded, input, 1.0D);
        FuelRefuelingTransaction.Result extraction = FuelRefuelingTransaction.extract(loaded, 1.0D);

        assertEquals(FuelRefuelingTransaction.Status.COLUMN_ACTIVE, insertion.status());
        assertEquals(FuelRefuelingTransaction.Status.COLUMN_ACTIVE, extraction.status());
        assertEquals(1, insertion.remainingInput().getCount());
        assertEquals(loaded, insertion.nextColumn());
        assertEquals(loaded, extraction.nextColumn());
        assertTrue(extraction.output().isEmpty());
    }

    @Test
    void extractedFreshAssemblyKeepsItemStackDurabilityAndColumnHeatState() {
        FuelColumnState loaded = new FuelColumnState(
                FuelAssemblyState.installed(MAX_DAMAGE, 54_321), 0.63D, 4.5D);

        FuelRefuelingTransaction.Result result = FuelRefuelingTransaction.extract(loaded, 0.0D);

        assertEquals(FuelRefuelingTransaction.Status.REMOVED, result.status());
        assertTrue(FuelAssemblyItemCodec.isFreshFuel(result.output()));
        assertEquals(54_321, result.output().getDamageValue());
        assertFalse(result.nextColumn().fuelAssembly().present());
        assertEquals(0.63D, result.nextColumn().integrity(), 1.0E-12D);
        assertEquals(4.5D, result.nextColumn().cachedHeatHu(), 1.0E-12D);
        assertEquals(0.0D, result.nextColumn().fuelBurnRemainder(), 1.0E-12D);
    }

    @Test
    void exactExtractionPreservesAllItemStackComponents() {
        FuelColumnState loaded = new FuelColumnState(
                FuelAssemblyState.installed(MAX_DAMAGE, 54_321), 1.0D, 0.0D);
        ItemStack stored = freshFuel(54_321, 1);
        stored.set(DataComponents.CUSTOM_NAME, Component.literal("带标记燃料"));

        FuelRefuelingTransaction.Result result = FuelRefuelingTransaction.extract(
                loaded, 0.0D, stored);

        assertEquals(FuelRefuelingTransaction.Status.REMOVED, result.status());
        assertTrue(ItemStack.matches(stored, result.output()),
                "精确取料必须保留名称、耐久和其它 ItemStack 数据组件");
    }

    @Test
    void exhaustionIsConvertedDirectlyToCooledSpentFuelOnExtraction() {
        FuelColumnState nearlyExhausted = new FuelColumnState(
                FuelAssemblyState.installed(MAX_DAMAGE, MAX_DAMAGE - 1), 1.0D, 0.0D);
        FuelColumnState exhausted = nearlyExhausted.burnFraction(1.0D / MAX_DAMAGE);

        FuelRefuelingTransaction.Result result = FuelRefuelingTransaction.extract(exhausted, 0.0D);

        assertTrue(exhausted.fuelAssembly().exhausted());
        assertEquals(FuelRefuelingTransaction.Status.REMOVED, result.status());
        assertTrue(FuelAssemblyItemCodec.isCooledSpentFuel(result.output()));
        assertFalse(FuelAssemblyItemCodec.isFreshFuel(result.output()));
        assertFalse(result.nextColumn().fuelAssembly().present());
    }

    @Test
    void wrongAndExhaustedInputsAreRejectedWithoutChangingAnEmptyColumn() {
        FuelColumnState empty = FuelColumnState.empty();
        ItemStack spent = new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get());
        ItemStack exhaustedFresh = freshFuel(MAX_DAMAGE, 1);

        FuelRefuelingTransaction.Result wrong = FuelRefuelingTransaction.insert(empty, spent, 0.0D);
        FuelRefuelingTransaction.Result exhausted = FuelRefuelingTransaction.insert(empty, exhaustedFresh, 0.0D);

        assertEquals(FuelRefuelingTransaction.Status.WRONG_FUEL_ITEM, wrong.status());
        assertEquals(FuelRefuelingTransaction.Status.EXHAUSTED_FUEL_INPUT, exhausted.status());
        assertEquals(empty, wrong.nextColumn());
        assertEquals(empty, exhausted.nextColumn());
        assertEquals(1, wrong.remainingInput().getCount());
        assertEquals(1, exhausted.remainingInput().getCount());
    }

    @Test
    void twoFuelColumnsKeepIndependentAssemblyTransactions() {
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(FIRST, FuelColumnState.empty(), SECOND, FuelColumnState.empty()),
                Map.of(), 0L, 0L, 0L, false);
        FuelRefuelingTransaction.Result first = FuelRefuelingTransaction.insert(
                snapshot.fuelColumns().get(FIRST), freshFuel(7, 1), 0.0D);
        ReactorSnapshot loaded = snapshot.withFuelColumn(FIRST, first.nextColumn());

        FuelRefuelingTransaction.Result removed = FuelRefuelingTransaction.extract(
                loaded.fuelColumns().get(FIRST), 0.0D);
        ReactorSnapshot after = loaded.withFuelColumn(FIRST, removed.nextColumn());

        assertFalse(after.fuelColumns().get(FIRST).fuelAssembly().present());
        assertFalse(after.fuelColumns().get(SECOND).fuelAssembly().present());
        assertEquals(FuelColumnState.empty(), after.fuelColumns().get(SECOND));
    }

    @Test
    void exhaustedReplacementConsumesOneAndPreservesExactComponentsAndStructuralState() {
        FuelColumnState exhausted = new FuelColumnState(
                FuelAssemblyState.installed(MAX_DAMAGE, MAX_DAMAGE), 0.63D, 9.0D, 0.2D, 0.4D);
        ItemStack stored = FuelAssemblyItemCodec.createCooledSpentFuel();
        stored.set(DataComponents.CUSTOM_NAME, Component.literal("精确旧组件"));
        ItemStack input = freshFuel(123, 3);
        input.set(DataComponents.CUSTOM_NAME, Component.literal("新组件标记"));

        var result = FuelRefuelingTransaction.replaceExhausted(exhausted, input, 0.0D, stored);

        assertEquals(FuelRefuelingTransaction.Status.REPLACED, result.status());
        assertTrue(result.success());
        assertTrue(ItemStack.matches(stored, result.output()));
        assertTrue(ItemStack.matches(input.copyWithCount(2), result.remainingInput()));
        assertEquals(3, input.getCount());
        assertEquals(exhausted.withFuelAssembly(FuelAssemblyItemCodec.readFreshFuel(input)), result.nextColumn());
        assertEquals(0.0D, result.nextColumn().fuelBurnRemainder());
    }

    @Test
    void stoppedButOneDurabilityRemainingCannotBeReplaced() {
        ItemStack stored = freshFuel(MAX_DAMAGE - 1, 1);
        FuelColumnState current = new FuelColumnState(FuelAssemblyItemCodec.readFreshFuel(stored), 0.8D, 3.0D);
        ItemStack incoming = freshFuel(0, 2);

        assertReplacementFailure(FuelRefuelingTransaction.Status.TARGET_NOT_EXHAUSTED,
                current, incoming, 0.0D, stored);
    }

    @Test
    void replacementRejectsWrongExhaustedEmptyAndActiveInputsWithoutMutation() {
        FuelColumnState exhausted = new FuelColumnState(
                FuelAssemblyState.installed(MAX_DAMAGE, MAX_DAMAGE), 0.7D, 8.0D);
        ItemStack stored = FuelAssemblyItemCodec.createCooledSpentFuel();
        assertReplacementFailure(FuelRefuelingTransaction.Status.WRONG_FUEL_ITEM,
                exhausted, stored, 0.0D, stored);
        assertReplacementFailure(FuelRefuelingTransaction.Status.EXHAUSTED_FUEL_INPUT,
                exhausted, freshFuel(MAX_DAMAGE, 2), 0.0D, stored);
        assertReplacementFailure(FuelRefuelingTransaction.Status.EMPTY_INPUT,
                exhausted, ItemStack.EMPTY, 0.0D, stored);
        assertReplacementFailure(FuelRefuelingTransaction.Status.COLUMN_ACTIVE,
                exhausted, freshFuel(0, 2), 1.0D, stored);
        assertReplacementFailure(FuelRefuelingTransaction.Status.COLUMN_EMPTY,
                FuelColumnState.empty(), freshFuel(0, 2), 0.0D, ItemStack.EMPTY);
    }

    @Test
    void exhaustedFreshShapeUsesExistingCooledExtractionRule() {
        ItemStack stored = freshFuel(MAX_DAMAGE, 1);
        FuelColumnState exhausted = new FuelColumnState(FuelAssemblyItemCodec.readFreshFuel(stored), 1.0D, 0.0D);

        var result = FuelRefuelingTransaction.replaceExhausted(exhausted, freshFuel(1, 1), 0.0D, stored);

        assertTrue(result.success());
        assertTrue(ItemStack.matches(FuelRefuelingTransaction.extract(exhausted, 0.0D, stored).output(), result.output()));
        assertTrue(FuelAssemblyItemCodec.isCooledSpentFuel(result.output()));
        assertTrue(result.remainingInput().isEmpty());
    }

    @Test
    void replacementRejectsExactItemAndColumnProjectionMismatch() {
        FuelColumnState exhausted = new FuelColumnState(
                FuelAssemblyState.installed(MAX_DAMAGE, MAX_DAMAGE), 1.0D, 0.0D);
        assertReplacementFailure(FuelRefuelingTransaction.Status.WRONG_FUEL_ITEM,
                exhausted, freshFuel(0, 1), 0.0D, freshFuel(MAX_DAMAGE - 1, 1));
        assertReplacementFailure(FuelRefuelingTransaction.Status.WRONG_FUEL_ITEM,
                exhausted, freshFuel(0, 1), 0.0D, ItemStack.EMPTY);
        assertReplacementFailure(FuelRefuelingTransaction.Status.WRONG_FUEL_ITEM,
                exhausted, freshFuel(0, 1), 0.0D, FuelAssemblyItemCodec.createCooledSpentFuel().copyWithCount(2));
    }

    /** 失败结果必须保留输入、列和精确旧栈，且不得产生可返还输出。 */
    private static void assertReplacementFailure(FuelRefuelingTransaction.Status expected, FuelColumnState current,
                                                 ItemStack incoming, double heatHu, ItemStack stored) {
        ItemStack originalInput = incoming.copy();
        ItemStack originalStored = stored.copy();
        var result = FuelRefuelingTransaction.replaceExhausted(current, incoming, heatHu, stored);
        assertEquals(expected, result.status());
        assertFalse(result.success());
        assertEquals(current, result.nextColumn());
        assertTrue(result.output().isEmpty());
        assertTrue(ItemStack.matches(originalInput, incoming));
        assertTrue(ItemStack.matches(originalInput, result.remainingInput()));
        assertTrue(ItemStack.matches(originalStored, stored));
    }

    private static ItemStack freshFuel(int damage, int count) {
        ItemStack stack = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get(), count);
        stack.setDamageValue(damage);
        return stack;
    }
}
