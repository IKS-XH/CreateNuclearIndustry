package com.iksxh.create_nuclear_industry.production;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 验证真实菜单槽位的初始同步与取出，不把外部只读取口伪装成可写库存。 */
final class CentrifugeMenuOutputSlotTest {
    @Test
    void clientInitialSyncAcceptsReadOnlyHandlerWithoutChangingAuthority() {
        ReadOnlyOutput output = new ReadOnlyOutput(7);
        CentrifugeMenu.OutputSlot slot = new CentrifugeMenu.OutputSlot(output, 0, 0, 0, true);
        slot.initialize(new ItemStack(Items.IRON_INGOT, 7));
        assertEquals(7, slot.getItem().getCount());
        slot.set(new ItemStack(Items.IRON_INGOT, 5));
        assertEquals(5, slot.getItem().getCount());
        assertEquals(7, output.stack.getCount());
        assertFalse(slot.mayPlace(new ItemStack(Items.GOLD_INGOT)));
    }

    @Test
    void serverNormalAndShiftTakeExtractOnlyAndIgnoreSet() {
        ReadOnlyOutput output = new ReadOnlyOutput(7);
        CentrifugeMenu.OutputSlot slot = new CentrifugeMenu.OutputSlot(output, 0, 0, 0, false);
        slot.initialize(new ItemStack(Items.GOLD_INGOT, 64));
        assertEquals(7, slot.getItem().getCount());
        assertEquals(2, slot.remove(2).getCount());
        assertEquals(5, output.stack.getCount());
        slot.set(new ItemStack(Items.GOLD_INGOT, 64));
        assertEquals(5, output.stack.getCount());
        assertEquals(5, slot.remove(64).getCount());
        assertTrue(output.stack.isEmpty());
    }

    private static final class ReadOnlyOutput implements IItemHandler {
        private ItemStack stack;

        private ReadOnlyOutput(int count) {
            stack = new ItemStack(Items.IRON_INGOT, count);
        }

        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return stack.copy(); }
        @Override public ItemStack insertItem(int slot, ItemStack offered, boolean simulate) { return offered; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack taken = stack.copyWithCount(Math.min(amount, stack.getCount()));
            if (!simulate) stack.shrink(taken.getCount());
            return taken;
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack offered) { return false; }
    }
}
