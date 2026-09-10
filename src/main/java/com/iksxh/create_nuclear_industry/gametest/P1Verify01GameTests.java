package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.control.ControlRodScramStatus;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderService;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnFissionResult;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.FuelRefuelingArmInteractionPoint;
import com.iksxh.create_nuclear_industry.reactor.FuelRefuelingTransaction;
import com.iksxh.create_nuclear_industry.reactor.ReactorFissionCalculator;
import com.iksxh.create_nuclear_industry.reactor.ReactorFissionResult;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentTelemetry;
import com.iksxh.create_nuclear_industry.reactor.ReactorMeltdownEvent;
import com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * P1-VERIFY-01 的四类跨系统总回归，只新增 required GameTest，不修改生产行为。
 *
 * <p>四个用例分别串接：冷启动—受控运行—遥测与损伤倍率对照；三行 F-C-F 反馈簇的
 * 运行、真实红石 SCRAM、幂等与恢复及部分卡死；Create 机械臂换料、两类钢板维修与
 * 仪表/端口 NBT 保存重载；危险拆除单次事件占位与完全停机清空重组成型的互斥。</p>
 *
 * <p>所有用例都通过正式结构扫描、换料端口事务、流体 capability、服务端滑块/红石、
 * 正式方块实体保存入口或真实 {@link BlockEvent.BreakEvent} 驱动，不直接伪造最终
 * 状态。仪表端口在服务端有正式 ticker，因此“记录基线—推进—断言”必须放在同一个
 * 回调内完成，跨 tick 轮询阶段只观察状态不比较精确数值。</p>
 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Verify01GameTests {
    /** 复用既有空结构模板，避免新增重复 NBT。 */
    private static final String TEMPLATE = "p0_probe_empty";
    /** 结构局部坐标契约中的唯一仪表端口、默认冷端口以及破坏用的外壳/观察窗。 */
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos COLD = new BlockPos(1, 2, 4);
    private static final BlockPos CASING = new BlockPos(0, 0, 0);
    private static final BlockPos WINDOW = new BlockPos(0, 2, 2);
    /** 红石方块的放置位置位于结构北面外侧，只驱动仪表端口邻居更新。 */
    private static final BlockPos REDSTONE_SOURCE = new BlockPos(2, 2, -1);
    /** 场景一受损对照列的统一完整度，用于按配置终点线性推导期望倍率。 */
    private static final double DAMAGED_INTEGRITY = 0.5D;
    /** 场景三的燃料列与控制棒列坐标及其顶部方块位置。 */
    private static final CoreColumnPosition FUEL_COLUMN = new CoreColumnPosition(0, 0);
    private static final CoreColumnPosition CONTROL_COLUMN = new CoreColumnPosition(1, 0);
    private static final BlockPos FUEL_PORT = new BlockPos(1, 4, 1);
    private static final BlockPos DRIVE = new BlockPos(2, 4, 1);
    private static final BlockPos ARM_POS = new BlockPos(1, 5, 1);
    /** 每次钢板维修固定恢复 0.25 / internalHeight 的完整度。 */
    private static final double REPAIR_AMOUNT = 0.25D / ReactorSnapshot.INTERNAL_HEIGHT;
    /** 结构成型与状态轮询的等待上限，单位为服务端 tick。 */
    private static final long FORMATION_DEADLINE_TICKS = 40L;
    private static final long POLL_DEADLINE_TICKS = 12L;

    private P1Verify01GameTests() {
    }

    /**
     * 场景一：冷启动—受控运行—遥测。两座相同 F-C-F 结构真实扫描成型，通过正式换料
     * 端口装入燃料，通过正式流体 capability 接受冷态复合冷却剂，以服务端滑块建立
     * 50% 非零功率并执行正式 tick；同时断言燃料耐久下降、冷库存减少、热库存增加且
     * 与转化量守恒、仪表与列级遥测来自同一 tick，并在相同布局与深度下对照完好/受损
     * 列，证明默认损伤配置下燃耗倍率增长快于产热倍率。
     *
     * <p>跨越子系统：结构扫描/生命周期、换料端口事务、流体 capability、滑块服务、
     * 正式服务端 tick、仪表与端口遥测同步。</p>
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 260)
    public static void coldStartControlledRunTelemetryAndDamageBurnGrowth(GameTestHelper helper) {
        BlockPos originA = new BlockPos(0, 0, 0);
        BlockPos originB = new BlockPos(5, 0, 0);
        buildFcfStructure(helper, originA);
        buildFcfStructure(helper, originB);
        long startTick = helper.getLevel().getGameTime();
        int[] phase = {0};
        helper.succeedWhen(() -> {
            if (phase[0] == 0) {
                ReactorInstrumentPortBlockEntity a = instrument(helper, originA);
                ReactorInstrumentPortBlockEntity b = instrument(helper, originB);
                if (!a.structureValid() || !b.structureValid()) {
                    if (helper.getLevel().getGameTime() - startTick > FORMATION_DEADLINE_TICKS) {
                        helper.fail("冷启动总回归的两座 F-C-F 结构没有在期限内成型");
                    }
                    return;
                }
                // 基线、推进与断言必须在同一回调内完成，避免正式服务端 ticker 跨 tick 推进状态。
                loadFuel(helper, originA, 0, "VERIFY01-A");
                loadFuel(helper, originB, 0, "VERIFY01-B");
                for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                    commitRod(helper, originA, z, 50);
                    commitRod(helper, originB, z, 50);
                }
                require(helper, a.tickControlRods(), "完好结构没有完成控制棒目标应用");
                require(helper, b.tickControlRods(), "受损对照结构没有完成控制棒目标应用");

                ReactorFissionResult fissionA = ReactorFissionCalculator.calculate(
                        a.snapshot(), ReactorSimulationParameters.defaults());
                require(helper, fissionA.generatedHeatHu() > 0.0D
                                && fissionA.plannedFuelBurnUnits() > 0.0D,
                        "50% 控制深度没有建立非零裂变热与燃耗");
                // 受损对照：先写入统一 0.5 完整度，再读取端口物品重新投影，保持同一布局与深度。
                TreeMap<CoreColumnPosition, FuelColumnState> damagedFuel = new TreeMap<>();
                for (Map.Entry<CoreColumnPosition, FuelColumnState> entry
                        : b.snapshot().fuelColumns().entrySet()) {
                    damagedFuel.put(entry.getKey(), entry.getValue().withIntegrity(DAMAGED_INTEGRITY));
                }
                b.setSnapshot(b.snapshot().withColumns(damagedFuel, b.snapshot().controlRodColumns()));

                P1ServerConfig.DamageMultipliers multipliers = P1ServerConfig.damageMultipliers();
                double expectedHeatMultiplier = 1.0D + (1.0D - DAMAGED_INTEGRITY)
                        * (multipliers.heatMultiplier() - 1.0D);
                double expectedBurnMultiplier = 1.0D + (1.0D - DAMAGED_INTEGRITY)
                        * (multipliers.burnMultiplier() - 1.0D);
                require(helper, expectedBurnMultiplier > expectedHeatMultiplier,
                        "默认损伤配置的燃耗终点没有快于产热终点");
                ReactorFissionResult damagedFission = ReactorFissionCalculator.calculate(
                        b.snapshot(), ReactorSimulationParameters.defaults());
                FuelColumnFissionResult damagedColumn =
                        damagedFission.columns().get(new CoreColumnPosition(0, 0));
                require(helper, close(damagedColumn.damageHeatMultiplier(), expectedHeatMultiplier),
                        "受损列的产热倍率不符合线性损伤曲线");
                require(helper, close(damagedColumn.damageBurnMultiplier(), expectedBurnMultiplier),
                        "受损列的燃耗倍率不符合线性损伤曲线");

                int coldAcceptedA = fillCold(helper, originA, 128);
                int coldAcceptedB = fillCold(helper, originB, 128);
                require(helper, coldAcceptedA == 128 && coldAcceptedB == 128,
                        "正式冷端 capability 没有接受 128 mB 冷态复合冷却剂");
                long coldBeforeA = a.snapshot().coldCoolantMb();
                long hotBeforeA = a.snapshot().hotCoolantMb();
                long coldBeforeB = b.snapshot().coldCoolantMb();
                long hotBeforeB = b.snapshot().hotCoolantMb();
                int damageBeforeA = fuelDamageSum(helper, originA);
                int damageBeforeB = fuelDamageSum(helper, originB);

                require(helper, a.tickReactor(), "完好结构的正式 tick 没有产生状态变化");
                require(helper, b.tickReactor(), "受损结构的正式 tick 没有产生状态变化");

                ReactorInstrumentTelemetry telemetryA = a.telemetry();
                require(helper, telemetryA.available(), "完好结构的正式 tick 没有发布遥测");
                double totalHeatA = telemetryA.totalGeneratedFissionHeatHuPerTick();
                require(helper, totalHeatA > 0.0D, "完好结构遥测没有非零新生裂变热");
                double columnSumA = telemetryA.fuelColumns().stream()
                        .mapToDouble(ReactorInstrumentTelemetry.FuelColumnTelemetry
                                ::generatedFissionHeatHuPerTick)
                        .sum();
                require(helper, close(columnSumA, totalHeatA),
                        "仪表全堆遥测与逐列遥测之和不是同一 tick 数值");
                require(helper, telemetryA.fuelColumns().size() == 6,
                        "完好结构遥测没有覆盖六根燃料列");
                long coldAfterA = a.snapshot().coldCoolantMb();
                long hotAfterA = a.snapshot().hotCoolantMb();
                double convertedA = telemetryA.convertedCoolantMbPerTick();
                require(helper, coldAfterA < coldBeforeA, "正式 tick 没有消耗冷态库存");
                require(helper, hotAfterA > hotBeforeA, "正式 tick 没有增加热态库存");
                require(helper, close(convertedA, (double) (coldBeforeA - coldAfterA))
                                && close(convertedA, (double) (hotAfterA - hotBeforeA)),
                        "遥测转化量与冷/热库存变化不守恒");
                require(helper, fuelDamageSum(helper, originA) > damageBeforeA,
                        "正式 tick 没有使燃料耐久下降");
                // 完好列冷却充足时完整度保持不变；受损列同样被冷却覆盖，完整度也不继续下降。
                require(helper, a.snapshot().fuelColumns().values().stream()
                                .allMatch(column -> close(column.integrity(), 1.0D)),
                        "冷却充足的完好列完整度被错误损伤");
                require(helper, b.snapshot().fuelColumns().values().stream()
                                .allMatch(column -> close(column.integrity(), DAMAGED_INTEGRITY)),
                        "冷却充足的受损对照列完整度被错误继续损伤");
                // 每个换料端口的客户端遥测必须与仪表端口同一 tick 的列级数值一致。
                for (Map.Entry<CoreColumnPosition, ReactorInstrumentTelemetry.FuelColumnTelemetry> column
                        : telemetryA.fuelColumns().stream().collect(java.util.stream.Collectors
                        .toMap(ReactorInstrumentTelemetry.FuelColumnTelemetry::position,
                                columnValue -> columnValue)).entrySet()) {
                    ReactorPortBlockEntity port = refuelingPort(helper, originA, column.getKey());
                    CompoundTag update = port.getUpdateTag(helper.getLevel().registryAccess());
                    require(helper, update.getBoolean("FuelColumnBound"),
                            "换料端口更新包没有携带列绑定标记");
                    require(helper, close(update.getDouble("FuelColumnHeatHuPerTick"),
                                    column.getValue().generatedFissionHeatHuPerTick()),
                            "换料端口遥测与仪表遥测不是同一 tick 数值");
                    port.handleUpdateTag(update, helper.getLevel().registryAccess());
                    require(helper, port.clientFuelColumnTelemetry() != null
                                    && close(port.clientFuelColumnTelemetry()
                                    .generatedFissionHeatHuPerTick(),
                                    column.getValue().generatedFissionHeatHuPerTick()),
                            "换料端口客户端遥测没有同步同 tick 列级数值");
                }

                ReactorInstrumentTelemetry telemetryB = b.telemetry();
                require(helper, telemetryB.available(), "受损对照结构的正式 tick 没有发布遥测");
                require(helper, telemetryB.totalGeneratedFissionHeatHuPerTick() > 0.0D
                                && fuelDamageSum(helper, originB) > damageBeforeB,
                        "受损对照结构没有非零新生热或没有消耗燃料耐久");
                double heatRatio = telemetryB.totalGeneratedFissionHeatHuPerTick() / totalHeatA;
                double burnRatio = damagedFission.plannedFuelBurnUnits()
                        / fissionA.plannedFuelBurnUnits();
                require(helper, close(heatRatio, expectedHeatMultiplier),
                        "受损/完好总产热比不符合线性损伤倍率：实际=" + heatRatio
                                + "，期望=" + expectedHeatMultiplier);
                require(helper, close(burnRatio, expectedBurnMultiplier),
                        "受损/完好总燃耗比不符合线性损伤倍率：实际=" + burnRatio
                                + "，期望=" + expectedBurnMultiplier);
                require(helper, burnRatio > heatRatio,
                        "默认损伤配置下燃耗倍率没有快于产热倍率增长");
                helper.succeed();
            }
        });
    }

    /**
     * 场景二：运行—SCRAM—恢复。真实三行 F-C-F 六燃料布局先证明反馈簇产生裂变热并
     * 消耗燃料，再由仪表端口旁真实红石方块的高电平触发 SCRAM；覆盖完全插入停热、
     * 持续高电平幂等、低电平恢复停堆前目标，以及部分卡死棒的 SCRAM_INCOMPLETE。
     *
     * <p>跨越子系统：结构扫描/生命周期、换料端口事务、滑块服务、红石邻居更新与
     * SCRAM 服务、正式服务端 tick、遥测。</p>
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void fcfFeedbackRunScramRestoreAndPartialJam(GameTestHelper helper) {
        BlockPos origin = BlockPos.ZERO;
        buildFcfStructure(helper, origin);
        long startTick = helper.getLevel().getGameTime();
        int[] phase = {0};
        long[] phaseTick = {startTick};
        int[] damageBeforeScram = {0};
        ReactorSnapshot[] lockedSnapshot = {null};
        helper.succeedWhen(() -> {
            if (phase[0] == 0) {
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
                if (!instrument.structureValid()) {
                    if (helper.getLevel().getGameTime() - startTick > FORMATION_DEADLINE_TICKS) {
                        helper.fail("SCRAM 总回归的 F-C-F 结构没有在期限内成型");
                    }
                    return;
                }
                loadFuel(helper, origin, 0, "VERIFY01-FCF");
                for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                    commitRod(helper, origin, z, 0);
                }
                require(helper, instrument.tickControlRods(), "控制棒没有抽到 0% 目标");
                damageBeforeScram[0] = fuelDamageSum(helper, origin);
                require(helper, instrument.tickReactor(), "反馈簇的正式 tick 没有状态变化");
                require(helper, instrument.telemetry().available()
                                && instrument.telemetry().totalGeneratedFissionHeatHuPerTick() > 0.0D,
                        "0% 深度的 F-C-F 反馈簇没有产生新生裂变热");
                require(helper, fuelDamageSum(helper, origin) > damageBeforeScram[0],
                        "反馈簇运行期间燃料耐久没有下降");
                helper.setBlock(REDSTONE_SOURCE, Blocks.REDSTONE_BLOCK.defaultBlockState());
                phase[0] = 1;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }
            if (phase[0] == 1) {
                // 等待真实红石邻居更新触发 SCRAM；放置时邻居通知是同步的，轮询只做兜底。
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
                if (!instrument.snapshot().scramRequested()) {
                    if (helper.getLevel().getGameTime() - phaseTick[0] > POLL_DEADLINE_TICKS) {
                        helper.fail("真实红石高电平没有触发 SCRAM 请求");
                    }
                    return;
                }
                for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                    CoreColumnPosition column = new CoreColumnPosition(1, z);
                    ControlRodColumnState rod =
                            instrument.snapshot().controlRodColumns().get(column);
                    require(helper, rod != null && rod.targetDepth() == 1.0D
                                    && rod.actualDepth() == 1.0D,
                            "SCRAM 没有把第 " + z + " 根可动棒完全插入");
                    require(helper, close(instrument.snapshot().scramSavedTargetDepths()
                                    .getOrDefault(column, -1.0D), 0.0D),
                            "SCRAM 没有保存第 " + z + " 根棒的停堆前目标");
                }
                lockedSnapshot[0] = instrument.snapshot();
                phase[0] = 2;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }
            if (phase[0] == 2) {
                // 持续高电平幂等：等待数 tick 后 SCRAM 状态与快照不得漂移。
                if (helper.getLevel().getGameTime() - phaseTick[0] < 3L) {
                    return;
                }
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
                require(helper, instrument.snapshot().scramRequested(),
                        "持续高电平期间 SCRAM 请求被清除");
                require(helper, instrument.snapshot().equals(lockedSnapshot[0]),
                        "持续高电平期间权威快照发生漂移");
                int damageBefore = fuelDamageSum(helper, origin);
                instrument.tickReactor();
                require(helper, instrument.telemetry().available()
                                && instrument.telemetry()
                                .totalGeneratedFissionHeatHuPerTick() == 0.0D,
                        "完全插入后正式 tick 仍有新生裂变热");
                require(helper, fuelDamageSum(helper, origin) == damageBefore,
                        "完全插入后正式 tick 仍消耗燃料耐久");
                helper.setBlock(REDSTONE_SOURCE, Blocks.AIR.defaultBlockState());
                phase[0] = 3;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }
            if (phase[0] == 3) {
                // 低电平释放后等待恢复，并等待正式 ticker 把实际深度抽回 0%。
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
                if (instrument.snapshot().scramRequested()) {
                    if (helper.getLevel().getGameTime() - phaseTick[0] > POLL_DEADLINE_TICKS) {
                        helper.fail("红石低电平后 SCRAM 没有释放");
                    }
                    return;
                }
                require(helper, instrument.snapshot().scramSavedTargetDepths().isEmpty(),
                        "SCRAM 释放后仍保留恢复目标副本");
                for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                    ControlRodColumnState rod = instrument.snapshot().controlRodColumns()
                            .get(new CoreColumnPosition(1, z));
                    require(helper, rod != null && close(rod.targetDepth(), 0.0D),
                            "SCRAM 释放没有恢复第 " + z + " 根棒的停堆前目标");
                }
                if (instrument.telemetry().available()
                        && instrument.telemetry().totalGeneratedFissionHeatHuPerTick() > 0.0D) {
                    phase[0] = 4;
                } else if (helper.getLevel().getGameTime() - phaseTick[0]
                        > POLL_DEADLINE_TICKS * 2L) {
                    helper.fail("SCRAM 释放后反馈簇没有恢复新生裂变热");
                }
                return;
            }
            if (phase[0] == 4) {
                // 部分卡死：中列卡死在 50%，两侧可动棒停在 0%；服务端入口直接观察状态。
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
                TreeMap<CoreColumnPosition, ControlRodColumnState> controls = new TreeMap<>();
                controls.put(new CoreColumnPosition(1, 0),
                        new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D));
                controls.put(new CoreColumnPosition(1, 1),
                        new ControlRodColumnState(0.0D, 0.5D, 0.5D, true, 0.0D));
                controls.put(new CoreColumnPosition(1, 2),
                        new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D));
                instrument.setSnapshot(instrument.snapshot().withColumns(
                        instrument.snapshot().fuelColumns(), controls));
                var incomplete = instrument.updateRedstoneScram(true);
                require(helper, incomplete.status() == ControlRodScramStatus.SCRAM_INCOMPLETE,
                        "部分卡死棒的 SCRAM 没有报告 SCRAM_INCOMPLETE：" + incomplete.status());
                require(helper, incomplete.fissionHeatHu() > 0.0D,
                        "部分卡死棒 SCRAM 没有暴露残余裂变热");
                require(helper, instrument.snapshot().scramSavedTargetDepths().keySet()
                                .equals(Set.of(new CoreColumnPosition(1, 0),
                                        new CoreColumnPosition(1, 2))),
                        "SCRAM 为卡死棒保存了恢复目标");
                ControlRodColumnState jammed = instrument.snapshot().controlRodColumns()
                        .get(new CoreColumnPosition(1, 1));
                require(helper, jammed.jammed() && close(jammed.targetDepth(), 0.5D)
                                && close(jammed.actualDepth(), 0.5D),
                        "SCRAM 改变了卡死棒的插入深度或卡死状态");
                helper.setBlock(REDSTONE_SOURCE, Blocks.REDSTONE_BLOCK.defaultBlockState());
                phase[0] = 5;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }
            if (phase[0] == 5) {
                if (helper.getLevel().getGameTime() - phaseTick[0] < 2L) {
                    return;
                }
                // 真实红石高电平对已激活 SCRAM 幂等，卡死棒保持原位。
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
                require(helper, instrument.snapshot().scramRequested(),
                        "真实红石高电平没有保持 SCRAM 请求");
                ControlRodColumnState jammed = instrument.snapshot().controlRodColumns()
                        .get(new CoreColumnPosition(1, 1));
                require(helper, jammed.jammed() && close(jammed.targetDepth(), 0.5D)
                                && close(jammed.actualDepth(), 0.5D),
                        "真实红石持续高电平改变了卡死棒");
                helper.setBlock(REDSTONE_SOURCE, Blocks.AIR.defaultBlockState());
                phase[0] = 6;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }
            if (phase[0] == 6) {
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
                if (instrument.snapshot().scramRequested()) {
                    if (helper.getLevel().getGameTime() - phaseTick[0] > POLL_DEADLINE_TICKS) {
                        helper.fail("部分卡死场景的低电平没有释放 SCRAM");
                    }
                    return;
                }
                require(helper, instrument.snapshot().scramSavedTargetDepths().isEmpty(),
                        "部分卡死场景释放后仍保留恢复目标");
                for (int z : new int[]{0, 2}) {
                    ControlRodColumnState rod = instrument.snapshot().controlRodColumns()
                            .get(new CoreColumnPosition(1, z));
                    require(helper, rod != null && close(rod.targetDepth(), 0.0D),
                            "部分卡死场景释放没有恢复可动棒目标");
                }
                ControlRodColumnState jammed = instrument.snapshot().controlRodColumns()
                        .get(new CoreColumnPosition(1, 1));
                require(helper, jammed.jammed() && close(jammed.targetDepth(), 0.5D)
                                && close(jammed.actualDepth(), 0.5D),
                        "释放流程改变了部分卡死棒");
                helper.succeed();
            }
        });
    }

    /**
     * 场景三：自动换料/维修—保存重载。Create 机械臂交互点完成一次带自定义数据组件的
     * 原子装取，燃料列与控制棒列各执行一次真实玩家钢板维修，随后仪表与端口 NBT 保存
     * 重载；断言端口 ItemStack 仍是唯一燃料所有者、耐久和数据组件不复制，维修不补
     * 燃料、不清余热、不回退融毁进度，最后经真实移除—重扫—补回恢复绑定与 capability。
     *
     * <p>跨越子系统：结构扫描/生命周期、Create 机械臂交互点、换料端口事务、两类维修
     * 事务、方块实体保存/加载 NBT、流体 capability 生命周期。</p>
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 320)
    public static void armRefuelRepairSaveReloadAndRescanRestore(GameTestHelper helper) {
        BlockPos origin = BlockPos.ZERO;
        buildStructure(helper, origin, repairLayout());
        long startTick = helper.getLevel().getGameTime();
        int[] phase = {0};
        long[] phaseTick = {startTick};
        ItemStack[] reloadedFuel = {ItemStack.EMPTY};
        helper.succeedWhen(() -> {
            if (phase[0] == 0) {
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
                if (!instrument.structureValid()) {
                    if (helper.getLevel().getGameTime() - startTick > FORMATION_DEADLINE_TICKS) {
                        helper.fail("换料维修总回归的结构没有在期限内成型");
                    }
                    return;
                }
                // 机械臂原子装取：自定义名称与耐久随完整 ItemStack 往返，不产生复制。
                helper.setBlock(ARM_POS, AllBlocks.MECHANICAL_ARM.get().defaultBlockState());
                BlockPos absolutePort = helper.absolutePos(origin.offset(FUEL_PORT));
                ArmInteractionPoint point = ArmInteractionPoint.create(
                        helper.getLevel(), absolutePort,
                        helper.getLevel().getBlockState(absolutePort));
                require(helper, point != null
                                && point.getType() == FuelRefuelingArmInteractionPoint.TYPE
                                && point.isValid(),
                        "有效换料端口没有建立正式机械臂交互点");
                ArmBlockEntity arm = (ArmBlockEntity) helper.getBlockEntity(
                        origin.offset(ARM_POS));
                require(helper, arm != null, "Create 机械臂方块实体没有建立");
                ReactorPortBlockEntity port = refuelingPort(helper, origin, FUEL_COLUMN);
                ItemStack loaded = freshFuel(4321, "VERIFY01-ARM");
                require(helper, point.insert(arm, loaded, false).isEmpty(),
                        "机械臂正式装料没有消费完整输入栈");
                require(helper, ItemStack.matches(port.fuelAssembly(), loaded),
                        "机械臂装料没有保存完整耐久与数据组件");
                ItemStack extracted = point.extract(arm, 0, 1, false);
                require(helper, ItemStack.matches(extracted, loaded) && port.fuelAssembly().isEmpty(),
                        "机械臂取料没有原子返回精确物品栈");
                ItemStack reload = freshFuel(5678, "VERIFY01-RELOAD");
                reloadedFuel[0] = reload;
                require(helper, point.insert(arm, reload, false).isEmpty(),
                        "机械臂二次装料失败");

                // 燃料列钢板维修：不补燃料、不清余热、不回退融毁进度。
                FuelAssemblyState assembly = FuelAssemblyItemCodec.readFreshFuel(
                        port.fuelAssembly());
                FuelColumnState damagedColumn = new FuelColumnState(
                        assembly, 0.6D, 7.0D, 0.3D, 2.0D);
                instrument.setSnapshot(instrument.snapshot()
                        .withFuelColumn(FUEL_COLUMN, damagedColumn)
                        .withMeltdown(11L, true));
                Player repairPlayer = playerAt(helper, FUEL_PORT);
                repairPlayer.setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.STEEL_PLATE.get(), 16));
                ItemInteractionResult fuelRepair = rightClick(helper, FUEL_PORT, repairPlayer);
                require(helper, fuelRepair.consumesAction(), "燃料列维修没有消费方块交互");
                FuelColumnState repairedColumn =
                        instrument.snapshot().fuelColumns().get(FUEL_COLUMN);
                require(helper, repairedColumn != null
                                && close(repairedColumn.integrity(), 0.6D + REPAIR_AMOUNT),
                        "燃料列维修没有恢复固定完整度");
                require(helper, close(repairedColumn.cachedHeatHu(), 7.0D)
                                && close(repairedColumn.quantizedHeatRemainderHu(), 2.0D)
                                && close(repairedColumn.fuelBurnRemainder(), 0.3D),
                        "燃料列维修改变了缓存余热或小数余量");
                require(helper, repairedColumn.fuelAssembly().damage() == 5678
                                && ItemStack.matches(port.fuelAssembly(), reload),
                        "燃料列维修补充或修改了端口燃料耐久与组件");
                require(helper, repairPlayer.getItemInHand(InteractionHand.MAIN_HAND)
                                .getCount() == 15,
                        "燃料列维修没有逐次消耗一块合金钢板");
                require(helper, instrument.snapshot().meltdownProgressTicks() == 11L
                                && instrument.snapshot().meltdownCountdownStarted(),
                        "燃料列维修回退或清除了融毁进度");

                // 控制棒列钢板维修：只恢复绑定列完整度，保留插入深度与缓存热。
                instrument.setSnapshot(instrument.snapshot().withColumns(
                        instrument.snapshot().fuelColumns(),
                        Map.of(CONTROL_COLUMN,
                                new ControlRodColumnState(0.5D, 1.0D, 1.0D, false, 1.5D))));
                Player drivePlayer = playerAt(helper, DRIVE);
                drivePlayer.setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.STEEL_PLATE.get(), 8));
                ItemInteractionResult controlRepair = rightClick(helper, DRIVE, drivePlayer);
                require(helper, controlRepair.consumesAction(), "控制棒列维修没有消费方块交互");
                ControlRodColumnState repairedControl =
                        instrument.snapshot().controlRodColumns().get(CONTROL_COLUMN);
                require(helper, repairedControl != null
                                && close(repairedControl.integrity(), 0.5D + REPAIR_AMOUNT),
                        "控制棒列维修没有恢复固定完整度");
                require(helper, close(repairedControl.targetDepth(), 1.0D)
                                && close(repairedControl.actualDepth(), 1.0D)
                                && close(repairedControl.cachedHeatHu(), 1.5D),
                        "控制棒列维修改变了插入深度或缓存热");
                require(helper, drivePlayer.getItemInHand(InteractionHand.MAIN_HAND)
                                .getCount() == 7,
                        "控制棒列维修没有消耗一块合金钢板");
                require(helper, instrument.snapshot().meltdownProgressTicks() == 11L
                                && instrument.snapshot().meltdownCountdownStarted(),
                        "控制棒列维修回退了融毁进度");

                // 仪表与端口 NBT 保存重载：端口是唯一持久化燃料所有者，仪表只保存运行字段。
                double repairedIntegrity = instrument.snapshot().fuelColumns()
                        .get(FUEL_COLUMN).integrity();
                double repairedControlIntegrity = instrument.snapshot().controlRodColumns()
                        .get(CONTROL_COLUMN).integrity();
                CompoundTag savedInstrument = instrument.saveForServerTest(
                        helper.getLevel().registryAccess());
                CompoundTag savedPort = port.saveForServerTest(
                        helper.getLevel().registryAccess());
                ReactorInstrumentPortBlockEntity reloadedInstrument =
                        new ReactorInstrumentPortBlockEntity(
                                helper.absolutePos(origin.offset(INSTRUMENT)),
                                P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
                reloadedInstrument.loadForServerTest(savedInstrument,
                        helper.getLevel().registryAccess());
                ReactorPortBlockEntity reloadedPort = new ReactorPortBlockEntity(
                        helper.absolutePos(origin.offset(FUEL_PORT)),
                        P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState());
                reloadedPort.loadForServerTest(savedPort, helper.getLevel().registryAccess());
                require(helper, ItemStack.matches(reloadedPort.fuelAssembly(), reload)
                                && reloadedPort.fuelAssembly().getDamageValue() == 5678,
                        "端口 NBT 重载没有保留唯一燃料栈的耐久与数据组件");
                FuelColumnState reloadedColumn = reloadedInstrument.snapshot().fuelColumns()
                        .get(FUEL_COLUMN);
                require(helper, reloadedColumn != null
                                && close(reloadedColumn.integrity(), repairedIntegrity)
                                && close(reloadedColumn.cachedHeatHu(), 7.0D)
                                && close(reloadedColumn.fuelBurnRemainder(), 0.3D)
                                && close(reloadedColumn.quantizedHeatRemainderHu(), 2.0D),
                        "仪表 NBT 重载没有保留燃料列运行字段");
                require(helper, !reloadedColumn.fuelAssembly().present(),
                        "仪表 NBT 重载复制了燃料组件投影");
                ControlRodColumnState reloadedControl = reloadedInstrument.snapshot()
                        .controlRodColumns().get(CONTROL_COLUMN);
                require(helper, reloadedControl != null
                                && close(reloadedControl.integrity(), repairedControlIntegrity)
                                && close(reloadedControl.cachedHeatHu(), 1.5D),
                        "仪表 NBT 重载没有保留控制棒列运行字段");
                require(helper, reloadedInstrument.snapshot().meltdownProgressTicks() == 11L
                                && reloadedInstrument.snapshot().meltdownCountdownStarted(),
                        "仪表 NBT 重载没有保留融毁进度");

                require(helper, helper.getLevel().removeBlock(
                                helper.absolutePos(origin.offset(CASING)), false),
                        "外壳实际移除失败");
                phase[0] = 1;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }
            if (phase[0] == 1) {
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
                if (instrument.structureValid()) {
                    if (helper.getLevel().getGameTime() - phaseTick[0] > POLL_DEADLINE_TICKS) {
                        helper.fail("外壳移除后结构仍保持有效");
                    }
                    return;
                }
                require(helper, coldHandler(helper, origin) == null,
                        "结构失效后冷端 capability 仍可被发现");
                require(helper, !refuelingPort(helper, origin, FUEL_COLUMN).isBound(),
                        "结构失效后换料端口仍保持旧绑定");
                helper.setBlock(CASING, P1Blocks.REACTOR_CASING.get().defaultBlockState());
                ReactorStructureLifecycle.rescanInstrumentPortNow(
                        helper.getLevel(), helper.absolutePos(origin.offset(INSTRUMENT)));
                require(helper, instrument.structureValid(), "补回外壳后结构没有重新成型");
                ReactorPortBlockEntity restoredPort =
                        refuelingPort(helper, origin, FUEL_COLUMN);
                require(helper, restoredPort.isBoundTo(instrument,
                                ReactorPortBlockEntity.BindingType.REFUELING, FUEL_COLUMN),
                        "重扫后换料端口没有恢复唯一列绑定");
                require(helper, coldHandler(helper, origin) != null,
                        "重扫后冷端 capability 没有恢复");
                require(helper, ItemStack.matches(restoredPort.fuelAssembly(), reloadedFuel[0]),
                        "重扫恢复改变了端口燃料栈的耐久或数据组件");
                helper.succeed();
            }
        });
    }

    /**
     * 场景四：危险拆除与安全重组成型互斥。同一正式结构在裂变运行时破坏组件只发布一次
     * 现有融毁事件占位且不清空任何状态；替换仪表端口建立全新的权威所有者后，四项完全
     * 停机时破坏非仪表组件会清空端口、快照、迁移信封和遥测，随后经实际移除—重扫—补回
     * 断言全部控制棒默认完全插入。全程不要求任何事故世界效果。
     *
     * <p>跨越子系统：结构扫描/生命周期、正式服务端 tick、真实 BreakEvent、危险拆除
     * 事务与融毁事件发布、换料端口事务、方块实体生命周期与迁移信封。</p>
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 360)
    public static void dangerousBreakPublishesOnceAndSafeResetClearsThenReformStartsInserted(
            GameTestHelper helper
    ) {
        BlockPos origin = BlockPos.ZERO;
        buildFcfStructure(helper, origin);
        long startTick = helper.getLevel().getGameTime();
        int[] phase = {0};
        long[] phaseTick = {startTick};
        int[] published = {0};
        ReactorInstrumentPortBlockEntity[] oldInstrument = {null};
        @SuppressWarnings("unchecked")
        Map<CoreColumnPosition, ItemStack>[] beforeFuel = new Map[1];
        helper.succeedWhen(() -> {
            if (phase[0] == 0) {
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
                if (!instrument.structureValid()) {
                    if (helper.getLevel().getGameTime() - startTick > FORMATION_DEADLINE_TICKS) {
                        helper.fail("危险拆除总回归的 F-C-F 结构没有在期限内成型");
                    }
                    return;
                }
                loadFuel(helper, origin, 0, "VERIFY01-DANGER");
                for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                    commitRod(helper, origin, z, 0);
                }
                require(helper, instrument.tickControlRods(), "危险状态没有完成控制棒 tick");
                require(helper, instrument.tickReactor(), "危险状态没有完成正式 tick");
                require(helper, instrument.telemetry().available()
                                && instrument.telemetry()
                                .totalGeneratedFissionHeatHuPerTick() > 0.0D,
                        "危险拆除前置没有建立真实裂变运行");
                oldInstrument[0] = instrument;
                NeoForge.EVENT_BUS.addListener(ReactorMeltdownEvent.class, event -> {
                    if (event.instrumentPort().equals(instrument.getBlockPos())) {
                        published[0]++;
                    }
                });

                // 裂变运行中的第一次破坏：只发布一次事件占位，不清空任何状态。
                beforeFuel[0] = fuelItemMap(helper, origin);
                BlockEvent.BreakEvent dangerous = postBreak(helper, CASING,
                        helper.makeMockPlayer(GameType.SURVIVAL));
                require(helper, !dangerous.isCanceled(), "裂变运行的破坏被错误取消");
                require(helper, instrument.snapshot().meltdownCountdownStarted()
                                && instrument.snapshot().meltdownProgressTicks()
                                == P1ServerConfig.VALUES.meltdownCountdownTicks.get()
                                && instrument.snapshot().meltdownEventPublished(),
                        "危险拆除没有提交完整的融毁占位状态");
                require(helper, published[0] == 1, "危险拆除没有恰好发布一次融毁事件");
                require(helper, sameFuelItems(beforeFuel[0], fuelItemMap(helper, origin)),
                        "危险拆除错误清空了换料端口物品");
                ReactorSnapshot committed = instrument.snapshot();

                // 同一已提交状态的重复破坏：不重发事件、不改变快照。
                BlockEvent.BreakEvent repeated = postBreak(helper, WINDOW,
                        helper.makeMockPlayer(GameType.SURVIVAL));
                require(helper, !repeated.isCanceled(), "已提交危险状态的重复破坏被错误取消");
                require(helper, published[0] == 1, "重复危险破坏错误重复发布融毁事件");
                require(helper, instrument.snapshot().equals(committed),
                        "重复危险破坏改变了已提交的权威快照");

                // 完全停机路径：替换唯一仪表端口建立全新权威所有者，端口与管道保持原位。
                BlockPos absoluteInstrument = helper.absolutePos(origin.offset(INSTRUMENT));
                require(helper, helper.getLevel().removeBlock(absoluteInstrument, false),
                        "仪表端口实际移除失败");
                helper.setBlock(INSTRUMENT,
                        P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
                ReactorStructureLifecycle.rescanInstrumentPortNow(
                        helper.getLevel(), absoluteInstrument);
                ReactorInstrumentPortBlockEntity replacement = instrument(helper, origin);
                require(helper, replacement != oldInstrument[0] && replacement.structureValid(),
                        "仪表替换后没有建立新的有效结构所有者");
                require(helper, sameFuelItems(beforeFuel[0], fuelItemMap(helper, origin)),
                        "仪表替换改变了原位换料端口物品");
                oldInstrument[0] = replacement;
                installLegacyMigrationEnvelope(helper, replacement, new CoreColumnPosition(0, 0));

                // 四项完全停机：新所有者默认完全插棒，无新热、无燃耗、无余热、无倒计时。
                BlockEvent.BreakEvent safe = postBreak(helper, CASING,
                        helper.makeMockPlayer(GameType.SURVIVAL));
                require(helper, !safe.isCanceled(), "四项完全停机后的破坏被错误取消");
                require(helper, replacement.snapshot().equals(ReactorSnapshot.empty()),
                        "完全停机重置没有清空权威快照");
                require(helper, replacement.boundPorts(
                                ReactorPortBlockEntity.BindingType.REFUELING).stream()
                                .allMatch(port -> port.fuelAssembly().isEmpty()),
                        "完全停机重置没有清空全部换料端口");
                require(helper, !replacement.telemetry().available(),
                        "完全停机重置没有使运行时遥测失效");
                require(helper, !hasLegacyMigrationEnvelope(helper, replacement),
                        "完全停机重置没有清空旧迁移信封");
                require(helper, published[0] == 1, "完全停机重置错误发布了融毁事件");

                require(helper, helper.getLevel().removeBlock(
                                helper.absolutePos(origin.offset(CASING)), false),
                        "外壳实际移除失败");
                phase[0] = 1;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }
            if (phase[0] == 1) {
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
                if (instrument.structureValid()) {
                    if (helper.getLevel().getGameTime() - phaseTick[0] > POLL_DEADLINE_TICKS) {
                        helper.fail("外壳实际移除后结构仍保持有效");
                    }
                    return;
                }
                helper.setBlock(CASING, P1Blocks.REACTOR_CASING.get().defaultBlockState());
                ReactorStructureLifecycle.rescanInstrumentPortNow(
                        helper.getLevel(), helper.absolutePos(origin.offset(INSTRUMENT)));
                require(helper, instrument.structureValid(), "补回外壳后结构没有重新成型");
                require(helper, instrument.snapshot().equals(ReactorSnapshot.empty()),
                        "重新成型错误恢复了反应堆状态");
                Set<CoreColumnPosition> expectedControlColumns = new HashSet<>();
                for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                        : instrument.structureScan().columns().entrySet()) {
                    if (entry.getValue().type()
                            == ReactorStructureDefinition.ColumnType.CONTROL_ROD) {
                        expectedControlColumns.add(entry.getKey());
                    }
                }
                Map<CoreColumnPosition, ControlRodColumnState> actualControlColumns =
                        instrument.snapshot().controlRodColumns();
                require(helper, actualControlColumns.size() == expectedControlColumns.size()
                                && actualControlColumns.keySet().equals(expectedControlColumns),
                        "重新成型后的控制棒列集合与结构扫描不一致");
                require(helper, actualControlColumns.values().stream()
                                .allMatch(state -> state.equals(ControlRodColumnState.fullyInserted())),
                        "重新成型后的控制棒没有全部默认完全插入");
                require(helper, instrument.boundPorts(
                                ReactorPortBlockEntity.BindingType.REFUELING).stream()
                                .allMatch(port -> port.fuelAssembly().isEmpty()),
                        "重新成型后错误恢复了端口燃料");
                helper.succeed();
            }
        });
    }

    /** 按任务固定的三行 F-C-F 角色生成真实 5×5×5 结构。 */
    private static void buildFcfStructure(GameTestHelper helper, BlockPos origin) {
        buildStructure(helper, origin, fcfColumnLayout());
    }

    /** 场景三布局：默认八燃料环加 (1,0) 控制棒列，使 (0,0) 燃料列有相邻完全插入控制棒。 */
    private static Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> repairLayout() {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout =
                new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        layout.put(CONTROL_COLUMN, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        return layout;
    }

    /** 返回每行按 X 方向排列的燃料、控制棒、燃料角色。 */
    private static Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> fcfColumnLayout() {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> columns = new HashMap<>();
        for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
            columns.put(new CoreColumnPosition(0, z), ReactorStructureDefinition.ColumnType.FUEL);
            columns.put(new CoreColumnPosition(1, z),
                    ReactorStructureDefinition.ColumnType.CONTROL_ROD);
            columns.put(new CoreColumnPosition(2, z), ReactorStructureDefinition.ColumnType.FUEL);
        }
        return columns;
    }

    /** 通过正式结构坐标契约放置指定列布局的完整结构。 */
    private static void buildStructure(
            GameTestHelper helper,
            BlockPos origin,
            Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> columns
    ) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.templateFor(columns).entrySet()) {
            helper.setBlock(
                    origin.offset(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 通过正式换料端口事务为结构中全部燃料列装入一件新燃料。 */
    private static void loadFuel(GameTestHelper helper, BlockPos origin, int damage, String name) {
        ReactorInstrumentPortBlockEntity instrument = instrument(helper, origin);
        for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                : instrument.structureScan().columns().entrySet()) {
            if (entry.getValue().type() != ReactorStructureDefinition.ColumnType.FUEL) {
                continue;
            }
            ReactorPortBlockEntity port = refuelingPort(helper, origin, entry.getKey());
            FuelRefuelingTransaction.Result result = port.tryInsertFuel(freshFuel(damage, name));
            require(helper, result.success(),
                    "总回归装料失败：" + entry.getKey() + " " + result.status());
        }
    }

    /** 以服务端权威滑块提交为指定行控制棒设置目标深度百分比。 */
    private static void commitRod(GameTestHelper helper, BlockPos origin, int z, int depthPercent) {
        BlockPos drivePos = origin.offset(2, 4, z + 1);
        Player player = playerAt(helper, drivePos);
        var committed = ControlRodSliderService.commitFromCreate(
                player, helper.absolutePos(drivePos), 0, depthPercent);
        require(helper, committed.accepted(),
                "滑块提交被拒绝：row=" + z + " depth=" + depthPercent
                        + " " + committed.status());
        ControlRodSliderService.clearSession(player);
    }

    /** 通过正式方块 capability 向结构默认冷端输入冷态复合冷却剂，返回实际接受量。 */
    private static int fillCold(GameTestHelper helper, BlockPos origin, int amount) {
        IFluidHandler cold = coldHandler(helper, origin);
        require(helper, cold != null, "冷端 capability 不可用");
        return cold.fill(new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), amount),
                FluidAction.EXECUTE);
    }

    /** 读取结构默认冷端的正式方块流体 capability；结构失效时预期为 null。 */
    private static IFluidHandler coldHandler(GameTestHelper helper, BlockPos origin) {
        return helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(origin.offset(COLD)),
                Direction.SOUTH);
    }

    /** 返回指定结构原点对应的仪表端口方块实体。 */
    private static ReactorInstrumentPortBlockEntity instrument(
            GameTestHelper helper,
            BlockPos origin
    ) {
        var blockEntity = helper.getBlockEntity(origin.offset(INSTRUMENT));
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "固定坐标没有仪表端口方块实体");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    /** 返回指定燃料列顶部换料端口方块实体。 */
    private static ReactorPortBlockEntity refuelingPort(
            GameTestHelper helper,
            BlockPos origin,
            CoreColumnPosition column
    ) {
        var blockEntity = helper.getBlockEntity(
                origin.offset(column.x() + 1, 4, column.z() + 1));
        require(helper, blockEntity instanceof ReactorPortBlockEntity,
                "固定坐标没有换料端口方块实体：" + column);
        return (ReactorPortBlockEntity) blockEntity;
    }

    /** 汇总当前结构中全部换料端口燃料栈的整数耐久。 */
    private static int fuelDamageSum(GameTestHelper helper, BlockPos origin) {
        int total = 0;
        for (ReactorPortBlockEntity port : instrument(helper, origin).boundPorts(
                ReactorPortBlockEntity.BindingType.REFUELING)) {
            if (!port.fuelAssembly().isEmpty()) {
                total += port.fuelAssembly().getDamageValue();
            }
        }
        return total;
    }

    /** 读取当前结构中全部换料端口的精确物品副本，按列坐标排序。 */
    private static Map<CoreColumnPosition, ItemStack> fuelItemMap(
            GameTestHelper helper,
            BlockPos origin
    ) {
        Map<CoreColumnPosition, ItemStack> items = new TreeMap<>();
        for (ReactorPortBlockEntity port : instrument(helper, origin).boundPorts(
                ReactorPortBlockEntity.BindingType.REFUELING)) {
            items.put(port.boundColumn(), port.fuelAssembly());
        }
        return items;
    }

    /** 比较两组端口物品的 ID、耐久和全部数据组件，不比较世界对象身份。 */
    private static boolean sameFuelItems(
            Map<CoreColumnPosition, ItemStack> first,
            Map<CoreColumnPosition, ItemStack> second
    ) {
        if (!first.keySet().equals(second.keySet())) {
            return false;
        }
        return first.keySet().stream()
                .allMatch(column -> ItemStack.matches(first.get(column), second.get(column)));
    }

    /** 调用真实方块的服务端玩家交互入口。 */
    private static ItemInteractionResult rightClick(
            GameTestHelper helper,
            BlockPos relative,
            Player player
    ) {
        BlockPos absolute = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        return helper.getLevel().getBlockState(absolute).useItemOn(
                player.getItemInHand(InteractionHand.MAIN_HAND),
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                hit);
    }

    /** 在目标方块附近创建生存模式模拟玩家，保证服务端距离校验可以通过。 */
    private static Player playerAt(GameTestHelper helper, BlockPos relative) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absolute = helper.absolutePos(relative);
        player.setPos(absolute.getX() + 0.5D, absolute.getY() + 0.5D,
                absolute.getZ() + 2.0D);
        return player;
    }

    /** 发布真实 NeoForge BreakEvent；调用方自行决定是否执行实际世界移除。 */
    private static BlockEvent.BreakEvent postBreak(
            GameTestHelper helper,
            BlockPos relative,
            Player player
    ) {
        BlockPos absolute = helper.absolutePos(relative);
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(
                helper.getLevel(), absolute,
                helper.getLevel().getBlockState(absolute), player);
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    /** 在服务端测试保存路径中注入一个旧燃料迁移信封，模拟尚未完成的兼容迁移。 */
    private static void installLegacyMigrationEnvelope(
            GameTestHelper helper,
            ReactorInstrumentPortBlockEntity instrument,
            CoreColumnPosition column
    ) {
        CompoundTag saved = instrument.saveForServerTest(helper.getLevel().registryAccess());
        CompoundTag snapshotTag = saved.getCompound("ReactorSnapshot");
        ListTag migration = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putInt("X", column.x());
        entry.putInt("Z", column.z());
        entry.putBoolean("Present", true);
        entry.putInt("Damage", 77);
        entry.putInt("MaxDamage", ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY);
        migration.add(entry);
        snapshotTag.put("LegacyFuelMigration", migration);
        saved.put("ReactorSnapshot", snapshotTag);
        instrument.loadForServerTest(saved, helper.getLevel().registryAccess());
    }

    /** 读取正式保存结果确认旧迁移信封仍由仪表端口持有。 */
    private static boolean hasLegacyMigrationEnvelope(
            GameTestHelper helper,
            ReactorInstrumentPortBlockEntity instrument
    ) {
        CompoundTag saved = instrument.saveForServerTest(helper.getLevel().registryAccess());
        CompoundTag snapshotTag = saved.contains("ReactorSnapshot")
                ? saved.getCompound("ReactorSnapshot") : null;
        return snapshotTag != null
                && snapshotTag.contains("LegacyFuelMigration", Tag.TAG_LIST);
    }

    /** 构造带原版耐久和自定义名称数据组件的单件新燃料。 */
    private static ItemStack freshFuel(int damage, String name) {
        ItemStack stack = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        stack.setDamageValue(damage);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    /** 按正式注册 ID 将结构契约转换为真实方块。 */
    private static Block blockForId(String id) {
        return switch (id) {
            case "minecraft:air" -> Blocks.AIR;
            case "create_nuclear_industry:reactor_casing" -> P1Blocks.REACTOR_CASING.get();
            case "create_nuclear_industry:reactor_window" -> P1Blocks.REACTOR_WINDOW.get();
            case "create_nuclear_industry:reactor_instrument_port" ->
                    P1Blocks.REACTOR_INSTRUMENT_PORT.get();
            case "create_nuclear_industry:reactor_cold_port" -> P1Blocks.REACTOR_COLD_PORT.get();
            case "create_nuclear_industry:reactor_hot_port" -> P1Blocks.REACTOR_HOT_PORT.get();
            case "create_nuclear_industry:reactor_refueling_port" ->
                    P1Blocks.REACTOR_REFUELING_PORT.get();
            case "create_nuclear_industry:reactor_fuel_rod" -> P1Blocks.REACTOR_FUEL_ROD.get();
            case "create_nuclear_industry:control_rod_drive" -> P1Blocks.CONTROL_ROD_DRIVE.get();
            default -> throw new IllegalArgumentException("未知反应堆方块 ID：" + id);
        };
    }

    /** 双精度数值的严格相等比较，容差覆盖浮点运算与反馈迭代误差。 */
    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= 1.0E-9D;
    }

    /** 将断言失败统一交给 GameTest，失败立即终止当前用例。 */
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
