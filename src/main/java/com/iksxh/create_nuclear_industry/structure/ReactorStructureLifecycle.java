package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 仅在世界可能发生变化时安排结构重扫，并在服务端 tick 末端执行。 */
public final class ReactorStructureLifecycle {
    /** 仅存服务端已加载仪表实例；卸载、替换及世界关闭必须撤销，索引不成为持久化权威。 */
    private static final Map<Level, Set<ReactorInstrumentPortBlockEntity>> OWNERS = new IdentityHashMap<>();
    private static final Map<ReactorInstrumentPortBlockEntity, WaitingFrame> WAITING = new IdentityHashMap<>();
    private static final Map<ServerLevel, Set<BlockPos>> PENDING =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private ReactorStructureLifecycle() {
    }

    /** 仪表onLoad登记，随后安排一次延后扫描以覆盖端口反向加载。 */
    public static void registerOwner(ReactorInstrumentPortBlockEntity owner) {
        if (!(owner.getLevel() instanceof ServerLevel)) return;
        OWNERS.computeIfAbsent(owner.getLevel(), ignored -> new LinkedHashSet<>()).add(owner);
    }
    /** 删除实例身份，旧方块实体不得在相同坐标的新所有者恢复后重新入选。 */
    public static void unregisterOwner(ReactorInstrumentPortBlockEntity owner) {
        WAITING.remove(owner);
        Set<ReactorInstrumentPortBlockEntity> owners = OWNERS.get(owner.getLevel());
        if (owners != null) {
            owners.remove(owner);
            if (owners.isEmpty()) OWNERS.remove(owner.getLevel());
        }
    }
    /** 只读过滤已加载实例，距变化格三轴各至多15；不枚举31³世界方块或加载区块。 */
    public static List<ReactorInstrumentPortBlockEntity> loadedOwnersNear(Level level, BlockPos changed) {
        if (level == null || changed == null || level.isClientSide) return List.of();
        return OWNERS.getOrDefault(level, Set.of()).stream().filter(owner -> !owner.isRemoved()
                && level.hasChunkAt(owner.getBlockPos()) && level.getBlockEntity(owner.getBlockPos()) == owner
                && Math.abs((long) owner.getBlockPos().getX() - changed.getX()) <= 15
                && Math.abs((long) owner.getBlockPos().getY() - changed.getY()) <= 15
                && Math.abs((long) owner.getBlockPos().getZ() - changed.getZ()) <= 15).toList();
    }
    /** 区块可用性边沿只通知其有限可达仪表；失去远端区块立即撤销handler且保留模拟库存。 */
    @SubscribeEvent
    public static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            // FULL加载但尚未进入实体tick时onLoad可能延后；直接使用事件已有区块的NBT/实体
            // 坐标，实例化本区块仪表并登记，禁止通过Level.getBlockEntity探测远端。
            if (event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk) {
                for (var pos : List.copyOf(chunk.getBlockEntitiesPos())) {
                    if (chunk.getBlockEntity(pos) instanceof ReactorInstrumentPortBlockEntity owner) {
                        registerOwner(owner);
                        scheduleRescanAround(level, pos);
                    }
                }
            }
            notifyChunk(level, event.getChunk().getPos(), false);
        }
    }
    @SubscribeEvent
    public static void onChunkUnload(net.neoforged.neoforge.event.level.ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) notifyChunk(level, event.getChunk().getPos(), true);
    }
    private static void notifyChunk(ServerLevel level, net.minecraft.world.level.ChunkPos chunk, boolean unloading) {
        for (var owner : List.copyOf(OWNERS.getOrDefault(level, Set.of()))) {
            BlockPos pos = owner.getBlockPos();
            if (Math.abs((long) pos.getX() - (chunk.getMinBlockX() + 8)) > 23
                    || Math.abs((long) pos.getZ() - (chunk.getMinBlockZ() + 8)) > 23) continue;
            if (unloading && owner.structureValid() && owner.structureOrigin() != null) {
                var origin = owner.structureOrigin();
                var dimensions = owner.structureScan().dimensions();
                if (chunk.getMaxBlockX() >= origin.getX() && chunk.getMinBlockX() < origin.getX() + dimensions.width()
                        && chunk.getMaxBlockZ() >= origin.getZ() && chunk.getMinBlockZ() < origin.getZ() + dimensions.depth()) {
                    waitForChunks(owner);
                }
            }
            scheduleRescanAround(level, pos);
        }
    }
    /** 世界退出清除临时索引和未消费通知，不让其他世界实例继承所有权。 */
    @SubscribeEvent
    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        OWNERS.remove(event.getLevel());
        WAITING.keySet().removeIf(owner -> owner.getLevel() == event.getLevel());
        if (event.getLevel() instanceof ServerLevel level) PENDING.remove(level);
    }

    /**
     * 在普通 BreakEvent 监听器完成后执行停机破坏决策；所有者先只读预检，之后才提交
     * 危险事件或安全清空事务。LOWEST 是 NeoForge 常规优先级中最晚的处理点。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || event.isCanceled()) {
            return;
        }

        // 可用性消失但Post尚未运行，以及当前NBT仅近端恢复时，都用原已验证占据证据
        // 保守拒绝拆除。证据绑定具体仪表实例，不证明全堆停机，也不清任何权威状态。
        if (loadedOwnersNear(serverLevel, event.getPos()).stream()
                .anyMatch(owner -> owner.protectsUnavailableComponent(event.getPos(), event.getState()))) {
            cancelBreak(event, event.getPlayer(), "not_fully_stopped");
            return;
        }

        List<ReactorDisassemblyPlan> plans = collectDisassemblyPlans(serverLevel, event);
        if (plans.stream().anyMatch(plan -> plan.action() == ReactorDisassemblyPlan.Action.REJECTED)) {
            cancelBreak(event, event.getPlayer(), "not_fully_stopped");
            return;
        }
        if (event.isCanceled()) {
            return;
        }

        boolean hasReset = plans.stream()
                .anyMatch(plan -> plan.action() == ReactorDisassemblyPlan.Action.FULL_SHUTDOWN_RESET);
        try {
            commitDisassemblyPlans(plans, event);
            if (hasReset) {
                notifyPlayer(event.getPlayer(), "state_cleared");
            }
            scheduleRescanAround(serverLevel, event.getPos());
        } catch (RuntimeException exception) {
            rollbackDisassemblyPlans(plans);
            cancelBreak(event, event.getPlayer(), "transaction_failed");
        }
    }

    /**
     * 以破坏位置为中心去重收集所有可能的仪表端口，并在任何写入前完成全部只读预检。
     * 无效缓存不是所有者；有效缓存但无法证明安全或无法枚举端口时则形成拒绝计划。
     */
    private static List<ReactorDisassemblyPlan> collectDisassemblyPlans(
            ServerLevel serverLevel,
            BlockEvent.BreakEvent event
    ) {
        BlockPos brokenPos = event.getPos();
        List<ReactorDisassemblyPlan> plans = new ArrayList<>();
        for (ReactorInstrumentPortBlockEntity instrument : loadedOwnersNear(serverLevel, brokenPos)) {
            ReactorDisassemblyPlan plan = instrument.prepareDisassemblyPlan(brokenPos, event.getState());
            if (plan != null) plans.add(plan);
        }        return List.copyOf(plans);
    }

    /** 所有预检成功后按固定顺序提交，危险状态和安全清空都仍发生在原方块存在时。 */
    private static void commitDisassemblyPlans(
            List<ReactorDisassemblyPlan> plans,
            BlockEvent.BreakEvent event
    ) {
        if (event.isCanceled()) {
            throw new IllegalStateException("break event was canceled before disassembly commit");
        }
        for (ReactorDisassemblyPlan plan : plans) {
            if (plan.action() == ReactorDisassemblyPlan.Action.FULL_SHUTDOWN_RESET
                    || plan.action() == ReactorDisassemblyPlan.Action.DANGEROUS
                    || plan.action() == ReactorDisassemblyPlan.Action.INSTRUMENT_MAINTENANCE) {
                if (!plan.owner().commitDisassemblyPlan(plan)) {
                    throw new IllegalStateException("disassembly plan commit failed");
                }
            }
        }
        for (ReactorDisassemblyPlan plan : plans) {
            if (!plan.owner().verifyDisassemblyPlan(plan)) {
                throw new IllegalStateException("disassembly plan post-check failed");
            }
        }
        for (ReactorDisassemblyPlan plan : plans) {
            if (plan.action() == ReactorDisassemblyPlan.Action.DANGEROUS
                    && plan.publishDangerousEvent()) {
                if (!plan.owner().publishDangerousDisassembly(plan)) {
                    throw new IllegalStateException("dangerous disassembly event publish failed");
                }
            }
        }
        for (ReactorDisassemblyPlan plan : plans) {
            if (plan.action() == ReactorDisassemblyPlan.Action.FULL_SHUTDOWN_RESET)
                plan.owner().finishDisassemblySafetyEvidence(plan);
        }
    }

    /** 事务失败时按所有者恢复读阶段副本；恢复失败只记录日志，不能吞掉原始异常。 */
    private static void rollbackDisassemblyPlans(List<ReactorDisassemblyPlan> plans) {
        for (ReactorDisassemblyPlan plan : plans) {
            if (plan.action() == ReactorDisassemblyPlan.Action.FULL_SHUTDOWN_RESET
                    || plan.action() == ReactorDisassemblyPlan.Action.DANGEROUS
                    || plan.action() == ReactorDisassemblyPlan.Action.INSTRUMENT_MAINTENANCE) {
                plan.owner().rollbackDisassemblyPlan(plan);
            }
        }
    }

    /** 向触发玩家发送服务端决定的本地化动作栏消息；无玩家事件仍保持纯服务端行为。 */
    private static void cancelBreak(BlockEvent.BreakEvent event, Player player, String suffix) {
        event.setCanceled(true);
        notifyPlayer(player, suffix);
    }

    /** 发送维护事务提示，不把服务端失败原因硬编码到客户端。 */
    private static void notifyPlayer(Player player, String suffix) {
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.create_nuclear_industry.maintenance." + suffix),
                    true);
        }
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
            // FULL可读状态可能先于物理ChunkEvent.Unload消失；只查至多四个缓存区块，
            // 立即冻结并撤销绑定。恢复边沿只安排一次扫描，平时不重读结构方块。
            for (var owner : List.copyOf(OWNERS.getOrDefault(serverLevel, Set.of()))) {
                if (owner.structureScan().valid() && !owner.structureChunksAvailable()) waitForChunks(owner);
                WaitingFrame waiting = WAITING.get(owner);
                if (waiting != null && waiting.available(serverLevel)) {
                    WAITING.remove(owner);
                    scheduleRescanAround(serverLevel, owner.getBlockPos());
                }
            }
            Set<BlockPos> changes = PENDING.remove(serverLevel);
            if (changes == null) {
                continue;
            }
            synchronized (changes) {
                Set<ReactorInstrumentPortBlockEntity> owners = new LinkedHashSet<>();
                for (BlockPos change : changes) owners.addAll(loadedOwnersNear(serverLevel, change));
                for (var owner : owners) owner.updateStructureCache(
                        ReactorStructureScanner.scanInstrumentPort(serverLevel, owner.getBlockPos()));
            }
        }
    }

    private static void waitForChunks(ReactorInstrumentPortBlockEntity owner) {
        if (owner.structureOrigin() != null) WAITING.put(owner,
                new WaitingFrame(owner.structureOrigin(), owner.structureScan().dimensions()));
        owner.updateStructureCache(ReactorStructureScanner.WorldScanResult.invalid(
                ReactorStructureDefinition.DiagnosticCode.CHUNKS_UNAVAILABLE, "reactor waits for loaded chunks"));
    }

    /** 只保存等待前的几何证据；库存仍留在仪表快照，区块恢复不成为重组旁路。 */
    private record WaitingFrame(BlockPos origin, ReactorDimensions dimensions) {
        private boolean available(ServerLevel level) {
            for (int x = origin.getX() >> 4; x <= (origin.getX() + dimensions.width() - 1) >> 4; x++) {
                for (int z = origin.getZ() >> 4; z <= (origin.getZ() + dimensions.depth() - 1) >> 4; z++) {
                    if (!level.hasChunk(x, z)) return false;
                }
            }
            return true;
        }
    }

    /** 为直接方块修改或测试执行一次立即的服务端扫描。 */
    public static void rescanAroundNow(Level level, BlockPos changedPos) {
        if (level != null && !level.isClientSide && changedPos != null) {
            ReactorStructureScanner.rescanAround(level, changedPos);
        }
    }

    /**
     * 在冷/热端口的 capability 有效边沿后，通知相邻 Create 管道重新发现端点。
     *
     * <p>调用者必须先完成 {@link Level#invalidateCapabilities(BlockPos)}；这里使用
     * Create 6.0.10 的公开 {@link FluidPropagator#propagateChangedPipe} 入口清理相邻
     * 管道压力并重新发现机械泵，不访问 Create 私有字段，也不依赖固定延时或永久轮询。</p>
     */
    public static void notifyFluidNetworkAround(Level level, BlockPos endpointPos) {
        if (!(level instanceof ServerLevel serverLevel) || endpointPos == null) {
            return;
        }
        if (!serverLevel.hasChunkAt(endpointPos)) return;
        for (Direction direction : Direction.values()) {
            BlockPos pipePos = endpointPos.relative(direction);
            if (!serverLevel.hasChunkAt(pipePos)) continue;
            FluidTransportBehaviour pipe = FluidPropagator.getPipe(serverLevel, pipePos);
            if (pipe == null) {
                continue;
            }
            FluidPropagator.propagateChangedPipe(
                    serverLevel, pipePos, serverLevel.getBlockState(pipePos));
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
            registerOwner(instrument);
            instrument.updateStructureCache(scan);
        }
        return scan;
    }
}
