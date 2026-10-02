package com.iksxh.create_nuclear_industry.production;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** 纯账本单元验证；真实 Create 热源与世界物流由隔离 GameTest 核对。 */
final class FuelSinteringStateTest {
    @Test void onePelletRequiresExactlyFourHundredEffectiveTicks() {
        FuelSinteringState state = new FuelSinteringState();
        assertEquals(1, state.insert(1, false));
        for (int i = 0; i < 399; i++) state.tick(true);
        assertEquals(0, state.output());
        assertEquals(1, state.input());
        state.tick(true);
        assertEquals(1, state.output());
        assertEquals(0, state.input());
        assertEquals(0, state.progress());
    }

    @Test void heatAndBlockedOutputPauseWithoutErasingProgress() {
        FuelSinteringState state = new FuelSinteringState();
        state.insert(64, false);
        for (int i = 0; i < 400; i++) state.tick(true);
        assertEquals(1, state.output());
        for (int i = 0; i < 155; i++) state.tick(true);
        for (int i = 0; i < 50; i++) state.tick(false);
        assertEquals(155, state.progress());
        CompoundTag blocked = state.save();
        blocked.putInt("Output", 64);
        state.load(blocked);
        for (int i = 0; i < 50; i++) state.tick(true);
        assertEquals(155, state.progress());
        state.takeOutput(1, false);
        for (int i = 0; i < 245; i++) state.tick(true);
        assertEquals(64, state.output());
        assertEquals(62, state.input());
    }

    @Test void partialInputRemovalKeepsProgressButEmptyClearsIt() {
        FuelSinteringState state = new FuelSinteringState();
        state.insert(2, false);
        for (int i = 0; i < 90; i++) state.tick(true);
        assertEquals(1, state.takeInput(1, false));
        assertEquals(90, state.progress());
        assertEquals(1, state.takeInput(1, false));
        assertEquals(0, state.progress());
    }

    @Test void snapshotAndSimulationPreserveConservation() {
        FuelSinteringState state = new FuelSinteringState();
        assertEquals(64, state.insert(70, true));
        assertEquals(0, state.input());
        state.insert(3, false);
        for (int i = 0; i < 35; i++) state.tick(true);
        CompoundTag tag = state.save();
        FuelSinteringState restored = new FuelSinteringState();
        restored.load(tag);
        assertEquals(3, restored.input());
        assertEquals(35, restored.progress());
        assertEquals(2, restored.takeInput(2, true));
        assertEquals(3, restored.input());
        assertEquals(0, restored.takeOutput(10, false));
    }
}
