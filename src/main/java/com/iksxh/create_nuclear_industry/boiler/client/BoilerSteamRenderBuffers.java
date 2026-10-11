package com.iksxh.create_nuclear_industry.boiler.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/** 独立汽雾固定层，全部BER结束后、区块窗玻璃前统一提交；不在单炉BER提前flush。 */
@EventBusSubscriber(modid="create_nuclear_industry",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class BoilerSteamRenderBuffers {
    private static final List<RenderType> LAYERS=IntStream.range(0,BoilerSteamMaterials.SLOTS)
            .mapToObj(slot->RenderType.entityTranslucent(BoilerSteamMaterials.id(slot))).toList();
    private BoilerSteamRenderBuffers() {}
    static RenderType layer(int slot){return LAYERS.get(slot);}
    @SubscribeEvent public static void register(RegisterRenderBuffersEvent event) {
        for(var layer:LAYERS)event.registerRenderBuffer(layer);
    }
    /** 与生产调用共享的阶段门；只提交本批层，保留原生shader、alpha、深度写入与排序。 */
    static void finish(RenderLevelStageEvent.Stage stage,Consumer<RenderType> finish) {
        if(stage==RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)flush(finish);
    }
    static void flush(Consumer<RenderType> finish){for(var layer:LAYERS)finish.accept(layer);}
    @EventBusSubscriber(modid="create_nuclear_industry",value=Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {}
        @SubscribeEvent public static void render(RenderLevelStageEvent event) {
            finish(event.getStage(),Minecraft.getInstance().renderBuffers().bufferSource()::endBatch);
        }
    }
}
