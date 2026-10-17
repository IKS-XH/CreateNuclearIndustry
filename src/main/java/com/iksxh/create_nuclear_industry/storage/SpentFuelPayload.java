package com.iksxh.create_nuclear_industry.storage;

import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.SpentFuelStorageContent;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** 单件核废料封存事务；真实原乏燃料或事故废料是桶内容，不暴露第二份可访问库存。 */
public final class SpentFuelPayload {
    private SpentFuelPayload() {}

    /** 只读检验恰好一个数量1的乏燃料或事故废料；混装、全新燃料、空桶和异物均拒绝。 */
    public static boolean isValid(ItemStack stack) {
        if (!stack.is(SpentFuelStorageContent.SEALED_SPENT_FUEL_CASK.get()) || stack.getCount() != 1) return false;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) return false;
        List<ItemStack> stored = contents.stream().toList();
        return stored.size() == 1 && stored.getFirst().getCount() == 1
                && allowed(stored.getFirst());
    }

    /** 校验后返回一件独立封装栈，完整保留组件数据；调用者在完成批次时原子扣除输入。 */
    public static ItemStack seal(ItemStack spent) {
        if (!allowed(spent) || spent.getCount() != 1) return ItemStack.EMPTY;
        ItemStack sealed = new ItemStack(SpentFuelStorageContent.SEALED_SPENT_FUEL_CASK.get());
        sealed.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(spent.copy())));
        return sealed;
    }
    public static boolean allowed(ItemStack stack) {
        return stack.is(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get())
                || stack.is(com.iksxh.create_nuclear_industry.content.AccidentContent.ACCIDENT_FUEL_WASTE.get());
    }
}
