package com.iksxh.create_nuclear_industry.production;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 离心机定向事务测试：守恒、堵塞、速度及批次序列化，不重演原生机器流程。 */
final class CentrifugeStateTest {
    private static CentrifugeState.Batch batch(int work) {
        return new CentrifugeState.Batch(ResourceLocation.parse("create_nuclear_industry:centrifuging/test"),
                new FluidStack(Fluids.WATER, 1000), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.GOLD_INGOT, 7), new FluidStack(Fluids.WATER, 1000), work);
    }

    @Test
    void blockedOutputNeverConsumesOrLosesAnyProduct() {
        CentrifugeState state = new CentrifugeState();
        state.slurryMb = 1000;
        state.depleted = new ItemStack(Items.GOLD_INGOT, 58);
        assertFalse(state.begin(batch(400)));
        assertEquals(1000, state.slurryMb);
        assertNull(state.batch);
        state.depleted = new ItemStack(Items.GOLD_INGOT, 57);
        assertTrue(state.begin(batch(400)));
        assertEquals(0, state.slurryMb);
        state.waterMb = 3500;
        assertFalse(state.finish());
        assertNotNull(state.batch);
        assertEquals(57, state.depleted.getCount());
        state.waterMb = 3000;
        assertTrue(state.finish());
        assertEquals(1, state.enriched.getCount());
        assertEquals(64, state.depleted.getCount());
        assertEquals(4000, state.waterMb);
        assertFalse(state.finish());
    }

    @Test
    void speedStabilityAndFractionalWearFollowApprovedFormula() {
        CentrifugeState state = new CentrifugeState();
        state.slurryMb = 1000;
        assertTrue(state.begin(batch(1000000)));
        state.observeSpeed(-128);
        for (int tick = 0; tick < 19; tick++) state.observeSpeed(-128);
        assertEquals(19, state.stableTicks);
        state.advance(-128);
        assertEquals(0, state.progress);
        state.observeSpeed(-128);
        state.advance(-128);
        assertEquals(1, state.progress);
        assertEquals(0, state.wearUnits);
        state.observeSpeed(128.25f);
        assertEquals(0, state.stableTicks);
        for (int tick = 0; tick < 20; tick++) state.observeSpeed(128.25f);
        state.advance(128.25f);
        assertEquals(256, state.wearUnits);
        state.observeSpeed(0);
        assertFalse(state.repair());
    }

    @Test
    void bearingExhaustsExactlyAndOnlyAtRestCanRepair() {
        CentrifugeState state = new CentrifugeState();
        state.slurryMb = 1000;
        assertTrue(state.begin(batch(1000000)));
        state.stableTicks = 20;
        state.observedSpeed = 256;
        state.wearUnits = CentrifugeState.WEAR_LIMIT - 128 * 1024;
        state.advance(256);
        assertEquals(CentrifugeState.WEAR_LIMIT, state.wearUnits);
        double progress = state.progress;
        state.advance(256);
        assertEquals(progress, state.progress);
        assertFalse(state.repair());
        state.observeSpeed(0);
        assertTrue(state.repair());
        assertEquals(0, state.wearUnits);
        assertNotNull(state.batch);
    }

    @Test
    void sealedBatchAndWearSurviveSaveWithoutLookingUpRecipeAgain() {
        CentrifugeState state = new CentrifugeState();
        state.slurryMb = 1000;
        assertTrue(state.begin(batch(400)));
        state.progress = 123.5;
        state.wearUnits = 777;
        state.wearRemainder = .375;
        CompoundTag tag = new CompoundTag();
        state.write(tag, RegistryAccess.EMPTY);
        CentrifugeState restored = new CentrifugeState();
        restored.read(tag, RegistryAccess.EMPTY);
        assertEquals(0, restored.slurryMb);
        assertEquals(123.5, restored.progress);
        assertEquals(777, restored.wearUnits);
        assertEquals(.375, restored.wearRemainder);
        assertEquals(batch(400).recipeId(), restored.batch.recipeId());
        assertEquals(7, restored.batch.depleted().getCount());
        assertTrue(restored.finish());
        assertEquals(1, restored.enriched.getCount());
        assertEquals(7, restored.depleted.getCount());
        assertEquals(1000, restored.waterMb);
    }

    @Test
    void actualCentrifugeRecipeJsonDecodesWithLockedCodec() throws Exception {
        String text = Files.readString(Path.of("src/main/resources/data/create_nuclear_industry/recipe/centrifuging/uranium_slurry.json"));
        CentrifugeRecipe recipe = CentrifugeRecipe.CODEC.codec().parse(JsonOps.INSTANCE,
                JsonParser.parseString(text)).getOrThrow();
        assertEquals(1000, recipe.input().getAmount());
        assertEquals(1, recipe.enriched().getCount());
        assertEquals(7, recipe.depleted().getCount());
        assertEquals(1000, recipe.water().getAmount());
        assertEquals(400, recipe.work());
    }
}
