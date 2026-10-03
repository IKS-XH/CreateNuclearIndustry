package com.iksxh.create_nuclear_industry.production;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** 客户端仅绘制朝下的Create原生半轴；静态机身由方块模型负责。 */
public final class ShieldedAssemblyRenderer extends KineticBlockEntityRenderer<ShieldedAssemblyBlockEntity> {
    public ShieldedAssemblyRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected void renderSafe(ShieldedAssemblyBlockEntity machine, float partialTicks, PoseStack pose,
                                        MultiBufferSource buffers, int light, int overlay) {
        if (!machine.current()) return;
        renderRotatingBuffer(machine, CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,
                machine.getBlockState(), Direction.DOWN), pose, buffers.getBuffer(RenderType.solid()), light);
    }
}
