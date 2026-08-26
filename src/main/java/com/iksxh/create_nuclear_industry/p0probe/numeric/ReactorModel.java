package com.iksxh.create_nuclear_industry.p0probe.numeric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 确定性的、与世界无关的 P0 单 tick 反应堆原型。
 * 所有读取来自上一快照，所有写入都生成新快照；本类仅保留历史数值回归依据，
 * 不得被当作正式 P1 服务端状态源。
 */
public final class ReactorModel {
    private static final int MAX_FEEDBACK_ITERATIONS = 256;
    private static final double FEEDBACK_EPSILON = 1.0e-9;

    private ReactorModel() {
    }

    /** 按 P0 固定顺序结算一次纯数值 tick，输入快照不被修改。 */
    public static ReactorTickResult tick(ReactorSnapshot previous, ReactorParameters parameters,
                                         ReactorTickInput input) {
        Map<ColumnKey, ColumnState> columns = previous.columns();
        Map<ColumnKey, Boolean> effectiveFuel = new TreeMap<>();
        for (ColumnState column : columns.values()) {
            effectiveFuel.put(column.key(), column.effectiveFuel());
        }

        Map<ColumnKey, Double> controlledIntensity = new TreeMap<>();
        Map<ColumnKey, Double> heatEquivalent = new TreeMap<>();
        Map<ColumnKey, Double> burnEquivalent = new TreeMap<>();
        Map<ColumnKey, Boolean> overclocked = new TreeMap<>();

        for (ColumnState column : columns.values()) {
            if (!column.effectiveFuel()) {
                continue;
            }
            List<ColumnState> adjacentControls = adjacent(columns, column.key(), ColumnKind.CONTROL_ROD);
            double meanDepth = adjacentControls.stream()
                    .mapToDouble(control -> control.effectiveControlDepth(parameters))
                    .average()
                    .orElse(0.0);
            double controlled = input.scram() ? 0.0
                    : Math.pow(clamp01(1.0 - meanDepth), parameters.controlResponseExponent());
            controlledIntensity.put(column.key(), controlled);

            boolean hasFuelNeighbour = adjacent(columns, column.key(), ColumnKind.FUEL).stream()
                    .anyMatch(neighbour -> Boolean.TRUE.equals(effectiveFuel.get(neighbour.key())));
            overclocked.put(column.key(), hasFuelNeighbour);
        }

        Map<ColumnKey, Double> heatIntensity = new TreeMap<>();
        Map<ColumnKey, Double> burnIntensity = new TreeMap<>();
        for (ColumnKey key : controlledIntensity.keySet()) {
            heatIntensity.put(key, 1.0);
            burnIntensity.put(key, 1.0);
        }
        for (int iteration = 0; iteration < MAX_FEEDBACK_ITERATIONS; iteration++) {
            double largestDelta = 0;
            Map<ColumnKey, Double> nextHeat = new TreeMap<>(heatIntensity);
            Map<ColumnKey, Double> nextBurn = new TreeMap<>(burnIntensity);
            for (ColumnState column : columns.values()) {
                if (!column.effectiveFuel()) {
                    continue;
                }
                if (!Boolean.TRUE.equals(overclocked.get(column.key()))) {
                    nextHeat.put(column.key(), controlledIntensity.get(column.key()));
                    nextBurn.put(column.key(), controlledIntensity.get(column.key()));
                    continue;
                }
                double signal = 0;
                for (ColumnState neighbour : adjacent(columns, column.key(), ColumnKind.FUEL)) {
                    if (Boolean.TRUE.equals(effectiveFuel.get(neighbour.key()))) {
                        signal += heatIntensity.getOrDefault(neighbour.key(), 1.0);
                    }
                }
                double activation = 1.0 - Math.exp(-parameters.overclockFeedbackGain()
                        * Math.pow(Math.max(0, signal), parameters.overclockFeedbackExponent()));
                nextHeat.put(column.key(), 1.0
                        + (parameters.overclockHeatMultiplier() - 1.0) * clamp01(activation));
                nextBurn.put(column.key(), 1.0
                        + (parameters.overclockBurnMultiplier() - 1.0) * clamp01(activation));
            }
            for (ColumnKey key : heatIntensity.keySet()) {
                largestDelta = Math.max(largestDelta, Math.abs(nextHeat.get(key) - heatIntensity.get(key)));
                largestDelta = Math.max(largestDelta, Math.abs(nextBurn.get(key) - burnIntensity.get(key)));
            }
            heatIntensity = nextHeat;
            burnIntensity = nextBurn;
            if (largestDelta <= FEEDBACK_EPSILON) {
                break;
            }
        }
        for (ColumnState column : columns.values()) {
            if (column.effectiveFuel()) {
                heatEquivalent.put(column.key(), heatIntensity.get(column.key()));
                burnEquivalent.put(column.key(), burnIntensity.get(column.key()));
            }
        }

        Map<ColumnKey, Double> rawColumnHeat = new TreeMap<>();
        Map<ColumnKey, Double> generatedColumnHeat = new TreeMap<>();
        Map<ColumnKey, Double> plannedColumnBurn = new TreeMap<>();
        double rawHeat = 0;
        double plannedBurn = 0;
        int installedFuelColumns = 0;
        for (ColumnState column : columns.values()) {
            if (column.kind() == ColumnKind.FUEL && column.fuelRemaining() > 0) {
                installedFuelColumns++;
            }
            if (!column.effectiveFuel()) {
                rawColumnHeat.put(column.key(), 0.0);
                plannedColumnBurn.put(column.key(), 0.0);
                continue;
            }
            double damageMultiplier = 2.0 - column.fuelColumnIntegrity();
            double columnHeat = parameters.baseHeatPerFuel() * input.internalHeight()
                    * heatEquivalent.get(column.key()) * damageMultiplier;
            double columnBurn = parameters.baseBurnPerFuel() * input.internalHeight()
                    * burnEquivalent.get(column.key()) * damageMultiplier;
            rawColumnHeat.put(column.key(), finiteNonNegative(columnHeat));
            plannedColumnBurn.put(column.key(), finiteNonNegative(columnBurn));
            rawHeat += columnHeat;
            plannedBurn += columnBurn;
        }
        double heatCap = parameters.baseHeatPerFuel() * input.internalHeight() * installedFuelColumns
                * parameters.totalHeatMultiplierCap();
        double heatScale = rawHeat <= 0 ? 0 : Math.min(1.0, heatCap / rawHeat);
        double generatedHeat = 0;
        for (Map.Entry<ColumnKey, Double> entry : rawColumnHeat.entrySet()) {
            double generated = finiteNonNegative(entry.getValue() * heatScale);
            generatedColumnHeat.put(entry.getKey(), generated);
            generatedHeat += generated;
        }

        Map<ColumnKey, Double> currentThermal = new TreeMap<>();
        double availableHeat = 0;
        for (ColumnState column : columns.values()) {
            double thermal = finiteNonNegative(generatedColumnHeat.getOrDefault(column.key(), 0.0)
                    + column.cachedHeat());
            currentThermal.put(column.key(), thermal);
            availableHeat += thermal;
        }

        PortSummary ports = summarizePorts(input.coolantPorts(), parameters);
        double maxConverted = Math.min(Math.min(ports.coldAvailableMb, ports.hotOutputCapacityMb),
                Math.min(ports.coldFlowCapMb, ports.hotFlowCapMb));
        maxConverted = Math.min(maxConverted, availableHeat / parameters.coolantAbsorptionHuPerMb());
        maxConverted = finiteNonNegative(maxConverted);
        double maxRemovalHeat = maxConverted * parameters.coolantAbsorptionHuPerMb();

        Map<ColumnKey, Double> currentRemoval = allocateWithCaps(currentThermal, maxRemovalHeat,
                input.localCoolingCapacityHu());
        double currentRemovedHeat = sum(currentRemoval);

        Map<ColumnKey, Double> sourceResidual = new TreeMap<>();
        Map<ColumnKey, Double> propagationHeat = new TreeMap<>();
        Map<ColumnKey, Double> sourceTransfer = new TreeMap<>();
        for (ColumnState source : columns.values()) {
            if (source.kind() != ColumnKind.FUEL || source.fuelColumnIntegrity() > 0 || source.cachedHeat() <= 0) {
                continue;
            }
            double residual = Math.max(0, source.cachedHeat() - currentRemoval.getOrDefault(source.key(), 0.0));
            sourceResidual.put(source.key(), residual);
            List<ColumnState> targets = adjacent(columns, source.key(), ColumnKind.FUEL, ColumnKind.CONTROL_ROD).stream()
                    .filter(target -> target.kind() == ColumnKind.CONTROL_ROD || target.effectiveFuel())
                    .toList();
            if (targets.isEmpty()) {
                continue;
            }
            double transfer = residual * parameters.fuelColumnDamageTransferRate();
            sourceTransfer.put(source.key(), transfer);
            double perTarget = transfer / targets.size();
            for (ColumnState target : targets) {
                propagationHeat.merge(target.key(), perTarget, Double::sum);
            }
        }

        double remainingCoolingBudget = Math.max(0, maxRemovalHeat - currentRemovedHeat);
        Map<ColumnKey, Double> propagationRemovalCaps = new TreeMap<>();
        for (Map.Entry<ColumnKey, Double> entry : input.localCoolingCapacityHu().entrySet()) {
            propagationRemovalCaps.put(entry.getKey(), Math.max(0,
                    entry.getValue() - currentRemoval.getOrDefault(entry.getKey(), 0.0)));
        }
        Map<ColumnKey, Double> propagationRemoval = allocateWithCaps(propagationHeat, remainingCoolingBudget,
                propagationRemovalCaps);
        double propagationRemovedHeat = sum(propagationRemoval);
        double removedHeat = currentRemovedHeat + propagationRemovedHeat;
        double convertedCoolant = removedHeat / parameters.coolantAbsorptionHuPerMb();

        Map<ColumnKey, ColumnState> nextColumns = new TreeMap<>();
        Map<ColumnKey, ColumnTickResult> columnResults = new LinkedHashMap<>();
        Set<ColumnKey> coveredFuelColumns = new LinkedHashSet<>();
        for (ColumnState column : columns.values()) {
            double received = propagationHeat.getOrDefault(column.key(), 0.0);
            double receivedRemoval = propagationRemoval.getOrDefault(column.key(), 0.0);
            if (column.kind() == ColumnKind.FUEL && received > receivedRemoval && column.effectiveFuel()) {
                coveredFuelColumns.add(column.key());
            }
            double heat = generatedColumnHeat.getOrDefault(column.key(), 0.0);
            double currentCache = column.cachedHeat();
            double totalUnremoved = Math.max(0, heat + currentCache + received
                    - currentRemoval.getOrDefault(column.key(), 0.0) - receivedRemoval);
            double damage = 0;
            ColumnState next;
            if (column.kind() == ColumnKind.FUEL) {
                if (column.effectiveFuel()) {
                    damage = Math.max(0, totalUnremoved - parameters.fuelColumnDamageHeatThreshold())
                            * parameters.fuelColumnDamageRate();
                }
                double nextIntegrity = clamp01(column.fuelColumnIntegrity() - damage);
                double nextFuel = Math.max(0, column.fuelRemaining() - plannedColumnBurn.getOrDefault(column.key(), 0.0));
                double nextCache = totalUnremoved;
                if (column.fuelColumnIntegrity() <= 0 && sourceTransfer.containsKey(column.key())) {
                    nextCache = Math.max(0, nextCache - sourceTransfer.get(column.key()));
                }
                next = column.withFuel(nextFuel, nextIntegrity, nextCache);
            } else if (column.kind() == ColumnKind.CONTROL_ROD) {
                damage = Math.max(0, totalUnremoved - parameters.fuelColumnDamageHeatThreshold())
                        * parameters.fuelColumnDamageRate();
                double nextIntegrity = clamp01(column.controlRodColumnIntegrity() - damage);
                boolean jammed = column.jammed() || nextIntegrity <= parameters.controlRodColumnFailureThreshold();
                double effectiveDepth = column.effectiveControlDepth(parameters);
                double nextJammedDepth = jammed ? (column.jammed() ? column.jammedDepth() : effectiveDepth)
                        : column.jammedDepth();
                double nextDepth = jammed ? effectiveDepth : clamp01(column.controlRodDepth());
                next = column.withControl(nextIntegrity, nextDepth, totalUnremoved, jammed, nextJammedDepth);
            } else {
                next = column;
            }
            nextColumns.put(column.key(), next);
            columnResults.put(column.key(), new ColumnTickResult(column.key(), heat,
                    plannedColumnBurn.getOrDefault(column.key(), 0.0),
                    currentRemoval.getOrDefault(column.key(), 0.0) + receivedRemoval,
                    totalUnremoved, received, receivedRemoval, damage, next));
        }

        int denominator = (int) effectiveFuel.values().stream().filter(Boolean::booleanValue).count();
        double coverage = denominator == 0 ? 0 : (double) coveredFuelColumns.size() / denominator;
        boolean meltdownDanger = coverage >= parameters.meltdownTriggerFraction() && !coveredFuelColumns.isEmpty();
        double nextProgress = previous.meltdownProgress();
        boolean meltdownTriggered = previous.meltdownTriggered();
        if (meltdownDanger) {
            meltdownTriggered = true;
            if (!input.scram()) {
                nextProgress = Math.min(parameters.meltdownCountdownTicks(), nextProgress + 1);
            }
        }
        ReactorSnapshot next = new ReactorSnapshot(nextColumns, nextProgress, meltdownTriggered, previous.tick() + 1);
        if (next.allColumnIntegrityIsFull() && previous.meltdownTriggered()) {
            next = next.resetMeltdownIfAllRepaired();
        }
        return new ReactorTickResult(next, Map.copyOf(columnResults), rawHeat, generatedHeat, plannedBurn,
                convertedCoolant, removedHeat, coverage, meltdownDanger, ports.duplicatePortCount);
    }

    /** 在列停止且有修复材料时按 P0 规则修复一根燃料或可动控制棒列。 */
    public static ColumnState repair(ColumnState column, double internalHeight, boolean columnStopped,
                                     int availablePlates) {
        if (column == null || !columnStopped || internalHeight <= 0 || !Double.isFinite(internalHeight)
                || availablePlates <= 0) {
            return column;
        }
        if (column.kind() == ColumnKind.FUEL) {
            double repaired = clamp01(column.fuelColumnIntegrity() + 0.25 / internalHeight);
            return column.withFuel(column.fuelRemaining(), repaired, column.cachedHeat());
        }
        if (column.kind() == ColumnKind.CONTROL_ROD && !column.jammed()) {
            double repaired = clamp01(column.controlRodColumnIntegrity() + 0.25 / internalHeight);
            return column.withControl(repaired, column.controlRodDepth(), column.cachedHeat(), false,
                    column.jammedDepth());
        }
        return column;
    }

    private static List<ColumnState> adjacent(Map<ColumnKey, ColumnState> columns, ColumnKey key,
                                               ColumnKind... kinds) {
        List<ColumnState> result = new ArrayList<>();
        Set<ColumnKind> accepted = Set.of(kinds);
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] direction : directions) {
            ColumnState neighbour = columns.get(new ColumnKey(key.x() + direction[0], key.z() + direction[1]));
            if (neighbour != null && accepted.contains(neighbour.kind())) {
                result.add(neighbour);
            }
        }
        return result;
    }

    private static Map<ColumnKey, Double> allocateWithCaps(Map<ColumnKey, Double> demand, double budget,
                                                            Map<ColumnKey, Double> explicitCaps) {
        Map<ColumnKey, Double> allocation = new TreeMap<>();
        Map<ColumnKey, Double> remainingDemand = new TreeMap<>();
        Map<ColumnKey, Double> remainingCaps = new TreeMap<>();
        for (Map.Entry<ColumnKey, Double> entry : demand.entrySet()) {
            double amount = finiteNonNegative(entry.getValue());
            if (amount <= 0) {
                continue;
            }
            remainingDemand.put(entry.getKey(), amount);
            remainingCaps.put(entry.getKey(), Math.max(0,
                    explicitCaps.getOrDefault(entry.getKey(), budget)));
            allocation.put(entry.getKey(), 0.0);
        }
        double remainingBudget = Math.max(0, budget);
        while (remainingBudget > 1.0e-12 && !remainingDemand.isEmpty()) {
            double totalDemand = sum(remainingDemand);
            if (totalDemand <= 0) {
                break;
            }
            List<ColumnKey> capped = new ArrayList<>();
            for (ColumnKey key : remainingDemand.keySet()) {
                double proportional = remainingBudget * remainingDemand.get(key) / totalDemand;
                if (proportional > remainingCaps.get(key) + 1.0e-12) {
                    double amount = remainingCaps.get(key);
                    allocation.merge(key, amount, Double::sum);
                    remainingBudget -= amount;
                    capped.add(key);
                }
            }
            if (!capped.isEmpty()) {
                capped.forEach(key -> {
                    remainingDemand.remove(key);
                    remainingCaps.remove(key);
                });
                continue;
            }
            for (ColumnKey key : new ArrayList<>(remainingDemand.keySet())) {
                double proportional = remainingBudget * remainingDemand.get(key) / totalDemand;
                double amount = Math.min(proportional, remainingCaps.get(key));
                allocation.merge(key, amount, Double::sum);
            }
            break;
        }
        allocation.replaceAll((key, value) -> finiteNonNegative(value));
        return allocation;
    }

    private static PortSummary summarizePorts(List<CoolantPort> ports, ReactorParameters parameters) {
        Map<String, CoolantPort> unique = new TreeMap<>();
        int duplicates = 0;
        for (CoolantPort port : ports) {
            if (unique.putIfAbsent(port.connectionId(), port) != null) {
                duplicates++;
            }
        }
        double coldAvailable = 0;
        double hotCapacity = 0;
        double coldFlowCap = 0;
        double hotFlowCap = 0;
        for (CoolantPort port : unique.values()) {
            double capped = Math.min(port.availableMb(), parameters.coolantMaxFlowPerPort());
            if (port.kind() == CoolantPort.Kind.COLD_INPUT) {
                coldAvailable += port.availableMb();
                coldFlowCap += capped;
            } else {
                hotCapacity += port.availableMb();
                hotFlowCap += capped;
            }
        }
        return new PortSummary(coldAvailable, hotCapacity, coldFlowCap, hotFlowCap, duplicates);
    }

    private static double sum(Map<ColumnKey, Double> values) {
        return values.values().stream().mapToDouble(ReactorModel::finiteNonNegative).sum();
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, finiteNonNegative(value)));
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) && value > 0 ? value : 0;
    }

    private record PortSummary(double coldAvailableMb, double hotOutputCapacityMb,
                               double coldFlowCapMb, double hotFlowCapMb, int duplicatePortCount) {
    }
}
