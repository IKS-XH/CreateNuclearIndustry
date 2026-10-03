package com.iksxh.create_nuclear_industry.production;

import java.util.Arrays;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 装配台唯一物品与工时账本。四个输入槽依次为芯块、包壳、焊料、格架，输出槽仅容一件。
 * 此类不查询世界；方块实体在服务端确认配方和动力后才允许提交完整批次。
 */
public final class ShieldedAssemblyState {
    public static final int[] COST = {8, 4, 2, 1};
    public static final int WORK = 25_600;
    private final ItemStack[] input = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private ItemStack output = ItemStack.EMPTY;
    private int progress;

    public ItemStack input(int slot) { return slot >= 0 && slot < 4 ? input[slot].copy() : ItemStack.EMPTY; }
    public ItemStack output() { return output.copy(); }
    public int progress() { return progress; }
    public boolean ready() {
        for (int slot = 0; slot < 4; slot++) if (input[slot].getCount() < COST[slot]) return false;
        return true;
    }
    public boolean outputEmpty() { return output.isEmpty(); }

    /** 仅在外层已核实材料身份后调用；模拟和不兼容组件均不改写账本。 */
    public ItemStack insert(int slot, ItemStack offered, boolean simulate) {
        if (slot < 0 || slot >= 4 || offered.isEmpty()) return offered.copy();
        ItemStack stored = input[slot];
        if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, offered)) return offered.copy();
        int room = Math.min(64, offered.getMaxStackSize()) - stored.getCount();
        int accepted = Math.min(offered.getCount(), Math.max(0, room));
        if (!simulate && accepted > 0) {
            if (stored.isEmpty()) input[slot] = offered.copyWithCount(accepted);
            else stored.grow(accepted);
        }
        return offered.copyWithCount(offered.getCount() - accepted);
    }

    /** 提取不足一批后立即清除工时，不能让后续补料继承旧进度。 */
    public ItemStack extractInput(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= 4 || amount <= 0 || input[slot].isEmpty()) return ItemStack.EMPTY;
        ItemStack result = input[slot].copyWithCount(Math.min(amount, input[slot].getCount()));
        if (!simulate) {
            input[slot].shrink(result.getCount());
            if (input[slot].isEmpty()) input[slot] = ItemStack.EMPTY;
            if (!ready()) progress = 0;
        }
        return result;
    }

    public ItemStack extractOutput(int amount, boolean simulate) {
        if (amount <= 0 || output.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = output.copy();
        if (!simulate) output = ItemStack.EMPTY;
        return result;
    }

    public void invalidateProgress() { progress = 0; }

    /** 有效速度以RPM计，单tick最多计256；完成前重新检查全部物料和输出槽。 */
    public boolean advance(float speed, ItemStack freshResult) {
        if (Math.abs(speed) < 32 || !ready() || !output.isEmpty() || freshResult.isEmpty()
                || freshResult.getCount() != 1) return false;
        progress += (int) Math.min(Math.abs(speed), 256);
        if (progress < WORK) return true;
        for (int slot = 0; slot < 4; slot++) {
            input[slot].shrink(COST[slot]);
            if (input[slot].isEmpty()) input[slot] = ItemStack.EMPTY;
        }
        output = freshResult.copy();
        progress = 0;
        return true;
    }

    /** 完整ItemStack及进度共用同一保存格式，携带物与区块存储均复用此格式。 */
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Progress", progress);
        for (int slot = 0; slot < 4; slot++) if (!input[slot].isEmpty())
            tag.put("Input" + slot, input[slot].save(registries));
        if (!output.isEmpty()) tag.put("Output", output.save(registries));
        return tag;
    }

    /** 损坏或旧标签按空槽恢复；不从无物品记录中重造产物。 */
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        Arrays.fill(input, ItemStack.EMPTY);
        for (int slot = 0; slot < 4; slot++) {
            ItemStack parsed = ItemStack.parseOptional(registries, tag.getCompound("Input" + slot));
            if (!parsed.isEmpty() && parsed.getCount() <= 64) input[slot] = parsed;
        }
        ItemStack parsedOutput = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        output = parsedOutput.getCount() == 1 ? parsedOutput : ItemStack.EMPTY;
        progress = Math.clamp(tag.getInt("Progress"), 0, WORK - 1);
        if (!ready()) progress = 0;
    }
}
