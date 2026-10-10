package com.iksxh.create_nuclear_industry.structure.client;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.lang.ref.WeakReference;

/** 客户端MOD生命周期负责分件、既有BE渲染器与资源重载，公共入口不引用图形类。 */
@EventBusSubscriber(modid="create_nuclear_industry", value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class ReactorAnimationClientEvents {
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static long session;
    private static boolean pendingClear;
    private ReactorAnimationClientEvents() {}
    @SubscribeEvent public static void models(ModelEvent.RegisterGeometryLoaders event) {
        // 同步事件实际先于首次ModelManager模型/atlas准备；不能enqueue到FMLClientSetup。
        ReactorAnimationModels.initializeOnce();
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(P1BlockEntities.REACTOR_INSTRUMENT_PORT.get(), ReactorInternalRenderer::new);
        event.registerBlockEntityRenderer(P1BlockEntities.CONTROL_ROD_DRIVE.get(), ReactorControlRodRenderer::new);
    }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) { event.registerReloadListener(new ReactorAnimationMaterials()); }
    public static long session() { return session; }
    /** 世界身份仅弱引用；切换时先提交旧批次，数值会话不跨Level复用。 */
    public static void synchronizeWorld() {
        ClientLevel next=Minecraft.getInstance().level;
        if(world.get()==next && !pendingClear)return;
        ReactorAnimationMaterials.flushAndClear(); ReactorAnimationVisualState.clear();
        world=new WeakReference<>(next);session++;pendingClear=false;
    }
    /** 游戏事件独立订阅，不修改L1/L2的租约、同步与登记。 */
    @EventBusSubscriber(modid="create_nuclear_industry", value=Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {}
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            synchronizeWorld();var minecraft=Minecraft.getInstance();
            if(minecraft.level==null)return;
            ReactorAnimationVisualState.CLOCK.tick(minecraft.isPaused());
            // 客户端tick边界前一帧批次已结束；暂停只清理可靠失效身份，不推进过期时间。
            var snapshot=ReactorRuntimeSnapshots.capture(minecraft.level);
            ReactorAnimationMaterials.boundary(snapshot,ReactorAnimationVisualState.CLOCK.tick());
            ReactorAnimationVisualState.boundary(snapshot);
            ReactorAnimationVisualState.expire();
        }
        @SubscribeEvent public static void unload(LevelEvent.Unload event) {
            if(event.getLevel()==world.get()) { pendingClear=true; ReactorAnimationVisualState.clear(); }
        }
        @SubscribeEvent public static void chunkUnload(ChunkEvent.Unload event) {
            if(event.getLevel()!=world.get())return;
            // 撤下位姿立即完成，GPU槽等下一安全tick与L2发布快照共同核实，避免抢同帧纹理。
            ReactorAnimationVisualState.RODS.clear();
        }
    }
}
