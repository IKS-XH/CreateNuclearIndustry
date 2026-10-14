package com.iksxh.create_nuclear_industry.automation;

import java.util.OptionalDouble;

/** 同一服务端回合的原生整网SU样本；不保存NBT，容量0没有余量百分比，负余量保留过载含义。 */
public record ReactorStressSample(Status status,double capacitySu,double loadSu,Long networkId,long sampleTick) {
    public enum Status { VALID, NO_CAPACITY, UNAVAILABLE }
    public ReactorStressSample {
        if(status==null||sampleTick<0||!Double.isFinite(capacitySu)||!Double.isFinite(loadSu)||capacitySu<0||loadSu<0)status=Status.UNAVAILABLE;
        else if(status==Status.VALID&&capacitySu==0)status=Status.NO_CAPACITY;
        else if(status==Status.VALID&&(networkId==null||!Double.isFinite((capacitySu-loadSu)/capacitySu))
                ||status==Status.NO_CAPACITY&&capacitySu!=0)status=Status.UNAVAILABLE;
    }
    public OptionalDouble reserveFraction() {
        return status==Status.VALID?OptionalDouble.of((capacitySu-loadSu)/capacitySu):OptionalDouble.empty();
    }
}
