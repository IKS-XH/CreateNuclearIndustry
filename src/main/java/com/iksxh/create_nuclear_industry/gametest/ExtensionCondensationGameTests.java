package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlock;
import com.iksxh.create_nuclear_industry.heat.HeatExchangerBoilerBridge;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import java.util.ArrayList;
import java.util.List;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 冷凝合同的真实服务端入口；测试世界与用户存档完全隔离。 */
@GameTestHolder("create_nuclear_industry_condensation")
@PrefixGameTestTemplate(false)
public final class ExtensionCondensationGameTests {
    private static final BlockPos FIRST = new BlockPos(2, 2, 2);
    /** 普通汽轮机排汽必须能由原换热器背口接收并按54mB/t冷凝。 */
    @GameTest(template = "p0_probe_empty", timeoutTicks = 80)
    public static void ordinarySteamCondenses(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState());
        helper.setBlock(pos.above(), Blocks.BLUE_ICE);
        helper.runAfterDelay(10, () -> {
            var machine = (NuclearHeatExchangerBlockEntity) helper.getBlockEntity(pos);
            var input = machine.fluidPort(Direction.SOUTH);
            var before = machine.savePortableData();
            helper.assertTrue(input.fill(new FluidStack(TurbineContent.STEAM.get(), 100),
                    IFluidHandler.FluidAction.SIMULATE) == 100 && before.equals(machine.savePortableData()), "模拟选择了模式");
            helper.assertTrue(input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 100),
                    IFluidHandler.FluidAction.EXECUTE) == 0, "误收超临界蒸汽");
            helper.assertTrue(input != null && input.fill(new FluidStack(TurbineContent.STEAM.get(), 100),
                    IFluidHandler.FluidAction.EXECUTE) == 100, "换热器拒收普通蒸汽");
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(machine.ledger().cold() == 100, "普通蒸汽未产生等量水");
                helper.assertTrue(machine.fluidPort(Direction.NORTH).getFluidInTank(0).is(Fluids.WATER), "出口误报冷却剂");
                helper.assertTrue(machine.publishedHeat() < 0 && machine.ledger().reserve() == 0, "冷凝冒充锅炉热");
                var view = machine.getUpdateTag(helper.getLevel().registryAccess()).getCompound("View");
                helper.assertTrue(view.getString("Mode").equals("CONDENSATION") && view.getDouble("Nominal") == 54,
                        "服务端冷凝模式/额定流量未同步");
                helper.succeed();
            });
        });
    }

    /** 顶格五类冷源各自阶段边界；近边界状态来自本版本正常保存，并断言融水不进入机内。 */
    @GameTest(template = "chain_empty", timeoutTicks = 120)
    public static void topSourcesMeltEvaporateAndPersistProgress(GameTestHelper helper) {
        var sources = new net.minecraft.world.level.block.Block[]{Blocks.SNOW_BLOCK, Blocks.ICE,
                Blocks.PACKED_ICE, Blocks.WATER, Blocks.BLUE_ICE};
        var kinds = new String[]{"SNOW", "ICE", "PACKED_ICE", "WATER", "BLUE_ICE"};
        for (int i = 0; i < sources.length; i++) {
            var pos = new BlockPos(2 + i * 2, 2, 2);
            helper.setBlock(pos, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
            helper.setBlock(pos.above(), sources[i]);
            // 封住水的横向流动，确保单格孤立水阶段不会被邻格补水。
            for (Direction d : Direction.Plane.HORIZONTAL) helper.setBlock(pos.above().relative(d), Blocks.IRON_BLOCK);
        }
        helper.runAfterDelay(10, () -> {
            var cfg = HeatExchangerConfig.condensationSettings();
            helper.assertTrue(cfg.rateMbPerTick() == 54 && cfg.recoveryPermille() == 1000
                    && cfg.steamCapacityMb() == 4000 && cfg.waterCapacityMb() == 4000
                    && cfg.snowAfterMb() == 100000 && cfg.iceAfterMb() == 100000
                    && cfg.packedIceAfterMb() == 900000 && cfg.waterAfterMb() == 100000,
                    "实际SERVER默认值不符合八项合同");
            for (int i = 0; i < sources.length; i++) {
                var pos = new BlockPos(2 + i * 2, 2, 2);
                var m = machine(helper, pos);
                m.fluidPort(Direction.SOUTH).fill(steam(1000), IFluidHandler.FluidAction.EXECUTE);
                var saved = m.savePortableData();
                var c = saved.getCompound("Condensation");
                c.putString("Source", kinds[i]);
                c.putLong("Consumed", i == 2 ? 899993 : i == 4 ? 0 : 99993);
                m.loadPortableData(saved);
            }
            helper.runAfterDelay(1, () -> {
                for (int i = 0; i < 4; i++) {
                    var pos = new BlockPos(2 + i * 2, 2, 2);
                    helper.assertTrue(helper.getBlockState(pos.above()).is(i == 3 ? Blocks.AIR : Blocks.WATER),
                            "顶部冷源未在当前阶段边界变化：" + kinds[i]);
                    helper.assertTrue(machine(helper, pos).ledger().cold() == 7, "跨冷源阶段增产或融水入罐");
                }
                for (int i = 0; i < 3; i++) {
                    var m = machine(helper, new BlockPos(2 + i * 2, 2, 2));
                    var saved = m.savePortableData();
                    saved.getCompound("Condensation").putLong("Consumed", 99993);
                    m.loadPortableData(saved);
                }
                helper.runAfterDelay(1, () -> {
                    for (int i = 0; i < 3; i++) {
                        var pos = new BlockPos(2 + i * 2, 2, 2);
                        helper.assertTrue(helper.getBlockState(pos.above()).isAir() && machine(helper, pos).ledger().cold() == 14,
                                "融化后的水未独立蒸发或重复加入回水");
                    }
                    var blue = machine(helper, new BlockPos(10, 2, 2));
                    helper.assertTrue(helper.getBlockState(new BlockPos(10, 3, 2)).is(Blocks.BLUE_ICE)
                            && blue.ledger().condensation().consumed() == 0 && blue.ledger().cold() == 108,
                            "蓝冰应持续冷凝且不累计消耗");
                    helper.succeed();
                });
            });
        });
    }

    /** 输出满、顶部缺失、流水、雪层和含水方块均暂停；恢复冷源及空位后继续守恒。 */
    @GameTest(template = "chain_empty", timeoutTicks = 100)
    public static void blockedAndInvalidSourcesResumeWithoutConsumption(GameTestHelper helper) {
        helper.setBlock(FIRST, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
        helper.setBlock(FIRST.east(), Blocks.BLUE_ICE);
        helper.runAfterDelay(10, () -> {
            var m = machine(helper, FIRST);
            m.fluidPort(Direction.SOUTH).fill(steam(100), IFluidHandler.FluidAction.EXECUTE);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(m.ledger().hot() == 100 && m.ledger().status().equals("no_cold_source"), "侧面冷源错误生效");
                helper.setBlock(FIRST.above(), Blocks.WATER.defaultBlockState().setValue(BlockStateProperties.LEVEL, 1));
                // 先直接运行当前tick以阻止原版水更新把测试流水换成其他形态。
                var saved = m.savePortableData(); saved.putLong("LastTick", helper.getLevel().getGameTime() - 1);
                m.loadPortableData(saved);
                NuclearHeatExchangerBlockEntity.serverTick(helper.getLevel(), m.getBlockPos(), m.getBlockState(), m);
                helper.assertTrue(m.ledger().converted() == 0, "流水错误生效");
                helper.setBlock(FIRST.above(), Blocks.SNOW);
                saved = m.savePortableData(); saved.putLong("LastTick", helper.getLevel().getGameTime() - 1); m.loadPortableData(saved);
                NuclearHeatExchangerBlockEntity.serverTick(helper.getLevel(), m.getBlockPos(), m.getBlockState(), m);
                helper.assertTrue(m.ledger().converted() == 0, "雪层错误生效");
                helper.setBlock(FIRST.above(), Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
                saved = m.savePortableData(); saved.putLong("LastTick", helper.getLevel().getGameTime() - 1); m.loadPortableData(saved);
                NuclearHeatExchangerBlockEntity.serverTick(helper.getLevel(), m.getBlockPos(), m.getBlockState(), m);
                helper.assertTrue(m.ledger().converted() == 0, "含水方块错误生效");
                helper.setBlock(FIRST.above(), Blocks.BLUE_ICE);
                saved = m.savePortableData(); saved.putInt("Cold", 4000); m.loadPortableData(saved);
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(m.ledger().hot() == 100 && m.ledger().status().equals("water_full"), "出水满仍消耗输入");
                    helper.assertTrue(m.ledger().condensation().consumed() == 0, "暂停消耗冷源");
                    m.fluidPort(Direction.NORTH).drain(100, IFluidHandler.FluidAction.EXECUTE);
                    helper.runAfterDelay(2, () -> {
                        helper.assertTrue(m.ledger().hot() == 0 && m.ledger().cold() == 4000, "疏通后未恢复等量冷凝");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** 各成员54mB/t共用真实汽/水库存；混列不可交易，拆开只保留本机份额并恢复。 */
    @GameTest(template = "chain_empty", timeoutTicks = 100)
    public static void lineQuotasAndMixedModesPreserveInventory(GameTestHelper helper) {
        for (int i = 0; i < 3; i++) {
            helper.setBlock(FIRST.east(i), HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                    .setValue(NuclearHeatExchangerBlock.FACING, Direction.EAST));
            if (i != 1) helper.setBlock(FIRST.east(i).above(), Blocks.BLUE_ICE);
        }
        helper.runAfterDelay(10, () -> {
            var first = machine(helper, FIRST); var middle = machine(helper, FIRST.east()); var last = machine(helper, FIRST.east(2));
            first.fluidPort(Direction.WEST).fill(steam(1000), IFluidHandler.FluidAction.EXECUTE);
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(first.ledger().converted() == 54 && middle.ledger().converted() == 0
                        && last.ledger().converted() == 54 && total(helper, 3) == 1000, "成员额度或共享库存守恒错误");
                helper.assertTrue(middle.fluidPort(Direction.NORTH) == null, "中段开放接口");
                var mixed = middle.savePortableData(); mixed.putString("Mode", "NUCLEAR"); mixed.putInt("Hot", 100); mixed.putInt("Cold", 0);
                middle.loadPortableData(mixed);
                var snapshots = List.of(first.savePortableData(), middle.savePortableData(), last.savePortableData());
                helper.assertTrue(first.fluidPort(Direction.WEST).fill(steam(1), IFluidHandler.FluidAction.EXECUTE) == 0
                        && last.fluidPort(Direction.EAST).drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "混列能力串工质");
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(first.ledger().status().equals("mode_conflict") && middle.ledger().hot() == 100,
                            "混列未显示冲突或修改库存");
                    helper.assertTrue(first.ledger().hot() == snapshots.get(0).getInt("Hot")
                            && first.ledger().cold() == snapshots.get(0).getInt("Cold"), "冲突列偷偷转换");
                    helper.setBlock(FIRST.east(), Blocks.AIR);
                    helper.runAfterDelay(2, () -> {
                        helper.assertTrue(first.ledger().status().equals("condensing") && first.ledger().converted() == 54,
                                "拆开冲突成员未恢复");
                        helper.assertTrue(last.ledger().hot() + last.ledger().cold() == snapshots.get(2).getInt("Hot")
                                + snapshots.get(2).getInt("Cold"), "拆分复制成员份额");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** 非默认SERVER回收率在同一服务端回调内替换并恢复；没有中间世界tick或其他测试读取窗口。 */
    @GameTest(template = "chain_empty", timeoutTicks = 80)
    public static void serverNonDefaultRecoveryAndGoggleFlowApply(GameTestHelper helper) {
        helper.setBlock(FIRST, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
        helper.setBlock(FIRST.above(), Blocks.BLUE_ICE);
        helper.runAfterDelay(10, () -> {
            int original = HeatExchangerConfig.CONDENSATION_RECOVERY.get();
            try {
                HeatExchangerConfig.CONDENSATION_RECOVERY.set(333);
                var m = machine(helper, FIRST);
                m.fluidPort(Direction.SOUTH).fill(steam(54), IFluidHandler.FluidAction.EXECUTE);
                var saved = m.savePortableData(); saved.putLong("LastTick", helper.getLevel().getGameTime() - 1); m.loadPortableData(saved);
                NuclearHeatExchangerBlockEntity.serverTick(helper.getLevel(), m.getBlockPos(), m.getBlockState(), m);
                helper.assertTrue(m.ledger().cold() == 17 && m.ledger().condensation().remainder() == 982,
                        "SERVER非默认333千分比未生效");
                var view = m.getUpdateTag(helper.getLevel().registryAccess()).getCompound("View");
                helper.assertTrue(view.getInt("Flow") == 54 && view.getInt("LineCold") == 17, "实际冷凝流量未同步");
                List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
                m.addToGoggleTooltip(tooltip, false);
                helper.assertTrue(tooltip.size() == 5, "冷凝护目镜页面仍冒充热等级");
            } finally { HeatExchangerConfig.CONDENSATION_RECOVERY.set(original); }
            helper.assertTrue(HeatExchangerConfig.condensationSettings().recoveryPermille() == 1000, "测试未恢复SERVER配置");
            helper.succeed();
        });
    }

    /** 代表性原核热模式仍向真实Create小锅炉提供18级付费热，不要求新冷源。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 320)
    public static void nativeNuclearHeatStillPublishesPaidHeat(GameTestHelper helper) {
        BlockPos base = new BlockPos(2, 2, 2);
        helper.setBlock(base.below(), HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) helper.setBlock(base.offset(x, 0, z), AllBlocks.FLUID_TANK.get());
        helper.setBlock(base.west(), AllBlocks.STEAM_ENGINE.getDefaultState().setValue(SteamEngineBlock.FACE, AttachFace.WALL)
                .setValue(SteamEngineBlock.FACING, Direction.WEST));
        helper.onEachTick(() -> {
            var water = port(helper, base, Direction.NORTH);
            if (water != null) water.fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE);
            var m = machine(helper, base.below());
            var inlet = m.fluidPort(Direction.SOUTH); var outlet = m.fluidPort(Direction.NORTH);
            if (inlet != null) inlet.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 4000), IFluidHandler.FluidAction.EXECUTE);
            if (outlet != null) outlet.drain(4000, IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(260, () -> {
            var controller = ((FluidTankBlockEntity) helper.getBlockEntity(base)).getControllerBE();
            helper.assertTrue(controller.boiler.activeHeat == 18 && machine(helper, base.below()).ledger().converted() == 36,
                    "原核热供热或36mB/t额定转换回归");
            helper.succeed();
        });
    }

    /** 两端真实Create管泵输送普通蒸汽和回水，所有已存流体始终等量守恒。 */
    @GameTest(template = "chain_empty", timeoutTicks = 240)
    public static void actualCreatePumpsCondenseSteamToWater(GameTestHelper helper) {
        for (int i = 0; i < 3; i++) {
            helper.setBlock(FIRST.east(i), HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                    .setValue(NuclearHeatExchangerBlock.FACING, Direction.EAST));
            helper.setBlock(FIRST.east(i).above(), Blocks.BLUE_ICE);
        }
        // 管路布局复用已验收换热器直列夹具，方向对应背口收汽/前口出水。
        helper.setBlock(new BlockPos(2, 2, 8), AllBlocks.FLUID_TANK.get());
        var inputPipes = new BlockPos[]{new BlockPos(2, 2, 7), new BlockPos(2, 2, 5),
                new BlockPos(1, 2, 5), new BlockPos(1, 2, 4), new BlockPos(1, 2, 3), new BlockPos(1, 2, 2)};
        for (var p : inputPipes) helper.setBlock(p, AllBlocks.FLUID_PIPE.getDefaultState());
        helper.setBlock(new BlockPos(2, 2, 6), AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.NORTH));
        drive(helper, new BlockPos(3, 2, 6), new BlockPos(3, 2, 7), new BlockPos(3, 2, 8), Direction.Axis.Z, Direction.NORTH);
        helper.setBlock(new BlockPos(8, 2, 2), AllBlocks.FLUID_TANK.get());
        var outputPipes = new BlockPos[]{new BlockPos(5, 2, 2), new BlockPos(7, 2, 2)};
        for (var p : outputPipes) helper.setBlock(p, AllBlocks.FLUID_PIPE.getDefaultState());
        helper.setBlock(new BlockPos(6, 2, 2), AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.EAST));
        drive(helper, new BlockPos(6, 2, 3), new BlockPos(7, 2, 3), new BlockPos(8, 2, 3), Direction.Axis.X, Direction.WEST);
        seal(helper, inputPipes); seal(helper, outputPipes);
        boolean[] started = {false};
        helper.onEachTick(() -> {
            if (!started[0]) return;
            helper.assertTrue(amount(helper, new BlockPos(2, 2, 8)) + total(helper, 3)
                    + amount(helper, new BlockPos(8, 2, 2)) == 4000, "真实Create泵管汽水不守恒：源="
                    + amount(helper, new BlockPos(2, 2, 8)) + "，列=" + total(helper, 3)
                    + "，回水=" + amount(helper, new BlockPos(8, 2, 2)) + "，tick=" + helper.getTick());
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(port(helper, new BlockPos(2, 2, 8), Direction.NORTH).fill(steam(4000), IFluidHandler.FluidAction.EXECUTE) == 4000,
                    "实际Create源罐不能存普通蒸汽");
            started[0] = true;
        });
        helper.runAfterDelay(170, () -> {
                var output = port(helper, new BlockPos(8, 2, 2), Direction.NORTH).getFluidInTank(0);
                helper.assertTrue(output.is(Fluids.WATER) && output.getAmount() == 4000, "真实泵管未把全部蒸汽冷凝回水：" + output.getAmount());
                helper.succeed();
        });
    }

    /** 非默认容量及缩容只限制新增库存，各台超额存量不能抵销其他成员真实空位。 */
    @GameTest(template = "chain_empty", timeoutTicks = 80)
    public static void capacitySimulationAndShrinkKeepRealMemberSpace(GameTestHelper helper) {
        for (int i = 0; i < 2; i++) helper.setBlock(FIRST.east(i), HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                .setValue(NuclearHeatExchangerBlock.FACING, Direction.EAST));
        helper.runAfterDelay(10, () -> {
            int originalSteam = HeatExchangerConfig.CONDENSATION_STEAM_CAPACITY.get();
            int originalWater = HeatExchangerConfig.CONDENSATION_WATER_CAPACITY.get();
            try {
                HeatExchangerConfig.CONDENSATION_STEAM_CAPACITY.set(350);
                HeatExchangerConfig.CONDENSATION_WATER_CAPACITY.set(350);
                var first = machine(helper, FIRST);
                var inlet = first.fluidPort(Direction.WEST);
                var snapshot = first.savePortableData();
                helper.assertTrue(inlet.fill(steam(1000), IFluidHandler.FluidAction.SIMULATE) == 700
                        && inlet.getTankCapacity(0) == 700 && snapshot.equals(first.savePortableData()),
                        "空机首次模拟容量不符合候选工质或锁定模式");
                var shared = (com.iksxh.create_nuclear_industry.compat.create.SharedFluidReceiver) inlet;
                helper.assertTrue(shared.sharedFluidLimits().inventorySpaceMb() == 700, "模拟后共享额度误报核热容量");
                helper.assertTrue(inlet.fill(steam(350), IFluidHandler.FluidAction.EXECUTE) == 350, "实际接汽未选模式");
                HeatExchangerConfig.CONDENSATION_STEAM_CAPACITY.set(100);
                HeatExchangerConfig.CONDENSATION_WATER_CAPACITY.set(50);
                helper.assertTrue(inlet.fill(steam(500), IFluidHandler.FluidAction.SIMULATE) == 100
                        && shared.sharedFluidLimits().inventorySpaceMb() == 100, "缩容后其他成员空位被超额存量抵销");
                helper.assertTrue(inlet.fill(steam(500), IFluidHandler.FluidAction.EXECUTE) == 100
                        && first.ledger().hot() == 350 && machine(helper, FIRST.east()).ledger().hot() == 100,
                        "缩容截断存量或接汽事务预检与提交不一致");
                helper.assertTrue(machine(helper, FIRST.east()).fluidPort(Direction.EAST).getTankCapacity(0) == 100,
                        "非默认回水容量未按直列成员相加");
            } finally {
                HeatExchangerConfig.CONDENSATION_STEAM_CAPACITY.set(originalSteam);
                HeatExchangerConfig.CONDENSATION_WATER_CAPACITY.set(originalWater);
            }
            helper.succeed();
        });
    }

    private static NuclearHeatExchangerBlockEntity machine(GameTestHelper h, BlockPos p) { return (NuclearHeatExchangerBlockEntity) h.getBlockEntity(p); }
    private static FluidStack steam(int amount) { return new FluidStack(TurbineContent.STEAM.get(), amount); }
    private static IFluidHandler port(GameTestHelper h, BlockPos p, Direction side) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(p), side);
    }
    private static int amount(GameTestHelper h, BlockPos p) { return port(h, p, Direction.NORTH).getFluidInTank(0).getAmount(); }
    private static int total(GameTestHelper h, int count) {
        int result = 0;
        for (int i = 0; i < count; i++) result += machine(h, FIRST.east(i)).ledger().hot() + machine(h, FIRST.east(i)).ledger().cold();
        return result;
    }
    /** 与正式Create传动相同，马达经轴驱动同轴齿轮旁的机械泵。 */
    private static void drive(GameTestHelper h, BlockPos cog, BlockPos shaft, BlockPos motor, Direction.Axis axis, Direction facing) {
        h.setBlock(cog, AllBlocks.COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, axis));
        h.setBlock(shaft, AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, axis));
        h.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, facing));
        ((CreativeMotorBlockEntity) h.getBlockEntity(motor)).generatedSpeed.setValue(256);
    }
    /** 刷新真实管拓扑并封闭空气端点，防止开放排液干扰闭环守恒测试。 */
    private static void seal(GameTestHelper h, BlockPos[] pipes) {
        for (var p : pipes) FluidPropagator.propagateChangedPipe(h.getLevel(), h.absolutePos(p), h.getBlockState(p));
        for (var p : pipes) {
            FluidTransportBehaviour b = BlockEntityBehaviour.get(h.getLevel(), h.absolutePos(p), FluidTransportBehaviour.TYPE);
            for (Direction side : Direction.values()) if (b.getConnection(side) != null
                    && h.getBlockState(p.relative(side)).isAir() && FluidPropagator.isOpenEnd(h.getLevel(), h.absolutePos(p), side))
                h.setBlock(p.relative(side), Blocks.IRON_BLOCK);
        }
        for (var p : pipes) FluidPropagator.propagateChangedPipe(h.getLevel(), h.absolutePos(p), h.getBlockState(p));
    }
}
