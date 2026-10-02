package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** 专用烧结配方的数据身份；运行炉只接受已批准的生芯块和一件产物。 */
public record FuelSinteringRecipe(Ingredient input, ItemStack result, int work) implements Recipe<RecipeInput> {
    public static final MapCodec<FuelSinteringRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(FuelSinteringRecipe::input),
            ItemStack.CODEC.fieldOf("result").forGetter(FuelSinteringRecipe::result),
            com.mojang.serialization.Codec.INT.fieldOf("work").forGetter(FuelSinteringRecipe::work)
    ).apply(instance, FuelSinteringRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FuelSinteringRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override public FuelSinteringRecipe decode(RegistryFriendlyByteBuf buffer) {
            return new FuelSinteringRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                    ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, FuelSinteringRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeVarInt(recipe.work);
        }
    };

    @Override public boolean matches(RecipeInput inventory, Level level) {
        return inventory.size() == 1 && input.test(inventory.getItem(0));
    }
    @Override public ItemStack assemble(RecipeInput inventory, HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public RecipeSerializer<?> getSerializer() { return FuelProcessingContent.SINTERING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return FuelProcessingContent.SINTERING_TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<FuelSinteringRecipe> {
        @Override public MapCodec<FuelSinteringRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, FuelSinteringRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
