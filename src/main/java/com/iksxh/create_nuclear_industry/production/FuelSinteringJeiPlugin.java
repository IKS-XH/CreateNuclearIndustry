package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import java.util.List;
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

/** JEI 可选客户端入口；显示一件生芯块、烧结产物、最低热级及有效 tick。 */
@JeiPlugin
public final class FuelSinteringJeiPlugin implements IModPlugin {
    private static final RecipeType<FuelSinteringRecipe> TYPE = RecipeType.create(
            CreateNuclearIndustry.MOD_ID, "sintering", FuelSinteringRecipe.class);
    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "sintering");
    }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()
                .createDrawableItemStack(new ItemStack(FuelProcessingContent.FUEL_SINTERING_FURNACE_ITEM.get()))));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        List<FuelSinteringRecipe> recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(FuelProcessingContent.SINTERING_TYPE.get()).stream().map(holder -> holder.value()).toList();
        registration.addRecipes(TYPE, recipes);
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(FuelProcessingContent.FUEL_SINTERING_FURNACE_ITEM.get()), TYPE);
    }
    private record Category(IDrawable icon) implements IRecipeCategory<FuelSinteringRecipe> {
        @Override public RecipeType<FuelSinteringRecipe> getRecipeType() { return TYPE; }
        @Override public Component getTitle() {
            return Component.translatable("block.create_nuclear_industry.fuel_sintering_furnace");
        }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 112; }
        @Override public int getHeight() { return 62; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, FuelSinteringRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(8, 8).addIngredients(recipe.input());
            builder.addOutputSlot(80, 8).addItemStack(recipe.result());
        }
        @Override public void draw(FuelSinteringRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                                   double mouseX, double mouseY) {
            graphics.drawString(Minecraft.getInstance().font,
                    Component.translatable("gui.create_nuclear_industry.sintering.jei.heat"),
                    4, 34, 0xff606060, false);
            graphics.drawString(Minecraft.getInstance().font,
                    Component.translatable("gui.create_nuclear_industry.sintering.jei.time", recipe.work()),
                    4, 46, 0xff606060, false);
        }
    }
}
