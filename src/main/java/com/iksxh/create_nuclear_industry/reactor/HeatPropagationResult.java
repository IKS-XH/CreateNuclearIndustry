package com.iksxh.create_nuclear_industry.reactor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Immutable result of one snapshot-based four-way heat propagation pass. */
public record HeatPropagationResult(
        ReactorSnapshot snapshot,
        Map<CoreColumnPosition, Double> receivedHeatHu,
        Map<CoreColumnPosition, Double> removedHeatHu,
        Map<CoreColumnPosition, Double> netReceivedHeatHu,
        Set<CoreColumnPosition> coveredEffectiveFuelColumns,
        double totalTransferredHeatHu
) {
    public HeatPropagationResult {
        if (snapshot == null || receivedHeatHu == null || removedHeatHu == null
                || netReceivedHeatHu == null || coveredEffectiveFuelColumns == null) {
            throw new IllegalArgumentException("propagation result fields are required");
        }
        receivedHeatHu = immutableMap(receivedHeatHu);
        removedHeatHu = immutableMap(removedHeatHu);
        netReceivedHeatHu = immutableMap(netReceivedHeatHu);
        coveredEffectiveFuelColumns = Collections.unmodifiableSet(new TreeSet<>(coveredEffectiveFuelColumns));
        if (!Double.isFinite(totalTransferredHeatHu) || totalTransferredHeatHu < 0.0D) {
            throw new IllegalArgumentException("total transferred heat must be finite and non-negative");
        }
    }

    private static Map<CoreColumnPosition, Double> immutableMap(Map<CoreColumnPosition, Double> source) {
        return Collections.unmodifiableMap(new TreeMap<>(source));
    }
}
