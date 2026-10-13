package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineRotorBlockEntity;
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
import net.minecraft.world.phys.AABB;

/**
 * 绘制已通过B模型的完整轴/轮毂/叶片partial；只读runtimeDisplay整包决定可靠旋转或静止fallback。
 * 渲染包围盒按当前直径扩展到全部叶片扫掠范围，避免轴心格离开视锥后叶片消失。
 */
public final class TurbineRotorRenderer implements BlockEntityRenderer<TurbineRotorBlockEntity> {
    static final PartialModel[] BLADES = {
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
        var display = TurbineClientEvents.rotorPose(rotor, partialTicks);
        if (!state.getValue(TurbinePartBlock.LOCATED)) return;
        int diameter = display.sample().map(sample -> sample.diameter())
                .orElse(state.getValue(TurbinePartBlock.DIAMETER).blocks());
        int index = switch (diameter) {
            case 5 -> 1;
            case 7 -> 2;
            default -> 0;
        };
        renderPiece(CachedBuffers.partial(BLADES[index], state), display, state, pose,
                buffers.getBuffer(RenderType.cutoutMipped()), light, overlay);
    }

    /** 可靠样本同包提供朝向；empty只用当前合法外观静止绘制，不补用上次RPM或方向。 */
    static void renderPiece(SuperByteBuffer buffer, TurbineClientEvents.RotorPose display,
                            net.minecraft.world.level.block.state.BlockState state, PoseStack pose,
                            VertexConsumer vertex, int light, int overlay) {
        Direction facing = display.sample().map(sample -> sample.facing())
                .orElse(state.getValue(TurbinePartBlock.MACHINE_FACING));
        TurbineOutputShaftRenderer.renderPiece(buffer, facing, display.degrees(), pose, vertex, light, overlay);
    }
}
