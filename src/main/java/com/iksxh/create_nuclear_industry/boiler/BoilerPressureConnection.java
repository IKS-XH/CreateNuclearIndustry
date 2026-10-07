package com.iksxh.create_nuclear_industry.boiler;

/**
 * 锅炉对单个 Create 管连接的自有压力份额；单位为 Create 内部压力值。
 * 服务端以控制器位置区分来源，原生泵的压力由 Create 自己保留。
 */
public interface BoilerPressureConnection {
    void createNuclearIndustry$setBoilerPressure(long owner, boolean inbound, float pressure);

    /** 服务端控制器tick遗忘汽口首段连接的传输网络；不清流体、压力或外部库存，禁止在drain交易中调用。 */
    void createNuclearIndustry$forgetSteamEndpointNetwork();
}
