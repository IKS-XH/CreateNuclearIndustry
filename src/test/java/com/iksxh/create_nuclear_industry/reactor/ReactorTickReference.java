package com.iksxh.create_nuclear_industry.reactor;

import java.util.Map;
import java.util.TreeMap;

/**
 * 已成形 P1 反应堆的单 tick 权威编排器。
 *
 * <p>各计算器保持纯函数和确定性；本类统一定义生产顺序：</p>
 *
 * <pre>
 * 控制状态 -> 裂变热量/燃耗 -> 维护付热 -> 冷却分配 -> 有效热负荷/损伤
 * -> 四向传播 -> 熔毁状态 -> 燃耗持久化
 * </pre>
 */
public final class ReactorTickReference {
    private static final double HEAT_EPSILON = 1.0E-12D;

    private ReactorTickReference() {
    }

    /** 按固定 P1 阶段顺序结算一个服务端 tick。 */
    public static ReactorServerTick.Result advance(
            ReactorSnapshot previous,
            ReactorSimulationParameters parameters,
            ReactorServerTick.CoolantInput coolantInput
    ) {
        return advance(previous, parameters, coolantInput, 0);
    }

    /** 服务端一次真实核热事务；维护费单位HU，原三参入口等价于无管理员的零请求。 */
    public static ReactorServerTick.Result advance(ReactorSnapshot previous, ReactorSimulationParameters parameters,
                                 ReactorServerTick.CoolantInput coolantInput, double maintenanceHeatHu) {
        if (previous == null || parameters == null || coolantInput == null) {
            throw new IllegalArgumentException("reactor tick inputs are required");
        }

        // 1. 读取并应用权威控制状态；卡死棒由 ReactorControlRodTick 原样保留。
        ReactorSnapshot controlSnapshot = ReactorControlRodTick.advance(previous);

        // 2. 计算本 tick 各燃料列的裂变热量和燃耗需求。
        ReactorFissionResult fission = ReactorFissionReference.calculate(
                controlSnapshot,
                parameters
        );

        ReactorMaintenanceHeat.Settlement maintenance = ReactorMaintenanceHeat.settle(controlSnapshot, fission, maintenanceHeatHu);
        double fuelExpense=maintenance.removedByColumn().values().stream().mapToDouble(Double::doubleValue).sum();
        double availableHeatHu = Math.max(0, availableFuelHeat(controlSnapshot, fission) - fuelExpense);
        var afterMaintenanceRods=new TreeMap<CoreColumnPosition,ControlRodColumnState>(controlSnapshot.controlRodColumns());
        maintenance.removedFromControlRods().forEach((position,removed)->{
            var rod=afterMaintenanceRods.get(position);
            afterMaintenanceRods.put(position,new ControlRodColumnState(rod.integrity(),rod.targetDepth(),rod.actualDepth(),rod.jammed(),Math.max(0,rod.cachedHeatHu()-removed)));
        });
        // 维护可以支付杆真实余热，冷却仍只吸收原燃料账本；杆机械状态和事故算法不改。
        var maintenanceSnapshot=controlSnapshot.withColumns(controlSnapshot.fuelColumns(),afterMaintenanceRods);

        // 3. 只转换共享冷却剂账本能够承受的热量；流体 Capability 已将冷端输入结算到快照，
        // ticker 在此消耗共享冷却剂库存。
        ReactorCoolantSimulationAdapter.Result coolant =
                ReactorCoolantSimulationAdapter.settle(
                        maintenanceSnapshot,
                        new ReactorCoolantSimulationAdapter.TickInput(
                                availableHeatHu,
                                coolantInput.ports(),
                                0.0D,
                                0.0D,
                                coolantInput.coolantCapacityMb(),
                                coolantInput.coolantAbsorptionHuPerMb()
                        )
                );

        Map<CoreColumnPosition, Double> coolingByColumn = allocateHeatRemoval(
                controlSnapshot,
                fission,
                coolant.settlement().removedHeatHu(), maintenance.removedByColumn()
        );
        Map<CoreColumnPosition,Double> allRemoved = new TreeMap<>(coolingByColumn);
        maintenance.removedByColumn().forEach((position,amount) -> allRemoved.merge(position,amount,Double::sum));
        Map<CoreColumnPosition, Double> quantizedRemainderByColumn = allocateQuantizedRemainder(
                controlSnapshot,
                fission,
                allRemoved,
                coolant.quantizedHeatRemainderHu()
        );

        // 4. 逐列结算有效热负荷和完整度损伤；冷却后的库存仍由快照权威拥有。
        ReactorThermalResult thermal = ReactorThermalCalculator.settleFissionHeat(
                coolant.nextSnapshot(),
                fission,
                allRemoved,
                quantizedRemainderByColumn,
                parameters
        );

        // 5. 从完整热快照传播失效列的残余热量；第 3 步已消耗冷却预算，传播阶段不重复创建预算。
        HeatPropagationResult propagation = ReactorHeatPropagation.propagate(
                thermal.snapshot(),
                Map.of(),
                parameters
        );

        // 6. 维护不是冷却回路证据；只有冷却剂实际移热可暂停原融毁倒计时。
        MeltdownUpdateResult meltdown = ReactorMeltdownStateMachine.update(
                thermal.snapshot(),
                propagation,
                controlSnapshot.scramActive(),
                coolant.settlement().removedHeatHu() > HEAT_EPSILON,
                parameters
        );

        // 7. 在热量和传播快照完成后保存小数燃耗，使本 tick 耗尽的燃料仍贡献已结算热量，
        // 并从下一 tick 开始停止产热。
        ReactorSnapshot next = applyFuelBurn(meltdown.snapshot(), fission);
        return new ReactorServerTick.Result(
                next,
                controlSnapshot,
                fission,
                coolant,
                thermal,
                propagation,
                meltdown,
                coolant.settlement().removedHeatHu(), maintenance
        );
    }

    private static double availableFuelHeat(
            ReactorSnapshot snapshot,
            ReactorFissionResult fission
    ) {
        // 新生热采用裂变阶段的权威总量；缓存热按原账本直接相加，不能再次取整。
        double total = fission.generatedHeatHu();
        for (Map.Entry<CoreColumnPosition, FuelColumnState> entry : snapshot.fuelColumns().entrySet()) {
            total += entry.getValue().cachedHeatHu();
        }
        if (!Double.isFinite(total) || total < 0.0D) {
            throw new IllegalArgumentException("available reactor heat must be finite and non-negative");
        }
        return total;
    }

    private static Map<CoreColumnPosition, Double> allocateHeatRemoval(
            ReactorSnapshot snapshot,
            ReactorFissionResult fission,
            double totalRemovedHeatHu,
            Map<CoreColumnPosition,Double> maintenanceByColumn
    ) {
        if (!Double.isFinite(totalRemovedHeatHu) || totalRemovedHeatHu < 0.0D) {
            throw new IllegalArgumentException("removed reactor heat must be finite and non-negative");
        }
        double totalAvailable = Math.max(0,availableFuelHeat(snapshot, fission)
                - maintenanceByColumn.values().stream().mapToDouble(Double::doubleValue).sum());
        if (totalRemovedHeatHu <= HEAT_EPSILON || totalAvailable <= HEAT_EPSILON) {
            return Map.of();
        }
        double boundedRemoval = Math.min(totalRemovedHeatHu, totalAvailable);
        TreeMap<CoreColumnPosition, Double> allocated = new TreeMap<>();
        double assigned = 0.0D;
        CoreColumnPosition last = null;
        for (Map.Entry<CoreColumnPosition, FuelColumnState> entry : snapshot.fuelColumns().entrySet()) {
            CoreColumnPosition position = entry.getKey();
            FuelColumnFissionResult column = fission.columns().get(position);
            double available = (column == null ? 0.0D : column.generatedHeatHu())
                    + entry.getValue().cachedHeatHu() - maintenanceByColumn.getOrDefault(position,0.0D);
            if (available <= HEAT_EPSILON) {
                continue;
            }
            double share = boundedRemoval * available / totalAvailable;
            allocated.put(position, share);
            assigned += share;
            last = position;
        }
        if (last != null) {
            allocated.put(last, Math.max(0.0D,
                    allocated.get(last) + (boundedRemoval - assigned)));
        }
        return allocated;
    }

    /** 按各列冷却后剩余热量比例分配全堆整数 mB 量化余数。 */
    private static Map<CoreColumnPosition, Double> allocateQuantizedRemainder(
            ReactorSnapshot snapshot,
            ReactorFissionResult fission,
            Map<CoreColumnPosition, Double> removedHeatByColumn,
            double totalQuantizedRemainderHu
    ) {
        if (!Double.isFinite(totalQuantizedRemainderHu) || totalQuantizedRemainderHu < 0.0D) {
            throw new IllegalArgumentException("quantized reactor heat must be finite and non-negative");
        }
        if (totalQuantizedRemainderHu <= HEAT_EPSILON) {
            return Map.of();
        }

        TreeMap<CoreColumnPosition, Double> remainingByColumn = new TreeMap<>();
        double totalRemaining = 0.0D;
        for (Map.Entry<CoreColumnPosition, FuelColumnState> entry : snapshot.fuelColumns().entrySet()) {
            FuelColumnFissionResult column = fission.columns().get(entry.getKey());
            double available = (column == null ? 0.0D : column.generatedHeatHu())
                    + entry.getValue().cachedHeatHu();
            double removed = Math.min(available,
                    removedHeatByColumn.getOrDefault(entry.getKey(), 0.0D));
            double remaining = Math.max(0.0D, available - removed);
            if (remaining > HEAT_EPSILON) {
                remainingByColumn.put(entry.getKey(), remaining);
                totalRemaining += remaining;
            }
        }
        if (totalRemaining <= HEAT_EPSILON) {
            return Map.of();
        }

        double boundedRemainder = Math.min(totalQuantizedRemainderHu, totalRemaining);
        TreeMap<CoreColumnPosition, Double> allocated = new TreeMap<>();
        double assigned = 0.0D;
        CoreColumnPosition last = null;
        for (Map.Entry<CoreColumnPosition, Double> entry : remainingByColumn.entrySet()) {
            double share = boundedRemainder * entry.getValue() / totalRemaining;
            allocated.put(entry.getKey(), share);
            assigned += share;
            last = entry.getKey();
        }
        if (last != null) {
            allocated.put(last, Math.max(0.0D,
                    allocated.get(last) + (boundedRemainder - assigned)));
        }
        return allocated;
    }

    private static ReactorSnapshot applyFuelBurn(
            ReactorSnapshot snapshot,
            ReactorFissionResult fission
    ) {
        TreeMap<CoreColumnPosition, FuelColumnState> nextFuel =
                new TreeMap<>(snapshot.fuelColumns());
        for (Map.Entry<CoreColumnPosition, FuelColumnFissionResult> entry : fission.columns().entrySet()) {
            FuelColumnState fuel = nextFuel.get(entry.getKey());
            if (fuel != null) {
                nextFuel.put(entry.getKey(), fuel.burnFraction(entry.getValue().plannedFuelBurnUnits()));
            }
        }
        return snapshot.withColumns(nextFuel, snapshot.controlRodColumns());
    }
}
