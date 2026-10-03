package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.mojang.logging.LogUtils;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.recipe.HeatCondition;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;

import java.util.List;

/** 验证青金石粉配方、原磨石路线及无加热冷却剂的真实 Create 机器事务。 */
@GameTestHolder("create_nuclear_industry_coolant")
@PrefixGameTestTemplate(false)
public final class ExtensionCoolantProductionGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEMPLATE = "p0_probe_empty";
    private static final String COMPAT_PACK = "file/coolant-compat-pack";
    private static final BlockPos BASIN = new BlockPos(2, 1, 2);
    private static final BlockPos MIXER = BASIN.above(2);
    private static final BlockPos COG = MIXER.west();
    private static final BlockPos MOTOR = COG.above();

    private ExtensionCoolantProductionGameTests() {}

    /** 注册、标签、加载配方的产量/过滤及冷态流体身份须与批准配比一致。 */
    @GameTest(template = TEMPLATE)
    public static void loadedCoolantRouteHasExactInputsAndOutputs(GameTestHelper helper) {
        Item lapisDust = registered(helper, "lapis_dust");
        require(helper, lapisDust.getDefaultMaxStackSize() == 64
                        && new ItemStack(lapisDust).is(itemTag("dusts/lapis"))
                        && new ItemStack(lapisDust).is(itemTag("dusts")),
                "青金石粉未作为普通物品进入细分及父标签");
        require(helper, !new ItemStack(Items.LAPIS_LAZULI).is(itemTag("dusts/lapis"))
                        && !new ItemStack(Items.BLUE_DYE).is(itemTag("dusts/lapis"))
                        && !new ItemStack(registered(helper, "iron_dust")).is(itemTag("dusts/lapis")),
                "原青金石、染料或错误粉末污染青金石粉标签");
        require(helper, new ItemStack(Items.REDSTONE).is(itemTag("dusts"))
                        && new ItemStack(Items.GLOWSTONE_DUST).is(itemTag("dusts")),
                "父粉末标签未保留NeoForge提供的红石/荧石成员");

        var crushingHolder = helper.getLevel().getRecipeManager().byKey(id("crushing/lapis_dust")).orElseThrow();
        require(helper, crushingHolder.value() instanceof CrushingRecipe, "青金石制粉不是 Create 粉碎配方");
        CrushingRecipe crushing = (CrushingRecipe) crushingHolder.value();
        require(helper, crushing.getProcessingDuration() == 100
                        && crushing.matches(new SingleRecipeInput(new ItemStack(Items.LAPIS_LAZULI)), helper.getLevel())
                        && !crushing.matches(new SingleRecipeInput(new ItemStack(Items.LAPIS_BLOCK)), helper.getLevel())
                        && crushing.getRollableResults().size() == 1
                        && crushing.getRollableResults().getFirst().getStack().is(lapisDust)
                        && crushing.getRollableResults().getFirst().getStack().getCount() == 1
                        && crushing.getRollableResults().getFirst().getChance() == 1,
                "青金石粉碎的输入、参数或1:1输出不符");

        var mixingHolder = helper.getLevel().getRecipeManager().byKey(id("mixing/compound_coolant")).orElseThrow();
        require(helper, mixingHolder.value() instanceof MixingRecipe, "复合冷却剂不是 Create 搅拌配方");
        MixingRecipe mixing = (MixingRecipe) mixingHolder.value();
        require(helper, mixing.getProcessingDuration() == 100 && mixing.getRequiredHeat() == HeatCondition.NONE
                        && mixing.getIngredients().size() == 3 && mixing.getFluidIngredients().size() == 1,
                "冷却剂配方时长、无热要求或材料份数错误");
        require(helper, mixing.getIngredients().get(0).test(new ItemStack(lapisDust))
                        && !mixing.getIngredients().get(0).test(new ItemStack(Items.LAPIS_LAZULI))
                        && !mixing.getIngredients().get(0).test(new ItemStack(Items.BLUE_DYE))
                        && mixing.getIngredients().get(1).test(new ItemStack(Items.REDSTONE))
                        && mixing.getIngredients().get(2).test(new ItemStack(Items.GLOWSTONE_DUST)),
                "三种粉末标签或错误青金石形态匹配不符");
        require(helper, mixing.getFluidIngredients().getFirst().test(new FluidStack(Fluids.WATER, 1000))
                        && !mixing.getFluidIngredients().getFirst().test(new FluidStack(Fluids.LAVA, 1000))
                        && mixing.getFluidResults().size() == 1
                        && BuiltInRegistries.FLUID.getKey(mixing.getFluidResults().getFirst().getFluid())
                        .equals(id("compound_coolant"))
                        && mixing.getFluidResults().getFirst().getAmount() == 1000,
                "水输入或1000mB冷态冷却剂输出错误");
        helper.succeed();
    }

    /** 隔离数据包模拟第三方向细分标签添加等价粉末，并确认搅拌 Ingredient 随重载更新。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180, batch = "externalCoolantTags")
    public static void simulatedExternalPowderTagMemberMatchesAfterReload(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var commands = server.getCommands();
        var source = server.createCommandSourceStack();
        commands.performPrefixedCommand(source, "datapack disable \"" + COMPAT_PACK + "\"");
        commands.performPrefixedCommand(source, "reload");
        helper.runAfterDelay(45, () -> {
            require(helper, !new ItemStack(Items.FLINT).is(itemTag("dusts/lapis")),
                    "模拟第三方青金石粉在关闭时仍留在标签中");
            commands.performPrefixedCommand(source, "datapack enable \"" + COMPAT_PACK + "\"");
            commands.performPrefixedCommand(source, "reload");
        });
        helper.runAfterDelay(95, () -> {
            require(helper, new ItemStack(Items.FLINT).is(itemTag("dusts/lapis")),
                    "隔离数据包重载后没有将模拟等价粉末加入标签");
            MixingRecipe recipe = (MixingRecipe) helper.getLevel().getRecipeManager()
                    .byKey(id("mixing/compound_coolant")).orElseThrow().value();
            require(helper, recipe.getIngredients().getFirst().test(new ItemStack(Items.FLINT)),
                    "冷却剂配方没有接受细分标签中的等价粉末");
            LOGGER.info("COOLANT_EXTERNAL_TAG_SIMULATION member=minecraft:flint tag=c:dusts/lapis matched=true");
            commands.performPrefixedCommand(source, "datapack disable \"" + COMPAT_PACK + "\"");
            commands.performPrefixedCommand(source, "reload");
        });
        helper.runAfterDelay(145, () -> {
            require(helper, !new ItemStack(Items.FLINT).is(itemTag("dusts/lapis")),
                    "隔离模拟数据包关闭后标签未恢复");
            helper.succeed();
        });
    }

    /** 两只真实粉碎轮在动力下把单颗原版青金石加工成单份青金石粉。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void realCrushingWheelsMakeOneLapisDust(GameTestHelper helper) {
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
            ((CreativeMotorBlockEntity) helper.getBlockEntity(leftMotor)).generatedSpeed.setValue(128);
            ((CreativeMotorBlockEntity) helper.getBlockEntity(rightMotor)).generatedSpeed.setValue(128);
        });
        helper.runAfterDelay(30, () -> {
            require(helper, helper.getBlockState(controller).is(AllBlocks.CRUSHING_WHEEL_CONTROLLER.get())
                            && helper.getBlockState(controller).getValue(CrushingWheelControllerBlock.VALID),
                    "粉碎轮未成对成型");
            CrushingWheelControllerBlockEntity crusher = (CrushingWheelControllerBlockEntity) helper.getBlockEntity(controller);
            require(helper, crusher.crushingspeed > 0, "粉碎轮没有真实动力");
            var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(controller), Direction.UP);
            require(helper, handler != null && handler.insertItem(0, new ItemStack(Items.LAPIS_LAZULI), false).isEmpty(),
                    "粉碎轮拒收原版青金石");
            require(helper, crusher.findRecipe().isPresent()
                            && crusher.findRecipe().orElseThrow().id().equals(id("crushing/lapis_dust")),
                    "粉碎轮选择了非本批配方");
        });
        helper.runAfterDelay(200, () -> {
            BlockPos center = helper.absolutePos(controller);
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(center).inflate(2, 5, 2));
            int count = 0;
            for (ItemEntity drop : drops) {
                require(helper, drop.getItem().is(registered(helper, "lapis_dust")),
                        "粉碎轮出现非青金石粉副产物: " + drop.getItem());
                count += drop.getItem().getCount();
            }
            require(helper, count == 1, "真实粉碎轮没有精确产出一份青金石粉: " + count);
            helper.succeed();
        });
    }

    /** 新增粉碎配方后，Create 原有磨石仍把青金石加工成蓝色染料。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void realMillstoneKeepsLapisToBlueDyeRoute(GameTestHelper helper) {
        BlockPos millPos = new BlockPos(2, 1, 2);
        BlockPos motorPos = millPos.below();
        var holder = helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath("create", "milling/lapis_lazuli")).orElseThrow();
        require(helper, holder.value() instanceof MillingRecipe
                        && ((MillingRecipe) holder.value()).matches(
                        new SingleRecipeInput(new ItemStack(Items.LAPIS_LAZULI)), helper.getLevel()),
                "Create 原有青金石磨石配方已被移除");
        helper.setBlock(millPos, AllBlocks.MILLSTONE.get());
        helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.UP));
        MillstoneBlockEntity mill = (MillstoneBlockEntity) helper.getBlockEntity(millPos);
        mill.inputInv.setStackInSlot(0, new ItemStack(Items.LAPIS_LAZULI));
        helper.runAfterDelay(5, () -> ((CreativeMotorBlockEntity) helper.getBlockEntity(motorPos))
                .generatedSpeed.setValue(256));
        helper.runAfterDelay(240, () -> {
            require(helper, mill.getSpeed() != 0 && mill.inputInv.getStackInSlot(0).isEmpty()
                            && containsItem(mill.outputInv, Items.BLUE_DYE),
                    "真实磨石没有消费青金石并产出蓝色染料");
            helper.succeed();
        });
    }

    /** 无燃烧室的真实搅拌机消耗三粉和一桶水，只产一桶现有冷态冷却剂。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void unheatedMixerMakesOneCoolantBatch(GameTestHelper helper) {
        BasinBlockEntity basin = setupMixer(helper);
        setCoolantInputs(helper, basin);
        powerMixer(helper);
        helper.runAfterDelay(220, () -> {
            require(helper, ((MechanicalMixerBlockEntity) helper.getBlockEntity(MIXER)).getSpeed() != 0,
                    "真实搅拌机未获得动力");
            require(helper, count(basin.getInputInventory(), registered(helper, "lapis_dust")) == 0
                            && count(basin.getInputInventory(), Items.REDSTONE) == 0
                            && count(basin.getInputInventory(), Items.GLOWSTONE_DUST) == 0,
                    "真实搅拌机没有精确消耗三种粉末");
            var inputFluids = basin.inputTank.getCapability();
            var outputFluids = outputTank(basin);
            require(helper, inputFluids.getFluidInTank(0).isEmpty()
                            && fluidCount(outputFluids, id("compound_coolant")) == 1000,
                    "真实无热搅拌没有消耗1000mB水并输出1000mB冷却剂");
            require(helper, fluidCount(outputFluids, ResourceLocation.fromNamespaceAndPath("minecraft", "water")) == 0,
                    "输出中残留水或冷却剂身份错误");
            helper.succeed();
        });
    }

    /** 输出罐塞满时三粉与水都保留；清空后原生搅拌事务只产出一桶冷却剂。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 430)
    public static void blockedCoolantOutputPreservesInputsAndResumes(GameTestHelper helper) {
        BasinBlockEntity basin = setupMixer(helper);
        require(helper, basin.acceptOutputs(List.of(), List.of(new FluidStack(Fluids.WATER, 2000)), false),
                "未能在测试盆的输出罐建立流体阻塞条件");
        setCoolantInputs(helper, basin);
        powerMixer(helper);
        helper.runAfterDelay(170, () -> {
            require(helper, count(basin.getInputInventory(), registered(helper, "lapis_dust")) == 1
                            && count(basin.getInputInventory(), Items.REDSTONE) == 1
                            && count(basin.getInputInventory(), Items.GLOWSTONE_DUST) == 1
                            && basin.inputTank.getCapability().getFluidInTank(0).getAmount() == 1000
                            && fluidCount(outputTank(basin), id("compound_coolant")) == 0,
                    "输出受阻时搅拌机扣料或越过满罐输出");
            outputTank(basin).drain(2000, FluidAction.EXECUTE);
            basin.notifyChangeOfContents();
        });
        helper.runAfterDelay(350, () -> {
            require(helper, count(basin.getInputInventory(), registered(helper, "lapis_dust")) == 0
                            && count(basin.getInputInventory(), Items.REDSTONE) == 0
                            && count(basin.getInputInventory(), Items.GLOWSTONE_DUST) == 0
                            && basin.inputTank.getCapability().getFluidInTank(0).isEmpty()
                            && fluidCount(outputTank(basin), id("compound_coolant")) == 1000,
                    "解除输出阻塞后未精确完成一批冷却剂");
            helper.succeed();
        });
    }

    private static BasinBlockEntity setupMixer(GameTestHelper helper) {
        helper.setBlock(BASIN, AllBlocks.BASIN.get());
        helper.setBlock(MIXER, AllBlocks.MECHANICAL_MIXER.get());
        helper.setBlock(COG, AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
        return (BasinBlockEntity) helper.getBlockEntity(BASIN);
    }

    private static void setCoolantInputs(GameTestHelper helper, BasinBlockEntity basin) {
        basin.getInputInventory().setItem(0, new ItemStack(registered(helper, "lapis_dust")));
        basin.getInputInventory().setItem(1, new ItemStack(Items.REDSTONE));
        basin.getInputInventory().setItem(2, new ItemStack(Items.GLOWSTONE_DUST));
        int filled = basin.inputTank.getCapability().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        require(helper, filled == 1000, "搅拌盆拒收1000mB水: " + filled);
        basin.notifyChangeOfContents();
    }

    private static void powerMixer(GameTestHelper helper) {
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.DOWN));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(256);
    }

    private static net.neoforged.neoforge.fluids.capability.IFluidHandler outputTank(BasinBlockEntity basin) {
        return basin.getTanks().getSecond().getCapability();
    }

    private static Item registered(GameTestHelper helper, String path) {
        Item item = registered(path);
        require(helper, item != Items.AIR, "缺少物品注册: " + id(path));
        return item;
    }

    private static Item registered(String path) {
        return BuiltInRegistries.ITEM.get(id(path));
    }

    private static boolean containsItem(net.neoforged.neoforge.items.IItemHandler inventory, Item item) {
        for (int slot = 0; slot < inventory.getSlots(); slot++)
            if (inventory.getStackInSlot(slot).is(item)) return true;
        return false;
    }

    private static int count(net.neoforged.neoforge.items.IItemHandler inventory, Item item) {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static int fluidCount(net.neoforged.neoforge.fluids.capability.IFluidHandler tank, ResourceLocation id) {
        int total = 0;
        for (int slot = 0; slot < tank.getTanks(); slot++) {
            FluidStack fluid = tank.getFluidInTank(slot);
            if (!fluid.isEmpty() && BuiltInRegistries.FLUID.getKey(fluid.getFluid()).equals(id))
                total += fluid.getAmount();
        }
        return total;
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
