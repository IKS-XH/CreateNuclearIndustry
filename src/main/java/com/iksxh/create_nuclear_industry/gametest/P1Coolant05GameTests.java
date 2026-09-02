package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/** 验证保留冷却端口与 Create 管网时替换仪表端口的绑定和输送恢复。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Coolant05GameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos COLD = new BlockPos(1, 2, 4);
    private static final BlockPos HOT = new BlockPos(3, 2, 4);
    private static final BlockPos HOT_PIPE_NEAR = new BlockPos(3, 2, 5);
    private static final BlockPos HOT_PUMP = new BlockPos(3, 2, 6);
    private static final BlockPos HOT_SINK_PIPE = new BlockPos(3, 2, 7);
    private static final BlockPos HOT_SINK_TANK = new BlockPos(3, 2, 8);
    private static final BlockPos HOT_POWER_COG = new BlockPos(4, 2, 6);
    private static final BlockPos HOT_POWER_SHAFT = new BlockPos(4, 2, 7);
    private static final BlockPos HOT_MOTOR = new BlockPos(4, 2, 8);
    private static final BlockPos PIPE_NEAR = new BlockPos(1, 2, 5);
    private static final BlockPos PUMP = new BlockPos(1, 2, 6);
    private static final BlockPos SOURCE_PIPE = new BlockPos(1, 2, 7);
    private static final BlockPos SOURCE_TANK = new BlockPos(1, 2, 8);
    private static final BlockPos POWER_COG = new BlockPos(2, 2, 6);
    private static final BlockPos POWER_SHAFT = new BlockPos(2, 2, 7);
    private static final BlockPos MOTOR = new BlockPos(2, 2, 8);

    private P1Coolant05GameTests() {
    }

    /** 保留原冷端和完整 Create 管网，替换仪表端口后必须接管绑定且恢复输入。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void replacingInstrumentRetainsPortAndCreateNetwork(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        long startTick = helper.getLevel().getGameTime();
        int[] phase = {0};
        long[] phaseTick = {startTick};
        ReactorInstrumentPortBlockEntity[] oldInstrument = {null};
        IFluidHandler[] oldHandler = {null};
        IFluidHandler[] oldHotHandler = {null};
        long[] initialTotal = {0L};

        helper.succeedWhen(() -> {
            if (phase[0] == 0) {
                if (helper.getLevel().getGameTime() - startTick < 5L) {
                    return;
                }
                ReactorInstrumentPortBlockEntity instrument = instrument(helper);
                ReactorPortBlockEntity cold = coldPort(helper);
                require(helper, instrument.structureValid(), "替换仪表前反应堆未成型");
                require(helper, cold.isBoundTo(instrument,
                                ReactorPortBlockEntity.BindingType.COLD_COOLANT, null),
                        "替换仪表前冷端没有绑定到旧仪表端口");

                buildCreateNetwork(helper);
                oldInstrument[0] = instrument;
                oldHandler[0] = coldHandler(helper);
                oldHotHandler[0] = hotHandler(helper);
                require(helper, oldHandler[0] != null, "替换仪表前冷端 capability 不可用");
                require(helper, oldHotHandler[0] != null, "替换仪表前热端 capability 不可用");
                initialTotal[0] = sourceAmount(sourceHandler(helper))
                        + instrument.snapshot().coldCoolantMb()
                        + instrument.snapshot().hotCoolantMb();
                require(helper, initialTotal[0] == 2_000L,
                        "替换仪表前管网总量错误：" + initialTotal[0]);

                helper.setBlock(INSTRUMENT, Blocks.AIR.defaultBlockState());
                helper.setBlock(INSTRUMENT, P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
                ReactorStructureLifecycle.rescanInstrumentPortNow(
                        helper.getLevel(), helper.absolutePos(INSTRUMENT));
                phase[0] = 1;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (phase[0] == 1) {
                ReactorInstrumentPortBlockEntity instrument = instrument(helper);
                ReactorPortBlockEntity cold = coldPort(helper);
                require(helper, instrument != oldInstrument[0], "仪表端口方块实体没有真正替换");
                require(helper, instrument.structureValid(), "替换仪表后结构未恢复有效");
                require(helper, cold.isBoundTo(instrument,
                                ReactorPortBlockEntity.BindingType.COLD_COOLANT, null),
                        "替换仪表后保留的冷端没有接管到新仪表端口");
                require(helper, hotPort(helper).isBoundTo(instrument,
                                ReactorPortBlockEntity.BindingType.HOT_COOLANT, null),
                        "替换仪表后保留的热端没有接管到新仪表端口");
                require(helper, coldHandler(helper) != null,
                        "替换仪表后冷端 capability 没有恢复");
                require(helper, hotHandler(helper) != null,
                        "替换仪表后热端 capability 没有恢复");

                long newColdBeforeStaleCall = instrument.snapshot().coldCoolantMb();
                int staleFill = oldHandler[0].fill(
                        new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 128),
                        IFluidHandler.FluidAction.EXECUTE);
                require(helper, staleFill == 0,
                        "旧冷端 handler 在仪表替换后仍可写入，实际写入=" + staleFill);
                require(helper, instrument.snapshot().coldCoolantMb() == newColdBeforeStaleCall,
                        "旧冷端 handler 修改了新仪表快照");
                require(helper, oldHotHandler[0].drain(128, IFluidHandler.FluidAction.EXECUTE)
                                .isEmpty(),
                        "旧热端 handler 在仪表替换后仍可写入");

                phase[0] = 2;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (helper.getLevel().getGameTime() - phaseTick[0] < 30L) {
                return;
            }
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            long cold = instrument.snapshot().coldCoolantMb();
            long total = sourceAmount(sourceHandler(helper)) + cold
                    + instrument.snapshot().hotCoolantMb();
            require(helper, cold > 0L,
                    "仪表替换后 Create 管网没有恢复向原冷端输入，冷库存=" + cold);
            require(helper, total == initialTotal[0],
                    "仪表替换后冷却剂总量不守恒：初始=" + initialTotal[0] + "，当前=" + total);
            require(helper, oldInstrument[0].snapshot().coldCoolantMb() == 0L,
                    "旧仪表快照被旧 handler 或管网继续写入");
            helper.succeed();
        });
    }

    /** 保留热端和输出管网，在结构短暂失效后验证热库存仍能由 Create 输出。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void retainedHotPortRestoresCreateOutput(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        long startTick = helper.getLevel().getGameTime();
        long[] phaseTick = {startTick};
        int[] phase = {0};

        helper.succeedWhen(() -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity hot = hotPort(helper);
            if (phase[0] == 0) {
                if (helper.getLevel().getGameTime() - startTick < 5L) {
                    return;
                }
                require(helper, instrument.structureValid(), "热端恢复测试的反应堆未成型");
                require(helper, hot.isBoundTo(instrument,
                                ReactorPortBlockEntity.BindingType.HOT_COOLANT, null),
                        "热端恢复测试初始绑定缺失");
                buildHotCreateNetwork(helper);
                instrument.setSnapshot(instrument.snapshot().withCoolantInventories(0L, 512L));
                helper.setBlock(new BlockPos(0, 0, 0), Blocks.AIR.defaultBlockState());
                ReactorStructureLifecycle.rescanAroundNow(
                        helper.getLevel(), helper.absolutePos(new BlockPos(0, 0, 0)));
                require(helper, !instrument.structureValid(), "热端失效阶段结构仍有效");
                require(helper, hot.boundOwner() == null && hotHandler(helper) == null,
                        "热端失效后旧 capability 仍然可用");
                phase[0] = 1;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (phase[0] == 1) {
                if (helper.getLevel().getGameTime() - phaseTick[0] < 6L) {
                    return;
                }
                require(helper, instrument.snapshot().hotCoolantMb() == 512L,
                        "热端失效阶段热库存发生变化");
                helper.setBlock(new BlockPos(0, 0, 0),
                        P1Blocks.REACTOR_CASING.get().defaultBlockState());
                ReactorStructureLifecycle.rescanAroundNow(
                        helper.getLevel(), helper.absolutePos(new BlockPos(0, 0, 0)));
                require(helper, instrument.structureValid(), "热端失效后结构未恢复");
                require(helper, hot.isBoundTo(instrument,
                                ReactorPortBlockEntity.BindingType.HOT_COOLANT, null)
                                && hotHandler(helper) != null,
                        "热端恢复后原端口没有重新接管输出 capability");
                phase[0] = 2;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (helper.getLevel().getGameTime() - phaseTick[0] < 20L) {
                return;
            }
            long sinkAmount = hotSinkAmount(helper);
            require(helper, sinkAmount > 0L,
                    "热端恢复后 Create 输出管网没有收到热冷却剂");
            require(helper, instrument.snapshot().hotCoolantMb() + sinkAmount == 512L,
                    "热端恢复后冷却剂总量不守恒：仪表="
                            + instrument.snapshot().hotCoolantMb() + "，储罐=" + sinkAmount);
            helper.succeed();
        });
    }

    /** 保留同一个冷端和管道连续五次失效/恢复，验证绑定与真实输入不会漂移。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void retainedPortRecoversThroughFiveStructureCycles(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        long startTick = helper.getLevel().getGameTime();
        int[] phase = {0};
        int[] cycles = {0};
        long[] phaseTick = {startTick};
        long[] coldBefore = {0L};
        long[] sourceBefore = {0L};
        ReactorPortBlockEntity[] retainedCold = {null};

        helper.succeedWhen(() -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity cold = coldPort(helper);
            if (phase[0] == 0) {
                if (helper.getLevel().getGameTime() - startTick < 5L) {
                    return;
                }
                require(helper, instrument.structureValid(), "连续恢复测试的反应堆未成型");
                buildCreateNetwork(helper);
                retainedCold[0] = cold;
                phase[0] = 1;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (phase[0] == 1) {
                if (helper.getLevel().getGameTime() - phaseTick[0] < 20L) {
                    return;
                }
                require(helper, retainedCold[0] == cold,
                        "连续恢复测试中的冷端方块实体被替换");
                require(helper, coldHandler(helper) != null,
                        "连续恢复测试初始冷端 capability 不可用");
                long coldToMove = instrument.snapshot().coldCoolantMb();
                int replenished = sourceHandler(helper).fill(
                        new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(),
                                (int) Math.min(Integer.MAX_VALUE, coldToMove)),
                        IFluidHandler.FluidAction.EXECUTE);
                instrument.setSnapshot(instrument.snapshot().withCoolantInventories(
                        0L, instrument.snapshot().hotCoolantMb()));
                require(helper, replenished == coldToMove,
                        "连续恢复测试准备阶段无法把冷库存回灌到源储罐");
                phase[0] = 2;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (phase[0] == 2) {
                coldBefore[0] = instrument.snapshot().coldCoolantMb();
                sourceBefore[0] = sourceAmount(sourceHandler(helper));
                helper.setBlock(new BlockPos(0, 0, 0), Blocks.AIR.defaultBlockState());
                ReactorStructureLifecycle.rescanAroundNow(
                        helper.getLevel(), helper.absolutePos(new BlockPos(0, 0, 0)));
                require(helper, !instrument.structureValid(),
                        "第 " + (cycles[0] + 1) + " 次失效后结构仍有效");
                require(helper, cold.boundOwner() == null && coldHandler(helper) == null,
                        "第 " + (cycles[0] + 1) + " 次失效后保留冷端仍有旧绑定");
                phase[0] = 3;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (phase[0] == 3) {
                if (helper.getLevel().getGameTime() - phaseTick[0] < 6L) {
                    return;
                }
                require(helper, sourceAmount(sourceHandler(helper)) == sourceBefore[0],
                        "结构失效阶段 Create 管网仍消耗源储罐");
                require(helper, instrument.snapshot().coldCoolantMb() == coldBefore[0],
                        "结构失效阶段冷库存发生变化");
                helper.setBlock(new BlockPos(0, 0, 0),
                        P1Blocks.REACTOR_CASING.get().defaultBlockState());
                ReactorStructureLifecycle.rescanAroundNow(
                        helper.getLevel(), helper.absolutePos(new BlockPos(0, 0, 0)));
                require(helper, instrument.structureValid(),
                        "第 " + (cycles[0] + 1) + " 次恢复后结构未成型");
                require(helper, cold == retainedCold[0]
                                && cold.isBoundTo(instrument,
                                ReactorPortBlockEntity.BindingType.COLD_COOLANT, null)
                                && coldHandler(helper) != null,
                        "第 " + (cycles[0] + 1) + " 次恢复后原冷端未重新绑定");
                phase[0] = 4;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (helper.getLevel().getGameTime() - phaseTick[0] < 12L) {
                return;
            }
            long recovered = instrument.snapshot().coldCoolantMb();
            require(helper, recovered > coldBefore[0],
                    "第 " + (cycles[0] + 1) + " 次恢复后 Create 管网未重新输入");
            cycles[0]++;
            if (cycles[0] >= 5) {
                helper.succeed();
                return;
            }
            phase[0] = 1;
            phaseTick[0] = helper.getLevel().getGameTime();
        });
    }

    /** 放置固定结构模板。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 放置储罐、动力泵、管道和创造马达，流体转移由真实世界 tick 执行。 */
    private static void buildCreateNetwork(GameTestHelper helper) {
        helper.setBlock(SOURCE_TANK, AllBlocks.FLUID_TANK.get().defaultBlockState());
        int loaded = sourceHandler(helper).fill(
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 2_000),
                IFluidHandler.FluidAction.EXECUTE);
        require(helper, loaded == 2_000, "Create 源储罐无法装入测试冷却剂：" + loaded);
        helper.setBlock(SOURCE_PIPE, fluidPipeState());
        helper.setBlock(PIPE_NEAR, fluidPipeState());
        helper.setBlock(PUMP,
                AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
                        .setValue(PumpBlock.FACING, Direction.NORTH));
        helper.setBlock(POWER_COG,
                AllBlocks.COGWHEEL.get().defaultBlockState()
                        .setValue(com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS,
                                Direction.Axis.Z));
        helper.setBlock(POWER_SHAFT,
                AllBlocks.SHAFT.get().defaultBlockState()
                        .setValue(com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS,
                                Direction.Axis.Z));
        helper.setBlock(MOTOR,
                AllBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                        .setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        for (BlockPos pipe : new BlockPos[]{SOURCE_PIPE, PIPE_NEAR}) {
            FluidPropagator.propagateChangedPipe(
                    helper.getLevel(), helper.absolutePos(pipe),
                    helper.getLevel().getBlockState(helper.absolutePos(pipe)));
        }
    }

    /** 放置热端输出方向的 Create 管网和独立动力链。 */
    private static void buildHotCreateNetwork(GameTestHelper helper) {
        helper.setBlock(HOT_SINK_TANK, AllBlocks.FLUID_TANK.get().defaultBlockState());
        helper.setBlock(HOT_SINK_PIPE, fluidPipeState());
        helper.setBlock(HOT_PIPE_NEAR, fluidPipeState());
        helper.setBlock(HOT_PUMP,
                AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
                        .setValue(PumpBlock.FACING, Direction.SOUTH));
        helper.setBlock(HOT_POWER_COG,
                AllBlocks.COGWHEEL.get().defaultBlockState()
                        .setValue(com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS,
                                Direction.Axis.Z));
        helper.setBlock(HOT_POWER_SHAFT,
                AllBlocks.SHAFT.get().defaultBlockState()
                        .setValue(com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS,
                                Direction.Axis.Z));
        helper.setBlock(HOT_MOTOR,
                AllBlocks.CREATIVE_MOTOR.get().defaultBlockState()
                        .setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        for (BlockPos pipe : new BlockPos[]{HOT_SINK_PIPE, HOT_PIPE_NEAR}) {
            FluidPropagator.propagateChangedPipe(
                    helper.getLevel(), helper.absolutePos(pipe),
                    helper.getLevel().getBlockState(helper.absolutePos(pipe)));
        }
    }

    /** 返回仪表端口方块实体。 */
    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "固定坐标没有仪表端口方块实体");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    /** 返回冷端方块实体。 */
    private static ReactorPortBlockEntity coldPort(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(COLD);
        require(helper, blockEntity instanceof ReactorPortBlockEntity,
                "固定坐标没有冷端方块实体");
        return (ReactorPortBlockEntity) blockEntity;
    }

    /** 返回热端方块实体。 */
    private static ReactorPortBlockEntity hotPort(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(HOT);
        require(helper, blockEntity instanceof ReactorPortBlockEntity,
                "固定坐标没有热端方块实体");
        return (ReactorPortBlockEntity) blockEntity;
    }

    /** 读取冷端的正式世界 capability。 */
    private static IFluidHandler coldHandler(GameTestHelper helper) {
        return helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, helper.absolutePos(COLD), Direction.SOUTH);
    }

    /** 读取热端的正式世界 capability。 */
    private static IFluidHandler hotHandler(GameTestHelper helper) {
        return helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, helper.absolutePos(HOT), Direction.SOUTH);
    }

    /** 读取热端输出储罐中的库存，单位为 mB。 */
    private static long hotSinkAmount(GameTestHelper helper) {
        IFluidHandler handler = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(HOT_SINK_TANK), Direction.NORTH);
        require(helper, handler != null, "Create 热端输出储罐没有正式流体 capability");
        return sourceAmount(handler);
    }

    /** 读取源储罐的正式世界 capability。 */
    private static IFluidHandler sourceHandler(GameTestHelper helper) {
        IFluidHandler handler = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, helper.absolutePos(SOURCE_TANK), Direction.NORTH);
        require(helper, handler != null, "Create 源储罐没有正式流体 capability");
        return handler;
    }

    /** 读取流体处理器中的库存，单位为 mB。 */
    private static long sourceAmount(IFluidHandler handler) {
        return handler.getTanks() == 0 ? 0L : handler.getFluidInTank(0).getAmount();
    }

    /** 构造 Create 玻璃流体管的直线 Z 轴连接状态。 */
    private static BlockState fluidPipeState() {
        return ((GlassFluidPipeBlock) AllBlocks.GLASS_FLUID_PIPE.get()).defaultBlockState()
                .setValue(GlassFluidPipeBlock.AXIS, Direction.Axis.Z);
    }

    /** 将结构模板中的注册 ID 映射为实际方块。 */
    private static net.minecraft.world.level.block.Block blockForId(String id) {
        return switch (id) {
            case "minecraft:air" -> Blocks.AIR;
            case "create_nuclear_industry:reactor_casing" -> P1Blocks.REACTOR_CASING.get();
            case "create_nuclear_industry:reactor_window" -> P1Blocks.REACTOR_WINDOW.get();
            case "create_nuclear_industry:reactor_instrument_port" -> P1Blocks.REACTOR_INSTRUMENT_PORT.get();
            case "create_nuclear_industry:reactor_cold_port" -> P1Blocks.REACTOR_COLD_PORT.get();
            case "create_nuclear_industry:reactor_hot_port" -> P1Blocks.REACTOR_HOT_PORT.get();
            case "create_nuclear_industry:reactor_refueling_port" -> P1Blocks.REACTOR_REFUELING_PORT.get();
            case "create_nuclear_industry:reactor_fuel_rod" -> P1Blocks.REACTOR_FUEL_ROD.get();
            case "create_nuclear_industry:control_rod_drive" -> P1Blocks.CONTROL_ROD_DRIVE.get();
            default -> throw new IllegalArgumentException("未知反应堆方块 ID：" + id);
        };
    }

    /** 将异步断言统一交给 GameTest。 */
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
