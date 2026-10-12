package com.iksxh.create_nuclear_industry.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 专用网络 SERVER 配置，速率单位 mB/t，动力基准 RPM，应力单位 SU/RPM。 */
public final class IndustrialFluidConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue REINFORCED_FLOW, CERAMIC_FLOW, PUMP_FLOW, RATED_RPM, SEARCH_DISTANCE, MAX_NODES;
    public static final ModConfigSpec.DoubleValue STRESS;
    static {
        var b = new ModConfigSpec.Builder();
        REINFORCED_FLOW = b.comment("二级管及阀额定流量 mB/t。").defineInRange("reinforcedFlowMbPerTick", 8192, 1, 1000000);
        CERAMIC_FLOW = b.comment("三级管及阀额定流量 mB/t。").defineInRange("ceramicFlowMbPerTick", 32768, 1, 1000000);
        PUMP_FLOW = b.comment("唯一专用泵额定流量及最大周转量 mB。").defineInRange("pumpFlowMbPerTick", 16384, 1, 1000000);
        RATED_RPM = b.comment("额定流量对应 RPM，超速流量封顶。").defineInRange("ratedRpm", 256, 1, 65536);
        STRESS = b.comment("泵应力 SU/RPM，默认256RPM需要4096SU。").defineInRange("stressSuPerRpm", 16, .000001, 1000000);
        SEARCH_DISTANCE = b.comment("每侧最多搜寻的专用管段数，不强载区块。").defineInRange("searchDistance", 256, 1, 4096);
        MAX_NODES = b.comment("一次连接拓扑最多节点，超出停止交易。").defineInRange("maxNetworkNodes", 4096, 1, 65536);
        SPEC = b.build();
    }
    private IndustrialFluidConfig() {}
    /** 跨项与非有限数保护；非法运行快照只停止交易，不改已有缓存。 */
    public static boolean valid() {
        return REINFORCED_FLOW.get() > 0 && REINFORCED_FLOW.get() <= 1000000 && CERAMIC_FLOW.get() > 0 && CERAMIC_FLOW.get() <= 1000000
                && PUMP_FLOW.get() > 0 && PUMP_FLOW.get() <= 1000000 && RATED_RPM.get() > 0 && RATED_RPM.get() <= 65536
                && SEARCH_DISTANCE.get() > 0 && SEARCH_DISTANCE.get() <= 4096 && MAX_NODES.get() > 0 && MAX_NODES.get() <= 65536
                && Double.isFinite(STRESS.get()) && STRESS.get() > 0 && STRESS.get() <= 1000000;
    }
    /** 配置重载会影响所有工业拓扑；用完整数值快照比较，避免哈希碰撞留下旧搜寻边界。 */
    public static java.util.List<Number> snapshot() { return java.util.List.of(REINFORCED_FLOW.get(), CERAMIC_FLOW.get(), PUMP_FLOW.get(), RATED_RPM.get(), SEARCH_DISTANCE.get(), MAX_NODES.get(), STRESS.get()); }
    public static void register(ModContainer container) { container.registerConfig(ModConfig.Type.SERVER, SPEC, "create_nuclear_industry-industrial-fluid.toml"); }
}
