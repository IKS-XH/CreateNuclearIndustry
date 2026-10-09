package com.iksxh.create_nuclear_industry.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 一次成功服务端结算的完整不可变显示信封；世界坐标用方块，库存用 mB，功率用 HU/t。
 * generation 与 geometryRevision 引用 L1；revision/sample 为独立 L2 水位，不保存世界或库存引用。
 * 客户端会话和租约属于接收端，不能由此信封伪造。不可用信封仅携带身份及水位。
 */
public record ReactorRuntimeDescriptor(String dimension, BlockPos ownerPos, UUID ownerGeneration,
        long revision, long geometryRevision, long sample, long serverGameTime, boolean available,
        BlockPos origin, BlockPos maxInclusive, long coldCoolantMb, long hotCoolantMb,
        long coolantCapacityMb, double convertedCoolantMbPerTick, List<Column> columns) {
    public static final int MAX_COLUMNS = 256;
    public static final int MAX_BODY_POSITIONS = 4096;
    private static final String PREFIX = "create_nuclear_industry:";

    public ReactorRuntimeDescriptor {
        require(dimension != null && dimension.length() <= 128 && ResourceLocation.tryParse(dimension) != null);
        require(ownerGeneration != null && revision >= 0 && geometryRevision >= 0 && sample >= 0 && serverGameTime >= 0);
        require(safe(ownerPos) && safe(origin) && safe(maxInclusive) && columns != null && columns.size() <= MAX_COLUMNS);
        ownerPos = ownerPos.immutable(); origin = origin.immutable(); maxInclusive = maxInclusive.immutable();
        columns = List.copyOf(columns);
        require(coldCoolantMb >= 0 && hotCoolantMb >= 0 && coolantCapacityMb >= 0);
        nonnegative(convertedCoolantMbPerTick);
        if (!available) {
            require(columns.isEmpty() && origin.equals(ownerPos) && maxInclusive.equals(ownerPos)
                    && coldCoolantMb == 0 && hotCoolantMb == 0 && coolantCapacityMb == 0 && convertedCoolantMbPerTick == 0);
        } else {
            long width = (long) maxInclusive.getX() - origin.getX() + 1;
            long height = (long) maxInclusive.getY() - origin.getY() + 1;
            long depth = (long) maxInclusive.getZ() - origin.getZ() + 1;
            require(width >= 3 && height >= 3 && depth >= 3 && width <= ReactorSurfaceDescriptor.MAX_EDGE
                    && height <= ReactorSurfaceDescriptor.MAX_EDGE && depth <= ReactorSurfaceDescriptor.MAX_EDGE);
            require(inside(ownerPos, origin, maxInclusive)
                    && !ReactorSurfaceDescriptor.outwardFaces(ownerPos, origin, maxInclusive).isEmpty()
                    && columns.size() == (width - 2) * (depth - 2));
            Set<BlockPos> caps = new HashSet<>(); Set<BlockPos> bodies = new HashSet<>();
            for (Column column : columns) {
                require(column != null);
                BlockPos cap = column.capPos();
                require(caps.add(cap) && cap.getY() == maxInclusive.getY()
                        && cap.getX() > origin.getX() && cap.getX() < maxInclusive.getX()
                        && cap.getZ() > origin.getZ() && cap.getZ() < maxInclusive.getZ()
                        && column.bodyPositions().size() == height - 2);
                for (BlockPos body : column.bodyPositions()) {
                    require(body.getX() == cap.getX() && body.getZ() == cap.getZ()
                            && body.getY() > origin.getY() && body.getY() < maxInclusive.getY() && bodies.add(body));
                }
                require(bodies.size() <= MAX_BODY_POSITIONS);
            }
        }
    }

    /** 显示撤销仍携带版本，绝不保留上一份库存或动画。 */
    public static ReactorRuntimeDescriptor unavailable(String dimension, BlockPos owner, UUID generation,
            long revision, long geometryRevision, long sample, long serverGameTime) {
        return new ReactorRuntimeDescriptor(dimension, owner, generation, revision, geometryRevision, sample,
                serverGameTime, false, owner, owner, 0, 0, 0, 0, List.of());
    }

    /** 唯一合法冷却空间：空列和控制棒列的主体；不包括燃料或列帽。 */
    public Set<BlockPos> coolantSpace() {
        Set<BlockPos> result = new HashSet<>();
        for (Column column : columns) if (!(column instanceof FuelColumn)) result.addAll(column.bodyPositions());
        return Set.copyOf(result);
    }

    /** 独立的网络 NBT；本信封不写入服务端持久化。 */
    public CompoundTag encode() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Schema", 1); tag.putString("Dimension", dimension); tag.putUUID("Generation", ownerGeneration);
        putPos(tag, "Owner", ownerPos); putPos(tag, "Origin", origin); putPos(tag, "Max", maxInclusive);
        tag.putLong("Revision", revision); tag.putLong("GeometryRevision", geometryRevision);
        tag.putLong("Sample", sample); tag.putLong("ServerGameTime", serverGameTime); tag.putBoolean("Available", available);
        tag.putLong("Cold", coldCoolantMb); tag.putLong("Hot", hotCoolantMb); tag.putLong("Capacity", coolantCapacityMb);
        tag.putDouble("Converted", convertedCoolantMbPerTick);
        ListTag list = new ListTag();
        for (Column column : columns) {
            CompoundTag entry = new CompoundTag(); putPos(entry, "Cap", column.capPos());
            ListTag body = new ListTag();
            for (BlockPos pos : column.bodyPositions()) { CompoundTag cell = new CompoundTag(); putPos(cell, "Pos", pos); body.add(cell); }
            entry.put("Body", body); entry.putString("CapId", column.expectedCapBlockId());
            entry.putString("BodyId", column.expectedBodyBlockId());
            if (column instanceof FuelColumn fuel) {
                entry.putString("Type", "fuel"); entry.putBoolean("Usable", fuel.fuelUsable()); entry.putDouble("Fission", fuel.fissionHeatHuPerTick());
            } else if (column instanceof ControlRodColumn rod) {
                entry.putString("Type", "control"); entry.putDouble("Actual", rod.actualDepth());
                entry.putDouble("Target", rod.targetDepth()); entry.putBoolean("Jammed", rod.jammed());
            } else entry.putString("Type", "empty");
            list.add(entry);
        }
        tag.put("Columns", list); return tag;
    }

    /** 拒收缺字段、错类型、非有限数值和不完整几何；坏包不能续用 owner 旧样本。 */
    public static Optional<ReactorRuntimeDescriptor> decode(CompoundTag tag) {
        if (tag == null) return Optional.empty();
        try {
            typed(tag, "Schema", Tag.TAG_INT); require(tag.getInt("Schema") == 1);
            typed(tag, "Dimension", Tag.TAG_STRING); require(tag.hasUUID("Generation"));
            for (String key : List.of("Revision", "GeometryRevision", "Sample", "ServerGameTime", "Cold", "Hot", "Capacity")) typed(tag, key, Tag.TAG_LONG);
            bool(tag, "Available"); typed(tag, "Converted", Tag.TAG_DOUBLE);
            ListTag list = compounds(tag, "Columns", MAX_COLUMNS);
            List<Column> columns = new ArrayList<>(); int bodyCount = 0;
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i); typed(entry, "Type", Tag.TAG_STRING);
                typed(entry, "CapId", Tag.TAG_STRING); typed(entry, "BodyId", Tag.TAG_STRING);
                ListTag body = compounds(entry, "Body", MAX_BODY_POSITIONS);
                bodyCount += body.size(); require(bodyCount <= MAX_BODY_POSITIONS);
                List<BlockPos> positions = new ArrayList<>();
                for (int j = 0; j < body.size(); j++) positions.add(pos(body.getCompound(j), "Pos"));
                BlockPos cap = pos(entry, "Cap");
                Column column = switch (entry.getString("Type")) {
                    case "fuel" -> { bool(entry, "Usable"); typed(entry, "Fission", Tag.TAG_DOUBLE);
                        yield new FuelColumn(cap, positions, entry.getBoolean("Usable"), entry.getDouble("Fission")); }
                    case "control" -> { bool(entry, "Jammed"); typed(entry, "Actual", Tag.TAG_DOUBLE); typed(entry, "Target", Tag.TAG_DOUBLE);
                        yield new ControlRodColumn(cap, positions, entry.getDouble("Actual"), entry.getDouble("Target"), entry.getBoolean("Jammed")); }
                    case "empty" -> new EmptyColumn(cap, positions);
                    default -> throw new IllegalArgumentException("unknown runtime column");
                };
                require(column.expectedCapBlockId().equals(entry.getString("CapId")) && column.expectedBodyBlockId().equals(entry.getString("BodyId")));
                columns.add(column);
            }
            return Optional.of(new ReactorRuntimeDescriptor(tag.getString("Dimension"), pos(tag, "Owner"), tag.getUUID("Generation"),
                    tag.getLong("Revision"), tag.getLong("GeometryRevision"), tag.getLong("Sample"), tag.getLong("ServerGameTime"),
                    tag.getBoolean("Available"), pos(tag, "Origin"), pos(tag, "Max"), tag.getLong("Cold"), tag.getLong("Hot"),
                    tag.getLong("Capacity"), tag.getDouble("Converted"), columns));
        } catch (IllegalArgumentException exception) { return Optional.empty(); }
    }

    /** 列几何以世界坐标及预期注册 ID 表示；消费者还须核对当次本地方块。 */
    public sealed interface Column permits FuelColumn, ControlRodColumn, EmptyColumn {
        BlockPos capPos(); List<BlockPos> bodyPositions(); String expectedCapBlockId(); String expectedBodyBlockId();
    }
    /** 燃料可用状态来自结算后快照；本 tick 功率来自裂变阶段，耗尽 tick 二者可不同。 */
    public record FuelColumn(BlockPos capPos, List<BlockPos> bodyPositions, boolean fuelUsable, double fissionHeatHuPerTick) implements Column {
        public FuelColumn { capPos = checked(capPos); bodyPositions = copyBody(bodyPositions); nonnegative(fissionHeatHuPerTick); }
        public String expectedCapBlockId() { return PREFIX + "reactor_refueling_port"; }
        public String expectedBodyBlockId() { return PREFIX + "reactor_fuel_rod"; }
    }
    /** 深度 0 为拔出、1 为插入，实际行程按 bodyPositions.size()；卡死时实际不能用目标替代。 */
    public record ControlRodColumn(BlockPos capPos, List<BlockPos> bodyPositions, double actualDepth, double targetDepth, boolean jammed) implements Column {
        public ControlRodColumn { capPos = checked(capPos); bodyPositions = copyBody(bodyPositions); unit(actualDepth); unit(targetDepth); }
        public String expectedCapBlockId() { return PREFIX + "control_rod_drive"; }
        public String expectedBodyBlockId() { return "minecraft:air"; }
    }
    /** 空列没有运行状态，只有合法冷却空间及静态列帽。 */
    public record EmptyColumn(BlockPos capPos, List<BlockPos> bodyPositions) implements Column {
        public EmptyColumn { capPos = checked(capPos); bodyPositions = copyBody(bodyPositions); }
        public String expectedCapBlockId() { return PREFIX + "reactor_casing"; }
        public String expectedBodyBlockId() { return "minecraft:air"; }
    }
    private static List<BlockPos> copyBody(List<BlockPos> body) {
        require(body != null && !body.isEmpty() && body.size() <= MAX_BODY_POSITIONS);
        return body.stream().map(ReactorRuntimeDescriptor::checked).toList();
    }
    private static BlockPos checked(BlockPos pos) { require(safe(pos)); return pos.immutable(); }
    private static boolean safe(BlockPos pos) { return pos != null && Math.abs((long) pos.getX()) <= 30_000_000 && Math.abs((long) pos.getY()) <= 30_000_000 && Math.abs((long) pos.getZ()) <= 30_000_000; }
    private static boolean inside(BlockPos p, BlockPos min, BlockPos max) { return p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY() && p.getZ() >= min.getZ() && p.getZ() <= max.getZ(); }
    private static void nonnegative(double n) { require(Double.isFinite(n) && n >= 0); }
    private static void unit(double n) { require(Double.isFinite(n) && n >= 0 && n <= 1); }
    private static void require(boolean condition) { if (!condition) throw new IllegalArgumentException("invalid runtime envelope"); }
    private static void typed(CompoundTag tag, String key, int type) { require(tag.contains(key, type)); }
    private static void bool(CompoundTag tag, String key) { typed(tag, key, Tag.TAG_BYTE); require(tag.getByte(key) == 0 || tag.getByte(key) == 1); }
    private static ListTag compounds(CompoundTag tag, String key, int max) { typed(tag, key, Tag.TAG_LIST); Tag raw = tag.get(key); require(raw instanceof ListTag); ListTag list = (ListTag) raw; require(list.size() <= max && (list.isEmpty() || list.getElementType() == Tag.TAG_COMPOUND)); return list; }
    private static void putPos(CompoundTag tag, String key, BlockPos p) { tag.putIntArray(key, new int[]{p.getX(), p.getY(), p.getZ()}); }
    private static BlockPos pos(CompoundTag tag, String key) { typed(tag, key, Tag.TAG_INT_ARRAY); int[] p = tag.getIntArray(key); require(p.length == 3); return new BlockPos(p[0], p[1], p[2]); }
}
