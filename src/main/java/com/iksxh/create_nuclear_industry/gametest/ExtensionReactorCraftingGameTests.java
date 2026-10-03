package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition.ColumnType;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition.LocalPosition;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
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
                List.of(new ItemStack(steel), new ItemStack(grille), new ItemStack(steel)), "reactor_fuel_rod", 1);
        helper.succeed();
    }

    /** 确认陶瓷与铅玻璃需要普通加热，屏蔽混凝土为无热搅拌且仅接受硬化混凝土标签。 */
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

        MixingRecipe glass = mixing(helper, "shielded_glass");
        require(helper, glass.getProcessingDuration() == 100
                        && glass.getRequiredHeat() == HeatCondition.HEATED
                        && glass.getIngredients().size() == 2
                        && glass.getIngredients().get(0).test(new ItemStack(registered(helper, "lead_ingot")))
                        && !glass.getIngredients().get(0).test(new ItemStack(registered(helper, "lead_plate")))
                        && glass.getIngredients().get(1).test(new ItemStack(Items.GLASS))
                        && result(helper, glass.getRollableResults(), "shielded_glass", 1),
                "铅玻璃搅拌输入、热级或结果错误");
        helper.succeed();
    }

    /** 真实搅拌机无热时保留铅锭与玻璃，加入普通燃料后只产一块铅玻璃。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 430)
    public static void realHeatedMixerProducesShieldedGlassOnlyAfterFuel(GameTestHelper helper) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        Item lead = registered(helper, "lead_ingot");
        putMixInputs(helper, lead, Items.GLASS);
        powerMixer(helper, 256);
        helper.runAfterDelay(80, () -> {
            require(helper, count(basin.getInputInventory(), lead) == 1
                            && count(basin.getInputInventory(), Items.GLASS) == 1
                            && countAll(basin.getOutputInventory()) == 0,
                    "无热时搅拌机消费了铅玻璃输入");
            helper.setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState()
                    .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
            consumeBurnerFuel(helper, Items.COAL);
        });
        helper.runAfterDelay(350, () -> {
            require(helper, helper.getBlockState(BURNER).getValue(BlazeBurnerBlock.HEAT_LEVEL) == HeatLevel.KINDLED
                            && ((MechanicalMixerBlockEntity) helper.getBlockEntity(MIXER)).getSpeed() != 0
                            && countAll(basin.getInputInventory()) == 0
                            && countAll(basin.getOutputInventory()) == 1
                            && count(basin.getOutputInventory(), registered(helper, "shielded_glass")) == 1,
                    "真实普通加热搅拌没有精确消耗一批并产一块铅玻璃");
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

    /** 控制棒原生序列保留，四条被替换的序列身份已移除，三条机械合成配方保持原生类型。 */
    @GameTest(template = TEMPLATE)
    public static void replacedRecipePathsAndNativeMechanicalRecipesAreCorrect(GameTestHelper helper) {
        require(helper, recipe(helper, "sequenced_assembly/control_rod").value() instanceof SequencedAssemblyRecipe,
                "原生控制棒序列配方未保留");
        for (String path : List.of("shielded_glass", "reactor_instrument_port", "reactor_refueling_port",
                "control_rod_drive")) {
            require(helper, helper.getLevel().getRecipeManager().byKey(id("sequenced_assembly/" + path)).isEmpty(),
                    "旧序列配方仍加载: " + path);
        }
        assertMechanicalRecipe(helper, "reactor_instrument_port", 3, 1, List.of(
                new ItemStack(registered(helper, "reactor_casing")),
                new ItemStack(registered(helper, "industrial_sensor")),
                new ItemStack(createItem(helper, "electron_tube"))));
        assertMechanicalRecipe(helper, "reactor_refueling_port", 2, 2, List.of(
                new ItemStack(createItem(helper, "deployer")),
                new ItemStack(registered(helper, "industrial_sensor")),
                new ItemStack(registered(helper, "reactor_casing")),
                new ItemStack(registered(helper, "seal_ring"))));
        assertMechanicalRecipe(helper, "control_rod_drive", 2, 2, List.of(
                new ItemStack(createItem(helper, "mechanical_piston")),
                new ItemStack(createItem(helper, "piston_extension_pole")),
                new ItemStack(registered(helper, "control_rod")),
                new ItemStack(createItem(helper, "precision_mechanism"))));
        helper.succeed();
    }

    /** 核对每条机械合成只输出一个目标方块，且普通工作台配方输入不能触发 Create 专用配方。 */
    private static void assertMechanicalRecipe(GameTestHelper helper, String path, int width, int height,
                                               List<ItemStack> inputs) {
        var holder = recipe(helper, "mechanical_crafting/" + path);
        require(helper, holder.value() instanceof MechanicalCraftingRecipe,
                "不是 Create 原生机械合成配方: " + path);
        MechanicalCraftingRecipe mechanical = (MechanicalCraftingRecipe) holder.value();
        boolean ingredientsMatch = mechanical.getIngredients().size() == inputs.size();
        for (int i = 0; ingredientsMatch && i < inputs.size(); i++)
            ingredientsMatch = mechanical.getIngredients().get(i).test(inputs.get(i));
        require(helper, ingredientsMatch
                        && mechanical.getWidth() == width && mechanical.getHeight() == height
                        && !mechanical.acceptsMirrored()
                        && mechanical.getResultItem(helper.getLevel().registryAccess()).is(registered(helper, path))
                        && mechanical.getResultItem(helper.getLevel().registryAccess()).getCount() == 1
                        && !mechanical.matches(CraftingInput.of(width, height, inputs), helper.getLevel()),
                "机械合成尺寸、结果数量或工作台隔离错误: " + path);
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
