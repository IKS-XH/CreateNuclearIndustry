package com.iksxh.create_nuclear_industry.turbine;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** 仅验证纯账本的守恒、容量边界与历史预算；Create 双轴生命周期由 B/C 实机验证。 */
final class TurbineStateTest {
    private static final long INLET_A = 11L, INLET_B = 12L, EXHAUST_A = 21L, EXHAUST_B = 22L;

    @Test void exactlyThreeDistinctBoundedTiersAndRuntimeCreateLimit() {
        var defaults = TurbineState.Settings.DEFAULT;
        assertTrue(defaults.valid(256));
        assertFalse(defaults.valid(127));
        assertEquals(5, defaults.shortTier().length());
        assertEquals(8, defaults.mediumTier().length());
        assertEquals(11, defaults.longTier().length());
        assertNull(defaults.tierForRotors(4));
        var repeated = new TurbineState.Settings(defaults.shortTier(),
                new TurbineState.Tier(3, 108, 8000, 8000), defaults.longTier(),
                128, 32768, 40, 256, 256, .5);
        assertFalse(repeated.valid(256));
        var faster = new TurbineState.Settings(defaults.shortTier(), defaults.mediumTier(),
                defaults.longTier(), 512, 32768, 40, 256, 256, .5);
        assertTrue(faster.valid(512));
        assertFalse(faster.valid(256));
        var invalidShare = new TurbineState.Settings(defaults.shortTier(), defaults.mediumTier(),
                defaults.longTier(), 128, 32768, 40, 256, 256, Double.NaN);
        assertFalse(invalidShare.valid(256));
    }

    @Test void fullFortyTickWindowMatchesActualFlowAndSingleMillibucketBudget() {
        var state = new TurbineState();
        state.applySettings(TurbineState.Settings.DEFAULT, 3, 256);
        for (int tick = 0; tick < 40; tick++) {
            assertEquals(54, state.fillInput(INLET_A, 54, false, tick));
            assertEquals(54, state.tick(tick, true));
            assertEquals(0, state.tick(tick, true));
        }
        assertEquals(2160, state.exhaust());
        assertEquals(1_769_472D, state.totalSu());
        assertEquals(884_736D, state.frontSu());
        assertEquals(884_736D, state.rearSu());
        for (int tick = 40; tick < 80; tick++) state.tick(tick, true);
        assertEquals(0, state.totalSu());

        var tiny = new TurbineState();
        var settings = smallSettings(new TurbineState.Tier(3, 1, 10, 10), 40);
        tiny.applySettings(settings, 3, 256);
        tiny.fillInput(INLET_A, 1, false, 0);
        assertEquals(1, tiny.tick(0, true));
        for (int tick = 1; tick < 40; tick++) {
            tiny.tick(tick, true);
            assertEquals(32768D / 40, tiny.totalSu());
        }
        tiny.tick(40, true);
        assertEquals(0, tiny.totalSu());
    }

    @Test void fullExhaustBlocksConversionWithoutConsumingInput() {
        var state = new TurbineState();
        state.applySettings(smallSettings(new TurbineState.Tier(3, 3, 10, 3), 4), 3, 256);
        assertEquals(10, state.fillInput(INLET_A, 10, false, 0));
        assertEquals(3, state.tick(0, true));
        assertEquals(0, state.tick(1, true));
        assertEquals(7, state.input());
        assertEquals(3, state.exhaust());
        assertEquals(2, state.drainExhaust(EXHAUST_A, 2, false, 1));
        assertEquals(2, state.tick(2, true));
        assertEquals(5, state.input());
        assertEquals(3, state.exhaust());
    }

    @Test void tankSmallerThanRatedFlowStillProcessesAvailableVolume() {
        var tier = new TurbineState.Tier(3, 10, 3, 2);
        var settings = smallSettings(tier, 4);
        assertTrue(settings.valid(256));
        var state = new TurbineState();
        state.applySettings(settings, 3, 256);
        assertTrue(state.canFormForNewTier(3));
        assertEquals(3, state.fillInput(INLET_A, 10, false, 0));
        assertEquals(2, state.tick(0, true));
        assertEquals(1, state.input());
        assertEquals(2, state.exhaust());
        assertEquals(2, state.drainExhaust(EXHAUST_A, 2, false, 0));
        assertEquals(1, state.tick(1, true));
    }

    @Test void eachPortHasIndependentBudgetSimulationIsPureAndExhaustPathsShareIt() {
        var state = new TurbineState();
        state.applySettings(TurbineState.Settings.DEFAULT, 3, 256);
        assertEquals(256, state.fillInput(INLET_A, 1000, true, 10));
        assertEquals(0, state.input());
        assertEquals(256, state.fillInput(INLET_A, 1000, false, 10));
        assertEquals(0, state.fillInput(INLET_A, 1, false, 10));
        assertEquals(256, state.fillInput(INLET_B, 1000, false, 10));
        var seed = state.save();
        seed.putInt("Exhaust", 1000);
        state.load(seed);
        assertEquals(128, state.drainExhaust(EXHAUST_A, 128, true, 10));
        assertEquals(128, state.drainExhaust(EXHAUST_A, 128, false, 10));
        assertEquals(128, state.drainExhaust(EXHAUST_A, 256, false, 10));
        assertEquals(0, state.drainExhaust(EXHAUST_A, 1, false, 10));
        assertEquals(256, state.drainExhaust(EXHAUST_B, 256, false, 10));
        var restored = new TurbineState();
        restored.applySettings(TurbineState.Settings.DEFAULT, 3, 256);
        restored.load(state.save());
        assertEquals(0, restored.drainExhaust(EXHAUST_A, 1, false, 10));
        assertEquals(256, restored.remainingExhaust(EXHAUST_A, 11));
    }

    @Test void lowerCapacityKeepsOldInventoryAndExistingMachineCanProcessAfterDrain() {
        var state = new TurbineState();
        state.applySettings(TurbineState.Settings.DEFAULT, 3, 256);
        var saved = new CompoundTag();
        saved.putInt("Input", 100);
        saved.putInt("Exhaust", 200);
        state.load(saved);
        var lower = smallSettings(new TurbineState.Tier(3, 10, 50, 100), 40);
        state.applySettings(lower, 3, 256);
        assertFalse(state.canFormForNewTier(3));
        assertEquals(100, state.input());
        assertEquals(200, state.exhaust());
        assertEquals(0, state.fillInput(INLET_A, 1, false, 0));
        assertEquals(0, state.tick(0, true));
        assertEquals(120, state.drainExhaust(EXHAUST_A, 120, false, 0));
        assertEquals(10, state.tick(1, true));
        assertEquals(90, state.input());
        assertEquals(90, state.exhaust());
        var restored = new TurbineState();
        restored.applySettings(lower, 3, 256);
        restored.load(state.save());
        assertEquals(90, restored.input());
        assertEquals(90, restored.exhaust());
        assertEquals(0, restored.totalSu());
    }

    @Test void stopAndTimeJumpClearHistoryWithoutMakingOfflinePower() {
        var state = new TurbineState();
        state.applySettings(smallSettings(new TurbineState.Tier(3, 1, 10, 10), 4), 3, 256);
        state.fillInput(INLET_A, 2, false, 1);
        state.tick(1, true);
        assertTrue(state.totalSu() > 0);
        state.stop();
        assertEquals(0, state.totalSu());
        state.tick(2, false);
        assertEquals(0, state.totalSu());
        state.tick(1000, true);
        assertEquals(1, state.processed());
        assertEquals(32768D / 4, state.totalSu());
        state.tick(10, true);
        assertEquals(0, state.totalSu());
        state.tick(10, true);
        assertEquals(0, state.totalSu());
    }

    @Test void coefficientChangeClearsOldPowerAndCustomFrontShareOnlySplitsNewTotal() {
        var state = new TurbineState();
        var defaults = TurbineState.Settings.DEFAULT;
        state.applySettings(defaults, 3, 256);
        state.fillInput(INLET_A, 54, false, 0);
        state.tick(0, true);
        assertTrue(state.totalSu() > 0);
        var changed = new TurbineState.Settings(defaults.shortTier(), defaults.mediumTier(),
                defaults.longTier(), 128, 65536, 40, 256, 256, .25);
        state.applySettings(changed, 3, 256);
        assertEquals(0, state.totalSu());
        assertEquals(54, state.exhaust());
        state.fillInput(INLET_A, 54, false, 1);
        state.tick(1, true);
        assertEquals(state.totalSu() * .25, state.frontSu());
        assertEquals(state.totalSu() * .75, state.rearSu());
        var invalid = new TurbineState.Settings(defaults.shortTier(), defaults.mediumTier(),
                defaults.longTier(), 512, 65536, 40, 256, 256, .25);
        state.applySettings(invalid, 3, 256);
        assertEquals(0, state.totalSu());
        assertEquals(0, state.tick(2, true));
        assertEquals(108, state.exhaust());
    }

    @Test void portableRestoreCannotProcessTwiceInSameWorldTick() {
        var settings = smallSettings(new TurbineState.Tier(3, 1, 10, 10), 4);
        var first = new TurbineState();
        first.applySettings(settings, 3, 256);
        assertEquals(2, first.fillInput(INLET_A, 2, false, 7));
        assertEquals(1, first.tick(7, true));
        var restored = new TurbineState();
        restored.applySettings(settings, 3, 256);
        restored.load(first.save());
        assertEquals(0, restored.totalSu());
        assertEquals(0, restored.tick(7, true));
        assertEquals(1, restored.input());
        assertEquals(1, restored.exhaust());
        assertEquals(1, restored.tick(8, true));
        assertEquals(0, restored.input());
        assertEquals(2, restored.exhaust());
    }

    private static TurbineState.Settings smallSettings(TurbineState.Tier tier, int window) {
        return new TurbineState.Settings(tier, new TurbineState.Tier(4, 2, 10, 10),
                new TurbineState.Tier(5, 3, 10, 10), 128, 32768, window, 256, 256, .5);
    }
}
