package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.block.ReactorInstrumentPortBlock;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDiagnostics;
import com.iksxh.create_nuclear_industry.structure.ReactorInstrumentStructureSummary;
import com.simibubi.create.AllItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/** 验证结构形成、静态仪表摘要、事件驱动重扫、扳手显式重扫和稳定诊断代码。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1StructureGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    // 仪表端口是标准模板北面中心槽位，也是结构原点推断的锚点。
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    // 西北下角是固定外壳边界，用于验证移除后失效、恢复后重建。
    private static final BlockPos OUTER_CASING = new BlockPos(0, 0, 0);

    private P1StructureGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void structureFormsOnLoadAndBuildsColumnCache(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(),
                    "experimental reactor was not recognized during block entity load");
            require(helper, instrument.structureOrigin().equals(helper.absolutePos(BlockPos.ZERO)),
                    "scanner returned the wrong local structure origin");
            require(helper, instrument.structureScan().columns().size() == 9,
                    "scanner did not cache all nine core column mappings");
            require(helper, instrument.structureScan().ports()
                            .get(ReactorStructureDefinition.PortType.INSTRUMENT).size() == 1,
                    "scanner did not cache the unique instrument port");
            require(helper, instrument.structureScan().ports()
                            .get(ReactorStructureDefinition.PortType.COLD_COOLANT).size() >= 1,
                    "scanner did not cache a cold coolant port");
            require(helper, instrument.structureScan().ports()
                            .get(ReactorStructureDefinition.PortType.HOT_COOLANT).size() >= 1,
                    "scanner did not cache a hot coolant port");

            long scansAfterLoad = instrument.structureScanCount();
            helper.runAfterDelay(20, () -> {
                require(helper, instrument.structureScanCount() == scansAfterLoad,
                        "structure cache was rescanned from a per-tick loop");

                instrument.setSnapshot(fixtureSnapshot());
                CompoundTag saved = instrument.saveForServerTest(helper.getLevel().registryAccess());
                helper.setBlock(INSTRUMENT, Blocks.AIR.defaultBlockState());
                helper.setBlock(INSTRUMENT, P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
                helper.runAfterDelay(5, () -> {
                    ReactorInstrumentPortBlockEntity reloaded = instrument(helper);
                    reloaded.loadForServerTest(saved, helper.getLevel().registryAccess());
                    require(helper, reloaded.structureValid(),
                            "instrument port did not rebuild the structure cache during block entity load");
                    require(helper, reloaded.snapshot().equals(fixtureSnapshot().withoutFuelAssemblies()),
                            "instrument port reload changed the authoritative snapshot");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void structureInvalidatesOnBreakRestoresOnPlacementAndPreservesState(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorSnapshot expected = fixtureSnapshot();
            instrument.setSnapshot(expected);

            BlockPos absoluteCasing = helper.absolutePos(OUTER_CASING);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(
                    helper.getLevel(), absoluteCasing, helper.getLevel().getBlockState(absoluteCasing), player);
            NeoForge.EVENT_BUS.post(breakEvent);
            require(helper, !breakEvent.isCanceled(), "structure casing break was unexpectedly blocked");
            helper.setBlock(OUTER_CASING, Blocks.AIR.defaultBlockState());

            helper.runAfterDelay(2, () -> {
                require(helper, !instrument.structureValid(),
                        "structure remained valid after an outer casing was removed");
                require(helper, instrument.snapshot().equals(expected),
                        "invalid structure scan overwrote the authoritative reactor snapshot");

                helper.setBlock(OUTER_CASING, P1Blocks.REACTOR_CASING.get().defaultBlockState());
                BlockSnapshot restored = BlockSnapshot.create(
                        helper.getLevel().dimension(), helper.getLevel(), absoluteCasing, 3);
                NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(
                        restored, Blocks.AIR.defaultBlockState(), player));

                helper.runAfterDelay(2, () -> {
                    require(helper, instrument.structureValid(),
                            "structure did not revalidate after the missing casing was restored");
                    require(helper, instrument.snapshot().equals(expected),
                            "structure restoration changed the authoritative reactor snapshot");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void wrenchRequestsRescanAfterDirectComponentMutation(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            long initialScans = instrument.structureScanCount();
            helper.setBlock(OUTER_CASING, Blocks.AIR.defaultBlockState());
            require(helper, instrument.structureValid(),
                    "direct mutation should not silently change the cached result before a rescan request");

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, AllItems.WRENCH.asStack());
            BlockPos absoluteInstrument = helper.absolutePos(INSTRUMENT);
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(absoluteInstrument), Direction.UP, absoluteInstrument, false);
            InteractionResult result = ((ReactorInstrumentPortBlock) P1Blocks.REACTOR_INSTRUMENT_PORT.get())
                    .onWrenched(helper.getLevel().getBlockState(absoluteInstrument),
                            new net.minecraft.world.item.context.UseOnContext(
                                    player, InteractionHand.MAIN_HAND, hit));
            require(helper, result.consumesAction(), "Create wrench did not consume the rescan request");
            require(helper, !instrument.structureValid(),
                    "wrench rescan did not invalidate the mutated structure");
            require(helper, instrument.structureScanCount() > initialScans,
                    "wrench rescan did not increment the explicit scan counter");

            helper.setBlock(OUTER_CASING, P1Blocks.REACTOR_CASING.get().defaultBlockState());
            ((ReactorInstrumentPortBlock) P1Blocks.REACTOR_INSTRUMENT_PORT.get())
                    .onWrenched(helper.getLevel().getBlockState(absoluteInstrument),
                            new net.minecraft.world.item.context.UseOnContext(
                                    player, InteractionHand.MAIN_HAND, hit));
            require(helper, instrument.structureValid(),
                    "wrench rescan did not recognize the restored structure");
            long scansAfterWrench = instrument.structureScanCount();
            helper.runAfterDelay(20, () -> {
                require(helper, instrument.structureScanCount() == scansAfterWrench,
                        "structure cache changed without a lifecycle or wrench trigger");
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void wrenchFeedbackReportsCountsAndStableFailureCodes(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorStructureDefinition.ScanResult initial = instrument.structureScan();
            require(helper, initial.valid(), "canonical structure was not valid before wrench feedback test");
            require(helper, initial.diagnosticCode() == ReactorStructureDefinition.DiagnosticCode.VALID,
                    "valid scan did not expose the success diagnostic code");
            require(helper, initial.columns().values().stream()
                            .filter(column -> column.type() == ReactorStructureDefinition.ColumnType.FUEL).count() == 8,
                    "success scan did not count eight fuel columns");
            require(helper, initial.columns().values().stream()
                            .filter(column -> column.type() == ReactorStructureDefinition.ColumnType.EMPTY).count() == 1,
                    "success scan did not count one empty column");
            require(helper, initial.ports().get(ReactorStructureDefinition.PortType.COLD_COOLANT).size() == 1,
                    "success scan did not count the cold port");
            require(helper, initial.ports().get(ReactorStructureDefinition.PortType.HOT_COOLANT).size() == 1,
                    "success scan did not count the hot port");
            require(helper, ReactorStructureDiagnostics.message(initial) != null,
                    "success scan did not produce a translated feedback component");

            ReactorSnapshot expected = fixtureSnapshot();
            instrument.setSnapshot(expected);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack wrench = AllItems.WRENCH.asStack();

            helper.setBlock(OUTER_CASING, Blocks.AIR.defaultBlockState());
            wrench(helper, player, wrench);
            require(helper, instrument.structureScan().diagnosticCode()
                            == ReactorStructureDefinition.DiagnosticCode.STRUCTURE_BLOCKS,
                    "missing casing did not produce the structure-block diagnostic");
            require(helper, instrument.snapshot().equals(expected),
                    "structure diagnostic changed the authoritative snapshot");

            helper.setBlock(OUTER_CASING, P1Blocks.REACTOR_CASING.get().defaultBlockState());
            helper.setBlock(local(ReactorStructureDefinition.DEFAULT_COLD_PORT_POSITION),
                    P1Blocks.REACTOR_CASING.get().defaultBlockState());
            wrench(helper, player, wrench);
            require(helper, instrument.structureScan().diagnosticCode()
                            == ReactorStructureDefinition.DiagnosticCode.MISSING_COLD_PORT,
                    "missing cold port did not produce the cold-port diagnostic");

            helper.setBlock(local(ReactorStructureDefinition.DEFAULT_COLD_PORT_POSITION),
                    P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState());
            helper.setBlock(local(ReactorStructureDefinition.DEFAULT_HOT_PORT_POSITION),
                    P1Blocks.REACTOR_CASING.get().defaultBlockState());
            wrench(helper, player, wrench);
            require(helper, instrument.structureScan().diagnosticCode()
                            == ReactorStructureDefinition.DiagnosticCode.MISSING_HOT_PORT,
                    "missing hot port did not produce the hot-port diagnostic");

            helper.setBlock(local(ReactorStructureDefinition.DEFAULT_HOT_PORT_POSITION),
                    P1Blocks.REACTOR_HOT_PORT.get().defaultBlockState());
            BlockPos emptyBody = new BlockPos(2, 1, 2);
            helper.setBlock(emptyBody, P1Blocks.REACTOR_CASING.get().defaultBlockState());
            wrench(helper, player, wrench);
            require(helper, instrument.structureScan().diagnosticCode()
                            == ReactorStructureDefinition.DiagnosticCode.COLUMN_LAYOUT,
                    "wrong empty-column body did not produce the column-layout diagnostic");

            helper.setBlock(emptyBody, Blocks.AIR.defaultBlockState());
            wrench(helper, player, wrench);
            require(helper, instrument.structureScan().diagnosticCode()
                            == ReactorStructureDefinition.DiagnosticCode.VALID,
                    "repaired structure did not produce the success diagnostic");
            require(helper, instrument.snapshot().equals(expected),
                    "repair diagnostic changed the authoritative snapshot");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void wrenchRequiresWrenchAndPerformsOneServerScan(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            long initialScans = instrument.structureScanCount();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            helper.setBlock(OUTER_CASING, Blocks.AIR.defaultBlockState());

            InteractionResult withoutWrench = wrench(helper, player, ItemStack.EMPTY);
            require(helper, withoutWrench == InteractionResult.PASS,
                    "instrument port accepted a non-wrench interaction as a diagnostic request");
            require(helper, instrument.structureScanCount() == initialScans,
                    "non-wrench interaction changed the server scan cache");
            require(helper, instrument.structureValid(),
                    "non-wrench interaction changed the cached validity");

            InteractionResult withWrench = wrench(helper, player, AllItems.WRENCH.asStack());
            require(helper, withWrench.consumesAction(), "wrench interaction was not consumed");
            require(helper, instrument.structureScanCount() == initialScans + 1,
                    "one wrench interaction performed more than one server scan");
            require(helper, instrument.structureScan().diagnosticCode()
                            == ReactorStructureDefinition.DiagnosticCode.STRUCTURE_BLOCKS,
                    "single wrench scan did not publish the structure-block diagnostic");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void instrumentStaticSummaryUsesCachedStructureAndInvalidatesAfterRescan(
            GameTestHelper helper
    ) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorInstrumentStructureSummary initial = instrument.structureSummary();
            require(helper, initial.valid(), "canonical structure did not produce a valid static summary");
            require(helper, initial.width() == 5 && initial.height() == 5 && initial.depth() == 5,
                    "static summary reported incorrect fixed structure dimensions");
            require(helper, initial.fuelColumnCount() == 8 && initial.controlRodColumnCount() == 0,
                    "static summary reported incorrect canonical column counts");
            require(helper, initial.coldPortCount() == 1 && initial.hotPortCount() == 1,
                    "static summary reported incorrect canonical coolant port counts");
            require(helper, initial.totalFluidCapacityMb() == 2_000L,
                    "static summary did not add the configured cold and hot capacities");

            long scansAfterSummary = instrument.structureScanCount();
            require(helper, instrument.structureSummary().equals(initial),
                    "repeated static summary reads changed the cached result");
            require(helper, instrument.structureScanCount() == scansAfterSummary,
                    "static summary read triggered a world structure scan");

            helper.setBlock(OUTER_CASING, Blocks.AIR.defaultBlockState());
            require(helper, instrument.structureSummary().valid(),
                    "direct world mutation unexpectedly changed the cached summary");
            wrench(helper, helper.makeMockPlayer(GameType.SURVIVAL), AllItems.WRENCH.asStack());
            ReactorInstrumentStructureSummary invalid = instrument.structureSummary();
            require(helper, !invalid.valid() && !invalid.unavailableReason().isBlank(),
                    "invalid structure did not produce an unavailable summary");

            helper.setBlock(OUTER_CASING, P1Blocks.REACTOR_CASING.get().defaultBlockState());
            wrench(helper, helper.makeMockPlayer(GameType.SURVIVAL), AllItems.WRENCH.asStack());
            require(helper, instrument.structureSummary().valid(),
                    "restored structure did not rebuild the static summary");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void instrumentStaticSummarySyncsInReadOnlyGoggleTooltip(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            CompoundTag updateTag = instrument.getUpdateTag(helper.getLevel().registryAccess());
            instrument.handleUpdateTag(updateTag, helper.getLevel().registryAccess());
            List<Component> tooltip = new ArrayList<>();
            long scansBeforeTooltip = instrument.structureScanCount();
            ReactorSnapshot snapshotBeforeTooltip = instrument.snapshot();

            require(helper, instrument.addToGoggleTooltip(tooltip, false),
                    "valid static summary was not accepted by the Create goggle callback");
            require(helper, tooltip.size() == 12,
                    "valid goggle summary did not contain the static fields and runtime section: size="
                            + tooltip.size() + ", texts=" + tooltip.stream().map(Component::getString).toList());
            String[] expectedValues = {"5 × 5 × 5", "8", "0", "1", "1", "2000"};
            for (int index = 0; index < expectedValues.length; index++) {
                require(helper, tooltip.get(index + 1).getString().contains(expectedValues[index]),
                        "goggle summary field " + index + " was missing or out of order");
            }
            require(helper, tooltip.get(7).getString().contains("dynamic_summary")
                            || tooltip.get(7).getString().contains("动态运行遥测")
                            || tooltip.get(7).getString().contains("Dynamic runtime telemetry"),
                    "valid goggle summary did not contain the dynamic section header");
            require(helper, tooltip.get(8).getString().contains("0 / 1000"),
                    "valid goggle summary did not show the current cold stock and capacity");
            require(helper, tooltip.get(9).getString().contains("0 / 1000"),
                    "valid goggle summary did not show the current hot stock and capacity");
            require(helper, tooltip.get(10).getString().contains("0.0 HU/t"),
                    "valid goggle summary did not show zero fission heat");
            require(helper, tooltip.get(11).getString().contains("0.0 mB/t"),
                    "valid goggle summary did not show zero coolant conversion");
            require(helper, instrument.snapshot().equals(snapshotBeforeTooltip),
                    "goggle rendering changed the authoritative reactor snapshot");
            require(helper, instrument.structureScanCount() == scansBeforeTooltip,
                    "goggle rendering triggered a structure scan");

            helper.setBlock(OUTER_CASING, Blocks.AIR.defaultBlockState());
            wrench(helper, helper.makeMockPlayer(GameType.SURVIVAL), AllItems.WRENCH.asStack());
            instrument.handleUpdateTag(
                    instrument.getUpdateTag(helper.getLevel().registryAccess()),
                    helper.getLevel().registryAccess());
            tooltip.clear();
            require(helper, instrument.addToGoggleTooltip(tooltip, false),
                    "invalid static summary was not accepted by the Create goggle callback");
            require(helper, tooltip.size() == 2,
                    "invalid structure displayed zero-valued fields instead of an unavailable line");
            helper.succeed();
        });
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "expected instrument port block entity at " + INSTRUMENT + ", got " + blockEntity);
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    /** 使用带损伤、缓存热量、冷/热库存和融毁进度的快照验证重扫不改状态。 */
    private static ReactorSnapshot fixtureSnapshot() {
        return new ReactorSnapshot(
                Map.of(new CoreColumnPosition(0, 0),
                        new FuelColumnState(FuelAssemblyState.installed(216_000, 12_345), 0.72D, 18.5D)),
                Map.of(new CoreColumnPosition(1, 1),
                        new ControlRodColumnState(0.91D, 0.63D, 0.63D, false, 4.25D)),
                4_096L,
                512L,
                27L,
                true
        );
    }

    /** 按结构契约生成 5×5×5 标准模板，坐标由契约统一提供。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

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
            default -> throw new IllegalArgumentException("unknown canonical structure block " + id);
        };
    }

    private static InteractionResult wrench(GameTestHelper helper, Player player, ItemStack item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        BlockPos absoluteInstrument = helper.absolutePos(INSTRUMENT);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absoluteInstrument), Direction.UP, absoluteInstrument, false);
        return ((ReactorInstrumentPortBlock) P1Blocks.REACTOR_INSTRUMENT_PORT.get())
                .onWrenched(helper.getLevel().getBlockState(absoluteInstrument),
                        new net.minecraft.world.item.context.UseOnContext(
                                player, InteractionHand.MAIN_HAND, hit));
    }

    private static BlockPos local(ReactorStructureDefinition.LocalPosition position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
