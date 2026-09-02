package com.iksxh.create_nuclear_industry.reactor;

import java.util.Map;
import java.util.TreeMap;

/** 不访问世界或流体网络，结算逐列有效热负荷。 */
public final class ReactorThermalCalculator {
    private ReactorThermalCalculator() {
    }

    public static ReactorThermalResult settleFissionHeat(
            ReactorSnapshot previous,
            ReactorFissionResult fission,
            Map<CoreColumnPosition, Double> removedHeatByColumn,
            ReactorSimulationParameters parameters
    ) {
        return settleFissionHeat(previous, fission, removedHeatByColumn, Map.of(), parameters);
    }

    /**
     * 结算逐列热负荷，并单独标记由整数 mB 量化产生的安全余热。
     *
     * <p>{@code quantizedHeatRemainderByColumn} 只能抵消量化余数，不能抵消真实冷却
     * 短缺；因此损伤、传播和融毁判断都基于扣除该标记后的有效热负荷。热量总账仍保存在
     * {@link FuelColumnState#cachedHeatHu()} 中，标记由同一个反应堆快照持有。</p>
     */
    public static ReactorThermalResult settleFissionHeat(
            ReactorSnapshot previous,
            ReactorFissionResult fission,
            Map<CoreColumnPosition, Double> removedHeatByColumn,
            Map<CoreColumnPosition, Double> quantizedHeatRemainderByColumn,
            ReactorSimulationParameters parameters
    ) {
        if (previous == null || fission == null || removedHeatByColumn == null || parameters == null) {
            throw new IllegalArgumentException("thermal calculation inputs are required");
        }
        validateFuelColumnHeatMap(previous, removedHeatByColumn,
                "removed heat entries must be finite and non-negative",
                "removed heat may only target a fuel column");
        if (quantizedHeatRemainderByColumn == null) {
            throw new IllegalArgumentException("quantized heat remainder map is required");
        }
        validateFuelColumnHeatMap(previous, quantizedHeatRemainderByColumn,
                "quantized heat remainder entries must be finite and non-negative",
                "quantized heat remainder may only target a fuel column");

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
            double quantizedHeatRemainder = Math.min(netHeatLoad,
                    quantizedHeatRemainderByColumn.getOrDefault(position, 0.0D));
            double effectiveHeatLoad = finiteNonNegative(netHeatLoad - quantizedHeatRemainder);
            double damage = fuel.isEffectiveFuel()
                    ? finiteNonNegative(Math.max(0.0D,
                    effectiveHeatLoad - parameters.damageHeatThresholdHuPerTick())
                    * parameters.damageRatePerTickHuLoad())
                    : 0.0D;
            double nextIntegrity = clamp01(fuel.integrity() - damage);
            FuelColumnState nextState = new FuelColumnState(
                    fuel.fuelAssembly(),
                    nextIntegrity,
                    netHeatLoad,
                    fuel.fuelBurnRemainder(),
                    quantizedHeatRemainder
            );
            nextFuelColumns.put(position, nextState);
            results.put(position, new FuelColumnThermalResult(
                    generated,
                    removed,
                    netHeatLoad,
                    quantizedHeatRemainder,
                    fuel.integrity() - nextIntegrity,
                    nextState
            ));
        }

        ReactorSnapshot nextSnapshot = previous.withColumns(nextFuelColumns, previous.controlRodColumns());
        return new ReactorThermalResult(nextSnapshot, results);
    }

    private static void validateFuelColumnHeatMap(
            ReactorSnapshot previous,
            Map<CoreColumnPosition, Double> values,
            String valueMessage,
            String positionMessage
    ) {
        values.forEach((position, amount) -> {
            if (position == null || amount == null || !Double.isFinite(amount) || amount < 0.0D) {
                throw new IllegalArgumentException(valueMessage);
            }
            if (!previous.fuelColumns().containsKey(position)) {
                throw new IllegalArgumentException(positionMessage);
            }
        });
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, finiteNonNegative(value)));
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
    }
}
