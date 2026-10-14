package com.iksxh.create_nuclear_industry.automation;

import java.util.UUID;

/** 单次当前实例闭环：样本、积分、滤波、表实例和网络权永不保存。加载等待不重新预热。 */
public final class ReactorLoadFollowSession {
    private final Object gauge;
    private final UUID generation;
    private final boolean loading;
    private ReactorStressPiController.State pi=ReactorStressPiController.State.empty();
    private long firstTime=Long.MIN_VALUE,lastTime=Long.MIN_VALUE;
    private Long network;
    private boolean established;
    private ReactorAutomationPortBlockEntity.Settings settings;
    private double maximum=-1;
    private ReactorStressSample lastSample;
    public ReactorLoadFollowSession(Object gauge,UUID generation,boolean loading) {this.gauge=gauge;this.generation=generation;this.loading=loading;}
    /** 返回非空即须保护停堆；只允许从未建立有效容量的新会话有限等待。 */
    public String acceptSample(Object current,ReactorStressSample sample,int timeout) {
        if(current==null||current!=gauge)return "gauge_changed";
        if(sample==null||sample.status()==ReactorStressSample.Status.UNAVAILABLE)return "stress_sample_lost";
        long time=sample.sampleTick();
        if(time<0||lastTime!=Long.MIN_VALUE&&time<lastTime)return "stress_sample_lost";
        if(firstTime==Long.MIN_VALUE)firstTime=time;lastTime=time;lastSample=sample;
        // 已知当前网身份独立于容量建立；C0也不能退回“未知网”或迁到另一ID继续等待。
        if(network!=null&&!network.equals(sample.networkId()))return "stress_network_changed";
        if(network==null&&sample.networkId()!=null)network=sample.networkId();
        if(established) {
            if(sample.status()!=ReactorStressSample.Status.VALID)return "stress_capacity_lost";
            return "";
        }
        if(sample.status()==ReactorStressSample.Status.NO_CAPACITY)return time-firstTime>=timeout?"stress_timeout":"";
        established=true;return "";
    }
    public boolean loading() {return loading;}
    public boolean established() {return established;}
    public ReactorStressPiController.State pi() {return pi;}
    public UUID generation() {return generation;}
    public Long network() {return network;}
    public ReactorStressSample lastSample() {return lastSample;}
    /** 偏好或当前曲线上限变化时只重建PI，不释放当前表/网络已建立的身份检查。 */
    public void configure(ReactorAutomationPortBlockEntity.Settings next,double max) {
        if(!next.equals(settings)||Double.compare(max,maximum)!=0) {pi=ReactorStressPiController.State.empty();settings=next;maximum=max;}
    }
    public void applied(ReactorStressPiController.State next) {pi=next;}
}
