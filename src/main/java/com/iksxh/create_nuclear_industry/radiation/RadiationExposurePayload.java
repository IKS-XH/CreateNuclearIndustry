package com.iksxh.create_nuclear_industry.radiation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
/** 仅S2C显示包；场强/吸收D/s、剂量D，采样时间为服务端gameTime。 */
public record RadiationExposurePayload(ResourceLocation dimension,long gameTime,double field,double dose,double absorbed,boolean complete) implements CustomPacketPayload {
    public static final Type<RadiationExposurePayload> TYPE=new Type<>(ResourceLocation.parse("create_nuclear_industry:radiation_exposure"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RadiationExposurePayload> CODEC=new StreamCodec<>() {
        public RadiationExposurePayload decode(RegistryFriendlyByteBuf b) {return new RadiationExposurePayload(b.readResourceLocation(),b.readLong(),b.readDouble(),b.readDouble(),b.readDouble(),b.readBoolean());}
        public void encode(RegistryFriendlyByteBuf b,RadiationExposurePayload p) {b.writeResourceLocation(p.dimension);b.writeLong(p.gameTime);b.writeDouble(p.field);b.writeDouble(p.dose);b.writeDouble(p.absorbed);b.writeBoolean(p.complete);}
    };
    /** 完成结果允许接替较新的pending通知；其完成时刻不得被pending时间戳永久压住。 */
    public boolean supersedes(RadiationExposurePayload previous) {
        return previous==null||dimension.equals(previous.dimension())&&(gameTime>previous.gameTime()||complete&&!previous.complete());
    }
    @Override public Type<? extends CustomPacketPayload> type() {return TYPE;}
}
