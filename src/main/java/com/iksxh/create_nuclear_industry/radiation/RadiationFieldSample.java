package com.iksxh.create_nuclear_industry.radiation;
/** 权威场强样本，D/s；未完成样本只可显示等待，不能推进吸收或恢复。 */
public record RadiationFieldSample(double fieldDosePerSecond,boolean complete,long gameTime) {}
