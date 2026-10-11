package com.iksxh.create_nuclear_industry.boiler.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.lang.ref.WeakReference;
import java.util.HashSet;

/** 只维护汽雾资源与当前客户端会话，不修改锅炉L1/窗口或服务器账本。 */
@EventBusSubscriber(modid="create_nuclear_industry",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class BoilerSteamClientEvents {
    private static WeakReference<ClientLevel> world=new WeakReference<>(null);
    private static WeakReference<ClientLevel> ended=new WeakReference<>(null);
    private static long session;
    private static boolean pendingClear;
    private BoilerSteamClientEvents() {}
    /** 只登记已有Controller类型；原Window水位BER及其注册保持原样。 */
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BoilerContent.CONTROLLER_BE.get(),BoilerSteamRenderer::new);
    }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new BoilerSteamMaterials());
    }
    static long session(){return session;}
    /** 世界切换只在安全客户端边界提交旧批次再清槽，旧Unload后的Level不能重新开启会话。 */
    static void synchronizeWorld() {
        var next=Minecraft.getInstance().level;
        if(next==null)ended=new WeakReference<>(null);
        if(next!=null && next==ended.get())next=null;
        if(world.get()==next && !pendingClear)return;
        BoilerSteamMaterials.flushAndClear();BoilerSteamVisualState.clear();
        world=new WeakReference<>(next);session++;pendingClear=false;
    }
    @EventBusSubscriber(modid="create_nuclear_industry",value=Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {}
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            synchronizeWorld();var minecraft=Minecraft.getInstance();var level=world.get();
            if(level==null || level!=minecraft.level)return;
            BoilerSteamVisualState.CLOCK.tick(minecraft.isPaused());
            var snapshot=BoilerSurfaceSnapshots.capture(level);
            var candidates=new HashSet<>(BoilerSteamMaterials.keys());candidates.addAll(BoilerSteamVisualState.CACHE.keys());
            var valid=BoilerSteamVisualState.validIdentities(snapshot,session,candidates);
            // 前帧本批顶点已提交；暂停保留可靠几何，但撤销/卸载仍即时清理。
            BoilerSteamRenderBuffers.flush(minecraft.renderBuffers().bufferSource()::endBatch);
            BoilerSteamMaterials.boundary(BoilerSteamVisualState.CLOCK.tick(),valid);
            BoilerSteamVisualState.CACHE.retain(valid);
        }
        @SubscribeEvent public static void unload(LevelEvent.Unload event) {
            if(event.getLevel()!=world.get())return;
            ended=new WeakReference<>(world.get());pendingClear=true;BoilerSteamVisualState.CACHE.clear();
        }
        @SubscribeEvent public static void chunkUnload(ChunkEvent.Unload event) {
            if(event.getLevel()!=world.get())return;
            // 只标记已有槽；GPU资源等下一安全边界释放，不能在本帧中途改掉纹理内容。
            for(var key:BoilerSteamMaterials.keys()) {
                var owner=net.minecraft.core.BlockPos.of(key.owner());
                var chunk=event.getChunk().getPos();
                if(((owner.getX()>>4)==chunk.x && (owner.getZ()>>4)==chunk.z)
                        || BoilerSteamVisualState.CACHE.touchesChunk(key,chunk.x,chunk.z))BoilerSteamMaterials.invalidate(key);
            }
            BoilerSteamVisualState.CACHE.clear();
        }
    }
}
