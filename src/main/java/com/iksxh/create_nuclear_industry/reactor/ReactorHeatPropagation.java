package com.iksxh.create_nuclear_industry.reactor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** 基于快照从失效燃料列进行四向热传播；整数 mB 量化余数不属于传播热源。 */
public final class ReactorHeatPropagation {
    private ReactorHeatPropagation() {
    }

    public static HeatPropagationResult propagate(
            ReactorSnapshot previous,
            Map<CoreColumnPosition, Double> localCoolingCapacityHu,
            ReactorSimulationParameters parameters
    ) {
        if (previous == null || localCoolingCapacityHu == null || parameters == null) {
            throw new IllegalArgumentException("propagation inputs are required");
        }
        localCoolingCapacityHu.forEach((position, amount) -> {
            if (position == null || amount == null || !Double.isFinite(amount) || amount < 0.0D) {
                throw new IllegalArgumentException("local cooling entries must be finite and non-negative");
            }
            if (!containsColumn(previous, position)) {
                throw new IllegalArgumentException("local cooling may only target an existing column");
            }
        });

        Map<CoreColumnPosition, Double> remainingCooling = new TreeMap<>(localCoolingCapacityHu);
        Map<CoreColumnPosition, Double> sourceBaseHeat = new TreeMap<>();
        Map<CoreColumnPosition, Double> sourceBaseQuantizedHeat = new TreeMap<>();
        Map<CoreColumnPosition, Double> received = new TreeMap<>();
        Map<CoreColumnPosition, Double> removed = new TreeMap<>();
        Set<CoreColumnPosition> coveredFuel = new TreeSet<>();
        double totalTransferred = 0.0D;

        for (Map.Entry<CoreColumnPosition, FuelColumnState> entry : previous.fuelColumns().entrySet()) {
            CoreColumnPosition sourcePosition = entry.getKey();
            FuelColumnState source = entry.getValue();
            // 完整度归零但组件尚未耗尽的列本身就是融毁覆盖源，即使没有余热可继续传播。
            if (source.hasUsableFuel() && source.integrity() == 0.0D) {
                coveredFuel.add(sourcePosition);
            }
            double safeRemainder = Math.min(source.cachedHeatHu(), source.quantizedHeatRemainderHu());
            double propagatableHeat = Math.max(0.0D, source.cachedHeatHu() - safeRemainder);
            if (!source.hasUsableFuel() || source.integrity() > 0.0D || propagatableHeat <= 0.0D) {
                continue;
            }
            double sourceCooling = Math.min(
                    propagatableHeat,
                    remainingCooling.getOrDefault(sourcePosition, 0.0D)
            );
            removed.merge(sourcePosition, sourceCooling, Double::sum);
            remainingCooling.put(sourcePosition,
                    Math.max(0.0D, remainingCooling.getOrDefault(sourcePosition, 0.0D) - sourceCooling));
            double residual = propagatableHeat - sourceCooling;
            List<CoreColumnPosition> targets = adjacentColumns(previous, sourcePosition);
            double transfer = targets.isEmpty() ? 0.0D : residual * parameters.damageTransferRate();
            sourceBaseHeat.put(sourcePosition, safeRemainder + residual - transfer);
            sourceBaseQuantizedHeat.put(sourcePosition, safeRemainder);
            totalTransferred += transfer;
            if (transfer <= 0.0D) {
                continue;
            }
            double perTarget = transfer / targets.size();
            targets.forEach(target -> received.merge(target, perTarget, Double::sum));
        }

        Map<CoreColumnPosition, Double> netReceived = new TreeMap<>();
        for (Map.Entry<CoreColumnPosition, Double> entry : received.entrySet()) {
            CoreColumnPosition target = entry.getKey();
            double targetRemoval = Math.min(entry.getValue(), remainingCooling.getOrDefault(target, 0.0D));
            removed.merge(target, targetRemoval, Double::sum);
            netReceived.put(target, entry.getValue() - targetRemoval);
        }

        Map<CoreColumnPosition, FuelColumnState> nextFuel = new TreeMap<>();
        for (Map.Entry<CoreColumnPosition, FuelColumnState> entry : previous.fuelColumns().entrySet()) {
            CoreColumnPosition position = entry.getKey();
            FuelColumnState fuel = entry.getValue();
            double incoming = netReceived.getOrDefault(position, 0.0D);
            // 传播覆盖只统计仍有耐久的组件；完整度为零的列已在源集合中记录，集合会自动去重。
            if (fuel.hasUsableFuel() && incoming > 0.0D) {
                coveredFuel.add(position);
            }
            double damage = fuel.integrity() > 0.0D
                    ? Math.max(0.0D, incoming - parameters.damageHeatThresholdHuPerTick())
                    * parameters.damageRatePerTickHuLoad()
                    : 0.0D;
            double nextIntegrity = clamp01(fuel.integrity() - damage);
            double baseHeat = sourceBaseHeat.getOrDefault(position, fuel.cachedHeatHu());
            double baseQuantizedHeat = Math.min(baseHeat,
                    sourceBaseQuantizedHeat.getOrDefault(position,
                            fuel.quantizedHeatRemainderHu()));
            nextFuel.put(position, new FuelColumnState(
                    fuel.fuelAssembly(),
                    nextIntegrity,
                    finiteNonNegative(baseHeat + incoming),
                    fuel.fuelBurnRemainder(),
                    baseQuantizedHeat
            ));
        }

        Map<CoreColumnPosition, ControlRodColumnState> nextControls = new TreeMap<>();
        for (Map.Entry<CoreColumnPosition, ControlRodColumnState> entry : previous.controlRodColumns().entrySet()) {
            CoreColumnPosition position = entry.getKey();
            ControlRodColumnState control = entry.getValue();
            double incoming = netReceived.getOrDefault(position, 0.0D);
            double damage = Math.max(0.0D, incoming - parameters.damageHeatThresholdHuPerTick())
                    * parameters.damageRatePerTickHuLoad();
            ControlRodColumnState damaged = ControlRodStateTransitions.applyIntegrityDamage(
                    control,
                    damage,
                    parameters.controlRodFailureThreshold()
            );
            nextControls.put(position, new ControlRodColumnState(
                    damaged.integrity(),
                    damaged.targetDepth(),
                    damaged.actualDepth(),
                    damaged.jammed(),
                    finiteNonNegative(control.cachedHeatHu() + incoming)
            ));
        }

        ReactorSnapshot next = previous.withColumns(nextFuel, nextControls);
        return new HeatPropagationResult(
                next,
                received,
                removed,
                netReceived,
                coveredFuel,
                totalTransferred
        );
    }

    private static List<CoreColumnPosition> adjacentColumns(
            ReactorSnapshot snapshot,
            CoreColumnPosition source
    ) {
        List<CoreColumnPosition> targets = new ArrayList<>(4);
        for (CoreColumnPosition position : source.cardinalNeighbours()) {
            if (containsColumn(snapshot, position)) {
                targets.add(position);
            }
        }
        return targets;
    }

    private static boolean containsColumn(ReactorSnapshot snapshot, CoreColumnPosition position) {
        return snapshot.fuelColumns().containsKey(position)
                || snapshot.controlRodColumns().containsKey(position);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, finiteNonNegative(value)));
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
    }
}
