package com.iksxh.create_nuclear_industry.turbine.display;

import java.lang.ref.WeakReference;

/** 不引用 Minecraft 客户端类的会话时钟；世界弱引用，真实暂停只冻结活动 tick。 */
public final class TurbineRuntimeClock {
    private WeakReference<Object> session=new WeakReference<>(null);
    private long tick;
    /** 由物理客户端事件提供真实世界对象及 isPaused；切换世界重置时钟。 */
    public void advance(Object world,boolean paused) {
        if(session.get()!=world){session=new WeakReference<>(world);tick=0;}
        if(world!=null&&!paused)tick++;
    }
    /** 当前会话返回年龄基准，其他世界或已清理会话返回 -1，不能续旧租约。 */
    public long tick(Object world) {return world!=null&&session.get()==world?tick:-1;}
}
