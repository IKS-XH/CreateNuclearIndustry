package com.iksxh.create_nuclear_industry.heat;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import java.util.HashSet;
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
    private static final Map<Level, Set<BlockPos>> PENDING = new WeakHashMap<>();
    private static final Set<NuclearHeatExchangerBlockEntity> ACTIVE = java.util.Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<NuclearHeatExchangerBlockEntity> LOADED = java.util.Collections.newSetFromMap(new WeakHashMap<>());
    private HeatExchangerBoilerBridge() {}

    /** 仅登记本模组已经发布热的源，不追踪世界中的普通 Create 锅炉。 */
    static void track(NuclearHeatExchangerBlockEntity machine, boolean active) {
        if (active) ACTIVE.add(machine); else ACTIVE.remove(machine);
    }
    static void registerLoaded(NuclearHeatExchangerBlockEntity machine) { LOADED.add(machine); }
    static void unregisterLoaded(NuclearHeatExchangerBlockEntity machine) { LOADED.remove(machine); }

    /** 非供热成员停 tick 也须撤销整列句柄，否则恢复时旧端点能力可能重活。 */
    private static void auditLoadedSources() {
        for (var machine : java.util.List.copyOf(LOADED)) {
            if (!machine.current()) LOADED.remove(machine);
            else if (!machine.canTick()) machine.pauseHeat();
        }
    }

    /**
     * FULL 不等于方块正在 tick。源退出原生 tick 门时必须主动撤销邻区块的缓存热，
     * 不能等永远不会运行的源端 ticker；集合采用弱引用并在失活/移除时清理。
     */
    private static void auditActiveSources() {
        for (var machine : java.util.List.copyOf(ACTIVE)) {
            if (!machine.current() || !machine.canTick() || HeatExchangerLine.find(machine) == null) {
                machine.pauseHeat();
                ACTIVE.remove(machine);
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
            if (controller != null) PENDING.computeIfAbsent(level, ignored -> new HashSet<>()).add(controller.immutable());
        }
    }

    /** 卸载事件可能早于实体回调；先关闭源，再使仍加载的跨区块控制器重算。 */
    @SubscribeEvent public static void chunkUnloaded(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide() || !(event.getChunk() instanceof LevelChunk chunk)) return;
        for (var be : chunk.getBlockEntities().values())
            if (be instanceof NuclearHeatExchangerBlockEntity machine) machine.suspend();
    }

    /** 仅处理加载事件登记的位置，不逐 tick 扫描整个世界或保存世界引用。 */
    @SubscribeEvent public static void afterServerTick(ServerTickEvent.Post event) {
        auditActiveSources();
        PENDING.forEach((level, positions) -> positions.forEach(pos -> refresh(level, pos)));
        PENDING.clear();
    }
}
