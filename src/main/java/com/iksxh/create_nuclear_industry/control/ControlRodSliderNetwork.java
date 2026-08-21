package com.iksxh.create_nuclear_industry.control;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Registration and transport boundary for the server-authoritative slider protocol. */
public final class ControlRodSliderNetwork {
    private ControlRodSliderNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(ControlRodSliderPayload.TYPE, ControlRodSliderPayload.STREAM_CODEC,
                        ControlRodSliderNetwork::handleServer)
                .playToClient(ControlRodSliderResponsePayload.TYPE, ControlRodSliderResponsePayload.STREAM_CODEC,
                        ControlRodSliderNetwork::handleClient);
    }

    public static void handleServer(ControlRodSliderPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                sendResponse(player, ControlRodSliderResponsePayloadFactory.from(
                        payload == null ? null : payload.drivePos(),
                        ControlRodSliderService.handle(player, payload)));
            }
        });
    }

    public static void handleClient(ControlRodSliderResponsePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist.isClient()) {
            context.enqueueWork(() -> ControlRodSliderClientHandler.handle(payload));
        }
    }

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
