package com.iksxh.create_nuclear_industry.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * P1 反应堆的服务端配置契约。
 *
 * <p>这里集中保存会影响服务端模拟的数值；客户端不应把这些值当作本地显示状态。
 * 字段名中的 {@code HU}、{@code HU/t} 和 {@code mB/t} 分别表示热量、每 tick 热负荷
 * 和每 tick 的流体量，完整度与比例参数保持在 {@code [0,1]}。</p>
 */
public final class P1ServerConfig {
    public static final ModConfigSpec SPEC;
    public static final Values VALUES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        VALUES = new Values(builder);
        SPEC = builder.build();
    }

    private P1ServerConfig() {
    }

    /** 在模组容器初始化阶段注册服务端配置，使世界规则由服务器端配置拥有权威值。 */
    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    /** 配置值的稳定访问入口；构造过程只负责建立 NeoForge 配置规格。 */
    public static final class Values {
        public final ModConfigSpec.DoubleValue baseHeatPerFuelBlockHuPerTick;
        public final ModConfigSpec.DoubleValue fuelBurnTimeHours;
        public final ModConfigSpec.DoubleValue coolantAbsorptionHuPerMb;
        public final ModConfigSpec.IntValue perPortFlowMbPerTick;
        public final ModConfigSpec.IntValue coldInventoryCapacityMb;
        public final ModConfigSpec.IntValue hotInventoryCapacityMb;
        public final ModConfigSpec.DoubleValue damageHeatThresholdHuPerTick;
        public final ModConfigSpec.DoubleValue damageRatePerTickHuLoad;
        public final ModConfigSpec.DoubleValue damageTransferRate;
        public final ModConfigSpec.DoubleValue controlRodFailureThreshold;
        public final ModConfigSpec.DoubleValue meltdownTriggerFraction;
        public final ModConfigSpec.IntValue meltdownCountdownTicks;
        public final ModConfigSpec.DoubleValue controlResponseExponent;
        public final ModConfigSpec.DoubleValue overclockHeatMultiplier;
        public final ModConfigSpec.DoubleValue overclockBurnMultiplier;
        public final ModConfigSpec.DoubleValue overclockFeedbackGain;
        public final ModConfigSpec.DoubleValue overclockFeedbackExponent;
        public final ModConfigSpec.DoubleValue totalHeatMultiplierCap;

        private Values(ModConfigSpec.Builder builder) {
            builder.push("reactor");
            baseHeatPerFuelBlockHuPerTick = builder
                    .comment("每个燃料方块每 tick 产生的基础热量。")
                    .defineInRange("baseHeatPerFuelBlockHuPerTick", 1.0D, 0.0D, 1_000_000.0D);
            fuelBurnTimeHours = builder
                    .comment("一根新燃料组件的燃烧时长，单位为小时。")
                    .defineInRange("fuelBurnTimeHours", 3.0D, 0.001D, 1_000_000.0D);
            coolantAbsorptionHuPerMb = builder
                    .comment("一毫桶复合冷却剂吸收的热量。")
                    .defineInRange("coolantAbsorptionHuPerMb", 0.5D, 0.0D, 1_000_000.0D);
            perPortFlowMbPerTick = builder
                    .comment("单个端口每 tick 的最大冷却剂流量。")
                    .defineInRange("perPortFlowMbPerTick", 128, 0, 1_000_000);
            coldInventoryCapacityMb = builder
                    .comment("共享内部冷端冷却剂缓冲容量，单位为毫桶。")
                    .defineInRange("coldInventoryCapacityMb", 1_000, 0, 1_000_000_000);
            hotInventoryCapacityMb = builder
                    .comment("共享内部热端冷却剂缓冲容量，单位为毫桶。")
                    .defineInRange("hotInventoryCapacityMb", 1_000, 0, 1_000_000_000);
            damageHeatThresholdHuPerTick = builder
                    .comment("开始造成完整度损伤前的有效热负荷阈值。")
                    .defineInRange("damageHeatThresholdHuPerTick", 0.25D, 0.0D, 1_000_000.0D);
            damageRatePerTickHuLoad = builder
                    .comment("超过阈值的每 HU/t 有效热负荷每 tick 造成的完整度损伤。")
                    .defineInRange("damageRatePerTickHuLoad", 0.0000005D, 0.0D, 1_000_000.0D);
            damageTransferRate = builder
                    .comment("四向热损伤传播系数。")
                    .defineInRange("damageTransferRate", 0.25D, 0.0D, 1.0D);
            controlRodFailureThreshold = builder
                    .comment("控制棒完整度低于该阈值时进入卡死状态。")
                    .defineInRange("controlRodFailureThreshold", 0.0D, 0.0D, 1.0D);
            meltdownTriggerFraction = builder
                    .comment("受到有效热传播的燃料列比例达到该值时启动熔毁倒计时。")
                    .defineInRange("meltdownTriggerFraction", 0.20D, 0.0D, 1.0D);
            meltdownCountdownTicks = builder
                    .comment("熔毁倒计时持续的服务端 tick 数。")
                    .defineInRange("meltdownCountdownTicks", 900, 0, 20_000_000);
            controlResponseExponent = builder
                    .comment("控制棒实际插入深度对邻接燃料反应强度的响应指数。")
                    .defineInRange("controlResponseExponent", 1.0D, 0.001D, 1_000_000.0D);
            overclockHeatMultiplier = builder
                    .comment("邻接燃料触发超频时的热量倍率上限。")
                    .defineInRange("overclockHeatMultiplier", 10.0D, 1.0D, 1_000_000.0D);
            overclockBurnMultiplier = builder
                    .comment("邻接燃料触发超频时的燃耗倍率上限。")
                    .defineInRange("overclockBurnMultiplier", 10.0D, 1.0D, 1_000_000.0D);
            overclockFeedbackGain = builder
                    .comment("超频邻接反馈信号增益。")
                    .defineInRange("overclockFeedbackGain", 0.15D, 0.0D, 1_000_000.0D);
            overclockFeedbackExponent = builder
                    .comment("超频邻接反馈信号指数。")
                    .defineInRange("overclockFeedbackExponent", 0.5D, 0.001D, 1_000_000.0D);
            totalHeatMultiplierCap = builder
                    .comment("单 tick 全堆热量倍率上限。")
                    .defineInRange("totalHeatMultiplierCap", 20.0D, 0.001D, 1_000_000.0D);
            builder.pop();
        }
    }
}
