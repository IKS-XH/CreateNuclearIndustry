package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 客户端事件只注册转轴渲染，专用服务端不加载图形类。 */
@EventBusSubscriber(modid = CreateNuclearIndustry.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ShieldedAssemblyClientEvents {
    private ShieldedAssemblyClientEvents() {}
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FuelProcessingContent.SHIELDED_ASSEMBLY_BE.get(), ShieldedAssemblyRenderer::new);
    }
}
