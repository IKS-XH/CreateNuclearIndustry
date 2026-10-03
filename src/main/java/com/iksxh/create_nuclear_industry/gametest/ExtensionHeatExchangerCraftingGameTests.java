package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingInput;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** 核换热器材料配方的原生匹配、实际取料和精确产量合同。 */
@GameTestHolder("create_nuclear_industry_heat_exchanger")
@PrefixGameTestTemplate(false)
public final class ExtensionHeatExchangerCraftingGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
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

    /** 工作台配方在实际匹配器中按布局、输入身份及精确结果数量执行。 */
    @GameTest(template = TEMPLATE)
    public static void workbenchRecipesMatchExactLayoutsAndOutputs(GameTestHelper helper) {
        Item sturdySheet = createItem("sturdy_sheet");
        Item steelPlate = taggedItem("c:plates/steel");
        Item copperPlate = taggedItem("c:plates/copper");
        Item precisionMechanism = createItem("precision_mechanism");
        Item pipeBlank = registered(helper, "steel_pipe_blank");
        Item reinforcedPlate = registered(helper, "reinforced_steel_plate");
        Item bundle = registered(helper, "nuclear_heat_exchange_bundle");

        assertCraft(helper, "heat_exchanger/reinforced_steel_plate", 1, 3, List.of(
                new ItemStack(sturdySheet), new ItemStack(precisionMechanism), new ItemStack(steelPlate)),
                reinforcedPlate, 1);
        assertCraft(helper, "heat_exchanger/nuclear_heat_exchange_bundle", 3, 3, List.of(
                new ItemStack(pipeBlank), ItemStack.EMPTY, new ItemStack(pipeBlank),
                new ItemStack(copperPlate), new ItemStack(reinforcedPlate), new ItemStack(copperPlate),
                new ItemStack(pipeBlank), ItemStack.EMPTY, new ItemStack(pipeBlank)), bundle, 1);
        helper.succeed();
    }

    /** 21格Create机械合成匹配指定形状、12钢板及两份管束，并且仅产一台整机。 */
    @GameTest(template = TEMPLATE)
    public static void mechanicalCraftingMatchesTwentyOneSlotsAndOneOutput(GameTestHelper helper) {
        var holder = helper.getLevel().getRecipeManager().byKey(id("heat_exchanger/nuclear_heat_exchanger"))
                .orElseThrow();
        require(helper, holder.value() instanceof MechanicalCraftingRecipe,
                "整机不是Create原生机械合成配方");
        MechanicalCraftingRecipe recipe = (MechanicalCraftingRecipe) holder.value();
        Item steel = taggedItem("c:plates/steel");
        Item copper = taggedItem("c:plates/copper");
        Item fitting = registered(helper, "pressure_fitting");
        Item bundle = registered(helper, "nuclear_heat_exchange_bundle");
        Item sensor = registered(helper, "industrial_sensor");
        List<ItemStack> inputs = mechanicalInputs(steel, copper, fitting, bundle, sensor);
        RecipeGridHandler.GroupedItems grouped = groupedItems(helper, inputs);
        grouped.calcStats();
        CraftingInput craftingInput = MechanicalCraftingInput.of(grouped);
        ItemStack output = recipe.getResultItem(helper.getLevel().registryAccess());
        require(helper, recipe.getWidth() == 5 && recipe.getHeight() == 5 && !recipe.acceptsMirrored()
                        && recipe.matches(craftingInput, helper.getLevel())
                        && output.is(registered(helper, "nuclear_heat_exchanger"))
                        && output.getCount() == 1,
                "整机机械合成尺寸、方向、材料数或结果数量不符");
        require(helper, recipe.assemble(craftingInput, helper.getLevel().registryAccess())
                        .is(registered(helper, "nuclear_heat_exchanger")),
                "Create原生21格输入匹配后未组装出核换热器");
        require(helper, inputs.stream().filter(stack -> stack.is(steel)).mapToInt(ItemStack::getCount).sum() == 12
                        && inputs.stream().filter(stack -> stack.is(copper)).mapToInt(ItemStack::getCount).sum() == 4
                        && inputs.stream().filter(stack -> stack.is(fitting)).mapToInt(ItemStack::getCount).sum() == 2
                        && inputs.stream().filter(stack -> stack.is(bundle)).mapToInt(ItemStack::getCount).sum() == 2
                        && inputs.stream().filter(stack -> stack.is(sensor)).mapToInt(ItemStack::getCount).sum() == 1,
                "整机21格配方各材料数量错误");
        helper.succeed();
    }

    private static RecipeGridHandler.GroupedItems groupedItems(GameTestHelper helper, List<ItemStack> inputs) {
        CompoundTag nbt = new CompoundTag();
        ListTag grid = new ListTag();
        for (int index = 0; index < inputs.size(); index++) {
            ItemStack stack = inputs.get(index);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", index % 5);
            entry.putInt("y", 4 - index / 5);
            entry.put("item", stack.saveOptional(helper.getLevel().registryAccess()));
            grid.add(entry);
        }
        nbt.put("Grid", grid);
        // Create原生read只恢复格子；匹配前的尺寸统计由tryToApplyRecipe负责。
        return RecipeGridHandler.GroupedItems.read(nbt, helper.getLevel().registryAccess());
    }

    private static DepotBlockEntity depot(GameTestHelper helper) {
        return (DepotBlockEntity) helper.getBlockEntity(SAW_OUTPUT);
    }

    private static List<ItemStack> mechanicalInputs(Item steel, Item copper, Item fitting, Item bundle, Item sensor) {
        return new ArrayList<>(List.of(
                ItemStack.EMPTY, new ItemStack(steel), new ItemStack(steel), new ItemStack(steel), ItemStack.EMPTY,
                new ItemStack(steel), new ItemStack(copper), new ItemStack(fitting), new ItemStack(copper), new ItemStack(steel),
                new ItemStack(steel), new ItemStack(bundle), new ItemStack(sensor), new ItemStack(bundle), new ItemStack(steel),
                new ItemStack(steel), new ItemStack(copper), new ItemStack(fitting), new ItemStack(copper), new ItemStack(steel),
                ItemStack.EMPTY, new ItemStack(steel), new ItemStack(steel), new ItemStack(steel), ItemStack.EMPTY));
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
