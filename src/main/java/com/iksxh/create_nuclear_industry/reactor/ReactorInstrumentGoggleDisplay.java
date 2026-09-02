package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.structure.ReactorInstrumentStructureSummary;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 反应堆仪表端口的护目镜动态显示适配器。
 *
 * <p>本类只把已经同步到客户端的静态摘要和动态遥测转换为显示文本，不读取世界、不触发
 * 结构扫描、不修改反应堆状态，也不在客户端重新计算热量或冷却剂结算。行列编号使用堆芯
 * 坐标的 1-based 行/列表示：行对应 z，列对应 x；动态列列表已经由遥测模型稳定排序。</p>
 */
public final class ReactorInstrumentGoggleDisplay {
    private static final String GOGGLE_KEY_PREFIX = "goggle.create_nuclear_industry.reactor.";

    private ReactorInstrumentGoggleDisplay() {
    }

    /**
     * 在静态结构摘要之后追加动态运行遥测。
     *
     * @param tooltip 护目镜文本列表，追加顺序就是最终显示顺序
     * @param structureSummary 客户端最近一次同步的有效结构摘要
     * @param telemetry 客户端最近一次同步的动态遥测
     */
    public static void appendDynamicTooltip(
            List<Component> tooltip,
            ReactorInstrumentStructureSummary structureSummary,
            ReactorInstrumentTelemetry telemetry
    ) {
        Objects.requireNonNull(tooltip, "goggle tooltip is required");
        Objects.requireNonNull(structureSummary, "structure summary is required");
        Objects.requireNonNull(telemetry, "instrument telemetry is required");
        if (!structureSummary.valid()) {
            return;
        }

        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "dynamic_summary"));
        if (!telemetry.available()) {
            tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "runtime_data_waiting"));
            return;
        }

        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "cold_inventory",
                Long.toString(telemetry.coldCoolantMb()),
                Long.toString(structureSummary.coldInventoryCapacityMb())));
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "hot_inventory",
                Long.toString(telemetry.hotCoolantMb()),
                Long.toString(structureSummary.hotInventoryCapacityMb())));
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "total_fission_heat",
                formatRate(telemetry.totalGeneratedFissionHeatHuPerTick())));
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "coolant_conversion",
                formatRate(telemetry.convertedCoolantMbPerTick())));

    }

    /** 追加单个换料端口对应的燃料列完整度和发热量，不显示其它列。 */
    public static void appendFuelColumnTooltip(
            List<Component> tooltip,
            ReactorInstrumentTelemetry.FuelColumnTelemetry column
    ) {
        Objects.requireNonNull(tooltip, "goggle tooltip is required");
        Objects.requireNonNull(column, "fuel column telemetry is required");
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "fuel_column",
                row(column.position()),
                column(column.position()),
                formatPercent(column.fuelColumnIntegrity()),
                formatRate(column.generatedFissionHeatHuPerTick())));
    }

    /** 追加单个控制棒驱动器对应的控制棒列完整度，不显示其它列。 */
    public static void appendControlRodColumnTooltip(
            List<Component> tooltip,
            ReactorInstrumentTelemetry.ControlRodColumnTelemetry column
    ) {
        Objects.requireNonNull(tooltip, "goggle tooltip is required");
        Objects.requireNonNull(column, "control rod column telemetry is required");
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "control_rod_column",
                row(column.position()),
                column(column.position()),
                formatPercent(column.controlRodColumnIntegrity())));
    }

    /** 使用固定一位小数显示百分比，避免浮点尾数和本地化小数点造成不确定文本。 */
    static String formatPercent(double fraction) {
        return String.format(Locale.ROOT, "%.1f%%", normalizeZero(fraction * 100.0D));
    }

    /** 使用固定一位小数显示 HU/t 或 mB/t，零值明确显示为 0.0。 */
    static String formatRate(double value) {
        return String.format(Locale.ROOT, "%.1f", normalizeZero(value));
    }

    private static String row(CoreColumnPosition position) {
        return Integer.toString(position.z() + 1);
    }

    private static String column(CoreColumnPosition position) {
        return Integer.toString(position.x() + 1);
    }

    private static double normalizeZero(double value) {
        return value == 0.0D ? 0.0D : value;
    }
}
