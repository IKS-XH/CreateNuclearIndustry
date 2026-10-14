package com.iksxh.create_nuclear_industry.config;

import com.iksxh.create_nuclear_industry.automation.ReactorAutomationParameters;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 独立SERVER配置，不能覆盖原反应堆热工配置；读取一次后完整校验跨项约束。 */
public final class ReactorAutomationConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue ADJUST, WINDOW, LIFT, INSERT, MAX_HEAT, RESET, LEASE, ARMING;
    private static final ModConfigSpec.DoubleValue DEADBAND, START_COLD, START_HOT, WARN_COLD, WARN_HOT, TRIP_HOT, WARN_HEAT, MAINTENANCE;
    static {
        var b=new ModConfigSpec.Builder();
        ADJUST=b.comment("正常调节周期tick。").defineInRange("adjustIntervalTicks",10,1,1200);
        WINDOW=b.comment("仅显示平均窗口tick。").defineInRange("displayAverageWindowTicks",40,1,1200);
        LIFT=b.comment("每次提出百分点。").defineInRange("maxLiftPercentPerAdjustment",1,1,100);
        INSERT=b.comment("每次插入百分点。").defineInRange("maxInsertPercentPerAdjustment",5,1,100);
        DEADBAND=b.comment("目标功率死区比例。").defineInRange("powerDeadbandFraction",.02,0,1);
        MAX_HEAT=b.comment("管理员GUI最大目标HU/t，不定义物理上限。").defineInRange("maxTargetHeatHuPerTick",1000000,0,1000000000);
        START_COLD=b.defineInRange("startupColdFraction",.25,0,1);
        START_HOT=b.defineInRange("startupMaxHotFraction",.75,0,1);
        WARN_COLD=b.defineInRange("warningColdFraction",.15,0,1);
        WARN_HOT=b.defineInRange("warningHotFraction",.85,0,1);
        TRIP_HOT=b.defineInRange("tripHotFraction",.95,0,1);
        WARN_HEAT=b.defineInRange("warningHeatThresholdFraction",.5,0,1);
        RESET=b.defineInRange("resetStableTicks",100,1,12000);
        LEASE=b.defineInRange("controlLeaseTicks",20,1,1200);
        ARMING=b.defineInRange("armingTimeoutTicks",40,1,1200);
        MAINTENANCE=b.comment("核热维持普通燃烧的真实维护成本，单位HU/t；必须为正有限数。").defineInRange("burnerMaintenanceHuPerTick",1.0,Double.MIN_VALUE,1000000.0);
        SPEC=b.build();
    }
    private ReactorAutomationConfig() {}
    public static ReactorAutomationParameters parameters() {
        return new ReactorAutomationParameters(ADJUST.get(),WINDOW.get(),LIFT.get(),INSERT.get(),DEADBAND.get(),
                MAX_HEAT.get(),START_COLD.get(),START_HOT.get(),WARN_COLD.get(),WARN_HOT.get(),TRIP_HOT.get(),
                WARN_HEAT.get(),RESET.get(),LEASE.get(),ARMING.get(),MAINTENANCE.get());
    }
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER,SPEC,"create_nuclear_industry-reactor-automation-server.toml");
    }
}
