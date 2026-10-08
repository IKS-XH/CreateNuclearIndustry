package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.heat.HeatExchangerBasinBridge;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 将费用事务限定在Create原生apply的实际提交调用，模拟匹配仍由Create负责。 */
@Mixin(value = BasinRecipe.class, remap = false)
abstract class BasinRecipeHeatMixin {
    @Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void createNuclearIndustry$reserveBatch(BasinBlockEntity basin, Recipe<?> recipe, boolean test,
                                                            CallbackInfoReturnable<Boolean> cir) {
        if (!test && !HeatExchangerBasinBridge.beginApply(basin, recipe)) cir.setReturnValue(false);
    }

    @Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
            at = @At("RETURN"), remap = false)
    private static void createNuclearIndustry$settleBatch(BasinBlockEntity basin, Recipe<?> recipe, boolean test,
                                                           CallbackInfoReturnable<Boolean> cir) {
        if (!test) HeatExchangerBasinBridge.finishApply(cir.getReturnValue());
    }
}
