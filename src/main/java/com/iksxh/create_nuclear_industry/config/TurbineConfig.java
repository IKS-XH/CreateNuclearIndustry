package com.iksxh.create_nuclear_industry.config;

import com.iksxh.create_nuclear_industry.turbine.TurbineState;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 超临界汽轮机独立 SERVER 配置；只生成数值快照，控制器负责传入 Create 实际转速上限。 */
public final class TurbineConfig {
    public static final ModConfigSpec SPEC;
    private static final TierValues SHORT, MEDIUM, LONG;
    public static final ModConfigSpec.IntValue RPM, SMOOTHING_TICKS;
    public static final ModConfigSpec.DoubleValue SU_PER_MB_PER_TICK, FRONT_SHARE;
    public static final ModConfigSpec.IntValue INLET_PORT_FLOW_MB_PER_TICK, EXHAUST_PORT_FLOW_MB_PER_TICK;

    static {
        var b = new ModConfigSpec.Builder();
        SHORT = tier(b, "short", 3, 3, 54, 4000);
        MEDIUM = tier(b, "medium", 6, 5, 108, 8000);
        LONG = tier(b, "long", 9, 7, 162, 12000);
        RPM = b.comment("汽轮机两轴固定工作转速，单位 RPM；运行时还必须不超过当前 Create 服务端上限。")
                .defineInRange("rpm", 256, 1, 65_536);
        SU_PER_MB_PER_TICK = b.comment("实际平均每 1mB/t 蒸汽流量换算的总应力容量，单位 SU/(mB/t)。")
                .defineInRange("suPerMbPerTick", 32768D, .000001D, 1_000_000D);
        SMOOTHING_TICKS = b.comment("实际处理蒸汽的滑动平均窗口，单位服务端 tick；缺失历史按零。")
                .defineInRange("smoothingTicks", 40, 1, 1200);
        INLET_PORT_FLOW_MB_PER_TICK = b.comment("每个物理进汽口每 tick 最多接收量，单位 mB/t。")
                .defineInRange("inletPortFlowMbPerTick", 256, 1, 1_000_000);
        EXHAUST_PORT_FLOW_MB_PER_TICK = b.comment("每个物理排汽口每 tick 最多排出量，单位 mB/t。主动与被动共用。")
                .defineInRange("exhaustPortFlowMbPerTick", 256, 1, 1_000_000);
        FRONT_SHARE = b.comment("已废弃且不影响动力：保留旧 frontShare 键供现有 SERVER 配置兼容读取。")
                .defineInRange("frontShare", .5D, 0D, 1D);
        SPEC = b.build();
    }

    private TurbineConfig() {}

    /** 每档五键平铺在同一 TOML；直径须属于已建模的 3/5/7，转子数须互异。 */
    private static TierValues tier(ModConfigSpec.Builder b, String prefix, int rotors, int diameter,
                                   int flow, int capacity) {
        return new TierValues(
                b.comment(prefix + " 档转子数；轴向长度由此值加两端面派生，单位 节。")
                        .defineInRange(prefix + "RotorCount", rotors, 3, 16),
                b.comment(prefix + " 档外径，只支持已建模的 3、5、7 格；不符合时整机安全停机。")
                        .defineInRange(prefix + "Diameter", diameter, 3, 7),
                b.comment(prefix + " 档额定最大耗汽量，单位 mB/t；实际处理仍受两库存空位约束。")
                        .defineInRange(prefix + "RateMbPerTick", flow, 1, 10_000),
                b.comment(prefix + " 档超临界进汽罐容量，单位 mB；降低后保留既存液量。")
                        .defineInRange(prefix + "InputCapacityMb", capacity, 1, 1_000_000),
                b.comment(prefix + " 档普通排汽罐容量，单位 mB；降低后保留既存液量。")
                        .defineInRange(prefix + "ExhaustCapacityMb", capacity, 1, 1_000_000));
    }

    /** NeoForge 默认在实例 config 生成 SERVER 文件；世界 serverconfig 同名文件可覆盖。 */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC, "create_nuclear_industry-turbine.toml");
    }

    /** 非法跨字段组合原样进入快照，由纯账本 valid(createMaxRpm) 安全拒绝。 */
    public static TurbineState.Settings settings() {
        return new TurbineState.Settings(SHORT.snapshot(), MEDIUM.snapshot(), LONG.snapshot(), RPM.get(),
                SU_PER_MB_PER_TICK.get(), SMOOTHING_TICKS.get(), INLET_PORT_FLOW_MB_PER_TICK.get(),
                EXHAUST_PORT_FLOW_MB_PER_TICK.get(), FRONT_SHARE.get());
    }

    private record TierValues(ModConfigSpec.IntValue rotors, ModConfigSpec.IntValue diameter,
                              ModConfigSpec.IntValue rate,
                              ModConfigSpec.IntValue inputCapacity, ModConfigSpec.IntValue exhaustCapacity) {
        TurbineState.Tier snapshot() {
            return new TurbineState.Tier(rotors.get(), rate.get(), inputCapacity.get(), exhaustCapacity.get(),
                    diameter.get());
        }
    }
}
