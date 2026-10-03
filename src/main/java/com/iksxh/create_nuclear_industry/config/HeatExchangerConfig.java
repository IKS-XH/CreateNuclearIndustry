package com.iksxh.create_nuclear_industry.config;

import com.iksxh.create_nuclear_industry.heat.HeatExchangerState;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 核换热器独立服务端配置；只读取既有 P1 工质密度，不改反应堆公式或配置文件。 */
public final class HeatExchangerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue HEAT_LEVEL;
    public static final ModConfigSpec.DoubleValue HU_PER_LEVEL;
    public static final ModConfigSpec.IntValue BUFFER_TICKS;
    static {
        var b = new ModConfigSpec.Builder();
        HEAT_LEVEL = b.comment("Create 整数锅炉热等级；18 等效 9 个超级燃烧室。").defineInRange("heatLevel", 18, 1, 18);
        HU_PER_LEVEL = b.comment("每个热等级每 tick 支付 HU。").defineInRange("huPerLevel", 1D, .000001D, 1_000_000D);
        BUFFER_TICKS = b.comment("预热储备及余热上限，单位 tick。").defineInRange("bufferTicks", 40, 1, 1200);
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
                P1ServerConfig.VALUES.coolantAbsorptionHuPerMb.get());
    }
}
