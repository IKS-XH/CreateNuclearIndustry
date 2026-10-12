package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.industrial.RestrictionPolicy;
import com.simibubi.create.content.fluids.FluidNetwork;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 锁定Create已识别介质、任何模拟/执行drain之前的唯一闸门，拒绝整次受限交易。 */
@Mixin(value = FluidNetwork.class, remap = false)
abstract class FluidNetworkRestrictionMixin {
    @Shadow FluidStack fluid;
    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/fluids/FluidNetwork;keepPortableFluidInterfaceEngaged()V", remap = false),
            cancellable = true, remap = false, require = 1, expect = 1, allow = 1)
    private void createNuclearIndustry$rejectRestricted(CallbackInfo callback) {
        if (RestrictionPolicy.restricted(fluid)) callback.cancel();
    }
}
