package com.iksxh.create_nuclear_industry.boiler;

import static org.junit.jupiter.api.Assertions.*;
import static com.iksxh.create_nuclear_industry.boiler.BoilerSteamInventoryKind.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** 用当前双库存格式验证共享容量和生产归类；不依赖客户端枚举、外部管网或旧格式迁移。 */
class BoilerDualSteamInventoryTest {
    private static final double EPS = 1e-6;
    private static BoilerState state(int normal, double normalHu, int sc, double scHu) {
        var s = new BoilerState(); var t = s.save();
        t.putInt("NormalSteam", normal); t.putDouble("NormalSteamHu", normalHu);
        t.putInt("SupercriticalSteam", sc); t.putDouble("SupercriticalSteamHu", scHu);
        s.load(t); return s;
    }
    @Test void bothInventoriesUseExactlyOneCapacity() {
        var s = state(8000, 8000, 10000, 10000); var t = s.save();
        t.putInt("Water", 100); t.putDouble("WaterHu", (14400 + 10) * 2); s.load(t);
        assertEquals(18000, s.steam(), "两池总量必须计入同一汽区容量");
        assertEquals(18000, s.steamHu(), EPS);
        s.tick(1, 1, 18, true, false);
        assertEquals(18000, s.steam()); assertEquals(0, s.produced(), "共享容量满时不能给另一池继续增加量");
    }
    @Test void ordinaryInventoryReheatsWithoutUpgrading() {
        var s = state(9000, 7200, 0, 0);
        for (int t = 1; t <= 100; t++) s.tick(t, 1, 18, true, false);
        CompoundTag saved = s.save();
        assertEquals(9000, saved.getInt("NormalSteam"), "再热只增加真实HU，不重标已有普通汽");
        assertEquals(9000, saved.getDouble("NormalSteamHu"), EPS);
        assertEquals(0, saved.getInt("SupercriticalSteam")); assertEquals(.5, s.pressure(), EPS);
    }
    @Test void currentSavePreservesBothInventoriesAndHu() {
        var s = state(4000, 3200, 8000, 8000); s.setGeometry(16, 16, 16, 16, 4000, 4000); s.setMinimumPressure(.1);
        var restored = new BoilerState(); restored.load(s.save());
        assertEquals(12000, restored.steam()); assertEquals(11200, restored.steamHu(), EPS);
        assertEquals(.3125, restored.pressure(), EPS); assertEquals(s.save(), restored.save());
    }
    @Test void productionCrossingClassifiesOnlyTheNewBatchAfterAddingIt() {
        var s = state(8900, 8900, 0, 0); var t = s.save();
        t.putInt("Water", 100); t.putDouble("WaterHu", (14400 + 10) * 2); s.load(t);
        double before = s.totalHu(); s.tick(1, 1, 80, true, false);
        assertEquals(before + 80, s.totalHu(), EPS);
        assertEquals(8900, s.save().getInt("NormalSteam"));
        assertEquals(100, s.save().getInt("SupercriticalSteam"), "这批加量后达到门槛才归SC，不能重标旧8900mB");
        assertEquals(.5, s.pressure(), EPS);
    }
    @Test void existingScBelowProductionPressureDrainsWithoutChangingIdentity() {
        var s = state(1000, 800, 4000, 4000); s.setMinimumPressure(.1);
        assertTrue(s.pressure() < s.settings().supercriticalPressure());
        assertEquals(256, s.drainSteam(SUPERCRITICAL, BlockPos.ZERO, 256, false, 1));
        assertEquals(3744, s.steam(SUPERCRITICAL)); assertEquals(1000, s.steam(NORMAL));
        assertEquals(3744, s.steamHu(SUPERCRITICAL), EPS); assertEquals(800, s.steamHu(NORMAL), EPS);
    }
    @Test void coldScWaitsForRealPaidReheatAndNeverDowngrades() {
        var s = state(0, 0, 1000, 999); s.setMinimumPressure(0);
        assertEquals(0, s.drainSteam(SUPERCRITICAL, BlockPos.ZERO, 256, false, 1));
        assertEquals(1000, s.steam(SUPERCRITICAL)); assertEquals(0, s.steam(NORMAL));
        double before = s.totalHu(); s.tick(1, 1, 1, true, false);
        assertEquals(before + 1, s.totalHu(), EPS);
        assertEquals(256, s.drainSteam(SUPERCRITICAL, BlockPos.ZERO, 256, false, 2));
        assertEquals(744, s.steamHu(SUPERCRITICAL), EPS);
    }
    @Test void mixedTemperaturesDrainTheirOwnPressureContribution() {
        var s = state(4000, 3200, 8000, 8000); s.setMinimumPressure(.2);
        var d = s.settings();
        s.setSettings(new BoilerState.Settings(d.minDimension(), d.maxDimension(), d.waterCapacityPerCellMb(), d.steamCapacityPerCellMb(),
                20000, d.pairHeatHuPerTick(), d.boilingTemperature(), d.supercriticalTemperature(), d.wallHeatCapacityHuPerWaterCell(),
                d.waterSpecificHeatHuPerMb(), d.steamSpecificHeatHuPerMb(), d.vaporizationLatentHeatHuPerMb(), d.supercriticalPressure(),
                d.outputMinPressure(), d.valveOpenPressure(), d.valveClosePressure(), d.valveFlowPerSteamCellMbPerTick(),
                d.idleWaterCoolingHuPerCellPerTick(), d.idleSteamCoolingHuPerCellPerTick()));
        double before = s.totalHu();
        assertEquals(6400, s.drainSteam(SUPERCRITICAL, BlockPos.ZERO, 10000, false, 1));
        assertEquals(.2, s.pressure(), EPS); assertEquals(before - 6400, s.totalHu(), EPS);
        assertEquals(4000, s.steam(NORMAL)); assertEquals(3200, s.steamHu(NORMAL), EPS);
        assertEquals(0, s.drainSteam(NORMAL, new BlockPos(1, 0, 0), 1, false, 1));
    }
    @Test void sameTickSimulationAndTwoKindsShareOnePhysicalPortBudget() {
        var s = state(4000, 3200, 8000, 8000); s.setMinimumPressure(.1); var before = s.save();
        assertEquals(256, s.drainSteam(NORMAL, BlockPos.ZERO, 256, true, 9));
        assertEquals(256, s.drainSteam(SUPERCRITICAL, BlockPos.ZERO, 256, true, 9)); assertEquals(before, s.save());
        assertEquals(100, s.drainSteam(NORMAL, BlockPos.ZERO, 100, false, 9));
        var restored = new BoilerState(); restored.load(s.save());
        assertEquals(156, restored.drainSteam(SUPERCRITICAL, BlockPos.ZERO, 256, false, 9));
        assertEquals(256, restored.drainSteam(SUPERCRITICAL, new BlockPos(1, 0, 0), 256, false, 9));
        assertEquals(3120, restored.steamHu(NORMAL), EPS);
        assertEquals(8000 - 412, restored.steamHu(SUPERCRITICAL), EPS);
    }
    @Test void reheatAndCoolingShareTheirSingleRealHuBudget() {
        var s = state(1000, 800, 1000, 900); double before = s.totalHu();
        s.tick(1, 1, 30, true, false);
        assertEquals(820, s.steamHu(NORMAL), EPS); assertEquals(910, s.steamHu(SUPERCRITICAL), EPS);
        assertEquals(before + 30, s.totalHu(), EPS); s.idle(2);
        assertEquals(before + 30 - .9, s.totalHu(), EPS);
        s.prepare(100000, 1);
        assertEquals(800, s.steamHu(NORMAL), EPS); assertEquals(800, s.steamHu(SUPERCRITICAL), EPS);
        assertFalse(s.outputQualified(SUPERCRITICAL)); assertTrue(s.outputQualified(NORMAL));
    }
    @Test void mixedValveUsesOneQuotaAndKeepsClosePressureWithoutRelabeling() {
        var s = state(1000, 800, 17000, 17000); double before = s.totalHu();
        s.vent(1, true);
        assertEquals(288, s.vented()); assertEquals(984, s.steam(NORMAL)); assertEquals(16728, s.steam(SUPERCRITICAL));
        assertEquals(before - (16 * .8 + 272), s.totalHu(), EPS);
        var unchanged = s.save(); s.vent(1, true); assertEquals(unchanged, s.save());
        for (int t = 2; t < 100; t++) { s.vent(t, true); assertTrue(s.pressure() + EPS >= .8); assertTrue(s.vented() <= 288); }
        assertTrue(s.pressure() < .8001);
    }
}
