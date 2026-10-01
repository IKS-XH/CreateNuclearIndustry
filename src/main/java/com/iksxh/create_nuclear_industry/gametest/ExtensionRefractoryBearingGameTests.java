package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 材料05的服务端合同测试；读取已加载注册表与数据包，机器场景另由实际方块实体 tick 验证。
 * 半成品进度与加热门槛均使用锁定 Create 的原生数据结构，不引入模组自有状态。
 */
@GameTestHolder(CreateNuclearIndustry.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExtensionRefractoryBearingGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos BASIN = new BlockPos(2, 1, 2);
    private static final BlockPos MIXER = BASIN.above(2);
    private static final BlockPos COG = MIXER.west();
    private static final BlockPos MIXER_MOTOR = COG.above();
    private static final BlockPos BURNER = BASIN.below();
    private static final BlockPos DEPOT = BASIN;
    private static final BlockPos OPERATOR = DEPOT.above(2);
    private static final BlockPos MOTOR = OPERATOR.west();

    private ExtensionRefractoryBearingGameTests() {}

    /** 四身份、三配方与错误输入在实际加载的服务端数据中保持精确合同。 */
    @GameTest(template = TEMPLATE)
    public static void loadedMaterial05Contract(GameTestHelper helper) {
        Item quartz = registered(helper, "quartz_dust");
        Item brick = registered(helper, "refractory_brick");
        Item bearing = registered(helper, "heavy_bearing");
        Item interim = registered(helper, "incomplete_heavy_bearing");
        require(helper, quartz.getDefaultMaxStackSize() == 64 && brick.getDefaultMaxStackSize() == 64
                && bearing.getDefaultMaxStackSize() == 64, "三种成品的堆叠数错误");
        require(helper, interim instanceof SequencedAssemblyItem && interim.getDefaultMaxStackSize() == 1,
                "轴承半成品未用 Create 单件物品");
        require(helper, new ItemStack(quartz).is(tag("dusts/quartz"))
                && new ItemStack(quartz).is(tag("dusts")), "石英粉未进入两级通用标签");
        require(helper, new ItemStack(brick).is(TagKey.create(Registries.ITEM,
                id("refractory_bricks"))), "耐火砖未进入本模组公共材料标签");

        var millHolder = helper.getLevel().getRecipeManager().byKey(id("milling/quartz_dust")).orElseThrow();
        require(helper, millHolder.value() instanceof MillingRecipe, "缺少磨石石英粉配方");
        MillingRecipe mill = (MillingRecipe) millHolder.value();
        require(helper, mill.getProcessingDuration() == 100
                && mill.matches(new SingleRecipeInput(new ItemStack(Items.QUARTZ)), helper.getLevel())
                && !mill.matches(new SingleRecipeInput(new ItemStack(Items.QUARTZ_BLOCK)), helper.getLevel())
                && !mill.matches(new SingleRecipeInput(new ItemStack(Items.NETHER_QUARTZ_ORE)), helper.getLevel())
                && mill.getRollableResults().size() == 1
                && mill.getRollableResults().getFirst().getStack().is(quartz)
                && mill.getRollableResults().getFirst().getStack().getCount() == 1
                && mill.getRollableResults().getFirst().getChance() == 1,
                "磨石石英输入、时长或单份结果错误");
        var nativeOre = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                "create", "crushing/nether_quartz_ore")).orElseThrow();
        require(helper, nativeOre.value() instanceof CrushingRecipe
                && ((CrushingRecipe) nativeOre.value()).matches(
                new SingleRecipeInput(new ItemStack(Items.NETHER_QUARTZ_ORE)), helper.getLevel())
                && ((CrushingRecipe) nativeOre.value()).getRollableResults().getFirst().getStack().is(Items.QUARTZ),
                "Create 原生石英矿粉碎路线丢失");

        var mixHolder = helper.getLevel().getRecipeManager().byKey(id("mixing/refractory_brick")).orElseThrow();
        require(helper, mixHolder.value() instanceof MixingRecipe, "缺少耐火砖搅拌配方");
        MixingRecipe mix = (MixingRecipe) mixHolder.value();
        require(helper, mix.getProcessingDuration() == 100 && mix.getRequiredHeat() == HeatCondition.HEATED
                && mix.getIngredients().size() == 3
                && mix.getIngredients().get(0).test(new ItemStack(Items.BRICKS))
                && !mix.getIngredients().get(0).test(new ItemStack(Items.BRICK))
                && mix.getIngredients().get(1).test(new ItemStack(Items.CLAY_BALL))
                && !mix.getIngredients().get(1).test(new ItemStack(Items.CLAY))
                && mix.getIngredients().get(2).test(new ItemStack(quartz))
                && !mix.getIngredients().get(2).test(new ItemStack(Items.QUARTZ))
                && !mix.getIngredients().get(2).test(new ItemStack(registered(helper, "steel_dust")))
                && mix.getRollableResults().size() == 1
                && mix.getRollableResults().getFirst().getStack().is(brick)
                && mix.getRollableResults().getFirst().getStack().getCount() == 4
                && mix.getRollableResults().getFirst().getChance() == 1,
                "耐火砖三输入、热级、时长或四件结果错误");

        var assemblyHolder = helper.getLevel().getRecipeManager().byKey(id("sequenced_assembly/heavy_bearing")).orElseThrow();
        require(helper, assemblyHolder.value() instanceof SequencedAssemblyRecipe, "缺少轴承序列装配配方");
        SequencedAssemblyRecipe assembly = (SequencedAssemblyRecipe) assemblyHolder.value();
        Item sturdy = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "sturdy_sheet"));
        Item precision = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "precision_mechanism"));
        require(helper, assembly.getLoops() == 1 && assembly.getSequence().size() == 3
                && assembly.getIngredient().test(new ItemStack(sturdy))
                && assembly.getTransitionalItem().is(interim)
                && assembly.getResultItem(helper.getLevel().registryAccess()).is(bearing)
                && assembly.getResultItem(helper.getLevel().registryAccess()).getCount() == 1
                && assembly.getOutputChance() == 1f, "轴承基底、轮数或必成结果错误");
        require(helper, assembly.getSequence().get(0).getRecipe() instanceof DeployerApplicationRecipe
                && assembly.getSequence().get(0).getRecipe().getIngredients().get(1)
                .test(new ItemStack(registered(helper, "steel_ingot")))
                && assembly.getSequence().get(1).getRecipe() instanceof DeployerApplicationRecipe
                && assembly.getSequence().get(1).getRecipe().getIngredients().get(1).test(new ItemStack(precision))
                && assembly.getSequence().get(2).getRecipe() instanceof PressingRecipe,
                "轴承机械手与压片顺序错误");
        verified(helper, "loaded-material-05-contract");
    }

    /** 真实磨石先停转保料，再由原生动力完成一石英到一粉的加工。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 190)
    public static void realMillstoneProcessesOneQuartzAfterPowerReturns(GameTestHelper helper) {
        BlockPos millPos = BASIN;
        BlockPos motorPos = millPos.below();
        helper.setBlock(millPos, AllBlocks.MILLSTONE.get());
        helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.UP));
        MillstoneBlockEntity mill = (MillstoneBlockEntity) helper.getBlockEntity(millPos);
        mill.inputInv.setStackInSlot(0, new ItemStack(Items.QUARTZ));
        helper.runAfterDelay(35, () -> {
            require(helper, mill.inputInv.getStackInSlot(0).is(Items.QUARTZ)
                    && mill.outputInv.getStackInSlot(0).isEmpty(), "磨石停机时吞料或提前产粉");
            motor(helper, motorPos).generatedSpeed.setValue(256);
        });
        helper.runAfterDelay(150, () -> {
            LOGGER.info("MATERIAL_05_MILL_DIAG speed={} input={} output={}", mill.getSpeed(),
                    mill.inputInv.getStackInSlot(0), mill.outputInv.getStackInSlot(0));
            require(helper, mill.getSpeed() != 0 && mill.inputInv.getStackInSlot(0).isEmpty()
                    && count(mill.outputInv, registered(helper, "quartz_dust")) == 1,
                    "真实磨石没有精确产一份石英粉");
            verified(helper, "real-millstone-one-quartz-one-dust");
        });
    }

    /** 成对粉碎轮仅由新增磨石配方回退处理石英，世界掉落中只能有一份粉。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 240)
    public static void realCrushingWheelsFallbackToQuartzMilling(GameTestHelper helper) {
        BlockPos left = new BlockPos(1, 4, 2);
        BlockPos right = new BlockPos(3, 4, 2);
        BlockPos controller = new BlockPos(2, 4, 2);
        BlockPos leftMotor = new BlockPos(1, 4, 3);
        BlockPos rightMotor = new BlockPos(3, 4, 1);
        helper.setBlock(left, AllBlocks.CRUSHING_WHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(right, AllBlocks.CRUSHING_WHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(leftMotor, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        helper.setBlock(rightMotor, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        helper.runAfterDelay(5, () -> {
            motor(helper, leftMotor).generatedSpeed.setValue(128);
            motor(helper, rightMotor).generatedSpeed.setValue(128);
        });
        helper.runAfterDelay(30, () -> {
            require(helper, helper.getBlockState(controller).is(AllBlocks.CRUSHING_WHEEL_CONTROLLER.get())
                    && helper.getBlockState(controller).getValue(CrushingWheelControllerBlock.VALID),
                    "粉碎轮未形成有效成对结构");
            CrushingWheelControllerBlockEntity wheels = (CrushingWheelControllerBlockEntity) helper.getBlockEntity(controller);
            require(helper, wheels.crushingspeed > 0, "粉碎轮未获得动力");
            var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(controller), Direction.UP);
            require(helper, handler != null && handler.insertItem(0, new ItemStack(Items.QUARTZ), false).isEmpty(),
                    "粉碎轮拒收石英");
            require(helper, wheels.findRecipe().isPresent()
                    && wheels.findRecipe().orElseThrow().id().equals(id("milling/quartz_dust")),
                    "粉碎轮未回退选择本批磨石配方");
        });
        helper.runAfterDelay(200, () -> {
            BlockPos center = helper.absolutePos(controller);
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(center).inflate(2, 5, 2));
            int quantity = drops.stream().mapToInt(entity -> {
                require(helper, entity.getItem().is(registered(helper, "quartz_dust")), "粉碎轮出现意外副产物");
                return entity.getItem().getCount();
            }).sum();
            require(helper, quantity == 1, "粉碎轮石英粉产量不是一份: " + quantity);
            drops.forEach(ItemEntity::discard);
            verified(helper, "real-crushing-fallback-one-quartz-one-dust");
        });
    }

    /** 无燃烧室与阴燃都保留输入，加入普通煤燃料后真实搅拌产四件。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 440)
    public static void realMixerRequiresOrdinaryHeat(GameTestHelper helper) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        putMixInputs(helper, Items.BRICKS, Items.CLAY_BALL, registered(helper, "quartz_dust"));
        powerMixer(helper, 256);
        helper.runAfterDelay(80, () -> {
            assertMixInputAndNoOutput(helper, basin, Items.BRICKS, Items.CLAY_BALL,
                    registered(helper, "quartz_dust"), "无热");
            helper.setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
        });
        helper.runAfterDelay(160, () -> {
            require(helper, helper.getBlockState(BURNER).getValue(BlazeBurnerBlock.HEAT_LEVEL)
                    == HeatLevel.SMOULDERING, "未投入燃料时燃烧室不处于阴燃");
            assertMixInputAndNoOutput(helper, basin, Items.BRICKS, Items.CLAY_BALL,
                    registered(helper, "quartz_dust"), "阴燃");
            fuel(helper, Items.COAL);
        });
        helper.runAfterDelay(360, () -> {
            require(helper, helper.getBlockState(BURNER).getValue(BlazeBurnerBlock.HEAT_LEVEL)
                    == HeatLevel.KINDLED && ((MechanicalMixerBlockEntity) helper.getBlockEntity(MIXER)).getSpeed() != 0,
                    "普通煤燃烧未驱动加热搅拌");
            assertMixResult(helper, basin, "real-mixer-ordinary-heat-four-bricks");
        });
    }

    /** 超热燃料满足同一个最低热级，仍只产四件而无额外奖励。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 250)
    public static void realMixerAcceptsSuperheatWithoutBonus(GameTestHelper helper) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        putMixInputs(helper, Items.BRICKS, Items.CLAY_BALL, registered(helper, "quartz_dust"));
        helper.setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
        fuel(helper, AllItems.BLAZE_CAKE.get());
        powerMixer(helper, 256);
        helper.runAfterDelay(200, () -> {
            require(helper, helper.getBlockState(BURNER).getValue(BlazeBurnerBlock.HEAT_LEVEL)
                    == HeatLevel.SEETHING, "燃烧室未进入超热状态");
            assertMixResult(helper, basin, "real-mixer-superheat-four-bricks");
        });
    }

    /** 错误砖、黏土形态、未磨石英和缺料都不能触发热搅拌；纠正后恢复。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 650)
    public static void mixerRejectsWrongFormsAndMissingInput(GameTestHelper helper) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        helper.setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
        fuel(helper, Items.COAL);
        putMixInputs(helper, Items.BRICK, Items.CLAY_BALL, registered(helper, "quartz_dust"));
        powerMixer(helper, 256);
        helper.runAfterDelay(105, () -> {
            assertMixInputAndNoOutput(helper, basin, Items.BRICK, Items.CLAY_BALL,
                    registered(helper, "quartz_dust"), "红砖物品");
            basin.getInputInventory().setItem(0, new ItemStack(Items.BRICKS));
            basin.getInputInventory().setItem(1, new ItemStack(Items.CLAY));
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(210, () -> {
            assertMixInputAndNoOutput(helper, basin, Items.BRICKS, Items.CLAY,
                    registered(helper, "quartz_dust"), "黏土块");
            basin.getInputInventory().setItem(1, new ItemStack(Items.CLAY_BALL));
            basin.getInputInventory().setItem(2, new ItemStack(Items.QUARTZ));
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(315, () -> {
            assertMixInputAndNoOutput(helper, basin, Items.BRICKS, Items.CLAY_BALL, Items.QUARTZ,
                    "未磨石英");
            basin.getInputInventory().setItem(2, new ItemStack(registered(helper, "steel_dust")));
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(410, () -> {
            assertMixInputAndNoOutput(helper, basin, Items.BRICKS, Items.CLAY_BALL,
                    registered(helper, "steel_dust"), "错误粉末");
            basin.getInputInventory().setItem(2, ItemStack.EMPTY);
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(505, () -> {
            require(helper, inputCount(basin, Items.BRICKS) == 1 && inputCount(basin, Items.CLAY_BALL) == 1
                    && outputCount(basin, registered(helper, "refractory_brick")) == 0, "缺粉时错误加工");
            basin.getInputInventory().setItem(2, new ItemStack(registered(helper, "quartz_dust")));
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(630, () -> assertMixResult(helper, basin, "mixer-wrong-form-missing-recovery"));
    }

    /** 搅拌盆输出槽全满时先保留三份输入，腾出槽后才完成一次四件产出。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void blockedMixerOutputPreservesInputs(GameTestHelper helper) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        helper.setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
        fuel(helper, Items.COAL);
        for (int slot = 0; slot < basin.getOutputInventory().getSlots(); slot++)
            basin.getOutputInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        putMixInputs(helper, Items.BRICKS, Items.CLAY_BALL, registered(helper, "quartz_dust"));
        powerMixer(helper, 256);
        helper.runAfterDelay(170, () -> {
            assertMixInputAndNoOutput(helper, basin, Items.BRICKS, Items.CLAY_BALL,
                    registered(helper, "quartz_dust"), "输出堵塞");
            basin.getOutputInventory().setItem(0, ItemStack.EMPTY);
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(355, () -> {
            require(helper, countAll(basin.getInputInventory()) == 0
                    && outputCount(basin, registered(helper, "refractory_brick")) == 4
                    && outputCount(basin, Items.COBBLESTONE) == (basin.getOutputInventory().getSlots() - 1) * 64,
                    "解除盆输出堵塞后原料、产物或堵塞物不守恒");
            verified(helper, "mixer-blocked-output-recovery");
        });
    }

    /** 热搅拌真实进入加工进度后拆除热源，恢复后输入与产物仍只对应一批。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 450)
    public static void mixerHeatAndPowerInterruptionPreserveMaterials(GameTestHelper helper) {
        interruptStartedMixer(helper, true);
    }

    /** 热搅拌真实进入加工进度后切断动力，恢复后输入与产物仍只对应一批。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 450)
    public static void mixerPowerInterruptionAfterProgressPreservesMaterials(GameTestHelper helper) {
        interruptStartedMixer(helper, false);
    }

    /** 只读取 Create 的原生进度；先观察进度递减，再切断指定条件并按批次守恒检查。 */
    private static void interruptStartedMixer(GameTestHelper helper, boolean heat) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        helper.setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
        fuel(helper, Items.COAL);
        putMixInputs(helper, Items.BRICKS, Items.CLAY_BALL, registered(helper, "quartz_dust"));
        powerMixer(helper, 256);
        AtomicInteger firstProcessingTicks = new AtomicInteger(-1);
        AtomicBoolean interrupted = new AtomicBoolean();
        helper.onEachTick(() -> {
            if (interrupted.get()) return;
            MechanicalMixerBlockEntity mixer = (MechanicalMixerBlockEntity) helper.getBlockEntity(MIXER);
            if (!mixer.running || mixer.runningTicks != 20 || mixer.processingTicks <= 1) return;
            int current = mixer.processingTicks;
            if (firstProcessingTicks.compareAndSet(-1, current)) return;
            if (current >= firstProcessingTicks.get()) return;
            assertMixInputAndNoOutput(helper, basin, Items.BRICKS, Items.CLAY_BALL,
                    registered(helper, "quartz_dust"), "已启动加工");
            interrupted.set(true);
            LOGGER.info("MATERIAL_05_INTERRUPT_RESULT kind={} progressBefore={} progressAt={} inputCount={} outputCount={}",
                    heat ? "heat" : "power", firstProcessingTicks.get(), current,
                    countAll(basin.getInputInventory()), countAll(basin.getOutputInventory()));
            if (heat) helper.setBlock(BURNER, Blocks.AIR);
            else motor(helper, MIXER_MOTOR).generatedSpeed.setValue(0);
        });
        helper.runAfterDelay(150, () -> {
            require(helper, interrupted.get() && firstProcessingTicks.get() > 1,
                    "搅拌未在真实加工进度已前进后中断");
            if (heat) require(helper, helper.getBlockState(BURNER).isAir(), "断热时热源未移除");
            else require(helper, ((MechanicalMixerBlockEntity) helper.getBlockEntity(MIXER)).getSpeed() == 0,
                    "断动力时搅拌机仍有转速");
            assertOneBatchConserved(helper, basin, heat ? "断热" : "断动力");
            if (heat) {
                helper.setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState()
                        .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
                fuel(helper, Items.COAL);
            } else {
                motor(helper, MIXER_MOTOR).generatedSpeed.setValue(256);
            }
        });
        helper.runAfterDelay(380, () -> assertMixResult(helper, basin,
                heat ? "mixer-started-heat-interruption-recovery" : "mixer-started-power-interruption-recovery"));
    }

    /** 外部等价石英粉随真实 reload 的标签状态变化；启用时必须由加热盆实际消耗。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 280)
    public static void externalQuartzDustFollowsActualReload(GameTestHelper helper) {
        Item substitute = Items.FLINT;
        boolean tagged = new ItemStack(substitute).is(tag("dusts/quartz"));
        MixingRecipe recipe = (MixingRecipe) helper.getLevel().getRecipeManager()
                .byKey(id("mixing/refractory_brick")).orElseThrow().value();
        require(helper, recipe.getIngredients().get(2).test(new ItemStack(substitute)) == tagged,
                "重载后外部石英粉标签与搅拌配方不同步");
        require(helper, new ItemStack(registered(helper, "quartz_dust")).is(tag("dusts/quartz"))
                && new ItemStack(registered(helper, "quartz_dust")).is(tag("dusts"))
                && !new ItemStack(registered(helper, "steel_dust")).is(tag("dusts/quartz")),
                "重载破坏本模组两级石英标签或接纳错误粉末");
        LOGGER.info("MATERIAL_05_RELOAD_RESULT external={} substitute={} matchedExpected=true", tagged,
                BuiltInRegistries.ITEM.getKey(substitute));
        if (!tagged) {
            verified(helper, "external-quartz-reload-false");
            return;
        }
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        helper.setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
        fuel(helper, Items.COAL);
        putMixInputs(helper, Items.BRICKS, Items.CLAY_BALL, substitute);
        powerMixer(helper, 256);
        helper.runAfterDelay(220, () -> assertMixResult(helper, basin, "external-quartz-reload-true-real-mixer"));
    }

    /** 原生半成品组件经 ItemStack 的服务端编码往返后仍保留配方、步骤和进度。 */
    @GameTest(template = TEMPLATE)
    public static void incompleteBearingKeepsNativeProgressWhenSerialized(GameTestHelper helper) {
        ResourceLocation recipe = id("sequenced_assembly/heavy_bearing");
        ItemStack stack = new ItemStack(registered(helper, "incomplete_heavy_bearing"));
        stack.set(AllDataComponents.SEQUENCED_ASSEMBLY,
                new SequencedAssemblyRecipe.SequencedAssembly(recipe, 2, 2f / 3f));
        ItemStack restored = ItemStack.parseOptional(helper.getLevel().registryAccess(),
                (CompoundTag) stack.saveOptional(helper.getLevel().registryAccess()));
        var progress = restored.get(AllDataComponents.SEQUENCED_ASSEMBLY);
        require(helper, restored.is(stack.getItem()) && restored.getCount() == 1 && progress != null
                && progress.id().equals(recipe) && progress.step() == 2
                && Math.abs(progress.progress() - 2f / 3f) < .001f
                && Math.abs(((SequencedAssemblyItem) restored.getItem()).getProgress(restored) - 2f / 3f) < .001f,
                "Create 半成品组件序列化往返丢失");
        verified(helper, "bearing-native-component-roundtrip");
    }

    /** 真机械手依次扣钢锭、精密构件，真压片机最终只产一件重型轴承。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 470)
    public static void realBearingAssemblyConsumesThreeSteps(GameTestHelper helper) {
        setupDeployerDepot(helper);
        helper.runAfterDelay(5, () -> {
            putOnDepot(helper, new ItemStack(createItem("sturdy_sheet")));
            hand(helper, new ItemStack(registered(helper, "steel_ingot")));
            powerDeployer(helper, 256);
        });
        helper.runAfterDelay(110, () -> {
            assertInterim(helper, 1);
            require(helper, handCount(helper) == 0, "首步未精确扣钢锭");
            hand(helper, new ItemStack(createItem("precision_mechanism")));
        });
        helper.runAfterDelay(220, () -> {
            assertInterim(helper, 2);
            require(helper, handCount(helper) == 0, "第二步未精确扣精密构件");
            setupPress(helper);
        });
        helper.runAfterDelay(385, () -> {
            ItemStack result = depot(helper).getHeldItem();
            require(helper, result.is(registered(helper, "heavy_bearing")) && result.getCount() == 1,
                    "真压片机未得到唯一重型轴承");
            verified(helper, "real-bearing-two-deploy-one-press");
        });
    }

    /** 错序、错误物品、缺料和断动力都保持半成品，纠正后按原进度完成。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 820)
    public static void bearingAssemblyRejectsWrongOrderAndResumes(GameTestHelper helper) {
        setupDeployerDepot(helper);
        helper.runAfterDelay(5, () -> {
            putOnDepot(helper, new ItemStack(createItem("sturdy_sheet")));
            hand(helper, new ItemStack(createItem("precision_mechanism")));
            powerDeployer(helper, 256);
        });
        helper.runAfterDelay(115, () -> {
            require(helper, depot(helper).getHeldItem().is(createItem("sturdy_sheet"))
                    && handCount(helper) == 1, "错序精密构件提前消耗");
            hand(helper, new ItemStack(Items.IRON_INGOT));
        });
        helper.runAfterDelay(225, () -> {
            require(helper, depot(helper).getHeldItem().is(createItem("sturdy_sheet"))
                    && handCount(helper) == 1, "错误铁锭提前消耗");
            hand(helper, new ItemStack(registered(helper, "steel_ingot")));
        });
        helper.runAfterDelay(335, () -> {
            assertInterim(helper, 1);
            require(helper, handCount(helper) == 0, "修正首步后未扣钢锭");
        });
        helper.runAfterDelay(425, () -> {
            assertInterim(helper, 1);
            hand(helper, new ItemStack(createItem("precision_mechanism")));
            powerDeployer(helper, 0);
        });
        helper.runAfterDelay(520, () -> {
            assertInterim(helper, 1);
            require(helper, handCount(helper) == 1, "断动力时消耗精密构件");
            powerDeployer(helper, 256);
        });
        helper.runAfterDelay(640, () -> {
            assertInterim(helper, 2);
            require(helper, handCount(helper) == 0, "恢复动力后未扣精密构件");
            setupPress(helper);
        });
        helper.runAfterDelay(790, () -> {
            ItemStack result = depot(helper).getHeldItem();
            require(helper, result.is(registered(helper, "heavy_bearing")) && result.getCount() == 1,
                    "轴承错料断动力恢复后未精确产一件");
            verified(helper, "bearing-wrong-order-missing-power-recovery");
        });
    }

    private static void setupMixer(GameTestHelper helper) {
        helper.setBlock(BASIN, AllBlocks.BASIN.get());
        helper.setBlock(MIXER, AllBlocks.MECHANICAL_MIXER.get());
        helper.setBlock(COG, AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
    }

    private static void powerMixer(GameTestHelper helper, int speed) {
        helper.setBlock(MIXER_MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.DOWN));
        motor(helper, MIXER_MOTOR).generatedSpeed.setValue(speed);
    }

    /** 燃料通过 Create 燃烧室的公开投入路径消耗，而非直接改写热级。 */
    private static void fuel(GameTestHelper helper, Item item) {
        ItemStack fuel = new ItemStack(item);
        require(helper, BlazeBurnerBlock.tryInsert(helper.getBlockState(BURNER), helper.getLevel(),
                helper.absolutePos(BURNER), fuel, false, false, false).getResult().consumesAction()
                && fuel.isEmpty(), "燃烧室没有实际消耗燃料: " + item);
    }

    private static BasinBlockEntity basin(GameTestHelper helper) {
        return (BasinBlockEntity) helper.getBlockEntity(BASIN);
    }

    private static CreativeMotorBlockEntity motor(GameTestHelper helper, BlockPos pos) {
        return (CreativeMotorBlockEntity) helper.getBlockEntity(pos);
    }

    private static void putMixInputs(GameTestHelper helper, Item first, Item second, Item third) {
        BasinBlockEntity basin = basin(helper);
        basin.getInputInventory().setItem(0, new ItemStack(first));
        basin.getInputInventory().setItem(1, new ItemStack(second));
        basin.getInputInventory().setItem(2, new ItemStack(third));
        basin.notifyChangeOfContents();
    }

    private static void assertMixInputAndNoOutput(GameTestHelper helper, BasinBlockEntity basin,
                                                   Item first, Item second, Item third, String stage) {
        require(helper, inputCount(basin, first) == 1 && inputCount(basin, second) == 1
                && inputCount(basin, third) == 1
                && outputCount(basin, registered(helper, "refractory_brick")) == 0,
                stage + "时盆内三料或产量不守恒");
    }

    private static void assertMixResult(GameTestHelper helper, BasinBlockEntity basin, String scenario) {
        require(helper, countAll(basin.getInputInventory()) == 0
                && countAll(basin.getOutputInventory()) == 4
                && outputCount(basin, registered(helper, "refractory_brick")) == 4,
                "加热盆未精确消耗三料并产四耐火砖: " + scenario);
        verified(helper, scenario);
    }

    /** 输入完整或已成四件均表示原生处理中间态守恒；不要求中断后精确进度冻结。 */
    private static void assertOneBatchConserved(GameTestHelper helper, BasinBlockEntity basin, String stage) {
        int inputs = countAll(basin.getInputInventory());
        int outputs = countAll(basin.getOutputInventory());
        boolean pending = inputs == 3 && inputCount(basin, Items.BRICKS) == 1
                && inputCount(basin, Items.CLAY_BALL) == 1
                && inputCount(basin, registered(helper, "quartz_dust")) == 1 && outputs == 0;
        boolean completed = inputs == 0 && outputs == 4
                && outputCount(basin, registered(helper, "refractory_brick")) == 4;
        require(helper, pending || completed, stage + "后的原料与耐火砖不对应唯一一批");
    }

    private static int inputCount(BasinBlockEntity basin, Item item) {
        return count(basin.getInputInventory(), item);
    }

    private static int outputCount(BasinBlockEntity basin, Item item) {
        return count(basin.getOutputInventory(), item);
    }

    private static int count(net.neoforged.neoforge.items.IItemHandler inventory, Item item) {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++)
            if (inventory.getStackInSlot(slot).is(item)) total += inventory.getStackInSlot(slot).getCount();
        return total;
    }

    private static int countAll(net.neoforged.neoforge.items.IItemHandler inventory) {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++)
            total += inventory.getStackInSlot(slot).getCount();
        return total;
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
        powerDeployer(helper, 256);
    }

    private static void putOnDepot(GameTestHelper helper, ItemStack stack) {
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(DEPOT), Direction.UP);
        require(helper, handler != null && handler.insertItem(0, stack, false).isEmpty(), "置物台拒收轴承基底");
    }

    private static DepotBlockEntity depot(GameTestHelper helper) {
        return (DepotBlockEntity) helper.getBlockEntity(DEPOT);
    }

    private static void hand(GameTestHelper helper, ItemStack stack) {
        ((DeployerBlockEntity) helper.getBlockEntity(OPERATOR)).getPlayer()
                .setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    private static int handCount(GameTestHelper helper) {
        return ((DeployerBlockEntity) helper.getBlockEntity(OPERATOR)).getPlayer().getMainHandItem().getCount();
    }

    private static void powerDeployer(GameTestHelper helper, int speed) {
        motor(helper, MOTOR).generatedSpeed.setValue(speed);
    }

    private static Item createItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", path));
    }

    private static void assertInterim(GameTestHelper helper, int step) {
        ItemStack stack = depot(helper).getHeldItem();
        var component = stack.get(AllDataComponents.SEQUENCED_ASSEMBLY);
        require(helper, stack.is(registered(helper, "incomplete_heavy_bearing")) && stack.getCount() == 1
                && component != null && component.id().equals(id("sequenced_assembly/heavy_bearing"))
                && component.step() == step && Math.abs(component.progress() - step / 3f) < .001f,
                "轴承半成品身份或原生步骤进度不符: " + step);
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
        LOGGER.info("MATERIAL_05_TEST_RESULT scenario={} gameTime={} assertions=passed", scenario,
                helper.getLevel().getGameTime());
    }
}

