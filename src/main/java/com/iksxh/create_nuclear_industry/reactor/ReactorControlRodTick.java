package com.iksxh.create_nuclear_industry.reactor;

import java.util.Map;
import java.util.TreeMap;

/** Pure authoritative control-rod state adapter for one server tick. */
public final class ReactorControlRodTick {
    private ReactorControlRodTick() {
    }

    /**
     * Advances every control rod in the snapshot without calculating heat,
     * coolant, damage or meltdown. Those stages belong to P1-LOOP-01.
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
