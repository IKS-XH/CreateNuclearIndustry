package com.iksxh.create_nuclear_industry.config;

import com.iksxh.create_nuclear_industry.automation.ReactorAutomationParameters;
import com.iksxh.create_nuclear_industry.automation.ReactorLoadFollowParameters;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 独立SERVER配置，不能覆盖原反应堆热工配置；读取一次后完整校验跨项约束。 */
public final class ReactorAutomationConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue ADJUST, WINDOW, LIFT, INSERT, MAX_HEAT, RESET, LEASE, ARMING;
    private static final ModConfigSpec.DoubleValue DEADBAND, START_COLD, START_HOT, WARN_COLD, WARN_HOT, TRIP_HOT, WARN_HEAT, MAINTENANCE;
    private static final ModConfigSpec.IntValue RESERVE, MINIMUM, FILTER, ESTABLISHMENT;
    private static final ModConfigSpec.DoubleValue RESERVE_DEADBAND, KP, KI;
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
        RESERVE=b.comment("新管理员应力余量目标，百分比。").defineInRange("defaultTargetStressReservePercent",20,0,90);
        MINIMUM=b.comment("新管理员最低毛发热HU/t，默认维护1加净输出1。").defineInRange("defaultMinimumOperatingHeatHuPerTick",2,1,1000000000);
        RESERVE_DEADBAND=b.comment("应力余量比例死区。").defineInRange("stressReserveDeadbandFraction",.03,0,.2);
        FILTER=b.comment("应力余量指数滤波时间常数，tick。").defineInRange("stressFilterTimeTicks",40,1,12000);
        KP=b.comment("PI比例增益，乘本堆可达最大HU/t。").defineInRange("stressPiProportionalFraction",.25,Double.MIN_VALUE,10);
        KI=b.comment("PI积分增益，每秒乘本堆可达最大HU/t；按本批0/5/20秒时滞模型整定。").defineInRange("stressPiIntegralFractionPerSecond",.01,Double.MIN_VALUE,10);
        ESTABLISHMENT=b.comment("当前动力网建立/正常读档重建等待上限tick。").defineInRange("stressEstablishmentTimeoutTicks",1200,1,120000);
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
    /** 服务端一次读取七项闭环参数，不改原管理员/热工参数快照签名。 */
    public static ReactorLoadFollowParameters loadFollowParameters() {
        return new ReactorLoadFollowParameters(RESERVE.get(),MINIMUM.get(),RESERVE_DEADBAND.get(),FILTER.get(),KP.get(),KI.get(),ESTABLISHMENT.get());
    }
    /** 新接口出厂最低毛HU；只依赖本值和维护成本，不因未使用的控制阈值影响方块/NBT创建。 */
    public static int initialMinimumHeatHuPerTick() {
        int configured=MINIMUM.get();return configured==2?Math.max(2,(int)Math.ceil(MAINTENANCE.get()+1)):configured;
    }
}
