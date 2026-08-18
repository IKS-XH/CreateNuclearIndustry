package com.iksxh.create_nuclear_industry.reactor;

import java.util.Map;
import java.util.TreeMap;

/** Settles per-column effective heat load without world or fluid-network access. */
public final class ReactorThermalCalculator {
    private ReactorThermalCalculator() {
    }

    public static ReactorThermalResult settleFissionHeat(
            ReactorSnapshot previous,
            ReactorFissionResult fission,
            Map<CoreColumnPosition, Double> removedHeatByColumn,
            ReactorSimulationParameters parameters
    ) {
        if (previous == null || fission == null || removedHeatByColumn == null || parameters == null) {
            throw new IllegalArgumentException("thermal calculation inputs are required");
        }
        removedHeatByColumn.forEach((position, amount) -> {
            if (position == null || amount == null || !Double.isFinite(amount) || amount < 0.0D) {
                throw new IllegalArgumentException("removed heat entries must be finite and non-negative");
            }
            if (!previous.fuelColumns().containsKey(position)) {
                throw new IllegalArgumentException("removed heat may only target a fuel column");
            }
        });

        Map<CoreColumnPosition, FuelColumnState> nextFuelColumns = new TreeMap<>();
        Map<CoreColumnPosition, FuelColumnThermalResult> results = new TreeMap<>();
        for (Map.Entry<CoreColumnPosition, FuelColumnState> entry : previous.fuelColumns().entrySet()) {
            CoreColumnPosition position = entry.getKey();
            FuelColumnState fuel = entry.getValue();
            FuelColumnFissionResult columnFission = fission.columns().get(position);
            double generated = columnFission == null ? 0.0D : columnFission.generatedHeatHu();
            double available = finiteNonNegative(generated + fuel.cachedHeatHu());
            double removed = Math.min(available, removedHeatByColumn.getOrDefault(position, 0.0D));
            double netHeatLoad = finiteNonNegative(available - removed);
            double damage = fuel.isEffectiveFuel()
                    ? finiteNonNegative(Math.max(0.0D,
                    netHeatLoad - parameters.damageHeatThresholdHuPerTick())
                    * parameters.damageRatePerTickHuLoad())
                    : 0.0D;
            double nextIntegrity = clamp01(fuel.integrity() - damage);
            FuelColumnState nextState = new FuelColumnState(
                    fuel.fuelAssembly(),
                    nextIntegrity,
                    netHeatLoad
            );
            nextFuelColumns.put(position, nextState);
            results.put(position, new FuelColumnThermalResult(
                    generated,
                    removed,
                    netHeatLoad,
                    fuel.integrity() - nextIntegrity,
                    nextState
            ));
        }

        ReactorSnapshot nextSnapshot = new ReactorSnapshot(
                nextFuelColumns,
                previous.controlRodColumns(),
                previous.coldCoolantMb(),
                previous.hotCoolantMb(),
                previous.meltdownProgressTicks(),
                previous.meltdownCountdownStarted()
        );
        return new ReactorThermalResult(nextSnapshot, results);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, finiteNonNegative(value)));
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
    }
}
