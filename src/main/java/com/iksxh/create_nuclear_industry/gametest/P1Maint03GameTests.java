package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.iksxh.create_nuclear_industry.structure.ReactorDisassemblyPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** 验证完全停机破坏性重组的真实 BreakEvent、清空事务、回滚预检和生命周期闭环。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Maint03GameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final CoreColumnPosition SOURCE_FUEL = new CoreColumnPosition(1, 1);
    private static final CoreColumnPosition SECOND_FUEL = new CoreColumnPosition(0, 0);
    private static final CoreColumnPosition NON_ADJACENT_CONTROL = new CoreColumnPosition(2, 2);
    private static final BlockPos CASING = new BlockPos(0, 0, 0);
    private static final BlockPos WINDOW = new BlockPos(0, 2, 2);
    private static final BlockPos COLD_PORT = new BlockPos(1, 2, 4);
    private static final BlockPos HOT_PORT = new BlockPos(3, 2, 4);
    private static final BlockPos REFUELING_PORT = new BlockPos(2, 4, 2);
    private static final BlockPos FUEL_ROD = new BlockPos(2, 1, 2);
    private static final BlockPos CONTROL_ROD_DRIVE = new BlockPos(1, 4, 1);

    private P1Maint03GameTests() {
    }

    /** 七类非仪表组件共用同一完全停机清空事务，燃料物品和运行时字段全部归零。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void allNonInstrumentComponentsClearTheSameState(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, BlockPos.ZERO);
            if (!require(helper, instrument.structureValid(), "停机重组测试结构未成型")) {
                return;
            }
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            List<BlockPos> components = List.of(
                    CASING, WINDOW, COLD_PORT, HOT_PORT,
                    REFUELING_PORT, FUEL_ROD, CONTROL_ROD_DRIVE);
            for (BlockPos component : components) {
                installFullyStoppedFixture(instrument);
                BlockEvent.BreakEvent event = postBreak(helper,
                        helper.absolutePos(component), player);
                if (!require(helper, !event.isCanceled(),
                        "非仪表组件被完全停机安全门错误取消：" + component)) {
                    return;
                }
                if (!requireCleared(helper, instrument,
                        "非仪表组件没有清空全部反应堆状态：" + component)) {
                    return;
                }
            }
            helper.succeed();
        });
    }

    /** 真实服务端事件放行后实际移除方块，随后通过正式仪表重扫和补回方块完成重新成型。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void realBreakRemoveRescanAndReformStartsEmpty(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO);
        ReactorInstrumentPortBlockEntity[] instrumentRef = new ReactorInstrumentPortBlockEntity[1];
        boolean[] aborted = new boolean[1];
        helper.startSequence()
                .thenExecuteAfter(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, BlockPos.ZERO);
            instrumentRef[0] = instrument;
            if (aborted[0]) {
                return;
            }
            installFullyStoppedFixture(instrument);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos absoluteCasing = helper.absolutePos(CASING);
            BlockEvent.BreakEvent event = postBreak(helper, absoluteCasing, player);
            if (!require(helper, !event.isCanceled(), "完全停机外壳破坏被错误取消")) {
                aborted[0] = true;
                return;
            }
            if (!require(helper, helper.getLevel().removeBlock(absoluteCasing, false),
                    "BreakEvent 放行后没有实际移除外壳方块")) {
                aborted[0] = true;
                return;
            }
        })
                .thenExecuteAfter(3, () -> {
                    if (aborted[0]) {
                        return;
                    }
                    ReactorInstrumentPortBlockEntity instrument = instrumentRef[0];
                    if (!require(helper, !instrument.structureValid(),
                        "实际移除后结构仍保持有效")) {
                        aborted[0] = true;
                        return;
                    }
                    if (!requireCleared(helper, instrument, "实际移除后反应堆状态未保持清空")) {
                        aborted[0] = true;
                        return;
                    }

                    helper.setBlock(CASING, P1Blocks.REACTOR_CASING.get().defaultBlockState());
                    ReactorStructureLifecycle.rescanInstrumentPortNow(
                            helper.getLevel(), helper.absolutePos(INSTRUMENT));
                    if (!require(helper, instrument.structureValid(),
                            "补回外壳后结构没有重新成型")) {
                        aborted[0] = true;
                        return;
                    }
                    if (!require(helper, instrument.snapshot().fuelColumns().isEmpty(),
                            "重新成型后错误恢复了燃料列投影")) {
                        aborted[0] = true;
                        return;
                    }
                    if (!require(helper, instrument.snapshot().coldCoolantMb() == 0L
                                    && instrument.snapshot().hotCoolantMb() == 0L,
                            "重新成型后错误恢复了冷却剂库存")) {
                        aborted[0] = true;
                        return;
                    }
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
                    if (!require(helper,
                            actualControlColumns.size() == expectedControlColumns.size()
                                    && actualControlColumns.keySet().equals(expectedControlColumns),
                            "重新成型后的控制棒坐标集合与结构扫描不一致")) {
                        aborted[0] = true;
                        return;
                    }
                    if (!require(helper, actualControlColumns.values().stream()
                                    .allMatch(state -> state.equals(ControlRodColumnState.fullyInserted())),
                            "重新成型后的控制棒没有全部恢复为完全插入")) {
                        aborted[0] = true;
                        return;
                    }
                    if (!require(helper, !instrument.snapshot().meltdownCountdownStarted()
                                    && !instrument.snapshot().meltdownEventPublished(),
                            "重新成型后错误恢复了融毁状态")) {
                        aborted[0] = true;
                    }
                })
                .thenExecute(() -> helper.succeed());
    }

    /** 只有活动余热时取消破坏，保留方块、快照和全部端口物品。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void residualHeatRejectsWithoutMutation(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, BlockPos.ZERO);
            ReactorSnapshot fixture = installFullyStoppedFixture(instrument);
            FuelColumnState oldColumn = fixture.fuelColumns().get(SOURCE_FUEL);
            ReactorSnapshot unsafe = fixture.withFuelColumn(SOURCE_FUEL, new FuelColumnState(
                    oldColumn.fuelAssembly(), oldColumn.integrity(), 5.0D,
                    oldColumn.fuelBurnRemainder(), 0.0D));
            instrument.setSnapshot(unsafe);
            Map<CoreColumnPosition, ItemStack> beforeFuel = fuelItems(instrument);
            BlockPos absoluteWindow = helper.absolutePos(WINDOW);
            BlockStateSnapshot beforeBlock = new BlockStateSnapshot(
                    helper.getLevel().getBlockState(absoluteWindow));
            BlockEvent.BreakEvent event = postBreak(helper, absoluteWindow,
                    helper.makeMockPlayer(GameType.SURVIVAL));
            if (!require(helper, event.isCanceled(), "活动余热状态错误放行了破坏")) {
                return;
            }
            if (!require(helper, instrument.snapshot().equals(unsafe),
                    "活动余热拒绝改变了权威快照")) {
                return;
            }
            if (!require(helper, sameFuelItems(beforeFuel, fuelItems(instrument)),
                    "活动余热拒绝改变了端口物品")) {
                return;
            }
            if (!require(helper, beforeBlock.state().equals(helper.getLevel().getBlockState(absoluteWindow)),
                    "活动余热拒绝改变了待破坏方块")) {
                return;
            }
            helper.succeed();
        });
    }

    /** 端口物品是危险判定的权威来源；陈旧空快照不得把实际带料反应堆误判为安全清空。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void staleSnapshotUsesCurrentFuelPortProjection(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, BlockPos.ZERO);
            ReactorSnapshot fixture = installFullyStoppedFixture(instrument);
            TreeMap<CoreColumnPosition, FuelColumnState> staleFuel =
                    new TreeMap<>(fixture.fuelColumns());
            staleFuel.put(SOURCE_FUEL,
                    fixture.fuelColumns().get(SOURCE_FUEL).withoutFuelAssemblyProjection());
            TreeMap<CoreColumnPosition, ControlRodColumnState> staleControl =
                    new TreeMap<>(fixture.controlRodColumns());
            CoreColumnPosition uninsertedNeighbour = new CoreColumnPosition(1, 0);
            ControlRodColumnState neighbour = staleControl.get(uninsertedNeighbour);
            if (!require(helper, neighbour != null, "陈旧投影测试缺少源燃料相邻控制棒")) {
                return;
            }
            staleControl.put(uninsertedNeighbour, new ControlRodColumnState(
                    neighbour.integrity(), neighbour.targetDepth(), 0.0D,
                    neighbour.jammed(), neighbour.cachedHeatHu()));
            ReactorSnapshot stale = new ReactorSnapshot(
                    staleFuel, staleControl, fixture.coldCoolantMb(), fixture.hotCoolantMb(),
                    fixture.meltdownProgressTicks(), fixture.meltdownCountdownStarted(),
                    fixture.scramSavedTargetDepths(), fixture.scramRequested(),
                    fixture.meltdownEventPublished());
            instrument.setSnapshot(stale);
            ReactorPortBlockEntity sourcePort = findRefuelingPort(instrument, SOURCE_FUEL);
            ItemStack beforeFuel = sourcePort.fuelAssembly();
            BlockEvent.BreakEvent event = postBreak(helper,
                    helper.absolutePos(CASING), helper.makeMockPlayer(GameType.SURVIVAL));
            if (!require(helper, !event.isCanceled(), "带料端口被陈旧空快照误判为安全清空")) {
                return;
            }
            FuelColumnState projected = instrument.snapshot().fuelColumns().get(SOURCE_FUEL);
            if (!require(helper, projected != null && projected.hasUsableFuel()
                            && projected.fuelAssembly().equals(FuelAssemblyItemCodec.readFreshFuel(beforeFuel)),
                    "危险判定没有采用换料端口的精确燃料投影")) {
                return;
            }
            if (!require(helper, instrument.snapshot().meltdownCountdownStarted()
                            && instrument.snapshot().meltdownEventPublished(),
                    "端口带料的危险拆除没有提交融毁占位")) {
                return;
            }
            if (!require(helper, ItemStack.matches(beforeFuel, sourcePort.fuelAssembly()),
                    "危险拆除改变了权威换料端口物品")) {
                return;
            }
            helper.succeed();
        });
    }

    /** 受控提交后校验失败必须恢复端口、快照、迁移信封和遥测；成功重置随后清除迁移信封。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void resetRollbackRestoresMigrationAndTelemetry(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, BlockPos.ZERO);
            ReactorSnapshot stopped = installFullyStoppedFixture(instrument);
            installLegacyMigrationEnvelope(helper, instrument);
            Map<CoreColumnPosition, ItemStack> stoppedFuel = fuelItems(instrument);
            clearFuelItems(instrument);
            instrument.setSnapshot(ReactorSnapshot.empty());
            if (!require(helper, !instrument.tickReactor() && instrument.telemetry().available(),
                    "回滚测试没有生成可验证遥测")) {
                return;
            }
            restoreFuelItems(instrument, stoppedFuel);
            instrument.setSnapshot(stopped);
            ReactorSnapshot beforeSnapshot = instrument.snapshot();
            Map<CoreColumnPosition, ItemStack> beforeFuel = fuelItems(instrument);
            var beforeTelemetry = instrument.telemetry();
            if (!require(helper, beforeTelemetry.available(), "回滚测试遥测仍不可用")) {
                return;
            }
            ReactorDisassemblyPlan plan = instrument.prepareDisassemblyPlan(
                    helper.absolutePos(CASING),
                    helper.getLevel().getBlockState(helper.absolutePos(CASING)));
            if (!require(helper, plan != null
                            && plan.action() == ReactorDisassemblyPlan.Action.FULL_SHUTDOWN_RESET,
                    "完全停机回滚测试没有生成清空计划："
                            + (plan == null ? "null" : plan.action() + "/" + plan.shutdownAssessment()))) {
                return;
            }
            if (!require(helper, instrument.commitDisassemblyPlan(plan),
                    "完全停机回滚测试提交失败")) {
                return;
            }
            ReactorPortBlockEntity port = plan.fuelPorts().get(0).port();
            port.setFuelAssembly(freshFuel(177, "MAINT03-controlled-fault"));
            if (!require(helper, !instrument.verifyDisassemblyPlan(plan),
                    "受控端口故障没有触发提交后校验失败")) {
                return;
            }
            instrument.rollbackDisassemblyPlan(plan);
            if (!require(helper, instrument.snapshot().equals(beforeSnapshot),
                    "回滚没有恢复完整权威快照")) {
                return;
            }
            if (!require(helper, sameFuelItems(beforeFuel, fuelItems(instrument)),
                    "回滚没有恢复全部端口物品的耐久和数据组件")) {
                return;
            }
            if (!require(helper, instrument.telemetry().equals(beforeTelemetry),
                    "回滚没有恢复提交前遥测")) {
                return;
            }
            if (!require(helper, hasLegacyMigrationEnvelope(helper, instrument),
                    "回滚没有恢复待迁移燃料信封")) {
                return;
            }

            BlockEvent.BreakEvent event = postBreak(helper,
                    helper.absolutePos(CASING), helper.makeMockPlayer(GameType.SURVIVAL));
            if (!require(helper, !event.isCanceled(), "恢复后的完全停机重置被错误取消")) {
                return;
            }
            if (!require(helper, requireCleared(helper, instrument,
                    "成功完全停机重置没有清空全部状态"), "成功重置断言失败")) {
                return;
            }
            if (!require(helper, !hasLegacyMigrationEnvelope(helper, instrument),
                    "成功完全停机重置重新留下旧迁移信封")) {
                return;
            }
            helper.succeed();
        });
    }

    /** 完全停机仪表例外只放行原破坏；实际移除和补回后多个端口物品必须逐栈保持不变。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void instrumentMaintenancePreservesMultipleFuelPorts(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO, Set.of(SOURCE_FUEL, SECOND_FUEL));
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, BlockPos.ZERO);
            if (!require(helper, instrument.structureValid(), "多端口仪表维护结构未成型")) {
                return;
            }
            installFullyStoppedFixture(instrument);
            Map<CoreColumnPosition, ItemStack> beforeFuel = fuelItems(instrument);
            if (!require(helper, beforeFuel.size() == 2, "多端口夹具没有建立两个换料端口物品")) {
                return;
            }
            BlockPos absoluteInstrument = helper.absolutePos(INSTRUMENT);
            BlockEvent.BreakEvent event = postBreak(helper, absoluteInstrument,
                    helper.makeMockPlayer(GameType.SURVIVAL));
            if (!require(helper, !event.isCanceled(), "完全停机仪表维护例外错误取消 BreakEvent")) {
                return;
            }
            if (!require(helper, sameFuelItems(beforeFuel, fuelItems(instrument)),
                    "仪表维护例外在直接破坏前改变了多个端口物品")) {
                return;
            }
            if (!require(helper, helper.getLevel().removeBlock(absoluteInstrument, false),
                    "仪表维护 BreakEvent 放行后没有实际移除仪表方块")) {
                return;
            }
            helper.setBlock(INSTRUMENT, P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), absoluteInstrument);
            ReactorInstrumentPortBlockEntity replacement = instrument(helper, BlockPos.ZERO);
            if (!require(helper, replacement != instrument && replacement.structureValid(),
                    "仪表补回后没有建立新的有效结构所有者")) {
                return;
            }
            if (!require(helper, sameFuelItems(beforeFuel, fuelItems(replacement)),
                    "仪表实际移除—补回后没有保持多个端口物品的完整身份")) {
                return;
            }
            helper.succeed();
        });
    }

    /** 兼容入口只在本次首次提交并实际发布危险事件时返回 true，重复和完成状态均不得重复发布。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void dangerousCompatibilityEntryReportsOnlyFirstPublish(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, BlockPos.ZERO);
            ReactorSnapshot fixture = installFullyStoppedFixture(instrument);
            TreeMap<CoreColumnPosition, ControlRodColumnState> controls =
                    new TreeMap<>(fixture.controlRodColumns());
            CoreColumnPosition uninsertedNeighbour = new CoreColumnPosition(1, 0);
            ControlRodColumnState neighbour = controls.get(uninsertedNeighbour);
            if (!require(helper, neighbour != null, "兼容入口测试缺少源燃料相邻控制棒")) {
                return;
            }
            controls.put(uninsertedNeighbour, new ControlRodColumnState(
                    neighbour.integrity(), neighbour.targetDepth(), 0.0D,
                    neighbour.jammed(), neighbour.cachedHeatHu()));
            ReactorSnapshot dangerous = new ReactorSnapshot(
                    fixture.fuelColumns(), controls, fixture.coldCoolantMb(), fixture.hotCoolantMb(),
                    0L, false, fixture.scramSavedTargetDepths(), fixture.scramRequested(), false);
            instrument.setSnapshot(dangerous);
            BlockPos target = helper.absolutePos(CASING);
            int[] published = new int[1];
            NeoForge.EVENT_BUS.addListener(
                    com.iksxh.create_nuclear_industry.reactor.ReactorMeltdownEvent.class,
                    event -> {
                        if (event.instrumentPort().equals(instrument.getBlockPos())) {
                            published[0]++;
                        }
                    });
            BlockState state = helper.getLevel().getBlockState(target);
            if (!require(helper, instrument.tryCommitDangerousDisassembly(target, state),
                    "首次危险兼容提交没有返回实际发布成功")) {
                return;
            }
            if (!require(helper, published[0] == 1,
                    "首次危险兼容提交没有恰好发布一次事件")) {
                return;
            }
            ReactorSnapshot firstCommitted = instrument.snapshot();
            if (!require(helper, !instrument.tryCommitDangerousDisassembly(target, state)
                            && published[0] == 1,
                    "重复危险兼容提交错误返回成功或重复发布")) {
                return;
            }
            instrument.setSnapshot(dangerous.withMeltdown(
                    Math.max(1, com.iksxh.create_nuclear_industry.config.P1ServerConfig.VALUES
                            .meltdownCountdownTicks.get()), true));
            if (!require(helper, !instrument.tryCommitDangerousDisassembly(target, state)
                            && published[0] == 1,
                    "已完成但未发布的危险状态错误返回成功")) {
                return;
            }
            instrument.setSnapshot(firstCommitted);
            if (!require(helper, !instrument.tryCommitDangerousDisassembly(target, state)
                            && published[0] == 1,
                    "已发布危险状态错误返回成功或重复发布")) {
                return;
            }
            helper.succeed();
        });
    }

    /** 缺少结构映射要求的燃料端口时，整次破坏在读阶段取消且不先清空其他所有者。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void missingFuelPortRejectsBeforeAnyMutation(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, BlockPos.ZERO);
            ReactorSnapshot before = installFullyStoppedFixture(instrument);
            ReactorPortBlockEntity missing = instrument.boundPorts(
                            ReactorPortBlockEntity.BindingType.REFUELING).stream()
                    .findFirst().orElseThrow();
            helper.getLevel().removeBlockEntity(missing.getBlockPos());
            BlockEvent.BreakEvent event = postBreak(helper,
                    helper.absolutePos(CASING), helper.makeMockPlayer(GameType.SURVIVAL));
            if (!require(helper, event.isCanceled(), "缺少燃料端口时错误放行了破坏")) {
                return;
            }
            if (!require(helper, instrument.snapshot().equals(before),
                    "缺少燃料端口时提前清空了快照")) {
                return;
            }
            helper.succeed();
        });
    }

    /** 共享边界由两个有效所有者共同预检；任一所有者拒绝时另一个所有者也不得先清空。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void multipleOwnersUseAllOrNothingPreflight(GameTestHelper helper) {
        BlockPos secondOrigin = new BlockPos(4, 0, 0);
        buildStructure(helper, BlockPos.ZERO);
        buildStructure(helper, secondOrigin);
        helper.runAfterDelay(6, () -> {
            ReactorInstrumentPortBlockEntity first = instrument(helper, BlockPos.ZERO);
            ReactorInstrumentPortBlockEntity second = instrument(helper, secondOrigin);
            if (!require(helper, first.structureValid() && second.structureValid(),
                    "共享边界测试的两个结构没有同时成型")) {
                return;
            }
            installFullyStoppedFixture(first);
            installFullyStoppedFixture(second);
            BlockPos sharedWindow = helper.absolutePos(new BlockPos(4, 2, 2));
            BlockEvent.BreakEvent accepted = postBreak(helper, sharedWindow,
                    helper.makeMockPlayer(GameType.SURVIVAL));
            if (!require(helper, !accepted.isCanceled(), "两个安全所有者的共享破坏被错误取消")) {
                return;
            }
            if (!requireCleared(helper, first, "第一个共享所有者没有提交清空")) {
                return;
            }
            if (!requireCleared(helper, second, "第二个共享所有者没有提交清空")) {
                return;
            }

            ReactorSnapshot firstBefore = installFullyStoppedFixture(first);
            ReactorSnapshot secondFixture = installFullyStoppedFixture(second);
            FuelColumnState oldColumn = secondFixture.fuelColumns().get(SOURCE_FUEL);
            ReactorSnapshot secondUnsafe = secondFixture.withFuelColumn(SOURCE_FUEL,
                    new FuelColumnState(oldColumn.fuelAssembly(), oldColumn.integrity(),
                            7.0D, oldColumn.fuelBurnRemainder(), 0.0D));
            second.setSnapshot(secondUnsafe);
            BlockEvent.BreakEvent rejected = postBreak(helper, sharedWindow,
                    helper.makeMockPlayer(GameType.SURVIVAL));
            if (!require(helper, rejected.isCanceled(), "一个共享所有者不安全时错误放行了破坏")) {
                return;
            }
            if (!require(helper, first.snapshot().equals(firstBefore),
                    "共享所有者拒绝时先清空了安全的第一个反应堆")) {
                return;
            }
            if (!require(helper, second.snapshot().equals(secondUnsafe),
                    "共享所有者拒绝时改变了不安全的第二个反应堆")) {
                return;
            }
            helper.succeed();
        });
    }

    /** 安装带耐久、数据组件、库存、余数、损伤和卡死状态的完全停机快照。 */
    private static ReactorSnapshot installFullyStoppedFixture(
            ReactorInstrumentPortBlockEntity instrument
    ) {
        TreeMap<CoreColumnPosition, FuelColumnState> fuelColumns = new TreeMap<>();
        TreeMap<CoreColumnPosition, ControlRodColumnState> controlColumns = new TreeMap<>();
        int ordinal = 0;
        for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                : instrument.structureScan().columns().entrySet()) {
            if (entry.getValue().type() == ReactorStructureDefinition.ColumnType.CONTROL_ROD) {
                double actualDepth = entry.getKey().equals(NON_ADJACENT_CONTROL)
                        ? 0.75D : 1.0D;
                controlColumns.put(entry.getKey(), new ControlRodColumnState(
                        0.42D, 0.25D, actualDepth, true, 4.25D));
                continue;
            }
            ReactorPortBlockEntity port = findRefuelingPort(instrument, entry.getKey());
            ItemStack stored = freshFuel(100 + ordinal++, "MAINT03-" + entry.getKey());
            port.setFuelAssembly(stored);
            FuelAssemblyState assembly = FuelAssemblyItemCodec.readFreshFuel(stored);
            // 源燃料列四个相邻控制棒均完全插入，所以正式裂变计算的产热和计划燃耗为零；
            // 非零安全量化余热仅用于验证清空事务不会遗漏运行时字段。
            fuelColumns.put(entry.getKey(), new FuelColumnState(
                    assembly, 0.0D, 3.0D, 0.25D, 3.0D));
        }
        ReactorSnapshot snapshot = new ReactorSnapshot(
                fuelColumns,
                controlColumns,
                321L,
                654L,
                0L,
                false
        );
        instrument.setSnapshot(snapshot);
        return snapshot;
    }

    /** 构造一套包含控制棒列的有效结构，便于验证控制棒状态清空和重新初始化。 */
    private static void buildStructure(GameTestHelper helper, BlockPos origin) {
        buildStructure(helper, origin, Set.of(SOURCE_FUEL));
    }

    /** 按指定燃料列集合构造完整结构，供多端口原子性测试复用同一真实扫描契约。 */
    private static void buildStructure(
            GameTestHelper helper,
            BlockPos origin,
            Set<CoreColumnPosition> fuelColumns
    ) {
        TreeMap<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout = new TreeMap<>();
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                layout.put(new CoreColumnPosition(x, z),
                        fuelColumns.contains(new CoreColumnPosition(x, z))
                                ? ReactorStructureDefinition.ColumnType.FUEL
                                : ReactorStructureDefinition.ColumnType.CONTROL_ROD);
            }
        }
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.templateFor(layout).entrySet()) {
            helper.setBlock(origin.offset(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 在服务端测试保存路径中注入一个旧燃料迁移信封，模拟尚未完成的兼容迁移。 */
    private static void installLegacyMigrationEnvelope(
            GameTestHelper helper,
            ReactorInstrumentPortBlockEntity instrument
    ) {
        CompoundTag saved = instrument.saveForServerTest(helper.getLevel().registryAccess());
        CompoundTag snapshotTag = saved.getCompound("ReactorSnapshot");
        ListTag migration = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putInt("X", SOURCE_FUEL.x());
        entry.putInt("Z", SOURCE_FUEL.z());
        entry.putBoolean("Present", true);
        entry.putInt("Damage", 77);
        entry.putInt("MaxDamage", 216_000);
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
        return snapshotTag != null && snapshotTag.contains("LegacyFuelMigration", Tag.TAG_LIST);
    }

    /** 返回指定结构原点对应的仪表方块实体。 */
    private static ReactorInstrumentPortBlockEntity instrument(
            GameTestHelper helper,
            BlockPos origin
    ) {
        var blockEntity = helper.getLevel().getBlockEntity(
                helper.absolutePos(origin.offset(INSTRUMENT)));
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "没有找到停机重组测试仪表端口");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    /** 发布真实 NeoForge BreakEvent；调用方可再决定是否执行实际世界移除。 */
    private static BlockEvent.BreakEvent postBreak(
            GameTestHelper helper,
            BlockPos absolutePos,
            Player player
    ) {
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(
                helper.getLevel(), absolutePos,
                helper.getLevel().getBlockState(absolutePos), player);
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    /** 返回当前结构中指定列的唯一换料端口；查找只用于测试夹具，不替代生产读阶段枚举。 */
    private static ReactorPortBlockEntity findRefuelingPort(
            ReactorInstrumentPortBlockEntity instrument,
            CoreColumnPosition column
    ) {
        return instrument.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING).stream()
                .filter(port -> column.equals(port.boundColumn()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("缺少测试燃料端口 " + column));
    }

    /** 读取当前所有燃料端口的精确物品副本。 */
    private static Map<CoreColumnPosition, ItemStack> fuelItems(
            ReactorInstrumentPortBlockEntity instrument
    ) {
        Map<CoreColumnPosition, ItemStack> items = new TreeMap<>();
        for (ReactorPortBlockEntity port : instrument.boundPorts(
                ReactorPortBlockEntity.BindingType.REFUELING)) {
            items.put(port.boundColumn(), port.fuelAssembly());
        }
        return items;
    }

    /** 清空测试夹具中的全部燃料端口，便于独立生成不改变停机状态的有效遥测。 */
    private static void clearFuelItems(ReactorInstrumentPortBlockEntity instrument) {
        for (ReactorPortBlockEntity port : instrument.boundPorts(
                ReactorPortBlockEntity.BindingType.REFUELING)) {
            port.setFuelAssembly(ItemStack.EMPTY);
        }
    }

    /** 按列坐标恢复测试夹具中的完整燃料物品副本。 */
    private static void restoreFuelItems(
            ReactorInstrumentPortBlockEntity instrument,
            Map<CoreColumnPosition, ItemStack> items
    ) {
        for (ReactorPortBlockEntity port : instrument.boundPorts(
                ReactorPortBlockEntity.BindingType.REFUELING)) {
            port.setFuelAssembly(items.getOrDefault(port.boundColumn(), ItemStack.EMPTY));
        }
    }

    /** 比较物品 ID、耐久和全部数据组件。 */
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

    /** 断言完全清空结果不携带任何燃料、库存、融毁或运行时遥测。 */
    private static boolean requireCleared(
            GameTestHelper helper,
            ReactorInstrumentPortBlockEntity instrument,
            String message
    ) {
        boolean cleared = instrument.snapshot().equals(ReactorSnapshot.empty())
                && instrument.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING).stream()
                .allMatch(port -> port.fuelAssembly().isEmpty())
                && !instrument.telemetry().available();
        return require(helper, cleared, message);
    }

    /** 按正式注册 ID 将结构契约转换为真实方块。 */
    private static Block blockForId(String id) {
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
            default -> throw new IllegalArgumentException("未知停机重组方块 ID：" + id);
        };
    }

    /** 构造带原版耐久和自定义名称数据组件的单件新燃料。 */
    private static ItemStack freshFuel(int damage, String name) {
        ItemStack stack = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        stack.setDamageValue(damage);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    /** 保存方块状态的轻量副本，避免比较过程依赖可变世界对象。 */
    private record BlockStateSnapshot(net.minecraft.world.level.block.state.BlockState state) {
    }

    /** 游戏测试断言失败时停止当前测试，避免异步回调继续制造误导性结果。 */
    private static boolean require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
            return false;
        }
        return true;
    }
}
