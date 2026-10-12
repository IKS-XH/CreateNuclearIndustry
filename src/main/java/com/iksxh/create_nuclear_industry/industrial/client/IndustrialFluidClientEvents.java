package com.iksxh.create_nuclear_industry.industrial.client;

import com.iksxh.create_nuclear_industry.content.IndustrialFluidContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** 物理客户端MOD事件注册分件与BE渲染器；专服不加载图形类，不建立原生流体行为。 */
@EventBusSubscriber(modid = "create_nuclear_industry", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class IndustrialFluidClientEvents {
    private IndustrialFluidClientEvents() {}
    @SubscribeEvent public static void models(ModelEvent.RegisterGeometryLoaders event) {
        // 早于首次烘焙触发PartialModel登记，避免只在RegisterRenderers时注册导致首载缺活动件。
        var cog = IndustrialPumpRenderer.COG; var arrow = IndustrialPumpRenderer.ARROW;
        var steel = IndustrialValveRenderer.STEEL; var ceramic = IndustrialValveRenderer.CERAMIC;
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(IndustrialFluidContent.PUMP_ENTITY.get(), IndustrialPumpRenderer::new);
        event.registerBlockEntityRenderer(IndustrialFluidContent.VALVE_ENTITY.get(), IndustrialValveRenderer::new);
    }
}
