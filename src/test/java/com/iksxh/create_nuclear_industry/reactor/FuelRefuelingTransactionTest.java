package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.content.ModItems;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证燃料物品耐久、单列取放事务、耗尽产物和多列隔离。 */
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

    private static ItemStack freshFuel(int damage, int count) {
        ItemStack stack = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get(), count);
        stack.setDamageValue(damage);
        return stack;
    }
}
