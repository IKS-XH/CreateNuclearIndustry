package com.iksxh.create_nuclear_industry.heat;

import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
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
    private static final Map<ModConfigSpec.ConfigValue<?>, Object> ORIGINAL_VALUES = new LinkedHashMap<>();

    /** 仅为实际pauseHeat准备规格默认值缓存，结束后恢复；不装载或保存任何配置文件。 */
    @BeforeAll static void loadDefaults() throws ReflectiveOperationException {
        var cache = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cache.setAccessible(true);
        for (var value : List.of(HeatExchangerConfig.HEAT_LEVEL, HeatExchangerConfig.HU_PER_LEVEL,
                HeatExchangerConfig.BUFFER_TICKS, HeatExchangerConfig.HOT_CAPACITY_MB,
                HeatExchangerConfig.COLD_CAPACITY_MB, HeatExchangerConfig.MAX_LINE_LENGTH,
                HeatExchangerConfig.BASIN_HEAT_LEVEL_EQUIVALENT, P1ServerConfig.VALUES.coolantAbsorptionHuPerMb)) {
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
