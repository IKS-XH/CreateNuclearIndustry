package com.iksxh.create_nuclear_industry.gametest;

import static com.iksxh.create_nuclear_industry.boiler.BoilerSteamInventoryKind.*;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.boiler.BoilerPartBlock;
import com.iksxh.create_nuclear_industry.boiler.BoilerPortBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerSteamKind;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsPacket;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 独立汽口合同：各口只取对应真实库存，默认SC不能提前输出另一池普通汽。 */
@GameTestHolder("create_nuclear_industry_boiler_steam_selection")
@PrefixGameTestTemplate(false)
public final class BoilerSteamSelectionGameTests {
    private static final BlockPos BASE = new BlockPos(4, 2, 4), STEAM = BASE.offset(0, 3, 1);
    private static final BlockPos SECOND = BASE.offset(4, 3, 1), COLD = BASE.offset(3, 2, 0);
    private BoilerSteamSelectionGameTests() {}
    private static IFluidHandler handler(GameTestHelper h, BlockPos p, Direction side) {
        return h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(p), side);
    }
    private static BoilerPortBlockEntity port(GameTestHelper h, BlockPos p) { return (BoilerPortBlockEntity) h.getBlockEntity(p); }
    private static void twoPorts(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        h.setBlock(SECOND, BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.EAST));
    }
    private static void seed(GameTestHelper h, int amount, double hu, int cold, boolean sc) {
        var owner = ExtensionBoilerGameTests.owner(h); var tag = owner.ledger().save();
        ExtensionBoilerGameTests.seedInventories(tag, sc ? 0 : amount, sc ? 0 : hu, sc ? amount : 0, sc ? hu : 0);
        tag.putInt("Cold", cold); owner.ledger().load(tag);
    }
    /** 用Create真实选项包的服务端路由，不能直接调用产品选择回调绕过行为校验。 */
    static void submit(GameTestHelper h, BlockPos p, int value) { submit(h, p, 0, value); }
    private static void submit(GameTestHelper h, BlockPos p, int row, int value) {
        try {
            var packet = new ValueSettingsPacket(h.absolutePos(p), row, value, null, null, h.getBlockState(p).getValue(BoilerPartBlock.FACING), false, 0);
            Method apply = ValueSettingsPacket.class.getDeclaredMethod("applySettings", ServerPlayer.class, SmartBlockEntity.class);
            apply.setAccessible(true); apply.invoke(packet, FakePlayerFactory.getMinecraft(h.getLevel()), h.getBlockEntity(p));
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Create原生汽种包无法提交", e); }
    }
    private static Object field(Object target, String name) {
        try { var f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("只读汽口事务诊断失败：" + name, e); }
    }
    @GameTest(template = "inventory_empty", templateNamespace = "create_nuclear_industry_boiler_inventory", timeoutTicks = 40)
    public static void defaultSupercriticalWaitsWithoutConvertingOrdinarySteam(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        h.setBlock(STEAM.west(), AllBlocks.FLUID_TANK.get());
        h.runAfterDelay(4, () -> {
            var owner = ExtensionBoilerGameTests.owner(h); owner.selectMinimum(0);
            seed(h, 12000, 9600, 0, false);
            var port = handler(h, STEAM, Direction.WEST); var before = owner.ledger().save();
            h.assertTrue(port.getFluidInTank(0).isEmpty() && port.drain(256, IFluidHandler.FluidAction.SIMULATE).isEmpty(),
                    "默认超临界口错误声明或模拟输出普通蒸汽");
            h.assertTrue(before.equals(owner.ledger().save()), "拒收模拟改写HU或库存");
        });
        h.runAfterDelay(12, () -> {
            h.assertTrue(handler(h, STEAM.west(), Direction.EAST).getFluidInTank(0).isEmpty(), "不匹配口主动直填普通蒸汽");
            h.assertTrue(ExtensionBoilerGameTests.owner(h).ledger().steam() == 12000, "等待匹配消耗炉内蒸汽");
            submit(h, STEAM, 0); var owner = ExtensionBoilerGameTests.owner(h);
            var fluid = handler(h, STEAM, Direction.WEST).drain(128, IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(fluid.is(TurbineContent.STEAM.get()) && fluid.getAmount() == 128, "普通汽选择未放行实际普通汽");
            // 同tick被动领取128mB后，主动直填只能补3968mB；不能因控件或能力刷新再领取一整口额度。
            owner.tick();
            h.assertTrue(handler(h, STEAM.west(), Direction.EAST).getFluidInTank(0).getAmount() == 3968 && owner.ledger().steam() == 7904,
                    "主动/被动输出未共享4096mB同tick额度");
            System.out.println("[steam-selection] default SC rejects ordinary; native ordinary selection passive128+active3968=4096mB");
            h.succeed();
        });
    }
    @GameTest(template = "inventory_empty", templateNamespace = "create_nuclear_industry_boiler_inventory", timeoutTicks = 45)
    public static void nativeOptionsPersistIndependentlyAndOnlySteamHasControl(GameTestHelper h) {
        twoPorts(h);
        h.runAfterDelay(4, () -> {
            var owner = ExtensionBoilerGameTests.owner(h);
            h.assertTrue(owner.getAllBehaviours().stream().filter(b -> b instanceof ValueSettingsBehaviour).count() == 1,
                    "控制器新增了汽种控件或压力路由串扰");
            for (BlockPos p : new BlockPos[]{BASE.offset(2, 1, 0), BASE.offset(3, 0, 0), COLD})
                h.assertTrue(port(h, p).getAllBehaviours().isEmpty(), "非汽口新增了选项控件");
            h.assertTrue(port(h, STEAM).getAllBehaviours().stream().filter(b -> b instanceof ValueSettingsBehaviour).count() == 1,
                    "汽口原生选项行为未注册");
            h.assertTrue(port(h, STEAM).selectedSteamKind() == BoilerSteamKind.SUPERCRITICAL
                    && port(h, SECOND).selectedSteamKind() == BoilerSteamKind.SUPERCRITICAL, "汽口默认不是超临界");
            submit(h, STEAM, 0);
            var a = port(h, STEAM).saveWithoutMetadata(h.getLevel().registryAccess());
            var b = port(h, SECOND).saveWithoutMetadata(h.getLevel().registryAccess());
            submit(h, STEAM, 1); submit(h, SECOND, 0);
            port(h, STEAM).loadWithComponents(a, h.getLevel().registryAccess()); port(h, SECOND).loadWithComponents(b, h.getLevel().registryAccess());
            h.assertTrue(port(h, STEAM).selectedSteamKind() == BoilerSteamKind.NORMAL
                    && port(h, SECOND).selectedSteamKind() == BoilerSteamKind.SUPERCRITICAL, "两口当前保存恢复互相覆盖");
            submit(h, STEAM, -1); submit(h, STEAM, 2); submit(h, STEAM, 1, 1);
            h.assertTrue(port(h, STEAM).selectedSteamKind() == BoilerSteamKind.NORMAL, "服务端接受了非法选项/行");
            var snapshot = port(h, STEAM).getUpdateTag(h.getLevel().registryAccess());
            var client = new BoilerPortBlockEntity(h.absolutePos(STEAM), h.getBlockState(STEAM));
            client.readClient(snapshot, h.getLevel().registryAccess());
            h.assertTrue(client.selectedSteamKind() == BoilerSteamKind.NORMAL, "客户端快照未同步选项");
            h.assertTrue(Math.abs(owner.ledger().minimumPressure() - .6) < 1e-8, "选汽包改写控制器压力");
            owner.selectMinimum(100); seed(h, 12000, 9600, 0, false); port(h, STEAM).tick();
            h.assertTrue(port(h, STEAM).getUpdateTag(h.getLevel().registryAccess()).getString("SteamOutputStatus").equals("pressure"),
                    "压力下限100仍显示可出汽");
            submit(h, STEAM, 1); port(h, STEAM).tick();
            h.assertTrue(port(h, STEAM).getUpdateTag(h.getLevel().registryAccess()).getString("SteamOutputStatus").equals("no_steam"),
                    "选中SC池为空时未显示对应库存等待");
            System.out.println("[steam-selection] native netId0 options independent; normal/SC current NBT restored; invalid values rejected; client snapshot verified"); h.succeed();
        });
    }
    /** 真实原生动力夹具：可让泵直接邻接汽口，冷液则保持管-泵-管布局。 */
    private static BlockPos poweredOutput(GameTestHelper h, BlockPos port, Direction outward, boolean direct) {
        if (!port.equals(COLD)) return IndustrialFluidNetworkGameTests.outletAtRate(h, port, outward, 256);
        int offset = direct ? 1 : 2;
        BlockPos pump = port.relative(outward, offset), far = pump.relative(outward), tank = far.relative(outward);
        if (!direct) h.setBlock(port.relative(outward), AllBlocks.FLUID_PIPE.get());
        h.setBlock(pump, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, outward));
        h.setBlock(far, AllBlocks.FLUID_PIPE.get()); h.setBlock(tank, AllBlocks.FLUID_TANK.get());
        BlockPos cog = outward == Direction.EAST ? pump.south() : pump.east();
        BlockPos shaft = cog.relative(outward), motor = cog.relative(outward, 2);
        h.setBlock(cog, AllBlocks.COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, outward.getAxis()));
        h.setBlock(shaft, AllBlocks.SHAFT.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, outward.getAxis()));
        h.setBlock(motor, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, outward.getOpposite()));
        ((CreativeMotorBlockEntity) h.getBlockEntity(motor)).generatedSpeed.setValue(256);
        FluidPropagator.propagateChangedPipe(h.getLevel(), h.absolutePos(far), h.getBlockState(far));
        if (!direct) FluidPropagator.propagateChangedPipe(h.getLevel(), h.absolutePos(port.relative(outward)), h.getBlockState(port.relative(outward)));
        return tank;
    }
    @GameTest(template = "inventory_empty", templateNamespace = "create_nuclear_industry_boiler_inventory", timeoutTicks = 160)
    public static void nativeDualNetworksAndAdjacentPumpRecoverWithColdAndBackpressurePreserved(GameTestHelper h) {
        twoPorts(h);
        BlockPos pipe = STEAM.west(), tankA = poweredOutput(h, STEAM, Direction.WEST, false);
        BlockPos tankB = poweredOutput(h, SECOND, Direction.EAST, true);
        BlockPos coldTank = poweredOutput(h, COLD, Direction.NORTH, false);
        IFluidHandler[] originals = new IFluidHandler[3];
        int[] received = new int[3]; long[] gross = {16000, 0};
        int[] previousAmount = {0}; double[] previousHu = {0}; boolean[] tracking = {false};
        h.runAfterDelay(4, () -> {
            ExtensionBoilerGameTests.owner(h).selectMinimum(0); submit(h, STEAM, 0); submit(h, SECOND, 0);
            seed(h, 16000, 12800, 30000, false); previousAmount[0] = 16000; previousHu[0] = 12800; tracking[0] = true;
            originals[0] = handler(h, STEAM, Direction.WEST); originals[1] = handler(h, SECOND, Direction.EAST); originals[2] = handler(h, COLD, Direction.NORTH);
        });
        // 每tick只从真实收罐观测mB；显热散失允许最多0.9HU/t，输送必须扣实际比焓。
        h.onEachTick(() -> {
            if (!tracking[0]) return;
            var s = ExtensionBoilerGameTests.owner(h).ledger();
            int a = handler(h, tankA, Direction.EAST).getFluidInTank(0).getAmount();
            int b = handler(h, tankB, Direction.WEST).getFluidInTank(0).getAmount();
            h.assertTrue(s.steam() + s.totalVented() + a + b + gross[1] == gross[0], "真实双管路丢汽/复制：炉=" + s.steam() + ", A=" + a + ", B=" + b);
            h.assertTrue(s.cold() + handler(h, coldTank, Direction.SOUTH).getFluidInTank(0).getAmount() == 30000, "切换使真实冷液丢量");
            int n = previousAmount[0] - s.steam(); double loss = previousHu[0] - s.steamHu();
            double carried = s.steam() == 0 ? loss : n * s.steamHu() / s.steam();
            h.assertTrue(n >= 0 && loss + 1e-6 >= carried && loss <= carried + 1.1, "真实双路HU损失超出实际比焓/散热：n=" + n + ", loss=" + loss + ", carried=" + carried);
            previousAmount[0] = s.steam(); previousHu[0] = s.steamHu();
        });
        h.runAfterDelay(20, () -> {
            received[0] = handler(h, tankA, Direction.EAST).getFluidInTank(0).getAmount();
            received[1] = handler(h, tankB, Direction.WEST).getFluidInTank(0).getAmount();
            received[2] = handler(h, coldTank, Direction.SOUTH).getFluidInTank(0).getAmount();
            h.assertTrue(received[0] > 0 && received[1] > 0 && received[2] > 0
                    && ((com.iksxh.create_nuclear_industry.industrial.IndustrialPumpBlockEntity) h.getBlockEntity(SECOND.east(2))).getSpeed() != 0, "真实双管路/直邻泵/冷液尚未成交");
            submit(h, STEAM, 1);
            h.assertTrue(originals[0].getTanks() == 0 && originals[1].getTanks() == 1 && originals[2].getTanks() == 1,
                    "切换汽口A连带撤销口B或冷液句柄");
        });
        h.runAfterDelay(30, () -> {
            h.assertTrue(handler(h, tankA, Direction.EAST).getFluidInTank(0).getAmount() == received[0], "不匹配口仍送普通汽");
            h.assertTrue(handler(h, tankB, Direction.WEST).getFluidInTank(0).getAmount() > received[1] && originals[1].getTanks() == 1
                    && handler(h, coldTank, Direction.SOUTH).getFluidInTank(0).getAmount() > received[2] && originals[2].getTanks() == 1,
                    "他口或真实冷液泵在单口切换后停流");
            h.assertTrue(((com.iksxh.create_nuclear_industry.industrial.IndustrialPumpBlockEntity)h.getBlockEntity(STEAM.west(2))).actualFlow() == 0, "异种过滤仍有工业真实交付");
            var s = ExtensionBoilerGameTests.owner(h).ledger(); gross[0] += 16000 - s.steam(); seed(h, 16000, 16800, s.cold(), true);
            previousAmount[0] = 16000; previousHu[0] = 16800;
        });
        h.runAfterDelay(48, () -> {
            var a = handler(h, tankA, Direction.EAST).getFluidInTank(0);
            h.assertTrue(a.is(TurbineContent.STEAM.get()) && a.getAmount() == received[0], "当前超临界改写/清空了外部普通汽背压");
            var targetB = handler(h, tankB, Direction.WEST); gross[1] += IndustrialFluidNetworkGameTests.consumeFixtureTank(h, tankB, Integer.MAX_VALUE);
            submit(h, SECOND, 1); h.assertTrue(originals[1].getTanks() == 0 && originals[2].getTanks() == 1, "直邻泵汽口切换未撤销其原句柄或连带撤销冷液");
        });
        h.runAfterDelay(68, () -> {
            h.assertTrue(handler(h, tankB, Direction.WEST).getFluidInTank(0).is(BoilerContent.SUPERCRITICAL_STEAM.get()), "直邻专用机械泵切换后未恢复超临界输出");
            received[1] = handler(h, tankB, Direction.WEST).getFluidInTank(0).getAmount();
            // 同tick快速离开再回到超临界：第二层流体仍同种，第三层来源必须在下个tick遗忘失效句柄。
            submit(h, SECOND, 0); submit(h, SECOND, 1);
            h.assertTrue(handler(h, tankA, Direction.EAST).getFluidInTank(0).getAmount() == received[0], "异种背压不保留外部库存");
            var targetA = handler(h, tankA, Direction.EAST); gross[1] += IndustrialFluidNetworkGameTests.consumeFixtureTank(h, tankA, Integer.MAX_VALUE);
        });
        h.runAfterDelay(100, () -> {
            h.assertTrue(handler(h, tankA, Direction.EAST).getFluidInTank(0).is(BoilerContent.SUPERCRITICAL_STEAM.get())
                    && handler(h, tankA, Direction.EAST).getFluidInTank(0).getAmount() > 0, "专用管道未在排空外罐后恢复所选超临界输出");
            h.assertTrue(handler(h, tankB, Direction.WEST).getFluidInTank(0).getAmount() > received[1], "直邻泵快速选择往返后缓存失效来源停流");
            var cold = handler(h, coldTank, Direction.SOUTH).getFluidInTank(0);
            h.assertTrue(cold.is(ModFluids.COMPOUND_COOLANT_SOURCE.get()) && cold.getAmount() > received[2] && originals[2].getTanks() == 1,
                    "选择/温压变化使冷液泵停流或原句柄失效");
            System.out.println("[steam-selection] native dual pipe + adjacent pump recovered without replacement; A ordinary=" + received[0]
                    + "; SC A=" + handler(h, tankA, Direction.EAST).getFluidInTank(0).getAmount() + "; SC B=" + handler(h, tankB, Direction.WEST).getFluidInTank(0).getAmount()
                    + "; cold=" + received[2] + "->" + cold.getAmount() + "; each tick mB/HU conserved; external ordinary retained until explicit receiver drain"); h.succeed();
        });
    }
    @GameTest(template = "inventory_empty", templateNamespace = "create_nuclear_industry_boiler_inventory", timeoutTicks = 45)
    public static void perPortEpochSimulationAndCrossingDrainConserveMassAndHu(GameTestHelper h) {
        twoPorts(h);
        BlockPos receiver = IndustrialFluidNetworkGameTests.outletAtRate(h, SECOND, Direction.EAST, 256);
        h.runAfterDelay(4, () -> {
            var owner = ExtensionBoilerGameTests.owner(h); owner.selectMinimum(0); seed(h, 12000, 9600, 1234, false);
            submit(h, STEAM, 0);
            var a = handler(h, STEAM, Direction.WEST); var b = handler(h, SECOND, Direction.EAST);
            var cold = handler(h, COLD, Direction.NORTH); var water = ExtensionBoilerGameTests.water(h);
            var hot = handler(h, BASE.offset(3, 0, 0), Direction.NORTH);
            var before = owner.ledger().save(); var choices = port(h, STEAM).saveWithoutMetadata(h.getLevel().registryAccess());
            var epochs = new HashMap<>((Map<?, ?>) field(owner, "steamEpochs")); var dirty = new HashSet<>((java.util.Set<?>) field(owner, "dirtyPorts"));
            h.assertTrue(a.getFluidInTank(0).is(TurbineContent.STEAM.get()) && b.getFluidInTank(0).isEmpty(), "异选择两口错误声明同种汽");
            h.assertTrue(a.drain(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 256), IFluidHandler.FluidAction.SIMULATE).isEmpty()
                    && b.drain(256, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "异种过滤或不匹配generic抽取失效");
            for (int i = 0; i < 3; i++) h.assertTrue(a.drain(256, IFluidHandler.FluidAction.SIMULATE).getAmount() == 256, "模拟耗费同tick额度");
            h.assertTrue(before.equals(owner.ledger().save()) && choices.equals(port(h, STEAM).saveWithoutMetadata(h.getLevel().registryAccess()))
                    && epochs.equals(field(owner, "steamEpochs")) && dirty.equals(field(owner, "dirtyPorts")), "SIMULATE改变账本、选择或网络刷新状态");
            var normal = a.drain(256, IFluidHandler.FluidAction.EXECUTE);
            h.assertTrue(normal.is(TurbineContent.STEAM.get()) && normal.getAmount() == 256 && owner.ledger().steam() == 11744
                    && Math.abs(owner.ledger().steamHu() - 9395.2) < 1e-6, "普通汽实际mB/HU不守恒");
            submit(h, STEAM, 1);
            h.assertTrue(a.getTanks() == 0 && b.getTanks() == 1 && cold.getTanks() == 1 && water.getTanks() == 1 && hot.getTanks() == 1,
                    "单口切换未只撤销该口旧句柄");
            submit(h, STEAM, 0);
            h.assertTrue(handler(h, STEAM, Direction.WEST).drain(4096, IFluidHandler.FluidAction.SIMULATE).getAmount() == 3840, "来回切换重置同tick额度");
            seed(h, 9100, 9116, 1234, true); var sc = handler(h, SECOND, Direction.EAST);
            h.assertTrue(handler(h, STEAM, Direction.WEST).getFluidInTank(0).isEmpty()
                    && handler(h, STEAM, Direction.WEST).drain(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 256), IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "普通汽选择口错误降级输出实际超临界汽");
            var beforeSc = owner.ledger().save();
            h.assertTrue(sc.drain(256, IFluidHandler.FluidAction.SIMULATE).isEmpty() && sc.drain(256, IFluidHandler.FluidAction.EXECUTE).isEmpty()
                    && beforeSc.equals(owner.ledger().save()), "SC标准模拟/执行不是拒绝零写");
            h.runAfterDelay(1, () -> {
                var inventory = handler(h, receiver, Direction.WEST).getFluidInTank(0);
                h.assertTrue(inventory.is(BoilerContent.SUPERCRITICAL_STEAM.get()) && inventory.getAmount() == 256
                        && owner.ledger().steam(SUPERCRITICAL) == 8844, "合法专用泵跨炉压线未按256mB守恒交付");
                double expected = 9116D * 8844 / 9100;
                h.assertTrue(owner.ledger().steamHu(SUPERCRITICAL) <= expected + 1e-6 && owner.ledger().steamHu(SUPERCRITICAL) >= expected - .9 - 1e-6,
                        "专用SC真实比焓扣款超出精确携热与单tick散热边界");
                h.assertTrue(sc.getTanks() == 1 && sc.getFluidInTank(0).is(BoilerContent.SUPERCRITICAL_STEAM.get())
                        && sc.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "跨生产压力线撤销SC身份或公开受限抽取");
                h.assertTrue(handler(h, STEAM, Direction.WEST).getFluidInTank(0).isEmpty(), "无普通池却免费降级SC");
                System.out.println("[steam-selection] ordinary256/204.8HU; same-tick remaining3840 after toggle; standard SC zero; real dedicated SC256 crossing threshold with exact specific enthalpy plus <=0.9HU cooling"); h.succeed();
            });
        });
    }
    /** 本版本混合库存与能力/遥测快照：两池可同时交易，低压SC不改种，保存不能刷新物理口额度。 */
    @GameTest(template = "inventory_empty", templateNamespace = "create_nuclear_industry_boiler_inventory", timeoutTicks = 35)
    public static void mixedInventoriesCurrentSaveAndTooltipKeepIdentityAndBudget(GameTestHelper h) {
        twoPorts(h);
        BlockPos receiver = IndustrialFluidNetworkGameTests.outletAtRate(h, SECOND, Direction.EAST, 256);
        h.runAfterDelay(4, () -> {
            var owner = ExtensionBoilerGameTests.owner(h); owner.selectMinimum(10); submit(h, STEAM, 0);
            var tag = owner.ledger().save(); ExtensionBoilerGameTests.seedInventories(tag, 5000, 4000, 5000, 5100); owner.ledger().load(tag);
            var normal = handler(h, STEAM, Direction.WEST); var sc = handler(h, SECOND, Direction.EAST);
            h.assertTrue(owner.ledger().pressure() < owner.ledger().settings().supercriticalPressure(), "混合保存夹具未覆盖低炉压SC");
            var before = owner.ledger().save();
            h.assertTrue(normal.drain(128, IFluidHandler.FluidAction.SIMULATE).is(TurbineContent.STEAM.get())
                    && sc.drain(256, IFluidHandler.FluidAction.SIMULATE).isEmpty()
                    && before.equals(owner.ledger().save()), "混合库存模拟错误或写账");
            h.runAfterDelay(1, () -> {
            var n = normal.drain(128, IFluidHandler.FluidAction.EXECUTE); var received = handler(h, receiver, Direction.WEST).getFluidInTank(0);
            h.assertTrue(n.is(TurbineContent.STEAM.get()) && received.is(BoilerContent.SUPERCRITICAL_STEAM.get()) && received.getAmount() == 256
                    && owner.ledger().steam(NORMAL) == 4872 && owner.ledger().steam(SUPERCRITICAL) == 4744
                    && Math.abs(owner.ledger().steamHu(NORMAL) - 3897.6) < 1e-6
                    && owner.ledger().steamHu(SUPERCRITICAL) <= 4838.88 + 1e-6 && owner.ledger().steamHu(SUPERCRITICAL) >= 4838.88 - .9 - 1e-6, "混合两池真实交易跨种/携热串账");
            var portable = owner.savePortableData(); owner.loadPortableData(portable);
            h.assertTrue(portable.equals(owner.savePortableData()) && normal.getTanks() == 1 && sc.getTanks() == 1
                    && sc.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty()
                    && normal.drain(10000, IFluidHandler.FluidAction.SIMULATE).getAmount() == 3968, "当前保存改变身份、旧句柄或同tick额度");
            var d = owner.ledger().settings();
            owner.ledger().setSettings(new com.iksxh.create_nuclear_industry.boiler.BoilerState.Settings(d.minDimension(), d.maxDimension(),
                    d.waterCapacityPerCellMb(), d.steamCapacityPerCellMb(), d.coolantPortFlowMbPerTick(), d.waterSteamPortFlowMbPerTick(), d.pairHeatHuPerTick(),
                    d.boilingTemperature(), d.supercriticalTemperature(), d.wallHeatCapacityHuPerWaterCell(), d.waterSpecificHeatHuPerMb(),
                    .4, d.vaporizationLatentHeatHuPerMb(), d.supercriticalPressure(), d.outputMinPressure(), d.valveOpenPressure(),
                    d.valveClosePressure(), d.valveFlowPerSteamCellMbPerTick(), d.idleWaterCoolingHuPerCellPerTick(), d.idleSteamCoolingHuPerCellPerTick()));
            // 只更改本测试实体的配置快照；SC当前1.02HU/mB在cpSteam=0.4时是1.55，客户端默认cp=0.2不能重算成2.10。
            var mirror = new com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity(owner.getBlockPos(), owner.getBlockState());
            mirror.handleUpdateTag(owner.getUpdateTag(h.getLevel().registryAccess()), h.getLevel().registryAccess());
            var tooltip = new java.util.ArrayList<net.minecraft.network.chat.Component>(); mirror.addToGoggleTooltip(tooltip, false);
            var inventoryLine = tooltip.stream().map(net.minecraft.network.chat.Component::getContents)
                    .filter(c -> c instanceof net.minecraft.network.chat.contents.TranslatableContents t
                            && t.getKey().equals("gui.create_nuclear_industry.boiler.steam_inventories"))
                    .map(c -> (net.minecraft.network.chat.contents.TranslatableContents) c).findFirst().orElseThrow();
            h.assertTrue(((Number) inventoryLine.getArgs()[0]).intValue() == 4872 && ((Number) inventoryLine.getArgs()[1]).intValue() == 4744
                    && mirror.ledger().save().equals(owner.ledger().save()), "客户端只读快照复制/重标两种库存或护目镜未分别显示");
            var temperatureLine = tooltip.stream().map(net.minecraft.network.chat.Component::getContents)
                    .filter(c -> c instanceof net.minecraft.network.chat.contents.TranslatableContents t
                            && t.getKey().equals("gui.create_nuclear_industry.boiler.steam_inventory_temperatures"))
                    .map(c -> (net.minecraft.network.chat.contents.TranslatableContents) c).findFirst().orElseThrow();
            h.assertTrue(temperatureLine.getArgs()[0].equals("1.00") && temperatureLine.getArgs()[1].equals("1.55"),
                    "客户端分池汽温没有使用服务端自定义比热的实际快照");
            h.setBlock(BASE, net.minecraft.world.level.block.Blocks.AIR); owner.invalidateForm();
            h.assertTrue(normal.getTanks() == 0 && sc.getTanks() == 0, "真实拆件没有撤销两种旧句柄");
            System.out.println("[dual-inventory] mixed N128@0.8HU dedicated SC256@1.02HU below Psc; current NBT/budgets + client tooltip restored; server cpSteam0.4 temperatures=1.00/1.55; dismantling invalidated both");
            h.succeed();
            });
        });
    }
    /** 冷SC始终保留流体身份，只有真实热液转冷付款足够后，原句柄才可交付同一库存。 */
    @GameTest(template = "inventory_empty", templateNamespace = "create_nuclear_industry_boiler_inventory", timeoutTicks = 45)
    public static void coldScKeepsIdentityAndReheatsFromActualHotFluid(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 5, 5, 5, 2);
        BlockPos receiver = IndustrialFluidNetworkGameTests.outletAtRate(h, STEAM, Direction.WEST, 128);
        IFluidHandler[] original = {null}; boolean[] active = {false}; long[] coldRemoved = {0};
        h.runAfterDelay(4, () -> {
            var owner = ExtensionBoilerGameTests.owner(h); owner.selectMinimum(0); seed(h, 1000, 900, 0, true);
            original[0] = handler(h, STEAM, Direction.WEST); var before = owner.ledger().save();
            h.assertTrue(original[0].getFluidInTank(0).is(BoilerContent.SUPERCRITICAL_STEAM.get())
                    && original[0].drain(256, IFluidHandler.FluidAction.SIMULATE).isEmpty()
                    && original[0].drain(256, IFluidHandler.FluidAction.EXECUTE).isEmpty()
                    && before.equals(owner.ledger().save()), "欠热SC错误免费交付或被降级");
            active[0] = true;
        });
        h.onEachTick(() -> {
            if (!active[0]) return;
            IndustrialFluidNetworkGameTests.feed(h, BASE.offset(3, 0, 0), Direction.NORTH, ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 36);
            coldRemoved[0] += handler(h, COLD, Direction.NORTH).drain(256, IFluidHandler.FluidAction.EXECUTE).getAmount();
        });
        Runnable[] detach = {() -> {}};
        double[] before = {0}, specific = {0}; boolean[] ready = {false};
        java.util.function.Consumer<net.neoforged.neoforge.event.tick.LevelTickEvent.Post> capture = event -> {
            if (event.getLevel() != h.getLevel() || !active[0]) return;
            var ledger = ExtensionBoilerGameTests.owner(h).ledger();
            if (!ledger.outputQualified(SUPERCRITICAL)) return;
            before[0] = ledger.totalHu(); specific[0] = ledger.steamHu(SUPERCRITICAL) / ledger.steam(SUPERCRITICAL); ready[0] = true;
        };
        java.util.function.Consumer<net.neoforged.neoforge.event.tick.LevelTickEvent.Post> verify = event -> {
            if (event.getLevel() != h.getLevel() || !ready[0]) return;
            var ledger = ExtensionBoilerGameTests.owner(h).ledger(); var fluid = handler(h, receiver, Direction.EAST).getFluidInTank(0);
            h.assertTrue(fluid.is(BoilerContent.SUPERCRITICAL_STEAM.get()) && fluid.getAmount() == 128 && ledger.steam(NORMAL) == 0 && ledger.steam(SUPERCRITICAL) == 872
                    && Math.abs(before[0] - ledger.totalHu() - 128 * specific[0]) < 1e-6, "再热后专用真实交付不是128mB及实际比焓扣款");
            double lost = 900 + .5 * (coldRemoved[0] + ledger.cold()) - before[0];
            h.assertTrue(lost >= -1e-6 && lost <= h.getTick() * .9 + 1e-6 && original[0].drain(128, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "再热付款/HU不守恒或旧标准SC句柄禁运失效");
            active[0] = false; ready[0] = false; detach[0].run();
            System.out.println("[dual-inventory] cold SC1000@900HU waits; real hot-to-cold reheats; dedicated128 carries actual specific HU; standard original handler stays restricted"); h.succeed();
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, capture);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, verify);
        detach[0] = () -> { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(capture); net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(verify); };
        h.runAfterDelay(40, detach[0]);
    }
}
