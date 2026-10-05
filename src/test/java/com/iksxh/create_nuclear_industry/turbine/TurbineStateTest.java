package com.iksxh.create_nuclear_industry.turbine;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** 服务端纯账本的成交、守恒、同 tick 额度与效率曲线；真实 Create 管路另由 GameTest 验证。 */
final class TurbineStateTest {
    private static final long IN_A = 11, IN_B = 12, OUT_A = 21, OUT_B = 22;

    @Test void configuredTiersAndCrossFieldValidation() {
        var defaults = TurbineState.Settings.DEFAULT;
        assertTrue(defaults.valid(256));
        assertFalse(defaults.valid(255));
        assertEquals(54, defaults.shortTier().ratedFlowMbPerTick());
        assertEquals(108, defaults.mediumTier().ratedFlowMbPerTick());
        assertEquals(216, defaults.longTier().ratedFlowMbPerTick());
        assertEquals(1.2, defaults.shortTier().maxEfficiencyMultiplier());
        assertEquals(1.5, defaults.mediumTier().maxEfficiencyMultiplier());
        assertEquals(1.8, defaults.longTier().maxEfficiencyMultiplier());
        assertFalse(settings(defaults.shortTier(), 40, 1, 1.3, .3).valid(256));
        assertFalse(settings(defaults.shortTier(), 40, 1, .5, 1).valid(256));
        assertFalse(settings(defaults.shortTier(), 40, 0, .5, .3).valid(256));
    }

    @Test void simulateAndAllPortsShareWholeMachineLimit() {
        var state = ready(TurbineState.Settings.DEFAULT, 3);
        assertEquals(54, state.fillInput(IN_A, 54, true, 1));
        assertEquals(0, state.exhaust());
        assertEquals(54, state.fillInput(IN_A, 54, false, 1));
        assertEquals(0, state.fillInput(IN_B, 54, false, 1));
        assertEquals(54, state.drainExhaust(OUT_A, 54, true, 1));
        assertEquals(54, state.exhaust());
        assertEquals(0, state.totalSu());
        assertEquals(54, state.drainExhaust(OUT_A, 54, false, 1));
        assertEquals(0, state.drainExhaust(OUT_B, 54, false, 1));
        assertEquals(0, state.fillInput(IN_B, 54, false, 1));
        assertEquals(0, state.exhaust());
        assertEquals(54, state.processedInWindow());
    }

    @Test void thresholdEqualityMidpointAndFullFlowForEveryTier() {
        var defaults = TurbineState.Settings.DEFAULT;
        for (var tier : new TurbineState.Tier[]{defaults.shortTier(), defaults.mediumTier(), defaults.longTier()}) {
            int rate = tier.ratedFlowMbPerTick();
            var state = ready(defaults, tier.rotorCount());
            for (int tick = 1; tick <= 40; tick++) move(state, rate, tick);
            assertEquals(rate, state.averageFlowMbPerTick());
            assertEquals(tier.maxEfficiencyMultiplier(), state.efficiencyMultiplier(), 1e-10);
            assertEquals(rate * defaults.suPerMbPerTick() * tier.maxEfficiencyMultiplier(),
                    state.totalSu(), 1e-6);
            state.tick(41, true);
            assertEquals(rate, state.averageFlowMbPerTick(),
                    "控制器先于本 tick 排汽读取时不能稳定少算一个窗口样本");
            assertEquals(rate, state.fillInput(IN_A, rate, false, 41));
            assertEquals(rate, state.drainExhaust(OUT_A, rate, false, 41));
            assertEquals(rate, state.averageFlowMbPerTick());

            // 窗口总量可精确表达 0.30R，即使每 tick 交易是整数 mB。
            state = ready(defaults, tier.rotorCount());
            int belowTotal = (int) (rate * 40 * .3) - 1;
            spread(state, belowTotal, rate, 40);
            assertEquals(0, state.totalSu());
            move(state, 1, 41);
            assertEquals(0, state.totalSu());
            state = ready(defaults, tier.rotorCount());
            spread(state, (int) (rate * 40 * .3), rate, 40);
            assertEquals(.5, state.efficiencyMultiplier(), 1e-10);
            assertEquals(rate * .3 * defaults.suPerMbPerTick() * .5, state.totalSu(), 1e-6);

            state = ready(defaults, tier.rotorCount());
            spread(state, (int) (rate * 40 * .65), rate, 40);
            assertEquals((.5 + tier.maxEfficiencyMultiplier()) / 2,
                    state.efficiencyMultiplier(), 1e-10);
        }
    }

    @Test void configurableCurveAndWindow() {
        var tier = new TurbineState.Tier(3, 20, 3, 2);
        var state = ready(settings(tier, 10, 1, .4, .2), 3);
        for (int tick = 1; tick <= 10; tick++) move(state, 4, tick);
        assertEquals(.4, state.efficiencyMultiplier(), 1e-10);
        for (int tick = 11; tick <= 20; tick++) move(state, 12, tick);
        assertEquals(1.2, state.efficiencyMultiplier(), 1e-10);
        assertEquals(12 * 32768 * 1.2, state.totalSu(), 1e-6);
    }

    @Test void blockedResidualRecoveryAndCurrentFormatSave() {
        var state = ready(TurbineState.Settings.DEFAULT, 3);
        assertEquals(54, state.fillInput(IN_A, 54, false, 1));
        for (int tick = 2; tick <= 50; tick++) {
            state.tick(tick, true);
            assertEquals(0, state.fillInput(IN_B, 54, false, tick));
        }
        assertEquals(54, state.exhaust());
        assertEquals(0, state.totalSu());
        var restored = ready(TurbineState.Settings.DEFAULT, 3);
        restored.load(state.save());
        assertEquals(54, restored.exhaust());
        assertEquals(0, restored.totalSu());
        assertEquals(54, restored.drainExhaust(OUT_A, 54, false, 51));
        assertEquals(54, restored.fillInput(IN_B, 54, false, 51));
        assertEquals(54, restored.exhaust());
        assertEquals(0, restored.drainExhaust(OUT_B, 1, false, 51));
    }

    @Test void outputBeforeControllerTickAndCapacityReductionPreserveAccounting() {
        var defaults = TurbineState.Settings.DEFAULT;
        var state = ready(settings(defaults.shortTier(), 1, 2, .5, .3), 3);
        assertEquals(54, state.fillInput(IN_A, 54, false, 7));
        assertEquals(54, state.drainExhaust(OUT_A, 54, false, 7));
        assertEquals(54, state.tick(7, true));
        assertEquals(54, state.processedInWindow());
        assertEquals(54, state.fillInput(IN_A, 54, false, 8));
        assertEquals(54, state.fillInput(IN_A, 54, false, 9));
        assertEquals(108, state.exhaust());
        state.applySettings(defaults, 3, 256);
        assertEquals(0, state.fillInput(IN_B, 1, false, 10));
        assertEquals(108, state.exhaust());
        assertEquals(54, state.drainExhaust(OUT_B, 54, false, 10));
        assertEquals(54, state.exhaust());
    }

    @Test void stopAndMissingTicksRemoveOnlyPower() {
        var state = ready(TurbineState.Settings.DEFAULT, 3);
        for (int tick = 1; tick <= 40; tick++) move(state, 54, tick);
        assertTrue(state.totalSu() > 0);
        state.stop();
        assertEquals(0, state.totalSu());
        assertEquals(54, state.fillInput(IN_A, 54, false, 41));
        state.tick(41, false);
        assertEquals(54, state.exhaust());
        assertEquals(0, state.totalSu());
        state.tick(100, true);
        assertEquals(0, state.totalSu());
        assertEquals(54, state.exhaust());
    }

    private static TurbineState ready(TurbineState.Settings settings, int rotors) {
        var state = new TurbineState();
        state.applySettings(settings, rotors, 256);
        return state;
    }

    private static void move(TurbineState state, int amount, int tick) {
        state.tick(tick, true);
        assertEquals(amount, state.fillInput(IN_A, amount, false, tick));
        assertEquals(amount, state.drainExhaust(OUT_A, amount, false, tick));
    }

    private static void spread(TurbineState state, int total, int rate, int ticks) {
        for (int tick = 1; tick <= ticks; tick++) {
            int amount = Math.min(rate, total);
            move(state, amount, tick);
            total -= amount;
        }
        assertEquals(0, total);
    }

    private static TurbineState.Settings settings(TurbineState.Tier shortTier, int window,
                                                  int turnover, double minimumMultiplier, double threshold) {
        var defaults = TurbineState.Settings.DEFAULT;
        return new TurbineState.Settings(shortTier, defaults.mediumTier(), defaults.longTier(), 256,
                32768, window, 256, 256, turnover, minimumMultiplier, threshold, .5);
    }
}
