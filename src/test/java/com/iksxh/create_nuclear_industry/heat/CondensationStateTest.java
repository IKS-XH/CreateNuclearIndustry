package com.iksxh.create_nuclear_industry.heat;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** 以库存守恒、模拟纯查询、阶段边界和当前格式恢复验证冷凝账本。 */
final class CondensationStateTest {
    private static final CondensationState.Settings CFG = CondensationState.Settings.DEFAULT;

    @Test void ratedCondensationConservesMassAndNeverProducesHeatOrRepeatsTick() {
        var s = new HeatExchangerState();
        assertEquals(200, s.fillInput(200, HeatExchangerMode.CONDENSATION, false));
        s.tickCondensation(1, CondensationState.Source.WATER, CFG);
        assertEquals(54, s.converted());
        assertEquals(200, s.hot() + s.cold());
        assertEquals(54, s.cold());
        assertEquals(-1, s.heat());
        assertEquals(0, s.reserve());
        assertEquals(0, s.claimDedicated(2, 100, s.settings()));
        var snapshot = s.save();
        s.tickCondensation(1, CondensationState.Source.WATER, CFG);
        assertEquals(snapshot, s.save());
    }

    @Test void simulateAndZeroAcceptanceDoNotChooseModeAndResidualHeatLocksMode() {
        var s = new HeatExchangerState();
        var snapshot = s.save();
        assertEquals(200, s.fillInput(200, HeatExchangerMode.CONDENSATION, true));
        assertEquals(0, s.fillInput(0, HeatExchangerMode.CONDENSATION, false));
        assertEquals(snapshot, s.save());
        s.fillHot(100, false);
        s.tick(1, true, s.settings());
        s.takeHotForConversion(100);
        s.drainCold(4000, false);
        assertTrue(s.reserve() > 0);
        assertEquals(0, s.fillInput(10, HeatExchangerMode.CONDENSATION, false));
        s.tick(50, false, s.settings());
        assertEquals(10, s.fillInput(10, HeatExchangerMode.CONDENSATION, false));
        assertEquals(0, s.fillHot(10, false));
    }

    @Test void smallPacketsAccumulateBoundedRecoveryRemainderThroughCurrentSaveRestore() {
        var cfg = new CondensationState.Settings(54, 333, 4000, 4000, 100000, 100000, 900000, 100000);
        var s = new HeatExchangerState();
        int recovered = 0;
        for (int tick = 1; tick <= 1000; tick++) {
            assertEquals(1, s.fillInput(1, HeatExchangerMode.CONDENSATION, false));
            s.tickCondensation(tick, CondensationState.Source.WATER, cfg);
            recovered += s.drainCold(4000, false);
            assertTrue(s.condensation().remainder() >= 0 && s.condensation().remainder() < 1000);
            if (tick == 500) {
                var saved = s.save();
                s = new HeatExchangerState();
                s.load(saved);
                assertEquals(saved, s.save());
            }
        }
        assertEquals(333, recovered);
        assertEquals(0, s.condensation().remainder());
        assertEquals(1000, s.condensation().consumed());
    }

    @Test void coldBudgetStopsAtStageBoundaryAndBlockedOutputDoesNotConsumeSource() {
        var cfg = new CondensationState.Settings(54, 1000, 4000, 4000, 61, 61, 91, 61);
        var s = new HeatExchangerState();
        s.fillInput(4000, HeatExchangerMode.CONDENSATION, false);
        s.tickCondensation(1, CondensationState.Source.ICE, cfg);
        var restored = new HeatExchangerState();
        restored.load(s.save());
        restored.tickCondensation(2, CondensationState.Source.ICE, cfg);
        assertEquals(7, restored.converted());
        assertEquals(61, restored.cold());
        assertTrue(restored.condensation().exhausted(cfg));
        assertEquals(61, restored.condensation().consumed());
        restored.tickCondensation(3, CondensationState.Source.ICE, cfg);
        assertEquals(0, restored.converted());
        restored.condensation().changedTo(CondensationState.Source.WATER);
        var blocked = restored.save();
        blocked.putInt("Cold", 4000);
        restored.load(blocked);
        restored.tickCondensation(4, CondensationState.Source.WATER, cfg);
        assertEquals(0, restored.condensation().consumed());
        assertEquals("water_full", restored.status());
        restored.drainCold(1, false);
        restored.tickCondensation(5, CondensationState.Source.WATER, cfg);
        assertEquals(1, restored.converted());
        assertEquals(4000, restored.cold());
        restored.tickCondensation(6, CondensationState.Source.NONE, cfg);
        assertEquals(0, restored.converted());
    }

    @Test void configuredCapacitiesPreserveInventoryAndInvalidSettingsPauseSafely() {
        var s = new HeatExchangerState();
        s.fillInput(4000, HeatExchangerMode.CONDENSATION, false);
        var cfg = new CondensationState.Settings(7, 500, 100, 100, 100, 100, 100, 100);
        s.setCondensationSettings(cfg);
        assertEquals(4000, s.hot());
        assertEquals(0, s.fillInput(1, HeatExchangerMode.CONDENSATION, false));
        s.tickCondensation(1, CondensationState.Source.BLUE_ICE, cfg);
        assertEquals(7, s.converted());
        assertEquals(3, s.cold());
        assertEquals(500, s.condensation().remainder());
        assertEquals(0, s.condensation().consumed());
        var invalid = new CondensationState.Settings(0, 1001, 100, 100, 1, 1, 1, 1);
        s.tickCondensation(2, CondensationState.Source.BLUE_ICE, invalid);
        assertEquals("invalid", s.status());
        assertEquals(3993, s.hot());
    }

    @Test void partialOutputSpacePreflightsRecoveredVolumeAndDoesNotConsumeRemainderWhenFull() {
        var s = new HeatExchangerState();
        s.fillInput(100, HeatExchangerMode.CONDENSATION, false);
        var saved = s.save(); saved.putInt("Cold", 3999); s.load(saved);
        var cfg = new CondensationState.Settings(54, 333, 4000, 4000, 100000, 100000, 900000, 100000);
        s.tickCondensation(1, CondensationState.Source.WATER, cfg);
        assertEquals(6, s.converted());
        assertEquals(94, s.hot());
        assertEquals(4000, s.cold());
        assertEquals(998, s.condensation().remainder());
        s.tickCondensation(2, CondensationState.Source.WATER, cfg);
        assertEquals(0, s.converted());
        assertEquals(998, s.condensation().remainder());
        assertEquals(6, s.condensation().consumed());
    }
}
