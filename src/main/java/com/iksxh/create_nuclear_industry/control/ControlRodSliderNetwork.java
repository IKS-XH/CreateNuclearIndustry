package com.iksxh.create_nuclear_industry.control;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 服务端权威控制棒滑块协议的注册与传输边界。 */
public final class ControlRodSliderNetwork {
    private ControlRodSliderNetwork() {
    }

    /** 在 NeoForge payload 注册生命周期中声明双向协议版本和处理器。 */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(ControlRodSliderPayload.TYPE, ControlRodSliderPayload.STREAM_CODEC,
                        ControlRodSliderNetwork::handleServer)
                .playToClient(ControlRodSliderResponsePayload.TYPE, ControlRodSliderResponsePayload.STREAM_CODEC,
                        ControlRodSliderNetwork::handleClient);
    }

    /** 将服务端 payload 切回游戏线程，完成校验后只向发送者回传结果。 */
    public static void handleServer(ControlRodSliderPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                sendResponse(player, ControlRodSliderResponsePayloadFactory.from(
                        payload == null ? null : payload.drivePos(),
                        ControlRodSliderService.handle(player, payload)));
            }
        });
    }

    /** 仅在物理客户端处理服务端响应，避免加载客户端展示类到专用服务端。 */
    public static void handleClient(ControlRodSliderResponsePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist.isClient()) {
            context.enqueueWork(() -> ControlRodSliderClientHandler.handle(payload));
        }
    }

    /** 向指定服务端玩家发送已编码的权威展示响应。 */
    public static void sendResponse(ServerPlayer player, ControlRodSliderResponsePayload response) {
        if (player != null && response != null) {
            PacketDistributor.sendToPlayer(player, response);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ControlRodSliderService.clearSession(player);
        }
    }
}
