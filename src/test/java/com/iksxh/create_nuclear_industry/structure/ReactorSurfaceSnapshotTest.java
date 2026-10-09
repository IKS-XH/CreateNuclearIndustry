package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.structure.client.ReactorSurfaceSnapshot;
import com.iksxh.create_nuclear_industry.structure.client.ReactorSurfaceSnapshots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 验证模型只读查询、独立长宽高、相邻 owner 和不可变历史快照。 */
class ReactorSurfaceSnapshotTest {
    static void load(ReactorSurfaceSnapshots.State state, Object world, Object owner,
                     ReactorSurfaceDescriptor descriptor) {
        for (var member : descriptor.members()) state.chunk(world, new ChunkPos(member.pos()), true);
        state.ownerLoaded(world, descriptor.ownerPos(), owner);
        assertTrue(state.receive(world, owner, descriptor));
        state.rebuild();
    }

    @Test
    void rectangularGeometryOnlyExposesRealFacesAndNeverChangesOldSnapshot() {
        Object world = new Object();
        Object owner = new Object();
        var state = new ReactorSurfaceSnapshots.State();
        state.begin(world, ReactorSurfaceDescriptorTest.DIMENSION);
        for (int[] size : java.util.List.of(new int[]{5, 5, 5}, new int[]{6, 5, 8}, new int[]{9, 7, 5})) {
            state.begin(world, ReactorSurfaceDescriptorTest.DIMENSION);
            var descriptor = ReactorSurfaceDescriptorTest.box(BlockPos.ZERO, size[0], size[1], size[2], UUID.randomUUID(), 1);
            load(state, world, owner, descriptor);
            var captured = state.snapshot();
            assertTrue(captured.findSurface(BlockPos.ZERO, Direction.NORTH).isPresent());
            assertTrue(captured.findSurface(BlockPos.ZERO, Direction.UP).isEmpty());
            assertTrue(captured.findSurface(new BlockPos(1, 1, 1), Direction.NORTH).isEmpty());
            var unavailable = ReactorSurfaceDescriptor.unavailable(descriptor.dimension(), descriptor.ownerPos(), descriptor.ownerGeneration(), 2);
            assertTrue(state.receive(world, owner, unavailable));
            assertFalse(state.rebuild().isEmpty());
            assertTrue(state.snapshot().findSurface(BlockPos.ZERO, Direction.NORTH).isEmpty());
            assertEquals(descriptor.maxInclusive(), captured.findSurface(BlockPos.ZERO, Direction.NORTH).orElseThrow().maxInclusive());
        }
        assertTrue(ReactorSurfaceSnapshot.empty().findSurface(BlockPos.ZERO, Direction.NORTH).isEmpty());
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorSurfaceSnapshots.capture(null));
    }

    @Test
    void adjacentOwnersRemainIndependentAndOverlappingPositionsAreAmbiguous() {
        Object world = new Object();
        Object a = new Object();
        Object b = new Object();
        var state = new ReactorSurfaceSnapshots.State();
        state.begin(world, ReactorSurfaceDescriptorTest.DIMENSION);
        var first = ReactorSurfaceDescriptorTest.box(BlockPos.ZERO, 5, 5, 5, UUID.randomUUID(), 1);
        var second = ReactorSurfaceDescriptorTest.box(new BlockPos(5, 0, 0), 5, 5, 5, UUID.randomUUID(), 1);
        load(state, world, a, first);
        load(state, world, b, second);
        var left = state.snapshot().findSurface(new BlockPos(4, 2, 2), Direction.EAST).orElseThrow();
        var right = state.snapshot().findSurface(new BlockPos(5, 2, 2), Direction.WEST).orElseThrow();
        assertNotEquals(left.ownerGeneration(), right.ownerGeneration());
        var overlapping = ReactorSurfaceDescriptorTest.box(new BlockPos(4, 0, 0), 5, 5, 5, UUID.randomUUID(), 1);
        load(state, world, new Object(), overlapping);
        assertTrue(state.snapshot().findSurface(new BlockPos(4, 2, 2), Direction.EAST).isEmpty());
        assertTrue(state.snapshot().findSurface(new BlockPos(4, 2, 2), Direction.WEST).isEmpty());
    }
}
