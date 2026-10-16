package com.iksxh.create_nuclear_industry.boiler.client;

import com.iksxh.create_nuclear_industry.boiler.BoilerSurfaceDescriptor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 游戏线程维护归属并发布只读快照；原子信封把真实世界身份与快照绑定，避免换世界时撕裂读取。
 * 模型一次构建必须调用带上下文 capture 一次，并复用返回值；无参入口仅用于游戏线程诊断。
 */
public final class BoilerSurfaceSnapshots {
    private static final AtomicReference<Publication> PUBLISHED = new AtomicReference<>(new Publication(null, BoilerSurfaceSnapshot.empty()));
    /**
     * Sodium 0.8.12 的确切 final 切片通过 final 字段绑定构建世界；只缓存字段访问，不保留世界。
     * 不加载可选模组类，字段形状不符或不可访问时继续返回空快照。
     */
    private static final ClassValue<Optional<Field>> SODIUM_WORLD_FIELDS = new ClassValue<>() {
        @Override
        protected Optional<Field> computeValue(Class<?> type) {
            if (!type.getName().equals("net.caffeinemc.mods.sodium.client.world.LevelSlice")
                    || !Modifier.isFinal(type.getModifiers())) return Optional.empty();
            try {
                Field field = type.getDeclaredField("level");
                if (field.getType() != ClientLevel.class || !Modifier.isFinal(field.getModifiers())
                        || Modifier.isStatic(field.getModifiers()) || !field.trySetAccessible()) return Optional.empty();
                return Optional.of(field);
            } catch (ReflectiveOperationException | SecurityException exception) {
                return Optional.empty();
            }
        }
    };
    private BoilerSurfaceSnapshots() { }

    public static BoilerSurfaceSnapshot capture() { return PUBLISHED.get().snapshot(); }

    /**
     * 只接受真实 ClientLevel、原生 RenderChunkRegion 或已核 Sodium 切片绑定的同一 Level 对象。
     * 未知包装、Ponder 以及旧世界延迟构建一律返回空；不查询 BE、区块、光照或世界方块。
     */
    public static BoilerSurfaceSnapshot capture(BlockAndTintGetter context) {
        Publication publication = PUBLISHED.get();
        Object world = context != null && context.getClass() == RenderChunkRegion.class
                ? ((RenderChunkRegion) context).level : context;
        if (!(world instanceof ClientLevel) && context != null) world = sodiumWorld(context);
        return world instanceof ClientLevel && world == publication.world()
                ? publication.snapshot() : BoilerSurfaceSnapshot.empty();
    }

    /** 模型线程仅读切片 final 世界身份，不读方块/BE/光照，也不借用当前全局世界。 */
    private static ClientLevel sodiumWorld(BlockAndTintGetter context) {
        Optional<Field> field = SODIUM_WORLD_FIELDS.get(context.getClass());
        if (field.isEmpty()) return null;
        try {
            return (ClientLevel) field.get().get(context);
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    static void publish(ClientLevel world, BoilerSurfaceSnapshot snapshot) {
        PUBLISHED.set(new Publication(world, snapshot));
    }

    private record Publication(ClientLevel world, BoilerSurfaceSnapshot snapshot) { }

    /** 有限刷新边界，坐标来自已经验证的描述；客户端渲染层再外扩一格并跳过未加载区块。 */
    public record Bounds(BlockPos min, BlockPos maxInclusive) {
        public Bounds {
            min = min.immutable();
            maxInclusive = maxInclusive.immutable();
        }
    }

    /**
     * 无世界类型的游戏线程状态机，session/ownerToken 只作对象身份比较，不进入模型快照。
     * 可靠 owner 与全部成员区块加载后才能索引；同实例拒绝旧 revision 或变 generation，重放实例另核身份。
     */
    public static final class State {
        private Object session;
        private String dimension;
        private final Map<BlockPos, Owner> owners = new HashMap<>();
        private final Map<BlockPos, Set<UUID>> retired = new HashMap<>();
        private final Map<BlockPos, BoilerSurfaceDescriptor> suspended = new HashMap<>();
        private final Set<Long> loadedChunks = new HashSet<>();
        private BoilerSurfaceSnapshot snapshot = BoilerSurfaceSnapshot.empty();
        private boolean dirty;

        /** 新会话撤销全部旧身份与加载证据；旧 session 后续投递只能被拒绝。 */
        public void begin(Object nextSession, String nextDimension) {
            session = nextSession;
            dimension = nextDimension;
            owners.clear();
            retired.clear();
            suspended.clear();
            loadedChunks.clear();
            snapshot = BoilerSurfaceSnapshot.empty();
            dirty = false;
        }

        public BoilerSurfaceSnapshot snapshot() { return snapshot; }

        /** 仅导出已知 owner 的实例 token，生命周期层在对应区块 BE 表中核实槽位，不遍历世界。 */
        public Map<BlockPos, Object> ownerTokens() {
            Map<BlockPos, Object> tokens = new HashMap<>();
            owners.forEach((pos, owner) -> tokens.put(pos, owner.token));
            return Map.copyOf(tokens);
        }

        /** 只有生命周期层证实当前 BE 实例，才允许后续描述绑定该 ownerToken。 */
        public void ownerLoaded(Object world, BlockPos pos, Object ownerToken) {
            if (world != session) return;
            Owner previous = owners.get(pos);
            if (previous != null && previous.token == ownerToken) return;
            if (previous != null) retire(pos, previous.descriptor);
            owners.put(pos.immutable(), new Owner(ownerToken));
            dirty = true;
        }

        /** 当前实例失效或卸载时撤销；旧实例的延迟 remove 不能移除新实例。 */
        public void ownerRemoved(Object world, BlockPos pos, Object ownerToken) {
            if (world != session) return;
            Owner current = owners.get(pos);
            if (current != null && current.token == ownerToken) {
                retire(pos, current.descriptor);
                owners.remove(pos);
                dirty = true;
            }
        }

        /** 显式区块卸载只暂停当前实例，不退休 generation；无论是否先收到 ChunkEvent 都安全。 */
        public void ownerUnloaded(Object world, BlockPos pos, Object ownerToken) {
            if (world != session) return;
            Owner owner = owners.get(pos);
            if (owner != null && owner.token == ownerToken) {
                if (owner.descriptor != null) suspended.put(pos.immutable(), owner.descriptor);
                owners.remove(pos);
                dirty = true;
            }
        }

        /** 仅列举已知描述所需区块，用于补偿原生缓存换槽没有 ChunkEvent.Unload 的路径。 */
        public Set<ChunkPos> requiredChunks() {
            Set<ChunkPos> chunks = new HashSet<>();
            for (var entry : owners.entrySet()) {
                chunks.add(new ChunkPos(entry.getKey()));
                BoilerSurfaceDescriptor descriptor = entry.getValue().descriptor;
                addChunks(chunks, descriptor);
            }
            suspended.forEach((pos, descriptor) -> { chunks.add(new ChunkPos(pos)); addChunks(chunks, descriptor); });
            return Set.copyOf(chunks);
        }

        private void addChunks(Set<ChunkPos> chunks, BoilerSurfaceDescriptor descriptor) {
            if (descriptor == null || !descriptor.valid()) return;
            for (int x = descriptor.min().getX() >> 4; x <= descriptor.maxInclusive().getX() >> 4; x++)
                for (int z = descriptor.min().getZ() >> 4; z <= descriptor.maxInclusive().getZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
        }

        /** 暂停 owner 的位置供可靠新槽位恢复；不把 suspended 描述直接重新发布。 */
        public Set<BlockPos> suspendedOwners() { return Set.copyOf(suspended.keySet()); }

        /** 加载证据只保留当前已知 owner/成员需要的去重区块，避免沿玩家行程无限累计。 */
        public void retainChunkEvidence(Set<ChunkPos> required) {
            Set<Long> keys = new HashSet<>();
            for (ChunkPos pos : required) keys.add(pos.toLong());
            loadedChunks.retainAll(keys);
        }

        /** 接受完整合法信封；坏包通过 unavailable 方法撤销，不能借坏坐标扩大索引。 */
        private boolean accept(Object world, Object ownerToken, BoilerSurfaceDescriptor descriptor) {
            if (world != session || !descriptor.dimension().equals(dimension)) return false;
            Owner owner = owners.get(descriptor.ownerPos());
            if (owner == null || owner.token != ownerToken
                    || retired.getOrDefault(descriptor.ownerPos(), Set.of()).contains(descriptor.ownerGeneration())) return false;
            BoilerSurfaceDescriptor old = owner.descriptor;
            if (old != null && (!old.ownerGeneration().equals(descriptor.ownerGeneration())
                    || descriptor.revision() < old.revision()
                    || (descriptor.revision() == old.revision() && !descriptor.equals(old)))) return false;
            BoilerSurfaceDescriptor unloaded = suspended.get(descriptor.ownerPos());
            if (unloaded != null) {
                if (unloaded.ownerGeneration().equals(descriptor.ownerGeneration())) {
                    if (descriptor.revision() < unloaded.revision()
                            || (descriptor.revision() == unloaded.revision() && !descriptor.equals(unloaded))) return false;
                } else {
                    retire(descriptor.ownerPos(), unloaded);
                }
                suspended.remove(descriptor.ownerPos());
            }
            owner.descriptor = descriptor;
            if (!descriptor.equals(old)) dirty = true;
            return true;
        }

        /** 未知 schema 或坏包使当前显示不可用；保留版本水位以拒绝之后的旧包。 */
        public void unavailable(Object world, BlockPos pos, Object ownerToken) {
            if (world != session) return;
            Owner owner = owners.get(pos);
            if (owner != null && owner.token == ownerToken && !owner.unavailable) {
                owner.unavailable = true;
                dirty = true;
            }
        }

        /** 新的合法非过期包可以结束坏包降级；拒绝结果不能改变原状态。 */
        public boolean receive(Object world, Object ownerToken, BoilerSurfaceDescriptor descriptor) {
            if (!accept(world, ownerToken, descriptor)) return false;
            Owner owner = owners.get(descriptor.ownerPos());
            if (owner.unavailable) dirty = true;
            owner.unavailable = false;
            return true;
        }

        /** 区块事件建立/撤销可靠加载证据；卸载成员只暂停索引，保留已加载 owner 描述供恢复。 */
        public void chunk(Object world, ChunkPos pos, boolean loaded) {
            if (world != session) return;
            boolean changed = loaded ? loadedChunks.add(pos.toLong()) : loadedChunks.remove(pos.toLong());
            if (changed) dirty = true;
            if (!loaded) {
                for (BlockPos ownerPos : Set.copyOf(owners.keySet())) {
                    if (new ChunkPos(ownerPos).equals(pos)) {
                        ownerUnloaded(world, ownerPos, owners.get(ownerPos).token);
                    }
                }
            }
        }

        /** 重算有限索引并返回真正变化成员所属旧/新 bounds；调用者必须先发布再刷新。 */
        public Set<Bounds> rebuild() {
            if (!dirty) return Set.of();
            dirty = false;
            Map<BlockPos, BoilerSurfaceSnapshot.Member> members = new HashMap<>();
            Set<BlockPos> ambiguous = new HashSet<>();
            for (Owner owner : owners.values()) {
                BoilerSurfaceDescriptor descriptor = owner.descriptor;
                if (descriptor == null || owner.unavailable || !descriptor.valid() || !fullyLoaded(descriptor)) continue;
                for (var member : descriptor.members()) {
                    var projected = new BoilerSurfaceSnapshot.Member(descriptor.dimension(), descriptor.ownerPos(),
                            descriptor.ownerGeneration(), descriptor.revision(), descriptor.min(), descriptor.maxInclusive(),
                            descriptor.partitionY(), member.expectedBlockId(), member.pos(), member.outwardFaces());
                    if (members.putIfAbsent(member.pos(), projected) != null) ambiguous.add(member.pos());
                }
            }
            ambiguous.forEach(members::remove);
            Set<Bounds> changed = new HashSet<>();
            Set<BlockPos> positions = new HashSet<>(snapshot.members().keySet());
            positions.addAll(members.keySet());
            for (BlockPos pos : positions) {
                var old = snapshot.members().get(pos);
                var next = members.get(pos);
                if (java.util.Objects.equals(old, next)) continue;
                if (old != null) changed.add(new Bounds(old.min(), old.maxInclusive()));
                if (next != null) changed.add(new Bounds(next.min(), next.maxInclusive()));
            }
            if (!changed.isEmpty()) snapshot = new BoilerSurfaceSnapshot(members);
            return Set.copyOf(changed);
        }

        private boolean fullyLoaded(BoilerSurfaceDescriptor descriptor) {
            if (!loadedChunks.contains(new ChunkPos(descriptor.ownerPos()).toLong())) return false;
            Set<ChunkPos> required = new HashSet<>();
            addChunks(required, descriptor);
            for (ChunkPos chunk : required) if (!loadedChunks.contains(chunk.toLong())) return false;
            return true;
        }

        private void retire(BlockPos pos, BoilerSurfaceDescriptor descriptor) {
            if (descriptor != null) retired.computeIfAbsent(pos.immutable(), ignored -> new HashSet<>()).add(descriptor.ownerGeneration());
        }

        private static final class Owner {
            private final Object token;
            private BoilerSurfaceDescriptor descriptor;
            private boolean unavailable;
            private Owner(Object token) { this.token = token; }
        }
    }
}
