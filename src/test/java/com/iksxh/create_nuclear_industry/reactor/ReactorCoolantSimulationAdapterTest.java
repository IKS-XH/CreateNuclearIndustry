package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReactorCoolantSimulationAdapterTest {
    private static final double EPSILON = 1.0E-12D;

    @Test
    void convertsWholeMillibucketsIntoTheAuthoritativeSnapshot() {
        ReactorSnapshot previous = snapshot(100L, 0L);
        ReactorCoolantSimulationAdapter.Result result = ReactorCoolantSimulationAdapter.settle(
                previous,
                new ReactorCoolantSimulationAdapter.TickInput(
                        50.0D,
                        ports(128.0D, 128.0D),
                        0.0D,
                        0.0D,
                        1_000L,
                        0.5D
                )
        );

        assertEquals(100.0D, result.settlement().convertedCoolantMb(), EPSILON);
        assertEquals(0L, result.nextSnapshot().coldCoolantMb());
        assertEquals(100L, result.nextSnapshot().hotCoolantMb());
        assertEquals(50.0D, result.settlement().removedHeatHu(), EPSILON);
        assertEquals(100L, result.nextSnapshot().coldCoolantMb()
                + result.nextSnapshot().hotCoolantMb());
    }

    @Test
    void actualHotOutputIsAppliedOnceAndTotalFluidIsConserved() {
        ReactorSnapshot previous = snapshot(64L, 10L);
        ReactorCoolantSimulationAdapter.Result result = ReactorCoolantSimulationAdapter.settle(
                previous,
                new ReactorCoolantSimulationAdapter.TickInput(
                        32.0D,
                        ports(128.0D, 128.0D),
                        0.0D,
                        4.0D,
                        1_000L,
                        0.5D
                )
        );

        assertEquals(70L, result.nextSnapshot().hotCoolantMb());
        assertEquals(4.0D, result.settlement().hotOutActualMb(), EPSILON);
        assertEquals(74.0D,
                result.nextSnapshot().coldCoolantMb()
                        + result.nextSnapshot().hotCoolantMb()
                        + result.settlement().hotOutActualMb(), EPSILON);
    }

    @Test
    void fullHotBufferAndBlockedOutputKeepColdInventoryAndHeat() {
        ReactorSnapshot previous = snapshot(10L, 1_000L);
        ReactorCoolantSimulationAdapter.Result result = ReactorCoolantSimulationAdapter.settle(
                previous,
                new ReactorCoolantSimulationAdapter.TickInput(
                        100.0D,
                        ports(128.0D, 0.0D),
                        0.0D,
                        0.0D,
                        1_000L,
                        0.5D
                )
        );

        assertEquals(previous, result.nextSnapshot());
        assertEquals(0.0D, result.settlement().convertedCoolantMb(), EPSILON);
        assertEquals(100.0D, result.settlement().remainingHeatHu(), EPSILON);
    }

    @Test
    void fractionalMillibucketDemandLeavesTheHeatRemainderForLaterTicks() {
        ReactorCoolantSimulationAdapter.Result result = ReactorCoolantSimulationAdapter.settle(
                snapshot(1L, 0L),
                new ReactorCoolantSimulationAdapter.TickInput(
                        0.25D,
                        ports(128.0D, 128.0D),
                        0.0D,
                        0.0D,
                        1_000L,
                        0.5D
                )
        );

        assertEquals(1L, result.nextSnapshot().coldCoolantMb());
        assertEquals(0L, result.nextSnapshot().hotCoolantMb());
        assertEquals(0.25D, result.settlement().remainingHeatHu(), EPSILON);
    }

    @Test
    void resultSnapshotSurvivesTheFormalNbtCodec() {
        ReactorCoolantSimulationAdapter.Result result = ReactorCoolantSimulationAdapter.settle(
                snapshot(128L, 12L),
                new ReactorCoolantSimulationAdapter.TickInput(
                        64.0D,
                        ports(128.0D, 128.0D),
                        0.0D,
                        12.0D,
                        1_000L,
                        0.5D
                )
        );

        assertEquals(result.nextSnapshot(), ReactorSnapshotNbtCodec.decode(
                ReactorSnapshotNbtCodec.encode(result.nextSnapshot())));
    }

    @Test
    void rejectsNonWholeFluidTransfersAtTheFormalSnapshotBoundary() {
        assertThrows(IllegalArgumentException.class, () ->
                new ReactorCoolantSimulationAdapter.TickInput(
                        1.0D,
                        ports(128.0D, 128.0D),
                        0.5D,
                        0.0D,
                        1_000L,
                        0.5D
                ));
    }

    private static ReactorSnapshot snapshot(long cold, long hot) {
        return new ReactorSnapshot(Map.of(), Map.of(), cold, hot, 0L, false);
    }

    private static ReactorCoolantLedger.PortSummary ports(double cold, double hot) {
        return ReactorCoolantLedger.summarizePorts(List.of(
                ReactorCoolantLedger.Port.cold("cold-a", cold),
                ReactorCoolantLedger.Port.hot("hot-a", hot)
        ));
    }
}
