package com.iksxh.create_nuclear_industry.p0probe;

import com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnKey;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnState;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorModel;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorParameters;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorTickInput;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorTickResult;
import com.iksxh.create_nuclear_industry.p0probe.numeric.CoolantPort;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ControlRodController;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorSnapshotCodec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P0NumericPrototypeTest {
    private static final ReactorParameters DEFAULTS = ReactorParameters.defaults();
    private static final List<String> SCENARIOS = new ArrayList<>();

    @AfterAll
    static void writeEvidence() throws IOException {
        Path reportDir = Path.of("build", "reports", "p0", "numeric");
        Files.createDirectories(reportDir);
        Files.writeString(reportDir.resolve("parameters.csv"),
                "parameter,value\n"
                        + "baseHeatPerFuel," + DEFAULTS.baseHeatPerFuel() + "\n"
                        + "burnHoursPerBlock," + DEFAULTS.burnHoursPerBlock() + "\n"
                        + "baseBurnPerFuel," + DEFAULTS.baseBurnPerFuel() + "\n"
                        + "coolantAbsorptionHuPerMb," + DEFAULTS.coolantAbsorptionHuPerMb() + "\n"
                        + "coolantMaxFlowPerPort," + DEFAULTS.coolantMaxFlowPerPort() + "\n"
                        + "coolantTotalFlowCap," + DEFAULTS.coolantTotalFlowCap() + "\n"
                        + "fuelColumnDamageHeatThreshold," + DEFAULTS.fuelColumnDamageHeatThreshold() + "\n"
                        + "fuelColumnDamageRate," + DEFAULTS.fuelColumnDamageRate() + "\n"
                        + "fuelColumnDamageTransferRate," + DEFAULTS.fuelColumnDamageTransferRate() + "\n"
                        + "meltdownTriggerFraction," + DEFAULTS.meltdownTriggerFraction() + "\n"
                        + "meltdownCountdownTicks," + DEFAULTS.meltdownCountdownTicks() + "\n");
        Files.writeString(reportDir.resolve("scenario-matrix.csv"),
                "scenario,result\n" + String.join("\n", SCENARIOS) + (SCENARIOS.isEmpty() ? "" : "\n"));
        StringBuilder scan = new StringBuilder("baseHeatPerFuel,coolantFlowMbPerTick,netHeatLoad,integrityAfter10,status\n");
        for (double baseHeat : new double[]{0.5, 1, 100, 250, 500}) {
            for (double coolantFlow : new double[]{0, 50, 100, 200}) {
                ReactorParameters parameters = DEFAULTS.toBuilder().baseHeatPerFuel(baseHeat).build();
                ReactorSnapshot state = snapshot(ColumnState.fuel(new ColumnKey(0, 0), 1, 1, 0));
                ReactorTickResult first = ReactorModel.tick(state, parameters, ports(coolantFlow));
                state = first.next();
                for (int tick = 1; tick < 10; tick++) {
                    state = ReactorModel.tick(state, parameters, ports(coolantFlow)).next();
                }
                double integrity = state.columns().get(new ColumnKey(0, 0)).fuelColumnIntegrity();
                String status = first.columns().get(new ColumnKey(0, 0)).netHeatLoad()
                        <= parameters.fuelColumnDamageHeatThreshold() ? "SAFE"
                        : integrity > 0 ? "DANGER" : "EXTREME";
                scan.append(baseHeat).append(',').append(coolantFlow).append(',')
                        .append(first.columns().get(new ColumnKey(0, 0)).netHeatLoad()).append(',')
                        .append(integrity).append(',').append(status).append('\n');
            }
        }
        Files.writeString(reportDir.resolve("parameter-scan.csv"), scan.toString());
    }

    @Test
    void n01_emptyReactorHasNoHeatBurnDamageOrMeltdown() {
        ReactorTickResult result = tick(ReactorSnapshot.empty(), ReactorTickInput.noCooling(1));
        assertEquals(0, result.generatedHeat(), 1e-12);
        assertEquals(0, result.plannedBurn(), 1e-12);
        assertEquals(0, result.convertedCoolant(), 1e-12);
        assertEquals(0, result.next().meltdownProgress(), 1e-12);
        assertFalse(result.next().meltdownTriggered());
        pass("N-01");
    }

    @Test
    void n02_singleFullPowerColumnHitsThreeHourAnchor() {
        ReactorSnapshot state = snapshot(ColumnState.fuel(new ColumnKey(0, 0), 1, 1, 0));
        ReactorTickInput cooling = ports(1);
        int ticks = 0;
        while (state.columns().get(new ColumnKey(0, 0)).fuelRemaining() > 0 && ticks < 216005) {
            ReactorTickResult result = ReactorModel.tick(state, DEFAULTS, cooling);
            state = result.next();
            ticks++;
        }
        assertTrue(ticks >= 215999 && ticks <= 216002, "3 hour anchor ticks=" + ticks);
        assertEquals(0, state.columns().get(new ColumnKey(0, 0)).fuelRemaining(), 1e-10);
        assertEquals(1, state.columns().get(new ColumnKey(0, 0)).fuelColumnIntegrity(), 1e-12,
                "cooling should prevent damage, not fuel burn");
        pass("N-02");
    }

    @Test
    void n03_controlDepthIsBoundedAndMonotonicAndScramRestoresPreviousDepth() {
        ColumnKey fuel = new ColumnKey(0, 0);
        ColumnKey control = new ColumnKey(1, 0);
        double previous = Double.POSITIVE_INFINITY;
        for (double depth : new double[]{0, .25, .5, .75, 1}) {
            ReactorSnapshot state = snapshot(ColumnState.fuel(fuel, 1, 1, 0),
                    new ColumnState(control, com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnKind.CONTROL_ROD,
                            0, 0, 1, depth, depth, 0, false));
            double heat = tick(state, ReactorTickInput.noCooling(1)).generatedHeat();
            assertTrue(heat >= 0 && heat <= 1);
            assertTrue(heat <= previous + 1e-12, "depth must reduce heat");
            previous = heat;
        }

        Map<ColumnKey, ColumnState> controls = new TreeMapForTest();
        controls.put(control, new ColumnState(control,
                com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnKind.CONTROL_ROD,
                0, 0, 1, .35, .35, 0, false));
        ControlRodController controller = new ControlRodController();
        assertTrue(controller.setScram(controls, true));
        assertFalse(controller.setDepth(controls, control, .1));
        Map<ColumnKey, ColumnState> scramApplied = controller.apply(controls);
        assertEquals(1, scramApplied.get(control).controlRodDepth(), 1e-12);
        assertTrue(controller.setScram(controls, false));
        assertEquals(.35, controller.apply(controls).get(control).controlRodDepth(), 1e-12);
        pass("N-03");
    }

    @Test
    void n04_onlyDirectFuelNeighboursCreateFeedback() {
        ReactorTickResult adjacent = tick(snapshot(
                ColumnState.fuel(new ColumnKey(0, 0), 1, 1, 0),
                ColumnState.fuel(new ColumnKey(1, 0), 1, 1, 0)), ReactorTickInput.noCooling(1));
        ReactorTickResult separated = tick(snapshot(
                ColumnState.fuel(new ColumnKey(0, 0), 1, 1, 0),
                ColumnState.fuel(new ColumnKey(2, 0), 1, 1, 0)), ReactorTickInput.noCooling(1));
        assertTrue(adjacent.generatedHeat() > separated.generatedHeat());
        assertEquals(2, separated.generatedHeat(), 1e-12);
        pass("N-04");
    }

    @Test
    void n05FeedbackIsBoundedConvergentAndIndependentOfMapInsertionOrder() {
        Map<ColumnKey, ColumnState> first = new LinkedHashMap<>();
        Map<ColumnKey, ColumnState> second = new LinkedHashMap<>();
        List<ColumnState> states = new ArrayList<>();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                states.add(ColumnState.fuel(new ColumnKey(x, z), 1, 1, 0));
            }
        }
        states.forEach(column -> first.put(column.key(), column));
        for (int i = states.size() - 1; i >= 0; i--) {
            ColumnState column = states.get(i);
            second.put(column.key(), column);
        }
        ReactorTickResult firstResult = tick(new ReactorSnapshot(first, 0, false, 0), ReactorTickInput.noCooling(1));
        ReactorTickResult secondResult = tick(new ReactorSnapshot(second, 0, false, 0), ReactorTickInput.noCooling(1));
        assertEquals(firstResult.generatedHeat(), secondResult.generatedHeat(), 1e-12);
        assertTrue(firstResult.generatedHeat() <= 9 * DEFAULTS.overclockHeatMultiplier());
        pass("N-05");
    }

    @Test
    void n06IntegrityFeedbackIsSameForHeatAndBurnAndZeroStopsBoth() {
        Map<ColumnKey, ColumnState> columns = new LinkedHashMap<>();
        double[] integrity = {1, .75, .5, .25, 0};
        for (int i = 0; i < integrity.length; i++) {
            columns.put(new ColumnKey(i * 2, 0), ColumnState.fuel(new ColumnKey(i * 2, 0), 1,
                    integrity[i], 0));
        }
        ReactorTickResult result = tick(snapshot(columns), ReactorTickInput.noCooling(1));
        assertEquals(1, result.columns().get(new ColumnKey(0, 0)).generatedHeat(), 1e-12);
        assertEquals(1.25, result.columns().get(new ColumnKey(2, 0)).generatedHeat(), 1e-12);
        assertEquals(1.5, result.columns().get(new ColumnKey(4, 0)).generatedHeat(), 1e-12);
        assertEquals(1.75, result.columns().get(new ColumnKey(6, 0)).generatedHeat(), 1e-12);
        assertEquals(0, result.columns().get(new ColumnKey(8, 0)).generatedHeat(), 1e-12);
        assertTrue(result.columns().get(new ColumnKey(2, 0)).plannedBurn()
                > result.columns().get(new ColumnKey(0, 0)).plannedBurn());
        assertEquals(0, result.columns().get(new ColumnKey(8, 0)).plannedBurn(), 1e-12);
        pass("N-06");
    }

    @Test
    void n07CoolingUsesPerPortAndTotalCapsAndDoesNotHideResidualHeat() {
        ReactorParameters parameters = DEFAULTS.toBuilder().baseHeatPerFuel(500).build();
        ReactorSnapshot state = snapshot(ColumnState.fuel(new ColumnKey(0, 0), 1, 1, 0));
        for (double flow : new double[]{0, 50, 100, 200, 500}) {
            ReactorTickResult result = ReactorModel.tick(state, parameters, ports(flow));
            double expected = Math.min(flow, 200);
            assertEquals(expected, result.convertedCoolant(), 1e-9);
            assertEquals(expected, result.removedHeat(), 1e-9);
            assertTrue(result.columns().get(new ColumnKey(0, 0)).netHeatLoad() >= 500 - expected - 1e-9);
        }
        ReactorTickResult duplicate = ReactorModel.tick(state, parameters, new ReactorTickInput(
                List.of(CoolantPort.cold("cold", 100), CoolantPort.cold("cold", 100),
                        CoolantPort.hot("hot", 100), CoolantPort.hot("hot", 100)), Map.of(), 1, false));
        assertEquals(100, duplicate.convertedCoolant(), 1e-9);
        assertEquals(2, duplicate.duplicatePortCount());
        pass("N-07");
    }

    @Test
    void n08FailedFuelPropagatesOnlyFourWaysAndCoolingBlocksTargetDamage() {
        ColumnKey failed = new ColumnKey(0, 0);
        ColumnKey north = new ColumnKey(0, -1);
        ColumnKey east = new ColumnKey(1, 0);
        ColumnKey diagonal = new ColumnKey(1, 1);
        ReactorSnapshot state = snapshot(ColumnState.fuel(failed, 1, 0, 4),
                ColumnState.fuel(north, 1, 1, 0), ColumnState.fuel(east, 1, 1, 0),
                ColumnState.fuel(diagonal, 1, 1, 0));
        ReactorTickResult result = tick(state, ReactorTickInput.noCooling(1));
        assertTrue(result.columns().get(north).propagationHeatReceived() > 0);
        assertTrue(result.columns().get(east).propagationHeatReceived() > 0);
        assertEquals(0, result.columns().get(diagonal).propagationHeatReceived(), 1e-12);

        ReactorTickInput targetCooling = new ReactorTickInput(
                List.of(CoolantPort.cold("cold", 100), CoolantPort.hot("hot", 100)),
                Map.of(failed, 0.0, north, 100.0, east, 100.0), 1, false);
        ReactorTickResult cooled = tick(state, targetCooling);
        assertEquals(0, cooled.columns().get(north).netHeatLoad(), 1e-12);
        assertEquals(0, cooled.columns().get(east).netHeatLoad(), 1e-12);
        pass("N-08");
    }

    @Test
    void n09ControlRodDamageJamsAtActualDepthAndDoesNotPropagate() {
        ReactorParameters parameters = DEFAULTS.toBuilder().fuelColumnDamageRate(1).build();
        ColumnKey failed = new ColumnKey(0, 0);
        ColumnKey control = new ColumnKey(1, 0);
        ReactorSnapshot state = snapshot(ColumnState.fuel(failed, 1, 0, 4),
                new ColumnState(control,
                        com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnKind.CONTROL_ROD,
                        0, 0, .1, .35, .35, 0, false),
                ColumnState.fuel(new ColumnKey(2, 0), 1, 1, 0));
        ReactorTickResult result = ReactorModel.tick(state, parameters, ReactorTickInput.noCooling(1));
        ColumnState jammed = result.next().columns().get(control);
        assertEquals(0, jammed.controlRodColumnIntegrity(), 1e-12);
        assertTrue(jammed.jammed());
        assertEquals(.35, jammed.jammedDepth(), 1e-12);
        assertEquals(0, result.columns().get(new ColumnKey(2, 0)).propagationHeatReceived(), 1e-12);
        pass("N-09");
    }

    @Test
    void n10MeltdownCoverageUsesOnlyEffectiveFuelColumnsAt19_20_21Percent() {
        assertFalse(coverageScenario(19).meltdownDanger());
        assertTrue(coverageScenario(20).meltdownDanger());
        assertTrue(coverageScenario(21).meltdownDanger());
        pass("N-10");
    }

    @Test
    void n11ScramOrCoolingPausesMeltdownButNeverRewindsProgress() {
        ReactorTickResult first = coverageScenario(20);
        double progress = first.next().meltdownProgress();
        ReactorTickResult scram = tick(first.next(), new ReactorTickInput(
                List.of(), Map.of(), 1, true));
        assertEquals(progress, scram.next().meltdownProgress(), 1e-12);
        ReactorTickResult resumed = tick(scram.next(), new ReactorTickInput(List.of(), Map.of(), 1, false));
        assertTrue(resumed.next().meltdownProgress() > progress);
        pass("N-11");
    }

    @Test
    void n12OnlyFullRepairResetsMeltdownAndRepairRuleIsFixed() {
        ColumnKey fuel = new ColumnKey(0, 0);
        ColumnKey control = new ColumnKey(1, 0);
        ReactorSnapshot damaged = new ReactorSnapshot(Map.of(
                fuel, ColumnState.fuel(fuel, 1, .5, 0),
                control, new ColumnState(control,
                        com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnKind.CONTROL_ROD,
                        0, 0, .5, .4, .4, 0, false)), 10, true, 10);
        ColumnState repairedFuel = ReactorModel.repair(damaged.columns().get(fuel), 1, true, 1);
        ColumnState repairedControl = ReactorModel.repair(damaged.columns().get(control), 1, true, 1);
        assertEquals(.5, ReactorModel.repair(damaged.columns().get(fuel), 1, false, 1).fuelColumnIntegrity(), 1e-12);
        assertEquals(.75, repairedFuel.fuelColumnIntegrity(), 1e-12);
        assertEquals(.75, repairedControl.controlRodColumnIntegrity(), 1e-12);
        assertNotEquals(0, damaged.resetMeltdownIfAllRepaired().meltdownProgress());
        ReactorSnapshot fullyRepaired = new ReactorSnapshot(Map.of(
                fuel, ColumnState.fuel(fuel, 1, 1, 0),
                control, new ColumnState(control,
                        com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnKind.CONTROL_ROD,
                        0, 0, 1, .4, .4, 0, false)), 10, true, 10);
        assertEquals(0, fullyRepaired.resetMeltdownIfAllRepaired().meltdownProgress(), 1e-12);
        assertFalse(fullyRepaired.resetMeltdownIfAllRepaired().meltdownTriggered());
        pass("N-12");
    }

    @Test
    void n13HeightScalesHeatBurnAndRepairButKeepsTheoreticalLifeConstant() {
        double firstHeat = 0;
        for (int height : new int[]{1, 3, 5}) {
            ReactorSnapshot state = snapshot(ColumnState.fuel(new ColumnKey(0, 0), height, 1, 0));
            ReactorTickResult first = ReactorModel.tick(state, DEFAULTS,
                    ports(height, height));
            if (height == 1) {
                firstHeat = first.generatedHeat();
            }
            assertEquals(height, first.generatedHeat() / firstHeat, 1e-9);
            assertEquals(height, first.plannedBurn() / tick(snapshot(
                    ColumnState.fuel(new ColumnKey(0, 0), 1, 1, 0)), ports(1)).plannedBurn(), 1e-9);
            ColumnState repaired = ReactorModel.repair(ColumnState.fuel(new ColumnKey(0, 0), height, .5, 0), height, true, 1);
            assertEquals(.5 + .25 / height, repaired.fuelColumnIntegrity(), 1e-12);
        }
        pass("N-13");
    }

    @Test
    void n14RandomOrderAndNbtRoundTripAreDeterministicAndHaveNoPropagationMarker() {
        Random random = new Random(20260818);
        Map<ColumnKey, ColumnState> columns = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            ColumnKey key = new ColumnKey(i, 0);
            columns.put(key, ColumnState.fuel(key, 1, .5 + random.nextDouble() / 2, random.nextDouble()));
        }
        ReactorSnapshot continuous = new ReactorSnapshot(columns, 0, false, 0);
        ReactorSnapshot reloaded = continuous;
        for (int i = 0; i < 8; i++) {
            continuous = tick(continuous, ReactorTickInput.noCooling(1)).next();
            reloaded = ReactorSnapshotCodec.decode(ReactorSnapshotCodec.encode(
                    tick(reloaded, ReactorTickInput.noCooling(1)).next()));
        }
        assertEquals(continuous, reloaded);
        assertFalse(ReactorSnapshotCodec.encode(continuous).contains("Propagation"));
        pass("N-14");
    }

    @Test
    void parameterValidationRejectsNonFiniteOrInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> DEFAULTS.toBuilder().baseHeatPerFuel(Double.NaN).build());
        assertThrows(IllegalArgumentException.class, () -> DEFAULTS.toBuilder().meltdownTriggerFraction(1.1).build());
        assertThrows(IllegalArgumentException.class, () -> DEFAULTS.toBuilder().meltdownCountdownTicks(0).build());
        pass("PARAMETERS");
    }

    private static ReactorTickResult coverageScenario(int coveredCount) {
        Map<ColumnKey, ColumnState> columns = new LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            ColumnKey target = new ColumnKey(i * 3, 0);
            columns.put(target, ColumnState.fuel(target, 1, 1, 0));
        }
        for (int i = 0; i < coveredCount; i++) {
            ColumnKey source = new ColumnKey(i * 3 - 1, 0);
            columns.put(source, ColumnState.fuel(source, 1, 0, 4));
        }
        return tick(snapshot(columns), ReactorTickInput.noCooling(1));
    }

    private static ReactorTickResult tick(ReactorSnapshot state, ReactorTickInput input) {
        return ReactorModel.tick(state, DEFAULTS, input);
    }

    private static ReactorSnapshot snapshot(ColumnState... columns) {
        Map<ColumnKey, ColumnState> result = new LinkedHashMap<>();
        for (ColumnState column : columns) {
            result.put(column.key(), column);
        }
        return snapshot(result);
    }

    private static ReactorSnapshot snapshot(Map<ColumnKey, ColumnState> columns) {
        return new ReactorSnapshot(columns, 0, false, 0);
    }

    private static ReactorTickInput ports(double flow) {
        return ports(flow, 1);
    }

    private static ReactorTickInput ports(double flow, double internalHeight) {
        int count = (int) Math.ceil(flow / 100.0);
        List<CoolantPort> ports = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double amount = Math.min(100, flow - i * 100);
            ports.add(CoolantPort.cold("cold-" + i, Math.max(0, amount)));
            ports.add(CoolantPort.hot("hot-" + i, Math.max(0, amount)));
        }
        return new ReactorTickInput(ports, Map.of(), internalHeight, false);
    }

    private static List<CoolantPort> firstInputPorts() {
        return List.of(CoolantPort.cold("cold", 100), CoolantPort.hot("hot", 100));
    }

    private static void pass(String name) {
        SCENARIOS.add(name + ",PASS");
    }

    private static final class TreeMapForTest extends java.util.TreeMap<ColumnKey, ColumnState> {
    }
}
