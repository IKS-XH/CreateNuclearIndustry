package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerPartBlock;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 验证锅炉底层热液入口的层位、方向、共享限流与Create管网生命周期。 */
@GameTestHolder("create_nuclear_industry_boiler_hot_inlet")
@PrefixGameTestTemplate(false)
public final class BoilerHotInletGameTests {
    private static final BlockPos BASE = new BlockPos(4, 2, 4);
    private static final BlockPos CONTROL = BASE.offset(1, 1, 0);
    private static final BlockPos HOT = BASE.offset(3, 0, 0);

    private BoilerHotInletGameTests() {}

    /** 获取端口对外侧提供的流体能力。 */
    private static IFluidHandler handler(GameTestHelper h, BlockPos p, Direction side) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(p), side);
    }

    /** 四向底边多入口共享整炉热液库存，但各入口每tick仍受独立额度限制。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 50)
    public static void bottomPerimeterPortsShareInventoryAndPerPortLimit(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        BlockPos west = BASE.offset(0, 0, 2), east = BASE.offset(4, 0, 2), south = BASE.offset(2, 0, 4);
        h.setBlock(west, BoilerContent.HOT_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.WEST));
        h.setBlock(east, BoilerContent.HOT_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.EAST));
        h.setBlock(south, BoilerContent.HOT_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.SOUTH));
        h.runAfterDelay(4, () -> {
            var owner = (BoilerControllerBlockEntity) h.getBlockEntity(CONTROL);
            var form = owner.currentForm();
            h.assertTrue(form != null && form.hotPorts().size() == 4, "四向底边热口没有全部加入结构快照");
            var fluid = new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 256);
            BlockPos[] ports = {HOT, west, east, south};
            Direction[] sides = {Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH};
            int accepted = 0;
            for (int i = 0; i < ports.length; i++) {
                IFluidHandler cap = handler(h, ports[i], sides[i]);
                h.assertTrue(cap != null, "合法底边热口缺少外侧流体能力：" + sides[i]);
                accepted += cap.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
            }
            h.assertTrue(accepted == 1024 && owner.ledger().hot() == 1024, "多口未共享库存或逐口额度异常：" + accepted);
            h.assertTrue(handler(h, HOT, Direction.NORTH).fill(fluid, IFluidHandler.FluidAction.EXECUTE) == 0,
                    "同tick单口重复调用绕过额度");
            h.succeed();
        });
    }

    /** 较高层、错向、角点和非底层棱边端口均不得借底边例外成型。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 50)
    public static void rejectsHigherWrongFacingCornerAndOtherEdgePorts(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        h.runAfterDelay(4, () -> {
            var owner = (BoilerControllerBlockEntity) h.getBlockEntity(CONTROL);
            BlockPos[] invalid = {BASE.offset(3, 1, 0), BASE.offset(3, 0, 0), BASE, BASE.offset(0, 1, 0)};
            Direction[] facing = {Direction.NORTH, Direction.SOUTH, Direction.NORTH, Direction.WEST};
            for (int i = 0; i < invalid.length; i++) {
                BlockPos p = invalid[i];
                var original = h.getBlockState(p);
                h.setBlock(p, BoilerContent.HOT_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, facing[i]));
                owner.invalidateForm();
                h.assertTrue(owner.currentForm() == null, "非法热口位置或朝向仍允许结构成型：" + p);
                h.setBlock(p, original);
                owner.invalidateForm();
                h.assertTrue(owner.currentForm() != null, "移除非法端口后合法结构无法恢复：" + p);
            }
            BlockPos otherPart = BASE.offset(2, 0, 0);
            var original = h.getBlockState(otherPart);
            h.setBlock(otherPart, BoilerContent.WATER_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.NORTH));
            owner.invalidateForm();
            h.assertTrue(owner.currentForm() == null, "底层非角点外壳位置错误接受了非热液部件");
            h.setBlock(otherPart, original);
            owner.invalidateForm();
            h.assertTrue(owner.currentForm() != null, "恢复底层外壳后结构无法成型");
            h.succeed();
        });
    }

    /** 真实Create泵送入底边热口；拆除端口时撤销能力，恢复后总冷却剂量守恒。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 180)
    public static void poweredCreatePipeFeedsBottomPortAndRecoversAfterReassembly(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        BlockPos nearPipe = HOT.north(), pumpPos = HOT.north(2), sourcePipe = HOT.north(3), tankPos = HOT.north(4);
        BlockPos cog = pumpPos.east(), shaft = cog.north(), motor = shaft.north();
        h.setBlock(tankPos, AllBlocks.FLUID_TANK.get());
        IFluidHandler source = handler(h, tankPos, Direction.NORTH);
        int loaded = source.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 2000), IFluidHandler.FluidAction.EXECUTE);
        h.assertTrue(loaded == 2000, "Create源储罐未接收测试热液");
        h.setBlock(nearPipe, AllBlocks.FLUID_PIPE.get());
        h.setBlock(sourcePipe, AllBlocks.FLUID_PIPE.get());
        h.setBlock(pumpPos, AllBlocks.MECHANICAL_PUMP.get().defaultBlockState().setValue(PumpBlock.FACING, Direction.SOUTH));
        h.setBlock(cog, AllBlocks.COGWHEEL.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        h.setBlock(shaft, AllBlocks.SHAFT.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        h.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity) h.getBlockEntity(motor)).generatedSpeed.setValue(256);
        for (BlockPos p : new BlockPos[]{nearPipe, sourcePipe})
            FluidPropagator.propagateChangedPipe(h.getLevel(), h.absolutePos(p), h.getLevel().getBlockState(h.absolutePos(p)));

        h.runAfterDelay(35, () -> {
            var owner = (BoilerControllerBlockEntity) h.getBlockEntity(CONTROL);
            IFluidHandler oldPort = handler(h, HOT, Direction.NORTH);
            h.assertTrue(owner.currentForm() != null && oldPort != null, "底边热口没有形成或暴露能力");
            var pumpEntity = h.getBlockEntity(pumpPos);
            h.assertTrue(pumpEntity instanceof PumpBlockEntity && ((PumpBlockEntity) pumpEntity).getSpeed() != 0,
                    "真实Create机械泵未获得动力");
            h.assertTrue(owner.ledger().hot() + owner.ledger().cold() > 0,
                    "真实Create泵/管道没有向底边入口送入热液；源库存=" + source.getFluidInTank(0).getAmount());
            int total = owner.ledger().hot() + owner.ledger().cold() + source.getFluidInTank(0).getAmount();
            h.setBlock(HOT, BoilerContent.CASING.get());
            owner.invalidateForm();
            h.assertTrue(owner.currentForm() == null && handler(h, HOT, Direction.NORTH) == null,
                    "拆除底边热口后结构能力未撤销");
            h.assertTrue(oldPort.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "拆除后旧能力仍可写入");
            h.assertTrue(owner.ledger().hot() + owner.ledger().cold() + source.getFluidInTank(0).getAmount() == total,
                    "拆除热口改变冷却剂总量");
            h.setBlock(HOT, BoilerContent.HOT_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.NORTH));
            owner.invalidateForm();
            h.assertTrue(owner.currentForm() != null && handler(h, HOT, Direction.NORTH) != null,
                    "恢复底边热口后能力未恢复");
            h.assertTrue(oldPort.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "恢复后旧代次能力复活");
            h.assertTrue(owner.ledger().hot() + owner.ledger().cold() + source.getFluidInTank(0).getAmount() == total,
                    "恢复热口改变冷却剂总量");
            h.succeed();
        });
    }
}
