package com.iksxh.create_nuclear_industry.turbine.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 调用真实客户端累计相位，覆盖变速、零速与暂停；不启动世界或复制累计算法。 */
class TurbineAnimationVisualStateTest {
    @Test void constantRpmUsesCreateDegreesPerTickAndWrapsWholeTurns() {
        var phases = new TurbineAnimationVisualState.Phases<String>();
        assertEquals(0, phases.sample("shaft", 16, 0), 1e-9);
        assertEquals(96, phases.angle("shaft", 20), 1e-9);
        phases.sample("fullTurn", 60, 0);
        assertEquals(0, phases.angle("fullTurn", 20), 1e-9);
    }

    @Test void changingRpmAnchorsThePreviousAngleInsteadOfMultiplyingWorldAge() {
        var phases = new TurbineAnimationVisualState.Phases<String>();
        phases.sample("owner", 16, 1_000_000);
        assertEquals(45.6, phases.angle("owner", 1_000_009.5), 1e-8);
        assertEquals(48, phases.sample("owner", 32, 1_000_010), 1e-8);
        assertEquals(52.8, phases.angle("owner", 1_000_010.5), 1e-8);
        assertEquals(57.6, phases.sample("owner", 64, 1_000_011), 1e-8);
    }

    @Test void zeroRpmStopsAtTheCurrentAngleAndNegativeRpmReversesWithoutInertia() {
        var phases = new TurbineAnimationVisualState.Phases<String>();
        phases.sample("owner", 16, 0);
        assertEquals(48, phases.sample("owner", 0, 10), 1e-9);
        assertEquals(48, phases.angle("owner", 30), 1e-9);
        assertEquals(48, phases.sample("owner", -16, 30), 1e-9);
        assertEquals(24, phases.angle("owner", 35), 1e-9);
        phases.sample("reverse", -16, 0);
        assertEquals(312, phases.angle("reverse", 10), 1e-9);
    }

    @Test void sharedIdentityUsesOnePhaseAndRetractionDoesNotResumeOldMotion() {
        var phases = new TurbineAnimationVisualState.Phases<String>();
        phases.sample("generation1", 16, 0);
        assertEquals(48, phases.sample("generation1", 16, 10), 1e-9);
        assertEquals(48, phases.sample("generation1", 16, 10), 1e-9);
        assertEquals(0, phases.sample("generation2", 16, 10), 1e-9);
        phases.remove("generation1");
        assertEquals(0, phases.angle("generation1", 100), 1e-9);
        assertEquals(0, phases.sample("generation1", 16, 100), 1e-9);
        phases.clear();
        assertEquals(0, phases.angle("generation2", 100), 1e-9);
    }

    @Test void pauseFreezesBothTickAndPartialAndResumeDoesNotCatchUpUiTime() {
        var clock = new TurbineAnimationVisualState.Clock();
        clock.tick(false);
        assertEquals(1.4, clock.time(.4f, false), 1e-6);
        for (int i = 0; i < 100; i++) clock.tick(true);
        assertEquals(1.4, clock.time(.9f, true), 1e-6);
        assertEquals(1.4, clock.time(0, true), 1e-6);
        clock.tick(false);
        assertEquals(2.1, clock.time(.1f, false), 1e-6);
        clock.clear();
        assertEquals(0, clock.time(0, true), 1e-9);
    }

    @Test void invalidSamplesStopAndTimeRewindCannotCarryAnOldAngle() {
        var phases = new TurbineAnimationVisualState.Phases<String>();
        phases.sample("owner", 16, 0);
        assertEquals(48, phases.sample("owner", Float.NaN, 10), 1e-9);
        assertEquals(48, phases.angle("owner", 20), 1e-9);
        assertEquals(0, phases.sample("owner", 16, 5), 1e-9);
        assertEquals(4.8, phases.angle("owner", 6), 1e-9);
        assertEquals(0, phases.sample("owner", 16, Double.NaN), 1e-9);
        assertEquals(0, phases.angle("owner", 7), 1e-9);
    }
}
