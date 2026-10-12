package com.iksxh.create_nuclear_industry.industrial;

import com.iksxh.create_nuclear_industry.config.IndustrialFluidConfig;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** 唯一服务端统一调度；每世界共用路径/泵额度，真实交易许可不跨tick或拓扑变化。 */
public final class IndustrialNetworkScheduler {
    private static final Map<ServerLevel, World> WORLDS = new WeakHashMap<>();
    private record Routes(long revision, net.minecraft.core.Direction output, IndustrialTopology.Side source, IndustrialTopology.Side target) {}
    private static final class World {
        long revision, tick = Long.MIN_VALUE;
        int cursor;
        List<Number> configuration;
        final Map<BlockPos, IndustrialPumpBlockEntity> pumps = new HashMap<>();
        final Map<BlockPos, Routes> routes = new HashMap<>();
        final IndustrialFlowBudget budget = new IndustrialFlowBudget();
        final Map<BlockPos, FluidStack> media = new HashMap<>();
    }
    private IndustrialNetworkScheduler() {}
    public static void register(ServerLevel level, IndustrialPumpBlockEntity pump) { World world = WORLDS.computeIfAbsent(level, ignored -> new World()); world.pumps.put(pump.getBlockPos(), pump); invalidate(level, pump.getBlockPos()); }
    public static void unregister(ServerLevel level, IndustrialPumpBlockEntity pump) { World world = WORLDS.get(level); if (world != null) { world.pumps.remove(pump.getBlockPos(), pump); invalidate(level, pump.getBlockPos()); } }
    /** 仅撤销包含变更格或其邻格的缓存路线；额度账本不随拓扑变更清零。 */
    public static void invalidate(ServerLevel level, BlockPos pos) {
        World world = WORLDS.get(level); if (world == null) return;
        world.revision++;
        world.routes.entrySet().removeIf(entry -> entry.getKey().distManhattan(pos) <= 1
                || touches(entry.getValue().source(), node -> node.distManhattan(pos) <= 1)
                || touches(entry.getValue().target(), node -> node.distManhattan(pos) <= 1));
    }
    private static boolean touches(IndustrialTopology.Side side, java.util.function.Predicate<BlockPos> predicate) {
        return side.nodes().stream().anyMatch(predicate) || side.endpoints().stream().anyMatch(endpoint -> predicate.test(endpoint.position()));
    }
    private static void invalidateChunk(ServerLevel level, net.minecraft.world.level.ChunkPos chunk) {
        World world = WORLDS.get(level); if (world == null) return; world.revision++;
        java.util.function.Predicate<BlockPos> inChunk = pos -> new net.minecraft.world.level.ChunkPos(pos).equals(chunk);
        world.routes.entrySet().removeIf(entry -> inChunk.test(entry.getKey()) || touches(entry.getValue().source(), inChunk) || touches(entry.getValue().target(), inChunk)
                || entry.getValue().source().missingChunks().contains(chunk) || entry.getValue().target().missingChunks().contains(chunk));
    }
    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load event) { if (event.getLevel() instanceof ServerLevel level) invalidateChunk(level, event.getChunk().getPos()); }
    @SubscribeEvent public static void chunkUnloaded(ChunkEvent.Unload event) { if (event.getLevel() instanceof ServerLevel level) invalidateChunk(level, event.getChunk().getPos()); }
    @SubscribeEvent public static void neighborChanged(net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level) invalidate(level, event.getPos());
    }
    @SubscribeEvent public static void worldUnloaded(LevelEvent.Unload event) { if (event.getLevel() instanceof ServerLevel level) WORLDS.remove(level); }
    @SubscribeEvent public static void serverTick(LevelTickEvent.Post event) { if (event.getLevel() instanceof ServerLevel level) tick(level); }

    private static IFluidHandler resolve(ServerLevel level, IndustrialTopology.Endpoint endpoint) {
        if (!level.hasChunkAt(endpoint.position())) return null;
        return RestrictedFluidHandler.wrap(IndustrialTopology.capabilityAt(level, endpoint.position(), endpoint.side()));
    }
    private static boolean permittedRestrictedEndpoint(ServerLevel level, IndustrialTopology.Endpoint endpoint) {
        var entity = IndustrialTopology.entityAt(level, endpoint.position());
        return entity instanceof com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
                || entity instanceof com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity
                || entity instanceof com.iksxh.create_nuclear_industry.boiler.BoilerPortBlockEntity
                || entity instanceof com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity
                || entity instanceof com.iksxh.create_nuclear_industry.turbine.TurbinePortBlockEntity;
    }
    /** 泵轮转、端点轮转均固定排序；每tick只调度一次，节点不自行搬运库存。 */
    public static void tick(ServerLevel level) {
        World world = WORLDS.get(level); if (world == null || world.tick == level.getGameTime()) return; world.tick = level.getGameTime(); world.media.clear();
        var configuration = IndustrialFluidConfig.snapshot();
        if (!configuration.equals(world.configuration)) {
            world.configuration = configuration; world.routes.clear(); world.revision++;
            if (IndustrialFluidConfig.valid()) for (var pump : world.pumps.values()) if (!pump.isRemoved() && pump.hasNetwork())
                pump.getOrCreateNetwork().updateStressFor(pump, pump.calculateStressApplied());
        }
        List<IndustrialPumpBlockEntity> pumps = world.pumps.values().stream().filter(pump -> !pump.isRemoved() && level.hasChunkAt(pump.getBlockPos()))
                .sorted(Comparator.comparingLong(pump -> pump.getBlockPos().asLong())).toList();
        if (pumps.isEmpty()) return;
        int offset = Math.floorMod(world.cursor++, pumps.size());
        for (int i = 0; i < pumps.size(); i++) schedule(level, world, pumps.get((offset + i) % pumps.size()));
    }
    private static void schedule(ServerLevel level, World world, IndustrialPumpBlockEntity pump) {
        if (!IndustrialFluidConfig.valid() || !Float.isFinite(pump.getSpeed())) { pump.display(0, "invalid"); return; }
        pump.resetBottleneck();
        int rating = pump.flowLimit(); if (rating == 0) { pump.display(0, "stopped"); return; }
        Routes routes = world.routes.get(pump.getBlockPos());
        if (routes == null || routes.output() != pump.outputDirection() || routes.source().recovered(level) || routes.target().recovered(level)) {
            routes = new Routes(world.revision, pump.outputDirection(), IndustrialTopology.search(level, pump, false), IndustrialTopology.search(level, pump, true));
            world.routes.put(pump.getBlockPos(), routes);
        }
        Set<BlockPos> nodes = new HashSet<>(routes.source().nodes()); nodes.addAll(routes.target().nodes()); nodes.add(pump.getBlockPos());
        if (nodes.size() > IndustrialFluidConfig.MAX_NODES.get()) { pump.display(0, "too_large"); return; }
        if (routes.target().endpoints().isEmpty() || pump.cachedFluid().isEmpty() && routes.source().endpoints().isEmpty()) {
            pump.display(0, routes.target().endpoints().isEmpty() ? routes.target().failure() : routes.source().failure()); return;
        }
        int delivered = 0;
        var targets = routes.target().endpoints(); var sources = routes.source().endpoints();
        // 管段无库存；当前端点与沿路泵仍持有其他介质时，整张连接图拒绝混输。
        List<IndustrialTopology.Endpoint> endpoints = new ArrayList<>(sources); endpoints.addAll(targets);
        FluidStack selected = pump.cachedFluid();
        for (var endpoint : endpoints) {
            var handler = resolve(level, endpoint); if (handler == null) continue;
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack stack = handler.getFluidInTank(tank); if (stack.isEmpty()) continue;
                if (!selected.isEmpty() && !FluidStack.isSameFluidSameComponents(selected, stack)) { pump.display(0, "mixed"); return; }
                selected = stack;
            }
        }
        for (BlockPos node : nodes) if (level.hasChunkAt(node) && IndustrialTopology.entityAt(level, node) instanceof IndustrialPumpBlockEntity member) {
            FluidStack stack = member.cachedFluid(); if (stack.isEmpty()) continue;
            if (!selected.isEmpty() && !FluidStack.isSameFluidSameComponents(selected, stack)) { pump.display(0, "mixed"); return; }
            selected = stack;
        }
        int targetStart = pump.nextCursor(targets.size(), false);
        int sourceStart = pump.nextCursor(sources.size(), true);
        for (int i = 0; i < targets.size(); i++) {
            var target = targets.get((targetStart + i) % targets.size());
            // 缓存只需要合法输出路线；抽取必须同时拥有源路径，不能用旧源句柄跨断管运输。
            if (!pump.cachedFluid().isEmpty()) delivered += transact(level, world, pump, null, target);
            else for (int j = 0; j < sources.size(); j++) {
                var source = sources.get((sourceStart + j) % sources.size());
                if (source.position().equals(target.position())) continue;
                delivered += transact(level, world, pump, source, target);
                if (!pump.cachedFluid().isEmpty()) break;
            }
        }
        pump.display(delivered, delivered > 0 ? "running" : "blocked");
    }
    private static int transact(ServerLevel level, World world, IndustrialPumpBlockEntity pump, IndustrialTopology.Endpoint source, IndustrialTopology.Endpoint target) {
        var sourceBinding = source == null ? null : IndustrialTopology.bindingAt(level, source.position(), source.side());
        var targetBinding = IndustrialTopology.bindingAt(level, target.position(), target.side());
        IFluidHandler from = sourceBinding == null ? null : RestrictedFluidHandler.wrap(sourceBinding.handler());
        IFluidHandler to = RestrictedFluidHandler.wrap(targetBinding.handler());
        if (to == null || source != null && from == null) return 0;
        FluidStack medium = pump.cachedFluid();
        if (medium.isEmpty()) {
            for (int tank = 0; tank < from.getTanks(); tank++) {
                FluidStack candidate = from.getFluidInTank(tank);
                if (!candidate.isEmpty()) { medium = candidate.copy(); break; }
            }
        }
        if (medium.isEmpty()) return 0;
        if (RestrictionPolicy.restricted(medium) && (!permittedRestrictedEndpoint(level, target)
                || source != null && !permittedRestrictedEndpoint(level, source))) return 0;
        List<BlockPos> path = new ArrayList<>(); path.add(pump.getBlockPos());
        if (source != null) path.addAll(source.path()); path.addAll(target.path());
        List<IndustrialFlowBudget.Limit> limits = new ArrayList<>();
        Map<Long, Integer> capacities = new HashMap<>();
        for (BlockPos node : path) {
            FluidStack selected = world.media.get(node);
            if (selected != null && !FluidStack.isSameFluidSameComponents(selected, medium)) return 0;
            int capacity = IndustrialTopology.limit(level, node);
            limits.add(new IndustrialFlowBudget.Limit(node.asLong(), capacity)); capacities.merge(node.asLong(), capacity, Math::min); pump.considerBottleneck(capacity);
        }
        int allowed = world.budget.reserve(level.getGameTime(), limits, pump.flowLimit(), false); if (allowed == 0) return 0;
        final long revision = world.revision, now = level.getGameTime();
        final FluidStack fluid = medium.copy();
        IndustrialTransferPermit permit = new IndustrialTransferPermit(from, to, allowed,
                () -> level.getGameTime() == now && world.revision == revision && !pump.isRemoved() && pump.flowLimit() > 0
                        && path.stream().allMatch(node -> IndustrialTopology.limit(level, node) > 0)
                        && targetBinding.valid().getAsBoolean() && (sourceBinding == null || sourceBinding.valid().getAsBoolean()));
        IFluidHandler authorizedSource = from instanceof RestrictedFluidHandler restricted ? restricted.authorized(permit) : from;
        IFluidHandler authorizedTarget = ((RestrictedFluidHandler) to).authorized(permit);
        IndustrialTransfer transfer = new IndustrialTransfer(); transfer.restore(pump.cachedFluid().getAmount());
        int before = transfer.cached(); int[] extracted = {0};
        IndustrialTransfer.Store sourceStore = new IndustrialTransfer.Store() {
            public int remove(int requested, boolean execute) {
                if (authorizedSource == null) return 0;
                FluidStack removed = authorizedSource.drain(fluid.copyWithAmount(requested), execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
                if (execute) extracted[0] += removed.getAmount(); return removed.getAmount();
            }
            public int add(int requested, boolean execute) { return 0; }
        };
        IndustrialTransfer.Store targetStore = new IndustrialTransfer.Store() {
            public int remove(int requested, boolean execute) { return 0; }
            public int add(int requested, boolean execute) { return authorizedTarget.fill(fluid.copyWithAmount(requested), execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE); }
        };
        int delivered = transfer.move(sourceStore, targetStore, allowed, IndustrialFluidConfig.PUMP_FLOW.get(), true);
        if (extracted[0] > 0 || delivered > 0) {
            Map<BlockPos, Integer> crossed = new HashMap<>();
            crossed.put(pump.getBlockPos(), Math.max(extracted[0], delivered));
            if (source != null) for (BlockPos node : source.path()) crossed.merge(node, extracted[0], Math::max);
            for (BlockPos node : target.path()) crossed.merge(node, delivered, Math::max);
            // 短收余量只经过输入侧；输出侧按实收收费，泵仍按抽取/交付中的较大值收费。
            for (var entry : crossed.entrySet()) if (entry.getValue() > 0) {
                world.budget.reserve(now, List.of(new IndustrialFlowBudget.Limit(entry.getKey().asLong(), capacities.get(entry.getKey().asLong()))), entry.getValue(), true);
                world.media.put(entry.getKey(), fluid.copyWithAmount(1));
            }
        }
        if (before != transfer.cached() || extracted[0] > 0) pump.setCache(transfer.cached() == 0 ? FluidStack.EMPTY : fluid.copyWithAmount(transfer.cached()));
        return delivered;
    }
}
