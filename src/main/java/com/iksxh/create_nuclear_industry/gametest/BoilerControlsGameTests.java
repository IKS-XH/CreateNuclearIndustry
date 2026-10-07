package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsPacket;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 01B隔离域：原生数值包、冷液网络及跨资格交易；01D后蒸汽夹具显式选择汽种。 */
@GameTestHolder("create_nuclear_industry_boiler_controls")
@PrefixGameTestTemplate(false)
public final class BoilerControlsGameTests {
    private static final BlockPos BASE = new BlockPos(4, 2, 4);
    private static final BlockPos CONTROL = BASE.offset(1, 1, 0), COLD = BASE.offset(3, 2, 0), STEAM = BASE.offset(0, 3, 1);
    private BoilerControlsGameTests() {}
    private static BoilerControllerBlockEntity owner(GameTestHelper h) { return ExtensionBoilerGameTests.owner(h); }
    private static IFluidHandler handler(GameTestHelper h, BlockPos p, Direction side) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(p), side);
    }
    private static void seed(GameTestHelper h, int steam, double hu, int cold) {
        var tag = owner(h).ledger().save(); tag.putInt("Steam", steam); tag.putDouble("SteamHu", hu); tag.putInt("Cold", cold);
        owner(h).ledger().load(tag);
    }
    /** 使用Create真实包的服务端分发入口；FakePlayer只提供反馈音上下文，不能绕开netId路由。 */
    private static void submit(GameTestHelper h, int percent) {
        try {
            var packet = new ValueSettingsPacket(h.absolutePos(CONTROL), 0, percent, null, null, Direction.NORTH, false, 0);
            Method apply = ValueSettingsPacket.class.getDeclaredMethod("applySettings", ServerPlayer.class, SmartBlockEntity.class);
            apply.setAccessible(true); apply.invoke(packet, FakePlayerFactory.getMinecraft(h.getLevel()), owner(h));
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Create原生数值包无法提交", e); }
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 60)
    public static void nativePercentSettingsPersistAndCrossingDrainKeepsDeclaredFluid(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        int[] values = {17, 43, 70, 0, 100};
        for (int i = 0; i < values.length; i++) {
            int value = values[i]; long tick = 4 + i * 3;
            h.runAfterDelay(tick, () -> {
                h.assertTrue(owner(h).getAllBehaviours().stream().filter(b -> b instanceof ValueSettingsBehaviour).count() == 1,
                        "控制器仍有多个默认netId控件");
                submit(h, value);
                h.assertTrue(Math.abs(owner(h).ledger().minimumPressure() - value / 100D) < 1e-8, "原生提交错误：" + value);
            });
            h.runAfterDelay(tick + 1, () -> {
                var s = owner(h).ledger(); h.assertTrue(Math.abs(s.minimumPressure() - value / 100D) < 1e-8, "tick覆盖百分数：" + value);
                var saved = owner(h).savePortableData(); owner(h).loadPortableData(saved);
                h.assertTrue(saved.equals(owner(h).savePortableData()), "当前保存恢复覆盖显式下限：" + value);
            });
        }
        h.runAfterDelay(22, () -> {
            submit(h, 0); seed(h, 9100, 9100, 1234);
            var cold = handler(h, COLD, Direction.NORTH); var steam = handler(h, STEAM, Direction.WEST); var s = owner(h).ledger();
            var before = s.save(); FluidStack simulated = steam.drain(256, IFluidHandler.FluidAction.SIMULATE);
            h.assertTrue(simulated.is(BoilerContent.SUPERCRITICAL_STEAM.get()) && before.equals(s.save()), "模拟汽种错误或写入账本");
            FluidStack actual = steam.drain(simulated, IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(actual.is(simulated.getFluid()) && actual.getAmount() == 256 && s.steam() == 8844 && s.steamHu() == 8844,
                    "跨压力门槛后返回了另一汽种或未扣实际焓");
            h.assertTrue(steam.drain(256, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "旧汽网络generic drain抽走新汽");
            BoilerSteamSelectionGameTests.submit(h, STEAM, 0);
            var next = handler(h, STEAM, Direction.WEST);
            h.assertTrue(next.getFluidInTank(0).is(TurbineContent.STEAM.get()) && cold.getTanks() == 1, "汽种未更新或连带撤销冷口");
            h.assertTrue(next.drain(256, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "重建汽口复制同tick额度");
            h.setBlock(BASE, Blocks.AIR); owner(h).invalidateForm(); h.assertTrue(cold.getTanks() == 0, "真实拆炉未撤销旧冷口");
            h.setBlock(BASE, BoilerContent.CASING.get()); owner(h).invalidateForm();
            h.assertTrue(owner(h).currentForm() != null && cold.getTanks() == 0, "重装后结构旧句柄复活");
            System.out.println("[boiler-controls] native packet percentages 17/43/70/0/100 persisted; crossing drain SC->steam returned declared SC, 256mB/256HU; stale generic rejected; structure epoch preserved");
            h.succeed();
        });
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 130)
    public static void poweredColdPipeSurvivesPressureAndAutomaticSteamChanges(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        BlockPos near = COLD.north(), pump = COLD.north(2), far = COLD.north(3), tank = COLD.north(4);
        BlockPos cog = pump.east(), shaft = cog.north(), motor = shaft.north();
        h.setBlock(tank, AllBlocks.FLUID_TANK.get()); h.setBlock(near, AllBlocks.FLUID_PIPE.get()); h.setBlock(far, AllBlocks.FLUID_PIPE.get());
        // 与01A同传动夹具；本次向北出冷液，Create泵按FACING定流向，故仅将SOUTH输入泵反向为NORTH。
        h.setBlock(pump, AllBlocks.MECHANICAL_PUMP.get().defaultBlockState().setValue(PumpBlock.FACING, Direction.NORTH));
        h.setBlock(cog, AllBlocks.COGWHEEL.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        h.setBlock(shaft, AllBlocks.SHAFT.get().defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Z));
        h.setBlock(motor, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        ((CreativeMotorBlockEntity) h.getBlockEntity(motor)).generatedSpeed.setValue(256);
        for (BlockPos p : new BlockPos[]{near, far}) FluidPropagator.propagateChangedPipe(h.getLevel(), h.absolutePos(p), h.getBlockState(p));
        IFluidHandler[] oldCold = {null}; int[] received = {0, 0};
        h.runAfterDelay(4, () -> { seed(h, 12000, 11400, 30000); oldCold[0] = handler(h, COLD, Direction.NORTH); });
        h.runAfterDelay(35, () -> {
            var target = handler(h, tank, Direction.SOUTH); received[0] = target.getFluidInTank(0).getAmount();
            h.assertTrue(((PumpBlockEntity) h.getBlockEntity(pump)).getSpeed() != 0 && received[0] > 0, "真实冷液泵尚未出流");
            submit(h, 43); seed(h, 12000, 12400, owner(h).ledger().cold());
        });
        h.runAfterDelay(60, () -> {
            received[1] = handler(h, tank, Direction.SOUTH).getFluidInTank(0).getAmount();
            h.assertTrue(received[1] > received[0] && owner(h).ledger().supercritical() && oldCold[0].getTanks() == 1,
                    "调压或普通->超临界使冷液停流/句柄失效");
            submit(h, 100); seed(h, 12000, 9600, owner(h).ledger().cold());
        });
        h.runAfterDelay(90, () -> {
            var fluid = handler(h, tank, Direction.SOUTH).getFluidInTank(0);
            h.assertTrue(fluid.is(ModFluids.COMPOUND_COOLANT_SOURCE.get()) && fluid.getAmount() > received[1]
                    && !owner(h).ledger().supercritical() && oldCold[0].getTanks() == 1, "超临界->普通或100%设置使冷液停流");
            h.assertTrue(fluid.getAmount() + owner(h).ledger().cold() == 30000, "真实冷液管路丢量或复制");
            System.out.println("[boiler-controls] cold native pump continuous: " + received[0] + " -> " + received[1] + " -> " + fluid.getAmount()
                    + "; original capability alive across pressure 43/100 and steam->SC->steam; coolant total=30000mB");
            h.succeed();
        });
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 180)
    public static void nativeSteamPipeChangesTypeWithoutLossOrExternalRewrite(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        BlockPos pipe = STEAM.west(), tank = STEAM.west(2);
        h.setBlock(pipe, AllBlocks.FLUID_PIPE.get()); h.setBlock(tank, AllBlocks.FLUID_TANK.get());
        int[] withdrawn = {0}; boolean[] tracking = {false}; int[] previousAmount = {0}; double[] previousHu = {0};
        h.runAfterDelay(4, () -> { submit(h, 0); BoilerSteamSelectionGameTests.submit(h, STEAM, 0); seed(h, 14000, 11200, 0); tracking[0] = true; previousAmount[0] = 14000; previousHu[0] = 11200; });
        // 每tick核对真实接收量；有汽种差异却被generic抽出后丢弃，会立刻破坏质量等式。
        h.onEachTick(() -> {
            if (!tracking[0]) return;
            var s = owner(h).ledger(); var target = handler(h, tank, Direction.EAST);
            int inTank = target == null ? 0 : target.getFluidInTank(0).getAmount();
            h.assertTrue(s.steam() + inTank + withdrawn[0] + s.totalVented() == 14000, "汽种更新导致真实管路丢汽或复制");
            int n = previousAmount[0] - s.steam(); double loss = previousHu[0] - s.steamHu();
            if (n >= 0) {
                double carried = s.steam() == 0 ? loss : n * s.steamHu() / s.steam();
                h.assertTrue(loss + 1e-6 >= carried && loss <= carried + 1.1,
                        "出汽HU不符实际比焓及每tick最多0.9HU散热：n=" + n + ", loss=" + loss + ", carried=" + carried);
            }
            previousAmount[0] = s.steam(); previousHu[0] = s.steamHu();
        });
        h.runAfterDelay(28, () -> {
            var target = handler(h, tank, Direction.EAST); var fluid = target.getFluidInTank(0);
            h.assertTrue(fluid.is(TurbineContent.STEAM.get()) && fluid.getAmount() > 0, "原生管路未送普通蒸汽");
            withdrawn[0] += target.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).getAmount();
            var s = owner(h).ledger(); seed(h, s.steam(), s.steam() * 1.05, 0); previousHu[0] = s.steamHu(); previousAmount[0] = s.steam();
            BoilerSteamSelectionGameTests.submit(h, STEAM, 1);
        });
        h.runAfterDelay(65, () -> {
            var target = handler(h, tank, Direction.EAST); var fluid = target.getFluidInTank(0);
            h.assertTrue(fluid.is(BoilerContent.SUPERCRITICAL_STEAM.get()) && fluid.getAmount() > 0, "汽种变化后原生管路未自行更新为超临界蒸汽");
            // 保留外罐超临界蒸汽；降低炉内夹具显热并显式选择普通汽，异种罐必须保持真实背压。
            submit(h, 0); var s = owner(h).ledger(); seed(h, s.steam(), s.steam() * .8, 0); previousHu[0] = s.steamHu(); previousAmount[0] = s.steam();
            BoilerSteamSelectionGameTests.submit(h, STEAM, 0);
        });
        h.runAfterDelay(82, () -> {
            var target = handler(h, tank, Direction.EAST); var fluid = target.getFluidInTank(0);
            h.assertTrue(fluid.is(BoilerContent.SUPERCRITICAL_STEAM.get()) && fluid.getAmount() > 0, "自动汽种改写或删除了外罐异种流体");
            // 接收方真实抽取异种汽后腾出空间；测试与产品均不强制改写外罐。
            withdrawn[0] += target.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).getAmount(); submit(h, 0);
        });
        h.runAfterDelay(115, () -> {
            var fluid = handler(h, tank, Direction.EAST).getFluidInTank(0);
            h.assertTrue(fluid.is(TurbineContent.STEAM.get()) && fluid.getAmount() > 0, "清空接收罐后管网未自动恢复普通汽");
            System.out.println("[boiler-controls] selected steam native pipe steam->SC->steam without replacing pipe; every tick mass/HU checked; external SC retained until explicit receiver drain");
            h.succeed();
        });
    }
}
