package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineRotorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/**
 * 只绘制轴心方块模型之外的静态叶片；未来动画可绕同一中心轴转动这个 partial。
 * 渲染包围盒按当前直径扩展到全部叶片扫掠范围，避免轴心格离开视锥后叶片消失。
 */
public final class TurbineRotorRenderer implements BlockEntityRenderer<TurbineRotorBlockEntity> {
    private static final PartialModel[] BLADES = {
            model("rotor_blades_d3"), model("rotor_blades_d5"), model("rotor_blades_d7")};

    private static PartialModel model(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID,
                "block/turbine/" + name));
    }

    public TurbineRotorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public AABB getRenderBoundingBox(TurbineRotorBlockEntity rotor) {
        int diameter = rotor.getBlockState().getValue(TurbinePartBlock.DIAMETER).blocks();
        double extra = diameter / 2D;
        return new AABB(rotor.getBlockPos()).inflate(extra, extra, extra);
    }

    @Override public void render(TurbineRotorBlockEntity rotor, float partialTicks, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        var state = rotor.getBlockState();
        if (!state.getValue(TurbinePartBlock.LOCATED)) return;
        int index = switch (state.getValue(TurbinePartBlock.DIAMETER)) {
            case D3 -> 0;
            case D5 -> 1;
            case D7 -> 2;
        };
        Direction facing = state.getValue(TurbinePartBlock.MACHINE_FACING);
        float yaw = switch (facing) {
            case EAST -> -90;
            case SOUTH -> -180;
            case WEST -> -270;
            default -> 0;
        };
        CachedBuffers.partial(BLADES[index], state)
                .rotateCentered((float) Math.toRadians(yaw), Direction.UP)
                .light(light).renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
    }
}
