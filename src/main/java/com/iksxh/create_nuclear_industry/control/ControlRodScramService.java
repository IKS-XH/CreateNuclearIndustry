package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.ControlRodStateTransitions;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.ReactorFissionCalculator;
import com.iksxh.create_nuclear_industry.reactor.ReactorFissionResult;
import com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;

import java.util.Map;
import java.util.TreeMap;

/** Server-authoritative redstone SCRAM transitions for one instrument port. */
public final class ControlRodScramService {
    private static final double HEAT_EPSILON = 1.0E-9D;

    private ControlRodScramService() {
    }

    public static ControlRodScramResult apply(
            ReactorInstrumentPortBlockEntity instrument,
            boolean powered
    ) {
        if (instrument == null || !instrument.structureValid()) {
            return new ControlRodScramResult(
                    ControlRodScramStatus.SCRAM_INVALID_STRUCTURE,
                    false,
                    false,
                    0.0D,
                    "SCRAM requires one valid reactor structure"
            );
        }

        ReactorSnapshot before = instrument.snapshot();
        Map<CoreColumnPosition, ControlRodColumnState> mappedControls = mappedControls(instrument);
        if (!powered) {
            return release(instrument, before);
        }
        if (mappedControls.isEmpty()) {
            if (before.scramRequested()) {
                instrument.setSnapshot(before.withScramState(Map.of(), false));
            }
            return new ControlRodScramResult(
                    ControlRodScramStatus.SCRAM_UNAVAILABLE_NO_CONTROL_RODS,
                    false,
                    false,
                    0.0D,
                    "SCRAM is unavailable because the structure has no control rods"
            );
        }
        if (before.scramRequested()) {
            return activeResult(before, ControlRodScramStatus.SCRAM_ALREADY_ACTIVE);
        }

        TreeMap<CoreColumnPosition, ControlRodColumnState> controls =
                new TreeMap<>(before.controlRodColumns());
        TreeMap<CoreColumnPosition, Double> savedTargets = new TreeMap<>();
        for (Map.Entry<CoreColumnPosition, ControlRodColumnState> entry : mappedControls.entrySet()) {
            ControlRodColumnState state = entry.getValue();
            if (state.jammed()) {
                continue;
            }
            savedTargets.put(entry.getKey(), state.targetDepth());
            controls.put(entry.getKey(), ControlRodStateTransitions.scramInsert(state));
        }

        ReactorSnapshot scrammed = before.withColumns(before.fuelColumns(), controls)
                .withScramState(savedTargets, true);
        instrument.setSnapshot(scrammed);
        return activeResult(scrammed, ControlRodScramStatus.SCRAM_ACTIVE);
    }

    private static ControlRodScramResult release(
            ReactorInstrumentPortBlockEntity instrument,
            ReactorSnapshot before
    ) {
        if (!before.scramRequested()) {
            return new ControlRodScramResult(
                    ControlRodScramStatus.SCRAM_NOT_ACTIVE,
                    false,
                    false,
                    0.0D,
                    "SCRAM was not active"
            );
        }

        TreeMap<CoreColumnPosition, ControlRodColumnState> controls =
                new TreeMap<>(before.controlRodColumns());
        for (Map.Entry<CoreColumnPosition, Double> entry : before.scramSavedTargetDepths().entrySet()) {
            ControlRodColumnState state = controls.get(entry.getKey());
            if (state != null && !state.jammed()) {
                controls.put(entry.getKey(), ControlRodStateTransitions.restoreTargetDepth(
                        state, entry.getValue()));
            }
        }

        ReactorSnapshot released = before.withColumns(before.fuelColumns(), controls)
                .withScramState(Map.of(), false);
        instrument.setSnapshot(released);
        return new ControlRodScramResult(
                ControlRodScramStatus.SCRAM_RELEASED,
                false,
                false,
                0.0D,
                "SCRAM released and movable rod targets restored"
        );
    }

    private static ControlRodScramResult activeResult(
            ReactorSnapshot snapshot,
            ControlRodScramStatus activeStatus
    ) {
        double fissionHeat = projectedFissionHeat(snapshot);
        ControlRodScramStatus status = fissionHeat > HEAT_EPSILON
                ? ControlRodScramStatus.SCRAM_INCOMPLETE
                : activeStatus;
        return new ControlRodScramResult(
                status,
                true,
                snapshot.scramActive(),
                fissionHeat,
                status == ControlRodScramStatus.SCRAM_INCOMPLETE
                        ? "SCRAM is active but residual fission heat remains"
                        : "SCRAM is active"
        );
    }

    /**
     * SCRAM has already physically inserted every movable rod. Calculating
     * with normal control rules preserves the documented jammed-rod case:
     * a jammed rod at a partial depth can still leave fission heat.
     */
    private static double projectedFissionHeat(ReactorSnapshot snapshot) {
        ReactorFissionResult result = ReactorFissionCalculator.calculate(
                snapshot, ReactorSimulationParameters.defaults(), false);
        return result.generatedHeatHu();
    }

    private static Map<CoreColumnPosition, ControlRodColumnState> mappedControls(
            ReactorInstrumentPortBlockEntity instrument
    ) {
        TreeMap<CoreColumnPosition, ControlRodColumnState> mapped = new TreeMap<>();
        for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                : instrument.structureScan().columns().entrySet()) {
            if (entry.getValue().type() != ReactorStructureDefinition.ColumnType.CONTROL_ROD) {
                continue;
            }
            ControlRodColumnState state = instrument.snapshot().controlRodColumns().get(entry.getKey());
            if (state != null) {
                mapped.put(entry.getKey(), state);
            }
        }
        return mapped;
    }
}
