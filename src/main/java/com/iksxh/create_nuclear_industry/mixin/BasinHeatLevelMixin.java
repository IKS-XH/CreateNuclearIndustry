package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.heat.HeatExchangerBasinBridge;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 工作盆只读取正下方换热器已经支付的热级，不写入Create的tick缓存。 */
@Mixin(value = BasinBlockEntity.class, remap = false)
abstract class BasinHeatLevelMixin {
    @Inject(method = "getHeatLevel()Lcom/simibubi/create/content/processing/burner/BlazeBurnerBlock$HeatLevel;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void createNuclearIndustry$readPaidBasinHeat(CallbackInfoReturnable<HeatLevel> cir) {
        BasinBlockEntity basin = (BasinBlockEntity) (Object) this;
        HeatLevel heat = HeatExchangerBasinBridge.heatLevel(basin);
        if (heat != null) cir.setReturnValue(heat);
    }
}
