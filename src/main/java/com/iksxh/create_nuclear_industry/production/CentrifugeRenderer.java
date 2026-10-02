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

/** 只在有效下段渲染跨两格的独立转子；本机渲染框覆盖整机高度。 */
public final class CentrifugeRenderer extends KineticBlockEntityRenderer<CentrifugeBlockEntity> {
    public static final PartialModel ROTOR = PartialModel.of(
            ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "block/enrichment_centrifuge_rotor"));

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
    }
}
