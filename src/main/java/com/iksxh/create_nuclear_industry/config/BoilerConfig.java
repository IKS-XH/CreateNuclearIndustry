package com.iksxh.create_nuclear_industry.config;

import com.iksxh.create_nuclear_industry.boiler.BoilerState;
import com.iksxh.create_nuclear_industry.boiler.BoilerAccidentState;
import java.util.List;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 独立 SERVER 锅炉配置；旧固定容量与暖炉键不再参与任何热工结算。 */
public final class BoilerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> DIMENSION_RANGE;
    public static final ModConfigSpec.IntValue WATER_CAPACITY_PER_CELL_MB, STEAM_CAPACITY_PER_CELL_MB, COOLANT_PORT_FLOW_MB_PER_TICK, WATER_STEAM_PORT_FLOW_MB_PER_TICK, VALVE_FLOW_MB_PER_TICK;
    public static final ModConfigSpec.DoubleValue PAIR_HEAT, BOILING_TEMPERATURE, SUPERCRITICAL_TEMPERATURE,
            WALL_HEAT_CAPACITY, WATER_SPECIFIC_HEAT, STEAM_SPECIFIC_HEAT, LATENT_HEAT,
            SUPERCRITICAL_PRESSURE, OUTPUT_MIN_PRESSURE,
            VALVE_OPEN_PRESSURE, VALVE_CLOSE_PRESSURE, WATER_COOLING, STEAM_COOLING;
    public static final ModConfigSpec.DoubleValue RATED_PRESSURE, INSTANT_RATIO, RECOVERY_RATIO, MINIMUM_RADIUS, RADIUS_PER_RATIO, MAXIMUM_RADIUS, BURST_DAMAGE;
    public static final ModConfigSpec.IntValue PRESSURE_TICKS, RECOVERY_TICKS, DRY_TICKS;
    static {
        var b = new ModConfigSpec.Builder();
        DIMENSION_RANGE = b.comment("外部长宽高各自允许的闭区间，恰好两个整数，5≤min≤max≤16。")
                .defineList("dimensionRange", List.of(5, 16), value -> value instanceof Integer n && n >= 5 && n <= 16);
        WATER_CAPACITY_PER_CELL_MB = b.comment("每格有效水区容量，单位 mB。") .defineInRange("waterCapacityPerCellMb", 2000, 1, 1_000_000);
        STEAM_CAPACITY_PER_CELL_MB = b.comment("每格有效汽区容量，单位 mB。") .defineInRange("steamCapacityPerCellMb", 2000, 1, 1_000_000);
        COOLANT_PORT_FLOW_MB_PER_TICK = b.comment("每个冷热液物理口共用主被动及专用额度 mB/t。") .defineInRange("coolantPortFlowMbPerTick", 2048, 1, 1_000_000);
        WATER_STEAM_PORT_FLOW_MB_PER_TICK = b.comment("每个给水及蒸汽物理口共用主被动及专用额度 mB/t。") .defineInRange("waterSteamPortFlowMbPerTick", 4096, 1, 1_000_000);
        PAIR_HEAT = positive(b, "pairHeatHuPerTick", 18, "每对换热器/再加热段的 HU/t 上限。");
        BOILING_TEMPERATURE = positive(b, "boilingTemperature", 1, "归一沸点温度。");
        SUPERCRITICAL_TEMPERATURE = positive(b, "supercriticalTemperature", 2, "新批次归类与SC库存已付热交付所需的归一汽温，必须高于沸点。");
        WALL_HEAT_CAPACITY = positive(b, "wallHeatCapacityHuPerWaterCell", 1600, "每格水区对应的炉壁热容 HU/温升。");
        WATER_SPECIFIC_HEAT = positive(b, "waterSpecificHeatHuPerMb", .1, "水比热 HU/mB/温升。");
        STEAM_SPECIFIC_HEAT = positive(b, "steamSpecificHeatHuPerMb", .2, "汽比热 HU/mB/温升。");
        LATENT_HEAT = positive(b, "vaporizationLatentHeatHuPerMb", .7, "汽化追加潜热 HU/mB。");
        SUPERCRITICAL_PRESSURE = positive(b, "supercriticalPressure", .5, "新批次归类SC所需的共同炉压；已有SC仅受公共出汽下限限制。");
        OUTPUT_MIN_PRESSURE = b.comment("默认出汽压力下限，归一范围[0,1]；高于阀开启线也合法，仅限制正常出汽。")
                .defineInRange("outputMinPressure", .6, 0, 1);
        VALVE_OPEN_PRESSURE = positive(b, "valveOpenPressure", .9, "开启安全阀的炉压。");
        VALVE_CLOSE_PRESSURE = nonnegative(b, "valveClosePressure", .8, "关闭安全阀炉压，必须低于开启线。");
        VALVE_FLOW_MB_PER_TICK = b.comment("每个畅通物理泄压阀的固定排汽上限，单位 mB/t；不按汽区容积放大。") .defineInRange("valveFlowMbPerTick", 512, 1, 1_000_000);
        WATER_COOLING = nonnegative(b, "idleWaterCoolingHuPerCellPerTick", .9, "无收热时每水区格散失显热 HU/t。");
        STEAM_COOLING = nonnegative(b, "idleSteamCoolingHuPerCellPerTick", .1, "无收热时每汽区格散失高于沸点的显热 HU/t。");
        b.push("accident");
        RATED_PRESSURE=positive(b,"ratedPressure",1,"额定归一耐压，1对应100%。");
        INSTANT_RATIO=positive(b,"instantBurstPressureRatio",1.5,"立即爆裂的实际压力/额定压力，必须大于1。");
        PRESSURE_TICKS=b.comment("持续超压耐受的加载tick，默认200即10秒。").defineInRange("overpressureTicks",200,1,72000);
        RECOVERY_RATIO=positive(b,"recoveryPressureRatio",.95,"清除累计超压计时所需压力/额定压力，必须小于1。");
        RECOVERY_TICKS=b.comment("低于恢复线持续的加载tick。").defineInRange("recoveryTicks",40,1,72000);
        DRY_TICKS=b.comment("无水且炉壁不低于沸点，连续多少加载tick损毁一件换热部件。").defineInRange("dryFiringTicks",400,1,72000);
        MINIMUM_RADIUS=b.comment("爆裂最小半径，格。").defineInRange("minimumBurstRadius",2D,1D,32D);
        RADIUS_PER_RATIO=b.comment("每超过一倍额定压力增加的半径，格。").defineInRange("burstRadiusPerPressureRatio",4,.000001,32);
        MAXIMUM_RADIUS=b.comment("爆裂半径上限，格；不得小于最小半径。").defineInRange("maximumBurstRadius",6D,1D,32D);
        BURST_DAMAGE=b.comment("喷爆中心最高伤害，HP；按距离衰减且每生物每事故一次。").defineInRange("maximumBurstDamageHp",16,.000001,1024);
        b.pop();
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
                WATER_CAPACITY_PER_CELL_MB.get(), STEAM_CAPACITY_PER_CELL_MB.get(), COOLANT_PORT_FLOW_MB_PER_TICK.get(), WATER_STEAM_PORT_FLOW_MB_PER_TICK.get(), PAIR_HEAT.get(),
                BOILING_TEMPERATURE.get(), SUPERCRITICAL_TEMPERATURE.get(), WALL_HEAT_CAPACITY.get(), WATER_SPECIFIC_HEAT.get(),
                STEAM_SPECIFIC_HEAT.get(), LATENT_HEAT.get(), SUPERCRITICAL_PRESSURE.get(), OUTPUT_MIN_PRESSURE.get(), VALVE_OPEN_PRESSURE.get(), VALVE_CLOSE_PRESSURE.get(), VALVE_FLOW_MB_PER_TICK.get(), WATER_COOLING.get(), STEAM_COOLING.get());
    }
    /** 每次读取实际SERVER配置，跨字段非法时由控制器停止交易/新收热并提示。 */
    public static BoilerAccidentState.Parameters accidentParameters() {
        return new BoilerAccidentState.Parameters(RATED_PRESSURE.get(),INSTANT_RATIO.get(),PRESSURE_TICKS.get(),RECOVERY_RATIO.get(),RECOVERY_TICKS.get(),DRY_TICKS.get(),MINIMUM_RADIUS.get(),RADIUS_PER_RATIO.get(),MAXIMUM_RADIUS.get(),BURST_DAMAGE.get());
    }
}
