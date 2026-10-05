package com.iksxh.create_nuclear_industry.config;

import com.iksxh.create_nuclear_industry.boiler.BoilerState;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 高压锅炉独立服务端规格；只生成数值快照，不在客户端决定真实库存或热交易。 */
public final class BoilerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue WATER_CAPACITY_MB, STEAM_CAPACITY_MB, PORT_FLOW_MB_PER_TICK;
    public static final ModConfigSpec.DoubleValue SECTION_HEAT_HU_PER_TICK, STEAM_HU_PER_MB;
    public static final ModConfigSpec.DoubleValue WARM_HU_PER_SECTION, COOLING_HU_PER_SECTION_PER_TICK;
    public static final ModConfigSpec.DoubleValue REHEAT_FRACTION, VALVE_OPEN_FRACTION, VALVE_CLOSE_FRACTION;
    public static final ModConfigSpec.IntValue VALVE_FLOW_MB_PER_TICK;
    static {
        var b = new ModConfigSpec.Builder();
        WATER_CAPACITY_MB = b.comment("水罐容量，单位 mB；降低容量不删除既存水。")
                .defineInRange("waterCapacityMb", 16000, 1, 1_000_000);
        STEAM_CAPACITY_MB = b.comment("超临界汽罐容量，单位 mB；降低容量不删除既存汽。")
                .defineInRange("steamCapacityMb", 16000, 1, 1_000_000);
        PORT_FLOW_MB_PER_TICK = b.comment("每个物理水口及汽口流量上限，单位 mB/t。")
                .defineInRange("portFlowMbPerTick", 256, 1, 1_000_000);
        SECTION_HEAT_HU_PER_TICK = b.comment("每个热段每 tick 最多接收的热，单位 HU/t。")
                .defineInRange("sectionHeatHuPerTick", 18D, 0.000001D, 1_000_000D);
        STEAM_HU_PER_MB = b.comment("生产 1 mB 超临界汽消耗的热，单位 HU/mB。")
                .defineInRange("steamHuPerMb", 1D, 0.000001D, 1_000_000D);
        WARM_HU_PER_SECTION = b.comment("每段暖炉所需热量，单位 HU。")
                .defineInRange("warmHuPerSection", 3600D, 0.000001D, 1_000_000_000D);
        COOLING_HU_PER_SECTION_PER_TICK = b.comment("未收热时每段每 tick 散热，单位 HU/t。")
                .defineInRange("coolingHuPerSectionPerTick", 0.9D, 0D, 1_000_000D);
        REHEAT_FRACTION = b.comment("暖炉热量低于此比例时重新预热，范围 0 到 1。")
                .defineInRange("reheatFraction", 0.25D, 0D, 1D);
        VALVE_OPEN_FRACTION = b.comment("汽罐达到此容量比例时开启安全阀，必须高于关闭比例。")
                .defineInRange("valveOpenFraction", 0.9D, 0D, 1D);
        VALVE_CLOSE_FRACTION = b.comment("汽罐降到此容量比例时关闭安全阀，必须低于开启比例。")
                .defineInRange("valveCloseFraction", 0.8D, 0D, 1D);
        VALVE_FLOW_MB_PER_TICK = b.comment("安全阀每 tick 最多排汽量，单位 mB/t。")
                .defineInRange("valveFlowMbPerTick", 256, 1, 1_000_000);
        SPEC = b.build();
    }
    private BoilerConfig() {}

    /** NeoForge 默认在实例 config 生成 SERVER 文件；世界 serverconfig 同名文件可覆盖。 */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC, "create_nuclear_industry-boiler.toml");
    }

    /** 跨字段错误保留为无效快照，由账本拒绝运行并由控制器报告状态。 */
    public static BoilerState.Settings settings() {
        return new BoilerState.Settings(WATER_CAPACITY_MB.get(), STEAM_CAPACITY_MB.get(),
                PORT_FLOW_MB_PER_TICK.get(), SECTION_HEAT_HU_PER_TICK.get(), STEAM_HU_PER_MB.get(),
                WARM_HU_PER_SECTION.get(), COOLING_HU_PER_SECTION_PER_TICK.get(), REHEAT_FRACTION.get(),
                VALVE_OPEN_FRACTION.get(), VALVE_CLOSE_FRACTION.get(), VALVE_FLOW_MB_PER_TICK.get());
    }
}
