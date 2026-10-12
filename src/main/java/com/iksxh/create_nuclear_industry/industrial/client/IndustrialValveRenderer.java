package com.iksxh.create_nuclear_industry.industrial.client;

import com.iksxh.create_nuclear_industry.content.IndustrialFluidContent;
import com.iksxh.create_nuclear_industry.industrial.IndustrialValveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** 只读绘制原Create阀指针，显示现有0..100开度；红石有效流量仍以原护目镜与服务端规则为准。 */
public final class IndustrialValveRenderer extends SafeBlockEntityRenderer<IndustrialValveBlockEntity> {
    static final PartialModel STEEL = IndustrialPumpRenderer.partial("steel/fluid_valve/pointer");
    static final PartialModel CERAMIC = IndustrialPumpRenderer.partial("ceramic/fluid_valve/pointer");
    public IndustrialValveRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void renderSafe(IndustrialValveBlockEntity valve, float partialTicks, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay) {
        var state = valve.getBlockState(); var axis = state.getValue(BlockStateProperties.AXIS);
        var model = state.is(IndustrialFluidContent.CERAMIC_VALVE.get()) ? CERAMIC : STEEL;
        // 原pointer的Y=13.5/2.5平面须统一转到body的透明窗口，不能留在顶部/底部实心壳内。
        CachedBuffers.partial(model, state).center().rotateYDegrees(IndustrialVisualOrientation.valvePointerYaw(axis))
                .rotateXDegrees(IndustrialVisualOrientation.valvePointerTilt())
                .rotateYDegrees(IndustrialVisualOrientation.valvePointerAngle(axis, valve.opening())).uncenter()
                .light(light).renderInto(pose, buffers.getBuffer(RenderType.solid()));
    }
}
