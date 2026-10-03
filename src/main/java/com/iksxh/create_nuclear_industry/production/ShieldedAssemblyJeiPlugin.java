package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import java.util.List;
import java.util.Arrays;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** JEI仅作客户端配方视图，四料数量和参考64RPM耗时不参与服务端事务。 */
@JeiPlugin
public final class ShieldedAssemblyJeiPlugin implements IModPlugin {
    private static final RecipeType<ShieldedAssemblyRecipe> TYPE = RecipeType.create(
            CreateNuclearIndustry.MOD_ID, "shielded_assembly", ShieldedAssemblyRecipe.class);
    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "shielded_assembly");
    }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()
                .createDrawableItemStack(new ItemStack(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get()))));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        List<ShieldedAssemblyRecipe> recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(FuelProcessingContent.SHIELDED_ASSEMBLY_TYPE.get()).stream()
                .map(holder -> holder.value()).toList();
        registration.addRecipes(TYPE, recipes);
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get()), TYPE);
    }
    private record Category(IDrawable icon) implements IRecipeCategory<ShieldedAssemblyRecipe> {
        @Override public RecipeType<ShieldedAssemblyRecipe> getRecipeType() { return TYPE; }
        @Override public Component getTitle() {
            return Component.translatable("block.create_nuclear_industry.shielded_assembly_station");
        }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 138; }
        @Override public int getHeight() { return 83; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, ShieldedAssemblyRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(6, 7).addItemStacks(stacks(recipe.pellet()));
            builder.addInputSlot(27, 7).addItemStacks(stacks(recipe.cladding()));
            builder.addInputSlot(6, 28).addItemStacks(stacks(recipe.solder()));
            builder.addInputSlot(27, 28).addItemStacks(stacks(recipe.grate()));
            builder.addOutputSlot(103, 18).addItemStack(recipe.result());
        }
        private static List<ItemStack> stacks(ShieldedAssemblyRecipe.Input input) {
            return Arrays.stream(input.ingredient().getItems()).map(stack -> stack.copyWithCount(input.count())).toList();
        }
        @Override public void draw(ShieldedAssemblyRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                                   double mouseX, double mouseY) {
            graphics.drawString(Minecraft.getInstance().font,
                    Component.translatable("gui.create_nuclear_industry.shielded_assembly.jei.speed"),
                    4, 55, 0xff606060, false);
            graphics.drawString(Minecraft.getInstance().font,
                    Component.translatable("gui.create_nuclear_industry.shielded_assembly.jei.time", 20),
                    4, 68, 0xff606060, false);
        }
    }
}
