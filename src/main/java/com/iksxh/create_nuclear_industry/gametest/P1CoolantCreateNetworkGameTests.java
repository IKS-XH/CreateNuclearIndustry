package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.Map;
import java.lang.reflect.Field;
import java.util.Optional;

/** 使用 Create 6.0.10 正式泵、管道、储罐和动力方块验证反应堆冷端管网。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1CoolantCreateNetworkGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos COLD = new BlockPos(1, 2, 4);

    private static final BlockPos PIPE_NEAR = new BlockPos(1, 2, 5);
    private static final BlockPos PUMP = new BlockPos(1, 2, 6);
    private static final BlockPos SOURCE_PIPE = new BlockPos(1, 2, 7);
    private static final BlockPos SOURCE_TANK = new BlockPos(1, 2, 8);
    private static final BlockPos POWER_COG = new BlockPos(2, 2, 6);
    private static final BlockPos POWER_SHAFT = new BlockPos(2, 2, 7);
    private static final BlockPos MOTOR = new BlockPos(2, 2, 8);

    private P1CoolantCreateNetworkGameTests() {
    }

    /** 先验证同一组 Create 方块之间的标准储罐到储罐管网基线。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void createTankToTankNetworkBaseline(GameTestHelper helper) {
        buildCreateTankNetwork(helper);
        helper.succeedWhen(() -> {
            if (helper.getLevel().getGameTime() < 30L) {
                return;
            }
            long source = sourceAmount(sourceHandler(helper));
            long output = tankAmount(helper, COLD);
            if (output <= 0) {
                helper.fail("Create 储罐到储罐基线未形成有效输出，源储罐=" + source
                        + "，目标储罐=" + output
                        + "，管道=" + pipeDiagnostics(helper, PIPE_NEAR));
                return;
            }
            if (source + output != 2_000L) {
                helper.fail("Create 储罐到储罐基线总量不守恒：源=" + source + "，目标=" + output);
                return;
            }
            helper.succeed();
        });
    }

    /** 验证真实 Create 管网能够把储罐冷却剂送入已成型反应堆，并保持总量守恒。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void formedReactorAcceptsPoweredCreateNetworkInput(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        long startTick = helper.getLevel().getGameTime();
        long[] phaseTick = {startTick};
        int[] phase = {0};
        long[] initialTotal = {0L};
        int[] initialCold = {0};
        long[] coldAtInvalidation = {0L};
        long[] sourceAtInvalidation = {0L};
        helper.succeedWhen(() -> {
            if (phase[0] == 0) {
                if (helper.getLevel().getGameTime() - startTick < 5L) {
                    return;
                }
                require(helper, instrument(helper).structureValid(), "真实管网测试的反应堆未成型");
                buildCreateNetwork(helper, Direction.NORTH,
                        new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 2_000));
                phase[0] = 1;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (phase[0] == 1) {
                if (helper.getLevel().getGameTime() - phaseTick[0] < 12L) {
                    return;
                }
                ReactorInstrumentPortBlockEntity instrument = instrument(helper);
                FluidTankBlockEntity sourceTank = sourceTank(helper);
                PumpBlockEntity pump = pump(helper);
                IFluidHandler source = sourceHandler(helper);

                require(helper, instrument.structureValid(), "真实管网测试的反应堆未成型");
                require(helper, coldHandler(helper) != null,
                        "Create 管网建立前反应堆冷端 capability 不可用");
                require(helper, pump.getSpeed() != 0,
                        "机械泵未获得动力，方向=" + helper.getBlockState(PUMP).getValue(PumpBlock.FACING));
                initialTotal[0] = sourceAmount(source) + instrument.snapshot().coldCoolantMb()
                        + instrument.snapshot().hotCoolantMb();
                require(helper, initialTotal[0] == 2_000L,
                        "管网测试初始总量错误：" + initialTotal[0]);
                initialCold[0] = (int) instrument.snapshot().coldCoolantMb();
                phase[0] = 2;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (phase[0] == 2) {
                if (helper.getLevel().getGameTime() - phaseTick[0] < 30L) {
                    return;
                }
                ReactorInstrumentPortBlockEntity instrument = instrument(helper);
                FluidTankBlockEntity sourceTank = sourceTank(helper);
                PumpBlockEntity pump = pump(helper);
                IFluidHandler source = sourceHandler(helper);
                long acceptedCold = instrument.snapshot().coldCoolantMb();
                long total = sourceAmount(source) + acceptedCold + instrument.snapshot().hotCoolantMb();
                require(helper, acceptedCold > initialCold[0],
                        "真实 Create 管网未向冷端输入，冷库存=" + acceptedCold
                                + "，泵速=" + pump.getSpeed()
                                + "，源储罐=" + sourceAmount(source)
                                + "，管道=" + pipeDiagnostics(helper, PIPE_NEAR)
                                + " / " + pipeDiagnostics(helper, SOURCE_PIPE)
                                + "，冷端能力=" + coldCapabilityDiagnostics(helper));
                require(helper, total == initialTotal[0],
                        "冷却剂总量未守恒：初始=" + initialTotal[0] + "，当前=" + total);
                require(helper, coldHandler(helper) != null,
                        "成型反应堆冷端 capability 在管网传输后丢失");

                helper.setBlock(COLD, P1Blocks.REACTOR_CASING.get().defaultBlockState());
                ReactorStructureLifecycle.rescanAroundNow(
                        helper.getLevel(), helper.absolutePos(COLD));
                coldAtInvalidation[0] = instrument.snapshot().coldCoolantMb();
                sourceAtInvalidation[0] = sourceAmount(source);
                require(helper, !instrument.structureValid(), "冷端替换后结构仍被判定为有效");
                require(helper, coldHandler(helper) == null,
                        "结构失效后冷端 capability 仍被 Create 网络发现");
                phase[0] = 3;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (phase[0] == 3) {
                if (helper.getLevel().getGameTime() - phaseTick[0] < 8L) {
                    return;
                }
                ReactorInstrumentPortBlockEntity instrument = instrument(helper);
                IFluidHandler source = sourceHandler(helper);
                require(helper, instrument.snapshot().coldCoolantMb() == coldAtInvalidation[0],
                            "结构失效后 Create 管网仍修改冷库存");
                require(helper, sourceAmount(source) == sourceAtInvalidation[0],
                            "结构失效后 Create 管网仍从储罐取出冷却剂");

                helper.setBlock(COLD, P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState());
                ReactorStructureLifecycle.rescanAroundNow(
                        helper.getLevel(), helper.absolutePos(COLD));
                require(helper, instrument.structureValid(), "补回冷端后结构未恢复成型");
                require(helper, coldHandler(helper) != null,
                            "补回冷端后 capability 未恢复");
                phase[0] = 4;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (phase[0] == 4) {
                if (helper.getLevel().getGameTime() - phaseTick[0] < 12L) {
                    return;
                }
                ReactorInstrumentPortBlockEntity instrument = instrument(helper);
                FluidTankBlockEntity sourceTank = sourceTank(helper);
                IFluidHandler source = sourceHandler(helper);
                long recoveredCold = instrument.snapshot().coldCoolantMb();
                long recoveredTotal = sourceAmount(source) + recoveredCold
                        + instrument.snapshot().hotCoolantMb();
                require(helper, recoveredCold > coldAtInvalidation[0],
                        "冷端恢复后 Create 管网未重新传输，恢复前=" + coldAtInvalidation[0]
                                + "，恢复后=" + recoveredCold);
                require(helper, recoveredTotal == initialTotal[0],
                        "管网恢复后冷却剂总量未守恒：初始=" + initialTotal[0]
                                + "，当前=" + recoveredTotal);
                require(helper, sourceTank.getTankInventory().getFluidAmount() >= 0,
                        "Create 储罐实体在管网恢复后不可读");
                helper.succeed();
            }
        });
    }

    /** 验证反向泵只会把流体推离冷端，不会伪造冷端输入。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void reversedPumpDoesNotFillColdPort(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        long startTick = helper.getLevel().getGameTime();
        long[] phaseTick = {startTick};
        int[] phase = {0};
        long[] coldBefore = {0L};
        long[] sourceBefore = {0L};
        helper.succeedWhen(() -> {
            if (phase[0] == 0) {
                if (helper.getLevel().getGameTime() - startTick < 5L) {
                    return;
                }
                require(helper, instrument(helper).structureValid(), "反向泵测试的反应堆未成型");
                buildCreateNetwork(helper, Direction.SOUTH,
                        new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 512));
                phase[0] = 1;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }
            if (helper.getLevel().getGameTime() - phaseTick[0] < 12L) {
                return;
            }
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            IFluidHandler source = sourceHandler(helper);
            if (phase[0] == 1) {
                coldBefore[0] = instrument.snapshot().coldCoolantMb();
                sourceBefore[0] = sourceAmount(source);
                phase[0] = 2;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }
            require(helper, instrument.snapshot().coldCoolantMb() == coldBefore[0],
                        "反向泵错误地向冷端输入冷却剂");
            require(helper, sourceAmount(source) == sourceBefore[0],
                        "反向泵测试改变了错误方向储罐库存");
            helper.succeed();
        });
    }

    /** 验证 Create 网络携带错误流体时不会改变反应堆共享冷库存。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void wrongFluidDoesNotFillColdPort(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        long startTick = helper.getLevel().getGameTime();
        long[] phaseTick = {startTick};
        int[] phase = {0};
        long[] coldBefore = {0L};
        long[] sourceBefore = {0L};
        helper.succeedWhen(() -> {
            if (phase[0] == 0) {
                if (helper.getLevel().getGameTime() - startTick < 5L) {
                    return;
                }
                require(helper, instrument(helper).structureValid(), "错误流体测试的反应堆未成型");
                buildCreateNetwork(helper, Direction.NORTH, new FluidStack(Fluids.WATER, 512));
                phase[0] = 1;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }
            if (helper.getLevel().getGameTime() - phaseTick[0] < 12L) {
                return;
            }
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            IFluidHandler source = sourceHandler(helper);
            if (phase[0] == 1) {
                coldBefore[0] = instrument.snapshot().coldCoolantMb();
                sourceBefore[0] = sourceAmount(source);
                phase[0] = 2;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }
            require(helper, instrument.snapshot().coldCoolantMb() == coldBefore[0],
                        "错误流体通过 Create 网络修改了冷库存");
            require(helper, sourceAmount(source) == sourceBefore[0],
                        "错误流体被 Create 网络从储罐错误扣除");
            helper.succeed();
        });
    }

    /** 放置唯一正式仪表、冷端、热端和标准燃料列布局。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 放置 Create 6.0.10 的真实储罐、泵、流体管、齿轮、轴和创造马达。 */
    private static void buildCreateNetwork(
            GameTestHelper helper, Direction pumpFacing, FluidStack initialFluid
    ) {
        helper.setBlock(SOURCE_TANK, AllBlocks.FLUID_TANK.get().defaultBlockState());
        IFluidHandler source = sourceHandler(helper);
        int loaded = source.fill(initialFluid, FluidAction.EXECUTE);
        require(helper, loaded == initialFluid.getAmount(),
                "Create 储罐无法装入测试流体，期望=" + initialFluid.getAmount() + "，实际=" + loaded);
        helper.setBlock(SOURCE_PIPE, fluidPipeState());
        helper.setBlock(PIPE_NEAR,
                fluidPipeState());
        helper.setBlock(PUMP,
                AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
                        .setValue(PumpBlock.FACING, pumpFacing));
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
        // GameTest 直接替换方块状态时不会完整复现玩家逐段放置方块触发的 Create 管网重扫。
        for (BlockPos pipe : new BlockPos[]{SOURCE_PIPE, PIPE_NEAR}) {
            FluidPropagator.propagateChangedPipe(
                    helper.getLevel(), helper.absolutePos(pipe),
                    helper.getLevel().getBlockState(helper.absolutePos(pipe)));
        }
    }

    /** 构造不含反应堆的 Create 储罐到储罐基线，确认泵和管道测试夹具本身有效。 */
    private static void buildCreateTankNetwork(GameTestHelper helper) {
        helper.setBlock(SOURCE_TANK, AllBlocks.FLUID_TANK.get().defaultBlockState());
        IFluidHandler source = sourceHandler(helper);
        int loaded = source.fill(
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 2_000), FluidAction.EXECUTE);
        require(helper, loaded == 2_000, "Create 基线源储罐无法装入测试流体");
        helper.setBlock(COLD, AllBlocks.FLUID_TANK.get().defaultBlockState());
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

    /** 构造 Create 玻璃流体管的直线 Z 轴连接状态，复用 Create 官方泵送测试的管道类型。 */
    private static BlockState fluidPipeState() {
        return ((GlassFluidPipeBlock) AllBlocks.GLASS_FLUID_PIPE.get()).defaultBlockState()
                .setValue(GlassFluidPipeBlock.AXIS, Direction.Axis.Z);
    }

    /** 获取正式仪表端口方块实体。 */
    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "管网测试找不到仪表端口方块实体");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    /** 获取 Create 源储罐方块实体。 */
    private static FluidTankBlockEntity sourceTank(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(SOURCE_TANK);
        require(helper, blockEntity instanceof FluidTankBlockEntity,
                "管网测试找不到 Create 储罐方块实体");
        return (FluidTankBlockEntity) blockEntity;
    }

    /** 获取 Create 机械泵方块实体并确认其类型。 */
    private static PumpBlockEntity pump(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(PUMP);
        require(helper, blockEntity instanceof PumpBlockEntity,
                "管网测试找不到 Create 机械泵方块实体");
        return (PumpBlockEntity) blockEntity;
    }

    /** 通过 NeoForge 正式方块 capability 读取源储罐，而不是直接访问私有网络状态。 */
    private static IFluidHandler sourceHandler(GameTestHelper helper) {
        IFluidHandler handler = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(SOURCE_TANK), Direction.NORTH);
        require(helper, handler != null, "Create 储罐未暴露正式流体 capability");
        return handler;
    }

    /** 读取已经成型反应堆的冷端 capability；失效时预期返回 null。 */
    private static IFluidHandler coldHandler(GameTestHelper helper) {
        return helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(COLD), Direction.SOUTH);
    }

    /** 读取储罐中的流体量，单位为 mB。 */
    private static long sourceAmount(IFluidHandler source) {
        return source.getTanks() == 0 ? 0L : source.getFluidInTank(0).getAmount();
    }

    /** 读取 Create 目标储罐中的流体量，单位为 mB。 */
    private static long tankAmount(GameTestHelper helper, BlockPos position) {
        IFluidHandler tank = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, helper.absolutePos(position), Direction.NORTH);
        require(helper, tank != null, "Create 目标储罐未暴露正式流体 capability");
        return sourceAmount(tank);
    }

    /** 输出 Create 管道连接、压力和流向，便于失败时区分发现、动力和端点问题。 */
    private static String pipeDiagnostics(GameTestHelper helper, BlockPos position) {
        FluidTransportBehaviour behaviour = BlockEntityBehaviour.get(
                helper.getLevel(), helper.absolutePos(position), FluidTransportBehaviour.TYPE);
        if (behaviour == null) {
            return "无流体行为";
        }
            StringBuilder result = new StringBuilder();
        for (Direction direction : Direction.values()) {
            PipeConnection connection = behaviour.getConnection(direction);
            if (connection == null) {
                continue;
            }
            PipeConnection.Flow flow = behaviour.getFlow(direction);
            result.append(direction).append(" pressure=").append(connection.getPressure());
            result.append(" source=").append(sourceType(connection));
            if (flow != null) {
                result.append(" flow=").append(flow.inbound).append('/').append(flow.complete)
                        .append('/').append(flow.fluid.getAmount());
            }
            result.append(';');
        }
        return result.toString();
    }

    /** 输出冷端 capability 的容量和流体校验结果，不执行填充事务。 */
    private static String coldCapabilityDiagnostics(GameTestHelper helper) {
        IFluidHandler handler = coldHandler(helper);
        if (handler == null) {
            return "null";
        }
        return handler.getTankCapacity(0) + "/"
                + handler.isFluidValid(0,
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 1));
    }

    /** 读取 Create 连接当前缓存的端点类别，用于定位网络发现阶段的问题。 */
    private static String sourceType(PipeConnection connection) {
        try {
            Field field = PipeConnection.class.getDeclaredField("source");
            field.setAccessible(true);
            Optional<?> source = (Optional<?>) field.get(connection);
            return source.map(value -> value.getClass().getSimpleName()).orElse("none");
        } catch (ReflectiveOperationException exception) {
            return "reflection-error";
        }
    }

    /** 将模板 ID 映射为已注册方块。 */
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
