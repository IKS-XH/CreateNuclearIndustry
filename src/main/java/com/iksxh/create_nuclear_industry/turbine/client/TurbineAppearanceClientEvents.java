package com.iksxh.create_nuclear_industry.turbine.client;

import com.simibubi.create.CreateClient;
import net.createmod.catnip.render.SpriteShifter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.minecraft.resources.ResourceLocation;

/** 客户端MOD准备前同步登记两种模型包装；专用服务端不加载本类，不重复Create监听器。 */
@EventBusSubscriber(modid = "create_nuclear_industry", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TurbineAppearanceClientEvents {
    private static boolean registered;

    private TurbineAppearanceClientEvents() { }

    @SubscribeEvent public static synchronized void registerAppearance(ModelEvent.RegisterGeometryLoaders event) {
        if (registered) return;
        // Catnip持有可随重载更新的entry；此同步事件早于模型/atlas准备，不能enqueue。
        var shift = SpriteShifter.get(TurbineCasingAppearanceModel.SOURCE, TurbineCasingAppearanceModel.TARGET);
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", "turbine_casing"),
                original -> new TurbineCasingAppearanceModel(original, shift));
        CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", "turbine_window"),
                TurbineWindowConnectedModel::new);
        registered = true;
    }
}
