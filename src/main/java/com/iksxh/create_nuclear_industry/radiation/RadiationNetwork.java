package com.iksxh.create_nuclear_industry.radiation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
/** 只注册服务端到客户端显示，没有客户端设置/清空剂量的协议。 */
public final class RadiationNetwork {
    private RadiationNetwork() {}
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(RadiationExposurePayload.TYPE,RadiationExposurePayload.CODEC,(packet,context)->
                context.enqueueWork(()-> {
                    if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
                        com.iksxh.create_nuclear_industry.radiation.client.RadiationClientEvents.receive(packet);
                }));
    }
}
