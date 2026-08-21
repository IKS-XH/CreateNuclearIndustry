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

/** Schedules structure rescans only when the world can have changed. */
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

    /** Coalesces all changes in the current server turn into one dirty scan pass. */
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

    /** Consumes only dirty positions after world mutations have completed for the tick. */
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

    /** Performs an immediate server-side scan for a direct block mutation or a test. */
    public static void rescanAroundNow(Level level, BlockPos changedPos) {
        if (level != null && !level.isClientSide && changedPos != null) {
            ReactorStructureScanner.rescanAround(level, changedPos);
        }
    }

    /** Performs the explicit scan requested by a Create wrench on the instrument port. */
    public static void rescanInstrumentPortNow(Level level, BlockPos instrumentPortPos) {
        if (level == null || level.isClientSide || instrumentPortPos == null) {
            return;
        }
        ReactorStructureScanner.rescanAround(level, instrumentPortPos);
    }
}
