package com.iksxh.create_nuclear_industry.boiler;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** 直接验证可支付的质量/热量不变量，不将温压资格误当作免费工质升级。 */
class BoilerStateTest {
    private static final BlockPos A = BlockPos.ZERO, B = new BlockPos(1, 0, 0);
    private static final double EPS = 1e-6;
    private static BoilerState state(int water, int steam, double waterHu, double steamHu) {
        var s = new BoilerState(); var tag = s.save(); tag.putInt("Water", water); tag.putInt("Steam", steam);
        tag.putDouble("WaterHu", waterHu); tag.putDouble("SteamHu", steamHu); s.load(tag); return s;
    }
    @Test void actualThresholdsChooseSteamAndMinimumIsIndependent() {
        var lowPressure = state(0, 8999, 0, 8999);
        assertFalse(lowPressure.supercritical(), "实际炉压未达门槛不能标识超临界蒸汽");
        lowPressure.setMinimumPressure(.17);
        assertEquals(.17, lowPressure.minimumPressure(), EPS);
        assertTrue(lowPressure.outputQualified(), "已付汽化热的普通蒸汽仍可出汽");
    }
    @Test void referencePartitionHasEighteenBucketsPerZone() {
        var s = new BoilerState(); assertEquals(18000, s.waterCapacity()); assertEquals(18000, s.steamCapacity());
        s = state(18000, 0, 0, 0); s.setGeometry(9, 9, 9, 9, 4000, 4000);
        for (int t = 1; t <= 200; t++) s.tick(t, 9, 162, true, false);
        assertEquals(32400, s.warmHu(), EPS); assertEquals(2, s.waterTemperature(), EPS); assertEquals(0, s.steam());
    }
    @Test void fixedTargetCostsIncludeCarriedWaterHeatExactlyOnce() {
        var s = state(100, 0, (14400 + 10) * 2, 0);
        double before = s.totalHu(); s.tick(1, 1, 9, true, false);
        assertEquals(before + 9, s.totalHu(), EPS);
        assertEquals(100, s.water() + s.steam()); assertEquals(1, s.steamHu() / s.steam(), EPS);
        assertEquals(2, s.targetTemperature(), EPS);
    }
    @Test void nineBucketsAtBoilingNeedEighteenHundredHuToReheat() {
        var s = state(0, 9000, 0, 7200);
        for (int t = 1; t <= 100; t++) s.tick(t, 1, 18, true, false);
        assertEquals(9000, s.steamHu(), EPS); assertEquals(2, s.steamTemperature(), EPS); assertEquals(.5, s.pressure(), EPS);
        assertEquals(0, s.drainSteam(A, 256, false, 101));
    }
    @Test void fullLowTemperatureTankStillReheats() {
        var s = state(0, 18000, 0, 14400); assertTrue(s.demand(true) > 0);
        for (int t = 1; t <= 200; t++) s.tick(t, 1, 18, true, false);
        assertEquals(18000, s.steam()); assertEquals(18000, s.steamHu(), EPS); assertEquals(2, s.steamTemperature(), EPS);
    }
    @Test void coldFeedDilutesOnlyTemperatureAndSimulationIsPure() {
        var s = state(1000, 0, 29000, 0); CompoundTag before = s.save();
        assertEquals(256, s.fillWater(A, 1000, true, 1)); assertEquals(before, s.save());
        double tw = s.waterTemperature(); s.fillWater(A, 1000, false, 1);
        assertEquals(29000, s.totalHu(), EPS); assertTrue(s.waterTemperature() < tw);
        assertEquals(0, s.fillWater(A, 1, false, 1)); assertEquals(256, s.fillWater(B, 1000, false, 1));
    }
    @Test void multiplePortsCannotDuplicatePressureHeadroomAndCarryActualEnthalpy() {
        var s = state(0, 11100, 0, 11100); var before = s.save();
        assertEquals(256, s.drainSteam(A, 256, true, 2)); assertEquals(256, s.drainSteam(B, 256, true, 2)); assertEquals(before, s.save());
        assertEquals(256, s.drainSteam(A, 256, false, 2)); assertEquals(44, s.drainSteam(B, 256, false, 2));
        assertEquals(10800, s.steam()); assertEquals(.6, s.pressure(), EPS); assertEquals(10800, s.steamHu(), EPS);
        assertEquals(2, s.steamTemperature(), EPS); assertEquals(0, s.drainSteam(B, 1, false, 2));
        s.setMinimumPressure(.17); double energy = s.steamHu(); int n = s.drainSteam(B, 256, false, 3);
        assertEquals(energy - n, s.steamHu(), EPS); assertEquals(2, s.steamTemperature(), EPS);
    }
    @Test void singleMinimumKeepsEveryPercentWithoutChangingInventoryOrHeat() {
        var s = state(0, 12000, 0, 12000);
        for (int percent : new int[]{0, 17, 43, 70, 100}) {
            s.setMinimumPressure(percent / 100D);
            assertEquals(percent / 100D, s.minimumPressure(), EPS);
            assertEquals(12000, s.steam()); assertEquals(12000, s.totalHu(), EPS);
            assertEquals(percent == 100 ? 0 : Math.min(256, Math.max(0, 12000 - percent * 180)),
                    s.drainSteam(A, 256, true, 1));
            var restored = new BoilerState(); restored.load(s.save()); restored.setSettings(BoilerState.DEFAULT);
            assertEquals(s.save(), restored.save());
        }
    }
    @Test void bothActualThresholdsAndPaidLatentHeatAreRequired() {
        assertTrue(state(0, 9000, 0, 9000).supercritical());
        assertFalse(state(0, 9000, 0, 8999).supercritical());
        assertFalse(state(0, 8999, 0, 8999).supercritical());
        assertTrue(state(0, 9001, 0, 9001).supercritical());
        var unpaid = state(0, 12000, 0, 9599); unpaid.setMinimumPressure(0);
        assertFalse(unpaid.outputQualified()); assertFalse(unpaid.supercritical());
        assertEquals(0, unpaid.drainSteam(A, 256, false, 1));
        assertFalse(state(0, 0, 0, 20000).outputQualified());
        var s = state(0, 12000, 0, 12000); s.setMinimumPressure(0); double before = s.totalHu();
        assertEquals(256, s.drainSteam(A, 256, false, 1));
        assertEquals(before - 256, s.totalHu(), EPS); assertEquals(0, s.drainSteam(A, 256, false, 1));
    }
    @Test void coolingKeepsLatentHeatAndCriticalRestorationMustPayAgain() {
        var s = state(0, 9000, 0, 9000); s.tick(1, 1, 0, true, true); s.prepare(100000, 1);
        assertEquals(7200, s.steamHu(), EPS); assertEquals(1, s.steamTemperature(), EPS); assertTrue(s.outputQualified()); assertFalse(s.supercritical());
        assertEquals(9000, s.steam());
    }
    @Test void redstoneResidualProductionSpendsExistingHeatOnly() {
        var s = state(18000, 0, 32400, 0); double initial = s.totalHu();
        for (int t = 1; t <= 100; t++) s.tick(t, 9, 9999, true, true);
        assertTrue(s.steam() > 0); assertTrue(s.steam() < 18000); assertTrue(s.totalHu() <= initial);
        assertEquals(18000, s.water() + s.steam());
    }
    @Test void blockedValveCapsNewSteamPressureAndReheat() {
        var s = state(0, 18000, 0, 14400);
        for (int t = 1; t <= 300; t++) s.tick(t, 1, s.demand(false), false, false);
        assertTrue(s.pressure() <= .9 + EPS); assertEquals(.9, s.pressure(), EPS); s.vent(300, false);
        assertTrue(s.valveBlocked()); int before = s.steam(); double h = s.steamHu() / before;
        s.vent(301, true); assertEquals(288, s.vented()); assertEquals(before - 288, s.steam()); assertEquals(h, s.steamHu() / s.steam(), EPS);
        int after = s.steam(); s.vent(301, true); assertEquals(after, s.steam());
    }
    @Test void sharedCoolantConvertsEqualVolumeAndOnlyPaidHuIsAvailable() {
        var s = new BoilerState(); s.fillHot(A, 256, false, 1); double paid = 0;
        for (int t = 1; t <= 10; t++) {
            double q = s.collectHeat(t, 18, .3, 18); paid += q;
            assertEquals(0, s.collectHeat(t, 18, .3, 18));
        }
        assertEquals(256, s.hot() + s.cold()); assertEquals(s.cold() * .3, paid + s.coolantHu(), EPS);
        var before = s.save(); s.drainCold(B, 200, true, 20); assertEquals(before, s.save());
    }
    @Test void currentSaveRestoresHuGeometryAndUsedPortBudget() {
        var s = state(200, 12000, 1200, 12000); s.setGeometry(12, 15, 3, 2, 4000, 4000);
        s.fillWater(A, 128, false, 5); s.setMinimumPressure(.2);
        var restored = new BoilerState(); restored.load(s.save()); assertEquals(s.save(), restored.save());
        assertEquals(128, restored.fillWater(A, 256, false, 5)); assertEquals(24000, restored.waterCapacity()); assertEquals(30000, restored.steamCapacity());
        restored.setGeometry(1, 1, 1, 1, 4000, 4000); assertEquals(12000, restored.steam()); assertEquals(12000, restored.steamHu(), EPS);
    }
    @Test void configurationValidatesRangeProductsAndThresholds() {
        assertTrue(BoilerState.DEFAULT.valid());
        assertTrue(new BoilerState.Settings(5, 11, 2000, 2000, 256, 18, 1, 2, 1600, .1, .2, .7, .5, 1, .9, .8, 32, .9, .1).valid());
        assertFalse(new BoilerState.Settings(5, 11, 2000, 2000, 256, 18, 1, 2, 1600, .1, .2, .7, .5, 1.01, .9, .8, 32, .9, .1).valid());
        var d = BoilerState.DEFAULT;
        var valid = new BoilerState.Settings(6, 12, 500, 600, 128, 10, 1, 3, 100, .2, .3, .8, .4, .5, .9, .7, 16, 0, 0);
        assertTrue(valid.valid()); var s = new BoilerState(); s.setSettings(valid); s.setGeometry(12, 24, 3, 2, 2000, 3000);
        assertEquals(6000, s.waterCapacity()); assertEquals(14400, s.steamCapacity());
        for (int[] range : new int[][]{{4, 11}, {12, 5}, {5, 33}}) {
            var bad = new BoilerState.Settings(range[0], range[1], 2000, 2000, 256, 18, 1, 2, 1600, .1, .2, .7, .5, .6, .9, .8, 32, .9, .1);
            assertFalse(bad.valid()); s.setSettings(bad); assertEquals(0, s.fillWater(A, 10, false, 1));
        }
        assertFalse(new BoilerState.Settings(5, 32, 1000000, 1000000, 256, 18, 1, 2, 1600, .1, .2, .7, .5, .6, .9, .8, 32, .9, .1).valid());
    }
}
