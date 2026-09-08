package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证损伤倍率配置的缺项补默认、联合回退和纯模拟入口校验。 */
class P1DamageBalanceConfigurationTest {
    private static final double EPSILON = 1.0E-12D;

    @Test
    void missingDamageMultiplierFieldsFillTheirOwnDefaults() {
        P1ServerConfig.DamageMultipliers bothMissing =
                P1ServerConfig.resolveDamageMultipliers(null, null);
        P1ServerConfig.DamageMultipliers burnMissing =
                P1ServerConfig.resolveDamageMultipliers(1.5D, null);
        P1ServerConfig.DamageMultipliers heatMissing =
                P1ServerConfig.resolveDamageMultipliers(null, 5.0D);

        assertEquals(2.0D, bothMissing.heatMultiplier(), EPSILON);
        assertEquals(3.0D, bothMissing.burnMultiplier(), EPSILON);
        assertEquals(1.5D, burnMissing.heatMultiplier(), EPSILON);
        assertEquals(3.0D, burnMissing.burnMultiplier(), EPSILON);
        assertEquals(2.0D, heatMissing.heatMultiplier(), EPSILON);
        assertEquals(5.0D, heatMissing.burnMultiplier(), EPSILON);
    }

    @Test
    void validCustomDamageEndpointsArePreserved() {
        P1ServerConfig.DamageMultipliers custom =
                P1ServerConfig.resolveDamageMultipliers(3.0D, 5.0D);

        assertEquals(3.0D, custom.heatMultiplier(), EPSILON);
        assertEquals(5.0D, custom.burnMultiplier(), EPSILON);
    }

    @Test
    void invalidDamageEndpointPairsFallBackAsAWholePair() {
        assertFallback(Double.NaN, 3.0D);
        assertFallback(Double.POSITIVE_INFINITY, 3.0D);
        assertFallback(1.0D, 3.0D);
        assertFallback(0.5D, 3.0D);
        assertFallback(2.0D, 2.0D);
        assertFallback(3.0D, 2.0D);
        assertFallback(3.0D, Double.NaN);
    }

    @Test
    void pureSimulationParametersRejectInvalidDamageEndpointPairs() {
        assertThrows(IllegalArgumentException.class, () -> parameters(1.0D, 3.0D));
        assertThrows(IllegalArgumentException.class, () -> parameters(2.0D, 2.0D));
        assertThrows(IllegalArgumentException.class, () -> parameters(3.0D, 2.0D));
        assertThrows(IllegalArgumentException.class, () -> parameters(Double.NaN, 3.0D));
        assertThrows(IllegalArgumentException.class, () -> parameters(2.0D, Double.POSITIVE_INFINITY));
    }

    private static void assertFallback(Double heat, Double burn) {
        P1ServerConfig.DamageMultipliers result =
                P1ServerConfig.resolveDamageMultipliers(heat, burn);
        assertEquals(2.0D, result.heatMultiplier(), EPSILON);
        assertEquals(3.0D, result.burnMultiplier(), EPSILON);
    }

    private static ReactorSimulationParameters parameters(double heat, double burn) {
        return new ReactorSimulationParameters(
                1.0D, 3.0D, 0.25D, 0.0000005D,
                heat, burn,
                0.25D, 0.0D, 0.20D, 900,
                1.0D, 10.0D, 10.0D, 0.15D, 0.5D, 20.0D
        );
    }
}
