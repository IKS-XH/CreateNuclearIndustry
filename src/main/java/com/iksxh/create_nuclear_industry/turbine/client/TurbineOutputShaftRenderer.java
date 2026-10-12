package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.turbine.TurbineOutputShaftBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineShaftBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * 仅绘制输出轴/内圈的旋转partial，固定箱座和外圈仍由原方块模型负责。
 * 速度使用Create真实同步getSpeed，角度由客户端累计，不改变动力网或机组发电状态。
 */
public final class TurbineOutputShaftRenderer implements BlockEntityRenderer<TurbineOutputShaftBlockEntity> {
    static final PartialModel[] SHAFTS = {model("output_shaft_front_rotating"), model("output_shaft_rear_rotating")};

    public TurbineOutputShaftRenderer(BlockEntityRendererProvider.Context context) {}

    private static PartialModel model(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID,
                "block/turbine/" + name));
    }

    static PartialModel partialFor(TurbineShaftBlock.End end) {
        return SHAFTS[end == TurbineShaftBlock.End.FRONT ? 0 : 1];
    }

    @Override public void render(TurbineOutputShaftBlockEntity shaft, float partialTicks, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        var state = shaft.getBlockState();
        if (!state.getValue(TurbinePartBlock.LOCATED)) return;
        double angle = TurbineClientEvents.outputAngle(shaft, partialTicks);
        renderPiece(CachedBuffers.partial(partialFor(state.getValue(TurbineShaftBlock.END)), state),
                state.getValue(TurbinePartBlock.MACHINE_FACING), angle, pose,
                buffers.getBuffer(RenderType.cutoutMipped()), light, overlay);
    }

    /** Create正速度沿世界正轴；localZ朝向机后，东/南时须翻转符号以保持真实旋向。 */
    static float localRpm(Direction facing, float worldRpm) {
        return switch (facing) {
            case EAST, SOUTH -> -worldRpm;
            default -> worldRpm;
        };
    }

    /**
     * 角度单位为度，partial原点是本格；SBB矩阵右乘得到Yaw*Roll，几何先绕localZ中心(.5,.5)转动。
     * 外部BER Pose含相机/方块定位，保持原样；只将动态partial提交一次，固定端板不经过此入口。
     */
    static void renderPiece(SuperByteBuffer buffer, Direction facing, double degrees, PoseStack pose,
                            VertexConsumer vertex, int light, int overlay) {
        float yaw = switch (facing) {
            case EAST -> -90;
            case SOUTH -> -180;
            case WEST -> -270;
            default -> 0;
        };
        buffer.rotateCentered((float) Math.toRadians(yaw), Direction.UP)
                .rotateCentered((float) Math.toRadians(degrees), Direction.SOUTH)
                .light(light).overlay(overlay).renderInto(pose, vertex);
    }
}
