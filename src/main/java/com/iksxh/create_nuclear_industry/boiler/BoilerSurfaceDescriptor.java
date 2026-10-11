package com.iksxh.create_nuclear_industry.boiler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 服务端已决定 Form 的不可变表面投影，坐标及隔层均为世界方块单位，不含世界、BE 或库存引用。
 * 合法信封覆盖全部闭外表面，唯一 controller 位于 owner；32 格上限仅限制显示解码预算。
 */
public record BoilerSurfaceDescriptor(String dimension, BlockPos ownerPos, UUID ownerGeneration,
        long revision, boolean valid, BlockPos min, BlockPos maxInclusive, int partitionY,
        List<SurfaceMember> members) {
    public static final int MAX_EDGE = 32;
    public static final int MAX_MEMBERS = 5768;
    static final List<String> BLOCK_IDS = List.of(
            "create_nuclear_industry:high_pressure_boiler_casing",
            "create_nuclear_industry:high_pressure_boiler_window",
            "create_nuclear_industry:high_pressure_boiler_controller",
            "create_nuclear_industry:high_pressure_boiler_water_port",
            "create_nuclear_industry:high_pressure_boiler_steam_port",
            "create_nuclear_industry:high_pressure_boiler_hot_coolant_port",
            "create_nuclear_industry:high_pressure_boiler_cold_coolant_port",
            "create_nuclear_industry:boiler_safety_valve",
            "create_nuclear_industry:nuclear_heat_exchanger");
    private static final int MAX_COORDINATE = 30_000_000;

    public BoilerSurfaceDescriptor {
        if (dimension == null || dimension.length() > 128 || ResourceLocation.tryParse(dimension) == null
                || ownerGeneration == null || revision < 0 || !safePosition(ownerPos) || !safePosition(min)
                || !safePosition(maxInclusive) || members == null || members.size() > MAX_MEMBERS)
            throw new IllegalArgumentException("invalid boiler surface envelope");
        ownerPos = ownerPos.immutable(); min = min.immutable(); maxInclusive = maxInclusive.immutable();
        if (!valid) {
            if (!members.isEmpty() || !min.equals(ownerPos) || !maxInclusive.equals(ownerPos)
                    || partitionY != ownerPos.getY()) throw new IllegalArgumentException("invalid unavailable surface");
            members = List.of();
        } else {
            int expected = memberCount(min, maxInclusive, partitionY);
            if (members.size() != expected) throw new IllegalArgumentException("incomplete boiler surface");
            var byPosition = new HashMap<BlockPos, SurfaceMember>();
            int owners = 0;
            for (SurfaceMember member : members) {
                if (member == null || byPosition.putIfAbsent(member.pos(), member) != null
                        || !member.outwardFaces().equals(outwardFaces(member.pos(), min, maxInclusive)))
                    throw new IllegalArgumentException("invalid boiler surface member");
                if (BLOCK_IDS.get(2).equals(member.expectedBlockId())) {
                    if (!member.pos().equals(ownerPos)) throw new IllegalArgumentException("unexpected boiler owner");
                    owners++;
                }
            }
            if (owners != 1) throw new IllegalArgumentException("missing boiler owner");
            // 与 BlockPos.betweenClosed 一致，x 最快、随后 y、最后 z；wire 无需重复坐标或面掩码。
            var canonical = new ArrayList<SurfaceMember>(expected);
            for (BlockPos pos : BlockPos.betweenClosed(min, maxInclusive)) {
                if (!outwardFaces(pos, min, maxInclusive).isEmpty()) canonical.add(byPosition.get(pos));
            }
            members = List.copyOf(canonical);
        }
    }

    /** 失效信封保留身份和版本，刷新边界只收缩至 owner，不接受任意外包围盒。 */
    public static BoilerSurfaceDescriptor unavailable(String dimension, BlockPos owner, UUID generation, long revision) {
        return new BoilerSurfaceDescriptor(dimension, owner, generation, revision, false, owner, owner, owner.getY(), List.of());
    }

    /** 忽略版本比较实际几何及角色，遥测或运行状态变化不会推进 revision。 */
    public boolean sameGeometry(BoilerSurfaceDescriptor other) {
        return other != null && dimension.equals(other.dimension) && ownerPos.equals(other.ownerPos)
                && ownerGeneration.equals(other.ownerGeneration) && valid == other.valid && min.equals(other.min)
                && maxInclusive.equals(other.maxInclusive) && partitionY == other.partitionY && members.equals(other.members);
    }

    /** 在同一实例内替换版本，保持不可变几何。 */
    public BoilerSurfaceDescriptor withRevision(long nextRevision) {
        return new BoilerSurfaceDescriptor(dimension, ownerPos, ownerGeneration, nextRevision, valid, min, maxInclusive, partitionY, members);
    }

    /** 固定 schema1，每表面一 byte 注册角色；返回新 NBT，编码只读取冻结值。 */
    public CompoundTag encode() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Schema", 1); tag.putString("Dimension", dimension);
        tag.putIntArray("Owner", coordinates(ownerPos)); tag.putUUID("Generation", ownerGeneration);
        tag.putLong("Revision", revision); tag.putBoolean("Valid", valid);
        tag.putIntArray("Min", coordinates(min)); tag.putIntArray("Max", coordinates(maxInclusive));
        tag.putInt("PartitionY", partitionY);
        byte[] ids = new byte[members.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = (byte) BLOCK_IDS.indexOf(members.get(i).expectedBlockId());
        tag.putByteArray("Members", ids);
        return tag;
    }

    /** 先验证类型和有界几何，再枚举唯一顺序；坏包整台撤销，不发布部分归属或坏坐标。 */
    public static Optional<BoilerSurfaceDescriptor> decode(CompoundTag tag) {
        if (tag == null || !tag.contains("Schema", Tag.TAG_INT) || tag.getInt("Schema") != 1
                || !tag.contains("Dimension", Tag.TAG_STRING) || !tag.hasUUID("Generation")
                || !tag.contains("Revision", Tag.TAG_LONG) || !tag.contains("Valid", Tag.TAG_BYTE)
                || !tag.contains("PartitionY", Tag.TAG_INT) || !tag.contains("Members", Tag.TAG_BYTE_ARRAY)
                || tag.getByte("Valid") < 0 || tag.getByte("Valid") > 1) return Optional.empty();
        try {
            BlockPos owner = position(tag, "Owner"), min = position(tag, "Min"), max = position(tag, "Max");
            int partition = tag.getInt("PartitionY");
            byte[] ids = tag.getByteArray("Members");
            boolean valid = tag.getBoolean("Valid");
            int count = valid ? memberCount(min, max, partition) : 0;
            if (ids.length != count) return Optional.empty();
            var members = new ArrayList<SurfaceMember>(count);
            if (valid) {
                int i = 0;
                for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                    var faces = outwardFaces(pos, min, max);
                    if (faces.isEmpty()) continue;
                    int index = ids[i++];
                    if (index < 0 || index >= BLOCK_IDS.size()) return Optional.empty();
                    members.add(new SurfaceMember(pos, BLOCK_IDS.get(index), faces));
                }
            }
            return Optional.of(new BoilerSurfaceDescriptor(tag.getString("Dimension"), owner, tag.getUUID("Generation"),
                    tag.getLong("Revision"), valid, min, max, partition, members));
        } catch (IllegalArgumentException exception) { return Optional.empty(); }
    }

    static int memberCount(BlockPos min, BlockPos max, int partition) {
        if (!safePosition(min) || !safePosition(max)) throw new IllegalArgumentException("unsafe bounds");
        long w = (long) max.getX() - min.getX() + 1, h = (long) max.getY() - min.getY() + 1,
                d = (long) max.getZ() - min.getZ() + 1;
        if (w < 5 || h < 5 || d < 5 || w > MAX_EDGE || h > MAX_EDGE || d > MAX_EDGE
                || partition < min.getY() + 2 || partition > max.getY() - 2)
            throw new IllegalArgumentException("invalid boiler geometry");
        return (int) (w * h * d - (w - 2) * (h - 2) * (d - 2));
    }

    /** 六向真实外面，内部和边界外为空；棱角及底边热口不会丢失第二或第三面。 */
    public static Set<Direction> outwardFaces(BlockPos pos, BlockPos min, BlockPos max) {
        if (pos.getX() < min.getX() || pos.getX() > max.getX() || pos.getY() < min.getY()
                || pos.getY() > max.getY() || pos.getZ() < min.getZ() || pos.getZ() > max.getZ()) return Set.of();
        EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
        if (pos.getX() == min.getX()) faces.add(Direction.WEST);
        if (pos.getX() == max.getX()) faces.add(Direction.EAST);
        if (pos.getY() == min.getY()) faces.add(Direction.DOWN);
        if (pos.getY() == max.getY()) faces.add(Direction.UP);
        if (pos.getZ() == min.getZ()) faces.add(Direction.NORTH);
        if (pos.getZ() == max.getZ()) faces.add(Direction.SOUTH);
        return Set.copyOf(faces);
    }
    private static boolean safePosition(BlockPos pos) {
        return pos != null && Math.abs((long) pos.getX()) <= MAX_COORDINATE
                && Math.abs((long) pos.getY()) <= MAX_COORDINATE && Math.abs((long) pos.getZ()) <= MAX_COORDINATE;
    }
    private static int[] coordinates(BlockPos p) { return new int[]{p.getX(), p.getY(), p.getZ()}; }
    private static BlockPos position(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_INT_ARRAY)) throw new IllegalArgumentException("missing position");
        int[] xyz = tag.getIntArray(key);
        if (xyz.length != 3) throw new IllegalArgumentException("invalid position");
        return new BlockPos(xyz[0], xyz[1], xyz[2]);
    }

    /** 单块预期 ID 和朝外面均不可变，不携带实际方块或客户端采样引用。 */
    public record SurfaceMember(BlockPos pos, String expectedBlockId, Set<Direction> outwardFaces) {
        public SurfaceMember {
            if (!safePosition(pos) || !BLOCK_IDS.contains(expectedBlockId) || outwardFaces == null || outwardFaces.isEmpty())
                throw new IllegalArgumentException("invalid surface block");
            pos = pos.immutable(); outwardFaces = Set.copyOf(outwardFaces);
        }
    }
}
