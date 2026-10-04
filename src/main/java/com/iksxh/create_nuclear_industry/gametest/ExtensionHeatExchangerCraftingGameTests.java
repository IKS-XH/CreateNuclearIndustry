package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.HeatMaterialsContent;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** 核换热器材料配方的原生匹配、实际取料和精确产量合同。 */
@GameTestHolder("create_nuclear_industry_heat_exchanger")
@PrefixGameTestTemplate(false)
public final class ExtensionHeatExchangerCraftingGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos DEPOT = new BlockPos(2, 1, 2);
    private static final BlockPos OPERATOR = DEPOT.above(2);
    private static final BlockPos MOTOR = OPERATOR.west();
    private static final BlockPos SAW = new BlockPos(2, 2, 2);
    private static final BlockPos SAW_OUTPUT = SAW.north();
    private static final BlockPos SAW_MOTOR = SAW.west();

    private ExtensionHeatExchangerCraftingGameTests() {
    }

    /** 注册身份与新材料标签按父子关系加载，强化板不混入普通钢板子标签。 */
    @GameTest(template = TEMPLATE)
    public static void materialItemsAndTagsAreRegistered(GameTestHelper helper) {
        Item pipeBlank = registered(helper, "steel_pipe_blank");
        Item reinforcedPlate = registered(helper, "reinforced_steel_plate");
        Item bundle = registered(helper, "nuclear_heat_exchange_bundle");
        require(helper, new ItemStack(pipeBlank).is(itemTag("tubes/steel"))
                        && new ItemStack(pipeBlank).is(itemTag("tubes")),
                "钢管坯未进入钢管子标签及父标签");
        require(helper, new ItemStack(reinforcedPlate).is(itemTag("plates/reinforced_steel"))
                        && new ItemStack(reinforcedPlate).is(itemTag("plates"))
                        && !new ItemStack(reinforcedPlate).is(itemTag("plates/steel")),
                "强化钢板标签层级错误或混入普通钢板");
        require(helper, new ItemStack(bundle).is(modTag("nuclear_heat_exchange_bundles")),
                "核换热管束未进入专用标签");
        require(helper, new ItemStack(registered(helper, "pressure_fitting")).is(modTag("pressure_fittings"))
                        && new ItemStack(registered(helper, "industrial_sensor")).is(modTag("industrial_sensors")),
                "耐压接头或工业传感器未进入其专用标签");
        helper.succeed();
    }

    /** 原版切石配方匹配钢锭、拒绝铁锭，并在切石菜单实际扣一锭交付两根管坯。 */
    @GameTest(template = TEMPLATE)
    public static void stonecutterActuallyProducesTwoPipeBlanks(GameTestHelper helper) {
        var holder = helper.getLevel().getRecipeManager().byKey(id("heat_exchanger/steel_pipe_blank")).orElseThrow();
        require(helper, holder.value() instanceof StonecutterRecipe, "钢管坯不是原版切石配方");
        StonecutterRecipe recipe = (StonecutterRecipe) holder.value();
        Item output = registered(helper, "steel_pipe_blank");
        Item steelIngot = taggedItem("c:ingots/steel");
        require(helper, recipe.matches(new SingleRecipeInput(new ItemStack(steelIngot)), helper.getLevel())
                        && !recipe.matches(new SingleRecipeInput(new ItemStack(Items.IRON_INGOT)), helper.getLevel())
                        && recipe.getResultItem(helper.getLevel().registryAccess()).is(output)
                        && recipe.getResultItem(helper.getLevel().registryAccess()).getCount() == 2,
                "切石输入或两根管坯产量不符");

        BlockPos cutterPos = new BlockPos(2, 1, 2);
        helper.setBlock(cutterPos, Blocks.STONECUTTER);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        StonecutterMenu menu = new StonecutterMenu(0, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(cutterPos)));
        menu.getSlot(0).set(new ItemStack(steelIngot));
        boolean selected = false;
        for (int recipeIndex = 0; recipeIndex < menu.getNumRecipes(); recipeIndex++) {
            if (menu.clickMenuButton(player, recipeIndex) && menu.getSlot(1).getItem().is(output)) {
                selected = true;
                break;
            }
        }
        require(helper, selected,
                "原版切石菜单未提供钢管坯选项");
        ItemStack result = menu.getSlot(1).getItem().copy();
        require(helper, result.is(output) && result.getCount() == 2, "切石菜单展示产量不是两根管坯");
        menu.getSlot(1).onTake(player, result);
        require(helper, menu.getSlot(0).getItem().isEmpty(), "切石取料没有扣除一枚钢锭");
        helper.succeed();
    }

    /** Create机械锯按锁定版本原生设置兼容切石配方，并验证真实动力加工及产量。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void createSawUsesNativeStonecuttingFallback(GameTestHelper helper) {
        helper.setBlock(SAW, AllBlocks.MECHANICAL_SAW.getDefaultState()
                .setValue(SawBlock.FACING, Direction.UP)
                .setValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        helper.setBlock(SAW_MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.EAST));
        helper.setBlock(SAW_OUTPUT, AllBlocks.DEPOT.get());
        helper.runAfterDelay(5, () -> {
            ((CreativeMotorBlockEntity) helper.getBlockEntity(SAW_MOTOR)).generatedSpeed.setValue(256);
            require(helper, ((SawBlockEntity) helper.getBlockEntity(SAW)).getSpeed() != 0,
                    "真实机械锯未获得轴动力");
            var output = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(SAW_OUTPUT), Direction.UP);
            require(helper, output != null && output.insertItem(0, new ItemStack(Items.COBBLESTONE), false).isEmpty(),
                    "机械锯输出置物台无法设置阻塞物");
            var input = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(SAW), Direction.UP);
            require(helper, input != null && input.insertItem(0,
                    new ItemStack(taggedItem("c:ingots/steel")), false).isEmpty(),
                    "真实机械锯拒绝钢锭输入");
        });
        helper.runAfterDelay(100, () -> {
            SawBlockEntity saw = (SawBlockEntity) helper.getBlockEntity(SAW);
            if (AllConfigs.server().recipes.allowStonecuttingOnSaw.get()) {
                require(helper, saw.inventory.getStackInSlot(0).isEmpty()
                                && saw.inventory.getStackInSlot(1).is(registered(helper, "steel_pipe_blank"))
                                && saw.inventory.getStackInSlot(1).getCount() == 2
                                && depot(helper).getHeldItem().is(Items.COBBLESTONE),
                        "机械锯没有按原生切石回退把一钢锭加工成两根管坯");
                depot(helper).setHeldItem(ItemStack.EMPTY);
            } else {
                require(helper, saw.inventory.getStackInSlot(0).is(taggedItem("c:ingots/steel"))
                                && saw.inventory.getStackInSlot(1).isEmpty()
                                && depot(helper).getHeldItem().is(Items.COBBLESTONE),
                        "关闭Create原生切石开关后机械锯仍加工钢锭");
            }
        });
        helper.runAfterDelay(220, () -> {
            SawBlockEntity saw = (SawBlockEntity) helper.getBlockEntity(SAW);
            if (AllConfigs.server().recipes.allowStonecuttingOnSaw.get()) {
                require(helper, depot(helper).getHeldItem().is(registered(helper, "steel_pipe_blank"))
                                && depot(helper).getHeldItem().getCount() == 2 && saw.inventory.isEmpty(),
                        "机械锯解除堵塞后没有转交两根管坯");
            } else {
                require(helper, depot(helper).getHeldItem().is(Items.COBBLESTONE)
                                && saw.inventory.getStackInSlot(0).is(taggedItem("c:ingots/steel"))
                                && saw.inventory.getStackInSlot(1).isEmpty(),
                        "关闭原生切石开关后机械锯改变了钢锭或覆盖了堵塞物");
            }
            helper.succeed();
        });
    }

    /** 原强化钢板工作台ID现承载序列装配，核对基底、投入顺序、末步压片与唯一结果。 */
    @GameTest(template = TEMPLATE)
    public static void reinforcedPlateRecipeReplacesWorkbenchRoute(GameTestHelper helper) {
        var holder = helper.getLevel().getRecipeManager().byKey(id("heat_exchanger/reinforced_steel_plate"))
                .orElseThrow();
        require(helper, holder.value() instanceof SequencedAssemblyRecipe,
                "旧强化钢板工作台配方ID未替换为Create序列装配");
        SequencedAssemblyRecipe recipe = (SequencedAssemblyRecipe) holder.value();
        Item steelPlate = taggedItem("c:plates/steel");
        Item incomplete = registered(helper, "incomplete_reinforced_steel_plate");
        require(helper, recipe.getLoops() == 1
                        && recipe.getIngredient().test(new ItemStack(steelPlate))
                        && !recipe.getIngredient().test(new ItemStack(Items.IRON_INGOT))
                        && recipe.getTransitionalItem().is(incomplete)
                        && recipe.getResultItem(helper.getLevel().registryAccess())
                        .is(registered(helper, "reinforced_steel_plate"))
                        && recipe.getResultItem(helper.getLevel().registryAccess()).getCount() == 1
                        && recipe.getOutputChance() == 1f,
                "强化钢板基底、半成品、轮数或唯一结果不符");
        require(helper, recipe.getSequence().size() == 3, "强化钢板工序不是两次机械手加一次压片");
        var first = recipe.getSequence().get(0).getRecipe();
        var second = recipe.getSequence().get(1).getRecipe();
        var last = recipe.getSequence().get(2).getRecipe();
        require(helper, first instanceof DeployerApplicationRecipe && second instanceof DeployerApplicationRecipe
                        && last instanceof PressingRecipe
                        && first.getIngredients().get(1).test(new ItemStack(createItem("sturdy_sheet")))
                        && second.getIngredients().get(1).test(new ItemStack(createItem("precision_mechanism"))),
                "机械手耗材顺序或末步压片不符");
        helper.succeed();
    }

    /** 核换热管束工作台布局保持原有输入、输出和产量合同。 */
    @GameTest(template = TEMPLATE)
    public static void workbenchRecipesMatchExactLayoutsAndOutputs(GameTestHelper helper) {
        Item copperPlate = taggedItem("c:plates/copper");
        Item pipeBlank = registered(helper, "steel_pipe_blank");
        Item reinforcedPlate = registered(helper, "reinforced_steel_plate");
        Item bundle = registered(helper, "nuclear_heat_exchange_bundle");

        assertCraft(helper, "heat_exchanger/nuclear_heat_exchange_bundle", 3, 3, List.of(
                new ItemStack(pipeBlank), ItemStack.EMPTY, new ItemStack(pipeBlank),
                new ItemStack(copperPlate), new ItemStack(reinforcedPlate), new ItemStack(copperPlate),
                new ItemStack(pipeBlank), ItemStack.EMPTY, new ItemStack(pipeBlank)), bundle, 1);
        helper.succeed();
    }

    /** 真实机械手分步消耗坚固板、精密构件，真实压片机一轮产一块强化钢板。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 530)
    public static void realReinforcedPlateAssemblyConsumesEveryStep(GameTestHelper helper) {
        require(helper, HeatMaterialsContent.INCOMPLETE_REINFORCED_STEEL_PLATE.get() instanceof SequencedAssemblyItem,
                "半成品未注册为Create序列装配物品");
        setupDeployerDepot(helper);
        helper.runAfterDelay(5, () -> {
            putOnAssemblyDepot(helper, new ItemStack(registered(helper, "steel_plate")));
            hand(helper, new ItemStack(createItem("sturdy_sheet")));
            assemblyPower(helper, 256);
        });
        helper.runAfterDelay(110, () -> {
            assertAssemblyInterim(helper, 1, 1f / 3f);
            require(helper, handCount(helper) == 0, "第一步未精确消耗一块坚固板");
            hand(helper, new ItemStack(createItem("precision_mechanism")));
        });
        helper.runAfterDelay(220, () -> {
            assertAssemblyInterim(helper, 2, 2f / 3f);
            require(helper, handCount(helper) == 0, "第二步未精确消耗一个精密构件");
            setupPress(helper);
        });
        helper.runAfterDelay(390, () -> {
            ItemStack result = assemblyDepot(helper).getHeldItem();
            require(helper, result.is(registered(helper, "reinforced_steel_plate")) && result.getCount() == 1,
                    "真实压片未在一轮内产出唯一强化钢板");
            helper.succeed();
        });
    }

    private static void setupDeployerDepot(GameTestHelper helper) {
        helper.setBlock(DEPOT, AllBlocks.DEPOT.get());
        helper.setBlock(OPERATOR, AllBlocks.DEPLOYER.getDefaultState()
                .setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.EAST));
    }

    private static void setupPress(GameTestHelper helper) {
        helper.setBlock(OPERATOR, AllBlocks.MECHANICAL_PRESS.getDefaultState()
                .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        assemblyPower(helper, 256);
    }

    private static void putOnAssemblyDepot(GameTestHelper helper, ItemStack stack) {
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(DEPOT), Direction.UP);
        require(helper, handler != null && handler.insertItem(0, stack, false).isEmpty(), "置物台拒收钢板基底");
    }

    private static DepotBlockEntity assemblyDepot(GameTestHelper helper) {
        return (DepotBlockEntity) helper.getBlockEntity(DEPOT);
    }

    private static void hand(GameTestHelper helper, ItemStack stack) {
        ((DeployerBlockEntity) helper.getBlockEntity(OPERATOR)).getPlayer()
                .setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    private static int handCount(GameTestHelper helper) {
        return ((DeployerBlockEntity) helper.getBlockEntity(OPERATOR)).getPlayer().getMainHandItem().getCount();
    }

    private static void assemblyPower(GameTestHelper helper, int speed) {
        ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(speed);
    }

    private static void assertAssemblyInterim(GameTestHelper helper, int step, float expectedProgress) {
        ItemStack stack = assemblyDepot(helper).getHeldItem();
        var component = stack.get(AllDataComponents.SEQUENCED_ASSEMBLY);
        require(helper, stack.is(HeatMaterialsContent.INCOMPLETE_REINFORCED_STEEL_PLATE.get())
                        && stack.getCount() == 1 && component != null
                        && component.id().equals(id("heat_exchanger/reinforced_steel_plate"))
                        && component.step() == step && Math.abs(component.progress() - expectedProgress) < .001f,
                "半成品身份或原生序列进度不符: step=" + step);
    }

    /** 核换热器使用用户指定的3×3工作台布局，核对原生类型、材料匹配与单件产量。 */
    @GameTest(template = TEMPLATE)
    public static void nuclearHeatExchangerMatchesThreeByThreeWorkbenchRecipe(GameTestHelper helper) {
        var holder = helper.getLevel().getRecipeManager().byKey(id("heat_exchanger/nuclear_heat_exchanger"))
                .orElseThrow();
        require(helper, holder.value() instanceof CraftingRecipe
                        && holder.value().getType() == RecipeType.CRAFTING
                        && !(holder.value() instanceof MechanicalCraftingRecipe),
                "整机没有替换为原版有序工作台配方");
        Item steel = taggedItem("c:plates/steel");
        Item copper = taggedItem("c:plates/copper");
        Item bundle = registered(helper, "nuclear_heat_exchange_bundle");
        assertCraft(helper, "heat_exchanger/nuclear_heat_exchanger", 3, 3, List.of(
                new ItemStack(copper), new ItemStack(copper), new ItemStack(copper),
                new ItemStack(steel), new ItemStack(bundle), new ItemStack(steel),
                new ItemStack(steel), new ItemStack(steel), new ItemStack(steel)),
                registered(helper, "nuclear_heat_exchanger"), 1);
        helper.succeed();
    }

    private static DepotBlockEntity depot(GameTestHelper helper) {
        return (DepotBlockEntity) helper.getBlockEntity(SAW_OUTPUT);
    }

    private static void assertCraft(GameTestHelper helper, String path, int width, int height,
                                    List<ItemStack> inputs, Item output, int count) {
        var holder = helper.getLevel().getRecipeManager().byKey(id(path)).orElseThrow();
        require(helper, holder.value() instanceof CraftingRecipe, "不是原生工作台配方: " + path);
        CraftingRecipe recipe = (CraftingRecipe) holder.value();
        CraftingInput input = CraftingInput.of(width, height, inputs);
        ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
        require(helper, recipe.matches(input, helper.getLevel()) && result.is(output) && result.getCount() == count,
                "工作台配方布局、输入或产物数量不符: " + path);
    }

    private static Item registered(GameTestHelper helper, String path) {
        ResourceLocation key = id(path);
        Item item = BuiltInRegistries.ITEM.get(key);
        require(helper, item != Items.AIR && BuiltInRegistries.ITEM.getKey(item).equals(key), "物品身份未注册: " + key);
        return item;
    }

    private static Item createItem(String path) {
        ResourceLocation key = ResourceLocation.fromNamespaceAndPath("create", path);
        Item item = BuiltInRegistries.ITEM.get(key);
        if (item == Items.AIR || !BuiltInRegistries.ITEM.getKey(item).equals(key))
            throw new IllegalStateException("Create物品身份未注册: " + key);
        return item;
    }

    private static Item taggedItem(String tagId) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
        return BuiltInRegistries.ITEM.stream().filter(item -> new ItemStack(item).is(tag)).findFirst()
                .orElseThrow(() -> new IllegalStateException("物品标签没有成员: " + tagId));
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static TagKey<Item> modTag(String path) {
        return TagKey.create(Registries.ITEM, id(path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, path);
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
