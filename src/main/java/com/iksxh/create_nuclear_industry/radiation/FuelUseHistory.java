package com.iksxh.create_nuclear_industry.radiation;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** 服务端真实组件的使用历史；只写副本，不把列投影变成另一份库存。 */
public final class FuelUseHistory {
    public static final String KEY = "create_nuclear_industry:FuelHistory";
    private FuelUseHistory() {}
    public static boolean isUsed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getCompound(KEY).getBoolean("Used");
    }
    /** 保留所有原版组件和其他自定义数据，返回标记后的独立物品。 */
    public static ItemStack markUsedCopy(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (!copy.isEmpty()) CustomData.update(DataComponents.CUSTOM_DATA, copy, tag -> {
            CompoundTag history = tag.getCompound(KEY).copy();
            history.putBoolean("Used", true);
            tag.put(KEY, history);
        });
        return copy;
    }
    /** 仅供成功燃耗的准备阶段调用；取消事务时丢弃结果，输入保持不变。 */
    public static ItemStack prepareBurnCopy(ItemStack stack, double committedBurnUnits) {
        if (!Double.isFinite(committedBurnUnits) || committedBurnUnits < 0)
            throw new IllegalArgumentException("fuel burn must be finite and nonnegative");
        return committedBurnUnits > 0 ? markUsedCopy(stack) : stack.copy();
    }
}
