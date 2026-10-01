package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.mojang.logging.LogUtils;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 锡条与传感器的服务端加工合同测试。
 * 测试读取实际注册表和数据包，机器场景另由服务端世界 tick 驱动。
 */
@GameTestHolder(CreateNuclearIndustry.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExtensionSensorProcessingGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos DEPOT = new BlockPos(2, 1, 2);
    private static final BlockPos OPERATOR = DEPOT.above(2);
    private static final BlockPos MOTOR = OPERATOR.west();
    private static final BlockPos SAW = new BlockPos(2, 2, 2);
    private static final BlockPos SAW_OUTPUT = SAW.north();
    private static final BlockPos SAW_MOTOR = SAW.west();

    private ExtensionSensorProcessingGameTests() {}

    /** 五个任务物品必须由本模组注册，不能由空气身份替代。 */
    @GameTest(template = TEMPLATE)
    public static void allFiveSensorMaterialsAreRegistered(GameTestHelper helper) {
        for (String path : List.of("tin_wire", "industrial_sensor", "radiation_sensor",
                "incomplete_industrial_sensor", "incomplete_radiation_sensor")) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, path);
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR || !BuiltInRegistries.ITEM.getKey(item).equals(id))
                helper.fail("缺少本批物品注册: " + id);
        }
        helper.succeed();
    }

    /** 成品可堆叠，半成品是带进度条的单件 Create 物品，通用锡条标签可用。 */
    @GameTest(template = TEMPLATE)
    public static void sensorItemStackAndTagContract(GameTestHelper helper) {
        for (String path : List.of("tin_wire", "industrial_sensor", "radiation_sensor"))
            require(helper, registered(helper, path).getDefaultMaxStackSize() == 64, "成品不可堆叠: " + path);
        for (String path : List.of("incomplete_industrial_sensor", "incomplete_radiation_sensor")) {
            Item item = registered(helper, path);
            require(helper, item instanceof SequencedAssemblyItem && item.getDefaultMaxStackSize() == 1
                    && item.isBarVisible(new ItemStack(item)), "半成品未使用 Create 单件进度物品: " + path);
        }
        ItemStack wire = new ItemStack(registered(helper, "tin_wire"));
        require(helper, wire.is(tag("wires")) && wire.is(tag("wires/tin")), "锡条未进入两级通用标签");
        verified(helper, "item-stack-tags");
    }

    /** 两种语言资源中的五项名称均按玩家可见合同提供。 */
    @GameTest(template = TEMPLATE)
    public static void bothLanguageFilesContainSensorNames(GameTestHelper helper) {
        Map<String, String[]> names = Map.of(
                "tin_wire", new String[]{"锡条", "Tin Wire"},
                "industrial_sensor", new String[]{"工业传感器", "Industrial Sensor"},
                "radiation_sensor", new String[]{"辐射传感器", "Radiation Sensor"},
                "incomplete_industrial_sensor", new String[]{"工业传感器半成品", "Incomplete Industrial Sensor"},
                "incomplete_radiation_sensor", new String[]{"辐射传感器半成品", "Incomplete Radiation Sensor"});
        for (String locale : List.of("zh_cn", "en_us")) {
            String resource = "assets/create_nuclear_industry/lang/" + locale + ".json";
            try (InputStream stream = ExtensionSensorProcessingGameTests.class.getClassLoader()
                    .getResourceAsStream(resource)) {
                require(helper, stream != null, "语言资源缺失: " + resource);
                var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                int index = locale.equals("zh_cn") ? 0 : 1;
                for (var name : names.entrySet()) {
                    String key = "item.create_nuclear_industry." + name.getKey();
                    require(helper, json.has(key) && json.get(key).getAsString().equals(name.getValue()[index]),
                            "语言名称不符: " + resource + " / " + key);
                }
            } catch (IOException exception) {
                helper.fail("语言资源读取失败: " + resource + " / " + exception.getMessage());
            }
        }
        verified(helper, "sensor-zh-en-names");
    }

    /** 锡锭切石只产两条，错误金属不得匹配该配方。 */
    @GameTest(template = TEMPLATE)
    public static void stonecuttingRecipeHasExactYield(GameTestHelper helper) {
        var holder = helper.getLevel().getRecipeManager().byKey(id("stonecutting/tin_wire")).orElseThrow();
        require(helper, holder.value() instanceof StonecutterRecipe, "锡条不是原版切石配方");
        StonecutterRecipe recipe = (StonecutterRecipe) holder.value();
        require(helper, recipe.matches(new SingleRecipeInput(new ItemStack(registered(helper, "tin_ingot"))), helper.getLevel())
                && !recipe.matches(new SingleRecipeInput(new ItemStack(Items.IRON_INGOT)), helper.getLevel())
                && recipe.getResultItem(helper.getLevel().registryAccess()).is(registered(helper, "tin_wire"))
                && recipe.getResultItem(helper.getLevel().registryAccess()).getCount() == 2,
                "切石输入或两条产量不符");
        verified(helper, "stonecutting-recipe-two-wire");
    }

    /** 原版切石菜单实际选择配方并取走两条，输入只扣一锡锭。 */
    @GameTest(template = TEMPLATE)
    public static void realStonecutterMenuTakesTwoWires(GameTestHelper helper) {
        BlockPos position = new BlockPos(2, 1, 2);
        helper.setBlock(position, Blocks.STONECUTTER);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        StonecutterMenu menu = new StonecutterMenu(0, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(position)));
        menu.getSlot(0).set(new ItemStack(registered(helper, "tin_ingot")));
        require(helper, menu.getNumRecipes() > 0 && menu.clickMenuButton(player, 0),
                "原版切石菜单未提供锡条选项");
        ItemStack result = menu.getSlot(1).getItem().copy();
        require(helper, result.is(registered(helper, "tin_wire")) && result.getCount() == 2,
                "切石菜单展示产量不是两条");
        menu.getSlot(1).onTake(player, result);
        require(helper, menu.getSlot(0).getItem().isEmpty(), "切石取料未扣一锡锭");
        verified(helper, "real-stonecutter-take-two-wire");
    }

    /** 原生机械锯按服务器配置处理切石配方，满置物台阻塞输出并在腾空后释放两条。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void realSawRespectsConfigAndBlockedOutput(GameTestHelper helper) {
        helper.setBlock(SAW, AllBlocks.MECHANICAL_SAW.getDefaultState()
                .setValue(SawBlock.FACING, Direction.UP)
                .setValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        helper.setBlock(SAW_MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.EAST));
        helper.setBlock(SAW_OUTPUT, AllBlocks.DEPOT.get());
        helper.runAfterDelay(5, () -> {
            powerSaw(helper, 256);
            require(helper, ((SawBlockEntity) helper.getBlockEntity(SAW)).getSpeed() != 0,
                    "真实机械锯未获得轴动力");
            var output = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(SAW_OUTPUT), Direction.UP);
            require(helper, output != null && output.insertItem(0, new ItemStack(Items.COBBLESTONE), false).isEmpty(),
                    "输出置物台未能设置堵塞物");
            var input = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(SAW), Direction.UP);
            require(helper, input != null && input.insertItem(0, new ItemStack(registered(helper, "tin_ingot")), false).isEmpty(),
                    "真实机械锯未收锡锭");
        });
        helper.runAfterDelay(100, () -> {
            SawBlockEntity saw = (SawBlockEntity) helper.getBlockEntity(SAW);
            if (AllConfigs.server().recipes.allowStonecuttingOnSaw.get()) {
                ItemStack held = saw.inventory.getStackInSlot(1);
                require(helper, held.is(registered(helper, "tin_wire")) && held.getCount() == 2
                        && saw.inventory.getStackInSlot(0).isEmpty(), "机械锯堵塞输出时未保留两条且只扣一锡锭");
            } else {
                require(helper, saw.inventory.getStackInSlot(0).is(registered(helper, "tin_ingot"))
                        && saw.inventory.getStackInSlot(1).isEmpty(), "关闭原生切石开关后机械锯仍加工锡锭");
            }
            require(helper, depotAt(helper, SAW_OUTPUT).getHeldItem().is(Items.COBBLESTONE),
                    "堵塞物被机械锯覆盖");
            depotAt(helper, SAW_OUTPUT).setHeldItem(ItemStack.EMPTY);
        });
        helper.runAfterDelay(220, () -> {
            SawBlockEntity saw = (SawBlockEntity) helper.getBlockEntity(SAW);
            ItemStack held = depotAt(helper, SAW_OUTPUT).getHeldItem();
            if (AllConfigs.server().recipes.allowStonecuttingOnSaw.get()) {
                require(helper, held.is(registered(helper, "tin_wire")) && held.getCount() == 2
                        && saw.inventory.isEmpty(), "机械锯输出解除堵塞后未精确转交两条");
                verified(helper, "real-saw-blocked-output-two-wire");
            } else {
                ItemStack retained = saw.inventory.getStackInSlot(0);
                int tinCount = (held.is(registered(helper, "tin_ingot")) ? held.getCount() : 0)
                        + (retained.is(registered(helper, "tin_ingot")) ? retained.getCount() : 0);
                require(helper, !held.is(registered(helper, "tin_wire"))
                        && saw.inventory.getStackInSlot(1).isEmpty() && tinCount == 1,
                        "关闭原生切石开关后仍出现锡条或丢失锡锭");
                verified(helper, "real-saw-stonecutting-disabled");
            }
        });
    }

    /** 两条装配配方按批准的底板、顺序、轮数及唯一结果加载。 */
    @GameTest(template = TEMPLATE)
    public static void bothAssemblyRecipesHaveApprovedOrder(GameTestHelper helper) {
        assertAssembly(helper, "industrial_sensor", "incomplete_industrial_sensor", "create:iron_sheet",
                List.of("tin_wire", "minecraft:redstone", "create:electron_tube"));
        assertAssembly(helper, "radiation_sensor", "incomplete_radiation_sensor", "lead_plate",
                List.of("industrial_sensor", "create:electron_tube"));
        verified(helper, "assembly-recipe-order");
    }

    /** Create 序列组件经原生 ItemStack 编码后保留配方、步数及进度。 */
    @GameTest(template = TEMPLATE)
    public static void incompleteStackKeepsNativeProgressWhenSerialized(GameTestHelper helper) {
        ResourceLocation recipe = id("sequenced_assembly/industrial_sensor");
        ItemStack stack = new ItemStack(registered(helper, "incomplete_industrial_sensor"));
        stack.set(AllDataComponents.SEQUENCED_ASSEMBLY, new SequencedAssemblyRecipe.SequencedAssembly(recipe, 2, .5f));
        ItemStack restored = ItemStack.parseOptional(helper.getLevel().registryAccess(),
                (CompoundTag) stack.saveOptional(helper.getLevel().registryAccess()));
        var progress = restored.get(AllDataComponents.SEQUENCED_ASSEMBLY);
        require(helper, restored.is(stack.getItem()) && restored.getCount() == 1 && progress != null
                && progress.id().equals(recipe) && progress.step() == 2 && progress.progress() == .5f
                && ((SequencedAssemblyItem) restored.getItem()).getProgress(restored) == .5f,
                "Create 原生半成品组件序列化往返丢失");
        verified(helper, "native-component-roundtrip");
    }

    /** 外部临时包增删锡条等价身份后，运行时标签与装配首步必须同步变化。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void externalTinWireFollowsActualReload(GameTestHelper helper) {
        Item substitute = Items.FLINT;
        boolean tagged = new ItemStack(substitute).is(tag("wires/tin"));
        SequencedAssemblyRecipe recipe = assembly(helper, "industrial_sensor");
        boolean matched = recipe.getSequence().getFirst().getRecipe().getIngredients().get(1)
                .test(new ItemStack(substitute));
        require(helper, tagged == matched, "重载后外部锡条标签与装配首步不一致");
        LOGGER.info("MATERIAL_04_RELOAD_RESULT external={} substitute={} matchedExpected=true", tagged,
                BuiltInRegistries.ITEM.getKey(substitute));
        if (!tagged) {
            verified(helper, "external-wire-reload-false");
            return;
        }
        setupDeployerDepot(helper);
        helper.runAfterDelay(5, () -> {
            putOnDepot(helper, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "iron_sheet"))));
            hand(helper, new ItemStack(substitute));
            power(helper, 256);
        });
        helper.runAfterDelay(120, () -> {
            assertInterim(helper, "incomplete_industrial_sensor", "industrial_sensor", 1, .25f);
            require(helper, handCount(helper) == 0, "外部锡条等价物未由真实机械手扣料");
            verified(helper, "external-wire-reload-true-real-deployer");
        });
    }

    /** 错序、缺料和断动力均保持当前组件，纠正后逐步恢复至唯一成品。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 1100)
    public static void industrialAssemblyRejectsWrongInputsAndResumes(GameTestHelper helper) {
        setupDeployerDepot(helper);
        helper.runAfterDelay(5, () -> {
            putOnDepot(helper, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "iron_sheet"))));
            hand(helper, new ItemStack(Items.REDSTONE));
            power(helper, 256);
        });
        helper.runAfterDelay(120, () -> {
            require(helper, depot(helper).getHeldItem().is(tag("plates/iron")) && handCount(helper) == 1,
                    "错序红石错误消耗底板或投入");
            hand(helper, new ItemStack(registered(helper, "tin_wire")));
        });
        helper.runAfterDelay(240, () -> {
            assertInterim(helper, "incomplete_industrial_sensor", "industrial_sensor", 1, .25f);
            require(helper, handCount(helper) == 0, "纠正首步后锡条未消耗");
        });
        helper.runAfterDelay(330, () -> {
            assertInterim(helper, "incomplete_industrial_sensor", "industrial_sensor", 1, .25f);
            hand(helper, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "electron_tube"))));
        });
        helper.runAfterDelay(450, () -> {
            assertInterim(helper, "incomplete_industrial_sensor", "industrial_sensor", 1, .25f);
            require(helper, handCount(helper) == 1, "第二步错误电子管被消耗");
            power(helper, 0);
            hand(helper, new ItemStack(Items.REDSTONE));
        });
        helper.runAfterDelay(540, () -> {
            assertInterim(helper, "incomplete_industrial_sensor", "industrial_sensor", 1, .25f);
            require(helper, handCount(helper) == 1, "断动力时红石被消耗");
            power(helper, 256);
        });
        helper.runAfterDelay(660, () -> {
            assertInterim(helper, "incomplete_industrial_sensor", "industrial_sensor", 2, .5f);
            require(helper, handCount(helper) == 0, "恢复动力后红石未精确消耗");
            hand(helper, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "electron_tube"))));
        });
        helper.runAfterDelay(790, () -> {
            assertInterim(helper, "incomplete_industrial_sensor", "industrial_sensor", 3, .75f);
            require(helper, handCount(helper) == 0, "纠正末步后电子管未精确消耗");
            setupPress(helper);
        });
        helper.runAfterDelay(1010, () -> {
            ItemStack result = depot(helper).getHeldItem();
            require(helper, result.is(registered(helper, "industrial_sensor")) && result.getCount() == 1,
                    "错序和断动力恢复后未得到唯一工业传感器");
            verified(helper, "industrial-wrong-order-missing-power-recovery");
        });
    }

    /** 真实机械手依次消耗锡条、红石和电子管，再由真实压片机产一只工业传感器。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 530)
    public static void realIndustrialSensorAssemblyConsumesEveryStep(GameTestHelper helper) {
        setupDeployerDepot(helper);
        helper.runAfterDelay(5, () -> {
            putOnDepot(helper, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "iron_sheet"))));
            hand(helper, new ItemStack(registered(helper, "tin_wire")));
            power(helper, 256);
        });
        helper.runAfterDelay(110, () -> {
            assertInterim(helper, "incomplete_industrial_sensor", "industrial_sensor", 1, .25f);
            require(helper, handCount(helper) == 0, "第一步未扣一锡条");
            hand(helper, new ItemStack(Items.REDSTONE));
        });
        helper.runAfterDelay(220, () -> {
            assertInterim(helper, "incomplete_industrial_sensor", "industrial_sensor", 2, .5f);
            require(helper, handCount(helper) == 0, "第二步未扣一红石");
            hand(helper, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "electron_tube"))));
        });
        helper.runAfterDelay(330, () -> {
            assertInterim(helper, "incomplete_industrial_sensor", "industrial_sensor", 3, .75f);
            require(helper, handCount(helper) == 0, "第三步未扣一电子管");
            setupPress(helper);
        });
        helper.runAfterDelay(485, () -> {
            ItemStack result = depot(helper).getHeldItem();
            require(helper, result.is(registered(helper, "industrial_sensor")) && result.getCount() == 1,
                    "真实压片未得到唯一工业传感器");
            verified(helper, "real-industrial-three-deploy-one-press");
        });
    }

    /** 辐射传感器消耗已制成的工业传感器与电子管，最后由压片机产一件。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 420)
    public static void realRadiationSensorAssemblyConsumesEveryStep(GameTestHelper helper) {
        setupDeployerDepot(helper);
        helper.runAfterDelay(5, () -> {
            putOnDepot(helper, new ItemStack(registered(helper, "lead_plate")));
            hand(helper, new ItemStack(registered(helper, "industrial_sensor")));
            power(helper, 256);
        });
        helper.runAfterDelay(110, () -> {
            assertInterim(helper, "incomplete_radiation_sensor", "radiation_sensor", 1, 1f / 3f);
            require(helper, handCount(helper) == 0, "第一步未扣工业传感器");
            hand(helper, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "electron_tube"))));
        });
        helper.runAfterDelay(220, () -> {
            assertInterim(helper, "incomplete_radiation_sensor", "radiation_sensor", 2, 2f / 3f);
            require(helper, handCount(helper) == 0, "第二步未扣电子管");
            setupPress(helper);
        });
        helper.runAfterDelay(370, () -> {
            ItemStack result = depot(helper).getHeldItem();
            require(helper, result.is(registered(helper, "radiation_sensor")) && result.getCount() == 1,
                    "真实压片未得到唯一辐射传感器");
            verified(helper, "real-radiation-two-deploy-one-press");
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
        power(helper, 256);
    }

    private static void putOnDepot(GameTestHelper helper, ItemStack stack) {
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(DEPOT), Direction.UP);
        require(helper, handler != null && handler.insertItem(0, stack, false).isEmpty(),
                "置物台拒收底板");
    }

    private static DepotBlockEntity depot(GameTestHelper helper) {
        return (DepotBlockEntity) helper.getBlockEntity(DEPOT);
    }

    private static DepotBlockEntity depotAt(GameTestHelper helper, BlockPos pos) {
        return (DepotBlockEntity) helper.getBlockEntity(pos);
    }

    private static void powerSaw(GameTestHelper helper, int speed) {
        ((CreativeMotorBlockEntity) helper.getBlockEntity(SAW_MOTOR)).generatedSpeed.setValue(speed);
    }

    private static void hand(GameTestHelper helper, ItemStack stack) {
        ((DeployerBlockEntity) helper.getBlockEntity(OPERATOR)).getPlayer()
                .setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    private static int handCount(GameTestHelper helper) {
        return ((DeployerBlockEntity) helper.getBlockEntity(OPERATOR)).getPlayer().getMainHandItem().getCount();
    }

    private static void power(GameTestHelper helper, int speed) {
        ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(speed);
    }

    private static void assertInterim(GameTestHelper helper, String path, String recipePath,
                                      int step, float expectedProgress) {
        ItemStack stack = depot(helper).getHeldItem();
        var component = stack.get(AllDataComponents.SEQUENCED_ASSEMBLY);
        require(helper, stack.is(registered(helper, path)) && stack.getCount() == 1 && component != null
                        && component.id().equals(id("sequenced_assembly/" + recipePath))
                        && component.step() == step && Math.abs(component.progress() - expectedProgress) < .001f,
                "半成品身份、单件数量或原生组件进度不符: " + recipePath + " step=" + step);
    }

    private static void assertAssembly(GameTestHelper helper, String product, String interim, String base,
                                       List<String> extras) {
        SequencedAssemblyRecipe recipe = assembly(helper, product);
        require(helper, recipe.getLoops() == 1 && recipe.getSequence().size() == extras.size() + 1
                && recipe.getIngredient().test(new ItemStack(base.contains(":")
                ? BuiltInRegistries.ITEM.get(ResourceLocation.parse(base)) : registered(helper, base)))
                && recipe.getTransitionalItem().is(registered(helper, interim))
                && recipe.getResultItem(helper.getLevel().registryAccess()).is(registered(helper, product))
                && recipe.getResultItem(helper.getLevel().registryAccess()).getCount() == 1
                && recipe.getOutputChance() == 1f, "装配轮数、基底或结果不符: " + product);
        for (int i = 0; i < extras.size(); i++) {
            var step = recipe.getSequence().get(i).getRecipe();
            require(helper, step instanceof DeployerApplicationRecipe && step.getIngredients().size() == 2,
                    "第 " + i + " 步不是机械手: " + product);
            String expected = extras.get(i);
            Item input = expected.contains(":")
                    ? BuiltInRegistries.ITEM.get(ResourceLocation.parse(expected)) : registered(helper, expected);
            require(helper, step.getIngredients().get(1).test(new ItemStack(input)),
                    "机械手投入顺序错误: " + product + " step=" + i);
        }
        require(helper, recipe.getSequence().getLast().getRecipe() instanceof PressingRecipe,
                "装配末步不是压片: " + product);
    }

    private static SequencedAssemblyRecipe assembly(GameTestHelper helper, String product) {
        var holder = helper.getLevel().getRecipeManager().byKey(id("sequenced_assembly/" + product)).orElseThrow();
        require(helper, holder.value() instanceof SequencedAssemblyRecipe, "缺少 Create 装配配方: " + product);
        return (SequencedAssemblyRecipe) holder.value();
    }

    private static Item registered(GameTestHelper helper, String path) {
        ResourceLocation key = id(path);
        Item item = BuiltInRegistries.ITEM.get(key);
        require(helper, item != Items.AIR && BuiltInRegistries.ITEM.getKey(item).equals(key), "缺少物品注册: " + key);
        return item;
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, path);
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }

    private static void verified(GameTestHelper helper, String scenario) {
        helper.succeed();
        LOGGER.info("MATERIAL_04_TEST_RESULT scenario={} gameTime={} assertions=passed", scenario,
                helper.getLevel().getGameTime());
    }
}
