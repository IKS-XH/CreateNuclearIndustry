package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.industrial.RestrictedFluidHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 普通/创造罐均继承该私有能力选择入口；包装同一共享库存，保留原生锅炉handler语义。 */
@Mixin(value = FluidTankBlockEntity.class, remap = false)
abstract class CreateTankCapabilityRestrictionMixin {
    @Inject(method = "handlerForCapability", at = @At("RETURN"), cancellable = true, remap = false, require = 1)
    private void createNuclearIndustry$wrapInventory(CallbackInfoReturnable<IFluidHandler> callback) {
        callback.setReturnValue(RestrictedFluidHandler.wrap(callback.getReturnValue()));
    }
}
