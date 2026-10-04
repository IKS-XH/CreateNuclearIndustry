package com.iksxh.create_nuclear_industry.boiler;

/**
 * 锅炉对单个 Create 管连接的自有压力份额；单位为 Create 内部压力值。
 * 服务端以控制器位置区分来源，原生泵的压力由 Create 自己保留。
 */
public interface BoilerPressureConnection {
    void createNuclearIndustry$setBoilerPressure(long owner, boolean inbound, float pressure);
}
