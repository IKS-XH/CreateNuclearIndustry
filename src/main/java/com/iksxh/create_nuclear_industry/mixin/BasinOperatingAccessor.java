package com.iksxh.create_nuclear_industry.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 仅让工作盆热桥复用Create既有候选顺序与配方匹配入口。 */
@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
public interface BasinOperatingAccessor {
    @Invoker("getMatchingRecipes")
    List<Recipe<?>> createNuclearIndustry$getMatchingRecipes();

    @Invoker("getBasin")
    Optional<BasinBlockEntity> createNuclearIndustry$getBasin();

    @Invoker("matchBasinRecipe")
    boolean createNuclearIndustry$matchBasinRecipe(Recipe<?> recipe);
}
