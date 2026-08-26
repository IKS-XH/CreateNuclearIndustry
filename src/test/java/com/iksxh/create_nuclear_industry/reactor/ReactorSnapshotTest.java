package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证正式快照的列互斥、深度/库存/融毁不变量和防御性拷贝。 */
class ReactorSnapshotTest {
    @Test
    void constructsEmptyReactorSnapshotWithSafeDefaults() {
        ReactorSnapshot snapshot = ReactorSnapshot.empty();

        assertTrue(snapshot.fuelColumns().isEmpty());
        assertTrue(snapshot.controlRodColumns().isEmpty());
        assertEquals(0, snapshot.occupiedColumnCount());
        assertEquals(0L, snapshot.coldCoolantMb());
        assertEquals(0L, snapshot.hotCoolantMb());
        assertEquals(0L, snapshot.meltdownProgressTicks());
        assertFalse(snapshot.meltdownCountdownStarted());
        assertFalse(snapshot.hasCompleteCoreColumnSet());
    }

    @Test
    void constructsSingleFuelColumnAndPreservesAssemblyDurability() {
        CoreColumnPosition position = new CoreColumnPosition(1, 1);
        FuelAssemblyState assembly = FuelAssemblyState.installed(216_000, 42_000);
        FuelColumnState column = new FuelColumnState(assembly, 0.75D, 12.5D);
        ReactorSnapshot snapshot = ReactorSnapshot.singleFuelColumn(position, column);

        assertEquals(column, snapshot.fuelColumns().get(position));
        assertEquals(174_000, assembly.remainingDurability());
        assertEquals(174_000.0D / 216_000.0D, assembly.remainingFraction(), 1.0E-12D);
        assertEquals(0.75D, snapshot.fuelColumns().get(position).integrity());
        assertEquals(12.5D, snapshot.fuelColumns().get(position).cachedHeatHu());
    }

    @Test
    void constructsSingleJammedControlRodWithIndependentTargetAndActualDepth() {
        CoreColumnPosition position = new CoreColumnPosition(0, 2);
        ControlRodColumnState column = new ControlRodColumnState(0.0D, 1.0D, 0.35D, true, 4.5D);
        ReactorSnapshot snapshot = ReactorSnapshot.singleControlRodColumn(position, column);

        assertEquals(column, snapshot.controlRodColumns().get(position));
        assertEquals(1.0D, column.targetDepth());
        assertEquals(0.35D, column.actualDepth());
        assertTrue(column.jammed());
        assertEquals(4.5D, column.cachedHeatHu());
    }

    @Test
    void constructsCompleteFixedFiveByFiveByFiveSnapshot() {
        Map<CoreColumnPosition, FuelColumnState> fuelColumns = new HashMap<>();
        Map<CoreColumnPosition, ControlRodColumnState> controlColumns = new HashMap<>();
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                CoreColumnPosition position = new CoreColumnPosition(x, z);
                if ((x + z) % 2 == 0) {
                    fuelColumns.put(position, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, (x * 3 + z) * 1_000),
                            1.0D,
                            x + z
                    ));
                } else {
                    controlColumns.put(position, ControlRodColumnState.fullyInserted());
                }
            }
        }

        ReactorSnapshot snapshot = new ReactorSnapshot(
                fuelColumns,
                controlColumns,
                8_000L,
                2_000L,
                120L,
                true
        );

        assertEquals(5, ReactorSnapshot.OUTER_SIZE);
        assertEquals(3, ReactorSnapshot.INTERNAL_HEIGHT);
        assertEquals(9, ReactorSnapshot.MAX_CORE_COLUMNS);
        assertEquals(9, snapshot.occupiedColumnCount());
        assertTrue(snapshot.hasCompleteCoreColumnSet());
        assertEquals(8_000L, snapshot.coldCoolantMb());
        assertEquals(2_000L, snapshot.hotCoolantMb());
        assertEquals(120L, snapshot.meltdownProgressTicks());
        assertTrue(snapshot.meltdownCountdownStarted());

        List<CoreColumnPosition> orderedFuelPositions = new ArrayList<>(snapshot.fuelColumns().keySet());
        List<CoreColumnPosition> sortedFuelPositions = new ArrayList<>(orderedFuelPositions);
        sortedFuelPositions.sort(CoreColumnPosition::compareTo);
        assertEquals(sortedFuelPositions, orderedFuelPositions);
    }

    @Test
    void snapshotDefensivelyCopiesColumnMapsAndRejectsOverlap() {
        CoreColumnPosition position = new CoreColumnPosition(0, 0);
        Map<CoreColumnPosition, FuelColumnState> source = new HashMap<>();
        source.put(position, FuelColumnState.empty());
        ReactorSnapshot snapshot = new ReactorSnapshot(source, Map.of(), 0L, 0L, 0L, false);

        source.clear();
        assertEquals(1, snapshot.fuelColumns().size());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.fuelColumns().put(new CoreColumnPosition(1, 1), FuelColumnState.empty()));
        assertThrows(IllegalArgumentException.class, () -> new ReactorSnapshot(
                Map.of(position, FuelColumnState.empty()),
                Map.of(position, ControlRodColumnState.fullyInserted()),
                0L,
                0L,
                0L,
                false
        ));
    }

    @Test
    void rejectsInvalidDurabilityColumnRangesLedgersAndMeltdownProgress() {
        assertThrows(IllegalArgumentException.class, () -> new CoreColumnPosition(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new CoreColumnPosition(3, 0));
        assertThrows(IllegalArgumentException.class, () -> FuelAssemblyState.installed(0, 0));
        assertThrows(IllegalArgumentException.class, () -> FuelAssemblyState.installed(100, -1));
        assertThrows(IllegalArgumentException.class, () -> FuelAssemblyState.installed(100, 101));
        assertThrows(IllegalArgumentException.class,
                () -> new FuelAssemblyState(false, 1, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new FuelColumnState(FuelAssemblyState.empty(), -0.01D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new FuelColumnState(FuelAssemblyState.empty(), Double.NaN, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new FuelColumnState(FuelAssemblyState.empty(), 1.0D, -1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new ControlRodColumnState(1.0D, 1.01D, 0.5D, false, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new ControlRodColumnState(1.0D, 0.5D, Double.POSITIVE_INFINITY, false, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new ReactorSnapshot(Map.of(), Map.of(), -1L, 0L, 0L, false));
        assertThrows(IllegalArgumentException.class,
                () -> new ReactorSnapshot(Map.of(), Map.of(), 0L, -1L, 0L, false));
        assertThrows(IllegalArgumentException.class,
                () -> new ReactorSnapshot(Map.of(), Map.of(), 0L, 0L, -1L, true));
        assertThrows(IllegalArgumentException.class,
                () -> new ReactorSnapshot(Map.of(), Map.of(), 0L, 0L, 1L, false));
    }
}
