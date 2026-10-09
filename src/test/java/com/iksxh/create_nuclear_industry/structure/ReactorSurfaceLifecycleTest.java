package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.structure.client.ReactorSurfaceSnapshots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 用真实状态机验证加载证据、版本水位、实例替换和世界 token 隔离，不依赖字符串检查。 */
class ReactorSurfaceLifecycleTest {
    @Test
    void unloadCallbackBeforeChunkEventStillAllowsReliableSameGenerationReload() {
        Object world = new Object();
        Object owner = new Object();
        var state = new ReactorSurfaceSnapshots.State();
        state.begin(world, ReactorSurfaceDescriptorTest.DIMENSION);
        var descriptor = ReactorSurfaceDescriptorTest.box(BlockPos.ZERO, 5, 5, 5, UUID.randomUUID(), 2);
        ReactorSurfaceSnapshotTest.load(state, world, owner, descriptor);
        state.ownerUnloaded(world, descriptor.ownerPos(), owner);
        state.rebuild();
        state.chunk(world, new ChunkPos(0, 0), false);
        state.chunk(world, new ChunkPos(0, 0), true);
        Object reloaded = new Object();
        state.ownerLoaded(world, descriptor.ownerPos(), reloaded);
        assertTrue(state.receive(world, reloaded, descriptor));
        state.rebuild();
        assertTrue(state.snapshot().findSurface(BlockPos.ZERO, Direction.NORTH).isPresent());
    }
    @Test
    void memberUnloadRevokesWholeStructureAndReloadRestoresWithoutOwnerReplacement() {
        Object world = new Object();
        Object owner = new Object();
        var state = new ReactorSurfaceSnapshots.State();
        state.begin(world, ReactorSurfaceDescriptorTest.DIMENSION);
        var descriptor = ReactorSurfaceDescriptorTest.box(new BlockPos(14, 0, 14), 5, 5, 5, UUID.randomUUID(), 2);
        ReactorSurfaceSnapshotTest.load(state, world, owner, descriptor);
        var oldSnapshot = state.snapshot();
        var memberChunk = new ChunkPos(1, 1);
        state.chunk(world, memberChunk, false);
        assertFalse(state.rebuild().isEmpty());
        assertTrue(state.snapshot().findSurface(descriptor.origin(), Direction.NORTH).isEmpty());
        assertTrue(oldSnapshot.findSurface(descriptor.origin(), Direction.NORTH).isPresent());
        state.chunk(world, memberChunk, true);
        assertFalse(state.rebuild().isEmpty());
        assertTrue(state.snapshot().findSurface(descriptor.origin(), Direction.NORTH).isPresent());
        assertTrue(state.receive(world, owner, descriptor));
        assertTrue(state.rebuild().isEmpty());
    }

    @Test
    void ownerUnloadNeedsReliableNewOwnerAndRejectsDelayedOldObjects() {
        Object world = new Object();
        Object oldOwner = new Object();
        var state = new ReactorSurfaceSnapshots.State();
        state.begin(world, ReactorSurfaceDescriptorTest.DIMENSION);
        var descriptor = ReactorSurfaceDescriptorTest.box(BlockPos.ZERO, 5, 5, 5, UUID.randomUUID(), 4);
        ReactorSurfaceSnapshotTest.load(state, world, oldOwner, descriptor);
        ChunkPos chunk = new ChunkPos(descriptor.ownerPos());
        state.chunk(world, chunk, false);
        state.rebuild();
        state.ownerUnloaded(world, descriptor.ownerPos(), oldOwner);
        state.chunk(world, chunk, true);
        state.rebuild();
        assertTrue(state.snapshot().findSurface(BlockPos.ZERO, Direction.NORTH).isEmpty());
        assertFalse(state.receive(world, oldOwner, descriptor));
        Object loadedOwner = new Object();
        state.ownerLoaded(world, descriptor.ownerPos(), loadedOwner);
        assertFalse(state.receive(world, loadedOwner, descriptor.withRevision(3)));
        assertTrue(state.receive(world, loadedOwner, descriptor));
        state.rebuild();
        assertTrue(state.snapshot().findSurface(BlockPos.ZERO, Direction.NORTH).isPresent());
        assertFalse(state.receive(world, oldOwner, descriptor.withRevision(9)));
        state.ownerRemoved(world, descriptor.ownerPos(), oldOwner);
        assertTrue(state.rebuild().isEmpty());
    }

    @Test
    void revisionsRemovalReplayAndWorldSwitchNeverReviveOldIdentity() {
        Object world = new Object();
        Object owner = new Object();
        var state = new ReactorSurfaceSnapshots.State();
        state.begin(world, ReactorSurfaceDescriptorTest.DIMENSION);
        var first = ReactorSurfaceDescriptorTest.box(BlockPos.ZERO, 5, 5, 5, UUID.randomUUID(), 5);
        ReactorSurfaceSnapshotTest.load(state, world, owner, first);
        var unavailable = ReactorSurfaceDescriptor.unavailable(first.dimension(), first.ownerPos(), first.ownerGeneration(), 6);
        assertTrue(state.receive(world, owner, unavailable));
        state.rebuild();
        assertFalse(state.receive(world, owner, first));
        assertTrue(state.rebuild().isEmpty());
        state.ownerRemoved(world, first.ownerPos(), owner);
        Object replay = new Object();
        var second = ReactorSurfaceDescriptorTest.box(BlockPos.ZERO, 5, 5, 5, UUID.randomUUID(), 1);
        state.ownerLoaded(world, first.ownerPos(), replay);
        assertFalse(state.receive(world, replay, first.withRevision(20)));
        assertTrue(state.receive(world, replay, second));
        state.rebuild();
        assertFalse(state.receive(world, owner, first));
        Object newWorld = new Object();
        state.begin(newWorld, first.dimension());
        state.chunk(world, new ChunkPos(0, 0), true);
        state.ownerLoaded(world, first.ownerPos(), owner);
        assertFalse(state.receive(world, replay, second));
        assertTrue(state.rebuild().isEmpty());
        assertTrue(state.snapshot().findSurface(BlockPos.ZERO, Direction.NORTH).isEmpty());
    }

    @Test
    void earlyDescriptionRequiresLoadAndCorruptUpdateRevokesWithoutRefreshStorm() {
        Object world = new Object();
        Object owner = new Object();
        var state = new ReactorSurfaceSnapshots.State();
        state.begin(world, ReactorSurfaceDescriptorTest.DIMENSION);
        var descriptor = ReactorSurfaceDescriptorTest.box(BlockPos.ZERO, 5, 5, 5, UUID.randomUUID(), 1);
        assertFalse(state.receive(world, owner, descriptor));
        state.ownerLoaded(world, descriptor.ownerPos(), owner);
        assertTrue(state.receive(world, owner, descriptor));
        assertTrue(state.rebuild().isEmpty());
        state.chunk(world, new ChunkPos(0, 0), true);
        assertFalse(state.rebuild().isEmpty());
        state.unavailable(world, descriptor.ownerPos(), owner);
        assertFalse(state.rebuild().isEmpty());
        state.unavailable(world, descriptor.ownerPos(), owner);
        assertTrue(state.rebuild().isEmpty());
        assertTrue(state.receive(world, owner, descriptor));
        assertFalse(state.rebuild().isEmpty());
    }
}
