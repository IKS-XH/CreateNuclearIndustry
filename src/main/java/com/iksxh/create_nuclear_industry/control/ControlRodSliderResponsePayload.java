package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Clientbound acknowledgement carrying only the server-approved presentation value. */
public record ControlRodSliderResponsePayload(
        BlockPos drivePos,
        int columnX,
        int columnZ,
        int depthPercent,
        int phaseCode,
        int statusCode,
        long dragId,
        boolean authoritativeDepth
) implements CustomPacketPayload {
    public static final Type<ControlRodSliderResponsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "control_rod_slider_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ControlRodSliderResponsePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ControlRodSliderResponsePayload decode(RegistryFriendlyByteBuf buffer) {
                    return new ControlRodSliderResponsePayload(
                            buffer.readBlockPos(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                            buffer.readInt(), buffer.readInt(), buffer.readVarLong(), buffer.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, ControlRodSliderResponsePayload payload) {
                    buffer.writeBlockPos(payload.drivePos());
                    buffer.writeInt(payload.columnX());
                    buffer.writeInt(payload.columnZ());
                    buffer.writeInt(payload.depthPercent());
                    buffer.writeInt(payload.phaseCode());
                    buffer.writeInt(payload.statusCode());
                    buffer.writeVarLong(payload.dragId());
                    buffer.writeBoolean(payload.authoritativeDepth());
                }
            };

    public ControlRodSliderResponsePayload {
        drivePos = drivePos == null ? null : drivePos.immutable();
    }

    public boolean accepted() {
        return ControlRodSliderStatus.fromWireCode(statusCode).accepted();
    }

    public boolean hasAuthoritativeDepth() {
        return authoritativeDepth;
    }

    public ControlRodSliderPhase phase() {
        return ControlRodSliderPhase.fromWireCode(phaseCode);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
