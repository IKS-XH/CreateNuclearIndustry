package com.iksxh.create_nuclear_industry.structure.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 仅客户端MOD事件入口；自动订阅按Dist过滤，公共逻辑与专用服务端入口不引用本类。
 * ModelManager在本同步事件之后才准备模型与atlas，因此必须在处理器内直接登记。
 */
@EventBusSubscriber(modid = "create_nuclear_industry", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ReactorConnectedTextureClientEvents {
    private ReactorConnectedTextureClientEvents() { }

    @SubscribeEvent
    public static void registerConnectedTextures(ModelEvent.RegisterGeometryLoaders event) {
        // 本事件只借用确定的准备前时机，不添加geometry loader，也不enqueue或重复Create监听器。
        ReactorConnectedTextures.initializeOnce();
    }
}
