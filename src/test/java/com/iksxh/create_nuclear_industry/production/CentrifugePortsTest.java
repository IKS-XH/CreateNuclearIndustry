package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 使用注册离心机实体和 Create 原生过滤行为验证各面共享端口及快照兼容。 */
final class CentrifugePortsTest {
    private static CentrifugeBlockEntity machine() {
        BlockState state = FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get().defaultBlockState();
        return new CentrifugeBlockEntity(BlockPos.ZERO, state);
    }

    private static SidedFilteringBehaviour filtering(CentrifugeBlockEntity machine) throws Exception {
        Field field = CentrifugeBlockEntity.class.getDeclaredField("filtering");
        field.setAccessible(true);
        return (SidedFilteringBehaviour) field.get(machine);
    }

    private static CentrifugeState state(CentrifugeBlockEntity machine) throws Exception {
        Field field = CentrifugeBlockEntity.class.getDeclaredField("state");
        field.setAccessible(true);
        return (CentrifugeState) field.get(machine);
    }

    @Test
    void fiveMaterialFacesSharePortsAndBottomHasNone() throws Exception {
        CentrifugeBlockEntity machine = machine();
        for (Direction side : Direction.values()) {
            if (side == Direction.DOWN) {
                assertNull(machine.fluidPort(side));
                assertNull(machine.itemPort(side));
            } else {
                assertNotNull(filtering(machine).get(side));
                assertEquals(2, machine.fluidPort(side).getTanks());
                assertEquals(2, machine.itemPort(side).getSlots());
            }
        }
        assertNull(machine.fluidPort(null));
        assertNull(machine.itemPort(null));
    }

    @Test
    void nativeFilterSlotsStayAtFaceCornersOutsidePipeCrossSection() {
        BlockState state = FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get().defaultBlockState();
        CentrifugeFilterSlotTransform transform = new CentrifugeFilterSlotTransform();
        for (Direction side : Direction.values()) {
            transform.fromSide(side);
            if (side == Direction.DOWN) {
                assertFalse(transform.shouldRender(null, BlockPos.ZERO, state));
                continue;
            }
            Vec3 position = transform.getLocalOffset(null, BlockPos.ZERO, state);
            double[] tangent = switch (side.getAxis()) {
                case X -> new double[] {position.y, position.z};
                case Y -> new double[] {position.x, position.z};
                case Z -> new double[] {position.x, position.y};
            };
            double gapA = tangentGapToPipe(tangent[0]);
            double gapB = tangentGapToPipe(tangent[1]);
            assertTrue(Math.hypot(gapA, gapB) > transform.getScale() / 2,
                    "过滤值框命中范围需避开物流管道中央截面：" + side + " at " + position);
            assertTrue(transform.shouldRender(null, BlockPos.ZERO, state));
        }
    }

    private static double tangentGapToPipe(double coordinate) {
        if (coordinate < 0.25) return 0.25 - coordinate;
        if (coordinate > 0.75) return coordinate - 0.75;
        return 0;
    }

    @Test
    void cachedPortsReadNativeFilterChangesForBothItemAndFluid() throws Exception {
        CentrifugeBlockEntity machine = machine();
        Direction side = Direction.NORTH;
        SidedFilteringBehaviour filters = filtering(machine);
        IItemHandler items = machine.itemPort(side);
        IFluidHandler fluids = machine.fluidPort(side);
        CentrifugeState state = state(machine);
        state.enriched = new ItemStack(Items.IRON_INGOT, 2);
        state.slurryMb = 1000;
        state.waterMb = 1000;

        assertEquals(2, items.getStackInSlot(0).getCount());
        assertEquals(1000, fluids.getFluidInTank(0).getAmount());
        filters.setFilter(side, new ItemStack(Items.GOLD_INGOT));
        assertTrue(items.getStackInSlot(0).isEmpty());
        assertTrue(items.extractItem(0, 1, false).isEmpty());

        filters.setFilter(side, new ItemStack(Items.IRON_INGOT));
        assertEquals(2, items.extractItem(0, 2, true).getCount());
        assertEquals(2, items.extractItem(0, 2, false).getCount());
        assertFalse(items.isItemValid(0, new ItemStack(Items.IRON_INGOT)));
    }

    @Test
    void portableFilterSnapshotRoundTripsAndLegacySnapshotClearsFilters() throws Exception {
        CentrifugeBlockEntity machine = machine();
        SidedFilteringBehaviour filters = filtering(machine);
        filters.setFilter(Direction.UP, new ItemStack(Items.IRON_INGOT));
        CompoundTag portable = machine.savePortableData();
        assertTrue(portable.contains("CentrifugeFilters"));

        CentrifugeBlockEntity restored = machine();
        restored.readPortableFilters(portable, RegistryAccess.EMPTY);
        assertTrue(filtering(restored).get(Direction.UP).getFilter().is(Items.IRON_INGOT));

        restored.readPortableFilters(new CompoundTag(), RegistryAccess.EMPTY);
        for (Direction side : Direction.values()) {
            if (side != Direction.DOWN) assertTrue(filtering(restored).get(side).getFilter().isEmpty());
        }
    }
}
