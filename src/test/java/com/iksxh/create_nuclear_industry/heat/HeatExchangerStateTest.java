package com.iksxh.create_nuclear_industry.heat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** 以真实支付量验证热账本；世界、能力及原生锅炉接线另由 GameTest 覆盖。 */
final class HeatExchangerStateTest {
    private static final HeatExchangerState.Settings DEFAULT = new HeatExchangerState.Settings(18, 1, 40, .5);

    @Test void fortyTicksMustBePaidBeforePublishingAndResidualEndsExactly() {
        var s = new HeatExchangerState();
        s.fillHot(1440, false);
        for (int t = 0; t < 40; t++) {
            s.tick(t, true, DEFAULT);
            assertEquals(-1, s.heat());
        }
        assertEquals(720, s.reserve());
        assertEquals(1440, s.cold());
        for (int t = 40; t < 80; t++) {
            s.tick(t, true, DEFAULT);
            assertEquals(18, s.heat());
        }
        s.tick(80, true, DEFAULT);
        assertEquals(-1, s.heat());
        assertEquals(0, s.reserve());
    }

    @Test void steadyStateAndRepeatedReadsConserveFluidAndHeat() {
        var s = new HeatExchangerState();
        s.fillHot(4000, false);
        for (int t = 0; t < 65; t++) s.tick(t, true, DEFAULT);
        assertEquals(2340, s.cold());
        assertEquals(1660, s.hot());
        assertEquals(720, s.reserve());
        var before = s.save();
        for (int i = 0; i < 100; i++) {
            assertEquals(18, s.heat());
            s.fillHot(4000, true);
            s.drainCold(4000, true);
        }
        assertEquals(before, s.save());
    }

    @Test void unloadedTimeAndPortableRoundTripCannotRefreshPaidHeat() {
        var s = new HeatExchangerState();
        s.fillHot(1440, false);
        for (int t = 0; t < 50; t++) s.tick(t, true, DEFAULT);
        var restored = new HeatExchangerState();
        restored.load(s.save());
        assertEquals(-1, restored.heat());
        restored.tick(69, true, DEFAULT);
        assertEquals(180, restored.reserve());
        for (int t = 70; t < 80; t++) restored.tick(t, true, DEFAULT);
        restored.tick(80, true, DEFAULT);
        assertEquals(-1, restored.heat());
    }

    @Test void absentLoadBlockedReturnAndTrickleNeverRefillForFree() {
        var s = new HeatExchangerState();
        s.fillHot(4000, false);
        for (int t = 0; t < 100; t++) s.tick(t, false, DEFAULT);
        assertEquals(4000, s.hot());
        for (int t = 100; t < 140; t++) s.tick(t, true, DEFAULT);
        for (int t = 140; t < 180; t++) s.tick(t, false, DEFAULT);
        assertEquals(0, s.reserve());
        assertEquals(2560, s.hot());
        var tag = s.save();
        tag.putInt("Cold", 4000);
        s.load(tag);
        s.tick(180, true, DEFAULT);
        assertEquals(2560, s.hot());
        assertEquals(-1, s.heat());
        s.drainCold(1, false);
        s.tick(181, true, DEFAULT);
        assertEquals(.5, s.reserve());
        assertEquals(-1, s.heat());
    }

    @Test void fractionalFlowIsFiniteAndPaidAndInvalidConfigurationStops() {
        var s = new HeatExchangerState();
        var fraction = new HeatExchangerState.Settings(1, 1, 40, .3);
        s.fillHot(4000, false);
        for (int t = 0; t < 300; t++) {
            s.tick(t, true, fraction);
            s.drainCold(4000, false);
            assertTrue(Double.isFinite(s.reserve()));
            assertTrue(s.reserve() >= 0 && s.reserve() <= 40);
        }
        assertEquals(300, (4000 - s.hot()) * .3, .31);
        s.tick(300, true, new HeatExchangerState.Settings(18, 1, 40, 0));
        assertEquals(-1, s.heat());
        assertEquals(0, s.reserve());
        assertFalse(new HeatExchangerState.Settings(18, Double.NaN, 40, .5).valid());
    }

    @Test void highDensityRoundingAndPartialReturnSpaceNeverCreditUnconvertedFluid() {
        var s = new HeatExchangerState();
        s.fillHot(1, false);
        var dense = new HeatExchangerState.Settings(1, 1, 40, 100);
        for (int t = 0; t < 100; t++) s.tick(t, true, dense);
        assertEquals(1, s.cold());
        assertEquals(40, s.reserve());
        int emitted = 0;
        for (int t = 100; t < 141; t++) {
            s.tick(t, true, dense);
            if (s.heat() > 0) emitted++;
        }
        assertEquals(40, emitted);
        assertEquals(0, s.reserve());
        var partial = new HeatExchangerState();
        var tag = partial.save();
        tag.putInt("Hot", 50);
        tag.putInt("Cold", 3993);
        partial.load(tag);
        partial.tick(0, true, DEFAULT);
        assertEquals(43, partial.hot());
        assertEquals(4000, partial.cold());
        assertEquals(3.5, partial.reserve());
    }

    @Test void smallerConfigurationAndDuplicateTickCannotCreateOrExtendHeat() {
        var s = new HeatExchangerState();
        s.fillHot(1440, false);
        for (int t = 0; t < 40; t++) s.tick(t, true, DEFAULT);
        s.tick(40, true, new HeatExchangerState.Settings(1, 1, 40, .5));
        assertEquals(39, s.reserve());
        var before = s.save();
        for (int i = 0; i < 10; i++) s.tick(40, true, DEFAULT);
        assertEquals(before, s.save());
        var restored = new HeatExchangerState();
        restored.load(s.save());
        restored.tick(100, true, DEFAULT);
        assertEquals(-1, restored.heat());
        assertEquals(0, restored.reserve());
    }
}
