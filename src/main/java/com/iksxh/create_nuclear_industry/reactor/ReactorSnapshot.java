package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.structure.ReactorDimensions;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * 实际尺寸反应堆的不可变、加载器无关状态快照。
 * 实际内部映射中未出现的坐标代表空堆芯列；仪表端口拥有完整快照的权威副本。
 */
public record ReactorSnapshot(
        Map<CoreColumnPosition, FuelColumnState> fuelColumns,
        Map<CoreColumnPosition, ControlRodColumnState> controlRodColumns,
        long coldCoolantMb,
        long hotCoolantMb,
        long meltdownProgressTicks,
        boolean meltdownCountdownStarted,
        Map<CoreColumnPosition, Double> scramSavedTargetDepths,
        boolean scramRequested,
        boolean meltdownEventPublished,
        ReactorDimensions dimensions
) {
    public static final int OUTER_SIZE = 5;
    public static final int INTERNAL_HEIGHT = OUTER_SIZE - 2;
    public static final int MAX_CORE_COLUMNS = CoreColumnPosition.GRID_SIZE * CoreColumnPosition.GRID_SIZE;

    public ReactorSnapshot {
        if (dimensions == null) throw new IllegalArgumentException("reactor dimensions are required");
        fuelColumns = immutableOrderedCopy(fuelColumns, "fuel columns");
        controlRodColumns = immutableOrderedCopy(controlRodColumns, "control rod columns");
        scramSavedTargetDepths = immutableTargetCopy(scramSavedTargetDepths);

        for (CoreColumnPosition position : fuelColumns.keySet()) {
            if (controlRodColumns.containsKey(position)) {
                throw new IllegalArgumentException("a core position cannot contain both a fuel and control rod column");
            }
        }
        if (fuelColumns.size() + controlRodColumns.size() > dimensions.columnCount()) {
            throw new IllegalArgumentException("the core contains more columns than its actual dimensions");
        }
        if (fuelColumns.keySet().stream().anyMatch(p -> !dimensions.containsColumn(p))
                || controlRodColumns.keySet().stream().anyMatch(p -> !dimensions.containsColumn(p))) {
            throw new IllegalArgumentException("column lies outside actual reactor dimensions");
        }
        if (coldCoolantMb < 0L || hotCoolantMb < 0L) {
            throw new IllegalArgumentException("coolant ledger amounts must be non-negative");
        }
        if (meltdownProgressTicks < 0L) {
            throw new IllegalArgumentException("meltdown progress must be non-negative");
        }
        if (!meltdownCountdownStarted && meltdownProgressTicks != 0L) {
            throw new IllegalArgumentException("inactive meltdown countdown cannot have progress");
        }
        if (meltdownEventPublished && !meltdownCountdownStarted) {
            throw new IllegalArgumentException("published meltdown event requires a started countdown");
        }
        for (CoreColumnPosition position : scramSavedTargetDepths.keySet()) {
            if (!controlRodColumns.containsKey(position)) {
                throw new IllegalArgumentException("SCRAM restore targets must identify control rod columns");
            }
        }
        if (!scramRequested && !scramSavedTargetDepths.isEmpty()) {
            throw new IllegalArgumentException("inactive SCRAM cannot retain restore targets");
        }
    }

    /** 默认五格构造入口保留原示例；正式结构使用显式尺寸。 */
    public ReactorSnapshot(Map<CoreColumnPosition, FuelColumnState> fuels,
            Map<CoreColumnPosition, ControlRodColumnState> rods, long cold, long hot,
            long progress, boolean started, Map<CoreColumnPosition, Double> targets,
            boolean scram, boolean published) {
        this(fuels, rods, cold, hot, progress, started, targets, scram, published, ReactorDimensions.DEFAULT);
    }

    /** 只为首次成型的空状态指定实际尺寸，不改变库存或列状态。 */
    public ReactorSnapshot withDimensions(ReactorDimensions nextDimensions) {
        return new ReactorSnapshot(fuelColumns, controlRodColumns, coldCoolantMb, hotCoolantMb,
                meltdownProgressTicks, meltdownCountdownStarted, scramSavedTargetDepths,
                scramRequested, meltdownEventPublished, nextDimensions);
    }

    /** 兼容没有 SCRAM 状态字段的旧快照构造形式。 */
    public ReactorSnapshot(
            Map<CoreColumnPosition, FuelColumnState> fuelColumns,
            Map<CoreColumnPosition, ControlRodColumnState> controlRodColumns,
            long coldCoolantMb,
            long hotCoolantMb,
            long meltdownProgressTicks,
            boolean meltdownCountdownStarted
    ) {
        this(fuelColumns, controlRodColumns, coldCoolantMb, hotCoolantMb,
                meltdownProgressTicks, meltdownCountdownStarted, Map.of(), false, false);
    }

    /** 兼容没有融毁事件去重字段的旧快照构造形式。 */
    public ReactorSnapshot(
            Map<CoreColumnPosition, FuelColumnState> fuelColumns,
            Map<CoreColumnPosition, ControlRodColumnState> controlRodColumns,
            long coldCoolantMb,
            long hotCoolantMb,
            long meltdownProgressTicks,
            boolean meltdownCountdownStarted,
            Map<CoreColumnPosition, Double> scramSavedTargetDepths,
            boolean scramRequested
    ) {
        this(fuelColumns, controlRodColumns, coldCoolantMb, hotCoolantMb,
                meltdownProgressTicks, meltdownCountdownStarted, scramSavedTargetDepths,
                scramRequested, false);
    }

    /** 创建没有燃料、控制棒、冷却剂和融毁进度的空快照。 */
    public static ReactorSnapshot empty() {
        return new ReactorSnapshot(Map.of(), Map.of(), 0L, 0L, 0L, false);
    }

    /** 创建只含一根燃料列的测试快照。 */
    public static ReactorSnapshot singleFuelColumn(CoreColumnPosition position, FuelColumnState state) {
        return new ReactorSnapshot(Map.of(position, state), Map.of(), 0L, 0L, 0L, false);
    }

    /** 创建只含一根控制棒列的测试快照。 */
    public static ReactorSnapshot singleControlRodColumn(
            CoreColumnPosition position,
            ControlRodColumnState state
    ) {
        return new ReactorSnapshot(Map.of(), Map.of(position, state), 0L, 0L, 0L, false);
    }

    /** 返回已明确占用的燃料列与控制棒列数量。 */
    public int occupiedColumnCount() {
        return fuelColumns.size() + controlRodColumns.size();
    }

    /** 判断实际截面内所有列坐标是否都已有燃料或控制棒角色。 */
    public boolean hasCompleteCoreColumnSet() {
        return occupiedColumnCount() == dimensions.columnCount();
    }

    /**
     * 只替换共享冷却剂账本并返回新快照；仪表端口仍是完整快照的唯一所有者，流体
     * capability 不得在端口实体中创建独立库存。
     */
    public ReactorSnapshot withCoolantInventories(long nextColdCoolantMb, long nextHotCoolantMb) {
        return new ReactorSnapshot(
                fuelColumns,
                controlRodColumns,
                nextColdCoolantMb,
                nextHotCoolantMb,
                meltdownProgressTicks,
                meltdownCountdownStarted,
                scramSavedTargetDepths,
                scramRequested,
                meltdownEventPublished, dimensions
        );
    }

    /** 判断 SCRAM 请求是否仍绑定至少一根控制棒列。 */
    public boolean scramActive() {
        return scramRequested && !controlRodColumns.isEmpty();
    }

    /** 替换列状态并裁剪已不存在控制棒列的 SCRAM 恢复目标。 */
    public ReactorSnapshot withColumns(
            Map<CoreColumnPosition, FuelColumnState> nextFuelColumns,
            Map<CoreColumnPosition, ControlRodColumnState> nextControlRodColumns
    ) {
        TreeMap<CoreColumnPosition, Double> nextSavedTargetDepths = new TreeMap<>();
        scramSavedTargetDepths.forEach((position, targetDepth) -> {
            if (nextControlRodColumns.containsKey(position)) {
                nextSavedTargetDepths.put(position, targetDepth);
            }
        });
        return new ReactorSnapshot(
                nextFuelColumns,
                nextControlRodColumns,
                coldCoolantMb,
                hotCoolantMb,
                meltdownProgressTicks,
                meltdownCountdownStarted,
                nextSavedTargetDepths,
                scramRequested && !nextControlRodColumns.isEmpty(),
                meltdownEventPublished, dimensions
        );
    }

    /**
     * 替换一个已绑定燃料列的状态；物品事务通过此入口提交，所有其他列和全堆状态保持不变。
     * 列状态仍由仪表端口快照唯一拥有，端口方块不应缓存副本。
     */
    public ReactorSnapshot withFuelColumn(CoreColumnPosition position, FuelColumnState state) {
        if (position == null || state == null) {
            throw new IllegalArgumentException("fuel column position and state are required");
        }
        if (controlRodColumns.containsKey(position)) {
            throw new IllegalArgumentException("a control rod column cannot receive fuel");
        }
        TreeMap<CoreColumnPosition, FuelColumnState> nextFuelColumns = new TreeMap<>(fuelColumns);
        nextFuelColumns.put(position, state);
        return new ReactorSnapshot(
                nextFuelColumns,
                controlRodColumns,
                coldCoolantMb,
                hotCoolantMb,
                meltdownProgressTicks,
                meltdownCountdownStarted,
                scramSavedTargetDepths,
                scramRequested,
                meltdownEventPublished, dimensions
        );
    }

    /** 去除仅供运行时计算的燃料投影，保留完整性、热量、燃耗余量和全堆状态。 */
    public ReactorSnapshot withoutFuelAssemblies() {
        TreeMap<CoreColumnPosition, FuelColumnState> nextFuelColumns = new TreeMap<>();
        fuelColumns.forEach((position, state) ->
                nextFuelColumns.put(position, state.withoutFuelAssemblyProjection()));
        return new ReactorSnapshot(
                nextFuelColumns,
                controlRodColumns,
                coldCoolantMb,
                hotCoolantMb,
                meltdownProgressTicks,
                meltdownCountdownStarted,
                scramSavedTargetDepths,
                scramRequested,
                meltdownEventPublished, dimensions
        );
    }

    /** 替换融毁进度与启动标记，进度单位为服务端 tick。 */
    public ReactorSnapshot withMeltdown(long nextProgressTicks, boolean nextStarted) {
        return new ReactorSnapshot(
                fuelColumns,
                controlRodColumns,
                coldCoolantMb,
                hotCoolantMb,
                nextProgressTicks,
                nextStarted,
                scramSavedTargetDepths,
                scramRequested,
                nextStarted && meltdownEventPublished, dimensions
        );
    }

    /** 替换完成事件去重标记；该标记只服务端持久化，不改变热工状态。 */
    public ReactorSnapshot withMeltdownEventPublished(boolean nextPublished) {
        return new ReactorSnapshot(
                fuelColumns,
                controlRodColumns,
                coldCoolantMb,
                hotCoolantMb,
                meltdownProgressTicks,
                meltdownCountdownStarted,
                scramSavedTargetDepths,
                scramRequested,
                nextPublished, dimensions
        );
    }

    /** 替换 SCRAM 保存目标和请求标志，并由构造器校验列归属不变量。 */
    public ReactorSnapshot withScramState(
            Map<CoreColumnPosition, Double> nextSavedTargetDepths,
            boolean nextRequested
    ) {
        return new ReactorSnapshot(
                fuelColumns,
                controlRodColumns,
                coldCoolantMb,
                hotCoolantMb,
                meltdownProgressTicks,
                meltdownCountdownStarted,
                nextSavedTargetDepths,
                nextRequested,
                meltdownEventPublished, dimensions
        );
    }

    private static <T> Map<CoreColumnPosition, T> immutableOrderedCopy(
            Map<CoreColumnPosition, T> source,
            String name
    ) {
        if (source == null) {
            throw new IllegalArgumentException(name + " map is required");
        }
        TreeMap<CoreColumnPosition, T> ordered = new TreeMap<>();
        source.forEach((position, state) -> {
            if (position == null || state == null) {
                throw new IllegalArgumentException(name + " must contain non-null positions and states");
            }
            ordered.put(position, state);
        });
        return Collections.unmodifiableMap(ordered);
    }

    private static Map<CoreColumnPosition, Double> immutableTargetCopy(
            Map<CoreColumnPosition, Double> source
    ) {
        if (source == null) {
            throw new IllegalArgumentException("SCRAM restore targets are required");
        }
        TreeMap<CoreColumnPosition, Double> ordered = new TreeMap<>();
        source.forEach((position, target) -> {
            if (position == null || target == null || !Double.isFinite(target)
                    || target < 0.0D || target > 1.0D) {
                throw new IllegalArgumentException("SCRAM restore targets must be finite values in [0, 1]");
            }
            ordered.put(position, target);
        });
        return Collections.unmodifiableMap(ordered);
    }
}
