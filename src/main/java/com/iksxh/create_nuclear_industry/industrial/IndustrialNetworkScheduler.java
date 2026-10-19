package com.iksxh.create_nuclear_industry.industrial;

import com.iksxh.create_nuclear_industry.config.IndustrialFluidConfig;
import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity.ActiveSteamSource;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.turbine.TurbineControllerBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbineStructure;
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
    /** 同一拓扑快照的静态集合/拼接路径复用，实时流体、额度、红石和能力不放入缓存。 */
    private static final class Routes {
        private final BlockPos root;
        private final long revision;
        private final net.minecraft.core.Direction output;
        private final IndustrialTopology.Side source,target;
        private final Set<BlockPos> nodes;
        private final List<IndustrialTopology.Endpoint> endpoints;
        private final Map<IndustrialTopology.Endpoint,Map<IndustrialTopology.Endpoint,List<BlockPos>>> paths=new IdentityHashMap<>();
        private final Map<IndustrialTopology.Endpoint,Map<IndustrialTopology.Endpoint,IndustrialRoutePlan>> plans=new IdentityHashMap<>();
        private final IndustrialNetworkGraph.Snapshot graph;
        private final Map<BlockPos,Integer> pumpOrder;
        private final boolean hasValves;
        private Routes(BlockPos root,long revision,net.minecraft.core.Direction output,IndustrialTopology.Side source,IndustrialTopology.Side target) {this(root,revision,output,source,target,null);}
        private Routes(BlockPos root,long revision,net.minecraft.core.Direction output,IndustrialTopology.Side source,IndustrialTopology.Side target,IndustrialNetworkGraph.Snapshot graph) {
            this.graph=graph;
            this.root=root;this.revision=revision;this.output=output;this.source=source;this.target=target;
            var merged=new HashSet<>(source.nodes());merged.addAll(target.nodes());merged.add(root);nodes=Collections.unmodifiableSet(merged);
            var combined=new ArrayList<>(source.endpoints());combined.addAll(target.endpoints());endpoints=List.copyOf(combined);
            pumpOrder=graph==null?Map.of():IndustrialNetworkGraph.order(nodes);
            hasValves=graph==null||graph.model.nodes.values().stream().anyMatch(cell->cell.kind()==1);
        }
        /** 无阀且所有沿路泵额定余下限不低于根泵时，跳过才不会漏报更小的护目镜瓶颈。 */
        private boolean rootIsMinimum(ServerLevel level,int capacity) {
            if(hasValves||capacity<=0||graph==null||!graph.matches(level))return false;
            for(var pos:graph.pumpNodes)if(!(IndustrialTopology.entityAt(level,pos) instanceof IndustrialPumpBlockEntity member)||member.flowLimit()<capacity)return false;
            return true;
        }
        private IndustrialRoutePlan plan(IndustrialTopology.Endpoint from,IndustrialTopology.Endpoint to) {
            if(graph==null||!graph.live)return null;
            return plans.computeIfAbsent(from,ignored->new IdentityHashMap<>()).computeIfAbsent(to,ignored->IndustrialRoutePlan.joined(graph,path(from,to),from==null?List.of():from.path(),to.path()));
        }
        private net.minecraft.core.Direction output(){return output;}
        private IndustrialTopology.Side source(){return source;}
        private IndustrialTopology.Side target(){return target;}
        private List<BlockPos> path(IndustrialTopology.Endpoint from,IndustrialTopology.Endpoint to) {
            var targets=paths.computeIfAbsent(from,ignored->new IdentityHashMap<>());
            return targets.computeIfAbsent(to,ignored->new JoinedPath(root,from==null?List.of():from.path(),to.path()));
        }
    }
    /** 不复制数组的只读几何拼接；许可可长期持有此视图，不引用后续可覆写的交易buffer。 */
    private static final class JoinedPath extends AbstractList<BlockPos> implements RandomAccess {
        private final BlockPos root;
        private final List<BlockPos> source,target;
        private JoinedPath(BlockPos root,List<BlockPos> source,List<BlockPos> target){this.root=root;this.source=source;this.target=target;}
        @Override public int size(){return 1+source.size()+target.size();}
        @Override public BlockPos get(int index){Objects.checkIndex(index,size());if(index==0)return root;index--;return index<source.size()?source.get(index):target.get(index-source.size());}
    }
    private record ActiveRoutes(long revision, net.minecraft.core.Direction output, IndustrialTopology.Side target,IndustrialNetworkGraph.Snapshot graph) {
        ActiveRoutes(long revision,net.minecraft.core.Direction output,IndustrialTopology.Side target){this(revision,output,target,null);}
    }
    /** 只存本tick因自有轮机周转满而未成交的意图，不保存库存、能力或许可；次序就是原交易次序。 */
    private record DeferredDelivery(ActiveSteamSource steam, IndustrialPumpBlockEntity pump,
            IndustrialTopology.Endpoint source, IndustrialTopology.Endpoint target, Routes pumpRoutes,
            TurbineControllerBlockEntity owner, int epoch, int processed, long tick, long revision,
            int steamOffset, int steamTargets) {}
    private static final class World {
        long revision, tick = Long.MIN_VALUE;
        long pumpDispatches, exhaustedPumpSkips;
        int cursor, pumpCursor, steamCursor;
        List<Number> configuration;
        final Map<BlockPos, IndustrialPumpBlockEntity> pumps = new HashMap<>();
        final Map<BlockPos, Routes> routes = new HashMap<>();
        final Map<BlockPos, com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity.ActiveSteamSource> steam = new HashMap<>();
        final Map<BlockPos, ActiveRoutes> steamRoutes = new HashMap<>();
        final Map<BlockPos, Integer> steamTargetCursor = new HashMap<>();
        final IndustrialFlowBudget budget = new IndustrialFlowBudget();
        final Map<BlockPos, FluidStack> media = new HashMap<>();
        final IndustrialNetworkGraph.Registry graphs=new IndustrialNetworkGraph.Registry(budget,media);
        final ArrayDeque<IndustrialFlowBudget.Path> transactionPaths=new ArrayDeque<>();
        final List<DeferredDelivery> deferred = new ArrayList<>();
        final Set<TurbineControllerBlockEntity> released = Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<BlockPos> advancedSteamTargets = new HashSet<>();
        boolean retrying;
        long deferredAttempts, deferredDelivered;
        private IndustrialFlowBudget.Path borrowPath(){var path=transactionPaths.pollFirst();return path==null?new IndustrialFlowBudget.Path():path;}
        private void releasePath(IndustrialFlowBudget.Path path){path.clear();transactionPaths.addFirst(path);}
    }
    private IndustrialNetworkScheduler() {}
    /** 仅接收控制器合法tick生成的有限身份；请求不跨tick续用，也不登记其他设备主动源。 */
    public static void offerBoilerSteam(ServerLevel level, com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity.ActiveSteamSource source) {
        if (source.valid()) WORLDS.computeIfAbsent(level, ignored -> new World()).steam.put(source.position(), source);
    }
    public static void register(ServerLevel level, IndustrialPumpBlockEntity pump) { World world = WORLDS.computeIfAbsent(level, ignored -> new World()); world.pumps.put(pump.getBlockPos(), pump); invalidate(level, pump.getBlockPos()); }
    public static void unregister(ServerLevel level, IndustrialPumpBlockEntity pump) { World world = WORLDS.get(level); if (world != null) { world.pumps.remove(pump.getBlockPos(), pump); invalidate(level, pump.getBlockPos()); } }
    /** 仅撤销包含变更格或其邻格的缓存路线；额度账本不随拓扑变更清零。 */
    public static void invalidate(ServerLevel level, BlockPos pos) {
        World world = WORLDS.get(level); if (world == null) return;
        world.revision++;world.graphs.notified();
        world.routes.entrySet().removeIf(entry -> entry.getKey().distManhattan(pos) <= 1
                || touches(entry.getValue().source(), node -> node.distManhattan(pos) <= 1)
                || touches(entry.getValue().target(), node -> node.distManhattan(pos) <= 1));
        world.steamRoutes.entrySet().removeIf(entry -> {
            boolean changed = entry.getKey().distManhattan(pos) <= 1 || touches(entry.getValue().target(), node -> node.distManhattan(pos) <= 1);
            if (changed) world.steamTargetCursor.remove(entry.getKey()); return changed;
        });
    }
    private static boolean touches(IndustrialTopology.Side side, java.util.function.Predicate<BlockPos> predicate) {
        return side.nodes().stream().anyMatch(predicate) || side.endpoints().stream().anyMatch(endpoint -> predicate.test(endpoint.position()));
    }
    private static void invalidateChunk(ServerLevel level, net.minecraft.world.level.ChunkPos chunk) {
        World world = WORLDS.get(level); if (world == null) return; world.revision++;world.graphs.unload(chunk);
        java.util.function.Predicate<BlockPos> inChunk = pos -> new net.minecraft.world.level.ChunkPos(pos).equals(chunk);
        world.routes.entrySet().removeIf(entry -> inChunk.test(entry.getKey()) || touches(entry.getValue().source(), inChunk) || touches(entry.getValue().target(), inChunk)
                || entry.getValue().source().missingChunks().contains(chunk) || entry.getValue().target().missingChunks().contains(chunk));
        world.steamRoutes.entrySet().removeIf(entry -> {
            boolean changed = inChunk.test(entry.getKey()) || touches(entry.getValue().target(), inChunk) || entry.getValue().target().missingChunks().contains(chunk);
            if (changed) world.steamTargetCursor.remove(entry.getKey()); return changed;
        });
    }
    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load event) { if (event.getLevel() instanceof ServerLevel level) invalidateChunk(level, event.getChunk().getPos()); }
    @SubscribeEvent public static void chunkUnloaded(ChunkEvent.Unload event) { if (event.getLevel() instanceof ServerLevel level) invalidateChunk(level, event.getChunk().getPos()); }
    @SubscribeEvent public static void neighborChanged(net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level) invalidate(level, event.getPos());
    }
    @SubscribeEvent public static void worldUnloaded(LevelEvent.Unload event) { if (event.getLevel() instanceof ServerLevel level) {var world=WORLDS.remove(level);if(world!=null)world.graphs.clear();} }
    @SubscribeEvent public static void serverTick(LevelTickEvent.Post event) { if (event.getLevel() instanceof ServerLevel level) tick(level); }

    private static IFluidHandler resolve(ServerLevel level, IndustrialTopology.Endpoint endpoint,
                                        IndustrialTopology.DispatchBindings bindings) {
        if (!level.hasChunkAt(endpoint.position())) return null;
        return RestrictedFluidHandler.wrap(bindings.get(endpoint.position(), endpoint.side()).handler());
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
        World world = WORLDS.get(level); if (world == null || world.tick == level.getGameTime()) return; world.tick = level.getGameTime(); world.media.clear();world.graphs.beginTick();
        world.deferred.clear(); world.released.clear(); world.advancedSteamTargets.clear();
        var configuration = IndustrialFluidConfig.snapshot();
        if (!configuration.equals(world.configuration)) {
            world.configuration = configuration;world.graphs.clear(); world.routes.clear(); world.steamRoutes.clear(); world.steamTargetCursor.clear(); world.revision++;
            if (IndustrialFluidConfig.valid()) for (var pump : world.pumps.values()) if (!pump.isRemoved() && pump.hasNetwork())
                pump.getOrCreateNetwork().updateStressFor(pump, pump.calculateStressApplied());
        }
        List<IndustrialPumpBlockEntity> pumps = world.pumps.values().stream().filter(pump -> !pump.isRemoved() && level.hasChunkAt(pump.getBlockPos()))
                .sorted(Comparator.comparingLong(pump -> pump.getBlockPos().asLong())).toList();
        var steam = world.steam.values().stream().filter(source -> source.valid() && level.hasChunkAt(source.position()))
                .sorted(Comparator.comparingLong(source -> source.position().asLong())).toList();
        int cursor = world.cursor++;
        boolean steamFirst = (cursor & 1) == 0;
        int pumpStart = world.pumpCursor, steamStart = world.steamCursor;
        // 组内首位只在本组实际先行时推进；对方为空也算先行，不能与组间奇偶每tick同步递增。
        if (!pumps.isEmpty() && (!steamFirst || steam.isEmpty())) world.pumpCursor = Math.floorMod(pumpStart + 1, pumps.size());
        if (!steam.isEmpty() && (steamFirst || pumps.isEmpty())) world.steamCursor = Math.floorMod(steamStart + 1, steam.size());
        Runnable runPumps = () -> { for (int i = 0; i < pumps.size(); i++) {
            schedule(level, world, pumps.get(Math.floorMod(pumpStart + i, pumps.size())));
            // 一笔泵交易及其缓存/额度已闭合，才允许此前排队的入口利用真实释放的空间。
            retryReleased(level, world);
        } };
        Runnable runSteam = () -> { for (int i = 0; i < steam.size(); i++) scheduleSteam(level, world, steam.get(Math.floorMod(steamStart + i, steam.size()))); };
        try {
            if (steamFirst) { runSteam.run(); runPumps.run(); }
            else { runPumps.run(); runSteam.run(); }
        } finally {
            world.deferred.clear(); world.released.clear(); world.advancedSteamTargets.clear();
            world.steam.clear();
            Set<BlockPos> active = new HashSet<>(); steam.stream().filter(source -> source.valid()).forEach(source -> active.add(source.position()));
            world.steamRoutes.keySet().retainAll(active);
            world.steamTargetCursor.keySet().retainAll(active);
        }
    }
    /** 同一原BFS和端点单介质约束；实际路径中的泵必须仍有动力且方向合法。 */
    private static void scheduleSteam(ServerLevel level, World world, com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity.ActiveSteamSource source) {
        if (!IndustrialFluidConfig.valid() || !source.valid() || source.remaining() <= 0) return;
        ActiveRoutes routes = world.steamRoutes.get(source.position());
        var current=world.graphs.query(level,source.position(),source.outward(),true,true);
        if(current!=null){if(routes==null||routes.target()!=current.side()||routes.output()!=source.outward()){routes=new ActiveRoutes(world.revision,source.outward(),current.side(),current.graph());world.steamRoutes.put(source.position(),routes);}}
        else if (routes == null || routes.graph()!=null || routes.revision() != world.revision || routes.output() != source.outward() || routes.target().recovered(level)) {
            routes = new ActiveRoutes(world.revision, source.outward(), IndustrialTopology.searchBoiler(level, source.position(), source.outward()));
            world.steamRoutes.put(source.position(), routes);
        }
        var side = routes.target(); FluidStack selected = source.stock();
        if (selected.isEmpty() || side.nodes().isEmpty() || side.nodes().size() > IndustrialFluidConfig.MAX_NODES.get()) return;
        var bindings = new IndustrialTopology.DispatchBindings(level);
        for (var endpoint : side.endpoints()) {
            var handler = resolve(level, endpoint, bindings); if (handler == null) continue;
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack stack = handler.getFluidInTank(tank);
                if (!stack.isEmpty() && !FluidStack.isSameFluidSameComponents(selected, stack)) return;
            }
        }
        for (BlockPos node : routes.graph()==null?side.nodes():routes.graph().steamPumps(level,side)) if (IndustrialTopology.entityAt(level, node) instanceof IndustrialPumpBlockEntity member) {
            FluidStack stack = member.cachedFluid();
            if (!stack.isEmpty() && !FluidStack.isSameFluidSameComponents(selected, stack)) return;
        }
        var targets = side.endpoints(); int offset = Math.floorMod(world.steamTargetCursor.getOrDefault(source.position(), 0), Math.max(1, targets.size()));
        int delivered = 0;
        for (int i = 0; i < targets.size(); i++) {
            var target = targets.get((offset + i) % targets.size());
            if (!target.position().equals(source.position())) delivered += transactSteam(level, world, source, target, selected, bindings,routes.graph()==null?null:routes.graph().plan(target.path()));
        }
        // 每口只在自己真实交付后推进目标首位；无额度/拒收不消耗轮次，不能与组间奇偶同步。
        if (delivered > 0 && source.valid()) advanceSteamTarget(world, source, offset, targets.size());
    }
    /** 首轮和补交付共用一次目标轮转；一次物理口不能因同tick重试跳过两个首位。 */
    private static void advanceSteamTarget(World world, ActiveSteamSource source, int offset, int count) {
        if (count > 0 && world.advancedSteamTargets.add(source.position()))
            world.steamTargetCursor.put(source.position(), (offset + 1) % count);
    }
    private static void deferFullTurbine(ServerLevel level, World world, ActiveSteamSource steam,
            IndustrialPumpBlockEntity pump, IndustrialTopology.Endpoint source,
            IndustrialTopology.Endpoint target, Routes routes, IndustrialTopology.Binding binding, FluidStack fluid,
            long issuedRevision) {
        if (world.retrying || world.revision != issuedRevision || !binding.reusable()
                || !binding.valid().getAsBoolean() || !binding.reuseGuard().getAsBoolean() || !TurbineContent.isSupercritical(fluid)
                || !IndustrialTopology.stateAt(level, target.position()).is(TurbineContent.INLET.get())) return;
        var owner = TurbineStructure.ownerForPart(level, target.position());
        if (owner == null || !owner.validPort(target.position(), true)) return;
        var ledger = owner.ledger();
        if (ledger.exhaustCapacity() <= 0 || ledger.exhaust() < ledger.exhaustCapacity()) return;
        int count = 0, offset = 0;
        if (steam != null) {
            var active = world.steamRoutes.get(steam.position());
            if (active == null) return;
            count = active.target().endpoints().size();
            offset = Math.floorMod(world.steamTargetCursor.getOrDefault(steam.position(), 0), Math.max(1, count));
        }
        world.deferred.add(new DeferredDelivery(steam, pump, source, target, routes, owner,
                owner.portBindingEpoch(), ledger.processed(level.getGameTime()), level.getGameTime(),
                world.revision, offset, count));
    }
    /** 仅被本tick真实排汽唤醒；按原意图顺序各重试一次，仍重新解析绑定、原路径、库存和新许可。 */
    private static void retryReleased(ServerLevel level, World world) {
        if (world.released.isEmpty()) return;
        world.retrying = true;
        try {
            for (var iterator = world.deferred.iterator(); iterator.hasNext();) {
                var pending = iterator.next();
                if (!world.released.contains(pending.owner())) continue;
                if (pending.tick() != level.getGameTime() || pending.revision() != world.revision
                        || pending.owner().portBindingEpoch() != pending.epoch()
                        || !pending.owner().validPort(pending.target().position(), true)) {
                    iterator.remove(); continue;
                }
                var ledger = pending.owner().ledger();
                if (ledger.processed(pending.tick()) <= pending.processed()
                        || ledger.remainingInput(pending.target().position().asLong(), pending.tick()) <= 0) continue;
                iterator.remove(); world.deferredAttempts++;
                var bindings = new IndustrialTopology.DispatchBindings(level);
                int delivered = 0;
                if (pending.steam() != null && pending.steam().valid()) {
                    var routes = world.steamRoutes.get(pending.steam().position());
                    if (routes != null && routes.output() == pending.steam().outward()) {
                        var fluid = pending.steam().stock();
                        if (!fluid.isEmpty() && retryMediumMatches(level, world, routes.target().endpoints(),
                                routes.graph() == null ? routes.target().nodes() : routes.graph().steamPumps(level, routes.target()),
                                fluid, bindings) && world.revision == pending.revision()) delivered = transactSteam(level, world, pending.steam(), pending.target(),
                                fluid, bindings, routes.graph() == null ? null : routes.graph().plan(pending.target().path()));
                    }
                    if (delivered > 0 && pending.steam().valid())
                        advanceSteamTarget(world, pending.steam(), pending.steamOffset(), pending.steamTargets());
                } else if (pending.pump() != null && !pending.pump().isRemoved()
                        && world.routes.get(pending.pump().getBlockPos()) == pending.pumpRoutes()
                        && pending.pump().outputDirection() == pending.pumpRoutes().output()) {
                    var routes = pending.pumpRoutes();
                    FluidStack fluid = pending.pump().cachedFluid();
                    if (fluid.isEmpty() && pending.source() != null) {
                        var from = resolve(level, pending.source(), bindings);
                        if (from != null) for (int tank = 0; tank < from.getTanks(); tank++) {
                            var stock = from.getFluidInTank(tank);
                            if (!stock.isEmpty()) { fluid = stock; break; }
                        }
                    }
                    if (!fluid.isEmpty() && retryMediumMatches(level, world, routes.endpoints,
                            routes.graph == null ? routes.nodes : routes.graph.pumpCandidates(level, routes.nodes, routes.pumpOrder),
                            fluid, bindings) && world.revision == pending.revision())
                        delivered = transact(level, world, pending.pump(), pending.source(), pending.target(), bindings, routes);
                    if (delivered > 0) pending.pump().display(pending.pump().actualFlow() + delivered, "running");
                }
                world.deferredDelivered += delivered;
            }
        } finally { world.retrying = false; world.released.clear(); }
    }
    /** 首轮预检不能跨其他泵交易续用；补交付前重查端点库存与沿路泵缓存，未知回调仍走原能力入口。 */
    private static boolean retryMediumMatches(ServerLevel level, World world,
            List<IndustrialTopology.Endpoint> endpoints, Iterable<BlockPos> nodes, FluidStack fluid,
            IndustrialTopology.DispatchBindings bindings) {
        if (!IndustrialFluidConfig.valid()) return false;
        for (var endpoint : endpoints) {
            var handler = resolve(level, endpoint, bindings);
            if (handler == null) continue;
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                var stack = handler.getFluidInTank(tank);
                if (!stack.isEmpty() && !FluidStack.isSameFluidSameComponents(fluid, stack)) return false;
            }
        }
        for (var node : nodes) if (IndustrialTopology.entityAt(level, node) instanceof IndustrialPumpBlockEntity pump) {
            var stack = pump.cachedFluid();
            if (!stack.isEmpty() && !FluidStack.isSameFluidSameComponents(fluid, stack)) return false;
        }
        return true;
    }
    /** 收据在此同步边界内闭合；短收退原池mB/HU与本次口额度，管段只按实交付收费。 */
    private static int transactSteam(ServerLevel level, World world, com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity.ActiveSteamSource source,
            IndustrialTopology.Endpoint target, FluidStack fluid, IndustrialTopology.DispatchBindings bindings,IndustrialRoutePlan plan) {
        if (IndustrialTopology.rejectsInsertion(level, target)) return 0;
        var binding = bindings.get(target.position(), target.side());
        IFluidHandler to = RestrictedFluidHandler.wrap(binding.handler());
        if (to == null || RestrictionPolicy.restricted(fluid) && !permittedRestrictedEndpoint(level, target)) return 0;
        var path = target.path();var limits=world.borrowPath();
        try {
        if(plan!=null){if(!plan.prepare(level,limits,null,fluid))return 0;}
        else {world.graphs.flushMedia();
        for (BlockPos node : path) {
            FluidStack medium = world.media.get(node);
            if (medium != null && !FluidStack.isSameFluidSameComponents(medium, fluid)) return 0;
            IndustrialTopology.prepareLimit(level,node,limits);
        }        }

        int allowed = world.budget.reserve(level.getGameTime(), limits, source.remaining(), false);
        if (allowed <= 0) return 0;
        final long now = level.getGameTime(), revision = world.revision;
        IndustrialTransferPermit permit = new IndustrialTransferPermit(null, to, allowed,
                () -> level.getGameTime() == now && world.revision == revision && source.valid() && binding.valid().getAsBoolean()
                        && (plan==null?path.stream().allMatch(node -> IndustrialTopology.limit(level, node) > 0):plan.positive(level)));
        IFluidHandler receiver = ((RestrictedFluidHandler)to).authorized(permit);
        int simulated = Math.min(allowed, Math.max(0, receiver.fill(fluid.copyWithAmount(allowed), IFluidHandler.FluidAction.SIMULATE)));
        if (simulated == 0) {
            deferFullTurbine(level, world, source, null, null, target, null, binding, fluid, revision);
            return 0;
        }
        if (!source.valid()) return 0;
        try (var receipt = source.withdraw(simulated)) {
            if (receipt == null || receipt.amount() == 0) return 0;
            int delivered = Math.min(receipt.amount(), Math.max(0, receiver.fill(fluid.copyWithAmount(receipt.amount()), IFluidHandler.FluidAction.EXECUTE)));
            receipt.settle(delivered); source.delivered(delivered);
            if (delivered > 0) {
                world.budget.reserve(now, limits, delivered, true);
                if(plan!=null)plan.settle(world.budget,now,limits,fluid,false);else for (BlockPos node : path) world.graphs.markRaw(node,fluid);
            }
            return delivered;
        }
        } finally {world.releasePath(limits);}
    }
    private static void schedule(ServerLevel level, World world, IndustrialPumpBlockEntity pump) {
        world.pumpDispatches++;
        if (!IndustrialFluidConfig.valid() || !Float.isFinite(pump.getSpeed())) { pump.display(0, "invalid"); return; }
        pump.resetBottleneck();
        int rating = pump.flowLimit(); if (rating == 0) { pump.display(0, "stopped"); return; }
        Routes routes = world.routes.get(pump.getBlockPos());
        var currentSource=world.graphs.query(level,pump.getBlockPos(),pump.outputDirection().getOpposite(),false,false);
        var currentTarget=world.graphs.query(level,pump.getBlockPos(),pump.outputDirection(),true,false);
        if(currentSource!=null&&currentTarget!=null){
            if(routes==null||routes.source()!=currentSource.side()||routes.target()!=currentTarget.side()||routes.output()!=pump.outputDirection()){
                routes=new Routes(pump.getBlockPos(),world.revision,pump.outputDirection(),currentSource.side(),currentTarget.side(),currentSource.graph()==currentTarget.graph()?currentSource.graph():null);world.routes.put(pump.getBlockPos(),routes);
            }
        } else if (routes == null || routes.graph!=null || routes.output() != pump.outputDirection() || routes.source().recovered(level) || routes.target().recovered(level)) {
            routes = new Routes(pump.getBlockPos(),world.revision, pump.outputDirection(), IndustrialTopology.search(level, pump, false), IndustrialTopology.search(level, pump, true));
            world.routes.put(pump.getBlockPos(), routes);
        }
        Set<BlockPos> nodes = routes.nodes;
        if (nodes.size() > IndustrialFluidConfig.MAX_NODES.get()) { pump.display(0, "too_large"); return; }
        if (routes.target().endpoints().isEmpty() || pump.cachedFluid().isEmpty() && routes.source().endpoints().isEmpty()) {
            pump.display(0, routes.target().endpoints().isEmpty() ? routes.target().failure() : routes.source().failure()); return;
        }
        int delivered = 0;
        var targets = routes.target().endpoints(); var sources = routes.source().endpoints();
        var bindings = new IndustrialTopology.DispatchBindings(level);
        // 管段无库存；当前端点与沿路泵仍持有其他介质时，整张连接图拒绝混输。
        List<IndustrialTopology.Endpoint> endpoints = routes.endpoints;
        FluidStack selected = pump.cachedFluid();
        for (var endpoint : endpoints) {
            var handler = resolve(level, endpoint, bindings); if (handler == null) continue;
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack stack = handler.getFluidInTank(tank); if (stack.isEmpty()) continue;
                if (!selected.isEmpty() && !FluidStack.isSameFluidSameComponents(selected, stack)) { pump.display(0, "mixed"); return; }
                selected = stack;
            }
        }
        for (BlockPos node : routes.graph==null?nodes:routes.graph.pumpCandidates(level,nodes,routes.pumpOrder)) if (level.hasChunkAt(node) && IndustrialTopology.entityAt(level, node) instanceof IndustrialPumpBlockEntity member) {
            FluidStack stack = member.cachedFluid(); if (stack.isEmpty()) continue;
            if (!selected.isEmpty() && !FluidStack.isSameFluidSameComponents(selected, stack)) { pump.display(0, "mixed"); return; }
            selected = stack;
        }
        int targetStart = pump.nextCursor(targets.size(), false);
        int sourceStart = pump.nextCursor(sources.size(), true);
        // 原混液检查、状态与轮转之后才跳过；不指定主泵，不缓存库存，缓存持液继续原交易。
        if(pump.flowLimit()==rating&&world.budget.canSkipPump(level.getGameTime(),pump.getBlockPos().asLong(),rating,!pump.cachedFluid().isEmpty())&&routes.rootIsMinimum(level,rating)) {
            world.exhaustedPumpSkips++;pump.display(0,"blocked");return;
        }
        for (int i = 0; i < targets.size(); i++) {
            var target = targets.get((targetStart + i) % targets.size());
            // 缓存只需要合法输出路线；抽取必须同时拥有源路径，不能用旧源句柄跨断管运输。
            if (!pump.cachedFluid().isEmpty()) delivered += transact(level, world, pump, null, target, bindings,routes);
            else for (int j = 0; j < sources.size(); j++) {
                var source = sources.get((sourceStart + j) % sources.size());
                if (source.position().equals(target.position())) continue;
                delivered += transact(level, world, pump, source, target, bindings,routes);
                if (!pump.cachedFluid().isEmpty()) break;
            }
        }
        pump.display(delivered, delivered > 0 ? "running" : "blocked");
    }
    private static int transact(ServerLevel level, World world, IndustrialPumpBlockEntity pump, IndustrialTopology.Endpoint source, IndustrialTopology.Endpoint target,
                                IndustrialTopology.DispatchBindings bindings,Routes routes) {
        if (IndustrialTopology.rejectsInsertion(level, target)
                || source != null && IndustrialTopology.rejectsExtraction(level, source)) return 0;
        var sourceBinding = source == null ? null : bindings.get(source.position(), source.side());
        var targetBinding = bindings.get(target.position(), target.side());
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
        List<BlockPos> path=routes.path(source,target);var plan=routes.plan(source,target);var limits=world.borrowPath();
        try {
        if(plan!=null){if(!plan.prepare(level,limits,pump,medium))return 0;}
        else {world.graphs.flushMedia();
        for (BlockPos node : path) {
            FluidStack selected = world.media.get(node);
            if (selected != null && !FluidStack.isSameFluidSameComponents(selected, medium)) return 0;
            int capacity = IndustrialTopology.prepareLimit(level,node,limits);
            pump.considerBottleneck(capacity);
        }        }

        int allowed = world.budget.reserve(level.getGameTime(), limits, pump.flowLimit(), false); if (allowed == 0) return 0;
        final long revision = world.revision, now = level.getGameTime();
        final FluidStack fluid = medium.copy();
        IndustrialTransferPermit permit = new IndustrialTransferPermit(from, to, allowed,
                () -> level.getGameTime() == now && world.revision == revision && !pump.isRemoved() && pump.flowLimit() > 0
                        && (plan==null?path.stream().allMatch(node -> IndustrialTopology.limit(level, node) > 0):plan.positive(level))
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
            public int add(int requested, boolean execute) {
                int accepted = authorizedTarget.fill(fluid.copyWithAmount(requested), execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
                if (!execute && accepted == 0) deferFullTurbine(level, world, null, pump, source, target, routes, targetBinding, fluid, revision);
                return accepted;
            }
        };
        int delivered = transfer.move(sourceStore, targetStore, allowed, IndustrialFluidConfig.PUMP_FLOW.get(), true);
        if (extracted[0] > 0 || delivered > 0) {
            if(plan!=null){plan.crossPump(limits,pump.getBlockPos(),extracted[0],delivered);plan.settle(world.budget,now,limits,fluid,true);}
            else {
            limits.cross(pump.getBlockPos().asLong(),Math.max(extracted[0],delivered));
            if(source!=null)for(BlockPos node:source.path())limits.cross(node.asLong(),extracted[0]);
            for(BlockPos node:target.path())limits.cross(node.asLong(),delivered);
            // 短收余量只经过输入侧；输出侧按实收收费，泵仍按抽取/交付中的较大值收费。
            for(BlockPos node:path) {
                int amount=limits.takeCrossed(node.asLong());if(amount<=0)continue;
                world.budget.charge(now,limits,node.asLong(),amount);
                world.graphs.markRaw(node,fluid);
            }
        }
        }
        if (before != transfer.cached() || extracted[0] > 0) pump.setCache(transfer.cached() == 0 ? FluidStack.EMPTY : fluid.copyWithAmount(transfer.cached()));
        if (extracted[0] > 0 && source != null
                && IndustrialTopology.stateAt(level, source.position()).is(TurbineContent.EXHAUST.get())) {
            var owner = TurbineStructure.ownerForPart(level, source.position());
            if (owner != null) world.released.add(owner);
        }
        return delivered;
        } finally {world.releasePath(limits);}
    }
}
