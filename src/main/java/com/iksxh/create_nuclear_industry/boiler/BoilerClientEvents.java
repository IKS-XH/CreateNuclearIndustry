package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/** 客户端窗口渲染注册，专用服务端不会加载渲染类型。窗口填充只取同步比例。 */
@EventBusSubscriber(modid = CreateNuclearIndustry.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BoilerClientEvents {
    private BoilerClientEvents() {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BoilerContent.WINDOW_BE.get(), WindowRenderer::new);
    }
    private static final class WindowRenderer implements BlockEntityRenderer<BoilerWindowBlockEntity> {
        private WindowRenderer(BlockEntityRendererProvider.Context context) {}
        @Override public void render(BoilerWindowBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
            if (be.fill() <= 0) return;
            NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(new FluidStack(Fluids.WATER, 1),
                    .07f, .07f, .07f, .93f, Math.min(.93f, .07f + .86f * be.fill()), .93f, buffer, pose, light, false, true);
        }
    }
}
