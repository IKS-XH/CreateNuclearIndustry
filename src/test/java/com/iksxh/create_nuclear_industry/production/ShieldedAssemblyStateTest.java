package com.iksxh.create_nuclear_industry.production;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

/** 纯账本验证批次守恒、模拟和完整栈保存；实际注册及动力由独立GameTest覆盖。 */
final class ShieldedAssemblyStateTest {
    private static final net.minecraft.world.item.Item[] MATERIALS = {
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT, Items.STICK};
    private static ItemStack result() { return new ItemStack(Items.DIAMOND_SWORD); }
    private static void fill(ShieldedAssemblyState state, int batches) {
        for (int slot = 0; slot < 4; slot++)
            assertTrue(state.insert(slot, new ItemStack(MATERIALS[slot],
                    ShieldedAssemblyState.COST[slot] * batches), false).isEmpty());
    }

    @Test void sealingUsesDataCountsAndCannotCarryWorkAcrossOperations() {
        ShieldedAssemblyState state = new ShieldedAssemblyState();
        state.configure("sealing", "test:seal", new int[]{1, 2, 3}, 12800);
        assertTrue(state.insert(0, new ItemStack(Items.IRON_INGOT), true, "sealing").isEmpty());
        assertEquals("", state.operation());
        state.insert(0, new ItemStack(Items.IRON_INGOT), false, "sealing");
        assertEquals("sealing", state.operation());
        assertEquals(1, state.insert(1, new ItemStack(Items.GOLD_INGOT), false, "manufacture").getCount());
        state.insert(1, new ItemStack(Items.GOLD_INGOT, 2), false, "sealing");
        state.insert(2, new ItemStack(Items.COPPER_INGOT, 3), false, "sealing");
        for (int tick = 0; tick < 200; tick++) state.advance(64, result());
        assertEquals(1, state.output().getCount());
        assertEquals("sealing", state.operation());
        state.extractOutput(1, true);
        assertEquals("sealing", state.operation());
        state.extractOutput(1, false);
        assertEquals("", state.operation());
    }

    @Test void dataChangesAndIncompleteWithdrawalDiscardOnlyProgress() {
        ShieldedAssemblyState state = new ShieldedAssemblyState();
        state.configure("sealing", "test:seal", new int[]{1, 2, 3}, 12800);
        state.insert(0, new ItemStack(Items.IRON_INGOT), false, "sealing");
        state.insert(1, new ItemStack(Items.GOLD_INGOT, 2), false, "sealing");
        state.insert(2, new ItemStack(Items.COPPER_INGOT, 3), false, "sealing");
        state.advance(64, result());
        assertEquals(64, state.progress());
        state.configure("sealing", "test:seal", new int[]{1, 2, 3}, 6400);
        assertEquals(0, state.progress());
        state.advance(64, result());
        state.extractInput(2, 1, true);
        assertEquals(64, state.progress());
        state.extractInput(2, 1, false);
        assertEquals(0, state.progress());
        assertEquals(2, state.input(2).getCount());
    }

    @Test void exactBatchAndSpeedBoundaries() {
        ShieldedAssemblyState state = new ShieldedAssemblyState();
        fill(state, 1);
        for (int tick = 0; tick < 400; tick++) assertTrue(state.advance(64, result()));
        assertTrue(state.output().is(Items.DIAMOND_SWORD));
        assertEquals(0, state.output().getDamageValue());
        assertEquals(0, state.progress());
        for (int slot = 0; slot < 4; slot++) assertTrue(state.input(slot).isEmpty());
        assertFalse(state.advance(256, result()));

        ShieldedAssemblyState fast = new ShieldedAssemblyState();
        fill(fast, 1);
        assertFalse(fast.advance(31, result()));
        assertEquals(0, fast.progress());
        for (int tick = 0; tick < 99; tick++) fast.advance(-512, result());
        assertEquals(25_344, fast.progress());
        fast.advance(-512, result());
        assertEquals(1, fast.output().getCount());
    }

    @Test void simulationPauseAndWithdrawalDoNotDuplicate() {
        ShieldedAssemblyState state = new ShieldedAssemblyState();
        assertEquals(6, state.insert(0, new ItemStack(Items.IRON_INGOT, 70), true).getCount());
        assertTrue(state.input(0).isEmpty());
        fill(state, 2);
        for (int tick = 0; tick < 100; tick++) state.advance(64, result());
        int progress = state.progress();
        assertFalse(state.advance(0, result()));
        assertEquals(progress, state.progress());
        assertEquals(8, state.extractInput(0, 8, true).getCount());
        assertEquals(16, state.input(0).getCount());
        state.extractInput(0, 9, false);
        assertEquals(0, state.progress());
        assertTrue(state.insert(0, new ItemStack(Items.GOLD_INGOT), false).is(Items.GOLD_INGOT));
        assertEquals(7, state.input(0).getCount());
    }

    @Test void fullStackSaveAndReloadPreservesComponentsAndProgress() {
        ShieldedAssemblyState state = new ShieldedAssemblyState();
        ItemStack named = new ItemStack(Items.IRON_INGOT, 8);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("保留组件"));
        state.insert(0, named, false);
        for (int slot = 1; slot < 4; slot++)
            state.insert(slot, new ItemStack(MATERIALS[slot], ShieldedAssemblyState.COST[slot]), false);
        state.advance(64, result());
        CompoundTag saved = state.save(RegistryAccess.EMPTY);
        ShieldedAssemblyState restored = new ShieldedAssemblyState();
        restored.load(saved, RegistryAccess.EMPTY);
        assertEquals(64, restored.progress());
        assertEquals(8, restored.input(0).getCount());
        assertEquals(Component.literal("保留组件"), restored.input(0).get(DataComponents.CUSTOM_NAME));
        assertEquals(1, restored.extractInput(0, 1, true).getCount());
        assertEquals(8, restored.input(0).getCount());
    }
}
