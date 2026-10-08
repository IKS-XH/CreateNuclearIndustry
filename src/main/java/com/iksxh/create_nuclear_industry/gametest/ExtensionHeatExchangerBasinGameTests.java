package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
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

/** 用真实Create搅拌器与原生配方验证普通/超级盆热的批费和产物。 */
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

    /** 原生shielded_glass批次应先积足40HU，再产出并只扣一批费用。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 430)
    public static void heatedRecipeUsesPaidBatchHeat(GameTestHelper helper) {
        BasinBlockEntity basin = setup(helper, 100);
        basin.getFilter().setFilter(new ItemStack(Items.DIRT));
        basin.getInputInventory().setItem(0, new ItemStack(item("lead_ingot")));
        basin.getInputInventory().setItem(1, new ItemStack(Items.GLASS));
        basin.notifyChangeOfContents();
        helper.runAfterDelay(20, () -> {
            NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
            helper.assertTrue(exchanger.ledger().cold() == 0 && count(basin.getOutputInventory(), item("shielded_glass")) == 0,
                    "Create原生盆过滤拒绝时仍在充热或产出");
            basin.getFilter().setFilter(ItemStack.EMPTY);
            ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(0);
        });
        helper.runAfterDelay(35, () -> {
            NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
            helper.assertTrue(exchanger.ledger().cold() == 0
                            && count(basin.getInputInventory(), item("lead_ingot")) == 1,
                    "真实停转阶段仍在充热或消耗原料");
            ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(256);
        });
        helper.runAfterDelay(140, () -> {
            Item shieldedGlass = item("shielded_glass");
            helper.assertTrue(count(basin.getOutputInventory(), shieldedGlass) == 1,
                    "过滤与停转恢复后原生heated配方没有产出shielded_glass");
            NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
            helper.assertTrue(exchanger.ledger().cold() == 80 && exchanger.ledger().reserve() < 1e-9,
                    "普通批次未按40HU结算或热冷转换不是80mB");
            helper.succeed();
        });
    }

    /** 原生lava_from_cobble批次应达到SEETHING条件并只结算80HU。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void superheatedRecipeUsesPaidBatchHeat(GameTestHelper helper) {
        BasinBlockEntity basin = setup(helper, 160);
        basin.getInputInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        basin.notifyChangeOfContents();
        helper.runAfterDelay(100, () -> {
            FluidStack output = basin.getTanks().getSecond().getCapability().getFluidInTank(0);
            NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
            helper.assertTrue(output.is(Fluids.LAVA) && output.getAmount() == 50,
                    "原生superheated配方没有产出50mB熔岩");
            helper.assertTrue(exchanger.ledger().cold() == 160 && exchanger.ledger().reserve() < 1e-9,
                    "超级批次未按80HU结算或热冷转换不是160mB");
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
