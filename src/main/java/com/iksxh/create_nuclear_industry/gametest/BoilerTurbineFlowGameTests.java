package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.boiler.BoilerPartBlock;
import com.iksxh.create_nuclear_industry.config.TurbineConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.turbine.TurbineState;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 01C服务端持续流动诊断：mB、HU、mB/t及SU分别观测，不将压力下限当作目标炉压。 */
@GameTestHolder("create_nuclear_industry_boiler_turbine_flow")
@PrefixGameTestTemplate(false)
public final class BoilerTurbineFlowGameTests {
    private static final BlockPos BASE = new BlockPos(4, 2, 4);
    private static final BlockPos STEAM = BASE.offset(4, 3, 1);
    private BoilerTurbineFlowGameTests() {}

    @GameTest(template = "flow_empty", timeoutTicks = 820)
    public static void continuousMediumAndRepeatedZero(GameTestHelper h) { scenario(h, TurbineConfig.settings().mediumTier()); }
    @GameTest(template = "flow_empty", timeoutTicks = 820)
    public static void continuousLargeAndRepeatedZero(GameTestHelper h) { scenario(h, TurbineConfig.settings().longTier()); }
    @GameTest(template = "flow_empty", timeoutTicks = 820)
    public static void continuousEmptyReceiver(GameTestHelper h) { scenario(h, null); }

    /** 复用已验证外部异种背压与每tick质量/实际焓断言，确认重建仅影响运输缓存。 */
    @GameTest(template = "flow_empty", timeoutTicks = 180)
    public static void endpointRebuildPreservesExternalMixedTypeBackpressure(GameTestHelper h) {
        BoilerControlsGameTests.nativeSteamPipeChangesTypeWithoutLossOrExternalRewrite(h);
    }

    /** 单独诊断真实账本及双轴回接：供汽只经实际能力成交，四个原生齿轮箱闭环没有额外生成源。 */
    @GameTest(template = "flow_empty", timeoutTicks = 360)
    public static void nativeBothEndsLoopStopsAcrossRepeatedOperatingThreshold(GameTestHelper h) {
        var tier = TurbineConfig.settings().mediumTier(); BlockPos front = new BlockPos(7, 5, 3);
        ExtensionTurbineGameTests.build(h, front, tier.rotorCount(), false);
        BlockPos rear = front.south(tier.length() - 1), a = front.north(), b = rear.south();
        h.setBlock(a, AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        h.setBlock(b, AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        h.runAfterDelay(35, () -> {
            h.assertTrue(ExtensionTurbineGameTests.owner(h, front).totalSu() > 0, "闭环夹具未先发电");
            int outerX = front.getX() + 6;
            for (BlockPos corner : new BlockPos[]{a, b, new BlockPos(outerX, 5, a.getZ()), new BlockPos(outerX, 5, b.getZ())})
                h.setBlock(corner, AllBlocks.GEARBOX.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
            for (int x = front.getX() + 1; x < outerX; x++) for (int z : new int[]{a.getZ(), b.getZ()})
                h.setBlock(new BlockPos(x, 5, z), AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.X));
            for (int z = a.getZ() + 1; z < b.getZ(); z++)
                h.setBlock(new BlockPos(outerX, 5, z), AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        });
        h.onEachTick(() -> {
            long tick = h.getTick(); int rate = tick < 70 || tick >= 150 && tick < 220 ? tier.ratedFlowMbPerTick() : 0;
            var input = ExtensionTurbineGameTests.handler(h, ExtensionTurbineGameTests.inlet(front, tier), Direction.WEST);
            if (input != null && rate > 0) input.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), rate), IFluidHandler.FluidAction.EXECUTE);
            var output = ExtensionTurbineGameTests.handler(h, ExtensionTurbineGameTests.exhaust(front, tier), Direction.EAST);
            if (output != null) output.drain(tier.ratedFlowMbPerTick(), IFluidHandler.FluidAction.EXECUTE);
            if (tick % 10 == 0) System.out.println("[continuous-flow-loop] t=" + tick + " SU=" + ExtensionTurbineGameTests.owner(h, front).totalSu()
                    + " front=" + kinetic(h, front) + " rear=" + kinetic(h, rear) + " extF=" + kinetic(h, a) + " extR=" + kinetic(h, b));
        });
        for (int at : new int[]{140, 310}) h.runAfterDelay(at, () -> {
            var owner = ExtensionTurbineGameTests.owner(h, front);
            h.assertTrue(owner.totalSu() == 0, "闭环断汽后账本未归零");
            h.assertTrue(((KineticBlockEntity) h.getBlockEntity(a)).getTheoreticalSpeed() == 0
                    && ((KineticBlockEntity) h.getBlockEntity(b)).getTheoreticalSpeed() == 0,
                    "双端无外源闭环零SU后持续残转：front=" + kinetic(h, front) + " rear=" + kinetic(h, rear) + " extF=" + kinetic(h, a) + " extR=" + kinetic(h, b));
        });
        h.runAfterDelay(320, h::succeed);
    }

    private static IFluidHandler handler(GameTestHelper h, BlockPos p) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(p), null);
    }
    /** 储罐是真实供料/收料边界；仅补充罐中原料和抽取罐中产物，不能直接注入炉内HU或汽轮机蒸汽。 */
    private static BlockPos pump(GameTestHelper h, BlockPos port, Direction outward, boolean input, boolean left) {
        BlockPos near = port.relative(outward), pump = port.relative(outward, 2);
        BlockPos far = port.relative(outward, 3), tank = port.relative(outward, 4);
        BlockPos cog = left ? pump.west() : pump.east();
        BlockPos shaft = cog.relative(outward), motor = cog.relative(outward, 2);
        h.setBlock(tank, AllBlocks.FLUID_TANK.get());
        h.setBlock(near, AllBlocks.FLUID_PIPE.get()); h.setBlock(far, AllBlocks.FLUID_PIPE.get());
        h.setBlock(pump, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, input ? outward.getOpposite() : outward));
        h.setBlock(cog, AllBlocks.COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        h.setBlock(shaft, AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        h.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, outward.getOpposite()));
        ((CreativeMotorBlockEntity) h.getBlockEntity(motor)).generatedSpeed.setValue(256);
        for (BlockPos p : new BlockPos[]{near, far}) FluidPropagator.propagateChangedPipe(h.getLevel(), h.absolutePos(p), h.getBlockState(p));
        return tank;
    }

    private static String kinetic(GameTestHelper h, BlockPos p) {
        KineticBlockEntity k = (KineticBlockEntity) h.getBlockEntity(p);
        return "{rpm=" + k.getTheoreticalSpeed() + ",gen=" + k.getGeneratedSpeed() + ",source=" + k.source
                + ",net=" + k.network + ",capacity=" + (k.hasNetwork() ? k.getOrCreateNetwork().calculateCapacity() : 0) + "}";
    }

    /** 只读Create第二/三层流动缓存；含端口压力、相位、汽种和方向，定位炉内有汽但管网停止的边界。 */
    private static String pipe(GameTestHelper h, BlockPos p) {
        var b = BlockEntityBehaviour.get(h.getLevel(), h.absolutePos(p), FluidTransportBehaviour.TYPE);
        if (b == null) return p + " missing";
        StringBuilder result = new StringBuilder(p + " phase=" + b.phase);
        for (Direction d : Direction.values()) {
            var c = b.getConnection(d); if (c == null) continue;
            var f = b.getFlow(d);
            result.append(" ").append(d).append("[p=").append(c.getPressure())
                    .append(",flow=").append(f == null ? "none" : f.fluid + ",in=" + f.inbound + ",complete=" + f.complete).append("]");
            var optional = (java.util.Optional<?>) field(c, "network");
            if (optional.isPresent()) {
                Object n = optional.get();
                var provider = (com.simibubi.create.foundation.ICapabilityProvider<?>) field(n, "source");
                var cap = provider == null ? null : (IFluidHandler) provider.getCapability();
                var currentSource = (java.util.Optional<?>) field(c, "source");
                var liveProvider = currentSource.isEmpty() ? null : ((com.simibubi.create.content.fluids.FlowSource) currentSource.get()).provideHandler();
                var liveCap = liveProvider == null ? null : liveProvider.getCapability();
                result.append("{networkFluid=").append(field(n, "fluid")).append(",targets=").append(((List<?>) field(n, "targets")).size())
                        .append(",queued=").append(((List<?>) field(n, "queued")).size()).append(",frontier=").append(((java.util.Set<?>) field(n, "frontier")).size())
                        .append(",cap=").append(cap == null ? "null" : cap.getTanks() + "/" + cap.getFluidInTank(0) + "/sim=" + cap.drain(1, IFluidHandler.FluidAction.SIMULATE))
                        .append(",currentSourceCap=").append(liveCap == null ? "null" : liveCap.getTanks() + "/" + liveCap.getFluidInTank(0) + "/sim=" + liveCap.drain(1, IFluidHandler.FluidAction.SIMULATE))
                        .append(",sameProvider=").append(provider == liveProvider).append("}");
            }
        }
        return result.toString();
    }

    private static Object field(Object target, String name) {
        try { var f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("只读Create诊断字段失败：" + name, e); }
    }

    /** 初始种子只缩短合法已预热炉的等待；之后实际补水、热转冷付款、产汽及原生管网排汽运行。 */
    private static void scenario(GameTestHelper h, TurbineState.Tier tier) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        h.setBlock(BASE.offset(0, 3, 1), BoilerContent.CASING.get());
        h.setBlock(STEAM, BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.EAST));
        BlockPos hot2 = BASE.offset(3, 0, 4), cold2 = BASE.offset(3, 2, 4);
        h.setBlock(hot2, BoilerContent.HOT_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.SOUTH));
        h.setBlock(cold2, BoilerContent.COLD_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.SOUTH));
        BlockPos waterTank = pump(h, BASE.offset(2, 1, 0), Direction.NORTH, true, true);
        BlockPos hotTank = pump(h, BASE.offset(3, 0, 0), Direction.NORTH, true, false);
        BlockPos hotTank2 = pump(h, hot2, Direction.SOUTH, true, false);
        BlockPos coldTank = pump(h, BASE.offset(3, 2, 0), Direction.NORTH, false, false);
        BlockPos coldTank2 = pump(h, cold2, Direction.SOUTH, false, false);
        int center = tier == null ? 0 : tier.length() / 2 - (tier.length() % 2 == 0 ? 1 : 0);
        BlockPos front = new BlockPos(16, 5, 5 - center);
        BlockPos receiver;
        List<BlockPos> pipes = new ArrayList<>();
        if (tier == null) { receiver = STEAM.east(5); for (int i = 1; i < 5; i++) pipes.add(STEAM.east(i)); }
        else {
            ExtensionTurbineGameTests.build(h, front, tier.rotorCount(), false);
            BlockPos inlet = ExtensionTurbineGameTests.inlet(front, tier);
            for (int x = STEAM.getX() + 1; x < inlet.getX(); x++) pipes.add(new BlockPos(x, 5, 5));
            BlockPos exhaust = ExtensionTurbineGameTests.exhaust(front, tier);
            receiver = exhaust.east(3); pipes.add(exhaust.east()); pipes.add(exhaust.east(2));
            h.setBlock(front.north(), AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
            h.setBlock(front.south(tier.length()), AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        }
        h.setBlock(receiver, AllBlocks.FLUID_TANK.get());
        for (BlockPos p : pipes) h.setBlock(p, AllBlocks.FLUID_PIPE.get());
        for (BlockPos p : pipes) FluidPropagator.propagateChangedPipe(h.getLevel(), h.absolutePos(p), h.getBlockState(p));
        String label = tier == null ? "tank" : "rotors" + tier.rotorCount();
        long[] totals = new long[5]; int[] zeroTicks = {0}, kindChanges = {0}, previousKind = {-1};
        boolean[] initialized = {false}, generated = {false};
        h.runAfterDelay(6, () -> {
            var owner = ExtensionBoilerGameTests.owner(h); h.assertTrue(owner.currentForm() != null, "锅炉连续夹具未成型");
            owner.selectMinimum(60);
            var seed = owner.ledger().save(); seed.putInt("Water", 8000); seed.putDouble("WaterHu", (14400 + 800) * 2D);
            seed.putInt("Steam", 14400); seed.putDouble("SteamHu", 14400); owner.ledger().load(seed); initialized[0] = true;
            System.out.println("[continuous-flow] " + label + " seed water=8000mB waterHU=30400 steam=14400mB steamHU=14400; native pump motors isolated from turbine");
        });
        h.onEachTick(() -> {
            totals[0] += fill(h, waterTank, Fluids.WATER);
            totals[1] += fill(h, hotTank, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get()) + fill(h, hotTank2, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get());
            totals[2] += drain(h, coldTank) + drain(h, coldTank2); totals[3] += drain(h, receiver);
            if (!initialized[0]) return;
            var s = ExtensionBoilerGameTests.owner(h).ledger(); totals[4] += s.produced();
            // 本旧连续夹具主动提交当前实际汽种，保留原HU/流量/停转断言；产品汽口不再自动换种。
            BoilerSteamSelectionGameTests.submit(h, STEAM, s.supercritical() ? 1 : 0);
            int turnover = tier == null ? 0 : ExtensionTurbineGameTests.owner(h, front).ledger().exhaust();
            h.assertTrue(s.steam() + s.totalVented() + totals[3] + turnover == 14400 + totals[4], "持续产汽质量不守恒："
                    + label + " steam=" + s.steam() + " vent=" + s.totalVented() + " received=" + totals[3] + " turnover=" + turnover + " produced=" + totals[4]);
            // 连续工况汽温始终为2，每mB真实焓为1HU；低炉压时夹具选普通汽也必须扣实际比焓。
            h.assertTrue(Math.abs(s.steamHu() - s.steam()) < 1e-6, "连续工况汽温离开Ts=2，需按实际焓另行计量");
            double exportHu = totals[3] + turnover + s.totalVented();
            double heatDifference = 44800 + (totals[2] + s.cold()) * .5 - s.totalHu() - exportHu;
            long tick = h.getTick();
            double maximumCooling = Math.max(0, tick - 6) * (s.waterCells() * s.settings().idleWaterCoolingHuPerCellPerTick()
                    + s.steamCells() * s.settings().idleSteamCoolingHuPerCellPerTick());
            h.assertTrue(heatDifference >= -1e-6 && heatDifference <= maximumCooling + 1e-6,
                    "持续流动HU不守恒：" + label + " unpaidOrLostHU=" + heatDifference + " maximumCoolingHU=" + maximumCooling);
            int kind = s.supercritical() ? 2 : 1;
            if (previousKind[0] != -1 && previousKind[0] != kind) kindChanges[0]++;
            previousKind[0] = kind;
            if (tick == 100) ExtensionBoilerGameTests.owner(h).selectMinimum(10);
            if (tick == 450 || tick == 650) ExtensionBoilerGameTests.owner(h).selectMinimum(100);
            if (tick == 550) ExtensionBoilerGameTests.owner(h).selectMinimum(10);
            String machine = "";
            if (tier != null) {
                var owner = ExtensionTurbineGameTests.owner(h, front); var t = owner.ledger();
                if (owner.totalSu() > 0) { generated[0] = true; zeroTicks[0] = 0; }
                else if (generated[0]) zeroTicks[0]++;
                machine = " turnover=" + t.exhaust() + " out=" + t.processed(h.getLevel().getGameTime()) + " avg=" + t.averageFlowMbPerTick() + " SU=" + owner.totalSu()
                        + " front=" + kinetic(h, front) + " rear=" + kinetic(h, front.south(tier.length() - 1))
                        + " extF=" + kinetic(h, front.north()) + " extR=" + kinetic(h, front.south(tier.length()));
            }
            if (tick % 20 == 0 || zeroTicks[0] == 1 || zeroTicks[0] == 8)
                System.out.println("[continuous-flow] " + label + " t=" + tick + " min=" + s.minimumPressure() + " P=" + s.pressure() + " Tw=" + s.waterTemperature() + " Ts=" + s.steamTemperature()
                        + " SC=" + s.supercritical() + " steam=" + s.steam() + " prod=" + s.produced() + " vent=" + s.vented() + " ventTotal=" + s.totalVented()
                        + " hot=" + s.hot() + " cold=" + s.cold() + " water=" + s.water() + " received=" + totals[3] + " coldRemoved=" + totals[2] + machine);
            if (zeroTicks[0] >= 8) {
                KineticBlockEntity a = (KineticBlockEntity) h.getBlockEntity(front.north()), b = (KineticBlockEntity) h.getBlockEntity(front.south(tier.length()));
                h.assertTrue(a.getTheoreticalSpeed() == 0 && b.getTheoreticalSpeed() == 0, "无外源零SU持续8tick后外接轴残转：" + machine);
            }
        });
        h.runAfterDelay(180, () -> {
            for (BlockPos p : pipes) System.out.println("[continuous-flow-pipe] " + label + " t=180 " + pipe(h, p));
        });
        h.runAfterDelay(350, () -> {
            for (BlockPos p : pipes) System.out.println("[continuous-flow-pipe] " + label + " t=350 " + pipe(h, p));
            if (tier == null || tier.rotorCount() == TurbineConfig.settings().longTier().rotorCount())
                h.assertTrue(totals[3] > 25000, "60→10持续供热下接收端长期停流：" + label + " received=" + totals[3]);
        });
        h.runAfterDelay(800, () -> {
            h.assertTrue(totals[2] > 10000 && totals[3] > 10000, "真实连续冷热/蒸汽链路未产生足量成交：cold=" + totals[2] + " received=" + totals[3]);
            if (tier != null) h.assertTrue(generated[0], "联动从未达到启动SU门槛");
            if (tier == null || tier.rotorCount() == TurbineConfig.settings().longTier().rotorCount())
                h.assertTrue(kindChanges[0] >= 2, "持续工况未自然往返汽种资格门槛");
            System.out.println("[continuous-flow] " + label + " complete received=" + totals[3] + " produced=" + totals[4] + " naturalKindChanges=" + kindChanges[0]); h.succeed();
        });
    }
    private static int fill(GameTestHelper h, BlockPos p, Fluid fluid) {
        var tank = handler(h, p); return tank == null ? 0 : tank.fill(new FluidStack(fluid, 8000), IFluidHandler.FluidAction.EXECUTE);
    }
    private static int drain(GameTestHelper h, BlockPos p) {
        var tank = handler(h, p); return tank == null ? 0 : tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).getAmount();
    }
}
