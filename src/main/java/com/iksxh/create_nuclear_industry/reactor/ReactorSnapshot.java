package com.iksxh.create_nuclear_industry.reactor;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Immutable, loader-independent state snapshot for the fixed P1 5 x 5 x 5 reactor.
 * Missing positions in the 3 x 3 interior map represent empty core columns.
 */
public record ReactorSnapshot(
        Map<CoreColumnPosition, FuelColumnState> fuelColumns,
        Map<CoreColumnPosition, ControlRodColumnState> controlRodColumns,
        long coldCoolantMb,
        long hotCoolantMb,
        long meltdownProgressTicks,
        boolean meltdownCountdownStarted
) {
    public static final int OUTER_SIZE = 5;
    public static final int INTERNAL_HEIGHT = OUTER_SIZE - 2;
    public static final int MAX_CORE_COLUMNS = CoreColumnPosition.GRID_SIZE * CoreColumnPosition.GRID_SIZE;

    public ReactorSnapshot {
        fuelColumns = immutableOrderedCopy(fuelColumns, "fuel columns");
        controlRodColumns = immutableOrderedCopy(controlRodColumns, "control rod columns");

        for (CoreColumnPosition position : fuelColumns.keySet()) {
            if (controlRodColumns.containsKey(position)) {
                throw new IllegalArgumentException("a core position cannot contain both a fuel and control rod column");
            }
        }
        if (fuelColumns.size() + controlRodColumns.size() > MAX_CORE_COLUMNS) {
            throw new IllegalArgumentException("the fixed P1 core cannot contain more than 9 columns");
        }
        if (coldCoolantMb < 0L || hotCoolantMb < 0L) {
            throw new IllegalArgumentException("coolant ledger amounts must be non-negative");
        }
        if (meltdownProgressTicks < 0L) {
            throw new IllegalArgumentException("meltdown progress must be non-negative");
        }
        if (!meltdownCountdownStarted && meltdownProgressTicks != 0L) {
            throw new IllegalArgumentException("inactive meltdown countdown cannot have progress");
        }
    }

    public static ReactorSnapshot empty() {
        return new ReactorSnapshot(Map.of(), Map.of(), 0L, 0L, 0L, false);
    }

    public static ReactorSnapshot singleFuelColumn(CoreColumnPosition position, FuelColumnState state) {
        return new ReactorSnapshot(Map.of(position, state), Map.of(), 0L, 0L, 0L, false);
    }

    public static ReactorSnapshot singleControlRodColumn(
            CoreColumnPosition position,
            ControlRodColumnState state
    ) {
        return new ReactorSnapshot(Map.of(), Map.of(position, state), 0L, 0L, 0L, false);
    }

    public int occupiedColumnCount() {
        return fuelColumns.size() + controlRodColumns.size();
    }

    public boolean hasCompleteCoreColumnSet() {
        return occupiedColumnCount() == MAX_CORE_COLUMNS;
    }

    /**
     * Returns a copy with only the shared coolant inventories changed.
     *
     * <p>The instrument port remains the sole owner of the complete reactor
     * snapshot; fluid capabilities use this method instead of creating a
     * port-local inventory.</p>
     */
    public ReactorSnapshot withCoolantInventories(long nextColdCoolantMb, long nextHotCoolantMb) {
        return new ReactorSnapshot(
                fuelColumns,
                controlRodColumns,
                nextColdCoolantMb,
                nextHotCoolantMb,
                meltdownProgressTicks,
                meltdownCountdownStarted
        );
    }

    private static <T> Map<CoreColumnPosition, T> immutableOrderedCopy(
            Map<CoreColumnPosition, T> source,
            String name
    ) {
        if (source == null) {
            throw new IllegalArgumentException(name + " map is required");
        }
        TreeMap<CoreColumnPosition, T> ordered = new TreeMap<>();
        source.forEach((position, state) -> {
            if (position == null || state == null) {
                throw new IllegalArgumentException(name + " must contain non-null positions and states");
            }
            ordered.put(position, state);
        });
        return Collections.unmodifiableMap(ordered);
    }
}
