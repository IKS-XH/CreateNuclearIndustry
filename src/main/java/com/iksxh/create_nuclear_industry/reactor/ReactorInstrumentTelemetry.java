package com.iksxh.create_nuclear_industry.reactor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 仪表端口展示用的最后一次正式反应堆 tick 动态遥测。
 *
 * <p>这是只读派生数据，不属于 {@link ReactorSnapshot} 的第二份权威状态。有效遥测的
 * 冷/热库存来自 tick 后快照；发热只来自该 tick 的新生裂变热，不能把 {@code cachedHeatHu}
 * 余热再次计入；冷却剂转化量来自本 tick 的实际结算结果。坐标列表始终按先 z 后 x 排序，
 * 供客户端后续显示使用。</p>
 */
public record ReactorInstrumentTelemetry(
        boolean available,
        String unavailableReason,
        long coldCoolantMb,
        long hotCoolantMb,
        List<FuelColumnTelemetry> fuelColumns,
        List<ControlRodColumnTelemetry> controlRodColumns,
        double totalGeneratedFissionHeatHuPerTick,
        double convertedCoolantMbPerTick
) {
    private static final double SUM_EPSILON = 1.0E-9D;
    private static final Comparator<CoreColumnPosition> Z_THEN_X_ORDER =
            Comparator.comparingInt(CoreColumnPosition::z)
                    .thenComparingInt(CoreColumnPosition::x);

    public ReactorInstrumentTelemetry {
        if (unavailableReason == null) {
            throw new IllegalArgumentException("telemetry availability reason is required");
        }
        if (coldCoolantMb < 0L || hotCoolantMb < 0L) {
            throw new IllegalArgumentException("coolant inventories must be non-negative");
        }
        if (!Double.isFinite(totalGeneratedFissionHeatHuPerTick)
                || totalGeneratedFissionHeatHuPerTick < 0.0D
                || !Double.isFinite(convertedCoolantMbPerTick)
                || convertedCoolantMbPerTick < 0.0D) {
            throw new IllegalArgumentException("telemetry rates must be finite and non-negative");
        }
        fuelColumns = orderedFuelColumns(fuelColumns);
        controlRodColumns = orderedControlRodColumns(controlRodColumns);

        if (available) {
            if (!unavailableReason.isEmpty()) {
                throw new IllegalArgumentException("available telemetry cannot have an unavailable reason");
            }
            double summedHeat = fuelColumns.stream()
                    .mapToDouble(FuelColumnTelemetry::generatedFissionHeatHuPerTick)
                    .sum();
            if (Math.abs(summedHeat - totalGeneratedFissionHeatHuPerTick) > SUM_EPSILON) {
                throw new IllegalArgumentException("total fission heat must equal the column heat sum");
            }
        } else {
            if (unavailableReason.isBlank()) {
                throw new IllegalArgumentException("unavailable telemetry needs a reason");
            }
            if (coldCoolantMb != 0L || hotCoolantMb != 0L || !fuelColumns.isEmpty()
                    || !controlRodColumns.isEmpty() || totalGeneratedFissionHeatHuPerTick != 0.0D
                    || convertedCoolantMbPerTick != 0.0D) {
                throw new IllegalArgumentException("unavailable telemetry must not retain dynamic values");
            }
        }
    }

    /** 单个燃料列在最后一次正式 tick 的完整度与新生裂变热。热量单位为 HU/t。 */
    public record FuelColumnTelemetry(
            CoreColumnPosition position,
            double fuelColumnIntegrity,
            double generatedFissionHeatHuPerTick
    ) {
        public FuelColumnTelemetry {
            if (position == null) {
                throw new IllegalArgumentException("fuel telemetry position is required");
            }
            requireUnitInterval("fuel column integrity", fuelColumnIntegrity);
            requireFiniteNonNegative("fuel column generated heat", generatedFissionHeatHuPerTick);
        }
    }

    /** 单个控制棒列在最后一次正式 tick 的完整度。完整度为 [0, 1]，无单位。 */
    public record ControlRodColumnTelemetry(
            CoreColumnPosition position,
            double controlRodColumnIntegrity
    ) {
        public ControlRodColumnTelemetry {
            if (position == null) {
                throw new IllegalArgumentException("control rod telemetry position is required");
            }
            requireUnitInterval("control rod column integrity", controlRodColumnIntegrity);
        }
    }

    /** 创建尚未完成有效正式 tick 或动态数据已失效的遥测。 */
    public static ReactorInstrumentTelemetry unavailable(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("unavailable telemetry reason is required");
        }
        return new ReactorInstrumentTelemetry(false, reason, 0L, 0L,
                List.of(), List.of(), 0.0D, 0.0D);
    }

    /**
     * 从一次已经完成的正式 tick 结果派生遥测，不重新计算模拟，也不读取世界。
     *
     * @param result 正式服务端 tick 的完整阶段结果
     * @return 对应 tick 后状态的只读遥测
     */
    public static ReactorInstrumentTelemetry from(ReactorServerTick.Result result) {
        if (result == null) {
            throw new IllegalArgumentException("reactor tick result is required");
        }

        ReactorSnapshot snapshot = result.snapshot();
        List<FuelColumnTelemetry> fuel = new ArrayList<>();
        double totalHeat = 0.0D;
        for (Map.Entry<CoreColumnPosition, FuelColumnState> entry
                : snapshot.fuelColumns().entrySet()) {
            FuelColumnFissionResult fission = result.fission().columns().get(entry.getKey());
            double generatedHeat = fission == null ? 0.0D : fission.generatedHeatHu();
            fuel.add(new FuelColumnTelemetry(entry.getKey(), entry.getValue().integrity(), generatedHeat));
            totalHeat += generatedHeat;
        }

        List<ControlRodColumnTelemetry> controlRods = new ArrayList<>();
        for (Map.Entry<CoreColumnPosition, ControlRodColumnState> entry
                : snapshot.controlRodColumns().entrySet()) {
            controlRods.add(new ControlRodColumnTelemetry(
                    entry.getKey(), entry.getValue().integrity()));
        }

        return new ReactorInstrumentTelemetry(
                true,
                "",
                snapshot.coldCoolantMb(),
                snapshot.hotCoolantMb(),
                fuel,
                controlRods,
                totalHeat,
                result.coolant().settlement().convertedCoolantMb()
        );
    }

    private static List<FuelColumnTelemetry> orderedFuelColumns(
            List<FuelColumnTelemetry> columns
    ) {
        if (columns == null) {
            throw new IllegalArgumentException("fuel telemetry columns are required");
        }
        List<FuelColumnTelemetry> ordered = new ArrayList<>(columns);
        ordered.sort(Comparator.comparing(FuelColumnTelemetry::position, Z_THEN_X_ORDER));
        ensureUniqueFuelPositions(ordered);
        return List.copyOf(ordered);
    }

    private static List<ControlRodColumnTelemetry> orderedControlRodColumns(
            List<ControlRodColumnTelemetry> columns
    ) {
        if (columns == null) {
            throw new IllegalArgumentException("control rod telemetry columns are required");
        }
        List<ControlRodColumnTelemetry> ordered = new ArrayList<>(columns);
        ordered.sort(Comparator.comparing(ControlRodColumnTelemetry::position, Z_THEN_X_ORDER));
        CoreColumnPosition previous = null;
        for (ControlRodColumnTelemetry column : ordered) {
            if (previous != null && previous.equals(column.position())) {
                throw new IllegalArgumentException("duplicate control rod telemetry position");
            }
            previous = column.position();
        }
        return List.copyOf(ordered);
    }

    private static void ensureUniqueFuelPositions(List<FuelColumnTelemetry> columns) {
        CoreColumnPosition previous = null;
        for (FuelColumnTelemetry column : columns) {
            if (previous != null && previous.equals(column.position())) {
                throw new IllegalArgumentException("duplicate fuel telemetry position");
            }
            previous = column.position();
        }
    }

    private static void requireUnitInterval(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]");
        }
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
