package com.iksxh.create_nuclear_industry.structure.client;

import com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor;
import com.iksxh.create_nuclear_industry.structure.ReactorSurfaceDescriptor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** 当前真实客户端世界会话的独立 L2 发布器；L1 和 CT 不参与运行租约。 */
public final class ReactorRuntimeSnapshots {
    private static final AtomicReference<Publication> PUBLISHED = new AtomicReference<>(new Publication(null, ReactorRuntimeSnapshot.empty()));
    private ReactorRuntimeSnapshots() { }
    /** 同次绘制只捕获一次；原生区段的 AT 世界字段可读，未知包装/Ponder/旧世界均为空。 */
    public static ReactorRuntimeSnapshot capture(BlockAndTintGetter context) {
        Publication publication = PUBLISHED.get();
        Object world = context != null && context.getClass() == RenderChunkRegion.class ? ((RenderChunkRegion) context).level : context;
        return world != null && world.getClass() == ClientLevel.class && world == publication.world()
                ? publication.snapshot() : ReactorRuntimeSnapshot.empty();
    }
    static void publish(ClientLevel world, ReactorRuntimeSnapshot snapshot) { PUBLISHED.set(new Publication(world, snapshot)); }
    private record Publication(ClientLevel world, ReactorRuntimeSnapshot snapshot) { }

    /**
     * 游戏线程生命周期状态机；world/token/chunk 只比较对象身份，绝不进入绘制快照。
     * 每个 owner 保留 generation/revision/sample 水位；坏包、卸载、到期均需新可靠样本恢复。
     */
    public static final class State {
        private Object session;
        private String dimension;
        private long localTick;
        private final Map<BlockPos, Owner> owners = new HashMap<>();
        private final Map<ChunkPos, Object> chunks = new HashMap<>();
        private final Map<BlockPos, Set<UUID>> retired = new HashMap<>();

        /** 世界对象切换清空全部旧水位、实例证据和租约。 */
        public void begin(Object world, String dimension) {
            session = world; this.dimension = dimension; localTick = 0;
            owners.clear(); chunks.clear(); retired.clear();
        }
        /** 本地游戏 tick 是租约唯一时钟；服务端 gameTime 仅作样本排序证据。 */
        public void tick() { localTick++; }
        /** 真实 owner 实例换代先退休旧 generation；卸载的同一 generation 仍可凭新样本恢复。 */
        public void ownerLoaded(Object world, BlockPos pos, Object token) {
            if (world != session) return;
            Owner old = owners.get(pos);
            if (old == null) owners.put(pos.immutable(), new Owner(token));
            else if (old.token != token) {
                if (!old.unloaded && old.descriptor != null) retire(pos, old.descriptor.ownerGeneration());
                old.token = token; old.active = false;
            }
        }
        /** 旧实例 remove 不得撤销新 owner；真拆除退休身份，卸载保留水位但撤下显示。 */
        public void ownerRemoved(Object world, BlockPos pos, Object token, boolean unloaded) {
            if (world != session) return;
            Owner owner = owners.get(pos);
            if (owner == null || owner.token != token) return;
            owner.active = false; owner.unloaded = unloaded;
            if (!unloaded && owner.descriptor != null) retire(pos, owner.descriptor.ownerGeneration());
        }
        /** 空信封或几何不匹配立即撤销，保持旧版本水位拒绝旧包。 */
        public void unavailable(Object world, BlockPos pos, Object token) {
            if (world != session) return;
            Owner owner = owners.get(pos);
            if (owner != null && owner.token == token) owner.active = false;
        }
        /** 区块证据必须为真实实例；卸载/换槽撤销全部相关 owner，重加载不会自动复活旧样本。 */
        public void chunk(Object world, ChunkPos pos, Object instance) {
            if (world != session) return;
            Object old = chunks.get(pos);
            if (old == instance) return;
            if (instance == null) chunks.remove(pos); else chunks.put(pos, instance);
            for (var entry : owners.entrySet()) {
                Owner owner = entry.getValue();
                if (required(owner.descriptor, entry.getKey()).contains(pos)) {
                    owner.active = false;
                    if (new ChunkPos(entry.getKey()).equals(pos)) owner.unloaded = true;
                }
            }
        }
        /**
         * 接收必须匹配当前 L1 几何和全部 chunk 实例证据；重复/旧 sample 不续 20tick 租约。
         * 同 generation 的不可用信封允许 sample 不变而 revision 上升，明确撤销仍推进水位。
         */
        public boolean receive(Object world, Object token, ReactorRuntimeDescriptor next, ReactorSurfaceDescriptor geometry) {
            if (world != session || next == null) return false;
            Owner owner = owners.get(next.ownerPos());
            if (owner == null || owner.token != token) return false;
            // 只有当前会话的当前 owner 实例有权撤销；合法类型的错维度包也不能保留旧动画或改水位。
            if (!next.dimension().equals(dimension)) { owner.active = false; return false; }
            if (retired.getOrDefault(next.ownerPos(), Set.of()).contains(next.ownerGeneration())) return false;
            if (!matchesGeometry(next, geometry)) { owner.active = false; return false; }
            ReactorRuntimeDescriptor old = owner.descriptor;
            if (old != null && old.ownerGeneration().equals(next.ownerGeneration())) {
                if (next.revision() <= old.revision() || next.sample() < old.sample()
                        || next.serverGameTime() < old.serverGameTime() || next.geometryRevision() < old.geometryRevision()
                        || (next.available() && next.sample() == old.sample())) return false;
            } else if (old != null) {
                if (!owner.unloaded && !retired.getOrDefault(next.ownerPos(), Set.of()).contains(old.ownerGeneration())) return false;
                retire(next.ownerPos(), old.ownerGeneration());
            }
            owner.descriptor = next; owner.active = false;
            if (!matchesGeometry(next, geometry) || !fullyLoaded(next)) return false;
            owner.unloaded = false;
            if (next.available()) { owner.receivedTick = localTick; owner.active = true; }
            return true;
        }
        /** 只枚举已知有限 bounds 所需区块，不遍历玩家世界。 */
        public Set<ChunkPos> requiredChunks() {
            Set<ChunkPos> result = new HashSet<>();
            owners.forEach((pos, owner) -> result.addAll(required(owner.descriptor, pos)));
            return Set.copyOf(result);
        }
        public void retainChunkEvidence(Set<ChunkPos> required) { chunks.keySet().retainAll(required); }
        /** 有效 owner 之间只要几何重叠便撤销歧义归属，不能按遍历顺序选择赢家。 */
        public ReactorRuntimeSnapshot snapshot() {
            Map<BlockPos, ReactorRuntimeDescriptor> result = new HashMap<>();
            for (var entry : owners.entrySet()) {
                Owner owner = entry.getValue();
                if (owner.active && localTick - owner.receivedTick < 20 && fullyLoaded(owner.descriptor)) result.put(entry.getKey(), owner.descriptor);
            }
            Set<BlockPos> ambiguous = new HashSet<>();
            var descriptors = result.values().stream().toList();
            for (int i = 0; i < descriptors.size(); i++) for (int j = i + 1; j < descriptors.size(); j++) {
                var a = descriptors.get(i); var b = descriptors.get(j);
                if (overlap(a, b)) { ambiguous.add(a.ownerPos()); ambiguous.add(b.ownerPos()); }
            }
            ambiguous.forEach(result::remove);
            Map<BlockPos, ReactorRuntimeDescriptor.ControlRodColumn> rods = new HashMap<>();
            for (var descriptor : result.values()) for (var column : descriptor.columns())
                if (column instanceof ReactorRuntimeDescriptor.ControlRodColumn rod) rods.put(rod.capPos(), rod);
            return new ReactorRuntimeSnapshot(result, rods);
        }
        private boolean fullyLoaded(ReactorRuntimeDescriptor d) { return d != null && chunks.keySet().containsAll(required(d, d.ownerPos())); }
        private void retire(BlockPos pos, UUID generation) { retired.computeIfAbsent(pos.immutable(), ignored -> new HashSet<>()).add(generation); }
        private static boolean matchesGeometry(ReactorRuntimeDescriptor d, ReactorSurfaceDescriptor g) {
            if (g == null || !g.valid() || !g.dimension().equals(d.dimension()) || !g.ownerPos().equals(d.ownerPos())
                    || !g.ownerGeneration().equals(d.ownerGeneration()) || g.revision() != d.geometryRevision()) return false;
            if (!d.available()) return true;
            if (!g.origin().equals(d.origin()) || !g.maxInclusive().equals(d.maxInclusive())) return false;
            Map<BlockPos, String> ids = new HashMap<>(); g.members().forEach(m -> ids.put(m.pos(), m.expectedBlockId()));
            return d.columns().stream().allMatch(c -> c.expectedCapBlockId().equals(ids.get(c.capPos())));
        }
        /** 包括外壳和内部所在区块；只读非加载查询由事件适配层负责。 */
        public static Set<ChunkPos> required(ReactorRuntimeDescriptor d, BlockPos owner) {
            Set<ChunkPos> result = new HashSet<>(); result.add(new ChunkPos(owner));
            if (d != null && d.available()) for (int x = d.origin().getX() >> 4; x <= d.maxInclusive().getX() >> 4; x++)
                for (int z = d.origin().getZ() >> 4; z <= d.maxInclusive().getZ() >> 4; z++) result.add(new ChunkPos(x, z));
            return Set.copyOf(result);
        }
        private static boolean overlap(ReactorRuntimeDescriptor a, ReactorRuntimeDescriptor b) {
            return a.origin().getX() <= b.maxInclusive().getX() && b.origin().getX() <= a.maxInclusive().getX()
                    && a.origin().getY() <= b.maxInclusive().getY() && b.origin().getY() <= a.maxInclusive().getY()
                    && a.origin().getZ() <= b.maxInclusive().getZ() && b.origin().getZ() <= a.maxInclusive().getZ();
        }
        private static final class Owner {
            private Object token; private ReactorRuntimeDescriptor descriptor;
            private boolean active; private boolean unloaded; private long receivedTick;
            private Owner(Object token) { this.token = token; }
        }
    }
}
