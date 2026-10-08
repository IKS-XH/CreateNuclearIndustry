package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.heat.HeatExchangerBasinBridge;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 需求探测仅替换Create的同一原生候选调用，配方过滤及处理顺序继续由Create负责。 */
@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
abstract class BasinOperatingMixin {
    @Redirect(method = "matchBasinRecipe", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/processing/basin/BasinRecipe;match(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;)Z"),
            remap = false)
    private boolean createNuclearIndustry$probeNativeCandidate(BasinBlockEntity basin, Recipe<?> recipe) {
        return HeatExchangerBasinBridge.matchCandidate((BasinOperatingBlockEntity) (Object) this, basin, recipe);
    }
}
