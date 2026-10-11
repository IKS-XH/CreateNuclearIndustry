package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.boiler.client.BoilerSurfaceSnapshot;
import com.iksxh.create_nuclear_industry.boiler.client.BoilerSurfaceSnapshots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 验证模型只读查询、独立长宽高、相邻 owner 和不可变历史快照。 */
class BoilerSurfaceSnapshotTest {
    @Test
    void unknownWrapperIsEmptyWithoutConsultingWorldAndMemberAbiIncludesPartition() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var unknown = (net.minecraft.world.level.BlockAndTintGetter) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{net.minecraft.world.level.BlockAndTintGetter.class},
                (proxy, method, arguments) -> { calls.incrementAndGet(); throw new AssertionError("未知包装不应读取世界"); });
        assertSame(BoilerSurfaceSnapshot.empty(), BoilerSurfaceSnapshots.capture(unknown));
        assertEquals(0, calls.get());
        Object world = new Object(), owner = new Object();
        var state = new BoilerSurfaceSnapshots.State(); state.begin(world, BoilerSurfaceDescriptorTest.DIMENSION);
        var descriptor = BoilerSurfaceDescriptorTest.box(new BlockPos(-17, 64, 15), 6, 6, 8, UUID.randomUUID(), 2);
        load(state, world, owner, descriptor);
        var member = state.snapshot().findSurface(descriptor.min(), Direction.DOWN).orElseThrow();
        assertEquals(descriptor.dimension(), member.dimension()); assertEquals(descriptor.ownerPos(), member.ownerPos());
        assertEquals(descriptor.ownerGeneration(), member.ownerGeneration()); assertEquals(descriptor.revision(), member.revision());
        assertEquals(descriptor.min(), member.min()); assertEquals(descriptor.maxInclusive(), member.maxInclusive());
        assertEquals(descriptor.partitionY(), member.partitionY()); assertEquals(descriptor.min(), member.pos());
        assertTrue(member.expectedBlockId().endsWith("_casing"));
        assertThrows(UnsupportedOperationException.class, () -> member.outwardFaces().clear());
    }
    static void load(BoilerSurfaceSnapshots.State state, Object world, Object owner,
                     BoilerSurfaceDescriptor descriptor) {
        for (var member : descriptor.members()) state.chunk(world, new ChunkPos(member.pos()), true);
        state.ownerLoaded(world, descriptor.ownerPos(), owner);
        assertTrue(state.receive(world, owner, descriptor));
        state.rebuild();
    }

    @Test
    void rectangularGeometryOnlyExposesRealFacesAndNeverChangesOldSnapshot() {
        Object world = new Object();
        Object owner = new Object();
        var state = new BoilerSurfaceSnapshots.State();
        state.begin(world, BoilerSurfaceDescriptorTest.DIMENSION);
        for (int[] size : java.util.List.of(new int[]{5, 5, 5}, new int[]{6, 6, 8}, new int[]{9, 7, 5})) {
            state.begin(world, BoilerSurfaceDescriptorTest.DIMENSION);
            var descriptor = BoilerSurfaceDescriptorTest.box(BlockPos.ZERO, size[0], size[1], size[2], UUID.randomUUID(), 1);
            load(state, world, owner, descriptor);
            var captured = state.snapshot();
            assertTrue(captured.findSurface(BlockPos.ZERO, Direction.NORTH).isPresent());
            assertTrue(captured.findSurface(BlockPos.ZERO, Direction.UP).isEmpty());
            assertTrue(captured.findSurface(new BlockPos(1, 1, 1), Direction.NORTH).isEmpty());
            var unavailable = BoilerSurfaceDescriptor.unavailable(descriptor.dimension(), descriptor.ownerPos(), descriptor.ownerGeneration(), 2);
            assertTrue(state.receive(world, owner, unavailable));
            assertFalse(state.rebuild().isEmpty());
            assertTrue(state.snapshot().findSurface(BlockPos.ZERO, Direction.NORTH).isEmpty());
            assertEquals(descriptor.maxInclusive(), captured.findSurface(BlockPos.ZERO, Direction.NORTH).orElseThrow().maxInclusive());
        }
        assertTrue(BoilerSurfaceSnapshot.empty().findSurface(BlockPos.ZERO, Direction.NORTH).isEmpty());
        assertSame(BoilerSurfaceSnapshot.empty(), BoilerSurfaceSnapshots.capture(null));
    }

    @Test
    void adjacentOwnersRemainIndependentAndOverlappingPositionsAreAmbiguous() {
        Object world = new Object();
        Object a = new Object();
        Object b = new Object();
        var state = new BoilerSurfaceSnapshots.State();
        state.begin(world, BoilerSurfaceDescriptorTest.DIMENSION);
        var first = BoilerSurfaceDescriptorTest.box(BlockPos.ZERO, 5, 5, 5, UUID.randomUUID(), 1);
        var second = BoilerSurfaceDescriptorTest.box(new BlockPos(5, 0, 0), 5, 5, 5, UUID.randomUUID(), 1);
        load(state, world, a, first);
        load(state, world, b, second);
        var left = state.snapshot().findSurface(new BlockPos(4, 2, 2), Direction.EAST).orElseThrow();
        var right = state.snapshot().findSurface(new BlockPos(5, 2, 2), Direction.WEST).orElseThrow();
        assertNotEquals(left.ownerGeneration(), right.ownerGeneration());
        var overlapping = BoilerSurfaceDescriptorTest.box(new BlockPos(4, 0, 0), 5, 5, 5, UUID.randomUUID(), 1);
        load(state, world, new Object(), overlapping);
        assertTrue(state.snapshot().findSurface(new BlockPos(4, 2, 2), Direction.EAST).isEmpty());
        assertTrue(state.snapshot().findSurface(new BlockPos(4, 2, 2), Direction.WEST).isEmpty());
    }
}
