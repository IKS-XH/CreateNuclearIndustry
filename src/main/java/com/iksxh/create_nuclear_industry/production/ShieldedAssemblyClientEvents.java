package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 客户端事件预加载活动分件并注册整机渲染器；专用服务端不加载图形类。 */
@EventBusSubscriber(modid = CreateNuclearIndustry.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ShieldedAssemblyClientEvents {
    private ShieldedAssemblyClientEvents() {}
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 显式读取静态列表，令renderer在首次ModelEvent.RegisterAdditional前初始化三个PartialModel。
        var partialModels = ShieldedAssemblyRenderer.PARTIAL_MODELS;
        event.registerBlockEntityRenderer(FuelProcessingContent.SHIELDED_ASSEMBLY_BE.get(), ShieldedAssemblyRenderer::new);
    }
}
