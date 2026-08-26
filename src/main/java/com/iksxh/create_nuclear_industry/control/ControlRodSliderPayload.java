package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端发往服务端的滑块 START、PREVIEW、COMMIT、CANCEL 请求载荷。 */
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

    /** 将线上的阶段编号解析为协议阶段；未知编号返回 {@code null} 供服务端拒绝。 */
    public ControlRodSliderPhase phase() {
        return ControlRodSliderPhase.fromWireCode(phaseCode);
    }

    /** 构造开始拖动请求。 */
    public static ControlRodSliderPayload start(BlockPos drivePos, int columnX, int columnZ,
                                                 int depthPercent, long dragId) {
        return new ControlRodSliderPayload(drivePos, columnX, columnZ, 0, depthPercent,
                ControlRodSliderPhase.START.wireCode(), dragId);
    }

    /** 构造拖动预览请求。 */
    public static ControlRodSliderPayload preview(BlockPos drivePos, int columnX, int columnZ,
                                                   int depthPercent, long dragId) {
        return new ControlRodSliderPayload(drivePos, columnX, columnZ, 0, depthPercent,
                ControlRodSliderPhase.PREVIEW.wireCode(), dragId);
    }

    /** 构造提交目标深度请求。 */
    public static ControlRodSliderPayload commit(BlockPos drivePos, int columnX, int columnZ,
                                                  int depthPercent, long dragId) {
        return new ControlRodSliderPayload(drivePos, columnX, columnZ, 0, depthPercent,
                ControlRodSliderPhase.COMMIT.wireCode(), dragId);
    }

    /** 构造取消拖动请求；取消阶段不使用深度字段。 */
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
