package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderPayload;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderService;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderStatus;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorFissionCalculator;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentTelemetry;
import com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.TreeMap;

/** 验证真实成型结构中四向控制棒完全插入后的新生热、燃耗和换料遥测边界。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1ThermalGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final CoreColumnPosition FUEL_COLUMN = new CoreColumnPosition(1, 1);
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);

    private P1ThermalGameTests() {
    }

    /**
     * 在真实扫描、绑定和服务端正式 tick 链路中提交四根相邻可动控制棒的 100% 目标。
     * 缓存余热允许继续转化，但不得被误报为新生裂变热或燃耗。
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void fullyInsertedAdjacentControlRodsStopNewFissionHeat(GameTestHelper helper) {
        buildIsolatedFuelStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            Map<CoreColumnPosition, ControlRodColumnState> initialControls = new TreeMap<>();
            for (CoreColumnPosition position : cardinalNeighbours(FUEL_COLUMN)) {
                initialControls.put(position,
                        new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D));
            }
            instrument.setSnapshot(new ReactorSnapshot(
                    Map.of(FUEL_COLUMN, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, 0), 1.0D, 0.49D, 0.0D, 0.49D)),
                    initialControls,
                    1L,
                    0L,
                    0L,
                    false
            ));

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            long dragId = 100L;
            for (CoreColumnPosition position : cardinalNeighbours(FUEL_COLUMN)) {
                BlockPos drive = drivePosition(position);
                BlockPos absoluteDrive = helper.absolutePos(drive);
                player.setPos(absoluteDrive.getX() + 0.5D, absoluteDrive.getY() + 0.5D,
                        absoluteDrive.getZ() + 2.0D);
                var start = ControlRodSliderService.handle(player,
                        ControlRodSliderPayload.start(
                                absoluteDrive, position.x(), position.z(), 0, dragId));
                require(helper, start.status() == ControlRodSliderStatus.ACCEPTED,
                        "控制棒列 " + position + " 的 100% 提交未能开始拖动：" + start.reason());
                var committed = ControlRodSliderService.handle(player,
                        ControlRodSliderPayload.commit(
                                absoluteDrive, position.x(), position.z(), 100, dragId));
                require(helper, committed.status() == ControlRodSliderStatus.ACCEPTED,
                        "控制棒列 " + position + " 的 100% 目标提交失败：" + committed.reason());
                dragId++;
            }

            require(helper, instrument.tickControlRods(), "四根可动控制棒未应用 100% 目标深度");
            for (CoreColumnPosition position : cardinalNeighbours(FUEL_COLUMN)) {
                ControlRodColumnState state = instrument.snapshot().controlRodColumns().get(position);
                require(helper, state != null && state.targetDepth() == 1.0D
                                && state.actualDepth() == 1.0D,
                        "控制棒列 " + position + " 的目标/实际深度不是 1.0：" + state);
            }

            instrument.tickReactor();
            FuelColumnState fuel = instrument.snapshot().fuelColumns().get(FUEL_COLUMN);
            var fission = ReactorFissionCalculator.calculate(
                    instrument.snapshot(), ReactorSimulationParameters.defaults());
            ReactorInstrumentTelemetry.FuelColumnTelemetry telemetry = instrument.telemetry()
                    .fuelColumns().stream()
                    .filter(column -> column.position().equals(FUEL_COLUMN))
                    .findFirst()
                    .orElse(null);
            require(helper, fission.columns().get(FUEL_COLUMN).generatedHeatHu() == 0.0D,
                    "四根控制棒完全插入后仍产生新生裂变热");
            require(helper, fission.columns().get(FUEL_COLUMN).plannedFuelBurnUnits() == 0.0D,
                    "四根控制棒完全插入后仍产生燃耗");
            require(helper, instrument.currentFuelColumnFissionHeatHu(FUEL_COLUMN) == 0.0D,
                    "换料端口服务端新生裂变热不是零");
            require(helper, telemetry != null && telemetry.generatedFissionHeatHuPerTick() == 0.0D,
                    "正式 tick 遥测中的新生裂变热不是零：" + telemetry);
            require(helper, fuel != null && fuel.cachedHeatHu() == 0.49D
                            && fuel.quantizedHeatRemainderHu() == 0.49D,
                    "缓存余热未被保留为可区分的安全量化余数：" + fuel);

            ReactorPortBlockEntity refueling = refuelingPort(helper);
            CompoundTag update = refueling.getUpdateTag(helper.getLevel().registryAccess());
            require(helper, update.getBoolean("FuelColumnBound")
                            && update.getDouble("FuelColumnHeatHuPerTick") == 0.0D,
                    "换料端口遥测包没有在同步窗口内报告零新生裂变热");
            helper.succeed();
        });
    }

    /** 放置中心燃料列和四向控制棒列，角落列保持为空以构成孤立燃料场景。 */
    private static void buildIsolatedFuelStructure(GameTestHelper helper) {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> columns = new TreeMap<>();
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                CoreColumnPosition position = new CoreColumnPosition(x, z);
                columns.put(position, position.equals(FUEL_COLUMN)
                        ? ReactorStructureDefinition.ColumnType.FUEL
                        : isCardinal(position)
                        ? ReactorStructureDefinition.ColumnType.CONTROL_ROD
                        : ReactorStructureDefinition.ColumnType.EMPTY);
            }
        }
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.templateFor(columns).entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 返回中心列四向相邻坐标，顺序固定以便拖动会话和日志可复现。 */
    private static CoreColumnPosition[] cardinalNeighbours(CoreColumnPosition center) {
        return new CoreColumnPosition[]{
                new CoreColumnPosition(center.x(), center.z() - 1),
                new CoreColumnPosition(center.x() + 1, center.z()),
                new CoreColumnPosition(center.x(), center.z() + 1),
                new CoreColumnPosition(center.x() - 1, center.z())
        };
    }

    private static boolean isCardinal(CoreColumnPosition position) {
        return position.x() == FUEL_COLUMN.x() && Math.abs(position.z() - FUEL_COLUMN.z()) == 1
                || position.z() == FUEL_COLUMN.z() && Math.abs(position.x() - FUEL_COLUMN.x()) == 1;
    }

    /** 根据列坐标返回 5×5×5 结构顶部的控制棒驱动器局部位置。 */
    private static BlockPos drivePosition(CoreColumnPosition position) {
        return new BlockPos(position.x() + 1, 4, position.z() + 1);
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var entity = helper.getBlockEntity(INSTRUMENT);
        require(helper, entity instanceof ReactorInstrumentPortBlockEntity,
                "找不到真实成型结构的仪表端口方块实体");
        return (ReactorInstrumentPortBlockEntity) entity;
    }

    private static ReactorPortBlockEntity refuelingPort(GameTestHelper helper) {
        var entity = helper.getBlockEntity(new BlockPos(2, 4, 2));
        require(helper, entity instanceof ReactorPortBlockEntity,
                "找不到中心燃料列的换料端口方块实体");
        return (ReactorPortBlockEntity) entity;
    }

    /** 将模板中的注册 ID 映射为真实方块，确保测试使用成型和绑定的生产路径。 */
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
            default -> throw new IllegalArgumentException("未知结构方块 ID：" + id);
        };
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
