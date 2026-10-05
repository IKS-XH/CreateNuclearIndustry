package com.iksxh.create_nuclear_industry.config;

import com.iksxh.create_nuclear_industry.heat.HeatExchangerState;
import com.iksxh.create_nuclear_industry.heat.CondensationState;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 核换热器独立服务端配置；只读取既有 P1 工质密度，不改反应堆公式或配置文件。 */
public final class HeatExchangerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue HEAT_LEVEL;
    public static final ModConfigSpec.DoubleValue HU_PER_LEVEL;
    public static final ModConfigSpec.IntValue BUFFER_TICKS;
    public static final ModConfigSpec.IntValue HOT_CAPACITY_MB, COLD_CAPACITY_MB, MAX_LINE_LENGTH;
    public static final ModConfigSpec.IntValue CONDENSATION_RATE, CONDENSATION_RECOVERY,
            CONDENSATION_STEAM_CAPACITY, CONDENSATION_WATER_CAPACITY,
            CONDENSATION_SNOW, CONDENSATION_ICE, CONDENSATION_PACKED_ICE, CONDENSATION_WATER;
    static {
        var b = new ModConfigSpec.Builder();
        HEAT_LEVEL = b.comment("Create 整数锅炉热等级；18 等效 9 个超级燃烧室。").defineInRange("heatLevel", 18, 1, 18);
        HU_PER_LEVEL = b.comment("每个热等级每 tick 支付 HU。").defineInRange("huPerLevel", 1D, .000001D, 1_000_000D);
        BUFFER_TICKS = b.comment("预热储备及余热上限，单位 tick。").defineInRange("bufferTicks", 40, 1, 1200);
        HOT_CAPACITY_MB = b.comment("每台热液罐容量，单位 mB；下降时保留既存液量。")
                .defineInRange("hotCapacityMb", 4000, 1, 1_000_000);
        COLD_CAPACITY_MB = b.comment("每台冷液罐容量，单位 mB；下降时保留既存液量。")
                .defineInRange("coldCapacityMb", 4000, 1, 1_000_000);
        MAX_LINE_LENGTH = b.comment("水平直列最多允许的换热器台数，单位 台。")
                .defineInRange("maxLineLength", 16, 1, 64);
        CONDENSATION_RATE = b.comment("每台最大冷凝输入量，单位 mB/t。")
                .defineInRange("condensationRateMbPerTick", 54, 1, 1000000);
        CONDENSATION_RECOVERY = b.comment("冷凝回水率，千分比；1000为1:1。")
                .defineInRange("condensationRecoveryPermille", 1000, 1, 1000);
        CONDENSATION_STEAM_CAPACITY = b.comment("每台蒸汽罐容量，单位mB；缩容保留存量。")
                .defineInRange("condensationSteamCapacityMb", 4000, 1, 1000000);
        CONDENSATION_WATER_CAPACITY = b.comment("每台回水罐容量，单位mB；缩容保留存量。")
                .defineInRange("condensationWaterCapacityMb", 4000, 1, 1000000);
        CONDENSATION_SNOW = b.comment("顶格雪块融化前实际处理蒸汽量，单位mB。")
                .defineInRange("condensationSnowMeltAfterMb", 100000, 1, Integer.MAX_VALUE);
        CONDENSATION_ICE = b.comment("顶格冰融化前实际处理蒸汽量，单位mB。")
                .defineInRange("condensationIceMeltAfterMb", 100000, 1, Integer.MAX_VALUE);
        CONDENSATION_PACKED_ICE = b.comment("顶格浮冰融化前实际处理蒸汽量，单位mB。")
                .defineInRange("condensationPackedIceMeltAfterMb", 900000, 1, Integer.MAX_VALUE);
        CONDENSATION_WATER = b.comment("顶格水源蒸发前实际处理蒸汽量，单位mB。")
                .defineInRange("condensationWaterEvaporateAfterMb", 100000, 1, Integer.MAX_VALUE);
        SPEC = b.build();
    }
    private HeatExchangerConfig() {}
    /** 独立文件避免覆盖既有 SERVER 规格。 */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC, "create_nuclear_industry-heat-exchanger.toml");
    }
    /** 仅服务端 tick 读取；坏配置仍由账本二次防御，NaN 和零密度不运行。 */
    public static HeatExchangerState.Settings settings() {
        return new HeatExchangerState.Settings(HEAT_LEVEL.get(), HU_PER_LEVEL.get(), BUFFER_TICKS.get(),
                P1ServerConfig.VALUES.coolantAbsorptionHuPerMb.get(), HOT_CAPACITY_MB.get(),
                COLD_CAPACITY_MB.get(), MAX_LINE_LENGTH.get());
    }
    /** 冷凝与核热参数独立，不复用工质密度或HU窗口解释蒸汽。 */
    public static CondensationState.Settings condensationSettings() {
        return new CondensationState.Settings(CONDENSATION_RATE.get(), CONDENSATION_RECOVERY.get(),
                CONDENSATION_STEAM_CAPACITY.get(), CONDENSATION_WATER_CAPACITY.get(),
                CONDENSATION_SNOW.get(), CONDENSATION_ICE.get(), CONDENSATION_PACKED_ICE.get(), CONDENSATION_WATER.get());
    }
}
