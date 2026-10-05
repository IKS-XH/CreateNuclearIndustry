package com.iksxh.create_nuclear_industry.heat;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** 按外部供液、排液与实际发布热量验证账本；真实锅炉及生命周期另由 GameTest 覆盖。 */
final class HeatExchangerStateTest {
    private static final HeatExchangerState.Settings DEFAULT = new HeatExchangerState.Settings(18, 1, 40, .5);

    @Test void dedicatedSourcePaysConfiguredRateAboveEighteen() {
        var source = new HeatExchangerState();
        var boosted = new HeatExchangerState.Settings(18, 2, 40, 1);
        source.fillHot(100, false);
        assertEquals(0, source.claimDedicated(1, 40, boosted));
        assertEquals(36, source.converted());
        assertEquals(36, source.reserve());
        assertEquals(36, source.claimDedicated(2, 40, boosted));
        assertEquals(0, source.claimDedicated(2, 40, boosted));
    }

    @Test void smallerHotAndColdCapacitiesPreserveOldInventoryAndLimitNewTransactions() {
        var state = new HeatExchangerState();
        var old = new CompoundTag();
        old.putInt("Hot", 300);
        old.putInt("Cold", 350);
        state.load(old);
        var configured = new HeatExchangerState.Settings(6, 2, 20, .5, 200, 300, 4);
        state.setSettings(configured);
        assertEquals(300, state.hot());
        assertEquals(350, state.cold());
        assertEquals(0, state.fillHot(20, false));
        assertEquals(50, state.drainCold(50, false));
        assertEquals(50, state.drainCold(50, false));
        state.tick(1, true, configured);
        assertEquals(24, state.converted());
        assertEquals(274, state.cold());
        var restored = new HeatExchangerState();
        restored.load(state.save());
        restored.setSettings(configured);
        assertEquals(550, restored.hot() + restored.cold());
        assertEquals(274, restored.cold());
    }

    @Test void densityOrWindowChangeNeverRevaluesExistingReserve() {
        var state = new HeatExchangerState();
        var saved = new CompoundTag();
        saved.putInt("Hot", 100);
        saved.putDouble("ReserveHu", 20);
        saved.putDouble("FlowFraction", .5);
        saved.putDouble("SettingsDensity", .5);
        saved.putInt("SettingsBufferTicks", 40);
        state.load(saved);
        var changed = new HeatExchangerState.Settings(18, 1, 20, 2);
        state.setSettings(changed);
        assertEquals(0, state.reserve());
        assertEquals(0, state.save().getDouble("FlowFraction"));
        assertEquals(100, state.hot());
        assertFalse(new HeatExchangerState.Settings(18, Double.NaN, 40, .5).valid());
    }

    @Test void sustainedInputsConserveMassAndEnergyAndSettleAtNineAndEighteen() {
        for (int input : new int[]{18, 36}) {
            var s = new HeatExchangerState();
            long accepted = 0, returned = 0;
            double emitted = 0;
            for (int t = 0; t < 400; t++) {
                accepted += s.fillHot(input, false);
                s.tick(t, true, DEFAULT);
                returned += s.drainCold(4000, false);
                emitted += Math.max(0, s.heat());
                assertEquals(accepted, returned + s.hot() + s.cold());
                assertEquals((accepted - s.hot()) * .5, emitted + s.reserve(), 1e-9);
                if (t >= 250) {
                    assertEquals(input / 2, s.heat());
                    assertEquals(input, s.converted());
                    assertEquals("running", s.status());
                }
            }
            assertEquals(input == 18 ? 360 : 720, s.reserve());
        }
    }

    @Test void publishingUsesPreviouslyPaidHeatAndRepeatedQueriesOrTicksArePure() {
        var s = new HeatExchangerState();
        s.fillHot(4000, false);
        for (int t = 0; t < 3; t++) {
            s.tick(t, true, DEFAULT);
            assertEquals(-1, s.heat());
            assertEquals(36, s.converted());
        }
        assertEquals(54, s.reserve());
        s.tick(3, true, DEFAULT);
        assertEquals(1, s.heat());
        assertEquals(71, s.reserve());
        assertEquals(144, s.cold());
        var before = s.save();
        for (int i = 0; i < 100; i++) {
            s.fillHot(4000, true);
            s.drainCold(4000, true);
            s.remainingTicks();
            s.tick(3, true, DEFAULT);
        }
        assertEquals(before, s.save());
    }

    @Test void fractionalInputOscillatesOnlyBetweenAdjacentSupportedLevels() {
        for (int input : new int[]{1, 19}) {
            var s = new HeatExchangerState();
            int minimum = 18, maximum = 0;
            for (int t = 0; t < 500; t++) {
                assertEquals(input, s.fillHot(input, false));
                s.tick(t, true, DEFAULT);
                s.drainCold(4000, false);
                if (t >= 300) {
                    minimum = Math.min(minimum, Math.max(0, s.heat()));
                    maximum = Math.max(maximum, Math.max(0, s.heat()));
                }
            }
            assertEquals(input / 2, minimum);
            assertEquals(input / 2 + 1, maximum);
        }
    }

    @Test void pulsedInputAndMidRunSaveRestoreKeepPaidHeatAndEventuallyExpire() {
        var s = new HeatExchangerState();
        long accepted = 0, returned = 0;
        double emitted = 0;
        for (int t = 0; t < 200; t++) {
            if (t % 10 == 0) accepted += s.fillHot(180, false);
            s.tick(t, true, DEFAULT);
            returned += s.drainCold(4000, false);
            emitted += Math.max(0, s.heat());
            assertEquals(accepted, returned + s.hot() + s.cold());
            assertEquals((accepted - s.hot()) * .5, emitted + s.reserve(), 1e-9);
            if (t >= 180) assertTrue(s.heat() > 0 && s.heat() < 18);
            if (t == 55) {
                var saved = s.save();
                s = new HeatExchangerState();
                s.load(saved);
                assertEquals(saved, s.save());
            }
        }
        for (int t = 200; t <= 250; t++) {
            s.tick(t, true, DEFAULT);
            returned += s.drainCold(4000, false);
        }
        assertEquals(accepted, returned + s.hot() + s.cold());
        assertEquals(-1, s.heat());
        assertEquals(0, s.reserve());
        assertEquals(0, s.remainingTicks());
    }

    @Test void blockedReturnExpiresWithinFortyTicksAndRetainsUnconvertedHotFluid() {
        var s = warmedAtFullInput();
        var blocked = s.save();
        blocked.putInt("Hot", 1000);
        blocked.putInt("Cold", 4000);
        s.load(blocked);
        for (int t = 400; t <= 439; t++) {
            s.tick(t, true, DEFAULT);
            assertEquals(0, s.converted());
            assertEquals(1000, s.hot());
            assertEquals(4000, s.cold());
            if (s.heat() > 0) assertEquals("residual", s.status());
        }
        assertEquals(-1, s.heat());
        assertEquals(0, s.reserve());
        assertEquals(0, s.remainingTicks());
        s.drainCold(1, false);
        s.tick(440, true, DEFAULT);
        assertEquals(1, s.converted());
        assertEquals(.5, s.reserve());
        assertEquals(-1, s.heat());
    }

    @Test void unloadingAndPortableRoundTripsPreserveTheAbsoluteExpiry() {
        var s = warmedAtFullInput();
        for (int t = 400; t <= 404; t++) s.tick(t, true, DEFAULT);
        assertEquals(35, s.remainingTicks());
        var restored = new HeatExchangerState();
        restored.load(s.save());
        assertEquals(-1, restored.heat());
        assertEquals(s.save(), restored.save());
        var again = new HeatExchangerState();
        again.load(restored.save());
        again.tick(440, true, DEFAULT);
        assertEquals(0, again.reserve());
        assertEquals(0, again.remainingTicks());
        assertEquals(-1, again.heat());
    }

    @Test void freshConversionAfterSkippedDeadlineCannotSpendOrReviveExpiredHeat() {
        var s = warmedAtFullInput();
        for (int t = 400; t <= 437; t++) s.tick(t, true, DEFAULT);
        assertTrue(s.reserve() > 40);
        var restored = new HeatExchangerState();
        restored.load(s.save());
        restored.fillHot(1, false);
        restored.tick(440, true, DEFAULT);
        assertEquals(-1, restored.heat());
        assertEquals(1, restored.converted());
        assertEquals(.5, restored.reserve());
        assertEquals(40, restored.remainingTicks());
    }

    @Test void legacyMigrationSurvivesSavingBeforeFirstTickAndDoesNotRefreshFromLoadTime() {
        var legacy = new CompoundTag();
        legacy.putDouble("ReserveHu", 720);
        legacy.putDouble("LastRate", 18);
        legacy.putLong("LastTick", 10);
        var first = new HeatExchangerState();
        first.load(legacy);
        assertFalse(first.save().contains("NoFlowDeadlineTick"));
        var restored = new HeatExchangerState();
        restored.load(first.save());
        restored.tick(11, false, DEFAULT);
        assertEquals(702, restored.reserve());
        assertEquals(39, restored.remainingTicks());
        assertEquals(50, restored.save().getLong("NoFlowDeadlineTick"));
        restored.tick(50, true, DEFAULT);
        assertEquals(0, restored.reserve());
        assertEquals(-1, restored.heat());

        legacy.putDouble("ReserveHu", 20);
        restored.load(legacy);
        restored.fillHot(36, false);
        restored.tick(13, true, DEFAULT);
        assertEquals(-1, restored.heat());
        assertEquals(18, restored.reserve());
        assertEquals(36, restored.converted());
    }

    @Test void absentLoadDoesNotConvertAndInvalidConfigurationCannotChangeFluidAmounts() {
        var s = new HeatExchangerState();
        s.fillHot(4000, false);
        for (int t = 0; t < 100; t++) s.tick(t, false, DEFAULT);
        assertEquals(4000, s.hot());
        assertEquals(0, s.cold());
        s.tick(100, true, DEFAULT);
        assertEquals(36, s.converted());
        s.tick(101, false, DEFAULT);
        assertEquals(0, s.reserve());
        s.tick(102, true, new HeatExchangerState.Settings(18, 1, 40, 0));
        assertEquals(-1, s.heat());
        assertEquals(3964, s.hot());
        assertEquals(36, s.cold());
        assertFalse(new HeatExchangerState.Settings(18, Double.NaN, 40, .5).valid());
    }

    @Test void fractionalDensityCanReachItsThresholdWithoutEmittingUnpaidHeat() {
        var s = new HeatExchangerState();
        var cfg = new HeatExchangerState.Settings(1, 1, 40, .3);
        s.fillHot(4000, false);
        double emitted = 0;
        for (int t = 0; t < 300; t++) {
            s.tick(t, true, cfg);
            s.drainCold(4000, false);
            emitted += Math.max(0, s.heat());
            assertTrue(Double.isFinite(s.reserve()) && s.reserve() >= 0 && s.reserve() <= 40);
            assertTrue(emitted + s.reserve() <= (4000 - s.hot()) * .3 + 1e-8);
        }
        assertTrue(emitted > 100, "非整除密度不能永远停留在阈值以下");
    }

    @Test void veryDenseSingleMillibucketIsBoundedAndPartialReturnSpaceIsConserved() {
        var s = new HeatExchangerState();
        var dense = new HeatExchangerState.Settings(1, 1, 40, 100);
        s.fillHot(1, false);
        int convertedAt = -1;
        for (int t = 0; t < 110; t++) {
            s.tick(t, true, dense);
            if (s.converted() == 1) { convertedAt = t; break; }
        }
        assertTrue(convertedAt >= 0);
        assertEquals(1, s.cold());
        assertEquals(0, s.hot());
        assertEquals(40, s.reserve());
        s.tick(convertedAt + 1, true, dense);
        assertEquals(1, s.heat());
        for (int t = convertedAt + 2; t <= convertedAt + 40; t++) s.tick(t, true, dense);
        assertEquals(0, s.reserve());
        assertEquals(-1, s.heat());

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

    @Test void customSettingsScaleOutputAndConfigurationChangesNeverCreateHeat() {
        var custom = new HeatExchangerState.Settings(6, 2, 20, .5);
        var s = new HeatExchangerState();
        for (int t = 0; t < 200; t++) {
            s.fillHot(8, false);
            s.tick(t, true, custom);
            s.drainCold(4000, false);
        }
        assertEquals(2, s.heat());
        assertEquals(80, s.reserve());
        s = warmedAtFullInput();
        s.tick(400, true, custom);
        assertEquals(-1, s.heat());
        assertEquals(0, s.reserve());
        assertTrue(s.remainingTicks() <= 20);
        var before = s.save();
        s.tick(400, true, DEFAULT);
        assertEquals(before, s.save());
        s.tick(399, true, DEFAULT);
        assertEquals(-1, s.heat());
        assertEquals(0, s.reserve());
    }

    @Test void remoteInventoryCanFeedAnEmptyAndColdFullWorkingMemberWithoutSharingReserve() {
        var worker = new HeatExchangerState();
        var remote = new HeatExchangerState();
        var filled = worker.save();
        filled.putInt("Cold", 4000);
        worker.load(filled);
        remote.fillHot(4000, false);
        HeatExchangerState.Exchange shared = new HeatExchangerState.Exchange() {
            @Override public int hot() { return worker.hot() + remote.hot(); }
            @Override public int coldSpace() { return 8000 - worker.cold() - remote.cold(); }
            @Override public int convert(int amount) {
                int moved = Math.min(amount, Math.min(hot(), coldSpace()));
                var source = remote.save();
                source.putInt("Hot", source.getInt("Hot") - moved);
                source.putInt("Cold", source.getInt("Cold") + moved);
                remote.load(source);
                return moved;
            }
        };
        double emitted = 0;
        for (int tick = 0; tick < 80; tick++) {
            worker.tick(tick, true, DEFAULT, shared);
            emitted += Math.max(0, worker.heat());
        }
        assertEquals(0, worker.hot());
        assertEquals(4000, worker.cold());
        assertTrue(worker.heat() > 0 && worker.converted() > 0);
        assertEquals(0, remote.reserve());
        assertEquals(4000 - remote.hot(), remote.cold());
        assertEquals((4000 - remote.hot()) * .5, worker.reserve() + emitted, 1e-9);
    }

    /** 实际逐tick注入36mB并排出冷液，避免以预灌储备代替有偿升温。 */
    private static HeatExchangerState warmedAtFullInput() {
        var s = new HeatExchangerState();
        for (int t = 0; t < 400; t++) {
            assertEquals(36, s.fillHot(36, false));
            s.tick(t, true, DEFAULT);
            s.drainCold(4000, false);
        }
        assertEquals(18, s.heat());
        assertEquals(720, s.reserve());
        return s;
    }
}
