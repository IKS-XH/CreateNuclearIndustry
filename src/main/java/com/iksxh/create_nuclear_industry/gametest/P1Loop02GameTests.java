package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.ReactorCoolantFluidHandler;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

/** 验证冷/热/换料端口按照结构缓存绑定到共享账本或唯一燃料列。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Loop02GameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos COLD_PORT = new BlockPos(1, 2, 4);
    private static final BlockPos HOT_PORT = new BlockPos(3, 2, 4);
    private static final BlockPos REFUELING_PORT = new BlockPos(1, 4, 1);
    private static final BlockPos EXTRA_COLD_PORT = new BlockPos(2, 1, 4);
    private static final BlockPos EXTRA_HOT_PORT = new BlockPos(2, 3, 4);
    private static final BlockPos FORMATION_GAP = new BlockPos(0, 0, 0);

    private P1Loop02GameTests() {
    }

    /** 验证冷/热端口共享仪表端口账本，换料端口分别绑定到唯一燃料列。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void portsBindToSharedLedgerAndUniqueFuelColumns(GameTestHelper helper) {
        Map<ReactorStructureDefinition.LocalPosition, String> layout =
                new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        layout.put(new ReactorStructureDefinition.LocalPosition(
                EXTRA_COLD_PORT.getX(), EXTRA_COLD_PORT.getY(), EXTRA_COLD_PORT.getZ()),
                "create_nuclear_industry:reactor_cold_port");
        layout.put(new ReactorStructureDefinition.LocalPosition(
                EXTRA_HOT_PORT.getX(), EXTRA_HOT_PORT.getY(), EXTRA_HOT_PORT.getZ()),
                "create_nuclear_industry:reactor_hot_port");
        buildStructure(helper, layout);

        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "结构未成型，无法建立端口绑定");

            ReactorPortBlockEntity cold = port(helper, COLD_PORT);
            ReactorPortBlockEntity extraCold = port(helper, EXTRA_COLD_PORT);
            ReactorPortBlockEntity hot = port(helper, HOT_PORT);
            ReactorPortBlockEntity extraHot = port(helper, EXTRA_HOT_PORT);
            ReactorPortBlockEntity refueling = port(helper, REFUELING_PORT);

            require(helper, cold.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.COLD_COOLANT, null),
                    "冷端口未绑定到仪表端口的共享账本");
            require(helper, extraCold.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.COLD_COOLANT, null),
                    "第二个冷端口未绑定到仪表端口的共享账本");
            require(helper, hot.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.HOT_COOLANT, null),
                    "热端口未绑定到仪表端口的共享账本");
            require(helper, extraHot.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.HOT_COOLANT, null),
                    "第二个热端口未绑定到仪表端口的共享账本");
            require(helper, cold.binding().usesGlobalLedger()
                            && hot.binding().usesGlobalLedger(),
                    "冷/热端口错误地绑定到了单列状态");

            CoreColumnPosition expectedColumn = new CoreColumnPosition(0, 0);
            require(helper, refueling.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.REFUELING, expectedColumn),
                    "换料端口未绑定到其正下方燃料列");
            require(helper, !refueling.binding().usesGlobalLedger(),
                    "换料端口错误地访问了全堆账本");
            require(helper, instrument.boundPorts(ReactorPortBlockEntity.BindingType.COLD_COOLANT).size() == 2,
                    "冷端口绑定数量与结构映射不一致");
            require(helper, instrument.boundPorts(ReactorPortBlockEntity.BindingType.HOT_COOLANT).size() == 2,
                    "热端口绑定数量与结构映射不一致");
            require(helper, instrument.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING).size() == 8,
                    "换料端口绑定数量与燃料列数量不一致");
            require(helper, instrument.boundPortCount() == 12,
                    "端口重扫后出现重复绑定或遗漏绑定");

            ReactorCoolantFluidHandler firstCold = ReactorCoolantFluidHandler.forPort(cold);
            ReactorCoolantFluidHandler secondCold = ReactorCoolantFluidHandler.forPort(extraCold);
            ReactorCoolantFluidHandler firstHot = ReactorCoolantFluidHandler.forPort(hot);
            require(helper, firstCold != null && secondCold != null && firstHot != null,
                    "已绑定冷/热端口未提供流体 capability");
            require(helper, firstCold.owner() == instrument
                            && secondCold.owner() == instrument
                            && firstHot.owner() == instrument,
                    "多个端口没有共享同一个仪表端口状态所有者");

            int configuredLimit = Math.max(0, P1ServerConfig.VALUES.perPortFlowMbPerTick.get());
            long coldCapacity = Math.max(0L, P1ServerConfig.VALUES.coldInventoryCapacityMb.get().longValue());
            int request = Math.max(200, configuredLimit);
            int firstAccepted = firstCold.fill(
                    new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), request),
                    IFluidHandler.FluidAction.EXECUTE);
            int secondAccepted = secondCold.fill(
                    new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), request),
                    IFluidHandler.FluidAction.EXECUTE);
            int expectedFirst = (int) Math.min(coldCapacity, configuredLimit);
            int expectedSecond = (int) Math.min(
                    Math.max(0L, coldCapacity - expectedFirst), configuredLimit);
            require(helper, firstAccepted == expectedFirst && secondAccepted == expectedSecond,
                    "多个冷端口没有分别应用单端口流量上限：" + firstAccepted + "/" + secondAccepted
                            + "，配置值为" + configuredLimit);
            require(helper, instrument.snapshot().coldCoolantMb()
                            == (long) firstAccepted + secondAccepted,
                    "多个冷端口没有把流体写入同一共享冷库存");
            helper.succeed();
        });
    }

    /** 验证重复重扫不累加绑定，结构失效清除绑定，修复后重新建立绑定。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void portBindingsClearOnInvalidationAndReturnAfterRescan(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity hot = port(helper, HOT_PORT);
            require(helper, instrument.structureValid() && hot.isBound(),
                    "标准结构初始端口绑定未建立");
            int initialBindingCount = instrument.boundPortCount();

            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(INSTRUMENT));
            require(helper, instrument.boundPortCount() == initialBindingCount,
                    "重复结构重扫累加了端口绑定");

            helper.setBlock(COLD_PORT, Blocks.AIR.defaultBlockState());
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(COLD_PORT));
            require(helper, !instrument.structureValid(), "冷端口拆除后结构仍被判定为有效");
            require(helper, instrument.boundPortCount() == 0,
                    "结构失效后仍保留端口绑定缓存");
            require(helper, !hot.isBound(),
                    "结构失效后剩余热端口仍可访问共享账本");

            helper.setBlock(COLD_PORT, P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState());
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(COLD_PORT));
            ReactorPortBlockEntity restoredCold = port(helper, COLD_PORT);
            require(helper, instrument.structureValid(), "冷端口补回后结构未重新成型");
            require(helper, restoredCold.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.COLD_COOLANT, null),
                    "结构重扫后冷端口未恢复绑定");
            require(helper, instrument.boundPortCount() == initialBindingCount,
                    "结构重扫后端口绑定数量错误");
            helper.succeed();
        });
    }

    /** 验证错位的端口和重复仪表端口使结构无效，且不会留下可用绑定。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void misplacedOrDuplicatePortsAreRejectedWithoutBinding(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity hot = port(helper, HOT_PORT);
            require(helper, instrument.structureValid() && hot.isBound(),
                    "标准结构初始状态不满足端口拒绝测试");

            helper.setBlock(REFUELING_PORT, P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState());
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(REFUELING_PORT));
            require(helper, !instrument.structureValid(), "错位冷端口错误地使结构保持有效");
            require(helper, instrument.boundPortCount() == 0,
                    "错位端口导致结构失效后仍保留绑定");

            helper.setBlock(REFUELING_PORT, P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState());
            helper.setBlock(new BlockPos(0, 2, 2), P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(INSTRUMENT));
            require(helper, !instrument.structureValid(), "重复仪表端口错误地通过结构扫描");
            require(helper, instrument.boundPortCount() == 0,
                    "重复仪表端口被错误绑定到共享账本");
            helper.succeed();
        });
    }

    /** 验证世界 capability 缓存会随端口绑定建立、失效和恢复而更新。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void capabilityCacheFollowsBindingLifecycle(GameTestHelper helper) {
        Map<ReactorStructureDefinition.LocalPosition, String> layout =
                new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        layout.put(new ReactorStructureDefinition.LocalPosition(
                FORMATION_GAP.getX(), FORMATION_GAP.getY(), FORMATION_GAP.getZ()),
                "minecraft:air");
        buildStructure(helper, layout);

        helper.runAfterDelay(3, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, !instrument.structureValid(), "缺少结构方块时错误地完成了结构成型");

            BlockCapabilityCache<IFluidHandler, Direction> capabilityCache =
                    BlockCapabilityCache.create(
                            Capabilities.FluidHandler.BLOCK,
                            helper.getLevel(),
                            helper.absolutePos(COLD_PORT),
                            Direction.UP);
            require(helper, capabilityCache.getCapability() == null,
                    "结构未成型时世界 capability 缓存不应提供流体处理器");

            helper.setBlock(FORMATION_GAP,
                    P1Blocks.REACTOR_CASING.get().defaultBlockState());
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(INSTRUMENT));
            require(helper, instrument.structureValid(), "补齐结构后未完成重扫成型");

            IFluidHandler cachedHandler = capabilityCache.getCapability();
            require(helper, cachedHandler != null,
                    "结构成型后 capability 缓存未因绑定建立而失效并重新查询");
            long coldBeforeInvalidation = instrument.snapshot().coldCoolantMb();

            helper.setBlock(FORMATION_GAP, Blocks.AIR.defaultBlockState());
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(FORMATION_GAP));
            require(helper, !instrument.structureValid(), "结构失效后仍被判定为有效");
            require(helper, capabilityCache.getCapability() == null,
                    "结构失效后 capability 缓存仍持有旧流体处理器");

            int staleFill = cachedHandler.fill(
                    new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(),
                            Math.max(1, P1ServerConfig.VALUES.perPortFlowMbPerTick.get())),
                    IFluidHandler.FluidAction.EXECUTE);
            require(helper, staleFill == 0
                            && instrument.snapshot().coldCoolantMb() == coldBeforeInvalidation,
                    "结构失效后旧流体处理器仍可修改共享库存");

            helper.setBlock(FORMATION_GAP,
                    P1Blocks.REACTOR_CASING.get().defaultBlockState());
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(FORMATION_GAP));
            require(helper, instrument.structureValid(), "修复结构后重扫未恢复有效状态");
            require(helper, capabilityCache.getCapability() != null,
                    "修复重扫后 capability 未重新出现");
            helper.succeed();
        });
    }

    /** 按结构契约显式放置方块，端口映射始终来自同一份模板。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        buildStructure(helper, ReactorStructureDefinition.canonicalTemplate());
    }

    /** 将给定的完整结构布局放入 GameTest 世界。 */
    private static void buildStructure(
            GameTestHelper helper,
            Map<ReactorStructureDefinition.LocalPosition, String> layout
    ) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry : layout.entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 读取指定局部坐标的端口实体，缺失时让 GameTest 明确失败。 */
    private static ReactorPortBlockEntity port(GameTestHelper helper, BlockPos position) {
        var blockEntity = helper.getBlockEntity(position);
        require(helper, blockEntity instanceof ReactorPortBlockEntity,
                "指定坐标没有反应堆端口方块实体：" + position);
        return (ReactorPortBlockEntity) blockEntity;
    }

    /** 读取固定局部坐标的仪表端口方块实体。 */
    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "固定坐标没有仪表端口方块实体");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    /** 将模板中的正式方块 ID 映射到已注册方块。 */
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

    /** 将断言失败统一交给 GameTest，避免异步回调静默结束。 */
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
