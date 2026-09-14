package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证固定 P1 结构从空列与控制棒列主体坐标派生共享冷却剂容量。 */
class ReactorCoolantCapacityTest {
    @Test
    void canonicalLayoutCountsOnlyTheThreeEmptyBodyPositions() {
        ReactorCoolantCapacity.Derived derived = derive(
                ReactorStructureDefinition.defaultColumnLayout(), 1_000L);

        assertEquals(1, derived.emptyColumnCount());
        assertEquals(3, derived.coolantSpaceBlockCount());
        assertEquals(3_000L, derived.coolantCapacityMb());
    }

    @Test
    void fcfLayoutCountsAllThreeControlRodBodiesAsSharedSpace() {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout = new TreeMap<>();
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                layout.put(new CoreColumnPosition(x, z),
                        x == 1 ? ReactorStructureDefinition.ColumnType.CONTROL_ROD
                                : ReactorStructureDefinition.ColumnType.FUEL);
            }
        }

        ReactorCoolantCapacity.Derived derived = derive(layout, 1_000L);

        assertEquals(0, derived.emptyColumnCount());
        assertEquals(9, derived.coolantSpaceBlockCount());
        assertEquals(9_000L, derived.coolantCapacityMb());
    }

    @Test
    void allFuelIsValidButHasZeroCoolantSpace() {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout = layoutWith(
                ReactorStructureDefinition.ColumnType.FUEL);

        ReactorCoolantCapacity.Derived derived = derive(layout, 1_000L);

        assertEquals(0, derived.emptyColumnCount());
        assertEquals(0, derived.coolantSpaceBlockCount());
        assertEquals(0L, derived.coolantCapacityMb());
    }

    @Test
    void oneFuelAndEightEmptyColumnsUseAllTwentyFourBodyPositions() {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout = layoutWith(
                ReactorStructureDefinition.ColumnType.EMPTY);
        layout.put(new CoreColumnPosition(0, 0), ReactorStructureDefinition.ColumnType.FUEL);

        ReactorCoolantCapacity.Derived derived = derive(layout, 1_000L);

        assertEquals(8, derived.emptyColumnCount());
        assertEquals(24, derived.coolantSpaceBlockCount());
        assertEquals(24_000L, derived.coolantCapacityMb());
    }

    @Test
    void eachCountUsesTheCachedBodyPositionListExactly() {
        ReactorStructureDefinition.ScanResult scan =
                ReactorStructureDefinition.scan(ReactorStructureDefinition.canonicalTemplate());
        assertEquals(9, scan.columns().size());
        assertEquals(1L, scan.columns().values().stream()
                .mapToInt(mapping -> mapping.bodyPositions().size()).distinct().count());
        assertEquals(3, scan.columns().values().iterator().next().bodyPositions().size());
    }

    @Test
    void zeroAndMaximumPerBlockCapacityAreSafeForIntegerCapability() {
        assertEquals(0L, derive(ReactorStructureDefinition.defaultColumnLayout(), 0L)
                .coolantCapacityMb());
        ReactorCoolantCapacity.Derived maximum = derive(layoutWith(
                ReactorStructureDefinition.ColumnType.EMPTY),
                ReactorCoolantCapacity.MAX_CAPACITY_PER_EMPTY_BLOCK_MB);
        assertEquals((long) Integer.MAX_VALUE / 24 * 24, maximum.coolantCapacityMb());
    }

    @Test
    void invalidAndOverflowCapacityInputsFailClosed() {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout = layoutWith(
                ReactorStructureDefinition.ColumnType.EMPTY);
        layout.put(new CoreColumnPosition(0, 0), ReactorStructureDefinition.ColumnType.FUEL);
        ReactorStructureDefinition.ScanResult scan = ReactorStructureDefinition.scan(
                ReactorStructureDefinition.templateFor(layout));

        assertThrows(IllegalArgumentException.class,
                () -> ReactorCoolantCapacity.fromScan(scan, -1L));
        assertThrows(IllegalArgumentException.class,
                () -> ReactorCoolantCapacity.fromScan(scan,
                        ReactorCoolantCapacity.MAX_CAPACITY_PER_EMPTY_BLOCK_MB + 1L));
        assertThrows(IllegalArgumentException.class,
                () -> ReactorCoolantCapacity.capacityFor(25, 1_000L));
        assertThrows(IllegalArgumentException.class,
                () -> ReactorCoolantCapacity.capacityFor(-1, 1_000L));
    }

    private static ReactorCoolantCapacity.Derived derive(
            Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout,
            long capacityPerBlockMb
    ) {
        return ReactorCoolantCapacity.fromScan(
                ReactorStructureDefinition.scan(ReactorStructureDefinition.templateFor(layout)),
                capacityPerBlockMb);
    }

    private static Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layoutWith(
            ReactorStructureDefinition.ColumnType type
    ) {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout = new TreeMap<>();
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                layout.put(new CoreColumnPosition(x, z), type);
            }
        }
        if (type == ReactorStructureDefinition.ColumnType.EMPTY) {
            layout.put(new CoreColumnPosition(0, 0), ReactorStructureDefinition.ColumnType.FUEL);
        }
        return layout;
    }
}
