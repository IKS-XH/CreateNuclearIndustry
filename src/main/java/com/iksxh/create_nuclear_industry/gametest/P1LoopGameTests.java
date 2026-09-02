package com.iksxh.create_nuclear_industry.gametest;

import com.mojang.authlib.GameProfile;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorFissionCalculator;
import com.iksxh.create_nuclear_industry.reactor.ReactorFissionResult;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentTelemetry;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentTelemetryNbtCodec;
import com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import io.netty.channel.embedded.EmbeddedChannel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 正式 P1 服务端反应堆循环的 GameTest 回归夹具。
 *
 * <p>测试使用与结构扫描器相同的本地坐标和方块 ID，在真实 GameTest 服务端中验证无效结构拒绝
 * 推进、裂变—冷却—燃耗—持久化顺序，以及 SCRAM 对相邻和非相邻燃料列的边界影响。</p>
 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1LoopGameTests {
    /** 提供空世界的最小模板；正式结构由测试方法按契约坐标显式放置。 */
    private static final String TEMPLATE = "p0_probe_empty";
    /** 仪表端口在 5×5×5 结构中的本地锚点，也是权威快照的读取入口。 */
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    /** 用于单列回归的堆芯坐标，采用固定 3×3 内部坐标系。 */
    private static final CoreColumnPosition TEST_COLUMN = new CoreColumnPosition(0, 0);

    private P1LoopGameTests() {
    }

    /** 无效结构不得推进服务端权威快照，避免未成形反应堆产生热量或燃耗。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void unformedReactorDoesNotRun(GameTestHelper helper) {
        helper.setBlock(INSTRUMENT, P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorSnapshot before = ReactorSnapshot.singleFuelColumn(
                    TEST_COLUMN,
                    new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)
            );
            instrument.setSnapshot(before);
            require(helper, !instrument.tickReactor(), "an unformed reactor advanced its state");
            require(helper, instrument.snapshot().equals(before),
                    "an unformed reactor changed authoritative state");
            helper.succeed();
        });
    }

    /** 验证完整结构的一次 tick 会结算裂变、冷却、燃耗、损伤并可经 NBT 恢复。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void formedReactorRunsFissionCoolingBurnAndReload(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "canonical reactor did not form");
            ReactorSnapshot before = new ReactorSnapshot(
                    Map.of(TEST_COLUMN, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)),
                    Map.of(),
                    128L,
                    0L,
                    0L,
                    false
            );
            instrument.setSnapshot(before);
            require(helper, instrument.tickReactor(), "formed reactor did not advance on the server tick");

            ReactorSnapshot after = instrument.snapshot();
            require(helper, after.fuelColumns().get(TEST_COLUMN).fuelAssembly().damage() > 0,
                    "server tick did not commit fuel burn");
            require(helper, after.hotCoolantMb() > 0L && after.coldCoolantMb() < before.coldCoolantMb(),
                    "server tick did not settle cold-to-hot coolant conversion");
            require(helper, after.fuelColumns().get(TEST_COLUMN).integrity() == 1.0D,
                    "sufficient cooling damaged a fuel column");

            CompoundTag saved = instrument.saveForServerTest(helper.getLevel().registryAccess());
            ReactorInstrumentPortBlockEntity reloaded = new ReactorInstrumentPortBlockEntity(
                    helper.absolutePos(INSTRUMENT),
                    P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
            reloaded.loadForServerTest(saved, helper.getLevel().registryAccess());
            require(helper, reloaded.snapshot().equals(after),
                    "formal tick state did not survive NBT reload");
            helper.succeed();
        });
    }

    /** 验证正式 tick 生成动态遥测、更新包只读同步且遥测不写入权威快照 NBT。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void formedReactorPublishesReadOnlyDynamicTelemetry(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorSnapshot before = ReactorSnapshot.singleFuelColumn(
                    TEST_COLUMN,
                    new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)
            );
            instrument.setSnapshot(before);
            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), instrument.getBlockPos());
            require(helper, !instrument.telemetry().available(),
                    "structure rescan did not invalidate the previous telemetry");
            require(helper, instrument.tickReactor(),
                    "formed reactor did not complete a telemetry-producing formal tick");

            ReactorInstrumentTelemetry telemetry = instrument.telemetry();
            require(helper, telemetry.available(), "successful formal tick did not publish telemetry");
            require(helper, telemetry.fuelColumns().size() == 1,
                    "telemetry did not expose the authoritative fuel column set");
            require(helper, telemetry.totalGeneratedFissionHeatHuPerTick()
                            == telemetry.fuelColumns().stream()
                            .mapToDouble(ReactorInstrumentTelemetry.FuelColumnTelemetry
                                    ::generatedFissionHeatHuPerTick)
                            .sum(),
                    "whole-reactor heat did not equal the sum of column heat values");
            require(helper, telemetry.totalGeneratedFissionHeatHuPerTick()
                            == ReactorFissionCalculator.calculate(before,
                            ReactorSimulationParameters.defaults()).generatedHeatHu(),
                    "telemetry included cached residual heat in new fission heat");

            CompoundTag update = instrument.getUpdateTag(helper.getLevel().registryAccess());
            instrument.handleUpdateTag(update, helper.getLevel().registryAccess());
            require(helper, instrument.clientTelemetry().equals(telemetry),
                    "client update tag did not reproduce the last dynamic telemetry");

            CompoundTag saved = instrument.saveForServerTest(helper.getLevel().registryAccess());
            require(helper, !saved.contains("InstrumentTelemetry"),
                    "dynamic telemetry was persisted as a second reactor state");
            helper.succeed();
        });
    }

    /** 验证区块重载只恢复权威快照，首个成功正式 tick 前不会复用旧动态遥测。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void reloadedInstrumentTelemetryUnavailableUntilFirstSuccessfulTick(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            instrument.setSnapshot(snapshotWithFuelAndColdCoolant());
            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), instrument.getBlockPos());
            require(helper, instrument.tickReactor(), "initial formal tick did not create telemetry");
            require(helper, instrument.telemetry().available(), "initial telemetry was unavailable");

            CompoundTag saved = instrument.saveForServerTest(helper.getLevel().registryAccess());
            require(helper, !saved.contains("InstrumentTelemetry"),
                    "chunk reload save unexpectedly persisted dynamic telemetry");

            helper.setBlock(INSTRUMENT, Blocks.AIR.defaultBlockState());
            helper.setBlock(INSTRUMENT, P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
            ReactorInstrumentPortBlockEntity reloaded = instrument(helper);
            reloaded.loadForServerTest(saved, helper.getLevel().registryAccess());
            require(helper, !reloaded.telemetry().available(),
                    "chunk reload exposed stale server telemetry before a new formal tick");
            require(helper, !reloaded.clientTelemetry().available(),
                    "chunk reload exposed stale client telemetry before a new formal tick");

            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), reloaded.getBlockPos());
            require(helper, reloaded.structureValid(),
                    "reloaded structure was not available for the first post-load tick");
            require(helper, !reloaded.telemetry().available(),
                    "structure rescan restored telemetry before the first post-load tick");
            require(helper, reloaded.tickReactor(),
                    "first successful post-load formal tick did not complete");
            require(helper, reloaded.telemetry().available(),
                    "telemetry did not recover after the first successful post-load tick");
            helper.succeed();
        });
    }

    /** 验证多个控制棒列按先 z 后 x 排序，并同步正式 tick 后的各列完整度。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void multipleControlRodColumnsAreSortedWithPostTickIntegrity(GameTestHelper helper) {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> columns =
                new java.util.HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        CoreColumnPosition first = new CoreColumnPosition(2, 0);
        CoreColumnPosition second = new CoreColumnPosition(0, 1);
        columns.put(first, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        columns.put(second, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        buildStructure(helper, ReactorStructureDefinition.templateFor(columns));

        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "multi-control-rod structure did not form");
            instrument.setSnapshot(new ReactorSnapshot(
                    Map.of(),
                    Map.of(
                            first, new ControlRodColumnState(0.25D, 1.0D, 1.0D, false, 0.0D),
                            second, new ControlRodColumnState(0.75D, 1.0D, 1.0D, false, 0.0D)
                    ),
                    0L, 0L, 0L, false
            ));
            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), instrument.getBlockPos());
            instrument.tickReactor();

            ReactorInstrumentTelemetry telemetry = instrument.telemetry();
            require(helper, telemetry.available(), "multi-control-rod telemetry was unavailable");
            require(helper, telemetry.controlRodColumns().size() == 2,
                    "telemetry omitted one of the control rod columns");
            List<CoreColumnPosition> positions = telemetry.controlRodColumns().stream()
                    .map(ReactorInstrumentTelemetry.ControlRodColumnTelemetry::position)
                    .toList();
            require(helper, positions.equals(List.of(first, second)),
                    "control rod columns were not sorted by z then x: " + positions);
            require(helper, telemetry.controlRodColumns().get(0).controlRodColumnIntegrity() == 0.25D,
                    "first control rod integrity did not come from the post-tick snapshot");
            require(helper, telemetry.controlRodColumns().get(1).controlRodColumnIntegrity() == 0.75D,
                    "second control rod integrity did not come from the post-tick snapshot");
            helper.succeed();
        });
    }

    /** 验证实际更新包在服务端正式 tick 后十个游戏 tick内抵达模拟客户端连接。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void dynamicTelemetryPacketReachesClientWithinTenTicks(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ServerPlayer player = createUnloggedMockClient(helper);
            BlockPos absoluteInstrument = helper.absolutePos(INSTRUMENT);
            player.moveTo(absoluteInstrument.getX() + 0.5D,
                    absoluteInstrument.getY() + 0.5D,
                    absoluteInstrument.getZ() + 0.5D);
            trackMockClient(helper, player);
            ChunkPos instrumentChunk = new ChunkPos(absoluteInstrument);
            player.setChunkTrackingView(ChunkTrackingView.of(instrumentChunk, 10));
            player.connection.chunkSender.sendNextChunks(player);
            require(helper, helper.getLevel().getChunkSource().chunkMap
                            .getPlayers(instrumentChunk, false).contains(player),
                    "embedded client was not tracking the instrument chunk: view="
                            + player.getChunkTrackingView().contains(instrumentChunk)
                            + ", pending=" + player.connection.chunkSender.isPending(instrumentChunk.toLong()));
            EmbeddedChannel channel = (EmbeddedChannel) player.connection.getConnection().channel();
            drainOutbound(channel);

            instrument.setSnapshot(snapshotWithFuelAndColdCoolant());
            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), instrument.getBlockPos());
            drainOutbound(channel);
            long startTick = helper.getTick();
            helper.onEachTick(() -> {
                if (hasAvailableTelemetryPacket(channel, absoluteInstrument)) {
                    require(helper, helper.getTick() - startTick <= 10L,
                            "client telemetry update exceeded the ten-tick visibility bound");
                    helper.succeed();
                } else if (helper.getTick() - startTick > 10L) {
                    helper.fail("client telemetry update did not arrive within ten ticks");
                }
            });
        });
    }

    /** 验证重复读取服务端和客户端遥测只读副本不会触发新的结构扫描。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void repeatedTelemetryReadsDoNotRescanStructure(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            instrument.setSnapshot(snapshotWithFuelAndColdCoolant());
            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), instrument.getBlockPos());
            require(helper, instrument.tickReactor(), "formal tick did not create telemetry for read test");
            long scansBeforeReads = instrument.structureScanCount();
            ReactorSnapshot snapshotBeforeReads = instrument.snapshot();
            ReactorInstrumentTelemetry telemetryBeforeReads = instrument.telemetry();

            for (int index = 0; index < 100; index++) {
                require(helper, instrument.telemetry().equals(telemetryBeforeReads),
                        "repeated server telemetry read changed the value");
                instrument.clientTelemetry();
            }
            require(helper, instrument.structureScanCount() == scansBeforeReads,
                    "repeated telemetry reads triggered a structure scan");
            require(helper, instrument.snapshot().equals(snapshotBeforeReads),
                    "repeated telemetry reads changed the authoritative snapshot");
            helper.succeed();
        });
    }

    /** 验证结构失效立即清空旧遥测，修复重扫后需等待下一次正式 tick 才恢复。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void structureInvalidationClearsTelemetryUntilNextSuccessfulTick(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            instrument.setSnapshot(ReactorSnapshot.singleFuelColumn(
                    TEST_COLUMN,
                    new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)
            ));
            require(helper, instrument.tickReactor(), "formed reactor did not publish initial telemetry");
            require(helper, instrument.telemetry().available(), "initial telemetry was unavailable");

            helper.setBlock(new BlockPos(0, 0, 0), Blocks.AIR.defaultBlockState());
            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), instrument.getBlockPos());
            require(helper, !instrument.telemetry().available(),
                    "structure invalidation retained stale dynamic telemetry");
            instrument.handleUpdateTag(
                    instrument.getUpdateTag(helper.getLevel().registryAccess()),
                    helper.getLevel().registryAccess());
            require(helper, !instrument.clientTelemetry().available(),
                    "client update retained stale telemetry after structure invalidation");

            helper.setBlock(new BlockPos(0, 0, 0), P1Blocks.REACTOR_CASING.get().defaultBlockState());
            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), instrument.getBlockPos());
            require(helper, !instrument.telemetry().available(),
                    "repaired structure restored telemetry before a new formal tick");
            require(helper, instrument.tickReactor(),
                    "repaired structure did not complete a new formal tick");
            require(helper, instrument.telemetry().available(),
                    "telemetry did not recover after the first tick following repair");
            helper.succeed();
        });
    }

    /** 验证 SCRAM 只抑制相邻控制棒列影响范围内的裂变，不会全局清零非相邻燃料。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void scrammedReactorDoesNotGloballyZeroNonAdjacentFuel(GameTestHelper helper) {
        Map<ReactorStructureDefinition.LocalPosition, String> layout =
                new java.util.HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        CoreColumnPosition controlColumn = new CoreColumnPosition(1, 1);
        layout.put(new ReactorStructureDefinition.LocalPosition(2, 4, 2),
                "create_nuclear_industry:control_rod_drive");
        buildStructure(helper, layout);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "control-rod reactor did not form");
            instrument.setSnapshot(new ReactorSnapshot(
                    Map.of(TEST_COLUMN, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)),
                    Map.of(controlColumn, new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D)),
                    0L,
                    0L,
                    0L,
                    false
            ));
            require(helper, instrument.updateRedstoneScram(true).scramActive(),
                    "instrument port did not accept SCRAM for a control-rod structure");
            instrument.tickReactor();
            require(helper, instrument.snapshot().scramActive(), "SCRAM state was not retained by the tick");
            ReactorFissionResult fission = ReactorFissionCalculator.calculate(
                    instrument.snapshot(), ReactorSimulationParameters.defaults());
            require(helper, fission.generatedHeatHu() > 0.0D
                            && fission.plannedFuelBurnUnits() > 0.0D,
                    "SCRAM globally suppressed non-adjacent fuel heat or burn");
            helper.succeed();
        });
    }

    /** 从固定仪表坐标取得方块实体，并把夹具错误转换为 GameTest 失败。 */
    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "expected reactor instrument port block entity");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    /** 使用结构定义的标准模板，避免 GameTest 自己维护第二份结构坐标。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        buildStructure(helper, ReactorStructureDefinition.canonicalTemplate());
    }

    /** 提供非空冷却剂与已安装燃料，确保测试实际覆盖库存、转化和燃料状态。 */
    private static ReactorSnapshot snapshotWithFuelAndColdCoolant() {
        return new ReactorSnapshot(
                Map.of(TEST_COLUMN, new FuelColumnState(
                        FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)),
                Map.of(), 128L, 0L, 0L, false);
    }

    /** 按结构定义的本地坐标逐项放置方块；空方块由模板 ID 显式表示并保持为空气。 */
    private static void buildStructure(
            GameTestHelper helper,
            Map<ReactorStructureDefinition.LocalPosition, String> template
    ) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry : template.entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 将结构契约中的命名空间 ID 映射为已注册方块，未知 ID 必须立即暴露夹具错误。 */
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
            default -> throw new IllegalArgumentException("unknown reactor block " + id);
        };
    }

    /** 清空模拟连接的历史出站消息，避免把结构变化包误判为动态遥测同步。 */
    private static void drainOutbound(EmbeddedChannel channel) {
        channel.runPendingTasks();
        while (channel.readOutbound() != null) {
        }
    }

    /** 从真实方块实体更新包中识别可用动态遥测，不直接读取服务端方块实体字段。 */
    private static boolean hasAvailableTelemetryPacket(
            EmbeddedChannel channel,
            BlockPos instrumentPosition
    ) {
        channel.runPendingTasks();
        Object outbound;
        while ((outbound = channel.readOutbound()) != null) {
            if (outbound instanceof ClientboundBlockEntityDataPacket packet
                    && packet.getPos().equals(instrumentPosition)) {
                CompoundTag telemetryTag = packet.getTag().getCompound("InstrumentTelemetry");
                if (ReactorInstrumentTelemetryNbtCodec.decode(telemetryTag).available()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 创建不经过 Create 登录事件的嵌入式服务端玩家，避免无客户端协议的登录副作用。 */
    private static ServerPlayer createUnloggedMockClient(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "telemetry-test-client");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                profile,
                ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public boolean isMemoryConnection() {
                return true;
            }
        };
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(), connection, player, cookie);
        return player;
    }

    /** 将嵌入式客户端加入真实 ChunkMap 追踪，使 sendBlockUpdated 走实际观察者路径。 */
    private static void trackMockClient(GameTestHelper helper, ServerPlayer player) {
        try {
            var updatePlayerStatus = ChunkMap.class.getDeclaredMethod(
                    "updatePlayerStatus", ServerPlayer.class, boolean.class);
            updatePlayerStatus.setAccessible(true);
            updatePlayerStatus.invoke(helper.getLevel().getChunkSource().chunkMap, player, true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("unable to track embedded telemetry client", exception);
        }
    }

    /** 统一使用 GameTest 的失败通道，确保异步回调中的失败不会被普通断言吞掉。 */
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
