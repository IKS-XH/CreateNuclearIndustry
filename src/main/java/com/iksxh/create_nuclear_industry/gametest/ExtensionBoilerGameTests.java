package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.boiler.*;
import com.iksxh.create_nuclear_industry.content.*;
import com.iksxh.create_nuclear_industry.heat.*;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 本批隔离域：真实结构、能力、管网和独立设备冒烟；不使用旧固定暖炉夹具。 */
@GameTestHolder("create_nuclear_industry_boiler_rework")
@PrefixGameTestTemplate(false)
public final class ExtensionBoilerGameTests {
    private static final BlockPos BASE = new BlockPos(4, 2, 4);
    private static final BlockPos CONTROL = BASE.offset(1, 1, 0), WATER = BASE.offset(2, 1, 0), HOT = BASE.offset(3, 1, 0);
    private ExtensionBoilerGameTests() {}
    /** 最小夹具底面与隔层全部配置有效机/段；可以选择偏心隔层和偶数长方体。 */
    static void build(GameTestHelper h, int width, int height, int depth, int partition) {
        for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) for (int z = 0; z < depth; z++) {
            boolean interiorXZ = x > 0 && x < width - 1 && z > 0 && z < depth - 1;
            BlockState s = BoilerContent.CASING.get().defaultBlockState();
            if (interiorXZ && y > 0 && y < height - 1) s = y == partition ? BoilerContent.HEAT_SECTION.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
            if (interiorXZ && y == 0) s = HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState();
            h.setBlock(BASE.offset(x, y, z), s);
        }
        h.setBlock(CONTROL, BoilerContent.CONTROLLER.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.NORTH));
        h.setBlock(WATER, BoilerContent.WATER_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.NORTH));
        h.setBlock(HOT, BoilerContent.HOT_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.NORTH));
        h.setBlock(BASE.offset(3, partition, 0), BoilerContent.COLD_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.NORTH));
        h.setBlock(BASE.offset(0, partition + 1, 1), BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.WEST));
        h.setBlock(BASE.offset(1, height - 1, 1), BoilerContent.SAFETY_VALVE.get());
        h.setBlock(BASE.offset(width - 1, 1, 2), BoilerContent.WINDOW.get());
    }
    static BoilerControllerBlockEntity owner(GameTestHelper h) { return (BoilerControllerBlockEntity) h.getBlockEntity(CONTROL); }
    private static IFluidHandler handler(GameTestHelper h, BlockPos p, Direction side) { return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(p), side); }
    static IFluidHandler water(GameTestHelper h) { return handler(h, WATER, Direction.NORTH); }
    static IFluidHandler steam(GameTestHelper h) { return handler(h, BASE.offset(0, 3, 1), Direction.WEST); }
    private static IFluidHandler hot(GameTestHelper h) { return handler(h, HOT, Direction.NORTH); }
    private static IFluidHandler cold(GameTestHelper h) { return handler(h, BASE.offset(3, 2, 0), Direction.NORTH); }
    private static void seedSteam(GameTestHelper h, int amount, double energy) {
        CompoundTag tag = owner(h).ledger().save(); tag.putInt("Steam", amount); tag.putDouble("SteamHu", energy); owner(h).ledger().load(tag);
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 40)
    public static void referenceGeometryAndFourCapabilities(GameTestHelper h) {
        build(h, 5, 5, 5, 2);
        h.runAfterDelay(4, () -> {
            var f = owner(h).currentForm(); h.assertTrue(f != null && f.waterCells() == 9 && f.steamCells() == 9, "5³ 水汽各九格未识别");
            h.assertTrue(water(h).getTankCapacity(0) == 18000 && steam(h).getTankCapacity(0) == 18000, "参考容量应各18000mB");
            h.assertTrue(hot(h).getTankCapacity(0) == 36000 && cold(h).getTankCapacity(0) == 36000, "9机整炉冷热容量未共享");
            h.assertTrue(handler(h, WATER, Direction.SOUTH) == null, "端口内侧错误开放");
            h.succeed();
        });
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 60)
    public static void elevenBoundaryAndOffCenterPartition(GameTestHelper h) {
        build(h, 11, 11, 11, 3);
        h.runAfterDelay(4, () -> {
            var f = owner(h).currentForm(); h.assertTrue(f != null && f.width() == 11 && f.height() == 11 && f.depth() == 11, "11边界未成型");
            h.assertTrue(f.waterCells() == 162 && f.steamCells() == 486 && f.partitionY() == h.absolutePos(BASE.above(3)).getY(), "偏心分区格数错误");
            build(h, 12, 11, 11, 3); owner(h).invalidateForm();
            h.assertTrue(owner(h).currentForm() == null, "默认范围错误接受12格外边长"); h.succeed();
        });
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 50)
    public static void evenRectangularAndInvalidPartitionOrEdge(GameTestHelper h) {
        build(h, 6, 6, 8, 2);
        h.runAfterDelay(4, () -> {
            var f = owner(h).currentForm(); h.assertTrue(f != null && f.waterCells() == 24 && f.steamCells() == 48, "偶数长方体错误");
            h.setBlock(BASE.offset(2, 2, 2), Blocks.AIR); owner(h).invalidateForm(); h.assertTrue(owner(h).currentForm() == null, "隔层破洞仍成型");
            h.setBlock(BASE.offset(2, 2, 2), BoilerContent.CASING.get()); owner(h).invalidateForm(); h.assertTrue(owner(h).currentForm() != null, "普通外壳不能补齐隔层");
            h.setBlock(BASE, BoilerContent.WATER_PORT.get()); owner(h).invalidateForm(); h.assertTrue(owner(h).currentForm() == null, "棱角端口被接受"); h.succeed();
        });
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 50)
    public static void sharedPortsSimulationPressureAndStaleHandles(GameTestHelper h) {
        build(h, 5, 5, 5, 2);
        BlockPos second = BASE.offset(0, 3, 2);
        h.setBlock(second, BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.WEST));
        BlockPos secondWater = BASE.offset(4, 1, 1), secondHot = BASE.offset(1, 1, 4), secondCold = BASE.offset(1, 2, 4);
        h.setBlock(secondWater, BoilerContent.WATER_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.EAST));
        h.setBlock(secondHot, BoilerContent.HOT_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.SOUTH));
        h.setBlock(secondCold, BoilerContent.COLD_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.SOUTH));
        h.runAfterDelay(4, () -> {
            var ledger = owner(h).ledger(); var untouched = ledger.save();
            var waterB = handler(h, secondWater, Direction.EAST); var hotB = handler(h, secondHot, Direction.SOUTH); var coldB = handler(h, secondCold, Direction.SOUTH);
            var waterStack = new FluidStack(Fluids.WATER, 256); var hotStack = new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 256);
            h.assertTrue(water(h).fill(waterStack, IFluidHandler.FluidAction.SIMULATE) == 256 && waterB.fill(waterStack, IFluidHandler.FluidAction.SIMULATE) == 256
                    && hot(h).fill(hotStack, IFluidHandler.FluidAction.SIMULATE) == 256 && hotB.fill(hotStack, IFluidHandler.FluidAction.SIMULATE) == 256, "多个水/热口模拟额度错误");
            h.assertTrue(untouched.equals(ledger.save()), "多入口模拟写入账本");
            water(h).fill(waterStack, IFluidHandler.FluidAction.EXECUTE); waterB.fill(waterStack, IFluidHandler.FluidAction.EXECUTE);
            hot(h).fill(hotStack, IFluidHandler.FluidAction.EXECUTE); hotB.fill(hotStack, IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(ledger.water() == 512 && ledger.hot() == 512, "不同物理入口未共享库存/独立限流");
            h.assertTrue(hotB.fill(hotStack, IFluidHandler.FluidAction.EXECUTE) == 0, "同tick重复热入口绕过额度");
            // 由实际热转冷付款生成冷库存，再验证两个冷口不能重复领取同一产物。
            double paid = ledger.collectHeat(h.getLevel().getGameTime(), 162, .5, 162);
            ledger.tick(h.getLevel().getGameTime() + 1, 9, paid, true, false);
            int coldBefore = ledger.cold();
            h.assertTrue(cold(h).drain(256, IFluidHandler.FluidAction.SIMULATE).getAmount() == 256 && coldB.drain(256, IFluidHandler.FluidAction.SIMULATE).getAmount() == 256, "多个冷口模拟错误");
            int removedCold = cold(h).drain(256, IFluidHandler.FluidAction.EXECUTE).getAmount() + coldB.drain(256, IFluidHandler.FluidAction.EXECUTE).getAmount();
            h.assertTrue(removedCold == coldBefore && ledger.cold() == 0, "多冷口复制产物");
            var a = steam(h); var b = handler(h, second, Direction.WEST); seedSteam(h, 11100, 11100);
            var before = owner(h).ledger().save();
            h.assertTrue(a.drain(256, IFluidHandler.FluidAction.SIMULATE).getAmount() == 256 && b.drain(256, IFluidHandler.FluidAction.SIMULATE).getAmount() == 256, "模拟保压余量错误");
            h.assertTrue(before.equals(owner(h).ledger().save()), "模拟改变账本");
            h.assertTrue(a.drain(256, IFluidHandler.FluidAction.EXECUTE).getAmount() == 256 && b.drain(256, IFluidHandler.FluidAction.EXECUTE).getAmount() == 44, "多口复制保压余量");
            h.assertTrue(Math.abs(owner(h).ledger().steamHu() - 10800) < 1e-6, "抽汽未带走实际HU");
            var oldHot = hot(h); h.setBlock(BASE, Blocks.AIR); owner(h).invalidateForm();
            h.assertTrue(oldHot.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 256), IFluidHandler.FluidAction.EXECUTE) == 0, "拆炉旧热口仍写入");
            h.setBlock(BASE, BoilerContent.CASING.get()); owner(h).invalidateForm(); h.assertTrue(owner(h).currentForm() != null, "修复后不能成型");
            h.assertTrue(oldHot.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 256), IFluidHandler.FluidAction.EXECUTE) == 0, "旧代次句柄复活"); h.succeed();
        });
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 50)
    public static void membersTransferOnceAndCannotJoinExternalLine(GameTestHelper h) {
        BlockPos member = BASE.offset(1, 0, 1);
        h.setBlock(member, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
        h.runAfterDelay(2, () -> {
            var machine = (NuclearHeatExchangerBlockEntity) h.getBlockEntity(member);
            var old = machine.fluidPort(Direction.SOUTH);
            h.assertTrue(old != null, "独立机初始入口不可用"); old.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 300), IFluidHandler.FluidAction.EXECUTE);
            machine.ledger().claimDedicated(h.getLevel().getGameTime() + 1, 18, com.iksxh.create_nuclear_industry.config.HeatExchangerConfig.settings());
            build(h, 5, 5, 5, 2); owner(h).invalidateForm();
            h.runAfterDelay(3, () -> {
                var m = (NuclearHeatExchangerBlockEntity) h.getBlockEntity(member);
                h.assertTrue(owner(h).ledger().hot() == 264 && owner(h).ledger().cold() == 36 && m.ledger().hot() == 0
                        && Math.abs(owner(h).ledger().coolantHu() - 18) < 1e-8 && m.ledger().reserve() == 0, "成型未守恒迁入当前冷热液和已付热");
                h.assertTrue(m.fluidPort(Direction.SOUTH) == null && m.publishedHeat() < 0, "成员仍独立供热/暴露端口");
                h.assertTrue(old.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 1), IFluidHandler.FluidAction.EXECUTE) == 0, "成型后独立旧句柄仍有效");
                owner(h).tick(); h.assertTrue(owner(h).ledger().hot() == 264 && Math.abs(owner(h).ledger().coolantHu() - 18) < 1e-8, "重复接管复制或消耗无负载冷却剂");
                h.succeed();
            });
        });
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 700)
    public static void realContinuousCoolantWaterAndSteamLoop(GameTestHelper h) {
        build(h, 5, 5, 5, 2);
        BlockPos tank = BASE.offset(-1, 3, 1); h.setBlock(tank, AllBlocks.FLUID_TANK.get());
        long[] output = {0}; double[] heat = {0}; int[] converted = {0};
        h.onEachTick(() -> {
            if (water(h) == null || hot(h) == null || cold(h) == null) return;
            water(h).fill(new FluidStack(Fluids.WATER, 256), IFluidHandler.FluidAction.EXECUTE);
            hot(h).fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 256), IFluidHandler.FluidAction.EXECUTE);
            converted[0] += cold(h).drain(256, IFluidHandler.FluidAction.EXECUTE).getAmount();
            var t = handler(h, tank, Direction.EAST); if (t != null) output[0] += t.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount();
        });
        h.runAfterDelay(600, () -> {
            var s = owner(h).ledger(); h.assertTrue(output[0] > 10000, "持续补水/热液且已接输出仍无法充压并连续产汽");
            h.assertTrue(s.pressure() >= .59 && s.steamTemperature() >= 1.99, "连续工况失去超临界温压");
            h.assertTrue(s.totalHu() + output[0] <= (converted[0] + s.cold()) * .5 + 1e-5, "输出与炉内HU超过实际转冷付款");
            h.succeed();
        });
    }
    private static void nativePipe(GameTestHelper h, boolean critical) {
        build(h, 5, 5, 5, 2); BlockPos pipe = BASE.offset(-1, 3, 1), tank = BASE.offset(-2, 3, 1);
        h.setBlock(pipe, AllBlocks.FLUID_PIPE.get()); h.setBlock(tank, AllBlocks.FLUID_TANK.get());
        h.runAfterDelay(4, () -> {
            owner(h).selectMode(critical);
            if (critical) {
                seedSteam(h, 14000, 11200);
                h.assertTrue(steam(h).drain(256, IFluidHandler.FluidAction.SIMULATE).isEmpty(), "低温汽错误进入超临界管线");
            }
            // 留下明确已付的100HU过热显热，覆盖等待管网建立期间的自然散热。
            seedSteam(h, 14000, 14100);
        });
        h.runAfterDelay(35, () -> {
            var t = handler(h, tank, Direction.EAST); var fluid = t == null ? FluidStack.EMPTY : t.getFluidInTank(0);
            h.assertTrue(!fluid.isEmpty() && fluid.is(critical ? BoilerContent.SUPERCRITICAL_STEAM.get() : TurbineContent.STEAM.get()), "实际Create管道未输出所选汽种");
            var old = steam(h); double before = owner(h).ledger().steamHu(); owner(h).selectMode(!critical);
            h.assertTrue(old.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "切模式旧句柄仍放汽");
            h.assertTrue(owner(h).ledger().steamHu() == before && t.getFluidInTank(0).is(fluid.getFluid()), "切模式重写锅内焓或删除管外流体");
            h.succeed();
        });
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 60)
    public static void actualCreatePipeSupercriticalMode(GameTestHelper h) { nativePipe(h, true); }
    @GameTest(template = "boiler_empty", timeoutTicks = 60)
    public static void actualCreatePipeNormalMode(GameTestHelper h) { nativePipe(h, false); }
    @GameTest(template = "boiler_empty", timeoutTicks = 80)
    public static void blockedValveRedstoneAndCurrentSave(GameTestHelper h) {
        build(h, 5, 5, 5, 2); h.setBlock(BASE.offset(1, 5, 1), Blocks.STONE);
        h.runAfterDelay(4, () -> {
            // 18000mB、Ts1.8对应17280HU及P0.9，不能用库存占比替代真实炉压。
            seedSteam(h, 18000, 17280); var s = owner(h).ledger();
            h.assertTrue(Math.abs(s.pressure() - .9) < 1e-8, "堵阀夹具未到真实开启压力");
            h.assertTrue(s.demand(false) < .000001, "堵阀保护线仍申请升压热");
            var portable = owner(h).savePortableData(); owner(h).loadPortableData(portable);
            h.assertTrue(portable.equals(owner(h).savePortableData()), "当前保存加载改写库存/HU/模式");
            h.setBlock(CONTROL.north(), Blocks.REDSTONE_BLOCK);
            h.assertTrue(hot(h).fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 256), IFluidHandler.FluidAction.EXECUTE) == 256, "红石状态不能储存待用热液");
            int before = s.hot();
            h.runAfterDelay(5, () -> { h.assertTrue(owner(h).ledger().hot() == before, "红石停止仍转换新热液"); h.succeed(); });
        });
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 100, batch = "config")
    public static void nonDefaultConfig(GameTestHelper h) { ExtensionConfigGameTests.nonDefaultServerSettingsReachPhysicalMachine(h); }
    @GameTest(template = "boiler_empty", timeoutTicks = 320)
    public static void independentCreateBoilerSmoke(GameTestHelper h) { ExtensionHeatExchangerGameTests.smallNativeBoilerUsesRatedFlowAndQueriesArePure(h); }
    @GameTest(template = "boiler_empty", timeoutTicks = 100)
    public static void independentCondensationSmoke(GameTestHelper h) { ExtensionCondensationGameTests.ordinarySteamCondenses(h); }
    @GameTest(template = "boiler_empty", timeoutTicks = 400)
    public static void supercriticalTurbineSmoke(GameTestHelper h) { ExtensionTurbineGameTests.sharedCapacityDrivesEitherEndAtRealExhaustFlow(h); }
}
