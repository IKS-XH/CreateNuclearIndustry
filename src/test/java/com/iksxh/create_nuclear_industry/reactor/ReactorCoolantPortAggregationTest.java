package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReactorCoolantPortAggregationTest {
    private static final double EPSILON = 1.0E-12D;

    @Test
    void oneColdAndOneHotPortProvideOneHundredTwentyEightMbPerTick() {
        ReactorCoolantLedger.PortSummary summary = ReactorCoolantLedger.summarizePorts(List.of(
                ReactorCoolantLedger.Port.cold("cold-a", 128.0D),
                ReactorCoolantLedger.Port.hot("hot-a", 128.0D)
        ));

        assertEquals(128.0D, summary.coldInputMb(), EPSILON);
        assertEquals(128.0D, summary.hotOutputCapacityMb(), EPSILON);
        assertEquals(128.0D, summary.coldFlowCapacityMb(), EPSILON);
        assertEquals(128.0D, summary.hotFlowCapacityMb(), EPSILON);
        assertEquals(128.0D, summary.balancedFlowMb(), EPSILON);
    }

    @Test
    void twoAndThreeDistinctPortsScaleWithoutAnAggregateCap() {
        ReactorCoolantLedger.PortSummary two = ReactorCoolantLedger.summarizePorts(List.of(
                ReactorCoolantLedger.Port.cold("cold-a", 128.0D),
                ReactorCoolantLedger.Port.cold("cold-b", 128.0D),
                ReactorCoolantLedger.Port.hot("hot-a", 128.0D),
                ReactorCoolantLedger.Port.hot("hot-b", 128.0D)
        ));
        assertEquals(256.0D, two.coldFlowCapacityMb(), EPSILON);
        assertEquals(256.0D, two.hotFlowCapacityMb(), EPSILON);
        assertEquals(256.0D, two.balancedFlowMb(), EPSILON);

        ReactorCoolantLedger.PortSummary three = ReactorCoolantLedger.summarizePorts(List.of(
                ReactorCoolantLedger.Port.cold("cold-a", 128.0D),
                ReactorCoolantLedger.Port.cold("cold-b", 128.0D),
                ReactorCoolantLedger.Port.cold("cold-c", 128.0D),
                ReactorCoolantLedger.Port.hot("hot-a", 128.0D),
                ReactorCoolantLedger.Port.hot("hot-b", 128.0D),
                ReactorCoolantLedger.Port.hot("hot-c", 128.0D)
        ));
        assertEquals(384.0D, three.coldInputMb(), EPSILON);
        assertEquals(384.0D, three.hotOutputCapacityMb(), EPSILON);
        assertEquals(384.0D, three.coldFlowCapacityMb(), EPSILON);
        assertEquals(384.0D, three.hotFlowCapacityMb(), EPSILON);
        assertEquals(384.0D, three.balancedFlowMb(), EPSILON);
    }

    @Test
    void asymmetricActualFlowsUseTheSmallerSide() {
        ReactorCoolantLedger.PortSummary summary = ReactorCoolantLedger.summarizePorts(List.of(
                ReactorCoolantLedger.Port.cold("cold-a", 128.0D),
                ReactorCoolantLedger.Port.cold("cold-b", 128.0D),
                ReactorCoolantLedger.Port.cold("cold-c", 128.0D),
                ReactorCoolantLedger.Port.hot("hot-a", 64.0D),
                ReactorCoolantLedger.Port.hot("hot-b", 64.0D)
        ));

        assertEquals(384.0D, summary.coldInputMb(), EPSILON);
        assertEquals(128.0D, summary.hotOutputCapacityMb(), EPSILON);
        assertEquals(128.0D, summary.balancedFlowMb(), EPSILON);
    }

    @Test
    void duplicateConnectionsDoNotIncreaseCountsOrFlow() {
        ReactorCoolantLedger.PortSummary summary = ReactorCoolantLedger.summarizePorts(List.of(
                ReactorCoolantLedger.Port.cold("cold-a", 128.0D),
                ReactorCoolantLedger.Port.cold("cold-a", 128.0D),
                ReactorCoolantLedger.Port.cold("cold-b", 128.0D),
                ReactorCoolantLedger.Port.hot("hot-a", 128.0D),
                ReactorCoolantLedger.Port.hot("hot-a", 128.0D)
        ));

        assertEquals(2, summary.uniqueColdPortCount());
        assertEquals(1, summary.uniqueHotPortCount());
        assertEquals(2, summary.duplicatePortCount());
        assertEquals(256.0D, summary.coldInputMb(), EPSILON);
        assertEquals(128.0D, summary.hotOutputCapacityMb(), EPSILON);
        assertEquals(128.0D, summary.balancedFlowMb(), EPSILON);
    }

    @Test
    void eachPortIsCappedIndependentlyAndDuplicateObservationsAreNotSummed() {
        ReactorCoolantLedger.PortSummary summary = ReactorCoolantLedger.summarizePorts(List.of(
                ReactorCoolantLedger.Port.cold("cold-a", 500.0D),
                ReactorCoolantLedger.Port.cold("cold-a", 1.0D),
                ReactorCoolantLedger.Port.cold("cold-b", 500.0D),
                ReactorCoolantLedger.Port.hot("hot-a", 500.0D)
        ));

        assertEquals(256.0D, summary.coldInputMb(), EPSILON);
        assertEquals(128.0D, summary.hotOutputCapacityMb(), EPSILON);
        assertEquals(2, summary.uniqueColdPortCount());
        assertEquals(1, summary.duplicatePortCount());
    }

    @Test
    void invalidPortInputsAndLimitsAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ReactorCoolantLedger.summarizePorts(null));
        assertThrows(IllegalArgumentException.class, () ->
                ReactorCoolantLedger.summarizePorts(List.of(), -1.0D));
        assertThrows(IllegalArgumentException.class, () ->
                ReactorCoolantLedger.summarizePorts(Collections.singletonList(null)));
        assertThrows(IllegalArgumentException.class, () ->
                ReactorCoolantLedger.Port.cold("", 128.0D));
        assertThrows(IllegalArgumentException.class, () ->
                ReactorCoolantLedger.Port.hot("hot-a", Double.NaN));
    }
}
