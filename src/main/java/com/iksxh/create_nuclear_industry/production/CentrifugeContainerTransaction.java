package com.iksxh.create_nuclear_industry.production;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * 单个标准桶与机器侧端口的整桶交易。传入的物品处理器必须持有手中单件副本；
 * 先模拟两端，再改物品副本，最后改机器，失败时不提交玩家手持物。
 */
public final class CentrifugeContainerTransaction {
    private CentrifugeContainerTransaction() {}

    /** 成功返回变更后的容器，否则返回空栈；所有数量严格为 1000 mB。 */
    public static ItemStack transfer(IFluidHandler port, IFluidHandlerItem item) {
        if (port == null || item == null || item.getContainer().getCount() != 1) return ItemStack.EMPTY;
        FluidStack offered = item.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        if (offered.getAmount() == 1000 && port.fill(offered, IFluidHandler.FluidAction.SIMULATE) == 1000) {
            FluidStack removed = item.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            return removed.getAmount() == 1000 && port.fill(removed, IFluidHandler.FluidAction.EXECUTE) == 1000
                    ? item.getContainer() : ItemStack.EMPTY;
        }
        FluidStack available = port.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        if (available.getAmount() != 1000 || item.fill(available, IFluidHandler.FluidAction.SIMULATE) != 1000) {
            return ItemStack.EMPTY;
        }
        if (item.fill(available, IFluidHandler.FluidAction.EXECUTE) != 1000) return ItemStack.EMPTY;
        FluidStack removed = port.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        return removed.getAmount() == 1000 ? item.getContainer() : ItemStack.EMPTY;
    }
}
