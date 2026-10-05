package com.iksxh.create_nuclear_industry.storage;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** 十六个永久单件槽的唯一账本；配置容量以槽位为单位，仅冻结新插入，不裁剪保存。 */
public final class DryStorageState {
    public static final int MAX_SLOTS = 16;
    private final ItemStack[] items = new ItemStack[MAX_SLOTS];
    private final Predicate<ItemStack> validator;
    public DryStorageState() { this(SpentFuelPayload::isValid); }
    /** 注入纯校验器供独立账本测试；正式方块始终使用封装载荷校验器。 */
    public DryStorageState(Predicate<ItemStack> validator) {
        this.validator = validator;
        Arrays.fill(items, ItemStack.EMPTY);
    }
    public ItemStack item(int slot) { return slot >= 0 && slot < MAX_SLOTS ? items[slot].copy() : ItemStack.EMPTY; }
    public int used() { return (int) Arrays.stream(items).filter(s -> !s.isEmpty()).count(); }
    public int lastOccupied() {
        for (int slot = MAX_SLOTS - 1; slot >= 0; slot--) if (!items[slot].isEmpty()) return slot;
        return -1;
    }
    /** 外观只是占用比例；减容后超额桶仍算已用，库存本身不依赖此值。 */
    public int storageLevel(int capacity) {
        return used() == 0 ? 0 : Math.min(4, (used() * 4 + capacity - 1) / Math.max(1, capacity));
    }
    public ItemStack insert(int slot, ItemStack offered, int capacity, boolean simulate) {
        if (slot < 0 || slot >= Math.clamp(capacity, 1, MAX_SLOTS) || used() >= capacity
                || !items[slot].isEmpty() || !validator.test(offered)) return offered.copy();
        if (!simulate) items[slot] = offered.copyWithCount(1);
        return offered.copyWithCount(offered.getCount() - 1);
    }
    public ItemStack extract(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= MAX_SLOTS || amount < 1) return ItemStack.EMPTY;
        ItemStack result = items[slot].copy();
        if (!simulate) items[slot] = ItemStack.EMPTY;
        return result;
    }
    /** 拆除事务领取后立刻清空；重复回调只能取得空列表。 */
    public List<ItemStack> drain() {
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < MAX_SLOTS; slot++) if (!items[slot].isEmpty()) result.add(extract(slot, 1, false));
        return result;
    }
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        for (int slot = 0; slot < MAX_SLOTS; slot++) if (!items[slot].isEmpty()) tag.put("Slot" + slot, items[slot].save(registries));
        return tag;
    }
    /** 当前版本恢复全部十六槽；配置减容和暂存的损坏记录均不会删除真实物品。 */
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        for (int slot = 0; slot < MAX_SLOTS; slot++) items[slot] = ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot));
    }
}
