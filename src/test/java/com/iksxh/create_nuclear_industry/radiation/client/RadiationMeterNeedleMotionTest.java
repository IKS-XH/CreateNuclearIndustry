package com.iksxh.create_nuclear_industry.radiation.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 只验证新增显示状态的时效、世界和插值不变量，不替代实体身份审查或游戏观察。 */
final class RadiationMeterNeedleMotionTest {
    private static final String DIMENSION = "test:world";

    private static RadiationMeterNeedleMotion.Sample complete(long time, double field) {
        return new RadiationMeterNeedleMotion.Sample(DIMENSION, time, field, true);
    }

    @Test void nonlinearMappingIsMonotonicFiniteAndClamped() {
        assertEquals(-65, RadiationMeterNeedleMotion.angleForField(0));
        assertEquals(65, RadiationMeterNeedleMotion.angleForField(10));
        assertEquals(65, RadiationMeterNeedleMotion.angleForField(100));
        double previous = -65;
        for (int i = 0; i <= 1000; i++) {
            double angle = RadiationMeterNeedleMotion.angleForField(i / 100.0);
            assertTrue(Double.isFinite(angle) && angle >= previous && angle <= 65);
            previous = angle;
        }
        assertTrue(RadiationMeterNeedleMotion.angleForField(.01) - (-65) > 10);
        assertEquals(-105, RadiationMeterNeedleMotion.angleForField(-1));
        assertEquals(-105, RadiationMeterNeedleMotion.angleForField(Double.NaN));
        assertEquals(-105, RadiationMeterNeedleMotion.angleForField(Double.POSITIVE_INFINITY));
    }

    @Test void unreadAndValidZeroAreDifferentStates() {
        Object world = new Object();
        var motion = new RadiationMeterNeedleMotion();
        motion.tick(world, DIMENSION, 0, false, null);
        assertFalse(motion.hasReading());
        assertEquals(-105, motion.targetAngle());
        motion.tick(world, DIMENSION, 1, false, complete(1, 0));
        assertTrue(motion.hasReading());
        assertEquals(-65, motion.targetAngle());
    }

    @Test void pendingDoesNotRefreshCompleteAndExpiresAfterSixtyTicks() {
        Object world = new Object();
        var motion = new RadiationMeterNeedleMotion();
        motion.tick(world, DIMENSION, 0, false, complete(0, 1));
        motion.tick(world, DIMENSION, 20, false, new RadiationMeterNeedleMotion.Sample(DIMENSION, 20, 0, false));
        assertEquals(0, motion.lastCompleteTime());
        motion.tick(world, DIMENSION, 60, false, null);
        assertTrue(motion.hasReading());
        motion.tick(world, DIMENSION, 61, false, new RadiationMeterNeedleMotion.Sample(DIMENSION, 61, 0, false));
        assertFalse(motion.hasReading());
        assertEquals(-105, motion.targetAngle());
    }

    @Test void oldFutureWrongDimensionAndInvalidSamplesNeverRenewDisplay() {
        Object world = new Object();
        var motion = new RadiationMeterNeedleMotion();
        motion.tick(world, DIMENSION, 10, false, complete(10, 1));
        motion.tick(world, DIMENSION, 11, false, complete(9, 9));
        motion.tick(world, DIMENSION, 12, false, complete(53, 9));
        motion.tick(world, DIMENSION, 13, false, complete(13, Double.NaN));
        motion.tick(world, DIMENSION, 14, false, complete(14, -1));
        motion.tick(world, DIMENSION, 15, false, new RadiationMeterNeedleMotion.Sample("wrong", 15, 9, true));
        assertEquals(10, motion.lastCompleteTime());
        motion.tick(world, DIMENSION, 71, false, complete(10, 1));
        assertFalse(motion.hasReading());
    }

    @Test void futureFortyIsAcceptedButFortyOneIsNot() {
        var first = new RadiationMeterNeedleMotion();
        first.tick(new Object(), DIMENSION, 10, false, complete(50, 1));
        assertTrue(first.hasReading());
        var second = new RadiationMeterNeedleMotion();
        second.tick(new Object(), DIMENSION, 10, false, complete(51, 1));
        assertFalse(second.hasReading());
    }

    @Test void worldLogoutAndRollbackResetWithoutReacceptingSameCachedSample() {
        Object world = new Object();
        var sample = complete(10, 1);
        var motion = new RadiationMeterNeedleMotion();
        motion.tick(world, DIMENSION, 10, false, sample);
        motion.tick(world, DIMENSION, 11, false, sample);
        motion.tick(world, DIMENSION, 9, false, sample);
        assertFalse(motion.hasReading());
        assertEquals(-105, motion.currentAngle());
        motion.tick(world, DIMENSION, 10, false, sample);
        assertFalse(motion.hasReading());
        motion.tick(world, DIMENSION, 11, false, complete(11, 1));
        assertTrue(motion.hasReading());
        motion.tick(new Object(), DIMENSION, 12, false, null);
        assertFalse(motion.hasReading());
        assertEquals(-105, motion.currentAngle());
        motion.tick(null, null, 0, false, null);
        assertFalse(motion.hasReading());
    }

    @Test void pauseFreezesSampleAcceptanceAndInterpolation() {
        Object world = new Object();
        var motion = new RadiationMeterNeedleMotion();
        motion.tick(world, DIMENSION, 0, false, complete(0, 0));
        motion.tick(world, DIMENSION, 1, false, complete(1, 10));
        double angle = motion.currentAngle();
        motion.tick(world, DIMENSION, 2, true, complete(2, 0));
        assertEquals(angle, motion.currentAngle());
        assertEquals(angle, motion.angle(0));
        assertEquals(angle, motion.angle(1));
        assertEquals(1, motion.lastCompleteTime());
        motion.tick(world, DIMENSION, 3, false, complete(3, 0));
        assertEquals(3, motion.lastCompleteTime());
    }

    @Test void interpolationStaysWithinEndpointsAndSettlesWithoutOvershoot() {
        Object world = new Object();
        var motion = new RadiationMeterNeedleMotion();
        for (int tick = 0; tick < 6; tick++) {
            motion.tick(world, DIMENSION, tick, false, complete(0, 10));
            assertTrue(motion.currentAngle() <= 65 && motion.currentAngle() >= motion.previousAngle());
            assertEquals(motion.previousAngle(), motion.angle(-1));
            assertEquals(motion.currentAngle(), motion.angle(2));
            assertTrue(motion.angle(.4) >= motion.previousAngle() && motion.angle(.4) <= motion.currentAngle());
        }
        assertTrue(65 - motion.currentAngle() <= .05 * (65 - (-105)));
        assertTrue(Double.isFinite(motion.angle(Double.NaN)));
    }

    @Test void renderBeforeWorldTickAndPauseTransitionNeverLeakInterpolation() {
        Object world = new Object();
        var motion = new RadiationMeterNeedleMotion();
        motion.tick(world, DIMENSION, 0, false, complete(0, 10));
        assertEquals(-105, motion.angleForRender(new Object(), DIMENSION, false, 1));
        assertEquals(-105, motion.angleForRender(null, null, false, 1));
        assertEquals(-105, motion.angleForRender(world, "wrong", false, 1));
        assertEquals(motion.currentAngle(), motion.angleForRender(world, DIMENSION, true, 0));
        assertEquals(motion.currentAngle(), motion.angleForRender(world, DIMENSION, true, 1));
        assertEquals(motion.angle(.5), motion.angleForRender(world, DIMENSION, false, .5));
    }
}
