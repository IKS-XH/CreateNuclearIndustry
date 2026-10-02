package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 只验证无世界状态可检查的端口入口与便携账本；真实配对在客户端人工门检查。 */
final class CentrifugePortsTest {
    private static CentrifugeBlockEntity machine() {
        BlockState block = FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get().defaultBlockState();
        return new CentrifugeBlockEntity(BlockPos.ZERO, block);
    }

    private static CentrifugeState state(CentrifugeBlockEntity machine) throws Exception {
        Field field = CentrifugeBlockEntity.class.getDeclaredField("state");
        field.setAccessible(true);
        return (CentrifugeState) field.get(machine);
    }

    @Test void upperOnlyAcceptsTopSlurryPortAndHorizontalOutputPorts() {
        CentrifugeBlockEntity machine = machine();
        assertNotNull(machine.fluidPort(Direction.UP, true));
        assertNull(machine.fluidPort(Direction.UP, false));
        assertNull(machine.fluidPort(Direction.DOWN, true));
        assertNull(machine.fluidPort(Direction.DOWN, false));
        assertNull(machine.itemPort(Direction.UP, true));
        assertNull(machine.itemPort(Direction.DOWN, false));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            assertNotNull(machine.fluidPort(direction, true));
            assertNotNull(machine.fluidPort(direction, false));
            assertNotNull(machine.itemPort(direction, true));
            assertNotNull(machine.itemPort(direction, false));
        }
        assertNull(machine.fluidPort(null, true));
        assertNull(machine.itemPort(null, false));
        assertEquals(0, machine.fluidPort(Direction.UP, true).getTanks(),
                "Detached cached handlers must not expose material");
        assertEquals(0, machine.itemPort(Direction.NORTH, false).getSlots());
    }

    @Test void oldFilterSnapshotIsIgnoredWhileMaterialRemains() throws Exception {
        CentrifugeBlockEntity machine = machine();
        CentrifugeState state = state(machine);
        state.slurryMb = 1200;
        state.waterMb = 900;
        state.enriched = new ItemStack(Items.IRON_INGOT, 3);
        CompoundTag saved = machine.savePortableData();
        assertEquals(1200, saved.getInt("SlurryMb"));
        assertEquals(900, saved.getInt("WaterMb"));
        assertFalse(saved.contains("CentrifugeFilters"));
        saved.put("CentrifugeFilters", new CompoundTag());
        CentrifugeState restored = new CentrifugeState();
        restored.read(saved, net.minecraft.core.RegistryAccess.EMPTY);
        assertEquals(1200, restored.slurryMb);
        assertEquals(900, restored.waterMb);
        assertEquals(3, restored.enriched.getCount());
    }

    @Test void repeatedPortableDropQueriesDoNotClaimRemoval() throws Exception {
        CentrifugeBlockEntity machine = machine();
        CentrifugeState state = state(machine);
        state.slurryMb = 750;
        state.enriched = new ItemStack(Items.IRON_INGOT, 2);
        CompoundTag first = CentrifugeBlock.portableTag(machine);
        CompoundTag second = CentrifugeBlock.portableTag(machine);
        assertEquals(first, second);
        assertEquals(750, first.getCompound("CniCentrifuge").getInt("SlurryMb"));
        assertEquals(2, state.enriched.getCount());
        assertFalse(machine.isRemovalHandled(), "纯掉落快照不占用真实拆卸事务");
    }
}
