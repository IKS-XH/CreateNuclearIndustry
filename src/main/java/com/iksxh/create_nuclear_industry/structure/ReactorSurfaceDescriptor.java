package com.iksxh.create_nuclear_industry.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 服务端扫描缓存的不可变显示投影，坐标单位为世界方块；不保存世界、库存或运行状态。
 * 有效描述必须覆盖长方体全部外表面，owner 必须为其中唯一仪表，棱角保留多个朝外面。
 * 传输预算只保护解码和刷新，不定义游戏成型尺寸。
 */
public record ReactorSurfaceDescriptor(String dimension, BlockPos ownerPos, UUID ownerGeneration,
                                       long revision, boolean valid, BlockPos origin,
                                       BlockPos maxInclusive, List<SurfaceMember> members) {
    public static final int MAX_EDGE = 64;
    public static final int MAX_MEMBERS = 24_000;
    private static final int MAX_COORDINATE = 30_000_000;
    private static final String INSTRUMENT = "create_nuclear_industry:reactor_instrument_port";
    private static final Set<String> BLOCK_IDS = Set.of(INSTRUMENT,
            "create_nuclear_industry:reactor_casing", "create_nuclear_industry:reactor_window",
            "create_nuclear_industry:reactor_cold_port", "create_nuclear_industry:reactor_hot_port",
            "create_nuclear_industry:reactor_refueling_port", "create_nuclear_industry:control_rod_drive");

    public ReactorSurfaceDescriptor {
        if (dimension == null || dimension.length() > 128 || ResourceLocation.tryParse(dimension) == null
                || ownerGeneration == null || revision < 0 || !safePosition(ownerPos)
                || !safePosition(origin) || !safePosition(maxInclusive) || members == null
                || members.size() > MAX_MEMBERS) throw new IllegalArgumentException("invalid surface envelope");
        ownerPos = ownerPos.immutable();
        origin = origin.immutable();
        maxInclusive = maxInclusive.immutable();
        members = List.copyOf(members);
        if (!valid) {
            if (!members.isEmpty() || !origin.equals(ownerPos) || !maxInclusive.equals(ownerPos))
                throw new IllegalArgumentException("invalid unavailable surface");
        } else {
            long width = (long) maxInclusive.getX() - origin.getX() + 1;
            long height = (long) maxInclusive.getY() - origin.getY() + 1;
            long depth = (long) maxInclusive.getZ() - origin.getZ() + 1;
            if (width < 2 || height < 2 || depth < 2 || width > MAX_EDGE || height > MAX_EDGE || depth > MAX_EDGE
                    || members.size() != width * height * depth - (width - 2) * (height - 2) * (depth - 2))
                throw new IllegalArgumentException("invalid surface geometry");
            Set<BlockPos> seen = new HashSet<>();
            int owners = 0;
            for (SurfaceMember member : members) {
                if (member == null || !seen.add(member.pos()) || !inside(member.pos(), origin, maxInclusive)
                        || !member.outwardFaces().equals(outwardFaces(member.pos(), origin, maxInclusive)))
                    throw new IllegalArgumentException("invalid surface member");
                if (INSTRUMENT.equals(member.expectedBlockId())) {
                    if (!member.pos().equals(ownerPos)) throw new IllegalArgumentException("unexpected surface owner");
                    owners++;
                }
            }
            if (owners != 1) throw new IllegalArgumentException("surface owner is missing");
        }
    }

    /** 失效信封仍携带身份和版本，边界收缩到 owner，不能由坏数据扩展渲染刷新范围。 */
    public static ReactorSurfaceDescriptor unavailable(String dimension, BlockPos owner, UUID generation, long revision) {
        return new ReactorSurfaceDescriptor(dimension, owner, generation, revision, false, owner, owner, List.of());
    }

    /** 忽略版本比较真正显示内容；遥测重发不会推进 revision。 */
    public boolean sameGeometry(ReactorSurfaceDescriptor other) {
        return other != null && dimension.equals(other.dimension) && ownerPos.equals(other.ownerPos)
                && ownerGeneration.equals(other.ownerGeneration) && valid == other.valid
                && origin.equals(other.origin) && maxInclusive.equals(other.maxInclusive) && members.equals(other.members);
    }

    /** 仅更换同实例的展示版本，不修改成员或坐标。 */
    public ReactorSurfaceDescriptor withRevision(long nextRevision) {
        return new ReactorSurfaceDescriptor(dimension, ownerPos, ownerGeneration, nextRevision, valid, origin, maxInclusive, members);
    }

    /** 生成独立 NBT；接收方修改此信封不会改变权威描述。 */
    public CompoundTag encode() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Schema", 1);
        tag.putString("Dimension", dimension);
        tag.putIntArray("Owner", coordinates(ownerPos));
        tag.putUUID("Generation", ownerGeneration);
        tag.putLong("Revision", revision);
        tag.putBoolean("Valid", valid);
        tag.putIntArray("Origin", coordinates(origin));
        tag.putIntArray("Max", coordinates(maxInclusive));
        ListTag list = new ListTag();
        for (SurfaceMember member : members) {
            CompoundTag entry = new CompoundTag();
            entry.putIntArray("Pos", coordinates(member.pos()));
            entry.putString("Block", member.expectedBlockId());
            int mask = 0;
            for (Direction face : member.outwardFaces()) mask |= 1 << face.ordinal();
            entry.putInt("Faces", mask);
            list.add(entry);
        }
        tag.put("Members", list);
        return tag;
    }

    /** 严格按 schema/type/几何/大小预算解码；任何未知或错误返回空，不能发布部分归属。 */
    public static Optional<ReactorSurfaceDescriptor> decode(CompoundTag tag) {
        if (tag == null || !tag.contains("Schema", Tag.TAG_INT) || tag.getInt("Schema") != 1
                || !tag.contains("Dimension", Tag.TAG_STRING) || !tag.hasUUID("Generation")
                || !tag.contains("Revision", Tag.TAG_LONG) || !tag.contains("Valid", Tag.TAG_BYTE)
                || !tag.contains("Members", Tag.TAG_LIST)) return Optional.empty();
        Tag rawList = tag.get("Members");
        if (tag.getByte("Valid") != 0 && tag.getByte("Valid") != 1) return Optional.empty();
        if (!(rawList instanceof ListTag list) || list.size() > MAX_MEMBERS
                || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) return Optional.empty();
        try {
            var members = new java.util.ArrayList<SurfaceMember>(list.size());
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (!entry.contains("Block", Tag.TAG_STRING) || !entry.contains("Faces", Tag.TAG_INT)) return Optional.empty();
                int mask = entry.getInt("Faces");
                if (mask <= 0 || mask > 63) return Optional.empty();
                EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
                for (Direction face : Direction.values()) if ((mask & 1 << face.ordinal()) != 0) faces.add(face);
                members.add(new SurfaceMember(position(entry, "Pos"), entry.getString("Block"), faces));
            }
            return Optional.of(new ReactorSurfaceDescriptor(tag.getString("Dimension"), position(tag, "Owner"),
                    tag.getUUID("Generation"), tag.getLong("Revision"), tag.getBoolean("Valid"),
                    position(tag, "Origin"), position(tag, "Max"), members));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /** 计算边界真实朝外的面；内部和边界外坐标都没有外表面归属。 */
    public static Set<Direction> outwardFaces(BlockPos pos, BlockPos min, BlockPos max) {
        if (!inside(pos, min, max)) return Set.of();
        EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
        if (pos.getX() == min.getX()) faces.add(Direction.WEST);
        if (pos.getX() == max.getX()) faces.add(Direction.EAST);
        if (pos.getY() == min.getY()) faces.add(Direction.DOWN);
        if (pos.getY() == max.getY()) faces.add(Direction.UP);
        if (pos.getZ() == min.getZ()) faces.add(Direction.NORTH);
        if (pos.getZ() == max.getZ()) faces.add(Direction.SOUTH);
        return Set.copyOf(faces);
    }

    private static boolean inside(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getY() >= min.getY()
                && pos.getY() <= max.getY() && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    private static boolean safePosition(BlockPos pos) {
        return pos != null && Math.abs((long) pos.getX()) <= MAX_COORDINATE
                && Math.abs((long) pos.getY()) <= MAX_COORDINATE && Math.abs((long) pos.getZ()) <= MAX_COORDINATE;
    }

    private static int[] coordinates(BlockPos pos) { return new int[]{pos.getX(), pos.getY(), pos.getZ()}; }

    private static BlockPos position(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_INT_ARRAY)) throw new IllegalArgumentException("missing surface position");
        int[] xyz = tag.getIntArray(key);
        if (xyz.length != 3) throw new IllegalArgumentException("invalid surface position");
        return new BlockPos(xyz[0], xyz[1], xyz[2]);
    }

    /** 表面单块不可变预期值，面集合不能为空，内部棒体 ID 不属于显示合同。 */
    public record SurfaceMember(BlockPos pos, String expectedBlockId, Set<Direction> outwardFaces) {
        public SurfaceMember {
            if (!safePosition(pos) || !BLOCK_IDS.contains(expectedBlockId) || outwardFaces == null || outwardFaces.isEmpty())
                throw new IllegalArgumentException("invalid surface block");
            pos = pos.immutable();
            outwardFaces = Set.copyOf(outwardFaces);
        }
    }
}
