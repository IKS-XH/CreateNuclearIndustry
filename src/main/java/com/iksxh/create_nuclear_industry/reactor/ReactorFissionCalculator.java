package com.iksxh.create_nuclear_industry.reactor;

import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

/** 确定性计算 P1 燃料列的裂变热、燃耗、局部控制和邻列反馈，热量单位为 HU。 */
public final class ReactorFissionCalculator {
    private static final int MAX_FEEDBACK_ITERATIONS = 256;
    private static final double FEEDBACK_EPSILON = 1.0E-9D;

    // 缓存只含局部坐标和数值，按计算线程隔离；不引用世界、实体、快照，也不写入 NBT。
    private static final ThreadLocal<FeedbackCache> CACHE = ThreadLocal.withInitial(FeedbackCache::new);

    private ReactorFissionCalculator() {
    }

    /** 根据快照和数值参数计算一 tick 的裂变结果，不修改输入快照。 */
    public static ReactorFissionResult calculate(
            ReactorSnapshot snapshot,
            ReactorSimulationParameters parameters
    ) {
        if (snapshot == null || parameters == null) {
            throw new IllegalArgumentException("snapshot and parameters are required");
        }

        Feedback feedback = CACHE.get().feedback(snapshot, parameters);

        Map<CoreColumnPosition, FuelColumnFissionResult> rawResults = new TreeMap<>();
        double rawHeat = 0.0D;
        double totalBurn = 0.0D;
        int installedFuelColumns = 0;
        int columnIndex = 0;
        for (Map.Entry<CoreColumnPosition, FuelColumnState> entry : snapshot.fuelColumns().entrySet()) {
            CoreColumnPosition position = entry.getKey();
            int index = columnIndex++;
            FuelColumnState fuel = entry.getValue();
            if (fuel.hasUsableFuel()) {
                installedFuelColumns++;
            }
            if (!fuel.hasUsableFuel()) {
                rawResults.put(position, FuelColumnFissionResult.inactive());
                continue;
            }
            double damageFraction = 1.0D - clamp01(fuel.integrity());
            double damageHeatMultiplier = 1.0D
                    + (parameters.fuelColumnDamageHeatMultiplier() - 1.0D) * damageFraction;
            double damageBurnMultiplier = 1.0D
                    + (parameters.fuelColumnDamageBurnMultiplier() - 1.0D) * damageFraction;
            double columnHeat = finiteNonNegative(parameters.baseHeatPerFuelBlockHuPerTick()
                    * snapshot.dimensions().internalHeight() * feedback.heat[index] * damageHeatMultiplier);
            double columnBurn = finiteNonNegative(parameters.baseBurnPerFuelBlockPerTick()
                    * snapshot.dimensions().internalHeight() * feedback.burn[index] * damageBurnMultiplier);
            rawResults.put(position, new FuelColumnFissionResult(
                    feedback.controlled[index],
                    feedback.heat[index],
                    feedback.burn[index],
                    damageHeatMultiplier,
                    damageBurnMultiplier,
                    columnHeat,
                    columnBurn,
                    feedback.overclocked[index]
            ));
            rawHeat += columnHeat;
            totalBurn += columnBurn;
        }

        double heatCap = parameters.baseHeatPerFuelBlockHuPerTick()
                * snapshot.dimensions().internalHeight()
                * installedFuelColumns
                * parameters.totalHeatMultiplierCap();
        // 服务端每 tick 只对限幅后的全堆新生热取整一次；诊断原始热和燃耗均保留原公式。
        double limitedHeat = finiteNonNegative(Math.min(rawHeat, heatCap));
        double generatedHeat = Math.ceil(limitedHeat);
        CoreColumnPosition lastPositive = null;
        if (generatedHeat > 0.0D) {
            for (Map.Entry<CoreColumnPosition, FuelColumnFissionResult> entry : rawResults.entrySet()) {
                if (entry.getValue().generatedHeatHu() > 0.0D) {
                    lastPositive = entry.getKey();
                }
            }
        }
        Map<CoreColumnPosition, FuelColumnFissionResult> settled = new TreeMap<>();
        double assignedHeat = 0.0D;
        for (Map.Entry<CoreColumnPosition, FuelColumnFissionResult> entry : rawResults.entrySet()) {
            FuelColumnFissionResult raw = entry.getValue();
            // 坐标顺序固定，最后一个正产热列接收剩余量，避免尾差影响整数 mB 冷却；零热列不分配。
            double settledHeat = 0.0D;
            if (generatedHeat > 0.0D && raw.generatedHeatHu() > 0.0D) {
                double remainingHeat = Math.max(0.0D, generatedHeat - assignedHeat);
                settledHeat = entry.getKey().equals(lastPositive) ? remainingHeat
                        : Math.min(remainingHeat, raw.generatedHeatHu() / rawHeat * generatedHeat);
            }
            settled.put(entry.getKey(), new FuelColumnFissionResult(
                    raw.controlledIntensity(),
                    raw.heatIntensity(),
                    raw.burnIntensity(),
                    raw.damageHeatMultiplier(),
                    raw.damageBurnMultiplier(),
                    settledHeat,
                    raw.plannedFuelBurnUnits(),
                    raw.overclocked()
            ));
            assignedHeat += settledHeat;
        }
        return new ReactorFissionResult(settled, rawHeat, generatedHeat, totalBurn);
    }

    /** 兼容旧调用方；SCRAM 由控制棒实际深度决定，不再使用全局布尔值清零裂变。 */
    public static ReactorFissionResult calculate(
            ReactorSnapshot snapshot,
            ReactorSimulationParameters parameters,
            boolean ignoredScramActive
    ) {
        return calculate(snapshot, parameters);
    }

    /**
     * 有界的纯值缓存：每线程最多八种列布局，每布局最多四组反馈输入。
     * 不以每 tick 都变化的快照身份作为键，管理员预测与真实结算只在实际输入一致时复用。
     */
    private static final class FeedbackCache {
        private final List<Topology> layouts = new ArrayList<>();
        private long hits;
        private long solves;
        private long topologyBuilds;

        private Feedback feedback(ReactorSnapshot snapshot, ReactorSimulationParameters parameters) {
            Topology topology = null;
            for (int i = 0; i < layouts.size(); i++) {
                if (layouts.get(i).matches(snapshot)) {
                    topology = layouts.remove(i);
                    break;
                }
            }
            if (topology == null) {
                topology = new Topology(snapshot);
                topologyBuilds++;
            }
            layouts.addFirst(topology);
            if (layouts.size() > 8) layouts.removeLast();
            for (int i = 0; i < topology.saved.size(); i++) {
                Feedback saved = topology.saved.get(i);
                if (saved.matches(snapshot, parameters, topology)) {
                    topology.saved.remove(i);
                    topology.saved.addFirst(saved);
                    hits++;
                    return saved;
                }
            }
            solves++;
            Feedback result = solve(snapshot, parameters, topology);
            // 原计算器达到上限仍返回最后一轮。保留该行为，但不复用未收敛的结果。
            if (result.converged) {
                topology.saved.addFirst(result);
                if (topology.saved.size() > 4) topology.saved.removeLast();
            }
            return result;
        }
    }

    /** 有序燃料列和棒列的四向邻接索引；列顺序沿用快照，邻接沿用北、南、西、东。 */
    private static final class Topology {
        private final int width;
        private final int depth;
        private final CoreColumnPosition[] fuels;
        private final CoreColumnPosition[] rods;
        private final int[][] fuelNeighbours;
        private final int[][] rodNeighbours;
        private final List<Feedback> saved = new ArrayList<>();

        private Topology(ReactorSnapshot snapshot) {
            width = snapshot.dimensions().internalWidth();
            depth = snapshot.dimensions().internalDepth();
            fuels = snapshot.fuelColumns().keySet().toArray(CoreColumnPosition[]::new);
            rods = snapshot.controlRodColumns().keySet().toArray(CoreColumnPosition[]::new);
            fuelNeighbours = new int[fuels.length][];
            rodNeighbours = new int[fuels.length][];
            for (int i = 0; i < fuels.length; i++) {
                var neighbours = fuels[i].cardinalNeighbours(snapshot.dimensions());
                fuelNeighbours[i] = neighbours.stream().mapToInt(p -> Arrays.binarySearch(fuels, p))
                        .filter(index -> index >= 0).toArray();
                rodNeighbours[i] = neighbours.stream().mapToInt(p -> Arrays.binarySearch(rods, p))
                        .filter(index -> index >= 0).toArray();
            }
        }

        private boolean matches(ReactorSnapshot snapshot) {
            if (width != snapshot.dimensions().internalWidth() || depth != snapshot.dimensions().internalDepth()
                    || fuels.length != snapshot.fuelColumns().size() || rods.length != snapshot.controlRodColumns().size()) {
                return false;
            }
            int i = 0;
            for (var position : snapshot.fuelColumns().keySet()) if (!fuels[i++].equals(position)) return false;
            i = 0;
            for (var position : snapshot.controlRodColumns().keySet()) if (!rods[i++].equals(position)) return false;
            return true;
        }
    }

    /**
     * 缓存只保存参与反馈的可用燃料、实际棒深、五项参数及无单位强度。
     * 完整度、余热、燃耗余量、高度、热上限和库存不影响反馈，仍由每次结算读取当前值。
     */
    private static final class Feedback {
        private final boolean[] usable;
        private final double[] rodDepths;
        private final double[] parameters;
        private final double[] controlled;
        private final boolean[] overclocked;
        private double[] heat;
        private double[] burn;
        private boolean converged;

        private Feedback(ReactorSnapshot snapshot, ReactorSimulationParameters p, Topology topology) {
            usable = new boolean[topology.fuels.length];
            for (int i = 0; i < usable.length; i++) usable[i] = snapshot.fuelColumns().get(topology.fuels[i]).hasUsableFuel();
            rodDepths = new double[topology.rods.length];
            for (int i = 0; i < rodDepths.length; i++) rodDepths[i] = snapshot.controlRodColumns().get(topology.rods[i]).actualDepth();
            parameters = new double[] {p.controlResponseExponent(), p.overclockHeatMultiplier(), p.overclockBurnMultiplier(),
                    p.overclockFeedbackGain(), p.overclockFeedbackExponent()};
            controlled = new double[usable.length];
            overclocked = new boolean[usable.length];
            heat = new double[usable.length];
            burn = new double[usable.length];
        }

        private boolean matches(ReactorSnapshot snapshot, ReactorSimulationParameters p, Topology topology) {
            if (!same(parameters[0], p.controlResponseExponent()) || !same(parameters[1], p.overclockHeatMultiplier())
                    || !same(parameters[2], p.overclockBurnMultiplier()) || !same(parameters[3], p.overclockFeedbackGain())
                    || !same(parameters[4], p.overclockFeedbackExponent())) return false;
            for (int i = 0; i < usable.length; i++) {
                if (usable[i] != snapshot.fuelColumns().get(topology.fuels[i]).hasUsableFuel()) return false;
            }
            for (int i = 0; i < rodDepths.length; i++) {
                if (!same(rodDepths[i], snapshot.controlRodColumns().get(topology.rods[i]).actualDepth())) return false;
            }
            return true;
        }
    }

    /** 从原控制强度同步迭代，不用上次结果热启动；仅将迭代 Map 换成同序的私有数值数组。 */
    private static Feedback solve(ReactorSnapshot snapshot, ReactorSimulationParameters parameters, Topology topology) {
        Feedback result = new Feedback(snapshot, parameters, topology);
        for (int i = 0; i < result.usable.length; i++) {
            if (!result.usable[i]) continue;
            // 保留 DoubleStream 的补偿平均与求和语义，避免普通累加改变收敛或最终向上取整。
            double meanDepth = Arrays.stream(topology.rodNeighbours[i]).mapToDouble(index -> result.rodDepths[index])
                    .average().orElse(0.0D);
            result.controlled[i] = Math.pow(clamp01(1.0D - meanDepth), parameters.controlResponseExponent());
            result.overclocked[i] = Arrays.stream(topology.fuelNeighbours[i]).anyMatch(index -> result.usable[index]);
            result.heat[i] = result.controlled[i];
            result.burn[i] = result.controlled[i];
        }
        double[] nextHeat = new double[result.usable.length];
        double[] nextBurn = new double[result.usable.length];
        for (int iteration = 0; iteration < MAX_FEEDBACK_ITERATIONS; iteration++) {
            double[] currentHeat = result.heat;
            for (int i = 0; i < result.usable.length; i++) {
                if (!result.usable[i]) continue;
                if (!result.overclocked[i]) {
                    nextHeat[i] = result.controlled[i];
                    nextBurn[i] = result.controlled[i];
                    continue;
                }
                double signal = Arrays.stream(topology.fuelNeighbours[i]).filter(index -> result.usable[index])
                        .mapToDouble(index -> currentHeat[index]).sum();
                double activation = 1.0D - Math.exp(-parameters.overclockFeedbackGain()
                        * Math.pow(Math.max(0.0D, signal), parameters.overclockFeedbackExponent()));
                double controlFactor = result.controlled[i];
                nextHeat[i] = controlFactor * (1.0D + (parameters.overclockHeatMultiplier() - 1.0D) * clamp01(activation));
                nextBurn[i] = controlFactor * (1.0D + (parameters.overclockBurnMultiplier() - 1.0D) * clamp01(activation));
            }
            double largestDelta = 0.0D;
            for (int i = 0; i < result.usable.length; i++) {
                if (!result.usable[i]) continue;
                largestDelta = Math.max(largestDelta, Math.abs(nextHeat[i] - result.heat[i]));
                largestDelta = Math.max(largestDelta, Math.abs(nextBurn[i] - result.burn[i]));
            }
            double[] previousHeat = result.heat;
            double[] previousBurn = result.burn;
            result.heat = nextHeat;
            result.burn = nextBurn;
            nextHeat = previousHeat;
            nextBurn = previousBurn;
            if (largestDelta <= FEEDBACK_EPSILON) {
                result.converged = true;
                break;
            }
        }
        return result;
    }

    private static boolean same(double first, double second) {
        return Double.doubleToLongBits(first) == Double.doubleToLongBits(second);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, finiteNonNegative(value)));
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
    }
}
