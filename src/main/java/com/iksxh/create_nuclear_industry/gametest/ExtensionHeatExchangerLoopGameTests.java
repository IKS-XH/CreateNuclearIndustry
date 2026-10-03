package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/** 使用真实 Create 泵管网复现近满反应堆共享冷库存的多端口接收行为。 */
@GameTestHolder("create_nuclear_industry_heat_loop")
@PrefixGameTestTemplate(false)
public final class ExtensionHeatExchangerLoopGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEMPLATE = "loop_empty";
    private static final int INITIAL_COLD_MB = 9_000;
    private static final int SOURCE_MB = 4_000;
    private static final long REACTOR_CAPACITY_MB = 12_000L;
    private static final int PUMP_RPM = 256;
    private static final BlockPos COLD_ONE = new BlockPos(1, 2, 4);
    private static final BlockPos COLD_TWO = new BlockPos(2, 2, 4);
    private static final BlockPos COLD_THREE = new BlockPos(3, 2, 4);
    private static final BlockPos PUMP = new BlockPos(2, 2, 6);
    private static final BlockPos SOURCE_TANK = new BlockPos(2, 2, 8);
    private static final BlockPos COG = new BlockPos(3, 2, 6);
    private static final BlockPos SHAFT = new BlockPos(3, 2, 7);
    private static final BlockPos MOTOR = new BlockPos(3, 2, 8);
    private static final BlockPos TANK_SPLIT_LEFT = new BlockPos(0, 2, 5);
    private static final BlockPos TANK_SPLIT_RIGHT = new BlockPos(4, 2, 5);
    private static final BlockPos EXCHANGER = new BlockPos(2, 2, 2);

    private ExtensionHeatExchangerLoopGameTests() {
    }

    /** 单冷端实泵管对照：确认近满库存仍能真实接液且源液与冷热库存守恒。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void singleColdPortCreateNetworkConservesNearFullInventory(GameTestHelper helper) {
        runNearFullNetwork(helper, false);
    }

    /** 三冷端共用真实泵管网络：检查同一近满共享库存不会被多端口模拟超额许诺。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void threeColdPortsCreateNetworkConservesNearFullInventory(GameTestHelper helper) {
        runNearFullNetwork(helper, true);
    }

    /** 原生 Create 储罐分流基线：验证网络接收计划对彼此独立的原生储罐仍正常分配。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void nativeCreateTankSplitConservesFluid(GameTestHelper helper) {
        buildNativeTankSplit(helper);
        long[] initialTotal = {SOURCE_MB};
        boolean[] leftReceived = {false};
        boolean[] rightReceived = {false};
        helper.onEachTick(() -> {
            IFluidHandler source = fluidHandler(helper, SOURCE_TANK, Direction.NORTH);
            long sourceAmount = sourceAmount(source);
            long left = tankAmount(helper, TANK_SPLIT_LEFT);
            long right = tankAmount(helper, TANK_SPLIT_RIGHT);
            require(helper, sourceAmount + left + right == initialTotal[0],
                    "原生 Create 储罐分流总量漂移：源/左/右="
                            + sourceAmount + "/" + left + "/" + right);
            leftReceived[0] |= left > 0;
            rightReceived[0] |= right > 0;
            if (leftReceived[0] && rightReceived[0] && sourceAmount < SOURCE_MB) {
                LOGGER.info("EXCHANGER_LOOP_NATIVE_TANK_SPLIT source={} left={} right={} total={}",
                        sourceAmount, left, right, sourceAmount + left + right);
                helper.succeed();
            }
        });
    }

    /** 两个物理面向同一近满换热器热罐送液，核验同一账本的多句柄接收守恒。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void doubleSidedHeatExchangerNearFullInputConserves(GameTestHelper helper) {
        helper.setBlock(EXCHANGER, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
        helper.onEachTick(new Runnable() {
            private boolean started;
            private long initialTotal;
            private boolean westPortFlowObserved;
            private boolean eastPortFlowObserved;

            @Override
            public void run() {
                if (!started) {
                    NuclearHeatExchangerBlockEntity machine = exchanger(helper);
                    if (!machine.current() || !machine.canTick()) {
                        return;
                    }
                    IFluidHandler west = machine.fluidPort(Direction.WEST);
                    int prefilled = west.fill(new FluidStack(
                            ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1_000),
                            IFluidHandler.FluidAction.EXECUTE);
                    require(helper, prefilled == 1_000,
                            "近满换热器热罐预置失败：" + prefilled + "/1000");
                    buildDoubleSidedExchangerNetwork(helper);
                    initialTotal = SOURCE_MB + prefilled;
                    started = true;
                    LOGGER.info("EXCHANGER_LOOP_DOUBLE_SIDE_START source={} hot={} cold={} total={} capacity={} pumpRpm={}",
                            sourceAmount(fluidHandler(helper, SOURCE_TANK, Direction.NORTH)),
                            machine.ledger().hot(), machine.ledger().cold(), initialTotal,
                            com.iksxh.create_nuclear_industry.heat.HeatExchangerState.CAPACITY,
                            pump(helper).getSpeed());
                    return;
                }

                NuclearHeatExchangerBlockEntity machine = exchanger(helper);
                long sourceAmount = sourceAmount(fluidHandler(helper, SOURCE_TANK, Direction.NORTH));
                long hot = machine.ledger().hot();
                long cold = machine.ledger().cold();
                long total = sourceAmount + hot + cold;
                westPortFlowObserved |= pipeFlowActive(helper, new BlockPos(1, 2, 2), Direction.EAST);
                eastPortFlowObserved |= pipeFlowActive(helper, new BlockPos(3, 2, 2), Direction.WEST);
                require(helper, pump(helper).getSpeed() == PUMP_RPM,
                        "双面换热器输入网络的 Create 泵未保持 256 rpm");
                require(helper, total == initialTotal,
                        "双面近满换热器管网发生冷却剂总量漂移：源/热/冷="
                                + sourceAmount + "/" + hot + "/" + cold
                                + "，初始=" + initialTotal + "，当前=" + total);
                if (hot == com.iksxh.create_nuclear_industry.heat.HeatExchangerState.CAPACITY) {
                    require(helper, sourceAmount < SOURCE_MB,
                            "换热器满罐前没有观察到真实 Create 源液移动");
                    require(helper, westPortFlowObserved && eastPortFlowObserved,
                            "达到满罐前未观察到双侧换热器端口均有真实流体：西/东="
                                    + westPortFlowObserved + "/" + eastPortFlowObserved);
                    require(helper, total == initialTotal,
                            "双面近满换热器到达容量后总量不守恒");
                    LOGGER.info("EXCHANGER_LOOP_DOUBLE_SIDE_FINAL source={} hot={} cold={} total={} capacity={} branches={}",
                            sourceAmount, hot, cold, total,
                            com.iksxh.create_nuclear_industry.heat.HeatExchangerState.CAPACITY,
                            branchFlowDiagnostics(helper, true));
                    helper.succeed();
                }
            }
        });
    }

    private static void runNearFullNetwork(GameTestHelper helper, boolean branchToThreePorts) {
        buildReactor(helper);
        long structureStartTick = helper.getLevel().getGameTime();
        long[] networkStartTick = {0L};
        long[] initialTotal = {0L};
        long[] initialSource = {0L};
        boolean[] moved = {false};
        boolean[] branchFlowsObserved = new boolean[3];
        int[] phase = {0};

        helper.onEachTick(() -> {
            if (phase[0] == 0) {
                if (helper.getLevel().getGameTime() - structureStartTick < 5L) {
                    return;
                }
                ReactorInstrumentPortBlockEntity reactor = instrument(helper);
                require(helper, reactor.structureValid(), "冷却剂复现结构没有成型");
                require(helper, reactor.boundPorts(
                                com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity.BindingType
                                        .COLD_COOLANT).size() == 3,
                        "测试反应堆没有绑定恰好三个冷端");
                require(helper, reactor.coolantCapacityMb() == REACTOR_CAPACITY_MB,
                        "测试反应堆容量不是 12000 mB：" + reactor.coolantCapacityMb());
                reactor.setSnapshot(reactor.snapshot().withCoolantInventories(INITIAL_COLD_MB, 0L));

                buildCreateNetwork(helper, branchToThreePorts);
                initialSource[0] = sourceAmount(sourceHandler(helper));
                require(helper, initialSource[0] == SOURCE_MB,
                        "Create 源储罐初始冷却剂量错误：" + initialSource[0]);
                initialTotal[0] = totalAmount(helper, reactor);
                require(helper, initialTotal[0] == INITIAL_COLD_MB + SOURCE_MB,
                        "泵管接通前的冷却剂总量错误：" + initialTotal[0]);
                require(helper, pump(helper).getSpeed() == PUMP_RPM,
                        "真实机械泵未达到 256 rpm：" + pump(helper).getSpeed());
                requireConnectedColdPorts(helper, branchToThreePorts);
                LOGGER.info("EXCHANGER_LOOP_START scenario={} source={} cold={} hot={} total={} capacity={} pumpRpm={} connections={}",
                        branchToThreePorts ? "three_ports" : "single_port", initialSource[0],
                        reactor.snapshot().coldCoolantMb(), reactor.snapshot().hotCoolantMb(),
                        initialTotal[0], reactor.coolantCapacityMb(), pump(helper).getSpeed(),
                        networkDiagnostics(helper, branchToThreePorts));
                networkStartTick[0] = helper.getLevel().getGameTime();
                phase[0] = 1;
                return;
            }

            ReactorInstrumentPortBlockEntity reactor = instrument(helper);
            long sourceNow = sourceAmount(sourceHandler(helper));
            long coldNow = reactor.snapshot().coldCoolantMb();
            long hotNow = reactor.snapshot().hotCoolantMb();
            long totalNow = sourceNow + coldNow + hotNow;
            boolean movedNow = sourceNow < initialSource[0] && coldNow > INITIAL_COLD_MB;
            if (!moved[0] && movedNow) {
                LOGGER.info("EXCHANGER_LOOP_FIRST_TRANSFER scenario={} source={} cold={} hot={} total={} branches={}",
                        branchToThreePorts ? "three_ports" : "single_port", sourceNow, coldNow,
                        hotNow, totalNow, branchFlowDiagnostics(helper, branchToThreePorts));
            }
            moved[0] |= movedNow;
            if (moved[0]) {
                if (branchToThreePorts) {
                    BlockPos[] branchPipes = {
                            new BlockPos(1, 2, 5), new BlockPos(2, 2, 5), new BlockPos(3, 2, 5)};
                    for (int index = 0; index < branchPipes.length; index++) {
                        if (!branchFlowsObserved[index] && portFlowActive(helper, branchPipes[index])) {
                            branchFlowsObserved[index] = true;
                            LOGGER.info("EXCHANGER_LOOP_BRANCH_FLOW branch={} source={} cold={} hot={} total={} pipe={}",
                                    index + 1, sourceNow, coldNow, hotNow, totalNow,
                                    pipeDiagnostic(helper, branchPipes[index]));
                        }
                    }
                } else if (!branchFlowsObserved[0]
                        && portFlowActive(helper, new BlockPos(2, 2, 5))) {
                    branchFlowsObserved[0] = true;
                }
            }

            require(helper, totalNow == initialTotal[0],
                    (branchToThreePorts ? "三冷端" : "单冷端")
                            + "管网发生冷却剂总量漂移：初始源/冷/热="
                            + initialSource[0] + "/" + INITIAL_COLD_MB + "/0，当前源/冷/热="
                            + sourceNow + "/" + coldNow + "/" + hotNow
                            + "，容量=" + reactor.coolantCapacityMb()
                            + "，泵速=" + pump(helper).getSpeed()
                            + "，经过tick="
                            + (helper.getLevel().getGameTime() - networkStartTick[0]));

            if (helper.getLevel().getGameTime() - networkStartTick[0] < 5L) {
                return;
            }
            require(helper, pump(helper).getSpeed() == PUMP_RPM,
                    "Create 泵在管网启动后未保持 256 rpm：" + pump(helper).getSpeed());

            if (coldNow + hotNow == REACTOR_CAPACITY_MB) {
                require(helper, moved[0] && sourceNow < initialSource[0],
                        "库存达到容量前没有观察到真实 Create 泵管传输");
                require(helper, allExpectedBranchFlowsObserved(branchFlowsObserved, branchToThreePorts),
                        "达到容量前未观察到所有冷端支路的完整出流：源=" + sourceNow
                                + "，冷=" + coldNow + "，热=" + hotNow
                                + "，已观察支路=" + java.util.Arrays.toString(branchFlowsObserved)
                                + "，管网=" + branchFlowDiagnostics(helper, branchToThreePorts));
                require(helper, sourceNow + coldNow + hotNow == initialTotal[0],
                        "达到近满容量后冷却剂总量不守恒");
                LOGGER.info("EXCHANGER_LOOP_FINAL scenario={} source={} cold={} hot={} total={} capacity={} branchFlows={}",
                        branchToThreePorts ? "three_ports" : "single_port", sourceNow, coldNow,
                        hotNow, totalNow, reactor.coolantCapacityMb(),
                        branchFlowDiagnostics(helper, branchToThreePorts));
                helper.succeed();
                return;
            }

            if (helper.getLevel().getGameTime() - networkStartTick[0] > 100L) {
                helper.fail("真实 Create 管网未把近满冷库存填至容量：分支="
                        + branchToThreePorts + "，源=" + sourceNow + "，冷=" + coldNow
                        + "，热=" + hotNow + "，泵速=" + pump(helper).getSpeed()
                        + "，端口/管道诊断=" + networkDiagnostics(helper, branchToThreePorts));
            }
        });
    }

    /** 用四根空列把共享容量设为 12000 mB，并将三个冷口并排放在同一结构侧面。 */
    private static void buildReactor(GameTestHelper helper) {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> columns =
                new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        columns.put(new CoreColumnPosition(0, 0), ReactorStructureDefinition.ColumnType.EMPTY);
        columns.put(new CoreColumnPosition(0, 1), ReactorStructureDefinition.ColumnType.EMPTY);
        columns.put(new CoreColumnPosition(0, 2), ReactorStructureDefinition.ColumnType.EMPTY);
        Map<ReactorStructureDefinition.LocalPosition, String> layout =
                new HashMap<>(ReactorStructureDefinition.templateFor(columns));
        layout.put(local(COLD_ONE), "create_nuclear_industry:reactor_cold_port");
        layout.put(local(COLD_TWO), "create_nuclear_industry:reactor_cold_port");
        layout.put(local(COLD_THREE), "create_nuclear_industry:reactor_cold_port");
        layout.put(local(new BlockPos(4, 2, 1)), "create_nuclear_industry:reactor_hot_port");

        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry : layout.entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 构造单端或三端汇入同一干管的真实 Create 储罐、机械泵、玻璃管和动力网络。 */
    private static void buildCreateNetwork(GameTestHelper helper, boolean branchToThreePorts) {
        helper.setBlock(SOURCE_TANK, AllBlocks.FLUID_TANK.get().defaultBlockState());
        int loaded = sourceHandler(helper).fill(
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), SOURCE_MB),
                IFluidHandler.FluidAction.EXECUTE);
        require(helper, loaded == SOURCE_MB,
                "Create 源储罐未能装满测试冷却剂：" + loaded + "/" + SOURCE_MB);

        BlockPos[] pipes = branchToThreePorts
                ? new BlockPos[]{
                        new BlockPos(1, 2, 5), new BlockPos(2, 2, 5), new BlockPos(3, 2, 5),
                        new BlockPos(2, 2, 7)}
                : new BlockPos[]{new BlockPos(2, 2, 5), new BlockPos(2, 2, 7)};
        for (BlockPos pipe : pipes) {
            helper.setBlock(pipe, branchToThreePorts && pipe.getZ() == 5
                    ? AllBlocks.FLUID_PIPE.getDefaultState() : fluidPipeState());
        }
        helper.setBlock(PUMP, AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
                .setValue(PumpBlock.FACING, Direction.NORTH));
        helper.setBlock(COG, AllBlocks.COGWHEEL.get().defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(SHAFT, AllBlocks.SHAFT.get().defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(PUMP_RPM);

        if (branchToThreePorts) {
            sealAndAssertNoOpenPipeEnds(helper, new BlockPos[]{
                    new BlockPos(1, 2, 5), new BlockPos(2, 2, 5), new BlockPos(3, 2, 5)});
        }
        for (BlockPos pipe : pipes) {
            FluidPropagator.propagateChangedPipe(
                    helper.getLevel(), helper.absolutePos(pipe),
                    helper.getLevel().getBlockState(helper.absolutePos(pipe)));
        }
    }

    /** 构造一个原生 Create 源罐向左右两个独立原生储罐分流的基线。 */
    private static void buildNativeTankSplit(GameTestHelper helper) {
        helper.setBlock(SOURCE_TANK, AllBlocks.FLUID_TANK.get().defaultBlockState());
        helper.setBlock(TANK_SPLIT_LEFT, AllBlocks.FLUID_TANK.get().defaultBlockState());
        helper.setBlock(TANK_SPLIT_RIGHT, AllBlocks.FLUID_TANK.get().defaultBlockState());
        int loaded = fluidHandler(helper, SOURCE_TANK, Direction.NORTH).fill(
                new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), SOURCE_MB),
                IFluidHandler.FluidAction.EXECUTE);
        require(helper, loaded == SOURCE_MB,
                "原生 Create 分流源罐装液失败：" + loaded + "/" + SOURCE_MB);

        BlockPos[] pipes = {new BlockPos(1, 2, 5), new BlockPos(2, 2, 5), new BlockPos(3, 2, 5),
                new BlockPos(2, 2, 7)};
        for (BlockPos pipe : pipes) {
            helper.setBlock(pipe, AllBlocks.FLUID_PIPE.getDefaultState());
        }
        buildDriveTrain(helper);
        sealAndAssertNoOpenPipeEnds(helper, new BlockPos[]{
                new BlockPos(1, 2, 5), new BlockPos(2, 2, 5), new BlockPos(3, 2, 5),
                new BlockPos(2, 2, 7)});
        propagatePipes(helper, pipes);
    }

    /** 构造近满换热器两个侧面并联受液的真实 Create 管网。 */
    private static void buildDoubleSidedExchangerNetwork(GameTestHelper helper) {
        helper.setBlock(SOURCE_TANK, AllBlocks.FLUID_TANK.get().defaultBlockState());
        int loaded = fluidHandler(helper, SOURCE_TANK, Direction.NORTH).fill(
                new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), SOURCE_MB),
                IFluidHandler.FluidAction.EXECUTE);
        require(helper, loaded == SOURCE_MB,
                "双面换热器源罐装热液失败：" + loaded + "/" + SOURCE_MB);

        BlockPos[] pipes = {
                new BlockPos(1, 2, 5), new BlockPos(2, 2, 5), new BlockPos(3, 2, 5),
                new BlockPos(1, 2, 4), new BlockPos(1, 2, 3), new BlockPos(1, 2, 2),
                new BlockPos(3, 2, 4), new BlockPos(3, 2, 3), new BlockPos(3, 2, 2),
                new BlockPos(2, 2, 7)};
        for (BlockPos pipe : pipes) {
            helper.setBlock(pipe, AllBlocks.FLUID_PIPE.getDefaultState());
        }
        buildDriveTrain(helper);
        sealAndAssertNoOpenPipeEnds(helper, pipes);
        propagatePipes(helper, pipes);
    }

    /** 安装北向机械泵和 Create 创造马达传动轴；转速 256 rpm，Create 单刻输送上限为 128 mB。 */
    private static void buildDriveTrain(GameTestHelper helper) {
        helper.setBlock(PUMP, AllBlocks.MECHANICAL_PUMP.get().defaultBlockState()
                .setValue(PumpBlock.FACING, Direction.NORTH));
        helper.setBlock(COG, AllBlocks.COGWHEEL.get().defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(SHAFT, AllBlocks.SHAFT.get().defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(MOTOR, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(MOTOR)).generatedSpeed.setValue(PUMP_RPM);
    }

    /** 让 Create 刷新每一根新放置流体管的拓扑和连接行为。 */
    private static void propagatePipes(GameTestHelper helper, BlockPos[] pipes) {
        for (BlockPos pipe : pipes) {
            FluidPropagator.propagateChangedPipe(helper.getLevel(), helper.absolutePos(pipe),
                    helper.getLevel().getBlockState(helper.absolutePos(pipe)));
        }
    }

    /** 用不透流实心方块封闭正常管网的空气端，防止Create创建额外开放端点并排液。 */
    private static void sealAndAssertNoOpenPipeEnds(GameTestHelper helper, BlockPos[] pipes) {
        propagatePipes(helper, pipes);
        for (BlockPos pipe : pipes) {
            FluidTransportBehaviour behaviour = fluidBehaviour(helper, pipe);
            require(helper, behaviour != null, "Create 管缺少正式 FluidTransportBehaviour：" + pipe);
            for (Direction direction : Direction.values()) {
                if (behaviour.getConnection(direction) == null) {
                    continue;
                }
                BlockPos absolutePipe = helper.absolutePos(pipe);
                BlockPos neighbor = pipe.relative(direction);
                if (helper.getBlockState(neighbor).isAir()
                        && FluidPropagator.isOpenEnd(helper.getLevel(), absolutePipe, direction)) {
                    helper.setBlock(neighbor, Blocks.IRON_BLOCK);
                }
            }
        }
        propagatePipes(helper, pipes);
        for (BlockPos pipe : pipes) {
            BlockPos absolutePipe = helper.absolutePos(pipe);
            FluidTransportBehaviour behaviour = fluidBehaviour(helper, pipe);
            require(helper, behaviour != null, "Create 管缺少正式 FluidTransportBehaviour：" + pipe);
            for (Direction direction : Direction.values()) {
                if (behaviour.getConnection(direction) == null) {
                    continue;
                }
                require(helper, !FluidPropagator.isOpenEnd(helper.getLevel(), absolutePipe, direction),
                        "Create 流体管仍有未连接开放排液面：管=" + pipe + "，方向=" + direction);
            }
        }
    }

    private static BlockState fluidPipeState() {
        return ((GlassFluidPipeBlock) AllBlocks.GLASS_FLUID_PIPE.get()).defaultBlockState()
                .setValue(GlassFluidPipeBlock.AXIS, Direction.Axis.Z);
    }

    private static void requireConnectedColdPorts(GameTestHelper helper, boolean branchToThreePorts) {
        IFluidHandler first = coldHandler(helper, COLD_ONE);
        IFluidHandler second = coldHandler(helper, COLD_TWO);
        IFluidHandler third = coldHandler(helper, COLD_THREE);
        require(helper, first != null && second != null && third != null,
                "三冷口的正式世界 capability 不完整");
        require(helper, first != second && first != third && second != third,
                "三个物理冷口没有返回不同 capability handler");
        if (branchToThreePorts) {
            requirePortPipeConnection(helper, new BlockPos(1, 2, 5));
            requirePortPipeConnection(helper, new BlockPos(2, 2, 5));
            requirePortPipeConnection(helper, new BlockPos(3, 2, 5));
            requireConnection(helper, new BlockPos(1, 2, 5), Direction.EAST);
            requireConnection(helper, new BlockPos(2, 2, 5), Direction.WEST);
            requireConnection(helper, new BlockPos(2, 2, 5), Direction.EAST);
            requireConnection(helper, new BlockPos(2, 2, 5), Direction.SOUTH);
            requireConnection(helper, new BlockPos(3, 2, 5), Direction.WEST);
            require(helper, helper.getBlockState(new BlockPos(1, 2, 5)).is(AllBlocks.FLUID_PIPE.get())
                            && helper.getBlockState(new BlockPos(2, 2, 5)).is(AllBlocks.FLUID_PIPE.get())
                            && helper.getBlockState(new BlockPos(3, 2, 5)).is(AllBlocks.FLUID_PIPE.get()),
                    "三冷端横向歧管没有使用Create普通多向流体管");
        } else {
            requirePortPipeConnection(helper, new BlockPos(2, 2, 5));
        }
    }

    private static void requirePortPipeConnection(GameTestHelper helper, BlockPos pipePos) {
        requireConnection(helper, pipePos, Direction.NORTH);
    }

    private static void requireConnection(GameTestHelper helper, BlockPos pipePos, Direction direction) {
        FluidTransportBehaviour behaviour = fluidBehaviour(helper, pipePos);
        PipeConnection connection = behaviour == null ? null : behaviour.getConnection(direction);
        require(helper, connection != null,
                "Create流体管缺少" + direction + "连接：" + pipePos
                        + "；当前连接=" + pipeDiagnostic(helper, pipePos));
    }

    private static FluidTransportBehaviour fluidBehaviour(GameTestHelper helper, BlockPos pipePos) {
        return BlockEntityBehaviour.get(
                helper.getLevel(), helper.absolutePos(pipePos), FluidTransportBehaviour.TYPE);
    }

    private static boolean allExpectedBranchFlowsObserved(boolean[] observed, boolean triple) {
        return triple ? observed[0] && observed[1] && observed[2] : observed[0];
    }

    private static boolean portFlowActive(GameTestHelper helper, BlockPos pipePos) {
        FluidTransportBehaviour behaviour = fluidBehaviour(helper, pipePos);
        PipeConnection.Flow flow = behaviour == null ? null : behaviour.getFlow(Direction.NORTH);
        return flow != null && !flow.inbound && flow.complete && !flow.fluid.isEmpty();
    }

    private static boolean pipeFlowActive(GameTestHelper helper, BlockPos pipePos, Direction direction) {
        FluidTransportBehaviour behaviour = fluidBehaviour(helper, pipePos);
        PipeConnection.Flow flow = behaviour == null ? null : behaviour.getFlow(direction);
        return flow != null && !flow.inbound && flow.complete && !flow.fluid.isEmpty();
    }

    private static String networkDiagnostics(GameTestHelper helper, boolean branchToThreePorts) {
        return "源=" + sourceAmount(sourceHandler(helper))
                + ",冷端=" + instrument(helper).snapshot().coldCoolantMb()
                + ",热端=" + instrument(helper).snapshot().hotCoolantMb()
                + ",三端分支=" + branchToThreePorts
                + ",泵速=" + pump(helper).getSpeed()
                + ",分支=" + branchFlowDiagnostics(helper, branchToThreePorts);
    }

    private static String branchFlowDiagnostics(GameTestHelper helper, boolean branchToThreePorts) {
        if (!branchToThreePorts) {
            return pipeDiagnostic(helper, new BlockPos(2, 2, 5));
        }
        return "左{" + pipeDiagnostic(helper, new BlockPos(1, 2, 5)) + "}中{"
                + pipeDiagnostic(helper, new BlockPos(2, 2, 5)) + "}右{"
                + pipeDiagnostic(helper, new BlockPos(3, 2, 5)) + "}";
    }

    private static String pipeDiagnostic(GameTestHelper helper, BlockPos pipePos) {
        FluidTransportBehaviour behaviour = fluidBehaviour(helper, pipePos);
        if (behaviour == null) {
            return "无流体管网行为";
        }
        StringBuilder result = new StringBuilder();
        for (Direction direction : Direction.values()) {
            PipeConnection connection = behaviour.getConnection(direction);
            if (connection == null) {
                continue;
            }
            result.append(direction).append("压力=").append(connection.getPressure());
            PipeConnection.Flow flow = behaviour.getFlow(direction);
            if (flow != null) {
                result.append("流量=").append(flow.inbound).append('/').append(flow.complete)
                        .append('/').append(flow.fluid.getAmount());
            }
            result.append(';');
        }
        return result.toString();
    }

    private static long totalAmount(GameTestHelper helper, ReactorInstrumentPortBlockEntity reactor) {
        return sourceAmount(sourceHandler(helper)) + reactor.snapshot().coldCoolantMb()
                + reactor.snapshot().hotCoolantMb();
    }

    private static long sourceAmount(IFluidHandler handler) {
        return handler.getTanks() == 0 ? 0L : handler.getFluidInTank(0).getAmount();
    }

    private static IFluidHandler sourceHandler(GameTestHelper helper) {
        IFluidHandler handler = fluidHandler(helper, SOURCE_TANK, Direction.NORTH);
        require(helper, handler != null, "Create 源储罐未提供正式流体 capability");
        return handler;
    }

    private static IFluidHandler fluidHandler(GameTestHelper helper, BlockPos position, Direction side) {
        return helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, helper.absolutePos(position), side);
    }

    private static long tankAmount(GameTestHelper helper, BlockPos position) {
        IFluidHandler handler = fluidHandler(helper, position, Direction.NORTH);
        require(helper, handler != null && handler.getTanks() > 0,
                "原生 Create 输出储罐未提供正式流体 capability：" + position);
        return sourceAmount(handler);
    }

    private static NuclearHeatExchangerBlockEntity exchanger(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(EXCHANGER);
        require(helper, blockEntity instanceof NuclearHeatExchangerBlockEntity,
                "找不到正式换热器方块实体");
        return (NuclearHeatExchangerBlockEntity) blockEntity;
    }

    private static IFluidHandler coldHandler(GameTestHelper helper, BlockPos position) {
        return helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, helper.absolutePos(position), Direction.SOUTH);
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var entity = helper.getBlockEntity(new BlockPos(2, 2, 0));
        require(helper, entity instanceof ReactorInstrumentPortBlockEntity,
                "找不到正式反应堆仪表端口方块实体");
        return (ReactorInstrumentPortBlockEntity) entity;
    }

    private static PumpBlockEntity pump(GameTestHelper helper) {
        var entity = helper.getBlockEntity(PUMP);
        require(helper, entity instanceof PumpBlockEntity, "找不到 Create 机械泵方块实体");
        return (PumpBlockEntity) entity;
    }

    private static ReactorStructureDefinition.LocalPosition local(BlockPos position) {
        return new ReactorStructureDefinition.LocalPosition(
                position.getX(), position.getY(), position.getZ());
    }

    private static Block blockForId(String id) {
        return switch (id) {
            case "minecraft:air" -> Blocks.AIR;
            case "create_nuclear_industry:reactor_casing" -> P1Blocks.REACTOR_CASING.get();
            case "create_nuclear_industry:reactor_window" -> P1Blocks.REACTOR_WINDOW.get();
            case "create_nuclear_industry:reactor_instrument_port" -> P1Blocks.REACTOR_INSTRUMENT_PORT.get();
            case "create_nuclear_industry:reactor_cold_port" -> P1Blocks.REACTOR_COLD_PORT.get();
            case "create_nuclear_industry:reactor_hot_port" -> P1Blocks.REACTOR_HOT_PORT.get();
            case "create_nuclear_industry:reactor_refueling_port" -> P1Blocks.REACTOR_REFUELING_PORT.get();
            case "create_nuclear_industry:control_rod_drive" -> P1Blocks.CONTROL_ROD_DRIVE.get();
            case "create_nuclear_industry:reactor_fuel_rod" -> P1Blocks.REACTOR_FUEL_ROD.get();
            default -> throw new IllegalArgumentException("未知反应堆方块 ID：" + id);
        };
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
