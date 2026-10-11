package com.iksxh.create_nuclear_industry.boiler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** 验证纯 Form 投影、全部角色和实际紧凑 NBT 预算；不启动世界或重判玩法结构。 */
class BoilerSurfaceDescriptorTest {
    static final String DIMENSION = "minecraft:overworld";
    static BoilerStructure.Form form(BlockPos min, int w, int h, int d) {
        return new BoilerStructure.Form(min, min.offset(w - 1, h - 1, d - 1), min.getY() + 2,
                (w - 2) * (d - 2), (w - 2) * (d - 2) * (h - 4),
                List.of(min.offset(1, 2, 1)), List.of(min.offset(1, 0, 1)),
                List.of(min.offset(2, 1, 0)), List.of(min.offset(0, 3, 1)),
                List.of(min.offset(3, 0, 0)), List.of(min.offset(3, 2, 0)),
                List.of(min.offset(w - 1, 1, 2)), min.offset(1, h - 1, 1));
    }
    static BoilerSurfaceDescriptor box(BlockPos min, int w, int h, int d, UUID generation, long revision) {
        return BoilerSurfaceDescriptorFactory.project(DIMENSION, min.offset(1, 1, 0), generation, revision, form(min, w, h, d));
    }
    @Test void completeCompactSurfaceIsAcceptedAndProjectsNineActualRoles() {
        for (int[] size : List.of(new int[]{5, 5, 5}, new int[]{6, 6, 8}, new int[]{11, 11, 11}, new int[]{32, 32, 32})) {
            BlockPos min = new BlockPos(-17, 70, 15);
            var descriptor = box(min, size[0], size[1], size[2], UUID.randomUUID(), 3);
            assertTrue(descriptor.valid());
            assertEquals(descriptor, BoilerSurfaceDescriptor.decode(descriptor.encode()).orElseThrow());
            assertEquals(min.getY() + 2, descriptor.partitionY());
            assertEquals(9, descriptor.members().stream().map(BoilerSurfaceDescriptor.SurfaceMember::expectedBlockId).distinct().count());
            assertEquals(1, descriptor.members().stream().filter(m -> m.expectedBlockId().endsWith("_controller")).count());
            assertEquals(Set.of(Direction.WEST, Direction.DOWN, Direction.NORTH), descriptor.members().getFirst().outwardFaces());
            var hot = descriptor.members().stream().filter(m -> m.pos().equals(min.offset(3, 0, 0))).findFirst().orElseThrow();
            assertEquals(Set.of(Direction.DOWN, Direction.NORTH), hot.outwardFaces());
            assertTrue(hot.expectedBlockId().endsWith("_hot_coolant_port"));
            assertTrue(descriptor.members().stream().noneMatch(m -> m.pos().equals(min.offset(1, 2, 1))));
        }
    }
    @Test void maximumWireBudgetAndMisalignedChunkCountAreBounded() throws Exception {
        var descriptor = box(new BlockPos(15, 15, 15), 32, 32, 32, UUID.randomUUID(), 1);
        assertEquals(5768, descriptor.members().size());
        assertEquals(6144, descriptor.members().stream().mapToInt(m -> m.outwardFaces().size()).sum());
        assertEquals(9, descriptor.members().stream().map(m -> new ChunkPos(m.pos())).distinct().count());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        NbtIo.write(descriptor.encode(), new DataOutputStream(buffer));
        assertTrue(buffer.size() <= 16 * 1024, "实际 NBT 超过显示传输预算");
        Path report = Path.of("build/reports/art/ART-BOILER-05-L1/nbt-budget.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "32³ members=5768 outwardFaces=6144 chunks=9 NBT bytes=" + buffer.size());
    }
    @Test void mutableInputsAndWireCannotAlterDescriptor() {
        var original = box(BlockPos.ZERO, 6, 6, 8, UUID.randomUUID(), 1);
        var min = new BlockPos.MutableBlockPos();
        var members = new ArrayList<>(original.members());
        var copied = new BoilerSurfaceDescriptor(DIMENSION, original.ownerPos(), original.ownerGeneration(), 1,
                true, min, original.maxInclusive(), 2, members);
        min.set(100, 100, 100); members.clear();
        assertEquals(original, copied);
        assertThrows(UnsupportedOperationException.class, () -> copied.members().clear());
        assertThrows(UnsupportedOperationException.class, () -> copied.members().getFirst().outwardFaces().clear());
        var wire = copied.encode(); wire.getByteArray("Members")[0] = 8;
        assertEquals(original, copied);
        var shuffled = new ArrayList<>(original.members()); java.util.Collections.reverse(shuffled);
        assertEquals(original, new BoilerSurfaceDescriptor(DIMENSION, original.ownerPos(), original.ownerGeneration(), 1,
                true, original.min(), original.maxInclusive(), 2, shuffled));
    }
    @Test void badTypesSchemasSizesIndexesPartitionsAndOwnersAreRejected() {
        var tag = box(BlockPos.ZERO, 5, 5, 5, UUID.randomUUID(), 1).encode();
        List<java.util.function.Consumer<CompoundTag>> mutations = List.of(
                t -> t.putString("Schema", "1"), t -> t.putInt("Schema", 2), t -> t.remove("Generation"),
                t -> t.putInt("Revision", 1), t -> t.putLong("Revision", -1), t -> t.putByte("Valid", (byte) 2),
                t -> t.putString("Dimension", "a".repeat(129)), t -> t.putString("Dimension", "bad dimension"),
                t -> t.putIntArray("Min", new int[]{0, 0}), t -> t.putIntArray("Max", new int[]{32, 4, 4}),
                t -> t.putIntArray("Max", new int[]{Integer.MAX_VALUE, 4, 4}), t -> t.putInt("PartitionY", 1),
                t -> t.putByteArray("Members", new byte[97]), t -> t.putByteArray("Members", new byte[99]),
                t -> t.putByteArray("Members", new byte[0]), t -> t.getByteArray("Members")[0] = 9,
                t -> t.getByteArray("Members")[0] = -1, t -> t.getByteArray("Members")[0] = 2,
                t -> t.putIntArray("Owner", new int[]{2, 2, 2}));
        for (var mutation : mutations) { var bad = tag.copy(); mutation.accept(bad); assertTrue(BoilerSurfaceDescriptor.decode(bad).isEmpty()); }
        assertTrue(BoilerSurfaceDescriptor.decode(new CompoundTag()).isEmpty());
        var unavailable = BoilerSurfaceDescriptor.unavailable(DIMENSION, new BlockPos(1, 1, 0), UUID.randomUUID(), 2);
        assertEquals(unavailable, BoilerSurfaceDescriptor.decode(unavailable.encode()).orElseThrow());
        var badInvalid = unavailable.encode(); badInvalid.putIntArray("Max", new int[]{4, 4, 4});
        assertTrue(BoilerSurfaceDescriptor.decode(badInvalid).isEmpty());
    }
    @Test void sameGeometryIgnoresRevisionAndFactoryDoesNotTrustInvalidPublicForms() {
        var first = box(BlockPos.ZERO, 6, 6, 8, UUID.randomUUID(), 1);
        assertTrue(first.sameGeometry(first.withRevision(12)));
        assertFalse(first.sameGeometry(box(BlockPos.ZERO, 6, 6, 8, UUID.randomUUID(), 1)));
        assertFalse(BoilerSurfaceDescriptorFactory.project(DIMENSION, first.ownerPos(), first.ownerGeneration(), 2, null).valid());
        assertFalse(BoilerSurfaceDescriptorFactory.project(DIMENSION, first.ownerPos(), first.ownerGeneration(), 2,
                form(BlockPos.ZERO, 33, 5, 5)).valid());
    }
}
