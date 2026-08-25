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
        boolean meltdownCountdownStarted,
        Map<CoreColumnPosition, Double> scramSavedTargetDepths,
        boolean scramRequested
) {
    public static final int OUTER_SIZE = 5;
    public static final int INTERNAL_HEIGHT = OUTER_SIZE - 2;
    public static final int MAX_CORE_COLUMNS = CoreColumnPosition.GRID_SIZE * CoreColumnPosition.GRID_SIZE;

    public ReactorSnapshot {
        fuelColumns = immutableOrderedCopy(fuelColumns, "fuel columns");
        controlRodColumns = immutableOrderedCopy(controlRodColumns, "control rod columns");
        scramSavedTargetDepths = immutableTargetCopy(scramSavedTargetDepths);

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
        for (CoreColumnPosition position : scramSavedTargetDepths.keySet()) {
            if (!controlRodColumns.containsKey(position)) {
                throw new IllegalArgumentException("SCRAM restore targets must identify control rod columns");
            }
        }
        if (!scramRequested && !scramSavedTargetDepths.isEmpty()) {
            throw new IllegalArgumentException("inactive SCRAM cannot retain restore targets");
        }
    }

    /** Backward-compatible constructor for snapshots without SCRAM state. */
    public ReactorSnapshot(
            Map<CoreColumnPosition, FuelColumnState> fuelColumns,
            Map<CoreColumnPosition, ControlRodColumnState> controlRodColumns,
            long coldCoolantMb,
            long hotCoolantMb,
            long meltdownProgressTicks,
            boolean meltdownCountdownStarted
    ) {
        this(fuelColumns, controlRodColumns, coldCoolantMb, hotCoolantMb,
                meltdownProgressTicks, meltdownCountdownStarted, Map.of(), false);
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
                meltdownCountdownStarted,
                scramSavedTargetDepths,
                scramRequested
        );
    }

    public boolean scramActive() {
        return scramRequested && !controlRodColumns.isEmpty();
    }

    public ReactorSnapshot withColumns(
            Map<CoreColumnPosition, FuelColumnState> nextFuelColumns,
            Map<CoreColumnPosition, ControlRodColumnState> nextControlRodColumns
    ) {
        TreeMap<CoreColumnPosition, Double> nextSavedTargetDepths = new TreeMap<>();
        scramSavedTargetDepths.forEach((position, targetDepth) -> {
            if (nextControlRodColumns.containsKey(position)) {
                nextSavedTargetDepths.put(position, targetDepth);
            }
        });
        return new ReactorSnapshot(
                nextFuelColumns,
                nextControlRodColumns,
                coldCoolantMb,
                hotCoolantMb,
                meltdownProgressTicks,
                meltdownCountdownStarted,
                nextSavedTargetDepths,
                scramRequested && !nextControlRodColumns.isEmpty()
        );
    }

    public ReactorSnapshot withMeltdown(long nextProgressTicks, boolean nextStarted) {
        return new ReactorSnapshot(
                fuelColumns,
                controlRodColumns,
                coldCoolantMb,
                hotCoolantMb,
                nextProgressTicks,
                nextStarted,
                scramSavedTargetDepths,
                scramRequested
        );
    }

    public ReactorSnapshot withScramState(
            Map<CoreColumnPosition, Double> nextSavedTargetDepths,
            boolean nextRequested
    ) {
        return new ReactorSnapshot(
                fuelColumns,
                controlRodColumns,
                coldCoolantMb,
                hotCoolantMb,
                meltdownProgressTicks,
                meltdownCountdownStarted,
                nextSavedTargetDepths,
                nextRequested
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

    private static Map<CoreColumnPosition, Double> immutableTargetCopy(
            Map<CoreColumnPosition, Double> source
    ) {
        if (source == null) {
            throw new IllegalArgumentException("SCRAM restore targets are required");
        }
        TreeMap<CoreColumnPosition, Double> ordered = new TreeMap<>();
        source.forEach((position, target) -> {
            if (position == null || target == null || !Double.isFinite(target)
                    || target < 0.0D || target > 1.0D) {
                throw new IllegalArgumentException("SCRAM restore targets must be finite values in [0, 1]");
            }
            ordered.put(position, target);
        });
        return Collections.unmodifiableMap(ordered);
    }
}
