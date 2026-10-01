package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.OreContent;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

import java.util.List;

/**
 * 基础金属材料的服务端集成验证：使用已加载配方、实际熔炉 tick 与 Create 压片机库存路径。
 * 测试标签扩展只在隔离的测试数据包与 JVM 开关下启用，不改变正式服务器的数据包。
 */
@GameTestHolder(CreateNuclearIndustry.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExtensionBasicMaterialGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos FURNACE = new BlockPos(2, 2, 2);
    private static final BlockPos DEPOT = new BlockPos(2, 1, 2);
    private static final BlockPos PRESS = DEPOT.above(2);
    private static final BlockPos MOTOR = PRESS.west();

    private ExtensionBasicMaterialGameTests() {}

    /** 检查正式物品身份、绑定标签与六条服务端配方，负例只针对本批配方本身。 */
    @GameTest(template = TEMPLATE)
    public static void loadedRecipesAcceptOnlyTheirMetalForms(GameTestHelper helper) {
        for (String metal : List.of("lead", "tin")) {
            Item raw = metal.equals("lead") ? OreContent.LEAD.raw().get() : OreContent.TIN.raw().get();
            Item otherRaw = metal.equals("lead") ? OreContent.TIN.raw().get() : OreContent.LEAD.raw().get();
            Item ingot = registered(helper, metal + "_ingot");
            Item plate = registered(helper, metal + "_plate");
            require(helper, ingot.getDefaultMaxStackSize() == 64 && plate.getDefaultMaxStackSize() == 64,
                    "锭或板不是普通可堆叠物品: " + metal);
            require(helper, new ItemStack(ingot).is(itemTag("ingots/" + metal))
                            && new ItemStack(ingot).is(itemTag("ingots")), "锭的细分/汇总标签未绑定: " + metal);
            require(helper, new ItemStack(plate).is(itemTag("plates/" + metal))
                            && new ItemStack(plate).is(itemTag("plates")), "板的细分/汇总标签未绑定: " + metal);

            for (boolean blast : List.of(false, true)) {
                String method = blast ? "blasting" : "smelting";
                ResourceLocation id = id(method + "/" + metal + "_ingot_from_raw_" + metal);
                var holder = helper.getLevel().getRecipeManager().byKey(id).orElseThrow();
                require(helper, holder.value() instanceof AbstractCookingRecipe, "加载的不是原生 cooking 配方: " + id);
                AbstractCookingRecipe recipe = (AbstractCookingRecipe) holder.value();
                require(helper, recipe.getType() == (blast ? RecipeType.BLASTING : RecipeType.SMELTING), "配方机器类型错误: " + id);
                require(helper, recipe.getCookingTime() == (blast ? 100 : 200) && recipe.getExperience() == 0.7f,
                        "时长或经验不符: " + id);
                require(helper, recipe.matches(new SingleRecipeInput(new ItemStack(raw)), helper.getLevel()), "对应粗矿未匹配: " + id);
                for (Item wrong : List.of(otherRaw, metal.equals("lead") ? OreContent.LEAD.rawBlock().get().asItem()
                                : OreContent.TIN.rawBlock().get().asItem(),
                        Items.RAW_IRON, Items.RAW_COPPER, OreContent.URANIUM.raw().get(), crushed(metal))) {
                    require(helper, !recipe.matches(new SingleRecipeInput(new ItemStack(wrong)), helper.getLevel()),
                            "错误形态被本批粗矿配方接受: " + id + " item=" + wrong);
                }
                if (!new ItemStack(Items.FLINT).is(itemTag("raw_materials/lead")) || !metal.equals("lead")) {
                    require(helper, !recipe.matches(new SingleRecipeInput(new ItemStack(Items.FLINT)), helper.getLevel()),
                            "非等价粗矿被本批配方接受: " + id);
                }
                ItemStack result = recipe.getResultItem(helper.getLevel().registryAccess());
                require(helper, result.is(ingot) && result.getCount() == 1, "锭输出身份或数量错误: " + id);
            }

            ResourceLocation pressingId = id("pressing/" + metal + "_plate");
            var pressing = helper.getLevel().getRecipeManager().byKey(pressingId).orElseThrow();
            require(helper, pressing.value() instanceof PressingRecipe, "加载的不是 Create 压片配方: " + pressingId);
            PressingRecipe recipe = (PressingRecipe) pressing.value();
            require(helper, recipe.getIngredients().size() == 1
                            && recipe.getIngredients().getFirst().test(new ItemStack(ingot))
                            && !recipe.getIngredients().getFirst().test(new ItemStack(Items.IRON_INGOT))
                            && !recipe.getIngredients().getFirst().test(new ItemStack(OreContent.URANIUM.raw().get())),
                    "压片输入标签或拒绝规则错误: " + pressingId);
            require(helper, recipe.getRollableResults().size() == 1
                            && recipe.getRollableResults().getFirst().getStack().is(plate)
                            && recipe.getRollableResults().getFirst().getStack().getCount() == 1
                            && recipe.getRollableResults().getFirst().getChance() == 1,
                    "压片产物身份、数量或副产物错误: " + pressingId);
        }
        helper.succeed();
    }

    /** 对隔离测试包的等价成员执行真实已加载配方匹配；默认运行明确拒绝替代物。 */
    @GameTest(template = TEMPLATE)
    public static void externalTagMembersFollowReloadedRecipes(GameTestHelper helper) {
        String mode = System.getProperty("cni.material.externalTags", "false");
        Item rawSurrogate = Items.FLINT;
        Item ingotSurrogate = Items.CLAY_BALL;
        boolean external = mode.equals("dynamic")
                ? new ItemStack(rawSurrogate).is(itemTag("raw_materials/lead"))
                : Boolean.parseBoolean(mode);
        require(helper, new ItemStack(rawSurrogate).is(itemTag("raw_materials/lead")) == external,
                "隔离数据包的粗矿标签状态与测试模式不符");
        require(helper, new ItemStack(ingotSurrogate).is(itemTag("ingots/lead")) == external,
                "隔离数据包的锭标签状态与测试模式不符");
        LOGGER.info("MATERIAL_TAG_RELOAD_CHECK mode={} external={} raw={} ingot={}", mode, external,
                BuiltInRegistries.ITEM.getKey(rawSurrogate), BuiltInRegistries.ITEM.getKey(ingotSurrogate));
        for (String method : List.of("smelting", "blasting")) {
            AbstractCookingRecipe recipe = (AbstractCookingRecipe) helper.getLevel().getRecipeManager()
                    .byKey(id(method + "/lead_ingot_from_raw_lead")).orElseThrow().value();
            require(helper, recipe.matches(new SingleRecipeInput(new ItemStack(rawSurrogate)), helper.getLevel()) == external,
                    "外部等价粗矿未按加载后的标签匹配: " + method);
            require(helper, recipe.getResultItem(helper.getLevel().registryAccess()).is(registered(helper, "lead_ingot")),
                    "等价粗矿改变了固定锭输出: " + method);
        }
        setupPress(helper);
        helper.runAfterDelay(5, () -> {
            MechanicalPressBlockEntity press = (MechanicalPressBlockEntity) helper.getBlockEntity(PRESS);
            var recipe = press.getRecipe(new ItemStack(ingotSurrogate));
            require(helper, recipe.isPresent() == external,
                    "外部等价锭未按加载后的标签匹配");
            if (external) require(helper, recipe.orElseThrow().value().getResultItem(helper.getLevel().registryAccess())
                    .is(registered(helper, "lead_plate")), "等价锭改变了固定板输出");
            LOGGER.info("MATERIAL_TAG_RELOAD_RESULT mode={} external={} matchedExpected=true", mode, external);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void leadSmeltsInRealFurnace(GameTestHelper helper) {
        cook(helper, "lead", false);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void tinSmeltsInRealFurnace(GameTestHelper helper) {
        cook(helper, "tin", false);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void leadBlastsInRealBlastFurnace(GameTestHelper helper) {
        cook(helper, "lead", true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void tinBlastsInRealBlastFurnace(GameTestHelper helper) {
        cook(helper, "tin", true);
    }

    /** 输出槽占用时不可消费粗矿；解除堵塞后同一件输入只能得到一件锭。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 500)
    public static void blockedFurnacePreservesMaterialAndResumes(GameTestHelper helper) {
        helper.setBlock(FURNACE, Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) helper.getBlockEntity(FURNACE);
        furnace.setItem(2, new ItemStack(Items.COBBLESTONE));
        furnace.setItem(0, new ItemStack(OreContent.LEAD.raw().get()));
        furnace.setItem(1, new ItemStack(Items.COAL));
        helper.runAfterDelay(240, () -> {
            require(helper, furnace.getItem(0).is(OreContent.LEAD.raw().get()) && furnace.getItem(0).getCount() == 1,
                    "输出受阻时粗矿丢失");
            require(helper, furnace.getItem(2).is(Items.COBBLESTONE) && furnace.getItem(2).getCount() == 1,
                    "输出受阻时覆盖了原物品");
            furnace.setItem(2, ItemStack.EMPTY);
        });
        helper.runAfterDelay(445, () -> {
            require(helper, furnace.getItem(0).isEmpty(), "解除阻塞后未消费粗矿");
            require(helper, furnace.getItem(2).is(registered(helper, "lead_ingot"))
                            && furnace.getItem(2).getCount() == 1, "解除阻塞后产量不守恒");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void leadPressRunsOnlyWithPower(GameTestHelper helper) {
        pressWithPowerRecovery(helper, "lead");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void tinPressRunsOnlyWithPower(GameTestHelper helper) {
        pressWithPowerRecovery(helper, "tin");
    }

    private static void cook(GameTestHelper helper, String metal, boolean blast) {
        helper.setBlock(FURNACE, blast ? Blocks.BLAST_FURNACE : Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) helper.getBlockEntity(FURNACE);
        Item raw = metal.equals("lead") ? OreContent.LEAD.raw().get() : OreContent.TIN.raw().get();
        furnace.setItem(0, new ItemStack(raw));
        furnace.setItem(1, new ItemStack(Items.COAL));
        int duration = blast ? 100 : 200;
        helper.runAfterDelay(duration - 2, () -> {
            require(helper, furnace.getItem(0).is(raw) && furnace.getItem(0).getCount() == 1,
                    "原生炉在配方时长前消费了输入: " + metal);
            require(helper, furnace.getItem(2).isEmpty(), "原生炉提前产出: " + metal);
        });
        helper.runAfterDelay(duration + 3, () -> {
            require(helper, furnace.getItem(0).isEmpty(), "原生炉到时未消费粗矿: " + metal);
            require(helper, furnace.getItem(2).is(registered(helper, metal + "_ingot"))
                            && furnace.getItem(2).getCount() == 1, "原生炉产物身份/数量错误: " + metal);
            helper.succeed();
        });
    }

    private static void pressWithPowerRecovery(GameTestHelper helper, String metal) {
        setupPress(helper);
        Item ingot = registered(helper, metal + "_ingot");
        Item plate = registered(helper, metal + "_plate");
        helper.runAfterDelay(5, () -> {
            var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(DEPOT), Direction.UP);
            require(helper, handler != null && handler.insertItem(0, new ItemStack(ingot), false).isEmpty(),
                    "置物台 capability 拒绝锭: " + metal);
        });
        helper.runAfterDelay(45, () -> {
            DepotBlockEntity depot = (DepotBlockEntity) helper.getBlockEntity(DEPOT);
            require(helper, depot.getHeldItem().is(ingot) && depot.getHeldItem().getCount() == 1,
                    "无动力时压片机意外加工或丢料: " + metal);
            helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
        });
        helper.runAfterDelay(50, () -> {
            CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR);
            motor.generatedSpeed.setValue(256);
        });
        helper.runAfterDelay(150, () -> {
            MechanicalPressBlockEntity press = (MechanicalPressBlockEntity) helper.getBlockEntity(PRESS);
            DepotBlockEntity depot = (DepotBlockEntity) helper.getBlockEntity(DEPOT);
            require(helper, press.getSpeed() != 0, "压片机未获得真实动力: " + metal);
            require(helper, depot.getHeldItem().is(plate) && depot.getHeldItem().getCount() == 1,
                    "真实置物台未得到对应板或产量错误: " + metal);
            helper.succeed();
        });
    }

    private static void setupPress(GameTestHelper helper) {
        helper.setBlock(DEPOT, AllBlocks.DEPOT.get());
        helper.setBlock(PRESS, AllBlocks.MECHANICAL_PRESS.getDefaultState()
                .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
    }

    private static Item registered(GameTestHelper helper, String path) {
        ResourceLocation id = id(path);
        Item item = BuiltInRegistries.ITEM.get(id);
        require(helper, item != Items.AIR && BuiltInRegistries.ITEM.getKey(item).equals(id), "缺少物品注册: " + id);
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
