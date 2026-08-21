package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serverbound payload for start, preview, commit and cancel slider gestures. */
public record ControlRodSliderPayload(
        BlockPos drivePos,
        int columnX,
        int columnZ,
        int row,
        int depthPercent,
        int phaseCode,
        long dragId
) implements CustomPacketPayload {
    public static final Type<ControlRodSliderPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "control_rod_slider"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ControlRodSliderPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ControlRodSliderPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new ControlRodSliderPayload(
                            buffer.readBlockPos(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                            buffer.readInt(), buffer.readInt(), buffer.readVarLong());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, ControlRodSliderPayload payload) {
                    buffer.writeBlockPos(payload.drivePos());
                    buffer.writeInt(payload.columnX());
                    buffer.writeInt(payload.columnZ());
                    buffer.writeInt(payload.row());
                    buffer.writeInt(payload.depthPercent());
                    buffer.writeInt(payload.phaseCode());
                    buffer.writeVarLong(payload.dragId());
                }
            };

    public ControlRodSliderPayload {
        drivePos = drivePos == null ? null : drivePos.immutable();
    }

    public ControlRodSliderPhase phase() {
        return ControlRodSliderPhase.fromWireCode(phaseCode);
    }

    public static ControlRodSliderPayload start(BlockPos drivePos, int columnX, int columnZ,
                                                 int depthPercent, long dragId) {
        return new ControlRodSliderPayload(drivePos, columnX, columnZ, 0, depthPercent,
                ControlRodSliderPhase.START.wireCode(), dragId);
    }

    public static ControlRodSliderPayload preview(BlockPos drivePos, int columnX, int columnZ,
                                                   int depthPercent, long dragId) {
        return new ControlRodSliderPayload(drivePos, columnX, columnZ, 0, depthPercent,
                ControlRodSliderPhase.PREVIEW.wireCode(), dragId);
    }

    public static ControlRodSliderPayload commit(BlockPos drivePos, int columnX, int columnZ,
                                                  int depthPercent, long dragId) {
        return new ControlRodSliderPayload(drivePos, columnX, columnZ, 0, depthPercent,
                ControlRodSliderPhase.COMMIT.wireCode(), dragId);
    }

    public static ControlRodSliderPayload cancel(BlockPos drivePos, int columnX, int columnZ,
                                                  long dragId) {
        return new ControlRodSliderPayload(drivePos, columnX, columnZ, 0, 0,
                ControlRodSliderPhase.CANCEL.wireCode(), dragId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
