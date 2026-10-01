package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.OreContent;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 铅锡新增原矿、深层矿和粉碎料路线的服务端验证。
 * 炉子与水洗均由真实方块实体在逻辑服务端 tick，配方检查读取运行时管理器。
 */
@GameTestHolder(CreateNuclearIndustry.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExtensionNativeMetalProcessingGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos FURNACE = new BlockPos(2, 2, 2);
    private static final BlockPos FAN = new BlockPos(1, 2, 2);
    private static final BlockPos MOTOR = FAN.west();
    private static final BlockPos WATER = FAN.east();
    private static final BlockPos DEPOT = WATER.east().below();
    private static final BlockPos AIR_STOP = WATER.east(2);

    private enum Form { ORE, DEEPSLATE, CRUSHED }

    private ExtensionNativeMetalProcessingGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void leadOreSmelts(GameTestHelper helper) { cook(helper, "lead", Form.ORE, false); }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void leadOreBlasts(GameTestHelper helper) { cook(helper, "lead", Form.ORE, true); }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void leadDeepslateOreSmelts(GameTestHelper helper) { cook(helper, "lead", Form.DEEPSLATE, false); }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void leadDeepslateOreBlasts(GameTestHelper helper) { cook(helper, "lead", Form.DEEPSLATE, true); }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void leadCrushedSmelts(GameTestHelper helper) { cook(helper, "lead", Form.CRUSHED, false); }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void leadCrushedBlasts(GameTestHelper helper) { cook(helper, "lead", Form.CRUSHED, true); }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void tinOreSmelts(GameTestHelper helper) { cook(helper, "tin", Form.ORE, false); }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void tinOreBlasts(GameTestHelper helper) { cook(helper, "tin", Form.ORE, true); }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void tinDeepslateOreSmelts(GameTestHelper helper) { cook(helper, "tin", Form.DEEPSLATE, false); }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void tinDeepslateOreBlasts(GameTestHelper helper) { cook(helper, "tin", Form.DEEPSLATE, true); }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void tinCrushedSmelts(GameTestHelper helper) { cook(helper, "tin", Form.CRUSHED, false); }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void tinCrushedBlasts(GameTestHelper helper) { cook(helper, "tin", Form.CRUSHED, true); }

    /** 每种输入走真实熔炉或高炉，完工前后分别检查未提前消费和固定一锭产量。 */
    private static void cook(GameTestHelper helper, String metal, Form form, boolean blast) {
        OreContent.Mineral mineral = metal.equals("lead") ? OreContent.LEAD : OreContent.TIN;
        Item input = switch (form) {
            case ORE -> mineral.ore().get().asItem();
            case DEEPSLATE -> mineral.deepslateOre().get().asItem();
            case CRUSHED -> crushed(metal);
        };
        String method = blast ? "blasting" : "smelting";
        ResourceLocation recipeId = id(method + "/" + metal + "_ingot_from_"
                + (form == Form.CRUSHED ? "crushed_raw_" + metal : metal + "_ore"));
        var holder = helper.getLevel().getRecipeManager().byKey(recipeId).orElseThrow();
        require(helper, holder.value() instanceof AbstractCookingRecipe, "缺少原生炉配方: " + recipeId);
        AbstractCookingRecipe recipe = (AbstractCookingRecipe) holder.value();
        Item ingot = registered(helper, metal + "_ingot");
        int duration = blast ? 100 : 200;
        float experience = form == Form.CRUSHED ? 0.1f : 0.7f;
        require(helper, recipe.getType() == (blast ? RecipeType.BLASTING : RecipeType.SMELTING)
                        && recipe.getCookingTime() == duration && recipe.getExperience() == experience,
                "炉类型、时间或经验不符: " + recipeId);
        require(helper, recipe.matches(new SingleRecipeInput(new ItemStack(input)), helper.getLevel())
                        && recipe.getResultItem(helper.getLevel().registryAccess()).is(ingot)
                        && recipe.getResultItem(helper.getLevel().registryAccess()).getCount() == 1,
                "配方输入或固定一锭输出不符: " + recipeId);
        String other = metal.equals("lead") ? "tin" : "lead";
        Item wrongMetal = form == Form.CRUSHED ? crushed(other)
                : (metal.equals("lead") ? OreContent.TIN.ore().get().asItem() : OreContent.LEAD.ore().get().asItem());
        Item wrongForm = form == Form.CRUSHED ? mineral.ore().get().asItem() : mineral.raw().get();
        require(helper, !recipe.matches(new SingleRecipeInput(new ItemStack(wrongMetal)), helper.getLevel())
                        && !recipe.matches(new SingleRecipeInput(new ItemStack(wrongForm)), helper.getLevel()),
                "炉配方接受错误金属或错误形态: " + recipeId);

        helper.setBlock(FURNACE, blast ? Blocks.BLAST_FURNACE : Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) helper.getBlockEntity(FURNACE);
        furnace.setItem(0, new ItemStack(input));
        furnace.setItem(1, new ItemStack(Items.COAL));
        helper.runAfterDelay(duration - 2, () -> {
            require(helper, furnace.getItem(0).is(input) && furnace.getItem(0).getCount() == 1
                            && furnace.getItem(2).isEmpty(), "炉子提前消费或产出: " + recipeId);
        });
        helper.runAfterDelay(duration + 3, () -> {
            require(helper, furnace.getItem(0).isEmpty() && furnace.getItem(2).is(ingot)
                            && furnace.getItem(2).getCount() == 1, "真实炉子未得到一件对应锭: " + recipeId);
            helper.succeed();
        });
    }

    /** 两种洗矿配方须从各自粉碎料固定得到九粒，且不混入其他产物。 */
    @GameTest(template = TEMPLATE)
    public static void splashingRecipesLoadWithNuggetTags(GameTestHelper helper) {
        for (String metal : List.of("lead", "tin")) {
            Item nugget = registered(helper, metal + "_nugget");
            require(helper, nugget.getDefaultMaxStackSize() == 64
                            && new ItemStack(nugget).is(itemTag("nuggets/" + metal))
                            && new ItemStack(nugget).is(itemTag("nuggets")), "金属粒注册或通用标签错误: " + metal);
            ResourceLocation recipeId = id("splashing/crushed_raw_" + metal);
            var holder = helper.getLevel().getRecipeManager().byKey(recipeId).orElseThrow();
            require(helper, holder.value() instanceof SplashingRecipe, "未加载 Create 水洗配方: " + recipeId);
            SplashingRecipe recipe = (SplashingRecipe) holder.value();
            String other = metal.equals("lead") ? "tin" : "lead";
            require(helper, recipe.matches(new SingleRecipeInput(new ItemStack(crushed(metal))), helper.getLevel())
                            && !recipe.matches(new SingleRecipeInput(new ItemStack(crushed(other))), helper.getLevel())
                            && !recipe.matches(new SingleRecipeInput(new ItemStack(Items.RAW_IRON)), helper.getLevel()),
                    "水洗输入未按金属粉碎料标签匹配: " + recipeId);
            require(helper, recipe.getRollableResults().size() == 1
                            && recipe.getRollableResults().getFirst().getStack().is(nugget)
                            && recipe.getRollableResults().getFirst().getStack().getCount() == 9
                            && recipe.getRollableResults().getFirst().getChance() == 1,
                    "水洗未固定产九粒或出现副产物: " + recipeId);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void leadCrushedWashesOnlyWithFanPower(GameTestHelper helper) { wash(helper, "lead"); }

    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void tinCrushedWashesOnlyWithFanPower(GameTestHelper helper) { wash(helper, "tin"); }

    /** 水源位于风扇与置物台之间；停转时保留粉碎料，恢复轴动力后由真实气流加工。 */
    private static void wash(GameTestHelper helper, String metal) {
        Item crushed = crushed(metal);
        Item nugget = registered(helper, metal + "_nugget");
        helper.setBlock(FAN, AllBlocks.ENCASED_FAN.getDefaultState().setValue(EncasedFanBlock.FACING, Direction.EAST));
        helper.setBlock(WATER, Blocks.WATER);
        helper.setBlock(DEPOT, AllBlocks.DEPOT.get());
        helper.setBlock(AIR_STOP, Blocks.STONE);
        helper.runAfterDelay(5, () -> {
            var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(DEPOT), Direction.UP);
            require(helper, handler != null && handler.insertItem(0, new ItemStack(crushed), false).isEmpty(),
                    "置物台未收下粉碎料: " + metal);
        });
        helper.runAfterDelay(45, () -> {
            DepotBlockEntity depot = (DepotBlockEntity) helper.getBlockEntity(DEPOT);
            require(helper, depot.getHeldItem().is(crushed) && depot.getHeldItem().getCount() == 1,
                    "风扇停转时粉碎料未保留: " + metal);
            helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                    .setValue(CreativeMotorBlock.FACING, Direction.EAST));
        });
        helper.runAfterDelay(50, () -> {
            ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(256);
        });
        helper.runAfterDelay(80, () -> {
            EncasedFanBlockEntity fan = (EncasedFanBlockEntity) helper.getBlockEntity(FAN);
            require(helper, fan.getSpeed() != 0 && fan.getAirFlowDirection() == Direction.EAST
                            && fan.airCurrent.getTypeAt(1.5f) == AllFanProcessingTypes.SPLASHING,
                    "水源未进入真实有动力风扇气流: " + metal);
        });
        helper.runAfterDelay(350, () -> {
            DepotBlockEntity depot = (DepotBlockEntity) helper.getBlockEntity(DEPOT);
            require(helper, depot.getHeldItem().is(nugget) && depot.getHeldItem().getCount() == 9,
                    "真实水洗未得到九粒对应金属: " + metal);
            helper.succeed();
        });
    }

    /** 已加载的工作台配方须以九粒换一锭、再以一锭换九粒，不接受欠料或另一金属。 */
    @GameTest(template = TEMPLATE)
    public static void nuggetCraftingConservesAndRejectsWrongInputs(GameTestHelper helper) {
        for (String metal : List.of("lead", "tin")) {
            String other = metal.equals("lead") ? "tin" : "lead";
            Item nugget = registered(helper, metal + "_nugget");
            Item ingot = registered(helper, metal + "_ingot");
            Item otherNugget = registered(helper, other + "_nugget");
            Item otherIngot = registered(helper, other + "_ingot");
            var manager = helper.getLevel().getRecipeManager();
            ResourceLocation packId = id("crafting/materials/" + metal + "_ingot_from_nuggets");
            ResourceLocation unpackId = id("crafting/materials/" + metal + "_nugget_from_ingot");
            var packHolder = manager.byKey(packId).orElseThrow();
            var unpackHolder = manager.byKey(unpackId).orElseThrow();
            require(helper, packHolder.value() instanceof CraftingRecipe
                            && unpackHolder.value() instanceof CraftingRecipe,
                    "未加载工作台压合或拆解配方: " + metal);
            CraftingRecipe pack = (CraftingRecipe) packHolder.value();
            CraftingRecipe unpack = (CraftingRecipe) unpackHolder.value();
            List<ItemStack> inputs = new ArrayList<>();
            for (int i = 0; i < 9; i++) inputs.add(new ItemStack(nugget));
            CraftingInput nine = CraftingInput.of(3, 3, inputs);
            require(helper, pack.matches(nine, helper.getLevel()), "九粒未匹配对应锭: " + metal);
            ItemStack packed = pack.assemble(nine, helper.getLevel().registryAccess());
            require(helper, packed.is(ingot) && packed.getCount() == 1, "九粒未得到一锭: " + metal);
            inputs.set(8, ItemStack.EMPTY);
            require(helper, !pack.matches(CraftingInput.of(3, 3, inputs), helper.getLevel()),
                    "八粒也得到锭: " + metal);
            inputs.set(8, new ItemStack(otherNugget));
            require(helper, !pack.matches(CraftingInput.of(3, 3, inputs), helper.getLevel()),
                    "混合金属粒也得到锭: " + metal);
            CraftingInput one = CraftingInput.of(1, 1, List.of(packed));
            require(helper, unpack.matches(one, helper.getLevel())
                            && !unpack.matches(CraftingInput.of(1, 1, List.of(new ItemStack(otherIngot))), helper.getLevel()),
                    "拆解接受错误金属或拒绝对应锭: " + metal);
            ItemStack unpacked = unpack.assemble(one, helper.getLevel().registryAccess());
            require(helper, unpacked.is(nugget) && unpacked.getCount() == 9,
                    "一锭拆解未还原九粒: " + metal);
        }
        helper.succeed();
    }

    private static Item registered(GameTestHelper helper, String path) {
        ResourceLocation key = id(path);
        Item item = BuiltInRegistries.ITEM.get(key);
        require(helper, item != Items.AIR && BuiltInRegistries.ITEM.getKey(item).equals(key), "缺少物品注册: " + key);
        return item;
    }

    private static Item crushed(String metal) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_" + metal));
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, path);
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
