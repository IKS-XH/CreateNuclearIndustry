package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentTelemetry;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorInstrumentStructureSummary;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 验证 P1-GOGGLE-INSTRUMENT-04 的运行态等待、动态显示顺序、单位和只读边界。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1GoggleInstrument04GameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final CoreColumnPosition FUEL_COLUMN = new CoreColumnPosition(0, 0);
    private static final CoreColumnPosition FIRST_CONTROL_ROD = new CoreColumnPosition(2, 0);
    private static final CoreColumnPosition SECOND_CONTROL_ROD = new CoreColumnPosition(0, 1);

    private P1GoggleInstrument04GameTests() {
    }

    /** 有效结构在首次正式 tick 前必须明确显示等待态，不能显示陈旧或伪造的运行值。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void validInstrumentShowsWaitingRuntimeDataBeforeFirstTick(GameTestHelper helper) {
        buildStructure(helper, ReactorStructureDefinition.canonicalTemplate());
        ReactorInstrumentPortBlockEntity instrument = instrument(helper);
        ReactorStructureLifecycle.rescanInstrumentPortNow(
                helper.getLevel(), instrument.getBlockPos());
        CompoundTag updateTag = instrument.getUpdateTag(helper.getLevel().registryAccess());
        instrument.handleUpdateTag(updateTag, helper.getLevel().registryAccess());

        List<Component> tooltip = new ArrayList<>();
        require(helper, instrument.addToGoggleTooltip(tooltip, false),
                "valid instrument did not accept the goggle callback before the first tick");
        require(helper, tooltip.size() == 9,
                "pre-tick goggle output did not contain static fields and runtime waiting state: size="
                        + tooltip.size() + ", texts=" + tooltip.stream().map(Component::getString).toList());
        require(helper, containsAny(tooltip.get(7), "dynamic_summary", "动态运行遥测",
                        "Dynamic runtime telemetry"),
                "pre-tick goggle output missed the dynamic section header");
        require(helper, containsAny(tooltip.get(8), "runtime_data_waiting", "等待首次成功运行数据",
                        "Waiting for the first successful runtime tick"),
                "pre-tick goggle output exposed runtime values instead of waiting state");
        helper.succeed();
    }

    /** 正式 tick 后按显示归属显示全堆数据、燃料列数据和控制棒列数据，并保持读取只读。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void validInstrumentDisplaysSyncedDynamicTelemetry(GameTestHelper helper) {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> columns =
                new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        columns.put(FIRST_CONTROL_ROD, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        columns.put(SECOND_CONTROL_ROD, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        buildStructure(helper, ReactorStructureDefinition.templateFor(columns));

        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "multi-column reactor did not form");
            instrument.setSnapshot(new ReactorSnapshot(
                    Map.of(FUEL_COLUMN, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)),
                    Map.of(
                            FIRST_CONTROL_ROD,
                            new ControlRodColumnState(0.25D, 1.0D, 0.0D, false, 0.0D),
                            SECOND_CONTROL_ROD,
                            new ControlRodColumnState(0.75D, 1.0D, 0.0D, false, 0.0D)),
                    128L,
                    0L,
                    0L,
                    false
            ));
            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), instrument.getBlockPos());
            instrument.tickReactor();
            require(helper, instrument.telemetry().available(),
                    "formal tick did not produce dynamic telemetry: snapshot=" + instrument.snapshot());

            ReactorInstrumentTelemetry telemetry = instrument.telemetry();
            require(helper, telemetry.available(), "formal tick telemetry was unavailable");
            ReactorInstrumentStructureSummary summary = instrument.structureSummary();
            require(helper, summary.coldInventoryCapacityMb() == 1_000L
                            && summary.hotInventoryCapacityMb() == 1_000L,
                    "client display source did not expose the separate configured capacities");

            instrument.handleUpdateTag(
                    instrument.getUpdateTag(helper.getLevel().registryAccess()),
                    helper.getLevel().registryAccess());
            ReactorPortBlockEntity refuelingPort = port(helper, new BlockPos(1, 4, 1));
            refuelingPort.handleUpdateTag(
                    refuelingPort.getUpdateTag(helper.getLevel().registryAccess()),
                    helper.getLevel().registryAccess());
            ControlRodDriveBlockEntity firstControlRodDrive = controlRodDrive(
                    helper, new BlockPos(3, 4, 1));
            firstControlRodDrive.handleUpdateTag(
                    firstControlRodDrive.getUpdateTag(helper.getLevel().registryAccess()),
                    helper.getLevel().registryAccess());
            ControlRodDriveBlockEntity secondControlRodDrive = controlRodDrive(
                    helper, new BlockPos(1, 4, 2));
            secondControlRodDrive.handleUpdateTag(
                    secondControlRodDrive.getUpdateTag(helper.getLevel().registryAccess()),
                    helper.getLevel().registryAccess());
            ReactorSnapshot snapshotBeforeTooltip = instrument.snapshot();
            long scansBeforeTooltip = instrument.structureScanCount();
            List<Component> tooltip = new ArrayList<>();
            require(helper, instrument.addToGoggleTooltip(tooltip, false),
                    "valid instrument did not accept the goggle callback after the first tick");
            require(helper, tooltip.size() == 12,
                    "instrument goggle output must contain only whole-reactor rows after static summary");
            require(helper, containsAny(tooltip.get(7), "dynamic_summary", "动态运行遥测",
                            "Dynamic runtime telemetry"),
                    "dynamic goggle output was not grouped after static summary");
            require(helper, tooltip.get(8).getString().contains(Long.toString(telemetry.coldCoolantMb()))
                            && tooltip.get(8).getString().contains("1000"),
                    "cold inventory row did not show current and configured capacity");
            require(helper, tooltip.get(9).getString().contains(Long.toString(telemetry.hotCoolantMb()))
                            && tooltip.get(9).getString().contains("1000"),
                    "hot inventory row did not show current and configured capacity");
            require(helper, tooltip.get(10).getString().contains("HU/t"),
                    "total fission heat row did not include HU/t");
            require(helper, tooltip.get(11).getString().contains("mB/t"),
                    "coolant conversion row did not include mB/t");
            require(helper, tooltip.subList(7, tooltip.size()).stream().noneMatch(text ->
                            text.getString().contains("Fuel column")
                                    || text.getString().contains("燃料列 行")
                                    || text.getString().contains("Control-rod column")
                                    || text.getString().contains("控制棒列 行")),
                    "instrument goggle output still contains per-column rows");

            List<Component> fuelTooltip = new ArrayList<>();
            require(helper, refuelingPort.addToGoggleTooltip(fuelTooltip, false),
                    "bound refueling port did not accept the goggle callback");
            require(helper, fuelTooltip.size() == 2
                            && containsAny(fuelTooltip.get(0), "fuel_column_summary", "燃料列运行信息",
                            "Fuel column runtime")
                            && fuelTooltip.get(1).getString().contains("100.0%")
                            && fuelTooltip.get(1).getString().contains("HU/t"),
                    "refueling port did not show its column integrity and heat");

            List<Component> firstControlRodTooltip = new ArrayList<>();
            require(helper, firstControlRodDrive.addToGoggleTooltip(firstControlRodTooltip, false),
                    "first control-rod drive did not accept the goggle callback");
            require(helper, firstControlRodTooltip.size() == 2
                            && firstControlRodTooltip.get(1).getString().contains("1")
                            && firstControlRodTooltip.get(1).getString().contains("3")
                            && firstControlRodTooltip.get(1).getString().contains("25.0%"),
                    "first control-rod drive did not show its one-based column integrity");

            List<Component> secondControlRodTooltip = new ArrayList<>();
            require(helper, secondControlRodDrive.addToGoggleTooltip(secondControlRodTooltip, false),
                    "second control-rod drive did not accept the goggle callback");
            require(helper, secondControlRodTooltip.size() == 2
                            && secondControlRodTooltip.get(1).getString().contains("2")
                            && secondControlRodTooltip.get(1).getString().contains("1")
                            && secondControlRodTooltip.get(1).getString().contains("75.0%"),
                    "second control-rod drive did not show its one-based column integrity");
            require(helper, instrument.snapshot().equals(snapshotBeforeTooltip),
                    "dynamic goggle rendering changed the authoritative snapshot");
            require(helper, instrument.structureScanCount() == scansBeforeTooltip,
                    "dynamic goggle rendering triggered a structure scan");
            helper.succeed();
        });
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "expected reactor instrument port block entity");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    private static ReactorPortBlockEntity port(GameTestHelper helper, BlockPos position) {
        var blockEntity = helper.getBlockEntity(position);
        require(helper, blockEntity instanceof ReactorPortBlockEntity,
                "expected reactor port block entity at " + position);
        return (ReactorPortBlockEntity) blockEntity;
    }

    private static ControlRodDriveBlockEntity controlRodDrive(
            GameTestHelper helper,
            BlockPos position
    ) {
        var blockEntity = helper.getBlockEntity(position);
        require(helper, blockEntity instanceof ControlRodDriveBlockEntity,
                "expected control-rod drive block entity at " + position);
        return (ControlRodDriveBlockEntity) blockEntity;
    }

    private static void buildStructure(
            GameTestHelper helper,
            Map<ReactorStructureDefinition.LocalPosition, String> template
    ) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry : template.entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

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
            default -> throw new IllegalArgumentException("unknown reactor block " + id);
        };
    }

    private static boolean containsAny(Component component, String... values) {
        String text = component.getString();
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /** 将异步 GameTest 断言统一转为明确的测试失败。 */
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
