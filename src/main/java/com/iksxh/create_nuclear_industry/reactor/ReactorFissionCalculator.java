package com.iksxh.create_nuclear_industry.reactor;

import java.util.Map;
import java.util.TreeMap;

/** Deterministic P1 fuel-column heat, burn, local control and feedback calculation. */
public final class ReactorFissionCalculator {
    private static final int MAX_FEEDBACK_ITERATIONS = 256;
    private static final double FEEDBACK_EPSILON = 1.0E-9D;

    private ReactorFissionCalculator() {
    }

    public static ReactorFissionResult calculate(
            ReactorSnapshot snapshot,
            ReactorSimulationParameters parameters,
            boolean scramActive
    ) {
        if (snapshot == null || parameters == null) {
            throw new IllegalArgumentException("snapshot and parameters are required");
        }

        Map<CoreColumnPosition, Double> controlled = new TreeMap<>();
        Map<CoreColumnPosition, Boolean> overclocked = new TreeMap<>();
        for (Map.Entry<CoreColumnPosition, FuelColumnState> entry : snapshot.fuelColumns().entrySet()) {
            CoreColumnPosition position = entry.getKey();
            FuelColumnState fuel = entry.getValue();
            if (!fuel.isEffectiveFuel()) {
                continue;
            }
            double meanDepth = position.cardinalNeighbours().stream()
                    .map(snapshot.controlRodColumns()::get)
                    .filter(control -> control != null)
                    .mapToDouble(ControlRodColumnState::actualDepth)
                    .average()
                    .orElse(0.0D);
            double controlledIntensity = scramActive
                    ? 0.0D
                    : Math.pow(clamp01(1.0D - meanDepth), parameters.controlResponseExponent());
            controlled.put(position, controlledIntensity);
            overclocked.put(position, position.cardinalNeighbours().stream()
                    .map(snapshot.fuelColumns()::get)
                    .anyMatch(neighbour -> neighbour != null && neighbour.isEffectiveFuel()));
        }

        Map<CoreColumnPosition, Double> heatIntensity = new TreeMap<>();
        Map<CoreColumnPosition, Double> burnIntensity = new TreeMap<>();
        for (CoreColumnPosition position : controlled.keySet()) {
            heatIntensity.put(position, 1.0D);
            burnIntensity.put(position, 1.0D);
        }

        for (int iteration = 0; iteration < MAX_FEEDBACK_ITERATIONS; iteration++) {
            Map<CoreColumnPosition, Double> currentHeatIntensity = heatIntensity;
            Map<CoreColumnPosition, Double> nextHeat = new TreeMap<>();
            Map<CoreColumnPosition, Double> nextBurn = new TreeMap<>();
            double largestDelta = 0.0D;
            for (CoreColumnPosition position : controlled.keySet()) {
                if (!overclocked.get(position)) {
                    nextHeat.put(position, controlled.get(position));
                    nextBurn.put(position, controlled.get(position));
                    continue;
                }
                double signal = position.cardinalNeighbours().stream()
                        .filter(controlled::containsKey)
                        .mapToDouble(neighbour -> currentHeatIntensity.getOrDefault(neighbour, 1.0D))
                        .sum();
                double activation = 1.0D - Math.exp(-parameters.overclockFeedbackGain()
                        * Math.pow(Math.max(0.0D, signal), parameters.overclockFeedbackExponent()));
                nextHeat.put(position, 1.0D + (parameters.overclockHeatMultiplier() - 1.0D)
                        * clamp01(activation));
                nextBurn.put(position, 1.0D + (parameters.overclockBurnMultiplier() - 1.0D)
                        * clamp01(activation));
            }
            for (CoreColumnPosition position : controlled.keySet()) {
                largestDelta = Math.max(largestDelta,
                        Math.abs(nextHeat.get(position) - heatIntensity.get(position)));
                largestDelta = Math.max(largestDelta,
                        Math.abs(nextBurn.get(position) - burnIntensity.get(position)));
            }
            heatIntensity = nextHeat;
            burnIntensity = nextBurn;
            if (largestDelta <= FEEDBACK_EPSILON) {
                break;
            }
        }

        Map<CoreColumnPosition, FuelColumnFissionResult> rawResults = new TreeMap<>();
        double rawHeat = 0.0D;
        double totalBurn = 0.0D;
        int installedFuelColumns = 0;
        for (Map.Entry<CoreColumnPosition, FuelColumnState> entry : snapshot.fuelColumns().entrySet()) {
            CoreColumnPosition position = entry.getKey();
            FuelColumnState fuel = entry.getValue();
            if (fuel.hasUsableFuel()) {
                installedFuelColumns++;
            }
            if (!fuel.isEffectiveFuel()) {
                rawResults.put(position, FuelColumnFissionResult.inactive());
                continue;
            }
            double damageMultiplier = 2.0D - fuel.integrity();
            double columnHeat = finiteNonNegative(parameters.baseHeatPerFuelBlockHuPerTick()
                    * ReactorSnapshot.INTERNAL_HEIGHT * heatIntensity.get(position) * damageMultiplier);
            double columnBurn = finiteNonNegative(parameters.baseBurnPerFuelBlockPerTick()
                    * ReactorSnapshot.INTERNAL_HEIGHT * burnIntensity.get(position) * damageMultiplier);
            rawResults.put(position, new FuelColumnFissionResult(
                    controlled.get(position),
                    heatIntensity.get(position),
                    burnIntensity.get(position),
                    damageMultiplier,
                    columnHeat,
                    columnBurn,
                    overclocked.get(position)
            ));
            rawHeat += columnHeat;
            totalBurn += columnBurn;
        }

        double heatCap = parameters.baseHeatPerFuelBlockHuPerTick()
                * ReactorSnapshot.INTERNAL_HEIGHT
                * installedFuelColumns
                * parameters.totalHeatMultiplierCap();
        double heatScale = rawHeat <= 0.0D ? 0.0D : Math.min(1.0D, heatCap / rawHeat);
        Map<CoreColumnPosition, FuelColumnFissionResult> settled = new TreeMap<>();
        double generatedHeat = 0.0D;
        for (Map.Entry<CoreColumnPosition, FuelColumnFissionResult> entry : rawResults.entrySet()) {
            FuelColumnFissionResult raw = entry.getValue();
            double settledHeat = finiteNonNegative(raw.generatedHeatHu() * heatScale);
            settled.put(entry.getKey(), new FuelColumnFissionResult(
                    raw.controlledIntensity(),
                    raw.heatIntensity(),
                    raw.burnIntensity(),
                    raw.damageMultiplier(),
                    settledHeat,
                    raw.plannedFuelBurnUnits(),
                    raw.overclocked()
            ));
            generatedHeat += settledHeat;
        }
        return new ReactorFissionResult(settled, rawHeat, generatedHeat, totalBurn);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, finiteNonNegative(value)));
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
    }
}
