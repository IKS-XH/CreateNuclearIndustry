package com.iksxh.create_nuclear_industry.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 验证传输描述的几何、防御复制和拒绝坏包行为；假设尺寸不进入成型扫描。 */
class ReactorSurfaceDescriptorTest {
    static final String CASING = "create_nuclear_industry:reactor_casing";
    static final String INSTRUMENT = "create_nuclear_industry:reactor_instrument_port";
    static final String DIMENSION = "minecraft:overworld";
    static final UUID GENERATION = UUID.randomUUID();

    static ReactorSurfaceDescriptor box(BlockPos origin, int width, int height, int depth,
                                        UUID generation, long revision) {
        BlockPos max = origin.offset(width - 1, height - 1, depth - 1);
        BlockPos owner = origin.offset(1, 1, 0);
        List<ReactorSurfaceDescriptor.SurfaceMember> members = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    var faces = ReactorSurfaceDescriptor.outwardFaces(pos, origin, max);
                    if (!faces.isEmpty()) {
                        members.add(new ReactorSurfaceDescriptor.SurfaceMember(pos,
                                pos.equals(owner) ? INSTRUMENT : CASING, faces));
                    }
                }
            }
        }
        return new ReactorSurfaceDescriptor(DIMENSION, owner, generation, revision,
                true, origin, max, members);
    }

    @Test
    void roundTripPreservesAllRectangularFacesAndCornerDirections() {
        for (int[] size : List.of(new int[]{5, 5, 5}, new int[]{6, 5, 8}, new int[]{9, 7, 5})) {
            var descriptor = box(new BlockPos(-17, 70, 15), size[0], size[1], size[2], GENERATION, 3);
            assertEquals(descriptor, ReactorSurfaceDescriptor.decode(descriptor.encode()).orElseThrow());
            assertEquals(java.util.Set.of(Direction.WEST, Direction.DOWN, Direction.NORTH),
                    ReactorSurfaceDescriptor.outwardFaces(descriptor.origin(), descriptor.origin(), descriptor.maxInclusive()));
            assertTrue(ReactorSurfaceDescriptor.outwardFaces(descriptor.origin().offset(1, 1, 1),
                    descriptor.origin(), descriptor.maxInclusive()).isEmpty());
        }
    }

    @Test
    void mutablePositionsAndCollectionsCannotAlterPublishedDescriptor() {
        var original = box(BlockPos.ZERO, 5, 5, 5, GENERATION, 1);
        BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos(0, 0, 0);
        List<ReactorSurfaceDescriptor.SurfaceMember> list = new ArrayList<>(original.members());
        var copied = new ReactorSurfaceDescriptor(DIMENSION, original.ownerPos(), GENERATION, 1,
                true, origin, original.maxInclusive(), list);
        origin.set(100, 100, 100);
        list.clear();
        assertEquals(original, copied);
        assertThrows(UnsupportedOperationException.class, () -> copied.members().clear());
        assertThrows(UnsupportedOperationException.class, () -> copied.members().getFirst().outwardFaces().clear());
    }

    @Test
    void corruptAndOversizedPacketsAreUnavailable() {
        var valid = box(BlockPos.ZERO, 5, 5, 5, GENERATION, 1).encode();
        CompoundTag duplicate = valid.copy();
        var members = duplicate.getList("Members", 10);
        members.add(members.getCompound(0).copy());
        assertTrue(ReactorSurfaceDescriptor.decode(duplicate).isEmpty());
        CompoundTag huge = valid.copy();
        huge.putIntArray("Max", new int[]{Integer.MAX_VALUE, 4, 4});
        assertTrue(ReactorSurfaceDescriptor.decode(huge).isEmpty());
        CompoundTag badOwner = valid.copy();
        badOwner.putIntArray("Owner", new int[]{2, 2, 2});
        assertTrue(ReactorSurfaceDescriptor.decode(badOwner).isEmpty());
        CompoundTag missing = valid.copy();
        missing.getList("Members", 10).remove(0);
        assertTrue(ReactorSurfaceDescriptor.decode(missing).isEmpty());
        CompoundTag badFaces = valid.copy();
        badFaces.getList("Members", 10).getCompound(0).putInt("Faces", 63);
        assertTrue(ReactorSurfaceDescriptor.decode(badFaces).isEmpty());
        ListTag giantList = new ListTag();
        for (int i = 0; i <= ReactorSurfaceDescriptor.MAX_MEMBERS; i++) giantList.add(new CompoundTag());
        huge.put("Members", giantList);
        assertTrue(ReactorSurfaceDescriptor.decode(huge).isEmpty());
        assertTrue(ReactorSurfaceDescriptor.decode(new CompoundTag()).isEmpty());
    }
}
