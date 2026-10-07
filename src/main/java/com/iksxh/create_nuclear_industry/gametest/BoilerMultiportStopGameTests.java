package com.iksxh.create_nuclear_industry.gametest;

import static com.iksxh.create_nuclear_industry.boiler.BoilerSteamInventoryKind.*;

import com.iksxh.create_nuclear_industry.boiler.BoilerPartBlock;
import com.iksxh.create_nuclear_industry.config.TurbineConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.turbine.TurbineState;
import com.iksxh.create_nuclear_industry.turbine.TurbineOutputShaftBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 01E真实汇流及动力停转域：只通过能力供料，反射仅只读原生缓存，不写坏状态制造失败。 */
@GameTestHolder("create_nuclear_industry_boiler_multiport_stop")
@PrefixGameTestTemplate(false)
public final class BoilerMultiportStopGameTests {
    private static final BlockPos BASE = new BlockPos(4, 2, 4);
    private BoilerMultiportStopGameTests() {}

    @GameTest(template = "multiport_empty", timeoutTicks = 330)
    public static void fourPortsCommonNativePipeAfterMinimumChange(GameTestHelper h) { multiport(h, false); }

    @GameTest(template = "multiport_empty", timeoutTicks = 330)
    public static void fourPortsDirectTankReference(GameTestHelper h) { multiport(h, true); }

    @GameTest(template = "multiport_empty", timeoutTicks = 980)
    public static void fourCommonPortsFeedRealMediumAtContinuousSixteenPairHeat(GameTestHelper h) { sharedTurbine(h, TurbineConfig.settings().mediumTier(), false, false); }

    @GameTest(template = "multiport_empty", timeoutTicks = 980)
    public static void fourCommonPortsFeedRealLargeThroughNativePump(GameTestHelper h) { sharedTurbine(h, TurbineConfig.settings().longTier(), true, false); }

    @GameTest(template = "multiport_empty", timeoutTicks = 980)
    public static void latestFourSupercriticalCommonMediumAndCreativeSink(GameTestHelper h) { sharedTurbine(h, TurbineConfig.settings().mediumTier(), false, true); }

    @GameTest(template = "multiport_empty", timeoutTicks = 980)
    public static void latestFourSupercriticalCommonMediumAndCreativeSinkWithPump(GameTestHelper h) { sharedTurbine(h, TurbineConfig.settings().mediumTier(), true, true); }

    /** R1独立域仅运行现场三SC/独立NORMAL管路，保留旧域与旧证据。 */
    @GameTest(template = "inventory_empty", templateNamespace = "create_nuclear_industry_boiler_inventory", timeoutTicks = 980)
    public static void r1NormalThirteenPipesReceiveDuringQualifiedWindows(GameTestHelper h) { pressureR1(h, false); }

    /** 普通汽直邻罐对照使用同一热负荷与汽种选择，区别原生启动等待和过滤资格。 */
    @GameTest(template = "pressure_empty", templateNamespace = "create_nuclear_industry_boiler_pressure_r1", timeoutTicks = 980)
    public static void r1NormalDirectTankReference(GameTestHelper h) { pressureR1(h, true); }

    /**
     * 复刻R1受支持拓扑：16对、实际126mB/t热液即63HU/t，三SC共管接近邻创造罐和13管中型；
     * 第四NORMAL独立13管接普通空罐。仅只读原生缓存/模拟，真实成交仍由原生接收与锅炉交易执行。
     */
    private static void pressureR1(GameTestHelper h, boolean direct) {
        ExtensionBoilerGameTests.build(h, 6, 5, 6, 2);
        List<BlockPos> ports = new ArrayList<>(), scPipes = new ArrayList<>(), normalPipes = new ArrayList<>();
        for (int z = 1; z <= 4; z++) {
            BlockPos port = BASE.offset(0, 3, z); ports.add(port);
            h.setBlock(port, BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.WEST));
            if (z < 4) scPipes.add(port.west());
        }
        var tier = TurbineConfig.settings().mediumTier(); BlockPos front = new BlockPos(14, 5, 2);
        clearTurbineInterior(h, front, tier); ExtensionTurbineGameTests.build(h, front, tier.rotorCount(), false);
        scPipes.add(new BlockPos(3, 6, 5));
        for (int x = 3; x <= 11; x++) scPipes.add(new BlockPos(x, 7, 5));
        scPipes.add(new BlockPos(11, 6, 5)); scPipes.add(new BlockPos(11, 5, 5));
        for (BlockPos pipe : scPipes) h.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        BlockPos sinkPos = ports.getFirst().west(2), normal = ports.getLast();
        h.setBlock(sinkPos, AllBlocks.CREATIVE_FLUID_TANK.get());
        BlockPos normalTank = direct ? normal.west() : new BlockPos(3, 3, 1);
        if (!direct) {
            normalPipes.add(normal.west()); normalPipes.add(normal.west(2)); normalPipes.add(normal.west(3));
            normalPipes.add(new BlockPos(1, 4, 8)); normalPipes.add(new BlockPos(1, 3, 8));
            for (int z = 7; z >= 1; z--) normalPipes.add(new BlockPos(1, 3, z));
            normalPipes.add(new BlockPos(2, 3, 1));
            for (BlockPos pipe : normalPipes) h.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
            // 第一段玻璃管仅沿X轴连接，保持第四口与相邻SC主网物理隔离。
            h.setBlock(normal.west(), AllBlocks.GLASS_FLUID_PIPE.getDefaultState()
                    .setValue(com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock.AXIS, Direction.Axis.X));
        }
        for (int y = 0; y < 7; y++) h.setBlock(normalTank.above(y), AllBlocks.FLUID_TANK.get());
        // 原生包覆管固定E/N两面，避免直邻普通罐对照被相邻SC首管横向接入。
        h.setBlock(ports.get(2).west(), AllBlocks.ENCASED_FLUID_PIPE.getDefaultState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST, true)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH, true));
        R1CreativeTank sink = new R1CreativeTank(h.absolutePos(sinkPos), h.getBlockState(sinkPos));
        h.getLevel().removeBlockEntity(h.absolutePos(sinkPos)); h.getLevel().setBlockEntity(sink);
        R1NormalTank receiver = new R1NormalTank(h.absolutePos(normalTank), h.getBlockState(normalTank));
        h.getLevel().removeBlockEntity(h.absolutePos(normalTank)); h.getLevel().setBlockEntity(receiver);
        // 替换观察实体后通过原生入口重新成型，避免观察器默认单格容量造成对照满罐。
        receiver.removeController(true);
        for (BlockPos pipe : scPipes) FluidPropagator.propagateChangedPipe(h.getLevel(), h.absolutePos(pipe), h.getBlockState(pipe));
        for (BlockPos pipe : normalPipes) FluidPropagator.propagateChangedPipe(h.getLevel(), h.absolutePos(pipe), h.getBlockState(pipe));
        boolean[] active = {false}; long[] counts = new long[8]; int[] windows = new int[4]; int[] lowPressureScTicks = {0}, pressureHeadTicks = {0};
        IFluidHandler[] firstCaps = new IFluidHandler[2]; double[] initialHu = {0};
        long[] midpointNormal = {0};
        h.runAfterDelay(6, () -> {
            var owner = ExtensionBoilerGameTests.owner(h); h.assertTrue(owner.currentForm() != null, "R1锅炉未成型");
            owner.selectMinimum(60); BoilerSteamSelectionGameTests.submit(h, normal, 0);
            var tag = owner.ledger().save(); tag.putInt("Water", 32000); tag.putDouble("WaterHu", 57600);
            ExtensionBoilerGameTests.seedInventories(tag, 0, 0, 24000, 24000); owner.ledger().load(tag);
            initialHu[0] = owner.ledger().totalHu(); counts[6] = 24000;
            // 在首次账本tick前实际提供本tick热液，避免无收热初始化先消耗预热墙体余热。
            counts[1] += capability(h, BASE.offset(3, 0, 0), Direction.NORTH)
                    .fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 126), IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(receiver.getTankInventory().getCapacity() >= 56000, "R1观察普通罐未由原生正确成型为足够容量");
            firstCaps[0] = capability(h, ports.getFirst(), Direction.WEST); firstCaps[1] = capability(h, normal, Direction.WEST);
            active[0] = true;
            System.out.println("[pressure-r1] direct=" + direct + " scToMedium=13/14/15 normalPipes=" + normalPipes.size()
                    + " sinkDistance=1 Tsc=" + owner.ledger().settings().supercriticalTemperature() + " Psc=" + owner.ledger().settings().supercriticalPressure());
        });
        h.onEachTick(() -> {
            if (!active[0]) return;
            var owner = ExtensionBoilerGameTests.owner(h); var s = owner.ledger();
            counts[0] += ExtensionBoilerGameTests.water(h).fill(new FluidStack(Fluids.WATER, 256), IFluidHandler.FluidAction.EXECUTE);
            if (h.getTick() != 6) counts[1] += capability(h, BASE.offset(3, 0, 0), Direction.NORTH)
                    .fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 126), IFluidHandler.FluidAction.EXECUTE);
            counts[2] += capability(h, BASE.offset(3, 2, 0), Direction.NORTH).drain(256, IFluidHandler.FluidAction.EXECUTE).getAmount();
            var exhaust = ExtensionTurbineGameTests.handler(h, ExtensionTurbineGameTests.exhaust(front, tier), Direction.EAST);
            if (exhaust != null) counts[3] += exhaust.drain(256, IFluidHandler.FluidAction.EXECUTE).getAmount();
            counts[4] += s.produced(); var turbine = ExtensionTurbineGameTests.owner(h, front).ledger();
            int stored = receiver.getTankInventory().getFluidAmount();
            h.assertTrue(s.steam() + s.totalVented() + counts[3] + turbine.exhaust() + sink.executed + stored == 24000 + counts[4],
                    "R1两种汽真实成交mB不守恒");
            h.assertTrue(s.pressure() + 1e-8 >= s.minimumPressure(), "R1真实抽取越过共同出汽下限");
            double carried = counts[3] + turbine.exhaust() + sink.executed + stored + s.totalVented();
            double difference = initialHu[0] + .5 * (counts[2] + s.cold()) - s.totalHu() - carried;
            double coolingBound = Math.max(0, h.getTick() - 6) * (s.waterCells() * s.settings().idleWaterCoolingHuPerCellPerTick()
                    + s.steamCells() * s.settings().idleSteamCoolingHuPerCellPerTick());
            h.assertTrue(Math.abs(s.steamHu() - s.steam()) < 1e-6 && difference >= -1e-6 && difference <= coolingBound + 1e-6,
                    "R1两种汽真实成交HU不守恒：difference=" + difference);
            var normalCap = capability(h, normal, Direction.WEST); var scCap = capability(h, ports.getFirst(), Direction.WEST);
            CompoundTag before = s.save();
            int normalSim = normalCap.drain(256, IFluidHandler.FluidAction.SIMULATE).getAmount();
            int scSim = scCap.drain(256, IFluidHandler.FluidAction.SIMULATE).getAmount();
            h.assertTrue(before.equals(s.save()), "R1观测模拟写入账本或消耗额度");
            // 直邻罐已在本tick领取保压余量后，SIMULATE=0是合法结清；固定身份观察已付热库存，而不沿用旧自动切种短窗门。
            boolean qualified = s.outputQualified(NORMAL);
            if (s.removableSteam(NORMAL) > 0) pressureHeadTicks[0]++;
            int producedSc = s.producedKind() == SUPERCRITICAL ? s.produced() : 0;
            long drainedSc = Math.max(0, counts[6] + producedSc - s.steam(SUPERCRITICAL)); counts[6] = s.steam(SUPERCRITICAL);
            if (s.pressure() < s.settings().supercriticalPressure() && drainedSc > 0) { lowPressureScTicks[0]++; counts[7] += drainedSc; }
            if (qualified) { windows[0]++; windows[1]++; windows[2] = Math.max(windows[2], windows[0]); }
            else if (windows[0] > 0) { windows[3]++; windows[0] = 0; }
            if (h.getTick() == 100) { counts[5] = sink.executed + counts[3]; owner.selectMinimum(10); }
            if (h.getTick() == 450) midpointNormal[0] = receiver.executed;
            System.out.println("[pressure-r1] direct=" + direct + " t=" + h.getTick() + " P=" + s.pressure() + " Ts=" + s.steamTemperature()
                    + " min=" + s.minimumPressure() + " produced=" + s.produced() + " hot=" + counts[1] + " paidInputHU=" + .5 * (counts[2] + s.cold())
                    + " producedKind=" + s.producedKind() + " normalMb=" + s.steam(NORMAL) + " normalHU=" + s.steamHu(NORMAL)
                    + " scMb=" + s.steam(SUPERCRITICAL) + " scHU=" + s.steamHu(SUPERCRITICAL) + " lowPressureScTicks=" + lowPressureScTicks[0] + " lowPressureScMb=" + counts[7]
                    + " normalPaidStock=" + qualified + " stockWindow=" + windows[0] + " maxStockWindow=" + windows[2] + " paidStockTicks=" + windows[1] + " postTransactionHeadTicks=" + pressureHeadTicks[0]
                    + " currentNormal=" + normalCap.getFluidInTank(0) + " oldNormalTanks=" + firstCaps[1].getTanks()
                    + " oldScTanks=" + firstCaps[0].getTanks() + " normalSourceSim=" + normalSim + " scSourceSim=" + scSim
                    + " normalFillSim=" + receiver.simulated + " normalFillExecute=" + receiver.executed + " normalStored=" + stored
                    + " normalCapacity=" + receiver.getTankInventory().getCapacity()
                    + " scFillSim=" + sink.simulated + " scFillExecute=" + sink.executed + " turbineReceived=" + counts[3]
                    + " normalNative=" + r1Endpoint(h, normal.west(), Direction.EAST)
                    + " scNative=" + r1Endpoint(h, ports.getFirst().west(), Direction.EAST));
        });
        h.runAfterDelay(900, () -> {
            System.out.println("[pressure-r1-result] direct=" + direct + " normalPaidStockTicks=" + windows[1] + " maxStockWindow=" + windows[2]
                    + " windows=" + windows[3] + " normalExecute=" + receiver.executed + " scExecute=" + sink.executed + " turbine=" + counts[3]
                    + " lowPressureScTicks=" + lowPressureScTicks[0] + " lowPressureScMb=" + counts[7] + " postTransactionHeadTicks=" + pressureHeadTicks[0]
                    + " normalAfter450=" + (receiver.executed - midpointNormal[0]));
            h.assertTrue(counts[5] > 0, "R1下调前SC接收端未先成交");
            h.assertTrue(sink.executed + counts[3] > counts[5], "R1下调后SC支路永久停流");
            if (!direct) h.assertTrue(windows[1] >= 20, "R1长管未形成持续已付热普通库存，不能据此验收固定库存管路");
            h.assertTrue(receiver.executed > 0, "R1匹配NORMAL支路长期饥饿：direct=" + direct + " eligible=" + windows[1] + " maxWindow=" + windows[2]);
            h.assertTrue(receiver.executed > midpointNormal[0], "R1后半段NORMAL未继续真实成交");
            h.assertTrue(lowPressureScTicks[0] > 0, "R1未覆盖已有SC在生产炉压线以下真实可抽的工况");
            h.assertTrue(sink.nonSupercritical == 0 && receiver.nonNormal == 0, "R1纯过滤实际输出了错误汽种"); h.succeed();
        });
    }

    /** 读取首段真实LayerII与LayerIII等待，反射仅取值，不创建或修改网络异常。 */
    private static String r1Endpoint(GameTestHelper h, BlockPos pos, Direction side) {
        var transport = BlockEntityBehaviour.get(h.getLevel(), h.absolutePos(pos), FluidTransportBehaviour.TYPE);
        if (transport == null) return "direct";
        var connection = transport.getConnection(side); if (connection == null) return "disconnected";
        var flow = transport.getFlow(side); var network = (Optional<?>) field(connection, "network");
        String result = "{phase=" + transport.phase + ",P=" + connection.getPressure() + ",flow="
                + (flow == null ? "none" : flow.fluid + "/complete=" + flow.complete);
        var flowSource = (Optional<?>) field(connection, "source");
        if (flowSource.isPresent()) {
            var source = (com.simibubi.create.content.fluids.FlowSource) flowSource.get();
            var provider = source.provideHandler(); var handler = provider == null ? null : provider.getCapability();
            result += ",flowSource=" + source.getClass().getSimpleName() + ",handler="
                    + (handler == null ? "null" : handler.getTanks() + "/" + handler.drain(1, IFluidHandler.FluidAction.SIMULATE));
        }
        if (network.isPresent()) {
            Object source = field(network.get(), "source");
            var cap = source == null ? null : (IFluidHandler) ((com.simibubi.create.foundation.ICapabilityProvider<?>) source).getCapability();
            result += ",pause=" + field(network.get(), "pauseBeforePropagation") + ",source=" + (cap == null ? "null" : cap.getTanks());
        } else result += ",network=none";
        return result + "}";
    }

    /** 原生创造接收端只统计原生fill结果，不改无限接收或库存语义。 */
    private static final class R1CreativeTank extends CreativeFluidTankBlockEntity {
        private long simulated, executed, nonSupercritical;
        private R1CreativeTank(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { super(AllBlockEntityTypes.CREATIVE_FLUID_TANK.get(), pos, state); }
        @Override protected SmartFluidTank createInventory() {
            return new CreativeSmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged) {
                @Override public int fill(FluidStack stack, FluidAction action) {
                    int amount = super.fill(stack, action);
                    if (action.simulate()) simulated += amount;
                    else { executed += amount; if (!stack.is(BoilerContent.SUPERCRITICAL_STEAM.get())) nonSupercritical += amount; }
                    return amount;
                }
            };
        }
    }

    /** 普通罐仍由Create成型、储存和提供能力，仅累计真实fill的模拟与执行结果。 */
    private static final class R1NormalTank extends FluidTankBlockEntity {
        private long simulated, executed, nonNormal;
        private R1NormalTank(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { super(AllBlockEntityTypes.FLUID_TANK.get(), pos, state); }
        @Override protected SmartFluidTank createInventory() {
            return new SmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged) {
                @Override public int fill(FluidStack stack, FluidAction action) {
                    int amount = super.fill(stack, action);
                    if (action.simulate()) simulated += amount;
                    else { executed += amount; if (!stack.is(com.iksxh.create_nuclear_industry.content.TurbineContent.STEAM.get())) nonNormal += amount; }
                    return amount;
                }
            };
        }
    }

    /** 四口共同上行/跨炉顶管接单台真实汽轮机；3热口持续供料，16对付款上限288HU/t不变。 */
    private static void sharedTurbine(GameTestHelper h, TurbineState.Tier tier, boolean powered, boolean creative) {
        ExtensionBoilerGameTests.build(h, 6, 5, 6, 2);
        List<BlockPos> ports = new ArrayList<>(), pipes = new ArrayList<>();
        for (int z = 1; z <= 4; z++) {
            BlockPos p = BASE.offset(0, 3, z); ports.add(p);
            h.setBlock(p, BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.WEST)); pipes.add(p.west());
        }
        BlockPos front = new BlockPos(16, 5, tier.rotorCount() == 6 ? 5 : 3);
        clearTurbineInterior(h, front, tier); ExtensionTurbineGameTests.build(h, front, tier.rotorCount(), false);
        BlockPos inlet = ExtensionTurbineGameTests.inlet(front, tier), near = inlet.west();
        pipes.add(new BlockPos(3, 6, 8));
        for (int x = 3; x <= near.getX(); x++) pipes.add(new BlockPos(x, 7, 8));
        pipes.add(near.above()); pipes.add(near);
        BlockPos sinkPos = new BlockPos(10, 7, 6);
        if (creative) { pipes.add(sinkPos.south()); h.setBlock(sinkPos, AllBlocks.CREATIVE_FLUID_TANK.get()); }
        for (BlockPos p : pipes) h.setBlock(p, AllBlocks.FLUID_PIPE.get());
        RecordingCreativeTank[] sink = {null};
        if (creative) {
            // 仍用原生创造罐和其原生能力，只计数super.fill的真实成交；保留无限接收行为。
            BlockPos world = h.absolutePos(sinkPos); h.getLevel().removeBlockEntity(world);
            sink[0] = new RecordingCreativeTank(world, h.getBlockState(sinkPos)); h.getLevel().setBlockEntity(sink[0]);
        }
        if (powered) {
            BlockPos pump = new BlockPos(8, 7, 8), cog = pump.south(), motor = cog.east(2);
            h.setBlock(pump, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.EAST));
            h.setBlock(cog, AllBlocks.COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.X));
            h.setBlock(cog.east(), AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.X));
            h.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST));
            ((CreativeMotorBlockEntity) h.getBlockEntity(motor)).generatedSpeed.setValue(256);
        }
        BlockPos[] hot = {BASE.offset(3, 0, 0), BASE.offset(1, 0, 5), BASE.offset(2, 0, 5)};
        BlockPos[] cold = {BASE.offset(3, 2, 0), BASE.offset(1, 2, 5), BASE.offset(2, 2, 5)};
        for (int i = 1; i < hot.length; i++) {
            h.setBlock(hot[i], BoilerContent.HOT_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.SOUTH));
            h.setBlock(cold[i], BoilerContent.COLD_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.SOUTH));
        }
        for (BlockPos p : pipes) FluidPropagator.propagateChangedPipe(h.getLevel(), h.absolutePos(p), h.getBlockState(p));
        boolean[] active = {false}; long[] counts = new long[7]; int[] changedKinds = {0}, previousKind = {-1};
        h.runAfterDelay(6, () -> {
            var owner = ExtensionBoilerGameTests.owner(h); h.assertTrue(owner.currentForm() != null, "16对多汽口锅炉未成型");
            owner.selectMinimum(60); if (!creative) BoilerSteamSelectionGameTests.submit(h, ports.getLast(), 0);
            var tag = owner.ledger().save(); tag.putInt("Water", 16000); tag.putDouble("WaterHu", 54400);
            ExtensionBoilerGameTests.seedInventories(tag, 0, 0, 28000, 28000); owner.ledger().load(tag); active[0] = true;
            System.out.println("[shared-turbine] rotors=" + tier.rotorCount() + " powered=" + powered + " creative=" + creative + " topology=west4-manifold-up2-overRoof-east-down2, pipes=" + pipes);
        });
        h.onEachTick(() -> {
            if (!active[0]) return;
            var owner = ExtensionBoilerGameTests.owner(h); var s = owner.ledger();
            counts[0] += capability(h, BASE.offset(2, 1, 0), Direction.NORTH).fill(new FluidStack(Fluids.WATER, 256), IFluidHandler.FluidAction.EXECUTE);
            for (int i = 0; i < hot.length; i++) {
                Direction side = i == 0 ? Direction.NORTH : Direction.SOUTH;
                counts[1] += capability(h, hot[i], side).fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 256), IFluidHandler.FluidAction.EXECUTE);
                counts[2] += capability(h, cold[i], side).drain(256, IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
            var exhaust = ExtensionTurbineGameTests.handler(h, ExtensionTurbineGameTests.exhaust(front, tier), Direction.EAST);
            if (exhaust != null) counts[3] += exhaust.drain(256, IFluidHandler.FluidAction.EXECUTE).getAmount();
            counts[4] += s.produced(); var t = ExtensionTurbineGameTests.owner(h, front).ledger();
            long discarded = creative ? sink[0].accepted : 0;
            h.assertTrue(s.steam() + s.totalVented() + counts[3] + t.exhaust() + discarded == 28000 + counts[4], "汇流真实汽轮机mB不守恒");
            double carriedHu = counts[3] + t.exhaust() + discarded + s.totalVented();
            double heatDifference = 82400 + .5 * (counts[2] + s.cold()) - s.totalHu() - carriedHu;
            double coolingBound = Math.max(0, h.getTick() - 6) * (s.waterCells() * s.settings().idleWaterCoolingHuPerCellPerTick()
                    + s.steamCells() * s.settings().idleSteamCoolingHuPerCellPerTick());
            h.assertTrue(Math.abs(s.steamHu() - s.steam()) < 1e-6 && heatDifference >= -1e-6 && heatDifference <= coolingBound + 1e-6,
                    "汇流真实汽轮机/创造罐HU不守恒：difference=" + heatDifference + " coolingBound=" + coolingBound);
            int kind = s.outputQualified(SUPERCRITICAL) ? 1 : 0; if (previousKind[0] >= 0 && kind != previousKind[0]) changedKinds[0]++; previousKind[0] = kind;
            if (h.getTick() == 100) { counts[5] = counts[3]; owner.selectMinimum(10); }
            if (h.getTick() % 20 == 0) {
                System.out.println("[shared-turbine] rotors=" + tier.rotorCount() + " creative=" + creative + " powered=" + powered + " t=" + h.getTick() + " pairs=" + s.pairs() + " Ts=" + s.steamTemperature()
                        + " pressure=" + s.pressure() + " SC=" + s.outputQualified(SUPERCRITICAL) + " min=" + s.minimumPressure() + " hot=" + counts[1]
                        + " inputPaidHU=" + .5 * (counts[2] + s.cold()) + " produced=" + s.produced() + " cumulativeProduced=" + counts[4]
                        + " received=" + counts[3] + " discarded=" + discarded + " vented=" + s.totalVented() + " turbineFlow=" + t.averageFlowMbPerTick() + " SU=" + t.totalSu()
                        + " changes=" + changedKinds[0]);
                for (BlockPos p : ports) System.out.println("[shared-turbine] " + pipe(h, p.west()));
            }
        });
        h.runAfterDelay(900, () -> {
            h.assertTrue(counts[5] > 0, "调压前真实单台汇流夹具未先成交");
            h.assertTrue(counts[3] > counts[5] + 1000, "真实单台吞吐降下限后永久停流：rotors=" + tier.rotorCount() + " before=" + counts[5] + " after=" + counts[3]);
            if (creative) h.assertTrue(sink[0].accepted > 1000, "共同分支创造罐未真实销毁多余汽");
            if (creative) h.assertTrue(sink[0].nonSupercritical == 0, "纯SC汽口向创造罐真实送出异种流体");
            h.succeed();
        });
    }

    /** 6×6底面、5高、隔层y=2，西侧四口3SC/1普通；热液与水持续真实付款补给。 */
    private static void multiport(GameTestHelper h, boolean direct) {
        ExtensionBoilerGameTests.build(h, 6, 5, 6, 2);
        List<BlockPos> ports = new ArrayList<>();
        for (int z = 1; z <= 4; z++) {
            BlockPos port = BASE.offset(0, 3, z); ports.add(port);
            h.setBlock(port, BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.WEST));
            h.setBlock(port.west(), direct ? AllBlocks.FLUID_TANK.get() : AllBlocks.FLUID_PIPE.get());
        }
        BlockPos receiver = direct ? ports.getFirst().west() : BASE.offset(-1, 3, -1);
        if (!direct) {
            h.setBlock(BASE.offset(-1, 3, 0), AllBlocks.FLUID_PIPE.get());
            h.setBlock(receiver, AllBlocks.FLUID_TANK.get());
        }
        int[] initial = {0}, moved = {0}; boolean[] active = {false};
        h.runAfterDelay(6, () -> {
            var owner = ExtensionBoilerGameTests.owner(h);
            h.assertTrue(owner.currentForm() != null, "6×6×5汇流锅炉未成型");
            owner.selectMinimum(60); BoilerSteamSelectionGameTests.submit(h, ports.getLast(), 0);
            var seed = owner.ledger().save(); seed.putInt("Water", 16000); seed.putDouble("WaterHu", 65600);
            ExtensionBoilerGameTests.seedInventories(seed, 0, 0, 28000, 28000); owner.ledger().load(seed);
            active[0] = true;
        });
        h.onEachTick(() -> {
            if (!active[0]) return;
            var owner = ExtensionBoilerGameTests.owner(h); var s = owner.ledger();
            capability(h, BASE.offset(2, 1, 0), Direction.NORTH).fill(new FluidStack(Fluids.WATER, 256), IFluidHandler.FluidAction.EXECUTE);
            capability(h, BASE.offset(3, 0, 0), Direction.NORTH).fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 256), IFluidHandler.FluidAction.EXECUTE);
            capability(h, BASE.offset(3, 2, 0), Direction.NORTH).drain(256, IFluidHandler.FluidAction.EXECUTE);
            var fluid = capability(h, receiver, null).drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            moved[0] += fluid.getAmount();
            if (h.getTick() == 80) { initial[0] = moved[0]; owner.selectMinimum(10); }
            if (h.getTick() % 20 == 0) {
                System.out.println("[multiport] t=" + h.getTick() + " direct=" + direct + " steam=" + s.steam() + " HU=" + s.steamHu()
                        + " pressure=" + s.pressure() + " Ts=" + s.steamTemperature() + " SC=" + s.outputQualified(SUPERCRITICAL) + " min=" + s.minimumPressure() + " received=" + moved[0] + " last=" + fluid);
                for (BlockPos p : ports) System.out.println("[multiport] port=" + p + " declared=" + capability(h, p, Direction.WEST).getFluidInTank(0)
                        + " sim=" + capability(h, p, Direction.WEST).drain(1, IFluidHandler.FluidAction.SIMULATE) + " " + pipe(h, p.west()));
            }
        });
        h.runAfterDelay(280, () -> {
            h.assertTrue(moved[0] > initial[0], "四口共同管路降下限后完全停流：direct=" + direct + " before=" + initial[0] + " after=" + moved[0]);
            System.out.println("[multiport] completed direct=" + direct + " received=" + moved[0]); h.succeed();
        });
    }

    @GameTest(template = "multiport_empty", timeoutTicks = 580)
    public static void naturalMediumRepeatedSteamCutStopsNativeShafts(GameTestHelper h) { turbine(h, TurbineConfig.settings().mediumTier(), false); }

    @GameTest(template = "multiport_empty", timeoutTicks = 580)
    public static void naturalLargeRepeatedSteamCutStopsNativeShafts(GameTestHelper h) { turbine(h, TurbineConfig.settings().longTier(), false); }

    @GameTest(template = "multiport_empty", timeoutTicks = 580)
    public static void currentSavedMediumSteamCutStopsNativeShafts(GameTestHelper h) { turbine(h, TurbineConfig.settings().mediumTier(), true); }

    @GameTest(template = "multiport_empty", timeoutTicks = 580)
    public static void currentSavedLargeSteamCutStopsNativeShafts(GameTestHelper h) { turbine(h, TurbineConfig.settings().longTier(), true); }

    @GameTest(template = "multiport_empty", timeoutTicks = 150)
    public static void currentSavedStopPreservesRealExternalMotor(GameTestHelper h) {
        var tier = TurbineConfig.settings().mediumTier(); BlockPos front = new BlockPos(7, 5, 3);
        ExtensionTurbineGameTests.build(h, front, tier.rotorCount(), false);
        h.setBlock(front.north(), AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        BlockPos motorPos = front.north(2);
        h.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        ((CreativeMotorBlockEntity) h.getBlockEntity(motorPos)).generatedSpeed.setValue(256);
        h.onEachTick(() -> {
            var input = ExtensionTurbineGameTests.handler(h, ExtensionTurbineGameTests.inlet(front, tier), Direction.WEST);
            if (h.getTick() < 60 && input != null) input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 108), IFluidHandler.FluidAction.EXECUTE);
            var output = ExtensionTurbineGameTests.handler(h, ExtensionTurbineGameTests.exhaust(front, tier), Direction.EAST);
            if (output != null) output.drain(256, IFluidHandler.FluidAction.EXECUTE);
        });
        h.runAfterDelay(60, () -> {
            h.assertTrue(ExtensionTurbineGameTests.owner(h, front).totalSu() > 0, "外源对照未先实际发电");
            restoreCurrentShafts(h, front, front.south(tier.length() - 1));
        });
        h.runAfterDelay(120, () -> {
            var motor = (CreativeMotorBlockEntity) h.getBlockEntity(motorPos);
            float actual = motor.getOrCreateNetwork().calculateCapacity();
            float expected = motor.calculateAddedStressCapacity() * Math.abs(motor.getGeneratedSpeed());
            h.assertTrue(ExtensionTurbineGameTests.owner(h, front).totalSu() == 0 && Math.abs(actual - expected) < 2,
                    "当前恢复误清外源容量或残留本机SU：actual=" + actual + " expected=" + expected);
            h.assertTrue(((KineticBlockEntity) h.getBlockEntity(front)).getTheoreticalSpeed() == 256
                    && ((KineticBlockEntity) h.getBlockEntity(front.south(tier.length() - 1))).getTheoreticalSpeed() == 256,
                    "当前恢复误清真实外源转动"); h.succeed();
        });
    }

    @GameTest(template = "multiport_empty", timeoutTicks = 150)
    public static void recordedNaturalFaultCurrentSaveStopsWithoutReforming(GameTestHelper h) {
        var tier = TurbineConfig.settings().mediumTier(); BlockPos front = new BlockPos(7, 5, 3), rear = front.south(tier.length() - 1);
        clearTurbineInterior(h, front, tier); ExtensionTurbineGameTests.build(h, front, tier.rotorCount(), false);
        h.setBlock(front.north(), AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        h.setBlock(rear.south(), AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        h.runAfterDelay(10, () -> {
            // 本资源来自07自然失败t=160的真实服务端快照，仅重定位坐标，不拼造Source环或网络异常。
            CompoundTag snapshot;
            try (var stream = BoilerMultiportStopGameTests.class.getResourceAsStream("/data/create_nuclear_industry_boiler_multiport_stop/current_faulted_shaft.nbt")) {
                if (stream == null) throw new IllegalStateException("自然故障快照资源缺失");
                snapshot = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            } catch (java.io.IOException e) { throw new IllegalStateException("自然故障快照读取失败", e); }
            BlockPos origin = NbtUtils.readBlockPos(snapshot, "Origin").orElseThrow(); BlockPos delta = h.absolutePos(front).subtract(origin);
            String[] names = {"Front", "Rear", "ExternalFront", "ExternalRear"}; BlockPos[] positions = {front, rear, front.north(), rear.south()};
            for (int i = 0; i < positions.length; i++) {
                BlockPos world = h.absolutePos(positions[i]); CompoundTag tag = snapshot.getCompound(names[i]).copy();
                tag.putInt("x", world.getX()); tag.putInt("y", world.getY()); tag.putInt("z", world.getZ());
                if (tag.contains("Source")) tag.put("Source", NbtUtils.writeBlockPos(NbtUtils.readBlockPos(tag, "Source").orElseThrow().offset(delta)));
                if (tag.contains("Network")) {
                    CompoundTag network = tag.getCompound("Network"); network.putLong("Id", BlockPos.of(network.getLong("Id")).offset(delta).asLong());
                }
                System.out.println("[recorded-fault] " + names[i] + " speed=" + tag.getFloat("Speed") + " Network=" + tag.getCompound("Network"));
                var replacement = positions[i].equals(front) || positions[i].equals(rear)
                        ? new TurbineOutputShaftBlockEntity(world, h.getBlockState(positions[i]))
                        : new KineticBlockEntity(h.getBlockEntity(positions[i]).getType(), world, h.getBlockState(positions[i]));
                h.getLevel().removeBlockEntity(world); replacement.loadWithComponents(tag, h.getLevel().registryAccess()); h.getLevel().setBlockEntity(replacement);
            }
        });
        h.runAfterDelay(120, () -> {
            for (BlockPos p : new BlockPos[]{front, rear, front.north(), rear.south()})
                h.assertTrue(((KineticBlockEntity) h.getBlockEntity(p)).getTheoreticalSpeed() == 0, "真实故障当前快照再次加载仍残转：" + kinetic(h, p));
            h.succeed();
        });
    }

    /** 三次自然运行/断汽，账本40tick平均窗口与实际周转量耗尽后检查两轴与真实外接轴。 */
    private static void turbine(GameTestHelper h, TurbineState.Tier tier, boolean saved) {
        BlockPos front = new BlockPos(7, 5, 3), rear = front.south(tier.length() - 1);
        clearTurbineInterior(h, front, tier);
        ExtensionTurbineGameTests.build(h, front, tier.rotorCount(), false);
        h.setBlock(front.north(), AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        h.setBlock(rear.south(), AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        h.onEachTick(() -> {
            long t = h.getTick(); boolean supplying = t % 180 < 70;
            var input = ExtensionTurbineGameTests.handler(h, ExtensionTurbineGameTests.inlet(front, tier), Direction.WEST);
            if (supplying && input != null) input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), tier.ratedFlowMbPerTick()), IFluidHandler.FluidAction.EXECUTE);
            var output = ExtensionTurbineGameTests.handler(h, ExtensionTurbineGameTests.exhaust(front, tier), Direction.EAST);
            if (output != null) output.drain(tier.ratedFlowMbPerTick(), IFluidHandler.FluidAction.EXECUTE);
            if (ExtensionTurbineGameTests.owner(h, front) == null) return;
            if (t % 10 == 0) System.out.println("[natural-stop] saved=" + saved + " rotors=" + tier.rotorCount() + " t=" + t + " SU=" + ExtensionTurbineGameTests.owner(h, front).totalSu()
                    + " front=" + kinetic(h, front) + " rear=" + kinetic(h, rear) + " externalF=" + kinetic(h, front.north()) + " externalR=" + kinetic(h, rear.south()));
        });
        if (saved) h.runAfterDelay(70, () -> {
            // 保存真实运行产生的当前格式，不注入Source或Network；仅复用既有当前恢复夹具的实体重建边界。
            restoreCurrentShafts(h, front, rear);
            System.out.println("[natural-stop] restored current running snapshot rotors=" + tier.rotorCount());
        });
        for (int start : new int[]{0, 180, 360}) {
            h.runAfterDelay(start + 60, () -> h.assertTrue(ExtensionTurbineGameTests.owner(h, front).totalSu() > 0, "重复断汽夹具未先实际发电：" + ExtensionTurbineGameTests.owner(h, front).diagnostic().getString()));
            h.runAfterDelay(start + 165, () -> {
                h.assertTrue(ExtensionTurbineGameTests.owner(h, front).totalSu() == 0, "自然断汽后账本未归零");
                for (BlockPos p : new BlockPos[]{front, rear, front.north(), rear.south()})
                    h.assertTrue(((KineticBlockEntity) h.getBlockEntity(p)).getTheoreticalSpeed() == 0, "自然断汽零SU持续原生残转：" + p + " " + kinetic(h, p));
            });
        }
        h.runAfterDelay(535, h::succeed);
    }

    /** 原生创造罐的测试观察器，仅记录EXECUTE实际接收量；SIMULATE及原生能力语义全部保留。 */
    private static final class RecordingCreativeTank extends CreativeFluidTankBlockEntity {
        private long accepted;
        private long nonSupercritical;
        private RecordingCreativeTank(BlockPos p, net.minecraft.world.level.block.state.BlockState state) {
            super(AllBlockEntityTypes.CREATIVE_FLUID_TANK.get(), p, state);
        }
        @Override protected SmartFluidTank createInventory() {
            return new CreativeSmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged) {
                @Override public int fill(FluidStack resource, FluidAction action) {
                    int result = super.fill(resource, action);
                    if (action.execute()) {
                        accepted += result;
                        if (!resource.is(BoilerContent.SUPERCRITICAL_STEAM.get())) nonSupercritical += result;
                    }
                    return result;
                }
            };
        }
    }

    /** 复用搭建器跳过内腔；显式清空airSlot以隔离场地残留，不放宽真实结构校验。 */
    private static void clearTurbineInterior(GameTestHelper h, BlockPos front, TurbineState.Tier tier) {
        int radius = (tier.diameter() - 1) / 2;
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++)
            for (int z = 1; z < tier.length() - 1; z++)
                if (com.iksxh.create_nuclear_industry.turbine.TurbineGeometry.airSlot(tier.diameter(), x, y))
                    h.setBlock(front.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
    }

    /** 当前运行快照的恢复夹具；服务端重建实体，保留真实原生字段，不修改存档内容。 */
    private static void restoreCurrentShafts(GameTestHelper h, BlockPos front, BlockPos rear) {
        var owner = ExtensionTurbineGameTests.owner(h, front); owner.ledger().load(owner.ledger().save());
        for (BlockPos p : new BlockPos[]{front, rear}) {
            var old = (TurbineOutputShaftBlockEntity) h.getBlockEntity(p);
            var tag = old.saveWithFullMetadata(h.getLevel().registryAccess());
            BlockPos world = h.absolutePos(p); h.getLevel().removeBlockEntity(world);
            var replacement = new TurbineOutputShaftBlockEntity(world, h.getBlockState(p));
            replacement.loadWithComponents(tag, h.getLevel().registryAccess()); h.getLevel().setBlockEntity(replacement);
        }
    }

    private static IFluidHandler capability(GameTestHelper h, BlockPos p, Direction side) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(p), side);
    }

    private static String kinetic(GameTestHelper h, BlockPos p) {
        var k = (KineticBlockEntity) h.getBlockEntity(p);
        return "{rpm=" + k.getTheoreticalSpeed() + ",gen=" + k.getGeneratedSpeed() + ",source=" + k.source + ",network=" + k.network
                + ",capacity=" + (k.hasNetwork() ? k.getOrCreateNetwork().calculateCapacity() : 0)
                + ",sources=" + (k.hasNetwork() ? k.getOrCreateNetwork().sources.keySet().stream().map(s -> s.getBlockPos() + "/gen=" + s.getGeneratedSpeed()).toList() : List.of()) + "}";
    }

    /** 只读压力、LayerII与LayerIII真实来源，模拟drain不消耗额度或写库存。 */
    private static String pipe(GameTestHelper h, BlockPos p) {
        var b = BlockEntityBehaviour.get(h.getLevel(), h.absolutePos(p), FluidTransportBehaviour.TYPE);
        if (b == null) return "directTank";
        StringBuilder out = new StringBuilder("phase=" + b.phase);
        for (Direction d : Direction.values()) {
            var c = b.getConnection(d); if (c == null) continue;
            var f = b.getFlow(d); out.append(" ").append(d).append("[p=").append(c.getPressure()).append(",flow=")
                    .append(f == null ? "none" : f.fluid + ",in=" + f.inbound + ",complete=" + f.complete);
            var n = (Optional<?>) field(c, "network");
            if (n.isPresent()) {
                Object provider = field(n.get(), "source");
                var cap = provider == null ? null : (IFluidHandler) ((com.simibubi.create.foundation.ICapabilityProvider<?>) provider).getCapability();
                out.append(",targets=").append(((List<?>) field(n.get(), "targets")).size()).append(",cap=")
                        .append(cap == null ? "null" : cap.getTanks() + "/" + cap.getFluidInTank(0));
            }
            out.append("]");
        }
        return out.toString();
    }

    private static Object field(Object target, String name) {
        try { var f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("只读原生缓存诊断失败：" + name, e); }
    }
}
