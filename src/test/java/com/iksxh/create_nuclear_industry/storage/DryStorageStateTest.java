package com.iksxh.create_nuclear_industry.storage;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

/** 独立账本验证模拟、减容、完整保存及重复拆除；正式身份和能力由GameTest验证。 */
final class DryStorageStateTest {
    private DryStorageState rack() { return new DryStorageState(s -> s.is(Items.DIAMOND_SWORD) && s.getCount() == 1); }
    private ItemStack cask(int i) {
        ItemStack item = new ItemStack(Items.DIAMOND_SWORD);
        item.set(DataComponents.CUSTOM_NAME, Component.literal("桶" + i));
        return item;
    }
    @Test void allSixteenSlotsPreservePayloadAfterCapacityReduction() {
        DryStorageState rack = rack();
        for (int slot = 0; slot < 16; slot++) assertTrue(rack.insert(slot, cask(slot), 16, false).isEmpty());
        assertEquals(16, rack.used());
        assertEquals(4, rack.storageLevel(4));
        DryStorageState restored = rack();
        restored.load(rack.save(RegistryAccess.EMPTY), RegistryAccess.EMPTY);
        assertEquals(16, restored.used());
        for (int slot = 15; slot >= 0; slot--) {
            assertEquals(slot, restored.lastOccupied());
            assertEquals(Component.literal("桶" + slot), restored.extract(slot, 1, false).get(DataComponents.CUSTOM_NAME));
        }
        assertEquals(0, restored.storageLevel(4));
    }
    @Test void reducedCapacityRestrictsTotalAndSlotRange() {
        DryStorageState rack = rack();
        rack.insert(15, cask(15), 16, false);
        assertFalse(rack.insert(15, cask(0), 1, false).isEmpty());
        assertFalse(rack.insert(0, cask(0), 1, false).isEmpty());
        rack.extract(15, 1, false);
        assertFalse(rack.insert(1, cask(1), 1, false).isEmpty());
        assertTrue(rack.insert(0, cask(0), 1, false).isEmpty());
        assertEquals(1, rack.used());
    }
    @Test void simulationInvalidInputsAndRepeatedDrainConserveItems() {
        DryStorageState rack = rack();
        assertTrue(rack.insert(0, cask(0), 16, true).isEmpty());
        assertEquals(0, rack.used());
        assertFalse(rack.insert(-1, cask(0), 16, false).isEmpty());
        assertFalse(rack.insert(0, new ItemStack(Items.IRON_INGOT), 16, false).isEmpty());
        rack.insert(0, cask(0), 16, false);
        assertEquals(1, rack.extract(0, 1, true).getCount());
        assertEquals(1, rack.used());
        assertEquals(1, rack.drain().size());
        assertEquals(0, rack.drain().size());
        assertEquals(-1, rack.lastOccupied());
    }
}
