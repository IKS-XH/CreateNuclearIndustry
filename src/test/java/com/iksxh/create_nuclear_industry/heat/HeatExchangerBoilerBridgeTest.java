package com.iksxh.create_nuclear_industry.heat;

import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.config.ConfigBase;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.neoforged.neoforge.common.ModConfigSpec;
import sun.misc.Unsafe;
import static org.junit.jupiter.api.Assertions.*;

/** 真实BE、登记和事件入口配合局部世界夹具；不创建或加载存档。 */
class HeatExchangerBoilerBridgeTest {
    private final List<Fixture> held = new ArrayList<>();
    private final List<PendingLevel> pendingWorlds = new ArrayList<>();
    private static final Map<ModConfigSpec.ConfigValue<?>, Object> ORIGINAL_VALUES = new LinkedHashMap<>();

    /** 为实际pauseHeat和Create储罐构造准备规格默认值缓存，结束后恢复；不装载或保存配置文件。 */
    @BeforeAll static void loadDefaults() throws ReflectiveOperationException {
        var cache = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cache.setAccessible(true);
        var wrappedValue = ConfigBase.CValue.class.getDeclaredField("value");
        wrappedValue.setAccessible(true);
        var tankCapacity = (ModConfigSpec.ConfigValue<?>) wrappedValue.get(AllConfigs.server().fluids.fluidTankCapacity);
        for (var value : List.of(HeatExchangerConfig.HEAT_LEVEL, HeatExchangerConfig.HU_PER_LEVEL,
                HeatExchangerConfig.BUFFER_TICKS, HeatExchangerConfig.HOT_CAPACITY_MB,
                HeatExchangerConfig.COLD_CAPACITY_MB, HeatExchangerConfig.MAX_LINE_LENGTH,
                HeatExchangerConfig.BASIN_HEAT_LEVEL_EQUIVALENT, P1ServerConfig.VALUES.coolantAbsorptionHuPerMb, tankCapacity)) {
            ORIGINAL_VALUES.put(value, cache.get(value));
            cache.set(value, value.getDefault());
        }
    }

    @AfterAll static void unloadDefaults() throws ReflectiveOperationException {
        var cache = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cache.setAccessible(true);
        for (var entry : ORIGINAL_VALUES.entrySet()) cache.set(entry.getKey(), entry.getValue());
        ORIGINAL_VALUES.clear();
    }

    @AfterEach void removeFixtures() {
        for (var fixture : held) {
            HeatExchangerBoilerBridge.unregisterLoaded(fixture.machine);
            HeatExchangerBoilerBridge.track(fixture.machine, false);
        }
        for (var world : pendingWorlds) world.onRefresh = null;
        HeatExchangerBoilerBridge.afterServerTick(null);
    }

    @Test void clientLoadedSourceIsNeverAudited() {
        var fixture = fixture(true);
        HeatExchangerBoilerBridge.registerLoaded(fixture.machine);
        HeatExchangerBoilerBridge.beforeServerTick(null);
        assertEquals(0, fixture.world.reads, "客户端实例不能进入服务端已加载审计");
    }

    @Test void clientActiveSourceIsNeverAudited() {
        var fixture = fixture(true);
        HeatExchangerBoilerBridge.track(fixture.machine, true);
        HeatExchangerBoilerBridge.beforeServerTick(null);
        assertEquals(0, fixture.world.reads, "客户端实例不能进入服务端已供热审计");
        assertFalse(paused(fixture.machine));
    }

    @Test void unattachedSourcesAreRejectedByBothRegistries() {
        var fixture = fixture(false);
        fixture.machine.setLevel(null);
        HeatExchangerBoilerBridge.registerLoaded(fixture.machine);
        HeatExchangerBoilerBridge.track(fixture.machine, true);
        HeatExchangerBoilerBridge.beforeServerTick(null);
        assertFalse(paused(fixture.machine), "无世界来源不能进入服务端热撤销");
    }

    @Test void serverLoadedSourceIsPausedAndUnregisterStopsFurtherAudit() {
        var fixture = fixture(false);
        fixture.world.present = true;
        HeatExchangerBoilerBridge.registerLoaded(fixture.machine);
        HeatExchangerBoilerBridge.beforeServerTick(null);
        assertTrue(paused(fixture.machine), "服务端停tick来源仍须撤热");
        int reads = fixture.world.reads;
        HeatExchangerBoilerBridge.unregisterLoaded(fixture.machine);
        HeatExchangerBoilerBridge.beforeServerTick(null);
        assertEquals(reads, fixture.world.reads, "移除后不能继续审计");
    }

    @Test void activeInvalidSourceIsPausedAndRemoved() {
        var fixture = fixture(false);
        HeatExchangerBoilerBridge.track(fixture.machine, true);
        HeatExchangerBoilerBridge.afterServerTick(null);
        assertTrue(paused(fixture.machine));
        int reads = fixture.world.reads;
        HeatExchangerBoilerBridge.afterServerTick(null);
        assertEquals(reads, fixture.world.reads);
    }

    @Test void worldCallbackCanRemoveBothRegistriesWithoutHoldingTheirLock() {
        var fixture = fixture(false);
        HeatExchangerBoilerBridge.registerLoaded(fixture.machine);
        HeatExchangerBoilerBridge.track(fixture.machine, true);
        fixture.world.onRead = () -> {
            // 世界读取回调只派登记表操作；持锁审计将阻止后台移除在期限内完成。
            var executor = Executors.newSingleThreadExecutor();
            try {
                var removed = executor.submit(() -> {
                    HeatExchangerBoilerBridge.unregisterLoaded(fixture.machine);
                    HeatExchangerBoilerBridge.track(fixture.machine, false);
                });
                removed.get(2, TimeUnit.SECONDS);
            } catch (Exception exception) {
                throw new AssertionError("世界审计回调仍持有登记锁", exception);
            } finally { executor.shutdownNow(); }
        };
        HeatExchangerBoilerBridge.beforeServerTick(null);
        int reads = fixture.world.reads;
        HeatExchangerBoilerBridge.beforeServerTick(null);
        assertEquals(reads, fixture.world.reads);
    }

    @Test void pauseCallbackCanReenterRemovalWithoutHoldingEitherRegistryLock() {
        for (boolean active : new boolean[]{false, true}) {
            var fixture = fixture(false);
            fixture.world.present = !active;
            register(fixture.machine, active);
            fixture.world.onInvalidate = () -> {
                var executor = Executors.newSingleThreadExecutor();
                try {
                    executor.submit(() -> {
                        HeatExchangerBoilerBridge.unregisterLoaded(fixture.machine);
                        HeatExchangerBoilerBridge.track(fixture.machine, false);
                    }).get(2, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new AssertionError("实际pauseHeat回调仍持有登记锁", exception);
                } finally { executor.shutdownNow(); }
            };
            HeatExchangerBoilerBridge.beforeServerTick(null);
            assertTrue(paused(fixture.machine));
            int reads = fixture.world.reads;
            HeatExchangerBoilerBridge.beforeServerTick(null);
            assertEquals(reads, fixture.world.reads);
        }
    }

    @Test void concurrentLoadedRegistrationAndSnapshotsRemainConsistent() throws Exception {
        exerciseConcurrentSnapshots(false);
    }

    @Test void concurrentActiveRegistrationAndSnapshotsRemainConsistent() throws Exception {
        exerciseConcurrentSnapshots(true);
    }

    /** 强引用保留所有键；后台只变更登记表，世界查询和热撤销仅在JUnit主线程执行。 */
    private void exerciseConcurrentSnapshots(boolean active) throws Exception {
        var sources = new ArrayList<Fixture>();
        for (int i = 0; i < 2048; i++) sources.add(fixture(false));
        for (var fixture : sources) register(fixture.machine, active);
        var running = new AtomicBoolean(true);
        var started = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        var writes = executor.submit(() -> {
            started.countDown();
            while (running.get()) for (var fixture : sources) {
                if (active) HeatExchangerBoilerBridge.track(fixture.machine, false);
                else HeatExchangerBoilerBridge.unregisterLoaded(fixture.machine);
                register(fixture.machine, active);
            }
        });
        try {
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 300; i++) HeatExchangerBoilerBridge.beforeServerTick(null);
            }, "真实弱表登记/移除与事件快照并发时不能抛CME");
        } finally {
            running.set(false);
            writes.get(5, TimeUnit.SECONDS);
            executor.shutdownNow();
        }
        assertTrue(sources.stream().mapToInt(fixture -> fixture.world.reads).sum() > 0,
                "并发检查必须实际执行来源审计，不能跳过整个审计");
    }

    @Test void pendingRefreshReentryDefersNewPositionsAndDeduplicatesEachBatch() {
        var world = pendingWorld();
        var first = new BlockPos(1, 10, 1);
        var second = new BlockPos(2, 10, 2);
        var later = new BlockPos(3, 10, 3);
        var initial = loadEvent(world, first, second);
        var added = loadEvent(world, later);
        HeatExchangerBoilerBridge.chunkLoaded(initial);
        HeatExchangerBoilerBridge.chunkLoaded(initial);
        world.onRefresh = () -> {
            HeatExchangerBoilerBridge.chunkLoaded(added);
            HeatExchangerBoilerBridge.chunkLoaded(added);
        };
        assertDoesNotThrow(() -> HeatExchangerBoilerBridge.afterServerTick(null),
                "真实refresh重入ChunkEvent.Load不能修改正在遍历的位置集合");
        assertEquals(Map.of(first, 1, second, 1), world.refreshed,
                "本轮只消费已有去重位置，新登记留给下轮");
        HeatExchangerBoilerBridge.afterServerTick(null);
        assertEquals(Map.of(first, 1, second, 1, later, 1), world.refreshed);
        HeatExchangerBoilerBridge.afterServerTick(null);
        assertEquals(Map.of(first, 1, second, 1, later, 1), world.refreshed,
                "空队列不能重复刷新或扫描世界");
    }

    @Test void pendingRefreshReentryPreservesNewWorldRequests() {
        var firstWorld = pendingWorld();
        var otherWorld = pendingWorld();
        var pos = new BlockPos(1, 10, 1);
        var added = loadEvent(otherWorld, pos);
        HeatExchangerBoilerBridge.chunkLoaded(loadEvent(firstWorld, pos));
        firstWorld.onRefresh = () -> HeatExchangerBoilerBridge.chunkLoaded(added);
        assertDoesNotThrow(() -> HeatExchangerBoilerBridge.afterServerTick(null));
        assertEquals(Map.of(pos, 1), firstWorld.refreshed);
        assertTrue(otherWorld.refreshed.isEmpty(), "新增世界也须留到下轮");
        HeatExchangerBoilerBridge.afterServerTick(null);
        assertEquals(Map.of(pos, 1), otherWorld.refreshed, "本轮清理不能抹掉新增世界");
    }

    @Test void pendingWorldCallbackAllowsAnotherThreadToRegisterWithoutWorldReads() {
        var world = pendingWorld();
        var first = new BlockPos(1, 10, 1);
        var later = new BlockPos(3, 10, 3);
        var added = loadEvent(world, later);
        HeatExchangerBoilerBridge.chunkLoaded(loadEvent(world, first));
        world.onRefresh = () -> {
            var executor = Executors.newSingleThreadExecutor();
            try {
                executor.submit(() -> HeatExchangerBoilerBridge.chunkLoaded(added)).get(2, TimeUnit.SECONDS);
            } catch (Exception exception) {
                throw new AssertionError("刷新回调持有待处理锁，阻塞另一线程的纯登记", exception);
            } finally { executor.shutdownNow(); }
        };
        assertDoesNotThrow(() -> HeatExchangerBoilerBridge.afterServerTick(null));
        assertEquals(Map.of(first, 1), world.refreshed);
        HeatExchangerBoilerBridge.afterServerTick(null);
        assertEquals(Map.of(first, 1, later, 1), world.refreshed);
    }

    @Test void pendingRefreshRetainsUnloadedChunkGuard() {
        var world = pendingWorld();
        var pos = new BlockPos(1, 10, 1);
        HeatExchangerBoilerBridge.chunkLoaded(loadEvent(world, pos));
        world.loaded = false;
        HeatExchangerBoilerBridge.afterServerTick(null);
        assertTrue(world.refreshed.isEmpty(), "未加载控制器不能读取实体或强载区块");
        assertEquals(1, world.chunkChecks);
    }

    /** 只替代存档/区块边界；真实加载事件登记真实Create储罐，消费仍走生产afterServerTick与refresh。 */
    private PendingLevel pendingWorld() {
        var world = allocate(PendingLevel.class);
        world.auditThread = Thread.currentThread();
        world.tanks = new LinkedHashMap<>();
        world.refreshed = new LinkedHashMap<>();
        world.loaded = true;
        pendingWorlds.add(world);
        return world;
    }

    private static ChunkEvent.Load loadEvent(PendingLevel world, BlockPos... positions) {
        var chunk = allocate(PendingChunk.class);
        chunk.world = world;
        chunk.entities = new LinkedHashMap<>();
        for (var pos : positions) {
            var tank = new FluidTankBlockEntity(AllBlockEntityTypes.FLUID_TANK.get(), pos,
                    AllBlocks.FLUID_TANK.getDefaultState());
            world.tanks.put(pos, tank);
            chunk.entities.put(pos, tank);
        }
        return new ChunkEvent.Load(chunk, false);
    }

    private static <T> T allocate(Class<T> type) {
        try {
            var field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private static final class PendingChunk extends LevelChunk {
        PendingLevel world;
        Map<BlockPos, BlockEntity> entities;
        private PendingChunk() { super(null, (ChunkPos) null); }
        @Override public Level getLevel() { return world; }
        @Override public Map<BlockPos, BlockEntity> getBlockEntities() { return entities; }
    }

    /** 所有世界读取限定JUnit主线程；后台只触发事件读取预先准备的区块实体清单。 */
    private static final class PendingLevel extends ClientLevel {
        Thread auditThread;
        Map<BlockPos, BlockEntity> tanks;
        Map<BlockPos, Integer> refreshed;
        boolean loaded;
        int chunkChecks;
        Runnable onRefresh;
        private PendingLevel() { super(null, null, null, null, 0, 0, null, null, false, 0L); }
        private void checkThread() { assertSame(auditThread, Thread.currentThread(), "后台登记不能读取世界"); }
        @Override public boolean hasChunkAt(BlockPos pos) {
            checkThread();
            chunkChecks++;
            if (onRefresh != null) {
                var callback = onRefresh;
                onRefresh = null;
                callback.run();
            }
            return loaded;
        }
        @Override public BlockEntity getBlockEntity(BlockPos pos) {
            checkThread();
            assertTrue(loaded, "未加载时不能查询实体");
            refreshed.merge(pos, 1, Integer::sum);
            return tanks.get(pos);
        }
        @Override public BlockState getBlockState(BlockPos pos) {
            checkThread();
            return Blocks.AIR.defaultBlockState();
        }
    }

    private static void register(NuclearHeatExchangerBlockEntity machine, boolean active) {
        if (active) HeatExchangerBoilerBridge.track(machine, true);
        else HeatExchangerBoilerBridge.registerLoaded(machine);
    }

    /** 复用现有Unsafe局部世界夹具方案；实体按真实注册类型构造，只置为已加载状态。 */
    private Fixture fixture(boolean client) {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            var unsafe = (Unsafe) field.get(null);
            var world = (FixtureLevel) unsafe.allocateInstance(FixtureLevel.class);
            unsafe.putBoolean(world, unsafe.objectFieldOffset(Level.class.getDeclaredField("isClientSide")), client);
            world.auditThread = Thread.currentThread();
            var machine = new NuclearHeatExchangerBlockEntity(BlockPos.ZERO,
                    HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState());
            machine.setLevel(world);
            world.machine = machine;
            var available = NuclearHeatExchangerBlockEntity.class.getDeclaredField("available");
            available.setAccessible(true);
            available.setBoolean(machine, true);
            var fixture = new Fixture(machine, world);
            held.add(fixture);
            return fixture;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("无法创建热源侧隔离夹具", exception);
        }
    }

    private static boolean paused(NuclearHeatExchangerBlockEntity machine) {
        try {
            var field = NuclearHeatExchangerBlockEntity.class.getDeclaredField("tickPaused");
            field.setAccessible(true);
            return field.getBoolean(machine);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    private record Fixture(NuclearHeatExchangerBlockEntity machine, FixtureLevel world) {}

    /** 仅替代本任务以外的区块读边界；服务侧用false侧标记触发原BE的停tick路径。 */
    private static final class FixtureLevel extends ClientLevel {
        NuclearHeatExchangerBlockEntity machine;
        Thread auditThread;
        boolean present;
        int reads;
        Runnable onRead;
        Runnable onInvalidate;
        private FixtureLevel() { super(null, null, null, null, 0, 0, null, null, false, 0L); }
        @Override public boolean hasChunkAt(BlockPos pos) {
            assertSame(auditThread, Thread.currentThread(), "世界读取不能进入登记后台线程");
            reads++;
            if (onRead != null) onRead.run();
            return present && pos.equals(BlockPos.ZERO);
        }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return pos.equals(BlockPos.ZERO) ? machine : null; }
        @Override public void invalidateCapabilities(BlockPos pos) {
            assertSame(auditThread, Thread.currentThread(), "热撤销不能进入登记后台线程");
            if (onInvalidate != null) onInvalidate.run();
        }
        @Override public void sendBlockUpdated(BlockPos pos, BlockState before, BlockState after, int flags) {}
    }
}
