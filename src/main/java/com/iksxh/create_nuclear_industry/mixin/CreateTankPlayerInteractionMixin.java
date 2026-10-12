package com.iksxh.create_nuclear_industry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity.CreativeSmartFluidTank;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** 仅补回原Create创造罐容器成功后的身份提交；能力始终受限，不增加运输许可或修改容器数量。 */
@Mixin(value = FluidTankBlock.class, remap = false)
public abstract class CreateTankPlayerInteractionMixin {
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/fluid/FluidHelper;tryEmptyItemIntoBE(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;)Z"), require = 1)
    private boolean cni$markCreativeTank(Level level, Player player, InteractionHand hand, ItemStack stack,
                                        SmartBlockEntity target, Operation<Boolean> original) {
        boolean exchanged = original.call(level, player, hand, stack, target);
        CreativeSmartFluidTank inventory = exchanged ? cni$creativeInventory(level, target) : null;
        if (inventory != null) {
            // 原helper已成功完成模拟/执行与玩家返还；创造罐不会耗掉手持栈，沿原Create再次模拟读取身份。
            FluidStack identity = GenericItemEmptying.emptyItem(level, stack, true).getFirst();
            if (!identity.isEmpty()) inventory.setContainedFluid(identity);
        }
        return exchanged;
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/fluid/FluidHelper;tryFillItemFromBE(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;)Z"), require = 1)
    private boolean cni$clearCreativeTank(Level level, Player player, InteractionHand hand, ItemStack stack,
                                         SmartBlockEntity target, Operation<Boolean> original) {
        boolean exchanged = original.call(level, player, hand, stack, target);
        CreativeSmartFluidTank inventory = exchanged ? cni$creativeInventory(level, target) : null;
        if (inventory != null) inventory.setContainedFluid(FluidStack.EMPTY);
        return exchanged;
    }

    /** 仅在本次真实服务端创造罐controller上接续既有身份设置；缺区块、替换、普通罐或锅炉路径都保持原结果。 */
    @Unique
    private static CreativeSmartFluidTank cni$creativeInventory(Level level, SmartBlockEntity target) {
        if (level.isClientSide || !(target instanceof CreativeFluidTankBlockEntity tank)
                || !level.hasChunkAt(target.getBlockPos()) || level.getBlockEntity(target.getBlockPos()) != target) return null;
        var controller = tank.getControllerBE();
        if (!(controller instanceof CreativeFluidTankBlockEntity) || !controller.isController()
                || !level.hasChunkAt(controller.getBlockPos()) || level.getBlockEntity(controller.getBlockPos()) != controller
                || controller.boiler.isActive()) return null;
        return controller.getTankInventory() instanceof CreativeSmartFluidTank inventory ? inventory : null;
    }
}
