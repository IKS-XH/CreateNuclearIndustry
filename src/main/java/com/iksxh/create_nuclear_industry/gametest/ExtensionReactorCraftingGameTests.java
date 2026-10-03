package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition.ColumnType;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition.LocalPosition;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 验证固定实验堆配方的数据匹配、原生加工配置和既有正式结构身份。 */
@GameTestHolder("create_nuclear_industry_reactor_crafting")
@PrefixGameTestTemplate(false)
public final class ExtensionReactorCraftingGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos BASIN = new BlockPos(2, 1, 2);
    private static final BlockPos MIXER = BASIN.above(2);
    private static final BlockPos COG = MIXER.west();
    private static final BlockPos MOTOR = COG.above();
    private static final BlockPos BURNER = BASIN.below();
    private static final BlockPos DEPOT = BASIN;
    private static final BlockPos DEPLOYER = DEPOT.above(2);
    private static final BlockPos DEPLOYER_MOTOR = DEPLOYER.west();

    private ExtensionReactorCraftingGameTests() {}

    /** 在真实配方管理器中匹配五种工作台配方并核对成品身份与数量。 */
    @GameTest(template = TEMPLATE)
    public static void workbenchRecipesMatchTheFixedReactorParts(GameTestHelper helper) {
        Item steel = registered(helper, "steel_plate");
        Item steelRod = registered(helper, "steel_rod");
        require(helper, new ItemStack(steelRod).is(itemTag("rods/steel"))
                        && new ItemStack(steelRod).is(itemTag("rods")),
                "钢杆未进入 c:rods/steel 与父标签");
        Item lead = registered(helper, "lead_plate");
        Item concrete = registered(helper, "shielding_concrete");
        Item casing = registered(helper, "reactor_casing");
        Item glass = registered(helper, "shielded_glass");
        Item fitting = registered(helper, "pressure_fitting");
        Item ring = registered(helper, "seal_ring");
        Item grille = registered(helper, "steel_grate");

        assertCraft(helper, "crafting/reactor/reactor_casing", 3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(steel), ItemStack.EMPTY,
                new ItemStack(lead), new ItemStack(concrete), new ItemStack(lead),
                ItemStack.EMPTY, new ItemStack(steel), ItemStack.EMPTY), "reactor_casing", 4);
        assertCraft(helper, "crafting/reactor/reactor_window", 2, 1,
                List.of(new ItemStack(casing), new ItemStack(glass)), "reactor_window", 1);
        assertCraft(helper, "crafting/reactor/reactor_cold_port", 2, 2,
                List.of(new ItemStack(casing), new ItemStack(fitting), new ItemStack(ring), new ItemStack(Items.BLUE_DYE)),
                "reactor_cold_port", 1);
        assertCraft(helper, "crafting/reactor/reactor_hot_port", 2, 2,
                List.of(new ItemStack(casing), new ItemStack(fitting), new ItemStack(ring), new ItemStack(Items.RED_DYE)),
                "reactor_hot_port", 1);
        assertCraft(helper, "crafting/reactor/reactor_fuel_rod", 1, 3,
                List.of(new ItemStack(steel), new ItemStack(grille), new ItemStack(steel)), "reactor_fuel_rod", 3);
        helper.succeed();
    }

    /** 确认两种陶瓷搅拌需要普通加热，屏蔽混凝土为无热搅拌且仅接受硬化混凝土标签。 */
    @GameTest(template = TEMPLATE)
    public static void ceramicAndConcreteMixingUseTheirDeclaredHeatAndInputs(GameTestHelper helper) {
        MixingRecipe industrial = mixing(helper, "industrial_ceramic");
        require(helper, industrial.getProcessingDuration() == 100
                        && industrial.getRequiredHeat() == HeatCondition.HEATED
                        && industrial.getIngredients().size() == 2
                        && industrial.getIngredients().get(0).test(new ItemStack(Items.QUARTZ))
                        && industrial.getIngredients().get(1).test(new ItemStack(Items.CLAY_BALL))
                        && result(helper, industrial.getRollableResults(), "industrial_ceramic", 2),
                "工业陶瓷搅拌输入、热级或产量错误");

        MixingRecipe neutron = mixing(helper, "neutron_absorbing_ceramic");
        require(helper, neutron.getProcessingDuration() == 100
                        && neutron.getRequiredHeat() == HeatCondition.HEATED
                        && neutron.getIngredients().size() == 3
                        && neutron.getIngredients().get(0).test(new ItemStack(registered(helper, "industrial_ceramic")))
                        && neutron.getIngredients().get(1).test(new ItemStack(registered(helper, "lead_nugget")))
                        && neutron.getIngredients().get(2).test(new ItemStack(Items.REDSTONE))
                        && result(helper, neutron.getRollableResults(), "neutron_absorbing_ceramic", 1),
                "中子吸收陶瓷搅拌输入、热级或产量错误");

        MixingRecipe concrete = mixing(helper, "shielding_concrete");
        require(helper, concrete.getProcessingDuration() == 100
                        && concrete.getRequiredHeat() == HeatCondition.NONE
                        && concrete.getIngredients().size() == 2
                        && concrete.getIngredients().get(0).test(new ItemStack(Items.WHITE_CONCRETE))
                        && !concrete.getIngredients().get(0).test(new ItemStack(Items.WHITE_CONCRETE_POWDER))
                        && concrete.getIngredients().get(1).test(new ItemStack(registered(helper, "lead_nugget")))
                        && result(helper, concrete.getRollableResults(), "shielding_concrete", 1),
                "屏蔽混凝土搅拌输入、无热要求或结果错误");
        helper.succeed();
    }

    /** 真实搅拌机无热时保留陶瓷原料，加入普通燃料后只产两份工业陶瓷。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 430)
    public static void realHeatedMixerProducesIndustrialCeramicOnlyAfterFuel(GameTestHelper helper) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        putMixInputs(helper, Items.QUARTZ, Items.CLAY_BALL);
        powerMixer(helper, 256);
        helper.runAfterDelay(80, () -> {
            require(helper, count(basin.getInputInventory(), Items.QUARTZ) == 1
                            && count(basin.getInputInventory(), Items.CLAY_BALL) == 1
                            && countAll(basin.getOutputInventory()) == 0,
                    "无热时搅拌机消费了陶瓷输入");
            helper.setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState()
                    .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
            consumeBurnerFuel(helper, Items.COAL);
        });
        helper.runAfterDelay(350, () -> {
            require(helper, helper.getBlockState(BURNER).getValue(BlazeBurnerBlock.HEAT_LEVEL) == HeatLevel.KINDLED
                            && ((MechanicalMixerBlockEntity) helper.getBlockEntity(MIXER)).getSpeed() != 0
                            && countAll(basin.getInputInventory()) == 0
                            && countAll(basin.getOutputInventory()) == 2
                            && count(basin.getOutputInventory(), registered(helper, "industrial_ceramic")) == 2,
                    "真实普通加热搅拌没有精确消耗一批并产两份陶瓷");
            helper.succeed();
        });
    }

    /** 真实无热搅拌机用一块硬化混凝土和一粒铅产一块屏蔽混凝土。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void realUnheatedMixerProducesOneShieldingConcrete(GameTestHelper helper) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        putMixInputs(helper, Items.WHITE_CONCRETE, registered(helper, "lead_nugget"));
        powerMixer(helper, 256);
        helper.runAfterDelay(220, () -> {
            require(helper, helper.getBlockState(BURNER).isAir()
                            && ((MechanicalMixerBlockEntity) helper.getBlockEntity(MIXER)).getSpeed() != 0
                            && countAll(basin.getInputInventory()) == 0
                            && countAll(basin.getOutputInventory()) == 1
                            && count(basin.getOutputInventory(), registered(helper, "shielding_concrete")) == 1,
                    "真实无热搅拌没有精确消耗一批并产一块屏蔽混凝土");
            helper.succeed();
        });
    }

    /** 五条序列配方均已加载；重点核对四步换料端口序列及 Create 原生半成品进度身份。 */
    @GameTest(template = TEMPLATE)
    public static void nativeAssemblyRecipesIncludeTheLongestPortSequence(GameTestHelper helper) {
        for (String path : List.of("shielded_glass", "reactor_instrument_port", "reactor_refueling_port",
                "control_rod", "control_rod_drive")) {
            var holder = recipe(helper, "sequenced_assembly/" + path);
            require(helper, holder.value() instanceof SequencedAssemblyRecipe,
                    "缺少 Create 序列装配配方: " + path);
        }

        SequencedAssemblyRecipe refueling = assembly(helper, "reactor_refueling_port");
        require(helper, refueling.getLoops() == 1 && refueling.getSequence().size() == 4
                        && refueling.getIngredient().test(new ItemStack(registered(helper, "reactor_casing")))
                        && refueling.getTransitionalItem().is(registered(helper, "incomplete_reactor_refueling_port"))
                        && refueling.getResultItem(helper.getLevel().registryAccess())
                        .is(registered(helper, "reactor_refueling_port"))
                        && refueling.getResultItem(helper.getLevel().registryAccess()).getCount() == 1
                        && refueling.getOutputChance() == 1f,
                "换料端口的基底、轮数、半成品或必成结果错误");
        List<Item> orderedInputs = List.of(
                createItem(helper, "deployer"),
                registered(helper, "industrial_sensor"),
                registered(helper, "seal_ring"));
        for (int i = 0; i < orderedInputs.size(); i++) {
            var step = refueling.getSequence().get(i).getRecipe();
            require(helper, step instanceof DeployerApplicationRecipe && step.getIngredients().size() == 2
                            && step.getIngredients().get(1).test(new ItemStack(orderedInputs.get(i))),
                    "换料端口机械手顺序错误，步骤=" + i);
        }
        require(helper, refueling.getSequence().getLast().getRecipe() instanceof PressingRecipe,
                "换料端口末步不是原生压片");
        helper.succeed();
    }

    /** 真实机械手与压片机逐步完成最长的四步换料端口序列并各消耗一份投入。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 540)
    public static void realDeployerAndPressCompleteRefuelingPortAssembly(GameTestHelper helper) {
        setupDeployer(helper);
        helper.runAfterDelay(5, () -> {
            putOnDepot(helper, new ItemStack(registered(helper, "reactor_casing")));
            hand(helper, new ItemStack(createItem(helper, "deployer")));
            powerDeployer(helper, 256);
        });
        helper.runAfterDelay(115, () -> {
            assertAssemblyProgress(helper, 1, .25f);
            require(helper, handCount(helper) == 0, "第一步没有消耗一件完整机械手");
            hand(helper, new ItemStack(registered(helper, "industrial_sensor")));
        });
        helper.runAfterDelay(225, () -> {
            assertAssemblyProgress(helper, 2, .5f);
            require(helper, handCount(helper) == 0, "第二步没有消耗一只工业传感器");
            hand(helper, new ItemStack(registered(helper, "seal_ring")));
        });
        helper.runAfterDelay(335, () -> {
            assertAssemblyProgress(helper, 3, .75f);
            require(helper, handCount(helper) == 0, "第三步没有消耗一只密封环");
            setupPress(helper);
        });
        helper.runAfterDelay(495, () -> {
            ItemStack result = depot(helper).getHeldItem();
            require(helper, result.is(registered(helper, "reactor_refueling_port"))
                            && result.getCount() == 1,
                    "真实序列装配未精确得到一只换料端口");
            helper.succeed();
        });
    }

    /** 确认标准合法布局仍由原八种正式结构方块组成，控制棒柱保持空位且不混入燃料物品。 */
    @GameTest(template = TEMPLATE)
    public static void craftedOutputsRemainLegalFormalStructureBlocks(GameTestHelper helper) {
        Map<CoreColumnPosition, ColumnType> columns = new TreeMap<>(ReactorStructureDefinition.defaultColumnLayout());
        columns.put(new CoreColumnPosition(1, 1), ColumnType.CONTROL_ROD);
        Map<LocalPosition, String> template = ReactorStructureDefinition.templateFor(columns);
        var scan = ReactorStructureDefinition.scan(template);
        require(helper, scan.valid() && scan.columns().size() == 9
                        && scan.columns().values().stream().filter(column -> column.type() == ColumnType.FUEL).count() == 8
                        && scan.columns().values().stream().filter(column -> column.type() == ColumnType.CONTROL_ROD).count() == 1,
                "八燃料列加一控制棒列的既有固定结构不合法");
        for (String id : List.of(P1ContentIds.REACTOR_CASING_ID, P1ContentIds.REACTOR_WINDOW_ID,
                P1ContentIds.REACTOR_INSTRUMENT_PORT_ID, P1ContentIds.REACTOR_COLD_PORT_ID,
                P1ContentIds.REACTOR_HOT_PORT_ID, P1ContentIds.REACTOR_REFUELING_PORT_ID,
                P1ContentIds.REACTOR_FUEL_ROD_ID, P1ContentIds.CONTROL_ROD_DRIVE_ID)) {
            ResourceLocation key = ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", id);
            require(helper, BuiltInRegistries.BLOCK.containsKey(key), "正式反应堆方块未注册: " + key);
            require(helper, template.containsValue(key.toString()), "合法结构模板未使用正式产物: " + key);
        }
        long fuelRodBlocks = template.values().stream()
                .filter(id -> id.equals("create_nuclear_industry:reactor_fuel_rod")).count();
        require(helper, fuelRodBlocks == 24, "固定结构燃料柱块数量错误: " + fuelRodBlocks);
        require(helper, template.get(new LocalPosition(2, 2, 2)).equals(ReactorStructureDefinition.AIR_ID),
                "控制棒柱内部被结构材料占据");
        require(helper, !template.containsValue("create_nuclear_industry:fresh_fuel_assembly")
                        && !template.containsValue("create_nuclear_industry:cooled_spent_fuel_assembly"),
                "燃料组件被错误地当作结构方块");
        require(helper, registered(helper, P1ContentIds.CONTROL_ROD_ID).getDefaultMaxStackSize() == 64
                        && !BuiltInRegistries.BLOCK.containsKey(id(P1ContentIds.CONTROL_ROD_ID)),
                "控制棒组件身份被错误地迁移为结构方块");
        helper.succeed();
    }

    private static void assertCraft(GameTestHelper helper, String path, int width, int height,
                                    List<ItemStack> inputs, String output, int count) {
        var holder = recipe(helper, path);
        require(helper, holder.value() instanceof CraftingRecipe, "不是原生工作台配方: " + path);
        CraftingRecipe recipe = (CraftingRecipe) holder.value();
        CraftingInput input = CraftingInput.of(width, height, inputs);
        ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
        require(helper, recipe.matches(input, helper.getLevel())
                        && result.is(registered(helper, output)) && result.getCount() == count,
                "工作台输入或产物数量错误: " + path);
    }

    private static MixingRecipe mixing(GameTestHelper helper, String path) {
        var holder = recipe(helper, "mixing/" + path);
        require(helper, holder.value() instanceof MixingRecipe, "不是 Create 原生搅拌配方: " + path);
        return (MixingRecipe) holder.value();
    }

    private static SequencedAssemblyRecipe assembly(GameTestHelper helper, String path) {
        var holder = recipe(helper, "sequenced_assembly/" + path);
        require(helper, holder.value() instanceof SequencedAssemblyRecipe, "不是 Create 序列装配配方: " + path);
        return (SequencedAssemblyRecipe) holder.value();
    }

    private static net.minecraft.world.item.crafting.RecipeHolder<?> recipe(GameTestHelper helper, String path) {
        return helper.getLevel().getRecipeManager().byKey(id(path)).orElseThrow();
    }

    private static Item registered(GameTestHelper helper, String path) {
        ResourceLocation key = id(path);
        Item item = BuiltInRegistries.ITEM.get(key);
        require(helper, item != Items.AIR && BuiltInRegistries.ITEM.getKey(item).equals(key), "物品身份未注册: " + key);
        return item;
    }

    private static Item createItem(GameTestHelper helper, String path) {
        ResourceLocation key = ResourceLocation.fromNamespaceAndPath("create", path);
        Item item = BuiltInRegistries.ITEM.get(key);
        require(helper, item != Items.AIR && BuiltInRegistries.ITEM.getKey(item).equals(key), "Create 物品未注册: " + key);
        return item;
    }

    private static boolean result(GameTestHelper helper, List<ProcessingOutput> results,
                                  String output, int count) {
        return results.size() == 1 && results.getFirst().getStack().is(registered(helper, output))
                && results.getFirst().getStack().getCount() == count && results.getFirst().getChance() == 1f;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", path);
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }

    private static void setupMixer(GameTestHelper helper) {
        helper.setBlock(BASIN, AllBlocks.BASIN.get());
        helper.setBlock(MIXER, AllBlocks.MECHANICAL_MIXER.get());
        helper.setBlock(COG, AllBlocks.COGWHEEL.getDefaultState()
                .setValue(com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
    }

    private static void powerMixer(GameTestHelper helper, int speed) {
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.DOWN));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(speed);
    }

    private static void putMixInputs(GameTestHelper helper, Item first, Item second) {
        BasinBlockEntity basin = basin(helper);
        basin.getInputInventory().setItem(0, new ItemStack(first));
        basin.getInputInventory().setItem(1, new ItemStack(second));
        basin.notifyChangeOfContents();
    }

    private static void consumeBurnerFuel(GameTestHelper helper, Item item) {
        ItemStack fuel = new ItemStack(item);
        require(helper, BlazeBurnerBlock.tryInsert(helper.getBlockState(BURNER), helper.getLevel(),
                        helper.absolutePos(BURNER), fuel, false, false, false).getResult().consumesAction()
                        && fuel.isEmpty(),
                "燃烧室未通过原生投入路径消耗燃料");
    }

    private static BasinBlockEntity basin(GameTestHelper helper) {
        return (BasinBlockEntity) helper.getBlockEntity(BASIN);
    }

    private static void setupDeployer(GameTestHelper helper) {
        helper.setBlock(DEPOT, AllBlocks.DEPOT.get());
        helper.setBlock(DEPLOYER, AllBlocks.DEPLOYER.getDefaultState()
                .setValue(DirectionalKineticBlock.FACING, Direction.DOWN));
        helper.setBlock(DEPLOYER_MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.EAST));
    }

    private static void setupPress(GameTestHelper helper) {
        helper.setBlock(DEPLOYER, AllBlocks.MECHANICAL_PRESS.getDefaultState()
                .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        powerDeployer(helper, 256);
    }

    private static void putOnDepot(GameTestHelper helper, ItemStack stack) {
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(DEPOT), Direction.UP);
        require(helper, handler != null && handler.insertItem(0, stack, false).isEmpty(),
                "置物台拒收序列装配基底");
    }

    private static DepotBlockEntity depot(GameTestHelper helper) {
        return (DepotBlockEntity) helper.getBlockEntity(DEPOT);
    }

    private static void hand(GameTestHelper helper, ItemStack stack) {
        ((DeployerBlockEntity) helper.getBlockEntity(DEPLOYER)).getPlayer()
                .setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    private static int handCount(GameTestHelper helper) {
        return ((DeployerBlockEntity) helper.getBlockEntity(DEPLOYER)).getPlayer().getMainHandItem().getCount();
    }

    private static void powerDeployer(GameTestHelper helper, int speed) {
        ((CreativeMotorBlockEntity) helper.getBlockEntity(DEPLOYER_MOTOR)).generatedSpeed.setValue(speed);
    }

    private static void assertAssemblyProgress(GameTestHelper helper, int step, float progress) {
        ItemStack stack = depot(helper).getHeldItem();
        var component = stack.get(com.simibubi.create.AllDataComponents.SEQUENCED_ASSEMBLY);
        require(helper, stack.is(registered(helper, "incomplete_reactor_refueling_port"))
                        && stack.getCount() == 1 && component != null
                        && component.id().equals(id("sequenced_assembly/reactor_refueling_port"))
                        && component.step() == step && Math.abs(component.progress() - progress) < .001f,
                "换料端口半成品或原生进度不符，步骤=" + step);
    }

    private static int count(net.neoforged.neoforge.items.IItemHandler inventory, Item item) {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static int countAll(net.neoforged.neoforge.items.IItemHandler inventory) {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) total += inventory.getStackInSlot(slot).getCount();
        return total;
    }
}
