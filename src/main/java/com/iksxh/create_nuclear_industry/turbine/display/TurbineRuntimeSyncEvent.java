package com.iksxh.create_nuclear_industry.turbine.display;

import com.iksxh.create_nuclear_industry.turbine.TurbineRotorBlockEntity;
import net.neoforged.bus.api.Event;

/** common BE 到物理客户端订阅者的只读通知；专服没有客户端类引用或处理器。 */
public final class TurbineRuntimeSyncEvent extends Event {
    private final TurbineRotorBlockEntity rotor;
    public TurbineRuntimeSyncEvent(TurbineRotorBlockEntity rotor){this.rotor=rotor;}
    public TurbineRotorBlockEntity rotor(){return rotor;}
}
