package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 从侧控制器定位三档水平八棱机组。局部 +x 为机器面向的顺时针侧，+y 向上，
 * +z 从前轴到后轴；读取每格前先确认区块可 tick，核验绝不主动加载区块。
 */
public final class TurbineStructure {
    private static final Map<Level, Map<BlockPos, OwnerEntry>> OWNERS =
            new WeakHashMap<>();
    private static final Map<Level, MemberIndex<OwnerEntry>> MEMBERS = new WeakHashMap<>();
    private static final int[] AIR_3 = buildAirSection(3);
    private static final int[] AIR_5 = buildAirSection(5);
    private static final int[] AIR_7 = buildAirSection(7);

    /** 只索引成型快照的几何，不持有世界或控制器强引用，也不缓存实时运行资格。 */
    private static final class OwnerEntry {
        private final WeakReference<TurbineControllerBlockEntity> owner;
        private Form form;
        private PartBounds bounds;
        private Form indexedForm;
        private ContentCheck contentCheck;

        private OwnerEntry(TurbineControllerBlockEntity owner) {
            this.owner = new WeakReference<>(owner);
            current(owner);
        }

        private Form current(TurbineControllerBlockEntity owner) {
            Form current = owner.currentForm();
            if (form != current) {
                form = current;
                // scan 产生的成员列表不可变；重新成型即更换 Form 身份并重建索引。
                bounds = current == null ? null : PartBounds.of(current.parts());
                contentCheck = null;
            }
            return current;
        }
    }

    /**
     * 只索引已公布快照的实际成员坐标；long键不会持有可变BlockPos或世界。
     * 所有者按对象身份撤销。异常重叠单独标记，查询回退原机组顺序，不能静默覆盖归属。
     */
    private static final class MemberIndex<T> {
        private final Long2ObjectOpenHashMap<T> members = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<Set<T>> conflicts = new Long2ObjectOpenHashMap<>();
        private final Map<T, long[]> positions = new IdentityHashMap<>();

        private void put(T owner, List<BlockPos> parts) {
            remove(owner);
            LongOpenHashSet keys = new LongOpenHashSet(parts.size());
            for (BlockPos part : parts) keys.add(part.asLong());
            long[] saved = keys.toLongArray();
            positions.put(owner, saved);
            for (long key : saved) {
                T previous = members.putIfAbsent(key, owner);
                if (previous == null || previous == owner) continue;
                Set<T> owners = conflicts.computeIfAbsent(key,
                        ignored -> Collections.newSetFromMap(new IdentityHashMap<>()));
                owners.add(previous);
                owners.add(owner);
            }
        }

        private T get(BlockPos part) {
            long key = part.asLong();
            return conflicts.containsKey(key) ? null : members.get(key);
        }

        private boolean conflicted(BlockPos part) { return conflicts.containsKey(part.asLong()); }

        private void remove(T owner) {
            long[] keys = positions.remove(owner);
            if (keys == null) return;
            for (long key : keys) {
                Set<T> owners = conflicts.get(key);
                if (owners == null) {
                    if (members.get(key) == owner) members.remove(key);
                    continue;
                }
                owners.remove(owner);
                if (owners.isEmpty()) members.remove(key);
                else members.put(key, owners.iterator().next());
                if (owners.size() < 2) conflicts.remove(key);
            }
        }
    }

    /** 按真实成员计算闭区间包围盒；空集合不能与任何机组冲突。 */
    private record PartBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private static PartBounds of(List<BlockPos> parts) {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos part : parts) {
                minX = Math.min(minX, part.getX()); maxX = Math.max(maxX, part.getX());
                minY = Math.min(minY, part.getY()); maxY = Math.max(maxY, part.getY());
                minZ = Math.min(minZ, part.getZ()); maxZ = Math.max(maxZ, part.getZ());
            }
            return new PartBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }

        private boolean intersects(PartBounds other) {
            return minX <= maxX && other.minX <= other.maxX
                    && minX <= other.maxX && maxX >= other.minX
                    && minY <= other.maxY && maxY >= other.minY
                    && minZ <= other.maxZ && maxZ >= other.minZ;
        }

        private boolean contains(BlockPos part) {
            return part.getX() >= minX && part.getX() <= maxX
                    && part.getY() >= minY && part.getY() <= maxY
                    && part.getZ() >= minZ && part.getZ() <= maxZ;
        }
    }

    public record Form(Direction facing, int rotors, int diameter, BlockPos front, BlockPos rear,
                       BlockPos controller, List<BlockPos> parts, List<BlockPos> inlets,
                       List<BlockPos> exhausts, List<BlockPos> chunkSamples) {
        public int length() { return rotors + 2; }
        public boolean contains(BlockPos pos) { return parts.contains(pos); }
    }
    public record Issue(String reason, BlockPos pos) {}
    private record Result(Form form, Issue issue) {}
    private TurbineStructure() {}

    public static BlockPos at(BlockPos front, Direction facing, int x, int y, int z) {
        return front.relative(facing.getClockWise(), x).above(y).relative(facing.getOpposite(), z);
    }

    public static boolean ticking(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) return false;
        var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)
                && server.shouldTickBlocksAt(pos) && server.areEntitiesLoaded(ChunkPos.asLong(pos))
                && server.getWorldBorder().isWithinBounds(pos);
    }

    public static Form inspect(Level level, BlockPos controller, TurbineState.Settings settings) {
        return diagnose(level, controller, settings).form();
    }

    public static Issue issue(Level level, BlockPos controller, TurbineState.Settings settings) {
        return diagnose(level, controller, settings).issue();
    }

    private static Result diagnose(Level level, BlockPos controller, TurbineState.Settings settings) {
        if (!ticking(level, controller)) return new Result(null, new Issue("chunk", controller));
        BlockState state = level.getBlockState(controller);
        if (!state.is(TurbineContent.CONTROLLER.get()))
            return new Result(null, new Issue("controller", controller));
        Direction facing = state.getValue(TurbinePartBlock.MACHINE_FACING);
        TurbinePartBlock.Side side = state.getValue(TurbinePartBlock.SIDE);
        TurbineAssembly.Layout located = TurbineAssembly.at(level, controller);
        if (located != null) {
            TurbineState.Tier tier = settings.tierForRotors(located.length() - 2);
            if (tier != null && tier.diameter() == located.diameter())
                return scan(level, controller, located.front(), located.facing(), tier);
        }
        if (state.getValue(TurbinePartBlock.LOCATED))
            return new Result(null, new Issue("unlocated_axis", controller));
        Issue first = null;
        for (TurbineState.Tier tier : new TurbineState.Tier[]{settings.shortTier(),
                settings.mediumTier(), settings.longTier()}) {
            if (tier == null || !tier.valid()) continue;
            int length = tier.length(), radius = (tier.diameter() - 1) / 2;
            int x = side.x(radius), y = side.y(radius);
            for (int z : middleRows(length)) {
                BlockPos front = controller.relative(facing.getClockWise(), -x)
                        .below(y).relative(facing, z);
                Result candidate = scan(level, controller, front, facing, tier);
                if (candidate.form() != null) return candidate;
                if (first == null || "length".equals(first.reason())) first = candidate.issue();
            }
        }
        return new Result(null, first == null ? new Issue("length", controller) : first);
    }

    public static int[] middleRows(int length) {
        return length % 2 == 0 ? new int[]{length / 2 - 1, length / 2}
                : new int[]{length / 2};
    }

    private static Result scan(Level level, BlockPos controller, BlockPos front, Direction facing,
                               TurbineState.Tier tier) {
        int length = tier.length(), diameter = tier.diameter();
        int radius = (diameter - 1) / 2;
        List<BlockPos> parts = new ArrayList<>(length * diameter * diameter);
        List<BlockPos> inlets = new ArrayList<>(), exhausts = new ArrayList<>();
        List<BlockPos> chunkSamples = new ArrayList<>();
        Set<Long> seenChunks = new HashSet<>();
        int controllers = 0;
        for (int z = 0; z < length; z++) for (int y = -radius; y <= radius; y++)
            for (int x = -radius; x <= radius; x++) {
                if (!TurbineGeometry.footprint(diameter, x, y)) continue;
                BlockPos pos = at(front, facing, x, y, z);
                if (!ticking(level, pos)) return new Result(null, new Issue("chunk", pos));
                if (seenChunks.add(ChunkPos.asLong(pos))) chunkSamples.add(pos.immutable());
                BlockState state = level.getBlockState(pos);
                if (z == 0 || z == length - 1) {
                    if (x == 0 && y == 0) {
                        if (!state.is(TurbineContent.OUTPUT_SHAFT.get()))
                            return new Result(null, new Issue("axis", pos));
                    } else if (!state.is(TurbineContent.CASING.get()))
                        return new Result(null, new Issue("end", pos));
                } else if (x == 0 && y == 0) {
                    if (!state.is(TurbineContent.ROTOR.get()))
                        return new Result(null, new Issue("axis", pos));
                } else if (TurbineGeometry.airSlot(diameter, x, y)) {
                    if (!state.isAir()) return new Result(null, new Issue("cavity", pos));
                    continue;
                } else if (state.is(TurbineContent.CONTROLLER.get())) {
                    if (!middleRow(length, z) || !TurbineGeometry.sideSlot(diameter, x, y)
                            || !pos.equals(controller)) return new Result(null, new Issue("controller", pos));
                    controllers++;
                } else if (state.is(TurbineContent.INLET.get()) || state.is(TurbineContent.EXHAUST.get())) {
                    if (!TurbineGeometry.sideSlot(diameter, x, y))
                        return new Result(null, new Issue("port", pos));
                    if (state.is(TurbineContent.INLET.get())) {
                        if (!middleRow(length, z)) return new Result(null, new Issue("inlet_position", pos));
                        inlets.add(pos.immutable());
                    } else {
                        if (z != 1 && z != length - 2)
                            return new Result(null, new Issue("exhaust_position", pos));
                        exhausts.add(pos.immutable());
                    }
                } else if (state.is(TurbineContent.WINDOW.get())) {
                    if (!TurbineGeometry.windowSlot(diameter, x, y))
                        return new Result(null, new Issue("window", pos));
                } else if (!state.is(TurbineContent.CASING.get()))
                    return new Result(null, new Issue("ring", pos));
                parts.add(pos.immutable());
            }
        if (controllers != 1) return new Result(null, new Issue("controller", controller));
        if (inlets.isEmpty()) return new Result(null, new Issue("inlet",
                missingPortTarget(level, front, facing, tier, true)));
        if (exhausts.isEmpty()) return new Result(null, new Issue("exhaust",
                missingPortTarget(level, front, facing, tier, false)));
        BlockPos rear = at(front, facing, 0, 0, length - 1);
        return new Result(new Form(facing, tier.rotorCount(), diameter, front.immutable(),
                rear.immutable(), controller.immutable(), List.copyOf(parts), List.copyOf(inlets),
                List.copyOf(exhausts), List.copyOf(chunkSamples)), null);
    }

    /**
     * 已成型运行路径只检查少量实际区块票据、轴/口身份与内腔空气，不重试三档全体积扫描。
     * 仅原内容谓词的成功结果可复用；底层方块写入、区块/分节身份或Form变化立即使见证失效。
     * 区块票据与世界边界每次仍按原路径检查，红石和实际Port交易许可不在此缓存。
     */
    public static boolean quickLive(Level level, Form form) {
        OwnerEntry entry = contentEntry(level, form);
        for (BlockPos sample : form.chunkSamples()) if (!ticking(level, sample)) {
            if (entry != null) entry.contentCheck = null;
            return false;
        }
        if (entry == null) return contentLive(level, form);
        ServerLevel server = (ServerLevel) level;
        if (entry.contentCheck != null && entry.contentCheck.matches(server)) return true;
        entry.contentCheck = null;
        ContentCheck before = ContentCheck.capture(server, form);
        boolean result = contentLive(level, form);
        // 扫描期间发生写入或分节替换时不公布缓存；负结果也不缓存，同tick恢复仍实时生效。
        if (result && before != null && before.matches(server)) entry.contentCheck = before;
        return result;
    }

    private static OwnerEntry contentEntry(Level level, Form form) {
        if (!(level instanceof ServerLevel server) || !server.getServer().isSameThread()) return null;
        Map<BlockPos, OwnerEntry> entries = OWNERS.get(level);
        OwnerEntry entry = entries == null ? null : entries.get(form.controller());
        TurbineControllerBlockEntity owner = entry == null ? null : entry.owner.get();
        return owner != null && entry.current(owner) == form ? entry : null;
    }

    /** 原轴/口身份及内腔空气谓词，保持读取顺序和判定；此方法本身始终完整读取。 */
    private static boolean contentLive(Level level, Form form) {
        BlockState front = level.getBlockState(form.front());
        BlockState rear = level.getBlockState(form.rear());
        if (!level.getBlockState(form.controller()).is(TurbineContent.CONTROLLER.get())
                || !front.is(TurbineContent.OUTPUT_SHAFT.get())
                || !rear.is(TurbineContent.OUTPUT_SHAFT.get())
                || front.getValue(TurbinePartBlock.MACHINE_FACING) != form.facing()
                || rear.getValue(TurbinePartBlock.MACHINE_FACING) != form.facing()
                || front.getValue(TurbineShaftBlock.END) != TurbineShaftBlock.End.FRONT
                || rear.getValue(TurbineShaftBlock.END) != TurbineShaftBlock.End.REAR) return false;
        for (BlockPos inlet : form.inlets()) if (!level.getBlockState(inlet).is(TurbineContent.INLET.get()))
            return false;
        for (BlockPos exhaust : form.exhausts()) if (!level.getBlockState(exhaust).is(TurbineContent.EXHAUST.get()))
            return false;
        int[] section = airSection(form.diameter());
        Direction side = form.facing().getClockWise(), back = form.facing().getOpposite();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = 1; z < form.length() - 1; z++)
            for (int i = 0; i < section.length; i += 2) {
                setAirPosition(pos, form.front(), side, back, section[i], section[i + 1], z);
                if (!level.getBlockState(pos).isAir()) return false;
            }
        return true;
    }

    /** 仅缓存静态横截面，保持原 y/x 顺序；内容缓存失效时完整读取世界空气。 */
    private static int[] airSection(int diameter) {
        return switch (diameter) {
            case 3 -> AIR_3;
            case 5 -> AIR_5;
            case 7 -> AIR_7;
            default -> buildAirSection(diameter);
        };
    }

    private static int[] buildAirSection(int diameter) {
        int radius = (diameter - 1) / 2;
        List<Integer> offsets = new ArrayList<>();
        for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++)
            if (TurbineGeometry.airSlot(diameter, x, y)) {
                offsets.add(x); offsets.add(y);
            }
        int[] result = new int[offsets.size()];
        for (int i = 0; i < result.length; i++) result[i] = offsets.get(i);
        return result;
    }

    /** 与 at 使用同一局部轴变换，只复用本次检查的游标，不把可变坐标保存进索引。 */
    private static void setAirPosition(BlockPos.MutableBlockPos pos, BlockPos front,
                                       Direction side, Direction back, int x, int y, int z) {
        pos.set(front.getX() + side.getStepX() * x + back.getStepX() * z,
                front.getY() + y, front.getZ() + side.getStepZ() * x + back.getStepZ() * z);
    }

    /** 只在静止版本上创建见证；全量扫描后和每次复用前都要再次核对。 */
    private static final class RevisionSnapshot {
        private final List<TurbineBlockStateRevision> sources;
        private final long[] revisions;
        private RevisionSnapshot(List<TurbineBlockStateRevision> sources) {
            this.sources = List.copyOf(sources);
            revisions = new long[sources.size()];
        }
        private static RevisionSnapshot capture(List<TurbineBlockStateRevision> sources) {
            if (sources.isEmpty()) return null;
            RevisionSnapshot snapshot = new RevisionSnapshot(sources);
            for (int i = 0; i < sources.size(); i++) {
                TurbineBlockStateRevision source = sources.get(i);
                if (source.turbineWriting()) return null;
                snapshot.revisions[i] = source.turbineRevision();
            }
            return snapshot.matches() ? snapshot : null;
        }
        private boolean matches() {
            for (int i = 0; i < sources.size(); i++) {
                TurbineBlockStateRevision source = sources.get(i);
                if (source.turbineWriting() || source.turbineRevision() != revisions[i]
                        || source.turbineWriting()) return false;
            }
            return true;
        }
    }

    /** chunk使用弱引用，避免WeakHashMap的值经世界区块反向强持有世界。 */
    private record SectionWatch(int chunkX, int chunkZ, int index,
            WeakReference<net.minecraft.world.level.chunk.LevelChunk> chunk,
            net.minecraft.world.level.chunk.LevelChunkSection section,
            Object palette) {
        private boolean matches(ServerLevel level) {
            var current = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            if (current == null || current != chunk.get()) return false;
            var sections = current.getSections();
            return index >= 0 && index < sections.length && sections[index] == section
                    && section.getStates() == palette;
        }
    }

    /** 见证整个机组包围区间的实际分节；多观察少量角落只增加失效，不会减少原判定。 */
    private record ContentCheck(List<SectionWatch> sections, RevisionSnapshot revisions) {
        private static ContentCheck capture(ServerLevel level, Form form) {
            List<SectionWatch> sections = new ArrayList<>();
            List<TurbineBlockStateRevision> sources = new ArrayList<>();
            int radius = (form.diameter() - 1) / 2;
            int first = level.getSectionIndex(form.front().getY() - radius);
            int last = level.getSectionIndex(form.front().getY() + radius);
            for (BlockPos sample : form.chunkSamples()) {
                int x = sample.getX() >> 4, z = sample.getZ() >> 4;
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) return null;
                var values = chunk.getSections();
                for (int index = first; index <= last; index++) {
                    if (index < 0 || index >= values.length || values[index] == null) return null;
                    var section = values[index];
                    Object palette = section.getStates();
                    if (!(palette instanceof TurbineBlockStateRevision source)) return null;
                    sections.add(new SectionWatch(x, z, index, new WeakReference<>(chunk), section, palette));
                    sources.add(source);
                }
            }
            RevisionSnapshot revisions = RevisionSnapshot.capture(sources);
            return revisions == null ? null : new ContentCheck(List.copyOf(sections), revisions);
        }
        private boolean matches(ServerLevel level) {
            for (SectionWatch section : sections) if (!section.matches(level)) return false;
            return revisions.matches();
        }
    }

    private static boolean middleRow(int length, int z) {
        return z == length / 2 || length % 2 == 0 && z == length / 2 - 1;
    }

    /** 缺少接口时标记可替换的合法侧槽，不把控制器或已有另一类接口误报为落点。 */
    private static BlockPos missingPortTarget(Level level, BlockPos front, Direction facing,
                                              TurbineState.Tier tier, boolean input) {
        int radius = (tier.diameter() - 1) / 2;
        int[] rows = input ? middleRows(tier.length()) : new int[]{1, tier.length() - 2};
        for (int z : rows) for (int[] side : new int[][]{{0, radius}, {radius, 0},
                {0, -radius}, {-radius, 0}}) {
            BlockPos pos = at(front, facing, side[0], side[1], z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(TurbineContent.CASING.get())
                    || state.is(TurbineContent.WINDOW.get())) return pos;
        }
        return front;
    }

    public static Direction outward(Direction facing, int x, int y) {
        return y > 0 ? Direction.UP : y < 0 ? Direction.DOWN
                : x < 0 ? facing.getCounterClockWise() : facing.getClockWise();
    }

    /** 先排除几何不相交机组，再对可能重叠者按原路径检查实时资格与实际成员。 */
    public static boolean unique(Level level, TurbineControllerBlockEntity proposed, Form form) {
        Map<BlockPos, OwnerEntry> entries = OWNERS.get(level);
        if (entries == null) return true;
        Set<BlockPos> parts = null;
        OwnerEntry own = entries.get(proposed.getBlockPos());
        PartBounds bounds = own != null && own.owner.get() == proposed && own.current(proposed) == form
                ? own.bounds : PartBounds.of(form.parts());
        entries.entrySet().removeIf(entry -> discardCollected(level, entry.getValue()));
        for (OwnerEntry entry : entries.values()) {
            TurbineControllerBlockEntity other = entry.owner.get();
            if (other == null || other == proposed) continue;
            if (entry.current(other) == null || !bounds.intersects(entry.bounds)) continue;
            Form claimed = other.claimedForm();
            if (claimed == null) continue;
            if (parts == null) parts = new HashSet<>(form.parts());
            for (BlockPos part : claimed.parts()) if (parts.contains(part)) return false;
        }
        return true;
    }

    public static void claim(Level level, TurbineControllerBlockEntity owner) {
        Map<BlockPos, OwnerEntry> entries = OWNERS.computeIfAbsent(level, ignored -> new HashMap<>());
        OwnerEntry entry = entries.get(owner.getBlockPos());
        if (entry == null || entry.owner.get() != owner) {
            if (entry != null) unindex(level, entry);
            entry = new OwnerEntry(owner);
            entries.put(owner.getBlockPos().immutable(), entry);
        }
        updateIndex(level, entry, owner);
    }

    public static void forget(Level level, TurbineControllerBlockEntity owner) {
        Map<BlockPos, OwnerEntry> entries = OWNERS.get(level);
        if (entries != null) entries.entrySet().removeIf(entry -> {
            TurbineControllerBlockEntity current = entry.getValue().owner.get();
            if (current != null && current != owner) return false;
            unindex(level, entry.getValue());
            return true;
        });
    }

    /** 同一Form只建一次索引；实时区块、红石和内腔资格仍由原调用者逐次检查。 */
    private static void updateIndex(Level level, OwnerEntry entry, TurbineControllerBlockEntity owner) {
        Form form = entry.current(owner);
        if (form == entry.indexedForm) return;
        unindex(level, entry);
        if (form != null) {
            MEMBERS.computeIfAbsent(level, ignored -> new MemberIndex<>()).put(entry, form.parts());
            entry.indexedForm = form;
        }
    }

    private static void unindex(Level level, OwnerEntry entry) {
        MemberIndex<OwnerEntry> index = MEMBERS.get(level);
        if (index != null) index.remove(entry);
        entry.indexedForm = null;
    }

    private static boolean discardCollected(Level level, OwnerEntry entry) {
        if (entry.owner.get() != null) return false;
        unindex(level, entry);
        return true;
    }

    /** 按世界和实际成员坐标定位；不加载区块，不将几何命中当作实时运行许可。 */
    public static TurbineControllerBlockEntity ownerForPart(Level level, BlockPos part) {
        if (!ticking(level, part)) return null;
        MemberIndex<OwnerEntry> index = MEMBERS.get(level);
        if (index == null) return null;
        if (index.conflicted(part)) return overlappingOwner(level, part);
        OwnerEntry entry = index.get(part);
        if (entry == null) return null;
        TurbineControllerBlockEntity owner = entry.owner.get();
        if (owner == null) {
            unindex(level, entry);
            return null;
        }
        if (owner.currentForm() != entry.indexedForm) {
            updateIndex(level, entry, owner);
            return ownerForPart(level, part);
        }
        return entry.indexedForm == null ? null : owner;
    }

    /** 仅异常重叠走旧遍历语义，普通非重叠机组查询不遍历所有者或成员。 */
    private static TurbineControllerBlockEntity overlappingOwner(Level level, BlockPos part) {
        Map<BlockPos, OwnerEntry> entries = OWNERS.get(level);
        if (entries == null) return null;
        for (OwnerEntry entry : entries.values()) {
            TurbineControllerBlockEntity owner = entry.owner.get();
            if (owner != null && entry.current(owner) != null && entry.bounds.contains(part)
                    && entry.form.contains(part))
                return owner;
        }
        return null;
    }

    /** 构件或空腔变化通知近邻已加载控制器；上限使用配置允许的最大 18 格轴长。 */
    public static void invalidateNearby(Level level, BlockPos changed) {
        if (level.isClientSide) return;
        Map<BlockPos, OwnerEntry> entries = OWNERS.get(level);
        if (entries != null) for (OwnerEntry entry :
                List.copyOf(entries.values())) {
            TurbineControllerBlockEntity owner = entry.owner.get();
            if (owner != null && owner.currentForm() != null
                    && withinBox(owner.currentForm(), changed)) owner.invalidateForm();
        }
        if (level.hasChunkAt(changed)) level.invalidateCapabilities(changed);
    }

    private static boolean withinBox(Form form, BlockPos pos) {
        BlockPos front = form.front();
        Direction facing = form.facing();
        int x = pos.getX() - front.getX(), y = pos.getY() - front.getY(), z = pos.getZ() - front.getZ();
        int localX = x * facing.getClockWise().getStepX() + z * facing.getClockWise().getStepZ();
        int localZ = x * facing.getOpposite().getStepX() + z * facing.getOpposite().getStepZ();
        int radius = (form.diameter() - 1) / 2;
        return Math.abs(localX) <= radius && Math.abs(y) <= radius
                && localZ >= 0 && localZ < form.length();
    }

    public static TurbineControllerBlockEntity ownerForPort(Level level, BlockPos port, BlockState state,
                                                              boolean input) {
        if (!state.getValue(TurbinePartBlock.FORMED)) return null;
        TurbineControllerBlockEntity owner = ownerForPart(level, port);
        return owner != null && owner.validPort(port, input) ? owner : null;
    }
}
