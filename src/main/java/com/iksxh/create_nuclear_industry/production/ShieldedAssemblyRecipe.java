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

/** 四料数量及总工时由数据包同步；机器只执行符合已确认身份和参数的装配合同。 */
public record ShieldedAssemblyRecipe(Input pellet, Input cladding, Input solder,
                                     Input grate, ItemStack result, int work) implements Recipe<RecipeInput> {
    /** 每种输入同时携带材料谓词与一批数量，数据包及网络用同一结构。 */
    public record Input(Ingredient ingredient, int count) {
        public static final MapCodec<Input> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(Input::ingredient),
                com.mojang.serialization.Codec.INT.fieldOf("count").forGetter(Input::count)
        ).apply(instance, Input::new));
        public boolean test(ItemStack stack) { return ingredient.test(stack); }
    }
    public static final MapCodec<ShieldedAssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Input.CODEC.fieldOf("pellet").forGetter(ShieldedAssemblyRecipe::pellet),
            Input.CODEC.fieldOf("cladding").forGetter(ShieldedAssemblyRecipe::cladding),
            Input.CODEC.fieldOf("solder").forGetter(ShieldedAssemblyRecipe::solder),
            Input.CODEC.fieldOf("grate").forGetter(ShieldedAssemblyRecipe::grate),
            ItemStack.CODEC.fieldOf("result").forGetter(ShieldedAssemblyRecipe::result),
            com.mojang.serialization.Codec.INT.fieldOf("work").forGetter(ShieldedAssemblyRecipe::work)
    ).apply(instance, ShieldedAssemblyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldedAssemblyRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override public ShieldedAssemblyRecipe decode(RegistryFriendlyByteBuf buffer) {
            return new ShieldedAssemblyRecipe(readInput(buffer), readInput(buffer), readInput(buffer),
                    readInput(buffer), ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ShieldedAssemblyRecipe recipe) {
            writeInput(buffer, recipe.pellet);
            writeInput(buffer, recipe.cladding);
            writeInput(buffer, recipe.solder);
            writeInput(buffer, recipe.grate);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeVarInt(recipe.work);
        }
    };

    private static Input readInput(RegistryFriendlyByteBuf buffer) {
        return new Input(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt());
    }
    private static void writeInput(RegistryFriendlyByteBuf buffer, Input input) {
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, input.ingredient());
        buffer.writeVarInt(input.count());
    }

    @Override public boolean matches(RecipeInput input, Level level) {
        return input.size() == 4 && pellet.test(input.getItem(0)) && cladding.test(input.getItem(1))
                && solder.test(input.getItem(2)) && grate.test(input.getItem(3));
    }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 4; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public RecipeSerializer<?> getSerializer() { return FuelProcessingContent.SHIELDED_ASSEMBLY_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return FuelProcessingContent.SHIELDED_ASSEMBLY_TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<ShieldedAssemblyRecipe> {
        @Override public MapCodec<ShieldedAssemblyRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ShieldedAssemblyRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
