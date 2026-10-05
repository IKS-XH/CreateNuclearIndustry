package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.SpentFuelStorageContent;
import com.iksxh.create_nuclear_industry.storage.SpentFuelPayload;
import java.util.List;
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

/** 工序、材料数量和RPM·tick工时由数据包同步；封存输出保存实际投入的单件组件。 */
public record ShieldedAssemblyRecipe(String operation, List<Input> inputs, ItemStack result, int work) implements Recipe<RecipeInput> {
    /** 每种输入同时携带材料谓词与一批数量，数据包及网络用同一结构。 */
    public record Input(Ingredient ingredient, int count) {
        public static final MapCodec<Input> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(Input::ingredient),
                com.mojang.serialization.Codec.INT.fieldOf("count").forGetter(Input::count)
        ).apply(instance, Input::new));
        public boolean test(ItemStack stack) { return ingredient.test(stack); }
    }
    public static final MapCodec<ShieldedAssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.STRING.fieldOf("operation").forGetter(ShieldedAssemblyRecipe::operation),
            Input.CODEC.codec().listOf().fieldOf("inputs").forGetter(ShieldedAssemblyRecipe::inputs),
            ItemStack.CODEC.fieldOf("result").forGetter(ShieldedAssemblyRecipe::result),
            com.mojang.serialization.Codec.INT.fieldOf("work").forGetter(ShieldedAssemblyRecipe::work)
    ).apply(instance, ShieldedAssemblyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldedAssemblyRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override public ShieldedAssemblyRecipe decode(RegistryFriendlyByteBuf buffer) {
            String operation = buffer.readUtf();
            int size = buffer.readVarInt();
            if (size < 1 || size > 4) throw new IllegalArgumentException("装配输入槽数无效");
            java.util.ArrayList<Input> inputs = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) inputs.add(readInput(buffer));
            return new ShieldedAssemblyRecipe(operation, List.copyOf(inputs), ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ShieldedAssemblyRecipe recipe) {
            buffer.writeUtf(recipe.operation);
            buffer.writeVarInt(recipe.inputs.size());
            for (Input input : recipe.inputs) writeInput(buffer, input);
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

    /** 固定工序和单件产物身份；普通材料数量与工时不被代码重新锁死。 */
    public boolean valid() {
        if (work < 1 || inputs.isEmpty() || inputs.size() > 4 || result.getCount() != 1
                || inputs.stream().anyMatch(i -> i.count < 1 || i.count > 64 || i.ingredient.isEmpty())) return false;
        return "manufacture".equals(operation) ? inputs.size() == 4 && result.is(ModItems.FRESH_FUEL_ASSEMBLY.get())
                && result.getDamageValue() == 0
                : "sealing".equals(operation) && inputs.size() == 3 && inputs.getFirst().count == 1
                && inputs.getFirst().test(new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()))
                && result.is(SpentFuelStorageContent.SEALED_SPENT_FUEL_CASK.get());
    }
    public boolean accepts(int slot, ItemStack stack) {
        return valid() && slot >= 0 && slot < inputs.size() && !stack.isEmpty()
                && inputs.get(slot).test(stack)
                && (!"manufacture".equals(operation) || !stack.is(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get())
                    && !stack.is(SpentFuelStorageContent.SEALED_SPENT_FUEL_CASK.get()))
                && (!"sealing".equals(operation) || slot != 0 || stack.is(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()));
    }
    public int[] costs() { return inputs.stream().mapToInt(Input::count).toArray(); }
    @Override public boolean matches(RecipeInput input, Level level) {
        if (!valid() || input.size() < inputs.size()) return false;
        for (int i = 0; i < inputs.size(); i++)
            if (!accepts(i, input.getItem(i)) || input.getItem(i).getCount() < inputs.get(i).count) return false;
        return true;
    }
    /** 无载荷桶只作JEI模板；实际输出从槽0完整栈构造，不复制独立可提取库存。 */
    public ItemStack actualResult(ItemStack spent) {
        return "sealing".equals(operation) ? SpentFuelPayload.seal(spent) : result.copy();
    }
    @Override public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) { return actualResult(input.getItem(0)); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= inputs.size(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public RecipeSerializer<?> getSerializer() { return FuelProcessingContent.SHIELDED_ASSEMBLY_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return FuelProcessingContent.SHIELDED_ASSEMBLY_TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<ShieldedAssemblyRecipe> {
        @Override public MapCodec<ShieldedAssemblyRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ShieldedAssemblyRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
