package com.iksxh.create_nuclear_industry.reactor;

import java.util.Map;
import java.util.TreeMap;

/** 一次服务端 tick 中只推进控制棒权威状态的纯适配器。 */
public final class ReactorControlRodTick {
    private ReactorControlRodTick() {
    }

    /**
     * 推进快照中的所有控制棒，但不计算热量、冷却、损伤或融毁；这些阶段属于
     * 正式服务端循环的后续阶段。
     */
    public static ReactorSnapshot advance(ReactorSnapshot previous) {
        if (previous == null) {
            throw new IllegalArgumentException("previous reactor snapshot is required");
        }
        if (previous.controlRodColumns().isEmpty()) {
            return previous;
        }

        boolean scramActive = previous.scramActive();
        TreeMap<CoreColumnPosition, ControlRodColumnState> nextControls = new TreeMap<>();
        boolean changed = false;
        for (Map.Entry<CoreColumnPosition, ControlRodColumnState> entry
                : previous.controlRodColumns().entrySet()) {
            ControlRodColumnState next = ControlRodStateTransitions.applyServerTick(
                    entry.getValue(), scramActive);
            nextControls.put(entry.getKey(), next);
            changed |= next != entry.getValue();
        }
        return changed
                ? previous.withColumns(previous.fuelColumns(), nextControls)
                : previous;
    }
}
