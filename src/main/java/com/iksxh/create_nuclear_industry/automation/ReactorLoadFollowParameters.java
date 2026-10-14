package com.iksxh.create_nuclear_industry.automation;

/** 服务端单次读取的闭环参数；HU为毛热量每tick，滤波/等待为tick，Ki为每秒，无世界或持久化状态。 */
public record ReactorLoadFollowParameters(int defaultTargetStressReservePercent,
        int defaultMinimumOperatingHeatHuPerTick,double stressReserveDeadbandFraction,
        int stressFilterTimeTicks,double stressPiProportionalFraction,
        double stressPiIntegralFractionPerSecond,int stressEstablishmentTimeoutTicks) {
    public ReactorLoadFollowParameters {
        if(defaultTargetStressReservePercent<0||defaultTargetStressReservePercent>90||defaultMinimumOperatingHeatHuPerTick<=0
                ||!Double.isFinite(stressReserveDeadbandFraction)||stressReserveDeadbandFraction<0||stressReserveDeadbandFraction>.2
                ||stressFilterTimeTicks<=0||!Double.isFinite(stressPiProportionalFraction)||stressPiProportionalFraction<=0
                ||!Double.isFinite(stressPiIntegralFractionPerSecond)||stressPiIntegralFractionPerSecond<=0||stressEstablishmentTimeoutTicks<=0)
            throw new IllegalArgumentException("应力闭环参数非法");
    }
}
