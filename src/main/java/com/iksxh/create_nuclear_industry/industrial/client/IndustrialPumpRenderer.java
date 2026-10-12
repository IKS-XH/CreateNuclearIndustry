package com.iksxh.create_nuclear_industry.industrial.client;

import com.iksxh.create_nuclear_industry.industrial.IndustrialPumpBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** 客户端原Create泵cog及出液法兰箭头；转速来自既有Create同步，箭头不是流体能力或新状态。 */
public final class IndustrialPumpRenderer extends KineticBlockEntityRenderer<IndustrialPumpBlockEntity> {
    static final PartialModel COG = partial("steel/mechanical_pump/cog");
    static final PartialModel ARROW = partial("output_arrow");
    static PartialModel partial(String path) { return PartialModel.of(ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", "block/industrial_fluid_visual_01/" + path)); }
    public IndustrialPumpRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected SuperByteBuffer getRotatedModel(IndustrialPumpBlockEntity pump, BlockState state) {
        return CachedBuffers.partialFacing(COG, state);
    }
    @Override protected void renderSafe(IndustrialPumpBlockEntity pump, float partialTicks, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay) {
        // 本BE没有Flywheel实例visual，故必须直接绘制，不能沿原基类的全局visualization跳过分支。
        renderRotatingBuffer(pump, getRotatedModel(pump, pump.getBlockState()), pose, buffers.getBuffer(RenderType.solid()), light);
        Direction output = IndustrialVisualOrientation.displayedOutput(pump);
        if (output == null) return;
        IndustrialVisualOrientation.orientArrow(CachedBuffers.partial(ARROW, pump.getBlockState()), output)
                .light(light).renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
    }
}
