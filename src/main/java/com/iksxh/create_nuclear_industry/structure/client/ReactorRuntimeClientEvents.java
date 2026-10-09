package com.iksxh.create_nuclear_industry.structure.client;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.structure.ReactorSurfaceSyncEvents;
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
import java.util.Map;

/** 独立物理客户端 L2 订阅；仅核实已知 chunk/owner 实例，不加载区块、不扫描玩家世界。 */
@EventBusSubscriber(modid = "create_nuclear_industry", value = Dist.CLIENT)
public final class ReactorRuntimeClientEvents {
    private static final ReactorRuntimeSnapshots.State STATE = new ReactorRuntimeSnapshots.State();
    private static final Map<BlockPos, ReactorInstrumentPortBlockEntity> PENDING = new HashMap<>();
    private static final Map<ChunkPos, LevelChunk> UNLOADING = new HashMap<>();
    private static ClientLevel currentWorld;
    private static ClientLevel endedWorld;
    private ReactorRuntimeClientEvents() { }

    /** 原生世界 Unload 可能先于 Minecraft.level 清空；结束的实例不能被旧 BE 回调重开。 */
    private static void synchronizeWorld() {
        ClientLevel next = Minecraft.getInstance().level;
        if (next == null) endedWorld = null;
        if (next == endedWorld) next = null;
        else if (next != null) endedWorld = null;
        if (next == currentWorld) return;
        currentWorld = next; PENDING.clear(); UNLOADING.clear();
        STATE.begin(next, next == null ? "" : next.dimension().location().toString());
        publish();
    }
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        synchronizeWorld();
        if (currentWorld == null) return;
        STATE.tick(); reconcile(); publish();
    }
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() != currentWorld) return;
        endedWorld = currentWorld; currentWorld = null; PENDING.clear(); UNLOADING.clear();
        STATE.begin(null, ""); publish();
    }
    /** 更新包或生命周期事件必须来自当前真实 world 中的当前 BE 对象。 */
    @SubscribeEvent
    public static void onOwnerUpdate(ReactorSurfaceSyncEvents.Update event) {
        if (!Minecraft.getInstance().isSameThread() || event.owner().getLevel() != Minecraft.getInstance().level) return;
        synchronizeWorld();
        var owner = event.owner();
        if (currentWorld == null || owner.getLevel() != currentWorld) return;
        BlockPos pos = owner.getBlockPos();
        if (event.removed()) {
            PENDING.remove(pos, owner); STATE.ownerRemoved(currentWorld, pos, owner, event.chunkUnloaded());
        } else {
            LevelChunk actual = query(new ChunkPos(pos));
            if (actual == null || actual.getBlockEntities().get(pos) != owner || owner.isRemoved()) {
                PENDING.put(pos.immutable(), owner); return;
            }
            receive(owner);
        }
        reconcile(); publish();
    }
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ClientLevel) || !Minecraft.getInstance().isSameThread()) return;
        synchronizeWorld();
        if (currentWorld == null || event.getLevel() != currentWorld || !(event.getChunk() instanceof LevelChunk chunk)) return;
        if (currentWorld.getChunkSource().getChunk(chunk.getPos().x, chunk.getPos().z, ChunkStatus.FULL, false) != chunk) return;
        UNLOADING.remove(chunk.getPos()); STATE.chunk(currentWorld, chunk.getPos(), chunk);
        for (var entity : chunk.getBlockEntities().values()) {
            if (entity instanceof ReactorInstrumentPortBlockEntity owner && !owner.isRemoved()) {
                receive(owner); PENDING.remove(owner.getBlockPos(), owner);
            }
        }
        reconcile(); publish();
    }
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (currentWorld == null || event.getLevel() != currentWorld) return;
        ChunkPos pos = event.getChunk().getPos();
        LevelChunk actual = currentWorld.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        if (actual != null && actual != event.getChunk()) return;
        // 缓存 drop 可在移除槽前发事件；显式排除该卸载实例，避免本轮查询恢复它。
        if (event.getChunk() instanceof LevelChunk chunk) UNLOADING.put(pos, chunk);
        STATE.chunk(currentWorld, pos, null);
        PENDING.keySet().removeIf(p -> new ChunkPos(p).equals(pos)); publish();
    }
    private static void receive(ReactorInstrumentPortBlockEntity owner) {
        BlockPos pos = owner.getBlockPos();
        STATE.ownerLoaded(currentWorld, pos, owner);
        var descriptor = owner.runtimeDescriptor();
        if (descriptor.isEmpty() || !descriptor.get().ownerPos().equals(pos)) {
            STATE.unavailable(currentWorld, pos, owner); return;
        }
        // 包可能第一次介绍成员区块；只核实描述的有限 chunk 对象，之后才接收新样本。
        for (ChunkPos chunk : ReactorRuntimeSnapshots.State.required(descriptor.get(), pos)) STATE.chunk(currentWorld, chunk, query(chunk));
        STATE.receive(currentWorld, owner, descriptor.get(), owner.surfaceDescriptor().orElse(null));
    }
    private static LevelChunk query(ChunkPos pos) {
        LevelChunk actual = currentWorld.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        LevelChunk unloading = UNLOADING.get(pos);
        if (actual == unloading) return null;
        if (unloading != null) UNLOADING.remove(pos);
        return actual;
    }
    /** 每 tick 补偿原生缩视距/换槽的无 Unload 路径；替换实例只撤销，恢复等待新可靠样本。 */
    private static void reconcile() {
        if (currentWorld == null) return;
        var required = STATE.requiredChunks();
        for (ChunkPos pos : required) STATE.chunk(currentWorld, pos, query(pos));
        STATE.retainChunkEvidence(required); UNLOADING.keySet().retainAll(required);
    }
    private static void publish() { ReactorRuntimeSnapshots.publish(currentWorld, STATE.snapshot()); }
}
