package com.iksxh.create_nuclear_industry.automation;

/** 单次服务端配置的不可变参数，热量单位HU/t，比例为0..1，时长单位tick。非法跨项配置拒绝提棒。 */
public record ReactorAutomationParameters(int adjustIntervalTicks, int displayAverageWindowTicks,
        int maxLiftPercentPerAdjustment, int maxInsertPercentPerAdjustment, double powerDeadbandFraction,
        int maxTargetHeatHuPerTick, double startupColdFraction, double startupMaxHotFraction,
        double warningColdFraction, double warningHotFraction, double tripHotFraction,
        double warningHeatThresholdFraction, int resetStableTicks, int controlLeaseTicks, int armingTimeoutTicks,
        double burnerMaintenanceHuPerTick) {
    public ReactorAutomationParameters {
        if (!Double.isFinite(burnerMaintenanceHuPerTick) || burnerMaintenanceHuPerTick<=0 || burnerMaintenanceHuPerTick>1000000)
            throw new IllegalArgumentException("管理员维护成本必须为正有限HU/t");
        if (adjustIntervalTicks <= 0 || displayAverageWindowTicks <= 0 || resetStableTicks <= 0
                || controlLeaseTicks <= 0 || armingTimeoutTicks <= 0 || maxLiftPercentPerAdjustment <= 0
                || maxInsertPercentPerAdjustment <= 0 || maxLiftPercentPerAdjustment > 100
                || maxInsertPercentPerAdjustment > 100 || maxTargetHeatHuPerTick < 0)
            throw new IllegalArgumentException("管理员周期、步长和目标范围非法");
        for (double f : new double[]{powerDeadbandFraction, startupColdFraction, startupMaxHotFraction,
                warningColdFraction, warningHotFraction, tripHotFraction, warningHeatThresholdFraction})
            if (!Double.isFinite(f) || f < 0 || f > 1) throw new IllegalArgumentException("管理员比例非法");
        if (warningColdFraction >= startupColdFraction || startupMaxHotFraction >= warningHotFraction
                || warningHotFraction >= tripHotFraction || warningHeatThresholdFraction <= 0)
            throw new IllegalArgumentException("管理员比例顺序非法");
    }
    /** 原控制参数夹具采用已批准默认维护费，不改变原保护字段含义。 */
    public ReactorAutomationParameters(int adjust,int window,int lift,int insert,double deadband,int maxHeat,
            double cold,double hot,double warnCold,double warnHot,double tripHot,double warnHeat,int reset,int lease,int arming) {
        this(adjust,window,lift,insert,deadband,maxHeat,cold,hot,warnCold,warnHot,tripHot,warnHeat,reset,lease,arming,1);
    }
    public static ReactorAutomationParameters defaults() {
        return new ReactorAutomationParameters(10,40,1,5,.02,1000000,.25,.75,.15,.85,.95,.5,100,20,40);
    }
}
