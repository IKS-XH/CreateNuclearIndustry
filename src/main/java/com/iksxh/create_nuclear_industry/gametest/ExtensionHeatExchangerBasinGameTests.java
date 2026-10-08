package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.heat.HeatExchangerBasinBridge;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 用真实工作盆验证无搅拌器持续供热、超级热加工及资源中断恢复。 */
@GameTestHolder("create_nuclear_industry_ext_b_basin")
@PrefixGameTestTemplate(false)
public final class ExtensionHeatExchangerBasinGameTests {
    private static final String TEMPLATE = "basin_empty";
    private static final BlockPos BASIN = new BlockPos(3, 2, 3);
    private static final BlockPos EXCHANGER = BASIN.below();
    private static final BlockPos MIXER = BASIN.above(2);
    private static final BlockPos COG = MIXER.west();
    private static final BlockPos MOTOR = COG.above();

    private ExtensionHeatExchangerBasinGameTests() {}

    /** 空盆且无搅拌器仍是持续负载，并在已付款的当前tick发布SEETHING。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void emptyBasinHeatsWithoutMixer(GameTestHelper helper) {
        BasinBlockEntity basin = setupWithoutMixer(helper, 100);
        helper.runAfterDelay(10, () -> {
            NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
            helper.assertTrue(exchanger.ledger().cold() >= 32 && exchanger.ledger().cold() <= 40,
                    "空盆没有按持续费用将核热液等量转成冷液");
            helper.assertTrue(HeatExchangerBasinBridge.heatLevel(basin) == HeatLevel.SEETHING,
                    "无搅拌器的空盆未读取已付款超级热");
            helper.succeed();
        });
    }

    /** 盆存在但热液耗尽时停热，补液后下一tick恢复，不借用HU余热。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void coolantInterruptionStopsAndRestoresBasinHeat(GameTestHelper helper) {
        BasinBlockEntity basin = setupWithoutMixer(helper, 8);
        helper.runAfterDelay(8, () -> {
            NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
            helper.assertTrue(exchanger.ledger().cold() == 8 && exchanger.ledger().hot() == 0,
                    "断流测试夹具未耗尽已输入热液");
            helper.assertTrue(HeatExchangerBasinBridge.heatLevel(basin) == HeatLevel.NONE,
                    "热液断流后仍用余热发布热级");
            helper.assertTrue(exchanger.ledger().fillHot(8, false) == 8, "恢复供液未进入热罐");
        });
        helper.runAfterDelay(10, () -> {
            NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
            helper.assertTrue(exchanger.ledger().cold() > 8 && exchanger.ledger().cold() <= 16
                            && HeatExchangerBasinBridge.heatLevel(basin) == HeatLevel.SEETHING,
                    "恢复供液后未在实际换液付款tick恢复超级热");
            helper.succeed();
        });
    }

    /** 有效加工中持续换液，不按普通热级或批次储备发布。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void heatedRecipeUsesContinuousSuperheat(GameTestHelper helper) {
        BasinBlockEntity basin = setup(helper, 1000);
        basin.getInputInventory().setItem(0, new ItemStack(item("lead_ingot")));
        basin.getInputInventory().setItem(1, new ItemStack(Items.GLASS));
        basin.notifyChangeOfContents();
        helper.runAfterDelay(100, () -> {
            Item shieldedGlass = item("shielded_glass");
            helper.assertTrue(count(basin.getOutputInventory(), shieldedGlass) == 1,
                    "SEETHING未能执行Create原生heated配方");
            NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
            helper.assertTrue(exchanger.ledger().cold() == 400 && exchanger.ledger().reserve() < 1e-9,
                    "默认2HU/t在100tick未持续转液400mB或错误使用了锅炉储备");
            helper.succeed();
        });
    }

    /** 先登记盆与搅拌器、最后登记热源，验证Create先tick操作器时仍可读取最近已付款热级。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void mixerRegisteredBeforeSourceProcessesHeatedRecipe(GameTestHelper helper) {
        BasinBlockEntity basin = setupMixerBeforeSource(helper, 1000);
        basin.getInputInventory().setItem(0, new ItemStack(item("lead_ingot")));
        basin.getInputInventory().setItem(1, new ItemStack(Items.GLASS));
        basin.notifyChangeOfContents();
        helper.runAfterDelay(100, () -> {
            NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
            helper.assertTrue(count(basin.getOutputInventory(), item("shielded_glass")) == 1,
                    "搅拌器先于热源登记后，Create原生heated加工仍未完成");
            helper.assertTrue(exchanger.ledger().cold() == 400,
                    "反向ticker顺序下热源未持续按固定费用等量换液");
            helper.succeed();
        });
    }

    /** 单个Create超级热配方输入仍只产原生50mB；换热器负载连续运行。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void superheatedRecipeUsesContinuousSuperheat(GameTestHelper helper) {
        BasinBlockEntity basin = setup(helper, 1000);
        basin.getInputInventory().setItem(0, new ItemStack(Items.COBBLESTONE));
        basin.notifyChangeOfContents();
        helper.runAfterDelay(100, () -> {
            FluidStack output = basin.getTanks().getSecond().getCapability().getFluidInTank(0);
            NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
            helper.assertTrue(output.is(Fluids.LAVA) && output.getAmount() == 50,
                    "持续SEETHING没有执行原生create:lava_from_cobble配方");
            helper.assertTrue(exchanger.ledger().cold() == 400 && exchanger.ledger().reserve() < 1e-9,
                    "超级热加工期间未按固定2HU/t持续转换等量冷液");
            helper.succeed();
        });
    }

    /** 没有本模组换热器时，原生烈焰人仍能执行普通heated配方。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 430)
    public static void nativeBlazeBurnerKeepsHeatedRecipeProcessing(GameTestHelper helper) {
        BasinBlockEntity basin = setupNativeBurner(helper);
        insertBurnerFuel(helper, Items.COAL);
        basin.getInputInventory().setItem(0, new ItemStack(item("lead_ingot")));
        basin.getInputInventory().setItem(1, new ItemStack(Items.GLASS));
        basin.notifyChangeOfContents();
        helper.runAfterDelay(350, () -> {
            helper.assertTrue(count(basin.getOutputInventory(), item("shielded_glass")) == 1,
                    "本设备之外的原生烈焰人heated加工被拦截；热级="
                            + BlazeBurnerBlock.getHeatLevelOf(helper.getBlockState(EXCHANGER)) + "，搅拌速度="
                            + ((com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity)
                            helper.getBlockEntity(MIXER)).getSpeed() + "，铅锭="
                            + count(basin.getInputInventory(), item("lead_ingot")) + "，玻璃="
                            + count(basin.getInputInventory(), Items.GLASS));
            helper.succeed();
        });
    }

    /** 没有本模组换热器时，原生烈焰人仍能执行超级热加工。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 430)
    public static void nativeBlazeBurnerKeepsSuperheatedRecipeProcessing(GameTestHelper helper) {
        BasinBlockEntity basin = setupNativeBurner(helper);
        insertBurnerFuel(helper, AllItems.BLAZE_CAKE.get());
        basin.getInputInventory().setItem(0, new ItemStack(Items.COBBLESTONE));
        basin.notifyChangeOfContents();
        helper.runAfterDelay(350, () -> {
            FluidStack output = basin.getTanks().getSecond().getCapability().getFluidInTank(0);
            helper.assertTrue(output.is(Fluids.LAVA) && output.getAmount() == 50,
                    "本设备之外的原生烈焰人superheated加工被拦截；热级="
                            + BlazeBurnerBlock.getHeatLevelOf(helper.getBlockState(EXCHANGER)) + "，输出=" + output);
            helper.succeed();
        });
    }

    private static BasinBlockEntity setup(GameTestHelper helper, int hotMb) {
        helper.setBlock(EXCHANGER, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState());
        helper.setBlock(BASIN, AllBlocks.BASIN.get());
        helper.setBlock(MIXER, AllBlocks.MECHANICAL_MIXER.get());
        helper.setBlock(COG, AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.DOWN));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(256);
        NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
        helper.assertTrue(exchanger.ledger().fillHot(hotMb, false) == hotMb, "换热器未接收测试热液库存");
        return (BasinBlockEntity) helper.getBlockEntity(BASIN);
    }

    private static BasinBlockEntity setupWithoutMixer(GameTestHelper helper, int hotMb) {
        helper.setBlock(EXCHANGER, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState());
        helper.setBlock(BASIN, AllBlocks.BASIN.get());
        NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
        helper.assertTrue(exchanger.ledger().fillHot(hotMb, false) == hotMb, "换热器未接收测试热液库存");
        return (BasinBlockEntity) helper.getBlockEntity(BASIN);
    }

    private static BasinBlockEntity setupMixerBeforeSource(GameTestHelper helper, int hotMb) {
        helper.setBlock(BASIN, AllBlocks.BASIN.get());
        helper.setBlock(MIXER, AllBlocks.MECHANICAL_MIXER.get());
        helper.setBlock(COG, AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.DOWN));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(256);
        helper.setBlock(EXCHANGER, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState());
        NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
        helper.assertTrue(exchanger.ledger().fillHot(hotMb, false) == hotMb,
                "搅拌器先登记用例未接收测试热液库存");
        return (BasinBlockEntity) helper.getBlockEntity(BASIN);
    }

    private static BasinBlockEntity setupNativeBurner(GameTestHelper helper) {
        helper.setBlock(BASIN, AllBlocks.BASIN.get());
        helper.setBlock(EXCHANGER, AllBlocks.BLAZE_BURNER.getDefaultState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
        helper.setBlock(MIXER, AllBlocks.MECHANICAL_MIXER.get());
        helper.setBlock(COG, AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.DOWN));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(256);
        return (BasinBlockEntity) helper.getBlockEntity(BASIN);
    }

    private static void insertBurnerFuel(GameTestHelper helper, Item fuelItem) {
        ItemStack fuel = new ItemStack(fuelItem);
        helper.assertTrue(BlazeBurnerBlock.tryInsert(helper.getBlockState(EXCHANGER), helper.getLevel(),
                        helper.absolutePos(EXCHANGER), fuel, false, false, false).getResult().consumesAction()
                        && fuel.isEmpty(),
                "原生测试烈焰人未通过Create投入路径接受燃料");
    }

    private static NuclearHeatExchangerBlockEntity exchanger(GameTestHelper helper) {
        return (NuclearHeatExchangerBlockEntity) helper.getBlockEntity(EXCHANGER);
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", path));
    }

    private static int count(net.neoforged.neoforge.items.IItemHandler inventory, Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }
}
