package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.iksxh.create_nuclear_industry.production.CentrifugeBlock;
import com.iksxh.create_nuclear_industry.production.CentrifugeBlockEntity;
import com.iksxh.create_nuclear_industry.production.CentrifugeUpperProxyBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 使用Create真实管道、机械泵和两格世界结构复现上段接管。 */
@GameTestHolder("create_nuclear_industry_01e")
@PrefixGameTestTemplate(false)
public final class ExtensionCentrifugeUpperPipeGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos LOWER = new BlockPos(3, 2, 3);
    private static final BlockPos UPPER = LOWER.above();
    private static final BlockPos TOP_PIPE = UPPER.above();
    private static final BlockPos PUMP = TOP_PIPE.above();
    private static final BlockPos SOURCE_PIPE = PUMP.above();
    private static final BlockPos SOURCE_TANK = SOURCE_PIPE.above();
    private static final BlockPos POWER_COG = PUMP.east();
    private static final BlockPos POWER_SHAFT = POWER_COG.above();
    private static final BlockPos MOTOR = POWER_SHAFT.above();

    private ExtensionCentrifugeUpperPipeGameTests() {}

    /** Create须能识别上段全部五个物料面，并将真实管网中的1000mB料浆送入机器。 */
    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry_01e", timeoutTicks = 400)
    public static void createPipeConnectsToUpperCentrifugeAndTransfersSlurry(GameTestHelper helper) {
        BlockState lowerState = FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get().defaultBlockState()
                .setValue(CentrifugeBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upperState = lowerState.setValue(CentrifugeBlock.HALF, DoubleBlockHalf.UPPER);
        helper.setBlock(LOWER, lowerState);
        helper.setBlock(UPPER, upperState);
        if (!(helper.getBlockEntity(LOWER) instanceof CentrifugeBlockEntity machine)) {
            helper.fail("两格离心机下段没有创建唯一状态方块实体");
            return;
        }
        if (!(helper.getBlockEntity(UPPER) instanceof CentrifugeUpperProxyBlockEntity)) {
            helper.fail("上段没有自动创建无状态 capability 代理，旧存档管道无法发现端点");
            return;
        }
        if (FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get().getTicker(helper.getLevel(), upperState,
                FuelProcessingContent.CENTRIFUGE_UPPER_PROXY_BE.get()) != null) {
            helper.fail("上段代理意外取得 Create kinetic ticker");
            return;
        }
        // 模拟01D旧区块没有上段BE标签；Create读取端点时应按当前方块状态补建代理。
        helper.getLevel().removeBlockEntity(helper.absolutePos(UPPER));
        machine.setPaired(true);

        for (Direction portFace : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH,
                Direction.WEST, Direction.EAST}) {
            Direction pipeTowardMachine = portFace.getOpposite();
            if (!FluidPipeBlock.canConnectTo(helper.getLevel(), helper.absolutePos(UPPER),
                    upperState, pipeTowardMachine)) {
                helper.fail("Create没有识别上段 " + portFace + " 物料面为流体管道端点");
                return;
            }
        }
        if (!(helper.getBlockEntity(UPPER) instanceof CentrifugeUpperProxyBlockEntity)) {
            helper.fail("Create查询旧区块上段端点后没有补建无状态代理BE");
            return;
        }

        helper.setBlock(SOURCE_TANK, AllBlocks.FLUID_TANK.get().defaultBlockState());
        IFluidHandler source = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(SOURCE_TANK), Direction.DOWN);
        if (source == null || source.fill(new FluidStack(FuelProcessingContent.URANIUM_SLURRY.get(), 1_000),
                FluidAction.EXECUTE) != 1_000) {
            helper.fail("Create源储罐无法装入1000mB料浆");
            return;
        }

        BlockState pipeState = ((GlassFluidPipeBlock) AllBlocks.GLASS_FLUID_PIPE.get()).defaultBlockState()
                .setValue(GlassFluidPipeBlock.AXIS, Direction.Axis.Y);
        helper.setBlock(TOP_PIPE, pipeState);
        helper.setBlock(SOURCE_PIPE, pipeState);
        helper.setBlock(PUMP, AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
                .setValue(PumpBlock.FACING, Direction.DOWN));
        helper.setBlock(POWER_COG, AllBlocks.COGWHEEL.get().defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
        helper.setBlock(POWER_SHAFT, AllBlocks.SHAFT.get().defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                .setValue(CreativeMotorBlock.FACING, Direction.DOWN));
        for (BlockPos pipe : new BlockPos[]{TOP_PIPE, SOURCE_PIPE}) {
            BlockPos absolute = helper.absolutePos(pipe);
            FluidPropagator.propagateChangedPipe(helper.getLevel(), absolute,
                    helper.getLevel().getBlockState(absolute));
        }

        helper.succeedWhen(() -> {
            IFluidHandler inlet = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                    helper.absolutePos(UPPER), Direction.UP);
            if (inlet == null || inlet.getTanks() == 0) {
                helper.fail("Create管网建立后上段顶面仍没有料浆handler");
                return;
            }
            int received = inlet.getFluidInTank(0).getAmount();
            int remaining = source.getTanks() == 0 ? -1 : source.getFluidInTank(0).getAmount();
            helper.assertTrue(received == 1_000 && remaining == 0
                            && inlet.getFluidInTank(0).is(FuelProcessingContent.URANIUM_SLURRY.get()),
                    "Create管网尚未把1000mB料浆全部送入机器：机器=" + received + "mB，源储罐=" + remaining + "mB");
            helper.succeed();
        });
    }
}
