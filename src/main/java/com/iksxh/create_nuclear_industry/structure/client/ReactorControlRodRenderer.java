package com.iksxh.create_nuclear_industry.structure.client;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/** 驱动只消费同一不可变快照中的归属与实际深度；完整杆整体移动，端部不缩放。 */
public final class ReactorControlRodRenderer implements BlockEntityRenderer<ControlRodDriveBlockEntity> {
    public ReactorControlRodRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public AABB getRenderBoundingBox(ControlRodDriveBlockEntity drive) {
        var level=drive.getLevel();if(level==null)return new AABB(drive.getBlockPos());
        var rod=ReactorRuntimeSnapshots.capture(level).findControlRod(drive.getBlockPos()).orElse(null);
        if(rod==null)return new AABB(drive.getBlockPos());
        var cap=rod.capPos();double travel=rod.bodyPositions().size();
        return new AABB(cap.getX(),cap.getY()-travel,cap.getZ(),cap.getX()+1,cap.getY()+travel+3./16,cap.getZ()+1);
    }
    @Override public void render(ControlRodDriveBlockEntity drive,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        ReactorAnimationClientEvents.synchronizeWorld();var level=drive.getLevel();if(level==null || level!=Minecraft.getInstance().level)return;
        var snapshot=ReactorRuntimeSnapshots.capture(level);
        var rod=snapshot.findControlRod(drive.getBlockPos()).orElse(null);
        var owner=snapshot.findControlOwner(drive.getBlockPos()).orElse(null);
        if(rod==null || owner==null || !ReactorAnimationVisualState.matches(level,owner.ownerPos(),"create_nuclear_industry:reactor_instrument_port")
                || !ReactorAnimationVisualState.matches(level,rod)) {
            ReactorAnimationVisualState.RODS.invalidateCap(drive.getBlockPos().asLong());return;
        }
        var key=ReactorAnimationVisualState.identity(owner);var minecraft=Minecraft.getInstance();
        double time=ReactorAnimationVisualState.CLOCK.time(partial,minecraft.isPaused());
        var position=ReactorAnimationVisualState.rodPose(key,rod,time);
        renderPiece(CachedBuffers.partial(ReactorAnimationModels.SHAFT,drive.getBlockState()),position,rod.capPos(),false,level,pose,buffers.getBuffer(RenderType.cutoutMipped()));
        renderPiece(CachedBuffers.partial(ReactorAnimationModels.HEAD,drive.getBlockState()),position,rod.capPos(),true,level,pose,buffers.getBuffer(RenderType.cutoutMipped()));
    }
    /**
     * 完整杆的生产提交入口，输入位姿为世界方块单位；单位shaft仅沿Y按行程缩放，3/16厚head固定。
     * 实际位移放在SBB自身变换中，Catnip采光才能沿实际拔出/插入位置移动；BER相机Pose保持基准。
     * 采光矩阵只含cap世界平移，不能带相机矩阵或重复bottom/travel变换；target不参与绘制。
     */
    static void renderPiece(SuperByteBuffer buffer,ReactorAnimationVisualState.RodPose position,BlockPos cap,boolean head,
                            BlockAndTintGetter level,PoseStack pose,VertexConsumer vertex) {
        buffer.translate(0,(head?position.top():position.bottom())-cap.getY(),0);
        if(!head)buffer.scale(1,(float)position.travel(),1);
        buffer.useLevelLight(level,new Matrix4f().translation(cap.getX(),cap.getY(),cap.getZ()))
                .overlay(OverlayTexture.NO_OVERLAY).renderInto(pose,vertex);
    }
}
