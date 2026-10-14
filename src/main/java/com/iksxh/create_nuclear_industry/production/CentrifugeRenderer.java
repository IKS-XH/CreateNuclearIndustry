package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

/** 客户端只在有效下段同相位渲染独立转子与底部输入轴；两者复用真实有符号RPM，渲染框覆盖整机两格。 */
public final class CentrifugeRenderer extends KineticBlockEntityRenderer<CentrifugeBlockEntity> {
    public static final PartialModel ROTOR = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "block/enrichment_centrifuge_rotor"));
    public static final PartialModel SHAFT = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "block/enrichment_centrifuge_shaft"));

    public CentrifugeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override public AABB getRenderBoundingBox(CentrifugeBlockEntity machine) {
        return new AABB(machine.getBlockPos()).expandTowards(0, 1, 0);
    }

    @Override protected void renderSafe(CentrifugeBlockEntity machine, float partialTicks, PoseStack pose,
                                        MultiBufferSource buffers, int light, int overlay) {
        if (machine.getLevel() == null || CentrifugeBlock.owner(machine.getLevel(),
                machine.getBlockPos(), machine.getBlockState()) != machine) return;
        float speed = machine.getSpeed();
        boolean powered = speed != 0 && !machine.isOverStressed()
                && machine.pauseReason() != CentrifugeBlockEntity.PauseReason.BEARING_WORN;
        float angle = powered ? ((AnimationTickHolder.getRenderTime(machine.getLevel()) * speed * 3f / 10f) % 360f)
                * (float) Math.PI / 180f : 0f;
        CachedBuffers.partial(ROTOR, machine.getBlockState())
                .rotateCentered(angle, Direction.UP).light(light)
                .renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
        // 输入轴与转子共享原有有符号角度和停转条件；不调用super重复绘制通用轴，也不让静态轴座跟转。
        CachedBuffers.partial(SHAFT, machine.getBlockState())
                .rotateCentered(angle, Direction.UP).light(light)
                .renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
    }
}
