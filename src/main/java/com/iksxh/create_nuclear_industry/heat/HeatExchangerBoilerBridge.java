package com.iksxh.create_nuclear_industry.heat;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 只读 Create 负载适配和跨区块热缓存失效桥；不依赖 activeHeat 判定启动。
 * 一切查找先核对区块已加载，防止源端生命周期通知加载远处控制器。
 */
public final class HeatExchangerBoilerBridge {
    /** 待刷新弱表和其位置集合统一加锁；锁内只交接请求，不读取世界或调用热源。 */
    private static final Object PENDING_LOCK = new Object();
    private static final Map<Level, Set<BlockPos>> PENDING = new WeakHashMap<>();
    /** 两张弱表共用登记锁；仅保护结构修改和引用快照，不保护或执行任何世界操作。 */
    private static final Object SOURCES_LOCK = new Object();
    private static final Set<NuclearHeatExchangerBlockEntity> ACTIVE = java.util.Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<NuclearHeatExchangerBlockEntity> LOADED = java.util.Collections.newSetFromMap(new WeakHashMap<>());
    private HeatExchangerBoilerBridge() {}

    /** 仅登记具有服务端世界且已经发布热的本模组源；任一侧的撤销均须与快照互斥。 */
    static void track(NuclearHeatExchangerBlockEntity machine, boolean active) {
        if (active && !serverSource(machine)) return;
        synchronized (SOURCES_LOCK) {
            if (active) ACTIVE.add(machine); else ACTIVE.remove(machine);
        }
    }

    /** 客户端onLoad和未附世界的实体不能进入服务端逐tick审计。 */
    static void registerLoaded(NuclearHeatExchangerBlockEntity machine) {
        if (!serverSource(machine)) return;
        synchronized (SOURCES_LOCK) { LOADED.add(machine); }
    }

    /** 移除不依赖当前世界侧，以保证卸载或世界脱离后的生命周期通知仍能安全清理。 */
    static void unregisterLoaded(NuclearHeatExchangerBlockEntity machine) {
        synchronized (SOURCES_LOCK) { LOADED.remove(machine); }
    }

    private static boolean serverSource(NuclearHeatExchangerBlockEntity machine) {
        Level level = machine.getLevel();
        return level != null && !level.isClientSide;
    }

    /** 临时强引用只覆盖本次审计；复制完成即释放锁，允许世界/热回调重入登记撤销。 */
    private static List<NuclearHeatExchangerBlockEntity> snapshot(Set<NuclearHeatExchangerBlockEntity> sources) {
        synchronized (SOURCES_LOCK) { return List.copyOf(sources); }
    }

    /** 非供热成员停 tick 也须撤销整列句柄，否则恢复时旧端点能力可能重活。 */
    private static void auditLoadedSources() {
        for (var machine : snapshot(LOADED)) {
            if (!machine.current()) unregisterLoaded(machine);
            else if (!machine.canTick()) machine.pauseHeat();
        }
    }

    /**
     * FULL 不等于方块正在 tick。源退出原生 tick 门时必须主动撤销邻区块的缓存热，
     * 不能等永远不会运行的源端 ticker；集合采用弱引用并在失活/移除时清理。
     */
    private static void auditActiveSources() {
        for (var machine : snapshot(ACTIVE)) {
            if (!machine.current() || !machine.canTick() || HeatExchangerLine.find(machine) == null) {
                machine.pauseHeat();
                track(machine, false);
            }
        }
    }

    @SubscribeEvent public static void beforeServerTick(ServerTickEvent.Pre event) {
        auditLoadedSources();
        auditActiveSources();
    }

    /** 公开热源回调纯读取；热级由已支付的服务端 tick 发布。 */
    public static float heat(Level level, BlockPos pos, BlockState state) {
        if (!level.hasChunkAt(pos)) return BoilerHeater.NO_HEAT;
        return level.getBlockEntity(pos) instanceof NuclearHeatExchangerBlockEntity machine
                ? machine.publishedHeat() : BoilerHeater.NO_HEAT;
    }

    /** 上方储罐必须处在控制器底层；侧向贴到锅炉中段不算负载。 */
    public static FluidTankBlockEntity controller(Level level, BlockPos source) {
        BlockPos above = source.above();
        if (level == null || !level.hasChunkAt(above)
                || !(level.getBlockEntity(above) instanceof FluidTankBlockEntity tank)) return null;
        BlockPos controllerPos = tank.getController();
        if (controllerPos == null || controllerPos.getY() != above.getY() || !level.hasChunkAt(controllerPos)) return null;
        return level.getBlockEntity(controllerPos) instanceof FluidTankBlockEntity controller && !controller.isRemoved()
                && controller.isController() ? controller : null;
    }

    /** 原生水量为近期输入采样估计，沿用其至少一级的语义；完全不读取热量缓存。 */
    public static boolean qualified(FluidTankBlockEntity controller) {
        return controller != null && controller.boiler.isActive()
                && controller.boiler.getMaxHeatLevelForBoilerSize(controller.getTotalTankSize()) >= 1
                && controller.boiler.getMaxHeatLevelForWaterSupply() >= 1;
    }

    /**
     * 立即重扫已加载底面，采用 Create 相同整数累加及被动热规则。
     * 原生 updateTemperature 会读取整个底面；这里跳过未加载区块，卸载通知不强制加载。
     */
    public static void refresh(Level level, BlockPos controllerPos) {
        if (level == null || level.isClientSide || controllerPos == null || !level.hasChunkAt(controllerPos)
                || !(level.getBlockEntity(controllerPos) instanceof FluidTankBlockEntity controller)
                || !controller.isController() || controller.isRemoved()) return;
        int active = 0;
        boolean passive = false;
        for (int x = 0; x < controller.getWidth(); x++) for (int z = 0; z < controller.getWidth(); z++) {
            BlockPos source = controllerPos.offset(x, -1, z);
            if (!level.hasChunkAt(source)) continue;
            float heat = BoilerHeater.findHeat(level, source, level.getBlockState(source));
            if (heat == 0) passive = true;
            else if (heat > 0 && Float.isFinite(heat)) active += (int) heat;
        }
        passive &= active == 0;
        boolean changed = controller.boiler.activeHeat != active || controller.boiler.passiveHeat != passive;
        controller.boiler.activeHeat = active;
        controller.boiler.passiveHeat = passive;
        controller.boiler.needsHeatLevelUpdate = false;
        if (changed) controller.notifyUpdate();
    }

    /** 控制器恢复可能带有持久化旧热值；排队到加载完成后重新读取真实热源。 */
    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide
                || !(event.getChunk() instanceof LevelChunk chunk)) return;
        for (var be : chunk.getBlockEntities().values()) if (be instanceof FluidTankBlockEntity tank) {
            BlockPos controller = tank.getController();
            if (controller != null) {
                synchronized (PENDING_LOCK) {
                    PENDING.computeIfAbsent(level, ignored -> new HashSet<>()).add(controller.immutable());
                }
            }
        }
    }

    /** 卸载事件可能早于实体回调；先关闭源，再使仍加载的跨区块控制器重算。 */
    @SubscribeEvent public static void chunkUnloaded(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide() || !(event.getChunk() instanceof LevelChunk chunk)) return;
        for (var be : chunk.getBlockEntities().values())
            if (be instanceof NuclearHeatExchangerBlockEntity machine) machine.suspend();
    }

    /**
     * 原子交接独立位置快照；本轮清理必须早于世界回调，使重入/并发登记保留到下一轮。
     * 世界强引用仅随本轮局部批次存活，长期队列继续采用弱引用。
     */
    private static Map<Level, Set<BlockPos>> takePending() {
        synchronized (PENDING_LOCK) {
            if (PENDING.isEmpty()) return Map.of();
            Map<Level, Set<BlockPos>> batch = new HashMap<>();
            PENDING.forEach((level, positions) -> batch.put(level, Set.copyOf(positions)));
            PENDING.clear();
            return batch;
        }
    }

    /** 仅在锁外刷新本轮加载事件登记的位置，不逐 tick 扫描整个世界。 */
    @SubscribeEvent public static void afterServerTick(ServerTickEvent.Post event) {
        auditActiveSources();
        takePending().forEach((level, positions) -> positions.forEach(pos -> refresh(level, pos)));
    }
}
