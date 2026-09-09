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
    private static final Map<ServerLevel, Set<BlockPos>> PENDING =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private ReactorStructureLifecycle() {
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
        Set<BlockPos> candidates = new LinkedHashSet<>();
        BlockPos brokenPos = event.getPos();
        int radius = ReactorStructureDefinition.SIZE - 1;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = brokenPos.offset(dx, dy, dz);
                    if (serverLevel.getBlockEntity(candidate)
                            instanceof ReactorInstrumentPortBlockEntity) {
                        candidates.add(candidate.immutable());
                    }
                }
            }
        }

        List<ReactorDisassemblyPlan> plans = new ArrayList<>();
        for (BlockPos candidate : candidates) {
            if (serverLevel.getBlockEntity(candidate)
                    instanceof ReactorInstrumentPortBlockEntity instrument) {
                ReactorDisassemblyPlan plan = instrument.prepareDisassemblyPlan(
                        brokenPos, event.getState());
                if (plan != null) {
                    plans.add(plan);
                }
            }
        }
        return List.copyOf(plans);
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
        for (Direction direction : Direction.values()) {
            BlockPos pipePos = endpointPos.relative(direction);
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
            instrument.updateStructureCache(scan);
        }
        return scan;
    }
}
