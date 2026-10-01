package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

import java.util.List;

/**
 * 钢材路线的逻辑服务端集成测试。配方断言读取当前数据包，机器断言等待真实方块实体 tick。
 * 外部粉末测试只观察隔离服务端经 /reload 后的标签状态，不修改正式世界。
 */
@GameTestHolder(CreateNuclearIndustry.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExtensionSteelProcessingGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos BASIN = new BlockPos(2, 1, 2);
    private static final BlockPos MIXER = BASIN.above(2);
    private static final BlockPos COG = MIXER.west();
    private static final BlockPos MIXER_MOTOR = COG.above();
    private static final BlockPos DEPOT = BASIN;
    private static final BlockPos PRESS = DEPOT.above(2);
    private static final BlockPos PRESS_MOTOR = PRESS.west();
    private static final BlockPos FURNACE = new BlockPos(2, 2, 2);

    private ExtensionSteelProcessingGameTests() {}

    /** 五项身份、通用标签、八条加载配方及错误形态都从运行时注册表核对。 */
    @GameTest(template = TEMPLATE)
    public static void loadedSteelRouteHasExactInputsAndOutputs(GameTestHelper helper) {
        for (String name : List.of("iron_dust", "coal_dust", "charcoal_dust", "steel_dust")) {
            Item item = registered(helper, name);
            require(helper, item.getDefaultMaxStackSize() == 64
                    && new ItemStack(item).is(tag("dusts"))
                    && new ItemStack(item).is(tag("dusts/" + name.replace("_dust", ""))),
                    "粉末身份或标签不符: " + name);
        }
        Item steel = registered(helper, "steel_ingot");
        Item plate = registered(helper, "steel_plate");
        require(helper, steel.getDefaultMaxStackSize() == 64
                && new ItemStack(steel).is(tag("ingots"))
                && new ItemStack(steel).is(tag("ingots/steel")), "钢锭身份或标签不符");
        require(helper, !new ItemStack(Items.IRON_INGOT).is(tag("dusts/iron"))
                && !new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "crushed_raw_iron"))).is(tag("dusts/iron")),
                "铁锭或粉碎粗铁污染了铁粉标签");

        for (String source : List.of("iron", "coal", "charcoal")) {
            ResourceLocation key = id("crushing/" + source + "_dust");
            var holder = helper.getLevel().getRecipeManager().byKey(key).orElseThrow();
            require(helper, holder.value() instanceof CrushingRecipe, "未加载 Create 粉碎配方: " + key);
            CrushingRecipe recipe = (CrushingRecipe) holder.value();
            Item input = source.equals("iron") ? Items.IRON_INGOT : source.equals("coal") ? Items.COAL : Items.CHARCOAL;
            Item wrong = source.equals("iron") ? Items.RAW_IRON : source.equals("coal") ? Items.CHARCOAL : Items.COAL;
            require(helper, recipe.getProcessingDuration() == 100
                    && recipe.matches(new SingleRecipeInput(new ItemStack(input)), helper.getLevel())
                    && !recipe.matches(new SingleRecipeInput(new ItemStack(wrong)), helper.getLevel()),
                    "粉碎时间、输入或拒绝规则不符: " + key);
            require(helper, recipe.getRollableResults().size() == 1
                    && recipe.getRollableResults().getFirst().getStack().is(registered(helper, source + "_dust"))
                    && recipe.getRollableResults().getFirst().getStack().getCount() == 1
                    && recipe.getRollableResults().getFirst().getChance() == 1,
                    "粉碎产物或副产物不符: " + key);
        }
        for (String carbon : List.of("coal", "charcoal")) {
            ResourceLocation key = id("mixing/steel_dust_from_" + carbon);
            var holder = helper.getLevel().getRecipeManager().byKey(key).orElseThrow();
            require(helper, holder.value() instanceof MixingRecipe, "未加载 Create 搅拌配方: " + key);
            MixingRecipe recipe = (MixingRecipe) holder.value();
            require(helper, recipe.getProcessingDuration() == 100 && recipe.getIngredients().size() == 5
                    && recipe.getRequiredHeat() == HeatCondition.NONE,
                    "搅拌配方没有五个独立 Ingredient、无热要求或时间错误: " + key);
            for (int i = 0; i < 4; i++) require(helper, recipe.getIngredients().get(i).test(new ItemStack(registered(helper, "iron_dust")))
                    && !recipe.getIngredients().get(i).test(new ItemStack(Items.IRON_INGOT)), "铁粉份额错误: " + key);
            require(helper, recipe.getIngredients().get(4).test(new ItemStack(registered(helper, carbon + "_dust")))
                    && !recipe.getIngredients().get(4).test(new ItemStack(registered(helper,
                    carbon.equals("coal") ? "charcoal_dust" : "coal_dust"))), "碳粉种类错误: " + key);
            require(helper, recipe.getRollableResults().size() == 1
                    && recipe.getRollableResults().getFirst().getStack().is(registered(helper, "steel_dust"))
                    && recipe.getRollableResults().getFirst().getStack().getCount() == 5
                    && recipe.getRollableResults().getFirst().getChance() == 1, "搅拌产物不符: " + key);
        }
        for (boolean blast : List.of(false, true)) {
            ResourceLocation key = id((blast ? "blasting" : "smelting") + "/steel_ingot_from_dust");
            var holder = helper.getLevel().getRecipeManager().byKey(key).orElseThrow();
            require(helper, holder.value() instanceof AbstractCookingRecipe, "未加载原生炉配方: " + key);
            AbstractCookingRecipe recipe = (AbstractCookingRecipe) holder.value();
            require(helper, recipe.getType() == (blast ? RecipeType.BLASTING : RecipeType.SMELTING)
                    && recipe.getCookingTime() == (blast ? 100 : 200)
                    && recipe.getExperience() == 0.1f
                    && recipe.matches(new SingleRecipeInput(new ItemStack(registered(helper, "steel_dust"))), helper.getLevel())
                    && !recipe.matches(new SingleRecipeInput(new ItemStack(Items.IRON_INGOT)), helper.getLevel())
                    && recipe.getResultItem(helper.getLevel().registryAccess()).is(steel)
                    && recipe.getResultItem(helper.getLevel().registryAccess()).getCount() == 1,
                    "炉配方身份、时间、经验或输出不符: " + key);
        }
        ResourceLocation pressKey = id("pressing/steel_plate");
        var pressHolder = helper.getLevel().getRecipeManager().byKey(pressKey).orElseThrow();
        require(helper, pressHolder.value() instanceof PressingRecipe, "未加载 Create 钢板压片配方");
        PressingRecipe press = (PressingRecipe) pressHolder.value();
        require(helper, press.getIngredients().size() == 1
                && press.getIngredients().getFirst().test(new ItemStack(steel))
                && !press.getIngredients().getFirst().test(new ItemStack(Items.IRON_INGOT))
                && press.getRollableResults().size() == 1
                && press.getRollableResults().getFirst().getStack().is(plate)
                && press.getRollableResults().getFirst().getStack().getCount() == 1
                && press.getRollableResults().getFirst().getChance() == 1,
                "钢板压片输入或输出不符");
        verified(helper, "loaded-steel-route");
    }

    /** 新粉碎路线不能覆盖 Create 原有的煤与木炭磨石染料配方。 */
    @GameTest(template = TEMPLATE)
    public static void originalCoalAndCharcoalMillingDyesRemain(GameTestHelper helper) {
        for (String source : List.of("coal", "charcoal")) {
            ResourceLocation key = ResourceLocation.fromNamespaceAndPath("create", "milling/" + source);
            var holder = helper.getLevel().getRecipeManager().byKey(key).orElseThrow();
            require(helper, holder.value() instanceof MillingRecipe, "原有磨石配方丢失: " + key);
            MillingRecipe recipe = (MillingRecipe) holder.value();
            Item input = source.equals("coal") ? Items.COAL : Items.CHARCOAL;
            int black = source.equals("coal") ? 2 : 1;
            require(helper, recipe.matches(new SingleRecipeInput(new ItemStack(input)), helper.getLevel())
                    && recipe.getRollableResults().size() == 2
                    && recipe.getRollableResults().getFirst().getStack().is(Items.BLACK_DYE)
                    && recipe.getRollableResults().getFirst().getStack().getCount() == black
                    && recipe.getRollableResults().getFirst().getChance() == 1
                    && recipe.getRollableResults().getLast().getStack().is(Items.GRAY_DYE)
                    && recipe.getRollableResults().getLast().getChance() == 0.1f,
                    "原有煤/木炭磨石染料产物改变: " + key);
        }
        verified(helper, "original-coal-charcoal-milling");
    }

    /** 三种原料逐一投入成对粉碎轮，收集真实世界实体核对单份产物。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 750)
    public static void realCrushingWheelsProcessThreeInputs(GameTestHelper helper) {
        BlockPos left = new BlockPos(1, 4, 2);
        BlockPos right = new BlockPos(3, 4, 2);
        BlockPos controller = new BlockPos(2, 4, 2);
        BlockPos leftMotor = new BlockPos(1, 4, 3);
        BlockPos rightMotor = new BlockPos(3, 4, 1);
        helper.setBlock(left, AllBlocks.CRUSHING_WHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(right, AllBlocks.CRUSHING_WHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(leftMotor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        helper.setBlock(rightMotor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        helper.runAfterDelay(5, () -> {
            ((CreativeMotorBlockEntity) helper.getBlockEntity(leftMotor)).generatedSpeed.setValue(128);
            ((CreativeMotorBlockEntity) helper.getBlockEntity(rightMotor)).generatedSpeed.setValue(128);
        });
        for (int i = 0; i < 3; i++) {
            int index = i;
            Item input = List.of(Items.IRON_INGOT, Items.COAL, Items.CHARCOAL).get(i);
            String powder = List.of("iron_dust", "coal_dust", "charcoal_dust").get(i);
            helper.runAfterDelay(30 + i * 210, () -> {
                require(helper, helper.getBlockState(controller).is(AllBlocks.CRUSHING_WHEEL_CONTROLLER.get())
                        && helper.getBlockState(controller).getValue(CrushingWheelControllerBlock.VALID), "粉碎轮未成对成型");
                CrushingWheelControllerBlockEntity be = (CrushingWheelControllerBlockEntity) helper.getBlockEntity(controller);
                require(helper, be.crushingspeed > 0, "粉碎轮未获得真实动力");
                var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(controller), Direction.UP);
                require(helper, handler != null && handler.insertItem(0, new ItemStack(input), false).isEmpty(), "粉碎轮拒收输入: " + input);
                require(helper, be.findRecipe().isPresent() && be.findRecipe().orElseThrow().id().equals(id("crushing/" + powder)),
                        "粉碎轮选择了错误配方: " + powder);
            });
            helper.runAfterDelay(195 + i * 210, () -> {
                BlockPos center = helper.absolutePos(controller);
                List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(center).inflate(2, 5, 2));
                int count = drops.stream().mapToInt(entity -> {
                    require(helper, entity.getItem().is(registered(helper, powder)), "粉碎轮产生了错误副产物: " + entity.getItem());
                    return entity.getItem().getCount();
                }).sum();
                require(helper, count == 1, "真实粉碎产量不是一份: " + powder + " count=" + count);
                drops.forEach(ItemEntity::discard);
                if (index == 2) verified(helper, "three-crushing-wheel-inputs");
            });
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void coalMixerConsumesFourPlusOneWithoutHeat(GameTestHelper helper) { mixWithPowerRecovery(helper, "coal"); }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void charcoalMixerConsumesFourPlusOneWithoutHeat(GameTestHelper helper) { mixWithPowerRecovery(helper, "charcoal"); }

    /** 停转时完整保存输入；侧齿轮恢复动力后须精确消耗四份铁粉和一份碳粉。 */
    private static void mixWithPowerRecovery(GameTestHelper helper, String carbon) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        putMixInputs(helper, basin, 4, carbon);
        helper.runAfterDelay(45, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 4
                    && inputCount(basin, registered(helper, carbon + "_dust")) == 1
                    && outputCount(basin, registered(helper, "steel_dust")) == 0,
                    "无动力时搅拌盆提前扣料: " + carbon);
            powerMixer(helper, 256);
        });
        helper.runAfterDelay(220, () -> {
            require(helper, ((MechanicalMixerBlockEntity) helper.getBlockEntity(MIXER)).getSpeed() != 0,
                    "真实搅拌机未获得动力: " + carbon);
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 0
                    && inputCount(basin, registered(helper, carbon + "_dust")) == 0
                    && outputCount(basin, registered(helper, "steel_dust")) == 5,
                    "搅拌盆未按4+1精确得到五份钢粉: " + carbon);
            verified(helper, "mixer-4iron-1" + carbon + "-5steel");
        });
    }

    /** 三份铁粉、错用铁锭或缺少碳粉时均不可扣料，补齐后才能加工。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 450)
    public static void mixerRejectsInsufficientAndWrongInputs(GameTestHelper helper) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        basin.getInputInventory().setItem(0, new ItemStack(registered(helper, "iron_dust"), 3));
        basin.getInputInventory().setItem(1, new ItemStack(registered(helper, "coal_dust")));
        basin.notifyChangeOfContents();
        powerMixer(helper, 256);
        helper.runAfterDelay(120, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 3
                    && inputCount(basin, registered(helper, "coal_dust")) == 1
                    && outputCount(basin, registered(helper, "steel_dust")) == 0, "三份铁粉被错误加工");
            basin.getInputInventory().setItem(2, new ItemStack(Items.IRON_INGOT));
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(230, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 3
                    && inputCount(basin, registered(helper, "coal_dust")) == 1
                    && inputCount(basin, Items.IRON_INGOT) == 1
                    && outputCount(basin, registered(helper, "steel_dust")) == 0, "铁锭被当作第四份铁粉");
            basin.getInputInventory().setItem(2, new ItemStack(registered(helper, "iron_dust")));
            basin.getInputInventory().setItem(1, ItemStack.EMPTY);
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(320, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 4
                    && outputCount(basin, registered(helper, "steel_dust")) == 0, "缺碳仍加工");
            basin.getInputInventory().setItem(1, new ItemStack(registered(helper, "coal_dust")));
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(440, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 0
                    && outputCount(basin, registered(helper, "steel_dust")) == 5, "补足原料后未恢复加工");
            verified(helper, "mixer-insufficient-iron-ingot-missing-carbon-recovery");
        });
    }

    /** 其他粉末和错误碳粉均不能凑成钢；改成两种正确粉末后才允许一批加工。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 390)
    public static void mixerRejectsOtherPowderAndWrongCarbon(GameTestHelper helper) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        basin.getInputInventory().setItem(0, new ItemStack(registered(helper, "iron_dust"), 3));
        basin.getInputInventory().setItem(1, new ItemStack(Items.REDSTONE));
        basin.getInputInventory().setItem(2, new ItemStack(registered(helper, "coal_dust")));
        basin.notifyChangeOfContents();
        powerMixer(helper, 256);
        helper.runAfterDelay(110, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 3
                    && inputCount(basin, Items.REDSTONE) == 1
                    && inputCount(basin, registered(helper, "coal_dust")) == 1
                    && outputCount(basin, registered(helper, "steel_dust")) == 0, "其他粉末被当作第四份铁粉");
            basin.getInputInventory().setItem(1, new ItemStack(registered(helper, "iron_dust")));
            basin.getInputInventory().setItem(2, new ItemStack(Items.REDSTONE));
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(220, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 4
                    && inputCount(basin, Items.REDSTONE) == 1
                    && outputCount(basin, registered(helper, "steel_dust")) == 0, "错误碳粉触发了钢配方");
            basin.getInputInventory().setItem(2, new ItemStack(registered(helper, "coal_dust")));
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(370, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 0
                    && inputCount(basin, registered(helper, "coal_dust")) == 0
                    && outputCount(basin, registered(helper, "steel_dust")) == 5, "修正错粉后未加工正确一批");
            verified(helper, "mixer-wrong-powder-wrong-carbon-recovery");
        });
    }

    /** 搅拌未完成时切断轴动力，恢复后只执行一批。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void mixerStopsAndResumesWithoutDuplicateOutput(GameTestHelper helper) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        putMixInputs(helper, basin, 4, "coal");
        powerMixer(helper, 256);
        helper.runAfterDelay(12, () -> motor(helper, MIXER_MOTOR).generatedSpeed.setValue(0));
        helper.runAfterDelay(90, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 4
                    && inputCount(basin, registered(helper, "coal_dust")) == 1
                    && outputCount(basin, registered(helper, "steel_dust")) == 0, "停机过程中提前消费或复制钢粉");
            motor(helper, MIXER_MOTOR).generatedSpeed.setValue(256);
        });
        helper.runAfterDelay(260, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 0
                    && outputCount(basin, registered(helper, "steel_dust")) == 5, "动力恢复后未精确加工一批");
            verified(helper, "mixer-power-interruption-recovery");
        });
    }

    /** 盆的九个输出槽占满时先保持原料，腾出槽后才允许事务提交。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 390)
    public static void blockedMixerOutputPreservesInputUntilSpaceReturns(GameTestHelper helper) {
        setupMixer(helper);
        BasinBlockEntity basin = basin(helper);
        for (int i = 0; i < basin.getOutputInventory().getSlots(); i++)
            basin.getOutputInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        putMixInputs(helper, basin, 4, "coal");
        powerMixer(helper, 256);
        helper.runAfterDelay(170, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 4
                    && inputCount(basin, registered(helper, "coal_dust")) == 1
                    && outputCount(basin, registered(helper, "steel_dust")) == 0, "输出满时搅拌吞料或越槽产出");
            basin.getOutputInventory().setItem(0, ItemStack.EMPTY);
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(350, () -> {
            require(helper, inputCount(basin, registered(helper, "iron_dust")) == 0
                    && inputCount(basin, registered(helper, "coal_dust")) == 0
                    && outputCount(basin, registered(helper, "steel_dust")) == 5, "解除输出堵塞后产量不守恒");
            verified(helper, "mixer-blocked-output-recovery");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void steelDustSmeltsInRealFurnace(GameTestHelper helper) { cook(helper, false); }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void steelDustBlastsInRealBlastFurnace(GameTestHelper helper) { cook(helper, true); }

    /** 炉子在配方时长前保留输入；完成后只能得到一份钢锭。 */
    private static void cook(GameTestHelper helper, boolean blast) {
        helper.setBlock(FURNACE, blast ? Blocks.BLAST_FURNACE : Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) helper.getBlockEntity(FURNACE);
        furnace.setItem(0, new ItemStack(registered(helper, "steel_dust")));
        furnace.setItem(1, new ItemStack(Items.COAL));
        int duration = blast ? 100 : 200;
        helper.runAfterDelay(duration - 2, () -> require(helper, furnace.getItem(0).is(registered(helper, "steel_dust"))
                && furnace.getItem(2).isEmpty(), "真实炉子提前完成制钢"));
        helper.runAfterDelay(duration + 3, () -> {
            require(helper, furnace.getItem(0).isEmpty() && furnace.getItem(2).is(registered(helper, "steel_ingot"))
                    && furnace.getItem(2).getCount() == 1, "真实炉子未产一钢锭");
            verified(helper, blast ? "steel-dust-blast-furnace" : "steel-dust-furnace");
        });
    }

    /** 熔岩气流实际穿过置物台上的单件钢粉并熔炼；停机前不加工。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 440)
    public static void lavaFanSmeltsSteelDustOnDepot(GameTestHelper helper) {
        BlockPos fan = new BlockPos(1, 2, 2);
        BlockPos motorPos = fan.west();
        BlockPos lava = fan.east();
        BlockPos depot = lava.east().below();
        helper.setBlock(fan, AllBlocks.ENCASED_FAN.getDefaultState().setValue(EncasedFanBlock.FACING, Direction.EAST));
        helper.setBlock(lava, Blocks.LAVA);
        helper.setBlock(depot, AllBlocks.DEPOT.get());
        helper.setBlock(lava.east(2), Blocks.STONE);
        helper.runAfterDelay(5, () -> {
            var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(depot), Direction.UP);
            require(helper, handler != null && handler.insertItem(0, new ItemStack(registered(helper, "steel_dust")), false).isEmpty(),
                    "置物台未收下钢粉");
        });
        helper.runAfterDelay(45, () -> {
            require(helper, ((DepotBlockEntity) helper.getBlockEntity(depot)).getHeldItem().is(registered(helper, "steel_dust")),
                    "无动力风扇提前加工");
            helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
        });
        helper.runAfterDelay(50, () -> motor(helper, motorPos).generatedSpeed.setValue(256));
        helper.runAfterDelay(80, () -> {
            EncasedFanBlockEntity fanEntity = (EncasedFanBlockEntity) helper.getBlockEntity(fan);
            require(helper, fanEntity.getSpeed() != 0 && fanEntity.airCurrent.getTypeAt(1.5f) == AllFanProcessingTypes.BLASTING,
                    "熔岩未进入有动力风扇的真实气流");
        });
        helper.runAfterDelay(390, () -> {
            ItemStack held = ((DepotBlockEntity) helper.getBlockEntity(depot)).getHeldItem();
            require(helper, held.is(registered(helper, "steel_ingot")) && held.getCount() == 1,
                    "Create 熔岩风扇未产一钢锭");
            verified(helper, "lava-fan-single-steel-dust");
        });
    }

    /** 真实压片机在无动力时保留钢锭，驱动后产旧身份的维修钢板。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 190)
    public static void realPressMakesExistingSteelPlate(GameTestHelper helper) {
        helper.setBlock(DEPOT, AllBlocks.DEPOT.get());
        helper.setBlock(PRESS, AllBlocks.MECHANICAL_PRESS.getDefaultState()
                .setValue(HorizontalKineticBlock.HORIZONTAL_FACING, Direction.EAST));
        helper.runAfterDelay(5, () -> {
            var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(DEPOT), Direction.UP);
            require(helper, handler != null && handler.insertItem(0, new ItemStack(registered(helper, "steel_ingot")), false).isEmpty(),
                    "置物台未收下钢锭");
        });
        helper.runAfterDelay(45, () -> {
            require(helper, ((DepotBlockEntity) helper.getBlockEntity(DEPOT)).getHeldItem().is(registered(helper, "steel_ingot")),
                    "压片机停机时提前加工");
            helper.setBlock(PRESS_MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                    .setValue(CreativeMotorBlock.FACING, Direction.EAST));
        });
        helper.runAfterDelay(50, () -> motor(helper, PRESS_MOTOR).generatedSpeed.setValue(256));
        helper.runAfterDelay(160, () -> {
            ItemStack held = ((DepotBlockEntity) helper.getBlockEntity(DEPOT)).getHeldItem();
            require(helper, held.is(registered(helper, "steel_plate")) && held.getCount() == 1,
                    "真实压片机未产原身份钢板");
            verified(helper, "press-existing-steel-plate");
        });
    }

    /** 外部测试包增删 flint 后，由真实 /reload 更新配方匹配并验证固定钢锭产物。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void externalSteelPowderFollowsActualReload(GameTestHelper helper) {
        Item substitute = Items.FLINT;
        boolean external = new ItemStack(substitute).is(tag("dusts/steel"));
        AbstractCookingRecipe recipe = (AbstractCookingRecipe) helper.getLevel().getRecipeManager()
                .byKey(id("smelting/steel_ingot_from_dust")).orElseThrow().value();
        require(helper, recipe.matches(new SingleRecipeInput(new ItemStack(substitute)), helper.getLevel()) == external,
                "重载后的等价钢粉标签与炉配方不一致");
        LOGGER.info("MATERIAL_03_RELOAD_RESULT external={} substitute={} matchedExpected=true", external,
                BuiltInRegistries.ITEM.getKey(substitute));
        if (!external) {
            verified(helper, "external-steel-powder-absent");
            return;
        }
        helper.setBlock(FURNACE, Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) helper.getBlockEntity(FURNACE);
        furnace.setItem(0, new ItemStack(substitute));
        furnace.setItem(1, new ItemStack(Items.COAL));
        helper.runAfterDelay(205, () -> {
            require(helper, furnace.getItem(0).isEmpty()
                    && furnace.getItem(2).is(registered(helper, "steel_ingot"))
                    && furnace.getItem(2).getCount() == 1, "外部等价钢粉没有经真实炉子产一钢锭");
            LOGGER.info("MATERIAL_03_RELOAD_COOK external=true result=steel_ingot count=1");
            verified(helper, "external-steel-powder-furnace");
        });
    }

    /** 盆与搅拌机间保留一格空气；侧向小齿轮从上方电机取动力。 */
    private static void setupMixer(GameTestHelper helper) {
        helper.setBlock(BASIN, AllBlocks.BASIN.get());
        helper.setBlock(MIXER, AllBlocks.MECHANICAL_MIXER.get());
        helper.setBlock(COG, AllBlocks.COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
    }

    private static void powerMixer(GameTestHelper helper, int speed) {
        helper.setBlock(MIXER_MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.DOWN));
        motor(helper, MIXER_MOTOR).generatedSpeed.setValue(speed);
    }

    private static CreativeMotorBlockEntity motor(GameTestHelper helper, BlockPos pos) {
        return (CreativeMotorBlockEntity) helper.getBlockEntity(pos);
    }

    private static BasinBlockEntity basin(GameTestHelper helper) {
        return (BasinBlockEntity) helper.getBlockEntity(BASIN);
    }

    private static void putMixInputs(GameTestHelper helper, BasinBlockEntity basin, int iron, String carbon) {
        basin.getInputInventory().setItem(0, new ItemStack(registered(helper, "iron_dust"), iron));
        basin.getInputInventory().setItem(1, new ItemStack(registered(helper, carbon + "_dust")));
        basin.notifyChangeOfContents();
    }

    private static int inputCount(BasinBlockEntity basin, Item item) {
        int count = 0;
        for (int i = 0; i < basin.getInputInventory().getSlots(); i++)
            if (basin.getInputInventory().getStackInSlot(i).is(item)) count += basin.getInputInventory().getStackInSlot(i).getCount();
        return count;
    }

    private static int outputCount(BasinBlockEntity basin, Item item) {
        int count = 0;
        for (int i = 0; i < basin.getOutputInventory().getSlots(); i++)
            if (basin.getOutputInventory().getStackInSlot(i).is(item)) count += basin.getOutputInventory().getStackInSlot(i).getCount();
        return count;
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

    /** 普通服内置成功报告不输出日志；所有断言完成并标记通过后再记录本批证据。 */
    private static void verified(GameTestHelper helper, String scenario) {
        helper.succeed();
        LOGGER.info("MATERIAL_03_TEST_RESULT scenario={} gameTime={} assertions=passed", scenario,
                helper.getLevel().getGameTime());
    }
}
