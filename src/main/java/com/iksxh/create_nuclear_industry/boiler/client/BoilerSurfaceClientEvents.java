package com.iksxh.create_nuclear_industry.boiler.client;

import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerSurfaceSyncEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 独立物理客户端订阅，游戏线程事件维护加载证据；不遍历玩家世界或强制取区块。
 * 锅炉控制器更新可早于 onLoad/区块加入缓存，暂存其对象并由该区块 Load 的真实 BE 列表核实。
 */
@EventBusSubscriber(modid = "create_nuclear_industry", value = Dist.CLIENT)
public final class BoilerSurfaceClientEvents {
    private static final BoilerSurfaceSnapshots.State STATE = new BoilerSurfaceSnapshots.State();
    private static final Map<BlockPos, BoilerControllerBlockEntity> PENDING = new HashMap<>();
    private static final Map<ChunkPos, LevelChunk> CHUNK_EVIDENCE = new HashMap<>();
    private static final Map<ChunkPos, LevelChunk> UNLOADING = new HashMap<>();
    private static ClientLevel currentWorld;
    private static ClientLevel endedWorld;
    private BoilerSurfaceClientEvents() { }

    /** 世界对象改变就清空信封、旧身份、加载证据和待处理项；tick 不扫描任何方块。 */
    private static void synchronizeWorld() {
        ClientLevel next = Minecraft.getInstance().level;
        // Level.Unload 可早于 Minecraft.level 清空，不能让随后旧 BE 回调重开已结束会话。
        if (next == null) endedWorld = null;
        if (next == endedWorld) next = null;
        else if (next != null) endedWorld = null;
        if (next == currentWorld) return;
        currentWorld = next;
        PENDING.clear();
        CHUNK_EVIDENCE.clear();
        UNLOADING.clear();
        STATE.begin(next, next == null ? "" : next.dimension().location().toString());
        BoilerSurfaceSnapshots.publish(next, STATE.snapshot());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        synchronizeWorld();
        if (currentWorld != null) publishAndRefresh();
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() != currentWorld) return;
        endedWorld = currentWorld;
        currentWorld = null;
        PENDING.clear();
        CHUNK_EVIDENCE.clear();
        UNLOADING.clear();
        STATE.begin(null, "");
        BoilerSurfaceSnapshots.publish(null, STATE.snapshot());
    }

    /** 生命周期/包通知只接受当前真实世界的当前 BE；旧对象的 remove 由 token 比较保护。 */
    @SubscribeEvent
    public static void onOwnerUpdate(BoilerSurfaceSyncEvents.Update event) {
        if (!Minecraft.getInstance().isSameThread()) return;
        if (event.owner().getLevel() != Minecraft.getInstance().level) return;
        synchronizeWorld();
        BoilerControllerBlockEntity owner = event.owner();
        if (currentWorld == null || owner.getLevel() != currentWorld) return;
        BlockPos pos = owner.getBlockPos();
        if (event.removed()) {
            PENDING.remove(pos, owner);
            if (event.chunkUnloaded()) STATE.ownerUnloaded(currentWorld, pos, owner);
            else STATE.ownerRemoved(currentWorld, pos, owner);
        } else {
            LevelChunk chunk = currentWorld.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
            if (chunk == null || chunk.getBlockEntities().get(pos) != owner || owner.isRemoved()) {
                // 旧对象的延迟包不能覆盖已知新 owner 的待验证首包。
                if (chunk == null || chunk.getBlockEntities().get(pos) == null) PENDING.put(pos.immutable(), owner);
                return;
            }
            reconcileChunks();
            receiveOwner(owner);
        }
        publishAndRefresh();
    }

    /** 区块 Load 在 packet 安装之后发布，可从真实 BE 列表可靠恢复，不要求拆放锅炉控制器。 */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel) || !Minecraft.getInstance().isSameThread()) return;
        synchronizeWorld();
        if (currentWorld == null || event.getLevel() != currentWorld || !(event.getChunk() instanceof LevelChunk chunk)) return;
        if (currentWorld.getChunkSource().getChunk(chunk.getPos().x, chunk.getPos().z, ChunkStatus.FULL, false) != chunk) return;
        UNLOADING.remove(chunk.getPos());
        LevelChunk old = CHUNK_EVIDENCE.put(chunk.getPos(), chunk);
        if (old != null && old != chunk) STATE.chunk(currentWorld, chunk.getPos(), false);
        STATE.chunk(currentWorld, chunk.getPos(), true);
        for (var entity : chunk.getBlockEntities().values()) {
            if (entity instanceof BoilerControllerBlockEntity owner && !owner.isRemoved()) {
                receiveOwner(owner);
                PENDING.remove(owner.getBlockPos(), owner);
            }
        }
        publishAndRefresh();
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (currentWorld == null || event.getLevel() != currentWorld) return;
        ChunkPos pos = event.getChunk().getPos();
        LevelChunk actual = currentWorld.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        if (actual != null && actual != event.getChunk()) return;
        // 原生 drop 在缓存移除前发事件；本轮核实不能把尚在缓存中的卸载对象重新视为加载。
        if (event.getChunk() instanceof LevelChunk chunk) UNLOADING.put(pos, chunk);
        CHUNK_EVIDENCE.remove(pos, event.getChunk());
        STATE.chunk(currentWorld, pos, false);
        PENDING.keySet().removeIf(ownerPos -> new ChunkPos(ownerPos).equals(pos));
        publishAndRefresh();
    }

    /** 已核实实例的信封必须匹配真实 owner 坐标；无数据或错身份撤销该实例显示。 */
    private static void receiveOwner(BoilerControllerBlockEntity owner) {
        BlockPos pos = owner.getBlockPos();
        STATE.ownerLoaded(currentWorld, pos, owner);
        var descriptor = owner.surfaceDescriptor();
        if (descriptor.isPresent() && descriptor.get().ownerPos().equals(pos))
            STATE.receive(currentWorld, owner, descriptor.get());
        else STATE.unavailable(currentWorld, pos, owner);
    }

    /** 原子发布在先；只重建变化结构 bounds 外扩一格的已加载区段，绝不强制加载。 */
    private static void publishAndRefresh() {
        reconcileChunks();
        Set<BoilerSurfaceSnapshots.Bounds> changed = STATE.rebuild();
        BoilerSurfaceSnapshots.publish(currentWorld, STATE.snapshot());
        if (currentWorld == null || changed.isEmpty()) return;
        Set<net.minecraft.core.SectionPos> sections = new HashSet<>();
        for (var bounds : changed) {
            BlockPos min = bounds.min().offset(-1, -1, -1);
            BlockPos max = bounds.maxInclusive().offset(1, 1, 1);
            for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) {
                for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) {
                    if (currentWorld.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false) == null) continue;
                    for (int y = Math.max(currentWorld.getMinSection(), min.getY() >> 4);
                         y <= Math.min(currentWorld.getMaxSection() - 1, max.getY() >> 4); y++) {
                        sections.add(net.minecraft.core.SectionPos.of(x, y, z));
                    }
                }
            }
        }
        var renderer = Minecraft.getInstance().levelRenderer;
        for (var section : sections) renderer.setSectionDirty(section.x(), section.y(), section.z());
    }

    /**
     * 原生 Storage 换槽/缩视距存在不发 Unload 的路径；每游戏 tick 只核实已知描述所需区块对象。
     * 不读方块、不扫描未知 BE 或玩家世界；仅在已知区块表核实 owner 槽位，更换实例先撤销再接收。
     */
    private static void reconcileChunks() {
        if (currentWorld == null) return;
        Set<ChunkPos> required = STATE.requiredChunks();
        for (ChunkPos pos : required) {
            LevelChunk actual = currentWorld.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
            LevelChunk unloading = UNLOADING.get(pos);
            if (actual != null && actual != unloading) UNLOADING.remove(pos);
            if (actual == unloading) actual = null;
            LevelChunk old = CHUNK_EVIDENCE.get(pos);
            if (actual == old) {
                if (actual == null) STATE.chunk(currentWorld, pos, false);
                continue;
            }
            if (old != null) STATE.chunk(currentWorld, pos, false);
            if (actual == null) CHUNK_EVIDENCE.remove(pos);
            else {
                CHUNK_EVIDENCE.put(pos, actual);
                STATE.chunk(currentWorld, pos, true);
            }
        }
        // 只查已知 owner 的真实 BE 槽位；同区块对象内替换 BE 也必须撤销旧身份。
        for (var entry : STATE.ownerTokens().entrySet()) {
            BlockPos pos = entry.getKey();
            LevelChunk chunk = CHUNK_EVIDENCE.get(new ChunkPos(pos));
            var actual = chunk == null ? null : chunk.getBlockEntities().get(pos);
            if (actual == entry.getValue() && !actual.isRemoved()) continue;
            if (chunk == null) STATE.ownerUnloaded(currentWorld, pos, entry.getValue());
            else STATE.ownerRemoved(currentWorld, pos, entry.getValue());
            if (actual instanceof BoilerControllerBlockEntity owner && !owner.isRemoved()) receiveOwner(owner);
        }
        Set<BlockPos> candidates = new HashSet<>(STATE.suspendedOwners());
        candidates.addAll(PENDING.keySet());
        for (BlockPos pos : candidates) {
            LevelChunk chunk = CHUNK_EVIDENCE.get(new ChunkPos(pos));
            if (chunk == null) continue;
            if (chunk.getBlockEntities().get(pos) instanceof BoilerControllerBlockEntity owner && !owner.isRemoved()) {
                receiveOwner(owner);
                PENDING.remove(pos);
            }
        }
        // owner 卸载可能在本轮核实中改变所需集合，随后丢弃已无用途的加载对象引用。
        required = STATE.requiredChunks();
        CHUNK_EVIDENCE.keySet().retainAll(required);
        UNLOADING.keySet().retainAll(required);
        STATE.retainChunkEvidence(required);
    }
}
