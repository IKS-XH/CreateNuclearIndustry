package com.iksxh.create_nuclear_industry.config;

import com.iksxh.create_nuclear_industry.boiler.BoilerState;
import java.util.List;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 独立 SERVER 锅炉配置；旧固定容量与暖炉键不再参与任何热工结算。 */
public final class BoilerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> DIMENSION_RANGE;
    public static final ModConfigSpec.IntValue WATER_CAPACITY_PER_CELL_MB, STEAM_CAPACITY_PER_CELL_MB, PORT_FLOW_MB_PER_TICK, VALVE_FLOW_PER_STEAM_CELL;
    public static final ModConfigSpec.DoubleValue PAIR_HEAT, BOILING_TEMPERATURE, SUPERCRITICAL_TEMPERATURE,
            WALL_HEAT_CAPACITY, WATER_SPECIFIC_HEAT, STEAM_SPECIFIC_HEAT, LATENT_HEAT,
            SUPERCRITICAL_PRESSURE, NORMAL_MIN_PRESSURE, SUPERCRITICAL_MIN_PRESSURE,
            VALVE_OPEN_PRESSURE, VALVE_CLOSE_PRESSURE, WATER_COOLING, STEAM_COOLING;
    static {
        var b = new ModConfigSpec.Builder();
        DIMENSION_RANGE = b.comment("外部长宽高各自允许的闭区间，恰好两个整数，5≤min≤max≤32。")
                .defineList("dimensionRange", List.of(5, 11), value -> value instanceof Integer n && n >= 5 && n <= 32);
        WATER_CAPACITY_PER_CELL_MB = b.comment("每格有效水区容量，单位 mB。") .defineInRange("waterCapacityPerCellMb", 2000, 1, 1_000_000);
        STEAM_CAPACITY_PER_CELL_MB = b.comment("每格有效汽区容量，单位 mB。") .defineInRange("steamCapacityPerCellMb", 2000, 1, 1_000_000);
        PORT_FLOW_MB_PER_TICK = b.comment("每个物理口共用主动/被动额度，单位 mB/t。") .defineInRange("portFlowMbPerTick", 256, 1, 1_000_000);
        PAIR_HEAT = positive(b, "pairHeatHuPerTick", 18, "每对换热器/再加热段的 HU/t 上限。");
        BOILING_TEMPERATURE = positive(b, "boilingTemperature", 1, "归一沸点温度。");
        SUPERCRITICAL_TEMPERATURE = positive(b, "supercriticalTemperature", 2, "归一超临界资格温度，必须高于沸点。");
        WALL_HEAT_CAPACITY = positive(b, "wallHeatCapacityHuPerWaterCell", 1600, "每格水区对应的炉壁热容 HU/温升。");
        WATER_SPECIFIC_HEAT = positive(b, "waterSpecificHeatHuPerMb", .1, "水比热 HU/mB/温升。");
        STEAM_SPECIFIC_HEAT = positive(b, "steamSpecificHeatHuPerMb", .2, "汽比热 HU/mB/温升。");
        LATENT_HEAT = positive(b, "vaporizationLatentHeatHuPerMb", .7, "汽化追加潜热 HU/mB。");
        SUPERCRITICAL_PRESSURE = positive(b, "supercriticalPressure", .5, "超临界资格炉压。");
        NORMAL_MIN_PRESSURE = nonnegative(b, "normalOutputMinPressure", .1, "普通模式默认保压。");
        SUPERCRITICAL_MIN_PRESSURE = nonnegative(b, "supercriticalOutputMinPressure", .6, "超临界默认保压，不低于资格门槛。");
        VALVE_OPEN_PRESSURE = positive(b, "valveOpenPressure", .9, "开启安全阀及堵塞保护的炉压。");
        VALVE_CLOSE_PRESSURE = nonnegative(b, "valveClosePressure", .8, "关闭安全阀炉压，必须低于开启线。");
        VALVE_FLOW_PER_STEAM_CELL = b.comment("每格汽区提供的阀泄放额度 mB/t。") .defineInRange("valveFlowPerSteamCellMbPerTick", 32, 1, 1_000_000);
        WATER_COOLING = nonnegative(b, "idleWaterCoolingHuPerCellPerTick", .9, "无收热时每水区格散失显热 HU/t。");
        STEAM_COOLING = nonnegative(b, "idleSteamCoolingHuPerCellPerTick", .1, "无收热时每汽区格散失高于沸点的显热 HU/t。");
        SPEC = b.build();
    }
    private BoilerConfig() {}
    private static ModConfigSpec.DoubleValue positive(ModConfigSpec.Builder b, String key, double value, String comment) {
        return b.comment(comment).defineInRange(key, value, .000001, 1e9);
    }
    private static ModConfigSpec.DoubleValue nonnegative(ModConfigSpec.Builder b, String key, double value, String comment) {
        return b.comment(comment).defineInRange(key, value, 0, 1e9);
    }
    public static void register(ModContainer container) { container.registerConfig(ModConfig.Type.SERVER, SPEC, "create_nuclear_industry-boiler.toml"); }
    /** 跨字段非法值保持无效快照，由服务端拒绝成型/交易并提示玩家。 */
    public static BoilerState.Settings settings() {
        List<? extends Integer> range = DIMENSION_RANGE.get();
        return new BoilerState.Settings(range.size() == 2 ? range.get(0) : 0, range.size() == 2 ? range.get(1) : 0,
                WATER_CAPACITY_PER_CELL_MB.get(), STEAM_CAPACITY_PER_CELL_MB.get(), PORT_FLOW_MB_PER_TICK.get(), PAIR_HEAT.get(),
                BOILING_TEMPERATURE.get(), SUPERCRITICAL_TEMPERATURE.get(), WALL_HEAT_CAPACITY.get(), WATER_SPECIFIC_HEAT.get(),
                STEAM_SPECIFIC_HEAT.get(), LATENT_HEAT.get(), SUPERCRITICAL_PRESSURE.get(), NORMAL_MIN_PRESSURE.get(),
                SUPERCRITICAL_MIN_PRESSURE.get(), VALVE_OPEN_PRESSURE.get(), VALVE_CLOSE_PRESSURE.get(), VALVE_FLOW_PER_STEAM_CELL.get(), WATER_COOLING.get(), STEAM_COOLING.get());
    }
}
