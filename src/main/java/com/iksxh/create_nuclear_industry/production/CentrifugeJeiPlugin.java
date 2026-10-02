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

/** 仅由 JEI 在客户端扫描的可选插件；服务端与未安装 JEI 的环境不主动加载本类。 */
@JeiPlugin
public final class CentrifugeJeiPlugin implements IModPlugin {
    private static final RecipeType<CentrifugeRecipe> TYPE = RecipeType.create(
            CreateNuclearIndustry.MOD_ID, "centrifuging", CentrifugeRecipe.class);

    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "centrifuging");
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()
                .createDrawableItemStack(new ItemStack(FuelProcessingContent.ENRICHMENT_CENTRIFUGE_ITEM.get()))));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        List<CentrifugeRecipe> recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(FuelProcessingContent.CENTRIFUGING_TYPE.get()).stream()
                .map(holder -> holder.value()).toList();
        registration.addRecipes(TYPE, recipes);
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(FuelProcessingContent.ENRICHMENT_CENTRIFUGE_ITEM.get()), TYPE);
    }

    private record Category(IDrawable icon) implements IRecipeCategory<CentrifugeRecipe> {
        @Override public RecipeType<CentrifugeRecipe> getRecipeType() { return TYPE; }
        @Override public Component getTitle() {
            return Component.translatable("block.create_nuclear_industry.enrichment_centrifuge");
        }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 132; }
        @Override public int getHeight() { return 68; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, CentrifugeRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(4, 8).addFluidStack(recipe.input().getFluid(), recipe.input().getAmount())
                    .setFluidRenderer(1000, false, 16, 16);
            builder.addOutputSlot(80, 8).addItemStack(recipe.enriched());
            builder.addOutputSlot(105, 8).addItemStack(recipe.depleted());
            builder.addOutputSlot(92, 35).addFluidStack(recipe.water().getFluid(), recipe.water().getAmount())
                    .setFluidRenderer(1000, false, 16, 16);
        }
        @Override public void draw(CentrifugeRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics,
                                   double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, Component.translatable("gui.create_nuclear_industry.centrifuge.rated", recipe.work()),
                    3, 57, 0xff606060, false);
        }
    }
}
