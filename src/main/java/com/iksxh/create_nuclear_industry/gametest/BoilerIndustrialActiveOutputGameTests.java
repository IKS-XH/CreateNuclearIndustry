package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.IndustrialFluidContent;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.boiler.BoilerPartBlock;
import com.iksxh.create_nuclear_industry.boiler.BoilerSteamInventoryKind;
import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerPortBlockEntity;
import com.iksxh.create_nuclear_industry.industrial.IndustrialPumpBlock;
import com.iksxh.create_nuclear_industry.industrial.IndustrialPumpBlockEntity;
import com.iksxh.create_nuclear_industry.industrial.RestrictedFluidHandler;
import com.iksxh.create_nuclear_industry.config.TurbineConfig;
import com.iksxh.create_nuclear_industry.turbine.TurbineControllerBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 无外置泵的锅炉主动工业出汽真实域；实际库存、HU与端点接收共同证明事务。 */
@GameTestHolder("create_nuclear_industry_boiler_active_output")
@PrefixGameTestTemplate(false)
public final class BoilerIndustrialActiveOutputGameTests {
    private static final BlockPos STEAM = new BlockPos(4, 5, 5);
    private static final BlockPos TANK = new BlockPos(1, 5, 5);
    private BoilerIndustrialActiveOutputGameTests() {}
    private static BoilerControllerBlockEntity owner(GameTestHelper h) { return ExtensionBoilerGameTests.owner(h); }
    private static FluidTankBlockEntity tank(GameTestHelper h, BlockPos p) { return (FluidTankBlockEntity)h.getBlockEntity(p); }
    private static int amount(GameTestHelper h, BlockPos p) { return tank(h, p).getTankInventory().getFluidAmount(); }
    private static void route(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        h.setBlock(STEAM.west(), IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(STEAM.west(2), IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(TANK, AllBlocks.FLUID_TANK.get());
    }
    private static void seed(GameTestHelper h, int normal, int sc) {
        h.assertTrue(owner(h).currentForm() != null, "主动出汽夹具未成型"); owner(h).selectMinimum(0);
        var tag = owner(h).ledger().save();
        ExtensionBoilerGameTests.seedInventories(tag, normal, normal * 1.1, sc, sc * 1.1); owner(h).ledger().load(tag);
    }
    /** 高/低优先级夹住真实统一Post调度；检查在后续GameTest回调抛出，避免事件异常被误报为服务器失败。 */
    private static void postCheck(GameTestHelper h, int delay, Runnable before, Runnable after) {
        boolean[] armed = {false}, sampled = {false}; RuntimeException[] error = {null};
        java.util.function.Consumer<LevelTickEvent.Post> high = event -> {
            if (event.getLevel() == h.getLevel() && armed[0]) {
                try { before.run(); } catch (RuntimeException e) { error[0] = e; }
            }
        };
        java.util.function.Consumer<LevelTickEvent.Post> low = event -> {
            if (event.getLevel() == h.getLevel() && armed[0]) {
                armed[0] = false; sampled[0] = true;
                if (error[0] == null) try { after.run(); } catch (RuntimeException e) { error[0] = e; }
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, high);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, low);
        h.runAfterDelay(delay, () -> armed[0] = true);
        h.runAfterDelay(delay + 2, () -> {
            NeoForge.EVENT_BUS.unregister(high); NeoForge.EVENT_BUS.unregister(low);
            h.assertTrue(sampled[0], "没有采到真实Post调度"); if (error[0] != null) throw error[0]; h.succeed();
        });
    }

    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 40)
    public static void supercriticalSourcePushesThroughDedicatedPipeWithoutPump(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        h.setBlock(STEAM.west(), IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(STEAM.west(2), IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(TANK, AllBlocks.FLUID_TANK.get());
        h.runAfterDelay(4, () -> {
            var owner = ExtensionBoilerGameTests.owner(h);
            h.assertTrue(owner.currentForm() != null, "RED夹具锅炉未成型");
            owner.selectMinimum(0);
            var tag = owner.ledger().save();
            ExtensionBoilerGameTests.seedInventories(tag, 0, 0, 6000, 6600);
            owner.ledger().load(tag);
            h.assertTrue(owner.ledger().removableSteam(com.iksxh.create_nuclear_industry.boiler.BoilerSteamInventoryKind.SUPERCRITICAL) == 6000,
                    "RED夹具库存欠热或没有保压余量");
        });
        h.runAfterDelay(8, () -> {
            var owner = ExtensionBoilerGameTests.owner(h);
            int received = ((FluidTankBlockEntity)h.getBlockEntity(TANK)).getTankInventory().getFluidAmount();
            System.out.println("[boiler-active] no-pump source=" + owner.ledger().steam() + "; received=" + received
                    + "; SC-HU=" + owner.ledger().steamHu() + "; worldTick=" + h.getLevel().getGameTime());
            h.assertTrue(received > 0, "有效成型且合格SC库存经工业管无泵时没有主动交付");
            h.assertTrue(owner.ledger().steam() + received == 6000, "无泵主动输出mB不守恒");
            h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 40)
    public static void ordinarySourcePushesThroughDedicatedPipeWithoutPump(GameTestHelper h) {
        route(h); h.runAfterDelay(4, () -> BoilerSteamSelectionGameTests.submit(h, STEAM, 0));
        postCheck(h, 6, () -> seed(h, 6000, 0), () -> {
            int received = amount(h, TANK);
            h.assertTrue(received == 4096 && owner(h).ledger().steam(BoilerSteamInventoryKind.NORMAL) == 1904, "普通汽无泵真实物理口额度不是4096");
            h.assertTrue(tank(h, TANK).getTankInventory().getFluid().is(TurbineContent.STEAM.get()), "普通汽主动输出被重标");
            System.out.println("[boiler-active] NORMAL no-pump4096; source1904; sameTick=" + h.getLevel().getGameTime());
        });
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 45)
    public static void shortReceiptRefundsOriginalScAfterTargetChangesFilter(GameTestHelper h) {
        route(h); double[] beforeHu = {0}; boolean[] switched = {false};
        h.runAfterDelay(4, () -> {
            try {
                var receiver = new FluidTank(8000) {
                    @Override public int fill(FluidStack stack, FluidAction action) {
                        if (action.simulate()) beforeHu[0] = owner(h).ledger().steamHu(BoilerSteamInventoryKind.SUPERCRITICAL);
                        int n = super.fill(action.execute() ? stack.copyWithAmount(Math.min(1024, stack.getAmount())) : stack, action);
                        if (action.execute() && !switched[0]) { switched[0] = true; BoilerSteamSelectionGameTests.submit(h, STEAM, 0); }
                        return n;
                    }
                };
                // 限量库存只安装到此隔离测试的真实普通罐，生产罐/能力授权与原模板不改。
                var storage = FluidTankBlockEntity.class.getDeclaredField("tankInventory"); storage.setAccessible(true); storage.set(tank(h, TANK), receiver);
                var capability = FluidTankBlockEntity.class.getDeclaredField("fluidCapability"); capability.setAccessible(true);
                capability.set(tank(h, TANK), RestrictedFluidHandler.wrap(receiver)); h.getLevel().invalidateCapabilities(h.absolutePos(TANK));
            } catch (ReflectiveOperationException e) { throw new IllegalStateException("真实罐短收夹具无法绑定同库", e); }
        });
        postCheck(h, 6, () -> seed(h, 0, 6000), () -> {
            h.assertTrue(switched[0] && amount(h, TANK) == 1024, "真实SC目标没有执行短收/过滤变更");
            var ledger = owner(h).ledger();
            h.assertTrue(ledger.steam(BoilerSteamInventoryKind.SUPERCRITICAL) == 4976 && ledger.steam(BoilerSteamInventoryKind.NORMAL) == 0,
                    "短收退款丢量或写入变更后的普通池");
            h.assertTrue(Math.abs(ledger.steamHu(BoilerSteamInventoryKind.SUPERCRITICAL) - beforeHu[0] * 4976 / 6000) < 1e-6, "短收没有返回本批准确HU");
            h.assertTrue(ledger.remainingDrain(BoilerSteamInventoryKind.SUPERCRITICAL, h.absolutePos(STEAM), h.getLevel().getGameTime()) == 3072,
                    "短收未交付口额度没有退回");
            System.out.println("[boiler-active] short SC1024; source4976; refund3072; filterNORMAL keeps original SC/HU; tick=" + h.getLevel().getGameTime());
        });
    }
    private static final BlockPos[] FOUR = {new BlockPos(4, 6, 5), new BlockPos(4, 6, 6), new BlockPos(4, 7, 5), new BlockPos(4, 7, 6)};
    private static void fourPorts(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 7, 7, 7, 3);
        for (BlockPos p : FOUR) h.setBlock(p, BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.WEST));
        for (int y : new int[]{6, 7}) for (int z : new int[]{5, 6}) h.setBlock(new BlockPos(3, y, z), IndustrialFluidContent.CERAMIC_PIPE.get());
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 45)
    public static void fourActivePortsShareOne8192Trunk(GameTestHelper h) {
        fourPorts(h); BlockPos target = new BlockPos(1, 6, 6);
        h.setBlock(new BlockPos(2, 6, 6), IndustrialFluidContent.REINFORCED_PIPE.get());
        h.setBlock(target, AllBlocks.FLUID_TANK.get()); h.setBlock(target.above(), AllBlocks.FLUID_TANK.get());
        h.setBlock(target.above(2), AllBlocks.FLUID_TANK.get()); h.setBlock(target.above(3), AllBlocks.FLUID_TANK.get());
        postCheck(h, 8, () -> seed(h, 0, 60000), () -> {
            h.assertTrue(amount(h, target) == 16384 && owner(h).ledger().steam() == 43616, "四主动口未在无管道封顶时各交付4096或不守恒");
            for (BlockPos p : FOUR) h.assertTrue(owner(h).ledger().remainingDrain(BoilerSteamInventoryKind.SUPERCRITICAL, h.absolutePos(p), h.getLevel().getGameTime()) >= 0,
                    "主动口预算出现负数");
            System.out.println("[boiler-active] four SC ports actual16384 over unmetered reinforced trunk; source43616");
        });
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 60, batch = "boiler_active_scene")
    public static void savedFifteenNodeFourPortRouteFeedsRealTurbineWithoutPump(GameTestHelper h) {
        fourPorts(h);
        for (int x = -4; x <= 2; x++) h.setBlock(new BlockPos(x, 6, 6), IndustrialFluidContent.CERAMIC_PIPE.get());
        for (int z = 7; z <= 9; z++) h.setBlock(new BlockPos(-4, 6, z), IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(new BlockPos(-4, 5, 9), IndustrialFluidContent.CERAMIC_PIPE.get());
        var tier = TurbineConfig.settings().longTier(); BlockPos front = new BlockPos(-8, 5, 4);
        // 现场长度大于空模板；单独批次清理真实汽轮机体积，避免旧批次外伸方块占据转子空气槽。
        for (int x = -11; x <= -5; x++) for (int y = 2; y <= 8; y++) for (int z = 4; z <= 14; z++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        ExtensionTurbineGameTests.build(h, front, tier.rotorCount(), false);
        h.setBlock(ExtensionTurbineGameTests.inlet(front, tier), TurbineContent.CASING.get());
        BlockPos inlet = new BlockPos(-5, 5, 9);
        h.setBlock(inlet, TurbineContent.INLET.get().defaultBlockState().setValue(TurbinePartBlock.OUTWARD, Direction.EAST));
        BlockPos exhaustTank = ExtensionTurbineGameTests.exhaust(front, tier).east(); h.setBlock(exhaustTank, AllBlocks.FLUID_TANK.get());
        h.runAfterDelay(8, () -> {
            var turbine = (TurbineControllerBlockEntity)h.getBlockEntity(ExtensionTurbineGameTests.controller(front, tier));
            h.assertTrue(owner(h).currentForm() != null && owner(h).currentForm().steamPorts().size() == 4 && turbine.claimedForm() != null,
                    "15节点现场代表锅炉或汽轮机未成型"); seed(h, 0, 70000);
        });
        h.runAfterDelay(18, () -> {
            var turbine = (TurbineControllerBlockEntity)h.getBlockEntity(ExtensionTurbineGameTests.controller(front, tier));
            int output = amount(h, exhaustTank), held = turbine.ledger().exhaust();
            h.assertTrue(output > 0 && owner(h).ledger().steam() + held + output == 70000, "15节点四口无泵至真实汽轮机没有交付或mB不守恒");
            h.assertTrue(tank(h, exhaustTank).getTankInventory().getFluid().is(TurbineContent.STEAM.get()), "汽轮机没有实际普通汽排出");
            System.out.println("[boiler-active] saved15-nodes four-SC→real turbine EAST inlet; source=" + owner(h).ledger().steam() + "; exhaust=" + held + "; discharged=" + output);
            h.succeed();
        });
    }
    private static void outputPump(GameTestHelper h) {
        h.setBlock(STEAM.west(2), IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.WEST));
        h.setBlock(STEAM.west(3), IndustrialFluidContent.CERAMIC_PIPE.get()); h.setBlock(STEAM.west(4), AllBlocks.FLUID_TANK.get());
        h.setBlock(new BlockPos(2, 4, 5), AllBlocks.COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        h.setBlock(new BlockPos(1, 4, 5), AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
        h.setBlock(new BlockPos(0, 4, 5), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.EAST));
        ((CreativeMotorBlockEntity)h.getBlockEntity(new BlockPos(0, 4, 5))).generatedSpeed.setValue(-256);
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 45)
    public static void nativeExtractionActiveSourceAndPumpShareOnePhysicalPort(GameTestHelper h) {
        route(h); h.setBlock(TANK, Blocks.AIR); outputPump(h);
        h.runAfterDelay(4, () -> BoilerSteamSelectionGameTests.submit(h, STEAM, 0));
        int[] nativeTaken = {0}; BlockPos target = STEAM.west(4);
        postCheck(h, 8, () -> {
            seed(h, 8000, 0);
            var pump = (IndustrialPumpBlockEntity)h.getBlockEntity(STEAM.west(2));
            h.assertTrue(pump.flowLimit() > 0 && pump.outputDirection() == Direction.WEST, "共享口夹具泵无合法西向动力");
            var handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(STEAM), Direction.WEST);
            nativeTaken[0] = handler.drain(1024, IFluidHandler.FluidAction.EXECUTE).getAmount();
        }, () -> {
            h.assertTrue(nativeTaken[0] == 1024 && amount(h, target) == 3072 && owner(h).ledger().steam() == 3904,
                    "同tick标准1024+主动/泵路径未共享物理口4096或不守恒");
            System.out.println("[boiler-active] sameTick native1024+active/pump3072=physical4096; source3904; pumpFlow="
                    + ((IndustrialPumpBlockEntity)h.getBlockEntity(STEAM.west(2))).actualFlow());
        });
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 70)
    public static void stoppedAndReversePumpCannotBeBypassedByActiveSource(GameTestHelper h) {
        route(h); h.setBlock(TANK, Blocks.AIR);
        h.setBlock(STEAM.west(2), IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.WEST));
        h.setBlock(STEAM.west(3), IndustrialFluidContent.CERAMIC_PIPE.get()); h.setBlock(STEAM.west(4), AllBlocks.FLUID_TANK.get());
        h.runAfterDelay(5, () -> seed(h, 0, 6000));
        h.runAfterDelay(8, () -> {
            h.assertTrue(amount(h, STEAM.west(4)) == 0 && owner(h).ledger().steam() == 6000, "主动源绕过停泵");
            outputPump(h); h.setBlock(STEAM.west(2), h.getBlockState(STEAM.west(2)).setValue(IndustrialPumpBlock.FACING, Direction.EAST));
        });
        h.runAfterDelay(14, () -> {
            h.assertTrue(amount(h, STEAM.west(4)) == 0 && owner(h).ledger().steam() == 6000, "主动源绕过反向工业泵");
            h.setBlock(STEAM.west(2), h.getBlockState(STEAM.west(2)).setValue(IndustrialPumpBlock.FACING, Direction.WEST));
        });
        h.runAfterDelay(20, () -> { h.assertTrue(amount(h, STEAM.west(4)) == 6000 && owner(h).ledger().steam() == 0, "恢复合法泵方向后没有恢复有限守恒输出"); h.succeed(); });
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 70)
    public static void valveBrokenPipeAndPortReplacementStopThenRecover(GameTestHelper h) {
        route(h); BlockPos valve = STEAM.west(2);
        h.setBlock(valve, IndustrialFluidContent.CERAMIC_VALVE.get().defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        h.setBlock(valve.above(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(5, () -> seed(h, 0, 6000));
        h.runAfterDelay(8, () -> {
            h.assertTrue(amount(h, TANK) == 0 && owner(h).ledger().steam() == 6000, "主动汽源绕过关阀");
            h.setBlock(valve.above(), Blocks.AIR); h.setBlock(STEAM.west(), Blocks.AIR);
        });
        h.runAfterDelay(11, () -> {
            h.assertTrue(amount(h, TANK) == 0 && owner(h).ledger().steam() == 6000, "主动汽源跨断管续取");
            h.setBlock(STEAM, BoilerContent.CASING.get()); h.setBlock(STEAM.west(), IndustrialFluidContent.CERAMIC_PIPE.get());
        });
        h.runAfterDelay(14, () -> {
            h.assertTrue(amount(h, TANK) == 0 && owner(h).ledger().steam() == 6000, "主动汽源沿失效端口实体继续取汽");
            h.setBlock(STEAM, BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.WEST));
        });
        h.runAfterDelay(20, () -> {
            h.assertTrue(amount(h, TANK) == 6000 && owner(h).ledger().steam() == 0, "重建汽口/恢复管阀后有限输出没有恢复");
            System.out.println("[boiler-active] valve/broken pipe/port replacement stop preserves6000; valid rebuilt port transfers6000"); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 60)
    public static void mixedSourceKindsAndOldTargetFluidBlockWithoutDowngrade(GameTestHelper h) {
        fourPorts(h); BlockPos target = new BlockPos(1, 6, 6);
        h.setBlock(new BlockPos(2, 6, 6), IndustrialFluidContent.CERAMIC_PIPE.get()); h.setBlock(target, AllBlocks.FLUID_TANK.get());
        h.runAfterDelay(5, () -> { BoilerSteamSelectionGameTests.submit(h, FOUR[0], 0); seed(h, 6000, 6000); });
        h.runAfterDelay(8, () -> {
            h.assertTrue(amount(h, target) == 0 && owner(h).ledger().steam() == 12000, "两种真实库存共管没有拒混");
            for (BlockPos p : FOUR) BoilerSteamSelectionGameTests.submit(h, p, 0);
        });
        h.runAfterDelay(11, () -> {
            h.assertTrue(amount(h, target) == 6000 && owner(h).ledger().steam(BoilerSteamInventoryKind.SUPERCRITICAL) == 6000, "纯普通过滤没有只取普通池");
            for (BlockPos p : FOUR) BoilerSteamSelectionGameTests.submit(h, p, 1);
        });
        h.runAfterDelay(14, () -> {
            h.assertTrue(amount(h, target) == 6000 && owner(h).ledger().steam(BoilerSteamInventoryKind.SUPERCRITICAL) == 6000, "目标旧普通汽没有阻止SC共管");
            h.assertTrue(tank(h, target).getTankInventory().drain(6000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 6000, "测试没有有限移出旧普通汽");
        });
        h.runAfterDelay(18, () -> {
            h.assertTrue(amount(h, target) == 6000 && owner(h).ledger().steam() == 0 && tank(h, target).getTankInventory().getFluid().is(BoilerContent.SUPERCRITICAL_STEAM.get()),
                    "清空目标后SC没有保持身份恢复"); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 50)
    public static void supercriticalNativePipeStaysForbiddenWhileOrdinaryNativeRouteWorks(GameTestHelper h) {
        route(h);
        h.setBlock(STEAM.west(), AllBlocks.FLUID_PIPE.get()); h.setBlock(STEAM.west(2), AllBlocks.FLUID_PIPE.get());
        h.runAfterDelay(5, () -> seed(h, 0, 6000));
        h.runAfterDelay(8, () -> {
            h.assertTrue(amount(h, TANK) == 0 && owner(h).ledger().steam() == 6000, "SC被普通Create管主动搬运");
            var handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(STEAM), Direction.WEST);
            h.assertTrue(handler.drain(1000, IFluidHandler.FluidAction.SIMULATE).isEmpty() && handler.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "标准SC能力禁运被主动桥放开");
            BoilerSteamSelectionGameTests.submit(h, STEAM, 0); seed(h, 6000, 6000);
        });
        h.runAfterDelay(18, () -> {
            h.assertTrue(amount(h, TANK) == 6000 && owner(h).ledger().steam(BoilerSteamInventoryKind.NORMAL) == 0
                    && owner(h).ledger().steam(BoilerSteamInventoryKind.SUPERCRITICAL) == 6000, "普通汽原Create路线失效或消费了SC池");
            System.out.println("[boiler-active] SC native pipe/capability rejected; NORMAL native route delivers6000; SC6000 preserved"); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 45)
    public static void sameTickPortEntityReplacementRejectsAlreadyOfferedSource(GameTestHelper h) {
        route(h);
        postCheck(h, 6, () -> {
            seed(h, 0, 6000);
            var old = h.getBlockEntity(STEAM);
            h.getLevel().setBlockEntity(new BoilerPortBlockEntity(h.absolutePos(STEAM), h.getBlockState(STEAM)));
            h.assertTrue(old != h.getBlockEntity(STEAM) && h.getBlockState(STEAM).is(BoilerContent.STEAM_PORT.get()), "实体换代夹具未保持同方块并真实换BE");
        }, () -> {
            h.assertTrue(amount(h, TANK) == 0 && owner(h).ledger().steam() == 6000, "同tick旧汽口请求沿同状态替换BE继续取液");
            System.out.println("[boiler-active] same-state port BE replacement at HIGH rejects already-offered source at sameTickPost; source6000 target0");
        });
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 60, batch = "boiler_active_fairness")
    public static void twoActivePortsAndOnePumpAllReceiveRealSharedBottleneckTurns(GameTestHelper h) {
        fourPorts(h);
        h.setBlock(FOUR[2], BoilerContent.CASING.get()); h.setBlock(FOUR[3], BoilerContent.CASING.get());
        BlockPos pumpPos = new BlockPos(2, 6, 3), pumpTank = new BlockPos(2, 6, 1), target = new BlockPos(0, 6, 6), valve = new BlockPos(1, 6, 6);
        for (int z : new int[]{2, 4, 5, 6}) h.setBlock(new BlockPos(2, 6, z), IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(pumpPos, IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.SOUTH));
        h.setBlock(valve, IndustrialFluidContent.REINFORCED_VALVE.get().defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        for (int y = 0; y < 6; y++) h.setBlock(pumpTank.above(y), AllBlocks.FLUID_TANK.get());
        for (int y = 0; y < 8; y++) h.setBlock(target.above(y), AllBlocks.FLUID_TANK.get());
        h.setBlock(pumpPos.east(), AllBlocks.COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        h.setBlock(pumpPos.east().north(), AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        h.setBlock(pumpPos.east().north(2), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        ((CreativeMotorBlockEntity)h.getBlockEntity(pumpPos.east().north(2))).generatedSpeed.setValue(-256);
        boolean[] armed = {false}; int[] frames = {0}, totals = {0, 0, 0}; RuntimeException[] error = {null};
        java.util.function.Consumer<LevelTickEvent.Post> high = event -> {
            if (event.getLevel() != h.getLevel() || !armed[0] || error[0] != null) return;
            try {
                // 单独批次只剔除已完成旧夹具登记；A/B请求仍由合法Controller tick自然创建，不伪造身份/库存许可。
                var field = com.iksxh.create_nuclear_industry.industrial.IndustrialNetworkScheduler.class.getDeclaredField("WORLDS"); field.setAccessible(true);
                Object world = ((java.util.Map<?, ?>)field.get(null)).get(h.getLevel());
                var steamField = world.getClass().getDeclaredField("steam"); steamField.setAccessible(true);
                var pumpField = world.getClass().getDeclaredField("pumps"); pumpField.setAccessible(true);
                java.util.Map<?, ?> steam = (java.util.Map<?, ?>)steamField.get(world), pumps = (java.util.Map<?, ?>)pumpField.get(world);
                steam.keySet().removeIf(p -> !p.equals(h.absolutePos(FOUR[0])) && !p.equals(h.absolutePos(FOUR[1])));
                pumps.keySet().removeIf(p -> !p.equals(h.absolutePos(pumpPos)));
                h.assertTrue(steam.size() == 2 && pumps.size() == 1, "公平夹具实际参与者不是两个自然汽口和一泵");
                var pump = (IndustrialPumpBlockEntity)h.getBlockEntity(pumpPos);
                h.assertTrue(pump.flowLimit() > 0 && pump.outputDirection() == Direction.SOUTH, "公平夹具泵方向或动力非法");
                h.assertTrue(owner(h).ledger().removableSteam(BoilerSteamInventoryKind.SUPERCRITICAL) >= 4096
                        && amount(h, pumpTank) >= 4096 && tank(h, target).getTankInventory().getSpace() >= 4096, "公平窗口源/目标不足导致假公平");
            } catch (ReflectiveOperationException e) { error[0] = new IllegalStateException("隔离测试登记无法只读定位", e); }
            catch (RuntimeException e) { error[0] = e; }
        };
        java.util.function.Consumer<LevelTickEvent.Post> low = event -> {
            if (event.getLevel() != h.getLevel() || !armed[0] || error[0] != null) return;
            try {
                var ledger = owner(h).ledger(); var saved = ledger.save(); long now = h.getLevel().getGameTime();
                int[] received = {0, 0, ((IndustrialPumpBlockEntity)h.getBlockEntity(pumpPos)).actualFlow()};
                if (saved.getLong("FlowTick") == now) for (var budget : saved.getList("Budgets", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                    var entry = (net.minecraft.nbt.CompoundTag)budget;
                    for (int i = 0; i < 2; i++) if (entry.getLong("Port") == h.absolutePos(FOUR[i]).asLong()) received[i] += entry.getInt("Used");
                }
                int sum = received[0] + received[1] + received[2];
                h.assertTrue(sum == 4096, "公平帧真实实收总额未保持4096瓶颈");
                int cached = ((IndustrialPumpBlockEntity)h.getBlockEntity(pumpPos)).cachedFluid().getAmount();
                h.assertTrue(ledger.steam() + amount(h, pumpTank) + amount(h, target) + cached == 100000, "公平帧有限源/目标/缓存不守恒");
                for (int i = 0; i < 3; i++) totals[i] += received[i];
                frames[0]++;
                System.out.println("[boiler-active-fairness] frame=" + frames[0] + "; tick=" + now + "; A=" + received[0] + "; B=" + received[1]
                        + "; pump=" + received[2] + "; total=" + sum + "; inventoryTotal=100000");
                if (frames[0] == 12) armed[0] = false;
            } catch (RuntimeException e) { error[0] = e; }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, high); NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, low);
        h.runAfterDelay(8, () -> {
            h.assertTrue(owner(h).currentForm() != null && owner(h).currentForm().steamCells() == 50, "公平锅炉不是合法100000mB汽容量");
            seed(h, 0, 60000);
            h.assertTrue(owner(h).ledger().pressure() < owner(h).ledger().settings().valveOpenPressure(), "公平种子超过安全阀开启线");
            h.assertTrue(tank(h, pumpTank).getTankInventory().fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 40000), IFluidHandler.FluidAction.EXECUTE) == 40000,
                    "公平泵有限库存容量不足");
            try {
                var control = com.iksxh.create_nuclear_industry.industrial.IndustrialValveBlockEntity.class.getDeclaredField("control"); control.setAccessible(true);
                ((com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour)control.get(h.getBlockEntity(valve))).setValue(50);
            } catch (ReflectiveOperationException e) { throw new IllegalStateException("公平真实阀开度无法设置", e); }
            h.assertTrue(((com.iksxh.create_nuclear_industry.industrial.IndustrialValveBlockEntity)h.getBlockEntity(valve)).effectiveLimit() == 4096, "公平真实强化阀不是4096瓶颈");
            armed[0] = true;
        });
        h.runAfterDelay(24, () -> {
            NeoForge.EVENT_BUS.unregister(high); NeoForge.EVENT_BUS.unregister(low);
            if (error[0] != null) throw error[0]; h.assertTrue(frames[0] == 12, "公平没有完整十二帧真实观测");
            System.out.println("[boiler-active-fairness] twelveFrameTotals A=" + totals[0] + "; B=" + totals[1] + "; pump=" + totals[2]);
            h.assertTrue(totals[0] > 0 && totals[1] > 0 && totals[2] > 0, "组优先奇偶与组内首位耦合，真实汽口或泵连续十二帧永久零交付"); h.succeed();
        });
    }
    @GameTest(template = "industrial_empty", templateNamespace = "create_nuclear_industry_industrial", timeoutTicks = 60, batch = "boiler_active_target_rotation")
    public static void activeSourceDeliversToBothTargetsDuringItsRealPriorityOpportunities(GameTestHelper h) {
        fourPorts(h); for (int i = 1; i < FOUR.length; i++) h.setBlock(FOUR[i], BoilerContent.CASING.get());
        BlockPos pumpPos = new BlockPos(2, 6, 3), pumpTank = new BlockPos(2, 6, 1), north = new BlockPos(0, 6, 5), west = new BlockPos(-1, 6, 6), valve = new BlockPos(1, 6, 6);
        for (int z : new int[]{2, 4, 5, 6}) h.setBlock(new BlockPos(2, 6, z), IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(new BlockPos(0, 6, 6), IndustrialFluidContent.CERAMIC_PIPE.get());
        h.setBlock(pumpPos, IndustrialFluidContent.PUMP.get().defaultBlockState().setValue(IndustrialPumpBlock.FACING, Direction.SOUTH));
        h.setBlock(valve, IndustrialFluidContent.REINFORCED_VALVE.get().defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        for (int y = 0; y < 6; y++) h.setBlock(pumpTank.above(y), AllBlocks.FLUID_TANK.get());
        for (int y = 0; y < 8; y++) { h.setBlock(north.above(y), AllBlocks.FLUID_TANK.get()); h.setBlock(west.above(y), AllBlocks.FLUID_TANK.get()); }
        h.setBlock(pumpPos.east(), AllBlocks.COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        h.setBlock(pumpPos.east().north(), AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        h.setBlock(pumpPos.east().north(2), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        ((CreativeMotorBlockEntity)h.getBlockEntity(pumpPos.east().north(2))).generatedSpeed.setValue(-256);
        boolean[] armed = {false}; int[] frames = {0}, sourceFrames = {0}, sourceTotals = {0, 0}, before = {0, 0}; RuntimeException[] error = {null};
        java.util.function.Consumer<LevelTickEvent.Post> high = event -> {
            if (event.getLevel() != h.getLevel() || !armed[0] || error[0] != null) return;
            try {
                // 与来源公平例相同：单独批次只隔离已完成夹具登记，源身份由本控制器自然发布。
                var field = com.iksxh.create_nuclear_industry.industrial.IndustrialNetworkScheduler.class.getDeclaredField("WORLDS"); field.setAccessible(true);
                Object world = ((java.util.Map<?, ?>)field.get(null)).get(h.getLevel());
                var steamField = world.getClass().getDeclaredField("steam"); steamField.setAccessible(true);
                var pumpField = world.getClass().getDeclaredField("pumps"); pumpField.setAccessible(true);
                java.util.Map<?, ?> steam = (java.util.Map<?, ?>)steamField.get(world), pumps = (java.util.Map<?, ?>)pumpField.get(world);
                steam.keySet().removeIf(p -> !p.equals(h.absolutePos(FOUR[0]))); pumps.keySet().removeIf(p -> !p.equals(h.absolutePos(pumpPos)));
                h.assertTrue(steam.size() == 1 && pumps.size() == 1, "目标轮转不是一个自然汽口与一台真实泵");
                h.assertTrue(owner(h).ledger().removableSteam(BoilerSteamInventoryKind.SUPERCRITICAL) >= 4096 && amount(h, pumpTank) >= 4096
                        && tank(h, north).getTankInventory().getSpace() >= 4096 && tank(h, west).getTankInventory().getSpace() >= 4096,
                        "目标轮转窗口源或任一接收罐不足，无法证明持续公平");
                before[0] = amount(h, north); before[1] = amount(h, west);
            } catch (ReflectiveOperationException e) { error[0] = new IllegalStateException("目标轮转测试无法隔离登记", e); }
            catch (RuntimeException e) { error[0] = e; }
        };
        java.util.function.Consumer<LevelTickEvent.Post> low = event -> {
            if (event.getLevel() != h.getLevel() || !armed[0] || error[0] != null) return;
            try {
                long now = h.getLevel().getGameTime(); var saved = owner(h).ledger().save(); int used = 0;
                if (saved.getLong("FlowTick") == now) for (var item : saved.getList("Budgets", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                    var budget = (net.minecraft.nbt.CompoundTag)item;
                    if (budget.getLong("Port") == h.absolutePos(FOUR[0]).asLong()) used += budget.getInt("Used");
                }
                var pump = (IndustrialPumpBlockEntity)h.getBlockEntity(pumpPos); int pumpFlow = pump.actualFlow();
                int n = amount(h, north) - before[0], w = amount(h, west) - before[1];
                h.assertTrue(n + w == 4096 && used + pumpFlow == 4096, "目标轮转每帧不符合真实4096瓶颈");
                if (used > 0) {
                    h.assertTrue(pumpFlow == 0 && n + w == used, "把泵交付误计为主动源目标公平");
                    sourceFrames[0]++; sourceTotals[0] += n; sourceTotals[1] += w;
                }
                h.assertTrue(owner(h).ledger().steam() + amount(h, pumpTank) + amount(h, north) + amount(h, west) + pump.cachedFluid().getAmount() == 100000,
                        "目标轮转有限源/双目标/缓存不守恒");
                frames[0]++; System.out.println("[boiler-target-rotation] frame=" + frames[0] + "; tick=" + now + "; portUsed=" + used
                        + "; pump=" + pumpFlow + "; northDelta=" + n + "; westDelta=" + w + "; inventoryTotal=100000");
                if (frames[0] == 12) armed[0] = false;
            } catch (RuntimeException e) { error[0] = e; }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, high); NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, low);
        h.runAfterDelay(8, () -> {
            h.assertTrue(owner(h).currentForm() != null && owner(h).currentForm().steamPorts().size() == 1 && owner(h).currentForm().steamCells() == 50, "目标轮转锅炉几何非法");
            seed(h, 0, 60000); h.assertTrue(owner(h).ledger().pressure() < owner(h).ledger().settings().valveOpenPressure(), "目标轮转库存触发安全阀");
            h.assertTrue(tank(h, pumpTank).getTankInventory().fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 40000), IFluidHandler.FluidAction.EXECUTE) == 40000, "目标轮转泵源不是有限40000");
            try {
                var control = com.iksxh.create_nuclear_industry.industrial.IndustrialValveBlockEntity.class.getDeclaredField("control"); control.setAccessible(true);
                ((com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour)control.get(h.getBlockEntity(valve))).setValue(50);
            } catch (ReflectiveOperationException e) { throw new IllegalStateException("目标轮转无法设置真实瓶颈阀", e); }
            var pump = (IndustrialPumpBlockEntity)h.getBlockEntity(pumpPos);
            h.assertTrue(pump.flowLimit() > 0 && pump.outputDirection() == Direction.SOUTH, "目标轮转泵没有合法南向动力"); armed[0] = true;
        });
        h.runAfterDelay(24, () -> {
            NeoForge.EVENT_BUS.unregister(high); NeoForge.EVENT_BUS.unregister(low); if (error[0] != null) throw error[0];
            System.out.println("[boiler-target-rotation] sourceOnlyFrames=" + sourceFrames[0] + "; northSource=" + sourceTotals[0] + "; westSource=" + sourceTotals[1]);
            h.assertTrue(frames[0] == 12 && sourceFrames[0] > 0 && sourceTotals[0] > 0 && sourceTotals[1] > 0,
                    "本主动源实际交付机会仍与全局奇偶耦合，两个持续可收目标之一始终收不到此源"); h.succeed();
        });
    }
}
