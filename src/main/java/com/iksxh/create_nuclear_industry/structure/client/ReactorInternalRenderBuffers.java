package com.iksxh.create_nuclear_industry.structure.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 客户端反应堆液体批次：沿用现有256个纹理槽，在全部BER提交后才绘制液体。
 * 固定注册只分配原生渲染缓冲，不增加纹理或库存；初始384KiB，顶点按原生机制增长。
 * 类初始化不分配原生缓冲；保留原entityTranslucent的shader、深度测试/写入与顶点alpha。
 */
@EventBusSubscriber(modid="create_nuclear_industry",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ReactorInternalRenderBuffers {
    private static final List<RenderType> LIQUIDS=IntStream.range(0,256).mapToObj(slot->layer(
            ResourceLocation.fromNamespaceAndPath("create_nuclear_industry","dynamic/reactor_coolant/slot_"+slot))).toList();
    private ReactorInternalRenderBuffers() {}
    /** 返回原生缓存层；与材质池现有槽ID一致，不另建shader或改变深度状态。 */
    static RenderType layer(ResourceLocation texture) { return RenderType.entityTranslucent(texture); }
    /** 物理客户端初始化时为每个有界槽注册固定缓冲，避免共享层切换提前画液体。 */
    @SubscribeEvent public static void register(RegisterRenderBuffersEvent event) {
        for(var layer:LIQUIDS)event.registerRenderBuffer(layer);
    }
    /** 全部BER结束后先提交剩余棒体与辉光再提交液体；只刷新指定层，不结束其他共享批次。 */
    static void finish(RenderLevelStageEvent.Stage stage,MultiBufferSource.BufferSource buffers) {
        if(stage!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
        buffers.endBatch(RenderType.cutoutMipped());
        buffers.endBatch(RenderType.translucent());
        for(var layer:LIQUIDS)buffers.endBatch(layer);
    }
    /** 游戏总线阶段监听独立于注册总线；不能在某一个owner的BER内提前刷新液体。 */
    @EventBusSubscriber(modid="create_nuclear_industry",value=Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {}
        /** 原生阶段位于不透明Sheet提交之后、主要区块透明层之前。 */
        @SubscribeEvent public static void render(RenderLevelStageEvent event) {
            if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
            finish(event.getStage(),Minecraft.getInstance().renderBuffers().bufferSource());
        }
    }
}
