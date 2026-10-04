package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlock;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import com.iksxh.create_nuclear_industry.heat.HeatExchangerBoilerBridge;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 在独立大模板内覆盖直列拓扑、两端真实管网和跨成员换热。 */
@GameTestHolder("create_nuclear_industry_heat_chain")
@PrefixGameTestTemplate(false)
public final class ExtensionHeatExchangerChainGameTests {
    private static final String TEMPLATE = "chain_empty";
    private static final BlockPos FIRST = new BlockPos(2, 2, 2);
    private static final BlockPos MIDDLE = FIRST.east();
    private static final BlockPos LAST = MIDDLE.east();
    private ExtensionHeatExchangerChainGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void directedPortsMergeSplitAndRejectStaleHandles(GameTestHelper helper) {
        line(helper, 3);
        helper.runAfterDelay(5, () -> {
            var first = machine(helper, FIRST);
            var middle = machine(helper, MIDDLE);
            var last = machine(helper, LAST);
            IFluidHandler inlet = port(helper, FIRST, Direction.WEST);
            IFluidHandler outlet = port(helper, LAST, Direction.EAST);
            require(helper, inlet.getTankCapacity(0) == 12000 && outlet.getTankCapacity(0) == 12000,
                    "三机直列未形成各12000mB的冷热容量");
            for (Direction side : Direction.values()) {
                if (side != Direction.WEST) require(helper, portOrNull(helper, FIRST, side) == null,
                        "列尾在错误面暴露流体口：" + side);
                if (side != Direction.EAST) require(helper, portOrNull(helper, LAST, side) == null,
                        "列首在错误面暴露流体口：" + side);
                require(helper, portOrNull(helper, MIDDLE, side) == null,
                        "中间机错误暴露外部流体口：" + side);
            }
            require(helper, inlet.fill(hot(5000), IFluidHandler.FluidAction.SIMULATE) == 5000
                    && first.ledger().hot() == 0, "模拟注液改动库存");
            require(helper, inlet.fill(hot(5000), IFluidHandler.FluidAction.EXECUTE) == 5000
                    && first.ledger().hot() == 4000 && middle.ledger().hot() == 1000,
                    "列尾实注未分布到成员本地库存");
            require(helper, inlet.fill(cold(100), IFluidHandler.FluidAction.EXECUTE) == 0
                    && outlet.fill(hot(100), IFluidHandler.FluidAction.EXECUTE) == 0
                    && inlet.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "错液或逆向交易未拒绝");
            var firstSaved = first.savePortableData();
            var middleSaved = middle.savePortableData();
            var lastSaved = last.savePortableData();
            wrench(helper, MIDDLE);
            require(helper, inlet.fill(hot(1), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "旋转后旧句柄继续作用于原直列");
            require(helper, firstSaved.equals(first.savePortableData())
                    && middleSaved.equals(middle.savePortableData())
                    && lastSaved.equals(last.savePortableData()), "旋转凭空移动成员库存或储备");
            require(helper, port(helper, FIRST, Direction.WEST).getTankCapacity(0) == 4000
                    && port(helper, LAST, Direction.EAST).getTankCapacity(0) == 4000,
                    "中间转向后两端未分裂成单机库存");
            for (int i = 0; i < 3; i++) wrench(helper, MIDDLE);
            require(helper, middle.getBlockState().getValue(NuclearHeatExchangerBlock.FACING) == Direction.EAST,
                    "扳手四次旋转未回到原朝向");
            require(helper, inlet.fill(hot(1), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "转向恢复后旧能力句柄复活");
            require(helper, port(helper, FIRST, Direction.WEST).getTankCapacity(0) == 12000,
                    "同向恢复未重新合并直列");
            var middleShare = middle.savePortableData();
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var drops = net.minecraft.world.level.block.Block.getDrops(middle.getBlockState(), helper.getLevel(),
                    helper.absolutePos(MIDDLE), middle, player,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
            var portable = drops.stream().filter(item -> item.is(HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER_ITEM.get()))
                    .findFirst().orElseThrow();
            var data = portable.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            require(helper, data != null && middleShare.equals(data.copyTag().getCompound("CniHeatExchanger")),
                    "中间机掉落物未只携带本机库存和已付热");
            helper.setBlock(MIDDLE, Blocks.AIR);
            require(helper, port(helper, FIRST, Direction.WEST).getTankCapacity(0) == 4000
                    && port(helper, LAST, Direction.EAST).getTankCapacity(0) == 4000,
                    "中间拆除后剩余成员未独立");
            require(helper, middleShare.getInt("Hot") == 1000 && first.ledger().hot() == 4000
                    && last.ledger().hot() == 0, "拆机份额被复制或搬到其他成员");
            require(helper, outlet.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "拆分后旧输出句柄未失效");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void sideReverseAndSeventeenMachineLineStayIsolated(GameTestHelper helper) {
        line(helper, 3);
        helper.setBlock(FIRST.south(), exchanger(Direction.EAST));
        helper.setBlock(LAST.east(), exchanger(Direction.WEST));
        helper.runAfterDelay(5, () -> {
            require(helper, port(helper, FIRST, Direction.WEST).getTankCapacity(0) == 12000,
                    "侧邻或反向机器被错误并入直列");
            require(helper, port(helper, FIRST.south(), Direction.WEST).getTankCapacity(0) == 4000,
                    "侧邻单机容量错误");
            require(helper, port(helper, LAST.east(), Direction.EAST).getTankCapacity(0) == 4000,
                    "反向单机容量错误");
            for (int x = 1; x <= 17; x++) helper.setBlock(new BlockPos(x, 2, 6), exchanger(Direction.EAST));
        });
        helper.runAfterDelay(10, () -> {
            for (int x = 1; x <= 17; x++) {
                BlockPos pos = new BlockPos(x, 2, 6);
                require(helper, portOrNull(helper, pos, Direction.WEST) == null
                        && portOrNull(helper, pos, Direction.EAST) == null,
                        "十七台超限列出现局部可用管口：" + x);
                require(helper, machine(helper, pos).ledger().hot() == 0,
                        "超限列出现库存复制：" + x);
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pipesPlacedBeforeMembersReconnectAfterLoadAndSplit(GameTestHelper helper) {
        BlockPos inputPipe = FIRST.west();
        BlockPos outputPipe = LAST.east();
        helper.setBlock(inputPipe, AllBlocks.FLUID_PIPE.getDefaultState());
        helper.setBlock(outputPipe, AllBlocks.FLUID_PIPE.getDefaultState());
        line(helper, 3);
        helper.runAfterDelay(5, () -> {
            require(helper, hasConnection(helper, inputPipe, Direction.EAST)
                    && hasConnection(helper, outputPipe, Direction.WEST),
                    "机器晚于管道加载后 Create 连接未自动重开");
            require(helper, port(helper, FIRST, Direction.WEST).getTankCapacity(0) == 12000
                    && port(helper, LAST, Direction.EAST).getTankCapacity(0) == 12000,
                    "先管后机未形成完整三台直列能力");
            helper.setBlock(MIDDLE, Blocks.AIR);
        });
        helper.runAfterDelay(12, () -> {
            require(helper, hasConnection(helper, inputPipe, Direction.EAST)
                    && hasConnection(helper, outputPipe, Direction.WEST),
                    "中间成员拆除后已加载端管失去连接");
            helper.setBlock(MIDDLE, exchanger(Direction.EAST));
        });
        helper.runAfterDelay(20, () -> {
            require(helper, hasConnection(helper, inputPipe, Direction.EAST)
                    && hasConnection(helper, outputPipe, Direction.WEST)
                    && port(helper, FIRST, Direction.WEST).getTankCapacity(0) == 12000,
                    "直列恢复后 Create 连接或组能力未自动恢复");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "heat_chain_liveness")
    public static void nonFirstMemberStopsTickAndOldEndpointHandleNeverRevives(GameTestHelper helper) {
        int x = 15 - Math.floorMod(helper.absolutePos(BlockPos.ZERO).getX(), 16);
        BlockPos firstPos = new BlockPos(x, 2, 7);
        BlockPos middlePos = firstPos.east();
        BlockPos lastPos = middlePos.east();
        for (BlockPos pos : new BlockPos[]{firstPos, middlePos, lastPos}) helper.setBlock(pos, exchanger(Direction.EAST));
        var chunk = helper.getLevel().getChunkAt(helper.absolutePos(middlePos));
        var previous = nativeFullStatusSupplier(chunk);
        IFluidHandler[] old = {null};
        helper.runAfterDelay(5, () -> {
            try {
                old[0] = port(helper, firstPos, Direction.WEST);
                require(helper, old[0].getTankCapacity(0) == 12000, "停tick前跨区块直列未形成");
                require(helper, (helper.absolutePos(firstPos).getX() >> 4)
                                != (helper.absolutePos(middlePos).getX() >> 4),
                        "夹具未跨越区块边界");
                chunk.setFullStatus(() -> net.minecraft.server.level.FullChunkStatus.FULL);
            } catch (RuntimeException error) {
                chunk.setFullStatus(previous);
                throw error;
            }
        });
        helper.runAfterDelay(12, () -> {
            try {
                require(helper, !machine(helper, middlePos).canTick()
                        && old[0].fill(hot(1), IFluidHandler.FluidAction.EXECUTE) == 0,
                        "非首成员停tick后旧端点能力仍可交易");
                require(helper, portOrNull(helper, firstPos, Direction.WEST) == null,
                        "非活动成员仍被截断为可用短列");
            } finally {
                chunk.setFullStatus(previous);
            }
        });
        helper.runAfterDelay(20, () -> {
            var fresh = port(helper, firstPos, Direction.WEST);
            require(helper, fresh != old[0] && fresh.getTankCapacity(0) == 12000
                    && old[0].fill(hot(1), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "成员恢复后旧端点句柄复活或新能力未刷新");
            require(helper, machine(helper, lastPos).ledger().hot() == 0,
                    "停tick恢复复制了成员库存");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 580)
    public static void threeMachineNativeBoilerUsesBothEndCreatePipesAndRemoteInventory(GameTestHelper helper) {
        line(helper, 3);
        nativeBoiler(helper);
        inputPipe(helper);
        outputPipe(helper);
        boolean[] middleConvertedFromRemote = {false};
        boolean[] coldReachedOutput = {false};
        boolean[] started = {false};
        int[] accepted = {0};
        int[] returned = {0};
        helper.onEachTick(() -> {
            var controller = controller(helper);
            if (controller != null) {
                IFluidHandler water = portOrNull(helper, FIRST.above(), Direction.NORTH);
                if (water != null) water.fill(new FluidStack(Fluids.WATER, 180), IFluidHandler.FluidAction.EXECUTE);
            }
            // 锅炉真实负载成立后才让 Create 管路送热，确保中央机从空本地热罐开始换热。
            if (HeatExchangerBoilerBridge.qualified(controller)) {
                IFluidHandler hotSource = portOrNull(helper, new BlockPos(2, 2, 8), Direction.NORTH);
                if (hotSource != null) {
                    if (!started[0]) {
                        accepted[0] += hotSource.fill(hot(4000), IFluidHandler.FluidAction.EXECUTE);
                        started[0] = true;
                    }
                    accepted[0] += hotSource.fill(hot(128), IFluidHandler.FluidAction.EXECUTE);
                }
            }
            var middle = machine(helper, MIDDLE);
            middleConvertedFromRemote[0] |= middle.ledger().converted() > 0 && middle.ledger().hot() == 0;
            IFluidHandler outputTank = portOrNull(helper, new BlockPos(8, 2, 2), Direction.WEST);
            coldReachedOutput[0] |= outputTank != null && !outputTank.getFluidInTank(0).isEmpty()
                    && ModFluids.isCompoundCoolant(outputTank.getFluidInTank(0));
            if (outputTank != null && ModFluids.isCompoundCoolant(outputTank.getFluidInTank(0)))
                returned[0] += outputTank.drain(8000, IFluidHandler.FluidAction.EXECUTE).getAmount();
        });
        helper.runAfterDelay(520, () -> {
            var first = machine(helper, FIRST);
            var middle = machine(helper, MIDDLE);
            var last = machine(helper, LAST);
            require(helper, started[0] && middleConvertedFromRemote[0],
                    "中央机本地无热液时未从整列取热转换：启动=" + started[0]
                            + "，累计注源=" + accepted[0]
                            + "，源余=" + fluidAmount(port(helper, new BlockPos(2, 2, 8), Direction.NORTH))
                            + "，输入/输出泵速=" + ((PumpBlockEntity) helper.getBlockEntity(new BlockPos(2, 2, 6))).getSpeed()
                            + "/" + ((PumpBlockEntity) helper.getBlockEntity(new BlockPos(6, 2, 2))).getSpeed()
                            + "，入口/出口管=" + pipeDiagnostic(helper, new BlockPos(1, 2, 2), Direction.EAST)
                            + "/" + pipeDiagnostic(helper, new BlockPos(5, 2, 2), Direction.WEST)
                            + "，第一/中/末热液=" + first.ledger().hot() + "/" + middle.ledger().hot()
                            + "，第一/中/末冷液=" + first.ledger().cold() + "/" + middle.ledger().cold()
                            + "/" + last.ledger().cold()
                            + "/" + last.ledger().hot() + "，中央转换=" + middle.ledger().converted()
                            + "，储备=" + middle.ledger().reserve()
                            + "，锅炉有效=" + HeatExchangerBoilerBridge.qualified(controller(helper)));
            require(helper, coldReachedOutput[0], "列首Create管线未将冷液送入输出储罐");
            require(helper, first.ledger().reserve() > 0 && middle.ledger().reserve() > 0
                    && last.ledger().reserve() > 0, "三台未独立建立已付HU储备");
            int inventory = first.ledger().hot() + middle.ledger().hot() + last.ledger().hot()
                    + first.ledger().cold() + middle.ledger().cold() + last.ledger().cold();
            int sourceAmount = fluidAmount(port(helper, new BlockPos(2, 2, 8), Direction.NORTH));
            int outputAmount = fluidAmount(port(helper, new BlockPos(8, 2, 2), Direction.WEST));
            require(helper, accepted[0] == inventory + sourceAmount + outputAmount + returned[0],
                    "三机Create回路冷热总量不守恒：输入=" + accepted[0]
                            + "，库存=" + inventory + "，源=" + sourceAmount + "，回收=" + (outputAmount + returned[0]));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 220)
    public static void fullLocalColdTankUsesOtherMembersReturnSpace(GameTestHelper helper) {
        line(helper, 3);
        nativeBoiler(helper);
        boolean[] centralWorked = {false};
        helper.runAfterDelay(5, () -> {
            var first = machine(helper, FIRST);
            var middle = machine(helper, MIDDLE);
            var full = middle.savePortableData();
            full.putInt("Cold", 4000);
            middle.loadPortableData(full);
            require(helper, port(helper, FIRST, Direction.WEST).fill(hot(4000),
                    IFluidHandler.FluidAction.EXECUTE) == 4000, "远端热液未进入直列");
            require(helper, middle.ledger().hot() == 0 && middle.ledger().cold() == 4000
                    && first.ledger().hot() == 4000, "夹具未构造本机空热满冷场景");
        });
        helper.onEachTick(() -> {
            IFluidHandler water = portOrNull(helper, FIRST.above(), Direction.NORTH);
            if (water != null) water.fill(new FluidStack(Fluids.WATER, 180), IFluidHandler.FluidAction.EXECUTE);
            var middle = machine(helper, MIDDLE);
            centralWorked[0] |= middle.ledger().converted() > 0 && middle.ledger().hot() == 0
                    && middle.ledger().cold() == 4000 && middle.ledger().reserve() > 0;
        });
        helper.runAfterDelay(160, () -> {
            require(helper, centralWorked[0], "中央机本地空热满冷时未使用整列热液及冷空间");
            int total = 0;
            for (BlockPos pos : new BlockPos[]{FIRST, MIDDLE, LAST})
                total += machine(helper, pos).ledger().hot() + machine(helper, pos).ledger().cold();
            require(helper, total == 8000, "跨成员换热未保持热冷总量守恒");
            helper.succeed();
        });
    }

    /** 方块与库存均使用世界真实能力；只有机器内部转换借用全列视图。 */
    private static void line(GameTestHelper helper, int count) {
        for (int x = 0; x < count; x++) helper.setBlock(FIRST.east(x), exchanger(Direction.EAST));
    }

    /** 调用真实 Create 扳手交互，覆盖旋转后全列能力缓存失效。 */
    private static void wrench(GameTestHelper helper, BlockPos pos) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, AllItems.WRENCH.asStack());
        BlockPos absolute = helper.absolutePos(pos);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        var block = (NuclearHeatExchangerBlock) HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get();
        require(helper, block.onWrenched(helper.getLevel().getBlockState(absolute),
                new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit))
                .consumesAction(), "真实Create扳手未完成换热器旋转");
    }

    private static net.minecraft.world.level.block.state.BlockState exchanger(Direction facing) {
        return HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                .setValue(NuclearHeatExchangerBlock.FACING, facing);
    }

    private static void nativeBoiler(GameTestHelper helper) {
        for (int x = 2; x <= 4; x++) for (int z = 2; z <= 4; z++)
            helper.setBlock(new BlockPos(x, 3, z), AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(new BlockPos(1, 3, 3), AllBlocks.STEAM_ENGINE.getDefaultState()
                .setValue(SteamEngineBlock.FACE, AttachFace.WALL)
                .setValue(SteamEngineBlock.FACING, Direction.WEST));
    }

    private static void inputPipe(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 2, 8), AllBlocks.FLUID_TANK.getDefaultState());
        for (BlockPos pos : new BlockPos[]{new BlockPos(2, 2, 7), new BlockPos(2, 2, 5),
                new BlockPos(1, 2, 5), new BlockPos(1, 2, 4), new BlockPos(1, 2, 3),
                new BlockPos(1, 2, 2)}) helper.setBlock(pos, AllBlocks.FLUID_PIPE.getDefaultState());
        helper.setBlock(new BlockPos(2, 2, 6), AllBlocks.MECHANICAL_PUMP.getDefaultState()
                .setValue(PumpBlock.FACING, Direction.NORTH));
        helper.setBlock(new BlockPos(3, 2, 6), AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(new BlockPos(3, 2, 7), AllBlocks.SHAFT.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(new BlockPos(3, 2, 8), AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(new BlockPos(3, 2, 8))).generatedSpeed.setValue(256);
        propagateAndSeal(helper, new BlockPos[]{new BlockPos(2, 2, 7), new BlockPos(2, 2, 5),
                new BlockPos(1, 2, 5), new BlockPos(1, 2, 4), new BlockPos(1, 2, 3),
                new BlockPos(1, 2, 2)});
    }

    private static void outputPipe(GameTestHelper helper) {
        helper.setBlock(new BlockPos(8, 2, 2), AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(new BlockPos(5, 2, 2), AllBlocks.FLUID_PIPE.getDefaultState());
        helper.setBlock(new BlockPos(6, 2, 2), AllBlocks.MECHANICAL_PUMP.getDefaultState()
                .setValue(PumpBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(7, 2, 2), AllBlocks.FLUID_PIPE.getDefaultState());
        helper.setBlock(new BlockPos(6, 2, 3), AllBlocks.COGWHEEL.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        helper.setBlock(new BlockPos(7, 2, 3), AllBlocks.SHAFT.getDefaultState()
                .setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        helper.setBlock(new BlockPos(8, 2, 3), AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.WEST));
        ((CreativeMotorBlockEntity) helper.getBlockEntity(new BlockPos(8, 2, 3))).generatedSpeed.setValue(256);
        propagateAndSeal(helper, new BlockPos[]{new BlockPos(5, 2, 2), new BlockPos(7, 2, 2)});
    }

    /** 与已验收 Create 回路夹具相同：刷新真实流体拓扑，并封闭空气开放端。 */
    private static void propagateAndSeal(GameTestHelper helper, BlockPos[] pipes) {
        for (BlockPos pipe : pipes) FluidPropagator.propagateChangedPipe(helper.getLevel(),
                helper.absolutePos(pipe), helper.getLevel().getBlockState(helper.absolutePos(pipe)));
        for (BlockPos pipe : pipes) {
            FluidTransportBehaviour behaviour = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(pipe),
                    FluidTransportBehaviour.TYPE);
            require(helper, behaviour != null, "Create管缺少正式流体行为：" + pipe);
            for (Direction side : Direction.values()) {
                if (behaviour.getConnection(side) != null && helper.getBlockState(pipe.relative(side)).isAir()
                        && FluidPropagator.isOpenEnd(helper.getLevel(), helper.absolutePos(pipe), side))
                    helper.setBlock(pipe.relative(side), Blocks.IRON_BLOCK);
            }
        }
        for (BlockPos pipe : pipes) FluidPropagator.propagateChangedPipe(helper.getLevel(),
                helper.absolutePos(pipe), helper.getLevel().getBlockState(helper.absolutePos(pipe)));
    }

    private static String pipeDiagnostic(GameTestHelper helper, BlockPos pipe, Direction side) {
        FluidTransportBehaviour behaviour = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(pipe),
                FluidTransportBehaviour.TYPE);
        if (behaviour == null) return "无行为";
        var connection = behaviour.getConnection(side);
        var flow = behaviour.getFlow(side);
        return "连接=" + (connection != null) + ",流=" + (flow == null ? "无" :
                flow.fluid.getAmount() + "mB/完整=" + flow.complete + "/入向=" + flow.inbound);
    }

    private static boolean hasConnection(GameTestHelper helper, BlockPos pipe, Direction side) {
        FluidTransportBehaviour behaviour = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(pipe),
                FluidTransportBehaviour.TYPE);
        return behaviour != null && behaviour.getConnection(side) != null;
    }

    private static NuclearHeatExchangerBlockEntity machine(GameTestHelper helper, BlockPos pos) {
        return (NuclearHeatExchangerBlockEntity) helper.getBlockEntity(pos);
    }

    private static FluidTankBlockEntity controller(GameTestHelper helper) {
        return helper.getBlockEntity(FIRST.above()) instanceof FluidTankBlockEntity tank ? tank.getControllerBE() : null;
    }

    private static IFluidHandler port(GameTestHelper helper, BlockPos pos, Direction side) {
        IFluidHandler handler = portOrNull(helper, pos, side);
        require(helper, handler != null, "流体能力不存在：" + pos + " / " + side);
        return handler;
    }

    private static IFluidHandler portOrNull(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), side);
    }

    private static FluidStack hot(int amount) {
        return new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), amount);
    }

    private static FluidStack cold(int amount) {
        return new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), amount);
    }

    private static int fluidAmount(IFluidHandler handler) {
        int amount = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) amount += handler.getFluidInTank(tank).getAmount();
        return amount;
    }

    /** 保存真实 LevelChunk 的原生状态源，测试结束精确恢复。 */
    @SuppressWarnings("unchecked")
    private static java.util.function.Supplier<net.minecraft.server.level.FullChunkStatus> nativeFullStatusSupplier(
            net.minecraft.world.level.chunk.LevelChunk chunk) {
        try {
            var field = net.minecraft.world.level.chunk.LevelChunk.class.getDeclaredField("fullStatus");
            field.setAccessible(true);
            return (java.util.function.Supplier<net.minecraft.server.level.FullChunkStatus>) field.get(chunk);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法保存原生FullStatus门", exception);
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
