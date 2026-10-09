package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** 公共 BE 的显示通知桥；不引用任何客户端类型，专用服务端不加载渲染实现。 */
public final class ReactorSurfaceSyncEvents {
    private ReactorSurfaceSyncEvents() { }

    /** 通知只在逻辑客户端发布；descriptor 可以缺失，接收者必须撤销而非保留旧显示。 */
    public static void publish(ReactorInstrumentPortBlockEntity owner, boolean removed) {
        publish(owner, removed, false);
    }

    /** 卸载与真拆除分开传递，不能把仍存在于服务端的 generation 永久退休。 */
    public static void publish(ReactorInstrumentPortBlockEntity owner, boolean removed, boolean chunkUnloaded) {
        if (owner.getLevel() != null && owner.getLevel().isClientSide)
            NeoForge.EVENT_BUS.post(new Update(owner, removed, chunkUnloaded));
    }

    /** 游戏线程事件，BE 引用仅供核实当前实例，不能放入模型快照。 */
    public static final class Update extends Event {
        private final ReactorInstrumentPortBlockEntity owner;
        private final boolean removed;
        private final boolean chunkUnloaded;
        public Update(ReactorInstrumentPortBlockEntity owner, boolean removed, boolean chunkUnloaded) {
            this.owner = owner;
            this.removed = removed;
            this.chunkUnloaded = chunkUnloaded;
        }
        public ReactorInstrumentPortBlockEntity owner() { return owner; }
        public boolean removed() { return removed; }
        public boolean chunkUnloaded() { return chunkUnloaded; }
    }
}
