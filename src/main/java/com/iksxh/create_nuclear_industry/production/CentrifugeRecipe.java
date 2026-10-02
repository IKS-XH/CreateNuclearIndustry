package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 离心机独立的数据配方。输入与全部输出以 mB 和物品栈记录，启动后复制进密闭批次。
 * 原生配方重载只影响新批次，不能改变已接管料浆的产物。
 */
public record CentrifugeRecipe(FluidStack input, ItemStack enriched, ItemStack depleted,
                               FluidStack water, int work) implements Recipe<RecipeInput> {
    public static final MapCodec<CentrifugeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FluidStack.CODEC.fieldOf("input").forGetter(CentrifugeRecipe::input),
            ItemStack.CODEC.fieldOf("enriched").forGetter(CentrifugeRecipe::enriched),
            ItemStack.CODEC.fieldOf("depleted").forGetter(CentrifugeRecipe::depleted),
            FluidStack.CODEC.fieldOf("water").forGetter(CentrifugeRecipe::water),
            com.mojang.serialization.Codec.INT.fieldOf("work").forGetter(CentrifugeRecipe::work)
    ).apply(instance, CentrifugeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CentrifugeRecipe decode(RegistryFriendlyByteBuf buffer) {
            return new CentrifugeRecipe(FluidStack.STREAM_CODEC.decode(buffer), ItemStack.STREAM_CODEC.decode(buffer),
                    ItemStack.STREAM_CODEC.decode(buffer), FluidStack.STREAM_CODEC.decode(buffer), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, CentrifugeRecipe recipe) {
            FluidStack.STREAM_CODEC.encode(buffer, recipe.input);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.enriched);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.depleted);
            FluidStack.STREAM_CODEC.encode(buffer, recipe.water);
            buffer.writeVarInt(recipe.work);
        }
    };

    /** 此配方由流体罐匹配，物品配方输入接口不承担机器库存校验。 */
    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return enriched.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return enriched.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FuelProcessingContent.CENTRIFUGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return FuelProcessingContent.CENTRIFUGING_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<CentrifugeRecipe> {
        @Override
        public MapCodec<CentrifugeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
