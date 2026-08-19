package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P1BalanceContractTest {
    private static final Path SERVER_CONFIG_SOURCE = Path.of(
            "src", "main", "java", "com", "iksxh", "create_nuclear_industry", "config", "P1ServerConfig.java");
    private static final Path P0_PARAMETERS_SOURCE = Path.of(
            "src", "main", "java", "com", "iksxh", "create_nuclear_industry", "p0probe", "numeric",
            "ReactorParameters.java");

    @Test
    void serverConfigUsesFrozenDefaultsAndHasNoAggregateFlowKey() throws IOException {
        String source = Files.readString(SERVER_CONFIG_SOURCE);

        assertTrue(source.contains("defineInRange(\"baseHeatPerFuelBlockHuPerTick\", 1.0D"));
        assertTrue(source.contains("defineInRange(\"fuelBurnTimeHours\", 3.0D"));
        assertTrue(source.contains("defineInRange(\"coolantAbsorptionHuPerMb\", 0.5D"));
        assertTrue(source.contains("defineInRange(\"perPortFlowMbPerTick\", 128"));
        assertTrue(source.contains("defineInRange(\"damageHeatThresholdHuPerTick\", 0.25D"));
        assertTrue(source.contains("defineInRange(\"damageRatePerTickHuLoad\", 0.0000005D"));
        assertTrue(source.contains("defineInRange(\"damageTransferRate\", 0.25D"));
        assertTrue(source.contains("defineInRange(\"meltdownTriggerFraction\", 0.20D"));
        assertTrue(source.contains("defineInRange(\"meltdownCountdownTicks\", 900"));
        assertFalse(source.contains("totalFlowCapMbPerTick"));
        assertFalse(source.contains("coolantTotalFlowCap"));
    }

    @Test
    void p0PrototypeHasNoAggregateFlowParameterAndUsesFrozenDefaults() throws IOException {
        String source = Files.readString(P0_PARAMETERS_SOURCE);
        var parameters = com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorParameters.defaults();

        assertEquals(1.0D, parameters.baseHeatPerFuel());
        assertEquals(3.0D, parameters.burnHoursPerBlock());
        assertEquals(0.5D, parameters.coolantAbsorptionHuPerMb());
        assertEquals(128.0D, parameters.coolantMaxFlowPerPort());
        assertEquals(0.25D, parameters.fuelColumnDamageHeatThreshold());
        assertEquals(0.0000005D, parameters.fuelColumnDamageRate());
        assertEquals(0.25D, parameters.fuelColumnDamageTransferRate());
        assertEquals(0.20D, parameters.meltdownTriggerFraction());
        assertEquals(900, parameters.meltdownCountdownTicks());
        assertFalse(source.contains("coolantTotalFlowCap"));
        assertFalse(source.contains("totalFlowCap"));
    }

    @Test
    void formalSimulationDefaultsKeepUnchangedCapsAndUseNewDamageRate() {
        ReactorSimulationParameters parameters = ReactorSimulationParameters.defaults();

        assertEquals(1.0D, parameters.baseHeatPerFuelBlockHuPerTick());
        assertEquals(3.0D, parameters.burnHoursPerBlock());
        assertEquals(0.25D, parameters.damageHeatThresholdHuPerTick());
        assertEquals(0.0000005D, parameters.damageRatePerTickHuLoad());
        assertEquals(0.25D, parameters.damageTransferRate());
        assertEquals(0.20D, parameters.meltdownTriggerFraction());
        assertEquals(900, parameters.meltdownCountdownTicks());
        assertEquals(1.0D, parameters.controlResponseExponent());
        assertEquals(10.0D, parameters.overclockHeatMultiplier());
        assertEquals(10.0D, parameters.overclockBurnMultiplier());
        assertEquals(0.15D, parameters.overclockFeedbackGain());
        assertEquals(0.5D, parameters.overclockFeedbackExponent());
        assertEquals(20.0D, parameters.totalHeatMultiplierCap());
    }
}
