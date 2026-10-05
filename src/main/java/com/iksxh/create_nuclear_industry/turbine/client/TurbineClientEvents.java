package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 客户端独立注册跨格转子渲染；专用服务端不加载 PartialModel。 */
@EventBusSubscriber(modid = CreateNuclearIndustry.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public final class TurbineClientEvents {
    private TurbineClientEvents() {}

    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // NeoForge 在首次资源加载前注册渲染器，此时先创建 partial 供 Flywheel 的模型注册事件枚举。
        var blades = TurbineRotorRenderer.BLADES;
        event.registerBlockEntityRenderer(TurbineContent.ROTOR_BE.get(), TurbineRotorRenderer::new);
    }
}
