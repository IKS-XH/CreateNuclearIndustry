package com.iksxh.create_nuclear_industry.reactor;

import java.util.Map;
import java.util.TreeMap;

/**
 * 已成形 P1 反应堆的单 tick 权威编排器。
 *
 * <p>各计算器保持纯函数和确定性；本类统一定义生产顺序：</p>
 *
 * <pre>
 * 控制状态 -> 裂变热量/燃耗 -> 冷却分配 -> 有效热负荷/损伤
 * -> 四向传播 -> 熔毁状态 -> 燃耗持久化
 * </pre>
 */
public final class ReactorServerTick {
    private static final double HEAT_EPSILON = 1.0E-12D;

    private ReactorServerTick() {
    }

    /** 世界侧适配器提供的本 tick 冷却剂观测。 */
    public record CoolantInput(
            ReactorCoolantLedger.PortSummary ports,
            long hotInventoryCapacityMb,
            double coolantAbsorptionHuPerMb
    ) {
        public CoolantInput {
            if (ports == null) {
                throw new IllegalArgumentException("coolant port summary is required");
            }
            if (hotInventoryCapacityMb < 0L) {
                throw new IllegalArgumentException("hot inventory capacity must be non-negative");
            }
            if (!Double.isFinite(coolantAbsorptionHuPerMb) || coolantAbsorptionHuPerMb <= 0.0D) {
                throw new IllegalArgumentException("coolant absorption must be finite and positive");
            }
        }

        public static CoolantInput none() {
            return new CoolantInput(
                    ReactorCoolantLedger.summarizePorts(java.util.List.of(), 0.0D),
                    0L,
                    1.0D
            );
        }
    }

    /** 暴露完整阶段证据，供确定性单元测试和 GameTest 断言。 */
    public record Result(
            ReactorSnapshot snapshot,
            ReactorSnapshot controlSnapshot,
            ReactorFissionResult fission,
            ReactorCoolantSimulationAdapter.Result coolant,
            ReactorThermalResult thermal,
            HeatPropagationResult propagation,
            MeltdownUpdateResult meltdown,
            double effectiveCoolingHeatHu
    ) {
        public Result {
            if (snapshot == null || controlSnapshot == null || fission == null
                    || coolant == null || thermal == null || propagation == null || meltdown == null) {
                throw new IllegalArgumentException("reactor tick stages are required");
            }
            if (!Double.isFinite(effectiveCoolingHeatHu) || effectiveCoolingHeatHu < 0.0D) {
                throw new IllegalArgumentException("effective cooling heat must be finite and non-negative");
            }
        }
    }

    /** 按固定 P1 阶段顺序结算一个服务端 tick。 */
    public static Result advance(
            ReactorSnapshot previous,
            ReactorSimulationParameters parameters,
            CoolantInput coolantInput
    ) {
        if (previous == null || parameters == null || coolantInput == null) {
            throw new IllegalArgumentException("reactor tick inputs are required");
        }

        // 1. 读取并应用权威控制状态；卡死棒由 ReactorControlRodTick 原样保留。
        ReactorSnapshot controlSnapshot = ReactorControlRodTick.advance(previous);

        // 2. 计算本 tick 各燃料列的裂变热量和燃耗需求。
        ReactorFissionResult fission = ReactorFissionCalculator.calculate(
                controlSnapshot,
                parameters
        );

        double availableHeatHu = availableFuelHeat(controlSnapshot, fission);

        // 3. 只转换共享冷却剂账本能够承受的热量；流体 Capability 已将冷端输入结算到快照，
        // ticker 在此消耗共享冷却剂库存。
        ReactorCoolantSimulationAdapter.Result coolant =
                ReactorCoolantSimulationAdapter.settle(
                        controlSnapshot,
                        new ReactorCoolantSimulationAdapter.TickInput(
                                availableHeatHu,
                                coolantInput.ports(),
                                0.0D,
                                0.0D,
                                coolantInput.hotInventoryCapacityMb(),
                                coolantInput.coolantAbsorptionHuPerMb()
                        )
                );

        Map<CoreColumnPosition, Double> coolingByColumn = allocateHeatRemoval(
                controlSnapshot,
                fission,
                coolant.settlement().removedHeatHu()
        );

        // 4. 逐列结算有效热负荷和完整度损伤；冷却后的库存仍由快照权威拥有。
        ReactorThermalResult thermal = ReactorThermalCalculator.settleFissionHeat(
                coolant.nextSnapshot(),
                fission,
                coolingByColumn,
                parameters
        );

        // 5. 从完整热快照传播失效列的残余热量；第 3 步已消耗冷却预算，传播阶段不重复创建预算。
        HeatPropagationResult propagation = ReactorHeatPropagation.propagate(
                thermal.snapshot(),
                Map.of(),
                parameters
        );

        // 6. 更新单调熔毁状态；实际移除热量即视为满足暂停合同的有效冷却观测。
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
        return new Result(
                next,
                controlSnapshot,
                fission,
                coolant,
                thermal,
                propagation,
                meltdown,
                coolant.settlement().removedHeatHu()
        );
    }

    private static double availableFuelHeat(
            ReactorSnapshot snapshot,
            ReactorFissionResult fission
    ) {
        double total = 0.0D;
        for (Map.Entry<CoreColumnPosition, FuelColumnState> entry : snapshot.fuelColumns().entrySet()) {
            FuelColumnFissionResult column = fission.columns().get(entry.getKey());
            double generated = column == null ? 0.0D : column.generatedHeatHu();
            total += generated + entry.getValue().cachedHeatHu();
        }
        if (!Double.isFinite(total) || total < 0.0D) {
            throw new IllegalArgumentException("available reactor heat must be finite and non-negative");
        }
        return total;
    }

    private static Map<CoreColumnPosition, Double> allocateHeatRemoval(
            ReactorSnapshot snapshot,
            ReactorFissionResult fission,
            double totalRemovedHeatHu
    ) {
        if (!Double.isFinite(totalRemovedHeatHu) || totalRemovedHeatHu < 0.0D) {
            throw new IllegalArgumentException("removed reactor heat must be finite and non-negative");
        }
        double totalAvailable = availableFuelHeat(snapshot, fission);
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
                    + entry.getValue().cachedHeatHu();
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
