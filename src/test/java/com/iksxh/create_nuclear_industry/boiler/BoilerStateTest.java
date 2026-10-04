package com.iksxh.create_nuclear_industry.boiler;

import static org.junit.jupiter.api.Assertions.*;
import com.iksxh.create_nuclear_industry.heat.HeatExchangerState;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class BoilerStateTest {
    private static final BlockPos WATER_EAST = new BlockPos(2, 1, 0);
    private static final BlockPos WATER_SOUTH = new BlockPos(0, 1, 2);
    private static final BlockPos STEAM_WEST = new BlockPos(-2, 3, 0);
    private static final BlockPos STEAM_EAST = new BlockPos(2, 3, 0);

    @Test void warmingHeatCannotBecomeSteamTwice() {
        var state = new BoilerState();
        state.fillWater(WATER_EAST, 1000, false, 1);
        for (int tick = 1; tick <= 200; tick++) state.tick(tick, 1, 18, true, false);
        assertEquals(3600, state.warmHu());
        assertEquals(0, state.steam());
        state.tick(201, 1, 18, true, false);
        assertEquals(18, state.steam());
        assertEquals(238, state.water());
    }

    @Test void waterPortsHaveIndependentBudgetsAndSimulationStaysPure() {
        var state = new BoilerState();
        assertEquals(256, state.fillWater(WATER_EAST, 1000, true, 1));
        assertEquals(0, state.water());
        assertEquals(256, state.fillWater(WATER_EAST, 1000, false, 1));
        assertEquals(0, state.fillWater(WATER_EAST, 1, false, 1));
        assertEquals(256, state.fillWater(WATER_SOUTH, 1000, false, 1));
        assertEquals(512, state.water());
        assertEquals(256, state.fillWater(WATER_EAST, 1000, false, 2));
    }

    @Test void independentWaterBudgetsStillShareTheWholeBoilerCapacity() {
        var state = new BoilerState();
        var saved = new net.minecraft.nbt.CompoundTag();
        saved.putInt("Water", BoilerState.CAPACITY - 300);
        state.load(saved);
        assertEquals(256, state.fillWater(WATER_EAST, 1000, true, 1));
        assertEquals(256, state.fillWater(WATER_SOUTH, 1000, true, 1));
        assertEquals(256, state.fillWater(WATER_EAST, 1000, false, 1));
        assertEquals(44, state.fillWater(WATER_SOUTH, 1000, false, 1));
        assertEquals(BoilerState.CAPACITY, state.water());
        assertEquals(0, state.remainingFill(WATER_SOUTH, 1));
    }

    @Test void steamPortsHaveIndependentBudgetsAndSamePortSharesItsBudget() {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("Steam", 1000);
        var state = new BoilerState();
        state.load(tag);
        assertEquals(128, state.drainSteam(STEAM_WEST, 128, false, 4));
        assertEquals(128, state.drainSteam(STEAM_WEST, 256, false, 4));
        assertEquals(0, state.drainSteam(STEAM_WEST, 1, false, 4));
        assertEquals(256, state.drainSteam(STEAM_EAST, 256, false, 4));
        assertEquals(488, state.steam());

        var restored = new BoilerState();
        restored.load(state.save());
        assertEquals(0, restored.drainSteam(STEAM_WEST, 1, false, 4));
        assertEquals(256, restored.drainSteam(STEAM_WEST, 256, false, 5));
        assertEquals(232, restored.drainSteam(STEAM_EAST, 256, false, 5));
        assertEquals(0, restored.steam());
    }

    @Test void oldSingleBudgetSaveRemainsConservativeUntilNextTick() {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("Water", 200);
        tag.putLong("FlowTick", 7);
        tag.putInt("FillUsed", 200);
        var state = new BoilerState();
        state.load(tag);
        assertEquals(56, state.fillWater(WATER_EAST, 256, false, 7));
        var restored = new BoilerState();
        restored.load(state.save());
        assertEquals(0, restored.fillWater(WATER_EAST, 1, false, 7));
        assertEquals(256, state.fillWater(WATER_SOUTH, 256, false, 8));
    }

    @Test void cooldownValveAndSaveRestoreConserveInventory() {
        var state = new BoilerState();
        state.fillWater(WATER_EAST, 1000, false, 1);
        for (int tick = 1; tick <= 200; tick++) state.tick(tick, 1, 18, true, false);
        state.tick(201, 1, 0, true, false);
        assertTrue(state.warmHu() < 3600);
        var saved = state.save();
        var restored = new BoilerState();
        restored.load(saved);
        assertEquals(state.water(), restored.water());
        assertEquals(state.warmHu(), restored.warmHu(), 0.0001);
        restored.tick(201, 1, 18, true, false);
        assertEquals(state.warmHu(), restored.warmHu(), 0.0001);
        restored.tick(199, 1, 18, true, false);
        assertEquals(0, restored.warmHu());
    }

    @Test void dedicatedHeatIsPaidOnceAndColdVolumeMatchesHot() {
        var source = new HeatExchangerState();
        var settings = new HeatExchangerState.Settings(18, 1, 40, 0.5);
        source.fillHot(72, false);
        assertEquals(0, source.claimDedicated(1, 18, settings));
        assertEquals(18, source.reserve());
        assertEquals(18, source.claimDedicated(2, 18, settings));
        assertEquals(0, source.claimDedicated(2, 18, settings));
        source.tick(2, true, settings);
        assertEquals(18, source.reserve());
        assertEquals(18, source.claimDedicated(3, 18, settings));
        assertEquals(0, source.claimDedicated(4, 18, settings));
        assertEquals(0, source.hot());
        assertEquals(72, source.cold());
    }

    @Test void resumedHeatCannotSkipElapsedCooling() {
        var state = new BoilerState();
        state.fillWater(WATER_EAST, 256, false, 1);
        for (int tick = 1; tick <= 200; tick++) state.tick(tick, 1, 18, true, false);
        var restored = new BoilerState();
        restored.load(state.save());
        restored.prepare(5000, 1);
        assertFalse(restored.ready());
        assertEquals(0, restored.warmHu());
        assertEquals(18, restored.demand(1));
        restored.tick(5000, 1, 18, true, false);
        assertEquals(18, restored.warmHu());
        assertEquals(0, restored.steam());
    }

    @Test void addingNinthSectionKeepsOldPaidHeatWithoutSkippingNewWarmup() {
        var saved = new net.minecraft.nbt.CompoundTag();
        saved.putDouble("WarmHu", 8 * 3600);
        saved.putBoolean("Ready", true);
        var state = new BoilerState();
        state.load(saved);
        state.sectionsChanged(9);
        assertEquals(8 * 3600, state.warmHu());
        assertFalse(state.ready());
        state.tick(1, 9, 162, true, false);
        assertEquals(8 * 3600 + 162, state.warmHu());
        assertEquals(0, state.steam());
    }

    @Test void openedValveThenExternalDrainBelowCloseThresholdNeverAddsSteam() {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("Steam", 12000);
        tag.putBoolean("ValveOpen", true);
        tag.putLong("TotalVented", 256);
        var state = new BoilerState();
        state.load(tag);
        state.tick(1, 1, 0, true, true);
        assertFalse(state.valveOpen());
        assertEquals(0, state.vented());
        assertEquals(12000, state.steam());
        assertEquals(256, state.totalVented());
    }

    @Test void fractionalPaidHeatRequiresWholeHuForOneSteam() {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("Water", 1);
        tag.putDouble("WarmHu", 3600);
        tag.putBoolean("Ready", true);
        var state = new BoilerState();
        state.load(tag);
        state.tick(1, 1, 0.5, true, false);
        assertEquals(0, state.steam());
        assertEquals(0.5, state.processHu());
        state.tick(2, 1, 0.5, true, false);
        assertEquals(1, state.steam());
        assertEquals(0, state.water());
        assertEquals(0, state.processHu());
    }

    @Test void blockedValveHoldsSteamThenVentsAndClosesAtEightyPercent() {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("Steam", 14400);
        var state = new BoilerState();
        state.load(tag);
        state.tick(1, 1, 0, false, true);
        assertTrue(state.valveOpen());
        assertTrue(state.valveBlocked());
        assertEquals(14400, state.steam());
        assertEquals(0, state.totalVented());
        state.tick(2, 1, 0, true, true);
        assertEquals(256, state.vented());
        assertEquals(14144, state.steam());
        assertEquals(256, state.totalVented());
        for (int tick = 3; tick <= 8; tick++) state.tick(tick, 1, 0, true, true);
        assertEquals(12800, state.steam());
        assertFalse(state.valveOpen());
        assertEquals(1600, state.totalVented());
    }
}
