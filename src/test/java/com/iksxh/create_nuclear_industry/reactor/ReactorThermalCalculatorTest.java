package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorThermalCalculatorTest {
    private static final ReactorSimulationParameters PARAMETERS = ReactorSimulationParameters.defaults();
    private static final CoreColumnPosition CENTER = new CoreColumnPosition(1, 1);

    @Test
    void sufficientCoolingRemovesAllGeneratedHeatAndPreventsDamage() {
        ReactorSnapshot snapshot = snapshot(fuel(1.0D, 0.0D));
        ReactorFissionResult fission = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, false);

        FuelColumnThermalResult result = settle(snapshot, fission.generatedHeatHu()).columns().get(CENTER);

        assertEquals(3.0D, result.generatedHeatHu(), 1.0E-12D);
        assertEquals(3.0D, result.removedHeatHu(), 1.0E-12D);
        assertEquals(0.0D, result.netHeatLoadHu(), 1.0E-12D);
        assertEquals(0.0D, result.integrityDamage(), 1.0E-12D);
        assertEquals(1.0D, result.nextState().integrity(), 1.0E-12D);
    }

    @Test
    void noCoolingDamagesFromHeatAboveThresholdAndCachesResidualHeat() {
        ReactorSnapshot snapshot = snapshot(fuel(1.0D, 0.0D));
        ReactorFissionResult fission = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, false);

        FuelColumnThermalResult result = settle(snapshot, 0.0D).columns().get(CENTER);
        double expectedDamage = (3.0D - PARAMETERS.damageHeatThresholdHuPerTick())
                * PARAMETERS.damageRatePerTickHuLoad();

        assertEquals(3.0D, result.netHeatLoadHu(), 1.0E-12D);
        assertEquals(expectedDamage, result.integrityDamage(), 1.0E-12D);
        assertEquals(3.0D, result.nextState().cachedHeatHu(), 1.0E-12D);
        assertEquals(1.0D - expectedDamage, result.nextState().integrity(), 1.0E-12D);
    }

    @Test
    void higherNetHeatLoadProducesSteeperIntegrityLoss() {
        ReactorSnapshot lowSnapshot = snapshot(fuel(1.0D, 0.0D));
        ReactorSnapshot highSnapshot = snapshot(fuel(0.5D, 2.0D));
        ReactorFissionResult lowFission = ReactorFissionCalculator.calculate(lowSnapshot, PARAMETERS, false);
        ReactorFissionResult highFission = ReactorFissionCalculator.calculate(highSnapshot, PARAMETERS, false);

        FuelColumnThermalResult low = settle(lowSnapshot, 2.5D).columns().get(CENTER);
        FuelColumnThermalResult high = ReactorThermalCalculator.settleFissionHeat(
                highSnapshot,
                highFission,
                Map.of(),
                PARAMETERS
        ).columns().get(CENTER);

        assertTrue(high.netHeatLoadHu() > low.netHeatLoadHu());
        assertTrue(high.integrityDamage() > low.integrityDamage());
        assertEquals(4.5D, high.generatedHeatHu(), 1.0E-12D);
    }

    @Test
    void overclockDoesNotDamageWhenItsSettledHeatIsFullyRemoved() {
        CoreColumnPosition east = new CoreColumnPosition(2, 1);
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(CENTER, fuel(1.0D, 0.0D), east, fuel(1.0D, 0.0D)),
                Map.of(),
                0L,
                0L,
                0L,
                false
        );
        ReactorFissionResult fission = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, false);
        ReactorThermalResult thermal = ReactorThermalCalculator.settleFissionHeat(
                snapshot,
                fission,
                Map.of(
                        CENTER, fission.columns().get(CENTER).generatedHeatHu(),
                        east, fission.columns().get(east).generatedHeatHu()
                ),
                PARAMETERS
        );

        assertTrue(fission.columns().get(CENTER).overclocked());
        assertEquals(0.0D, thermal.columns().get(CENTER).integrityDamage(), 1.0E-12D);
        assertEquals(1.0D, thermal.snapshot().fuelColumns().get(CENTER).integrity(), 1.0E-12D);
    }

    @Test
    void zeroIntegrityWithRemainingFuelStillSettlesHeatWithoutNegativeIntegrity() {
        ReactorSnapshot snapshot = snapshot(fuel(0.0D, 0.0D));
        ReactorFissionResult fission = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, false);

        FuelColumnThermalResult result = settle(snapshot, 0.0D).columns().get(CENTER);

        assertEquals(6.0D, result.generatedHeatHu(), 1.0E-12D);
        assertEquals(6.0D, result.netHeatLoadHu(), 1.0E-12D);
        assertEquals(0.0D, result.nextState().integrity(), 1.0E-12D);
        assertEquals(6.0D, result.nextState().cachedHeatHu(), 1.0E-12D);
    }

    @Test
    void exhaustedFuelCachedHeatUsesOrdinaryCoolingLedger() {
        ReactorSnapshot snapshot = snapshot(new FuelColumnState(
                FuelAssemblyState.installed(216_000, 216_000),
                0.0D,
                4.0D
        ));
        ReactorFissionResult fission = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, false);

        FuelColumnThermalResult result = ReactorThermalCalculator.settleFissionHeat(
                snapshot,
                fission,
                Map.of(CENTER, 1.0D),
                PARAMETERS
        ).columns().get(CENTER);

        assertEquals(0.0D, result.generatedHeatHu(), 1.0E-12D);
        assertEquals(1.0D, result.removedHeatHu(), 1.0E-12D);
        assertEquals(3.0D, result.netHeatLoadHu(), 1.0E-12D);
        assertEquals(0.0D, result.integrityDamage(), 1.0E-12D);
        assertEquals(3.0D, result.nextState().cachedHeatHu(), 1.0E-12D);
    }

    @Test
    void coolingRequestsAreCappedAndCannotTargetUnknownColumns() {
        ReactorSnapshot snapshot = snapshot(fuel(1.0D, 0.0D));
        ReactorFissionResult fission = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, false);
        FuelColumnThermalResult capped = settle(snapshot, 100.0D).columns().get(CENTER);

        assertEquals(capped.generatedHeatHu(), capped.removedHeatHu(), 1.0E-12D);
        assertThrows(IllegalArgumentException.class, () -> ReactorThermalCalculator.settleFissionHeat(
                snapshot,
                fission,
                Map.of(new CoreColumnPosition(0, 0), 1.0D),
                PARAMETERS
        ));
    }

    private static ReactorThermalResult settle(ReactorSnapshot snapshot, double removedHeat) {
        ReactorFissionResult fission = ReactorFissionCalculator.calculate(snapshot, PARAMETERS, false);
        return ReactorThermalCalculator.settleFissionHeat(
                snapshot,
                fission,
                Map.of(CENTER, removedHeat),
                PARAMETERS
        );
    }

    private static ReactorSnapshot snapshot(FuelColumnState fuel) {
        return ReactorSnapshot.singleFuelColumn(CENTER, fuel);
    }

    private static FuelColumnState fuel(double integrity, double cachedHeat) {
        return new FuelColumnState(FuelAssemblyState.installed(216_000, 0), integrity, cachedHeat);
    }
}
