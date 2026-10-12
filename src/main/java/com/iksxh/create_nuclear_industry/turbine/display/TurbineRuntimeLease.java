package com.iksxh.create_nuclear_industry.turbine.display;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** 单成员客户端租约；只接受权威身份匹配的新完整样本，重复包不续期，撤销不抹去排序水位。 */
public final class TurbineRuntimeLease {
    private TurbineRuntimeSample latest;
    private long receivedTick;
    private boolean active;
    private final Set<UUID> retired=new HashSet<>();
    /** 输入 now 为活动客户端 tick；身份来自当前真实控制器/转子实例，绝非邻近搜索。 */
    public boolean receive(TurbineRuntimeSample next,long now,UUID owner,int epoch,UUID member) {
        if(next==null || now<0 || !next.generation().equals(owner) || next.epoch()!=epoch || !next.member().equals(member)
                || retired.contains(next.generation())) {revoke();return false;}
        if(latest!=null) {
            if(latest.generation().equals(next.generation())) {
                if(next.epoch()<latest.epoch() || next.serverTick()<latest.serverTick()) {revoke();return false;}
                if(next.serverTick()==latest.serverTick()) {
                    // 同tick仅逐字段一致的整包才是重复包；新世代、成员、几何或RPM差异全部撤销。
                    if(!next.equals(latest))revoke();
                    return false;
                }
                if(next.epoch()==latest.epoch() && !next.member().equals(latest.member())) {revoke();return false;}
            } else retired.add(latest.generation());
        }
        latest=next;receivedTick=now;active=true;return true;
    }
    /** 20 个活动 tick 后停止显示；0 RPM 仍返回可靠样本，渲染器必须据此停转。 */
    public Optional<TurbineRuntimeSample> current(long now) {
        if(!active || now<receivedTick || now-receivedTick>=20) {active=false;return Optional.empty();}
        return Optional.of(latest);
    }
    /** 坏包、拆件、缺块和世界结束立即撤销；后续必须有更晚的完整样本才能恢复。 */
    public void revoke() {active=false;}
}
