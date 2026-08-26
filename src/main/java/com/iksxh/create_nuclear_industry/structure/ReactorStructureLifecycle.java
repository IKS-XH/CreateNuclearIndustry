package com.iksxh.create_nuclear_industry.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 仅在世界可能发生变化时安排结构重扫，并在服务端 tick 末端执行。 */
public final class ReactorStructureLifecycle {
    private static final Map<ServerLevel, Set<BlockPos>> PENDING =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private ReactorStructureLifecycle() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || event.isCanceled()) {
            return;
        }
        scheduleRescanAround(serverLevel, event.getPos());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || event.isCanceled()) {
            return;
        }
        scheduleRescanAround(serverLevel, event.getPos());
    }

    /** 将当前服务端回合内的多个方块变化合并为一次脏位置扫描。 */
    public static void scheduleRescanAround(Level level, BlockPos changedPos) {
        if (!(level instanceof ServerLevel serverLevel) || changedPos == null) {
            return;
        }
        Set<BlockPos> pending = PENDING.computeIfAbsent(serverLevel, ignored -> new LinkedHashSet<>());
        synchronized (pending) {
            if (!pending.add(changedPos.immutable())) {
                return;
            }
        }
    }

    /** 在本 tick 的世界修改完成后消费脏位置，避免扫描看到中间状态。 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel serverLevel : event.getServer().getAllLevels()) {
            Set<BlockPos> changes = PENDING.remove(serverLevel);
            if (changes == null) {
                continue;
            }
            synchronized (changes) {
                for (BlockPos change : changes) {
                    ReactorStructureScanner.rescanAround(serverLevel, change);
                }
            }
        }
    }

    /** 为直接方块修改或测试执行一次立即的服务端扫描。 */
    public static void rescanAroundNow(Level level, BlockPos changedPos) {
        if (level != null && !level.isClientSide && changedPos != null) {
            ReactorStructureScanner.rescanAround(level, changedPos);
        }
    }

    /** 执行 Create 扳手在仪表端口上明确请求的立即扫描，并更新仪表缓存。 */
    public static ReactorStructureScanner.WorldScanResult rescanInstrumentPortNow(
            Level level,
            BlockPos instrumentPortPos
    ) {
        if (!(level instanceof ServerLevel serverLevel) || instrumentPortPos == null) {
            return null;
        }
        ReactorStructureScanner.WorldScanResult scan =
                ReactorStructureScanner.scanInstrumentPort(serverLevel, instrumentPortPos);
        if (serverLevel.getBlockEntity(instrumentPortPos)
                instanceof com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity instrument) {
            instrument.updateStructureCache(scan);
        }
        return scan;
    }
}
