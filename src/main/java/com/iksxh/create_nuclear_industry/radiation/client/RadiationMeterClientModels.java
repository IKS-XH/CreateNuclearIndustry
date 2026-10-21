package com.iksxh.create_nuclear_industry.radiation.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** 独立客户端模型生命周期入口；重载worker仅处理事件模型表，玩家读取留在运行时。 */
@EventBusSubscriber(modid = "create_nuclear_industry", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class RadiationMeterClientModels {
    private static final String MODID = "create_nuclear_industry";
    private static final ModelResourceLocation BODY = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MODID, "item/radiation_meter_body"));
    private static final ModelResourceLocation NEEDLE = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(MODID, "item/radiation_meter_needle"));
    private static final ModelResourceLocation ENTRY = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath(MODID, "radiation_meter"));
    private static final RadiationMeterNeedleMotion MOTION = new RadiationMeterNeedleMotion();

    private RadiationMeterClientModels() {}

    @SubscribeEvent
    public static void additional(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(NEEDLE);
    }

    @SubscribeEvent
    public static void baked(ModelEvent.ModifyBakingResult event) {
        BakedModel entry = event.getModels().get(ENTRY);
        BakedModel body = event.getModels().get(BODY);
        BakedModel needle = event.getModels().get(NEEDLE);
        if (entry == null || body == null || needle == null) throw new IllegalStateException("辐射计动画模型未完成烘焙");
        // 每次重载建立全新包装；不保留上一轮BakedModel，也不在worker访问Minecraft。
        event.getModels().put(ENTRY, new RadiationMeterBakedModel(entry, body, needle, RadiationMeterClientModels::needleAngle));
    }

    private static double needleAngle() {
        Minecraft minecraft = Minecraft.getInstance();
        return MOTION.angleForRender(minecraft.level,
                minecraft.level == null ? null : minecraft.level.dimension().location(), minecraft.isPaused(),
                minecraft.getTimer().getGameTimeDeltaPartialTick(false));
    }

    /** GAME总线负责显示tick及登出清空；原辐射接收器、HUD和服务端状态不变。 */
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static final class Ticks {
        private Ticks() {}

        @SubscribeEvent
        public static void tick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                MOTION.reset();
                return;
            }
            var payload = RadiationClientEvents.meterSample(minecraft.level);
            var sample = payload == null ? null : new RadiationMeterNeedleMotion.Sample(
                    payload.dimension(), payload.gameTime(), payload.field(), payload.complete());
            MOTION.tick(minecraft.level, minecraft.level.dimension().location(), minecraft.level.getGameTime(), minecraft.isPaused(), sample);
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            MOTION.reset();
        }
    }
}
