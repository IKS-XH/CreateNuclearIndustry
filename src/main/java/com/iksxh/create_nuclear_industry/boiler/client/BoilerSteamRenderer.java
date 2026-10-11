package com.iksxh.create_nuclear_industry.boiler.client;

import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerSteamInventoryKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.function.ToIntFunction;

/**
 * 唯一Controller绘制整炉真实汽区；只在完整clientPacket后的渲染入口采样，不监听L1通知读取容量。
 * 同一帧使用同一归属、两库存及实际同步容量；未知/矛盾/零库存无雾，停热库存仍可见。
 */
public final class BoilerSteamRenderer implements BlockEntityRenderer<BoilerControllerBlockEntity> {
    public BoilerSteamRenderer(BlockEntityRendererProvider.Context context) {}
    /** 单次渲染输入；浓度和配比均由本次三个实际mB读数派生，不保留可变账本。 */
    record Sample(BoilerSurfaceSnapshot.Member member,double concentration,double ratio) {}

    /** 此方法与生产render共用；先核当前世界/BE槽位及L1全部几何身份，再读三个mB值一次。 */
    static Sample sample(BoilerControllerBlockEntity be,ClientLevel currentWorld,BoilerSurfaceSnapshot snapshot) {
        var member=geometry(be,currentWorld,snapshot);if(member==null)return null;
        int normal=be.ledger().steam(BoilerSteamInventoryKind.NORMAL);
        int supercritical=be.ledger().steam(BoilerSteamInventoryKind.SUPERCRITICAL);
        int capacity=be.syncedSteamCapacityMb();
        double concentration=BoilerSteamVisualState.concentration(normal,supercritical,capacity);
        return concentration<=0?null:new Sample(member,concentration,BoilerSteamVisualState.supercriticalRatio(normal,supercritical));
    }
    private static BoilerSurfaceSnapshot.Member geometry(BoilerControllerBlockEntity be,ClientLevel world,BoilerSurfaceSnapshot snapshot) {
        if(world==null || be.getLevel()!=world || be.isRemoved() || !world.hasChunkAt(be.getBlockPos())
                || world.getBlockEntity(be.getBlockPos())!=be)return null;
        var member=BoilerSteamVisualState.owner(snapshot,be.getBlockPos());if(member==null)return null;
        if(!world.dimension().location().toString().equals(member.dimension())
                || !BuiltInRegistries.BLOCK.getKey(world.getBlockState(be.getBlockPos()).getBlock()).toString().equals(member.expectedBlockId()))return null;
        var descriptor=be.surfaceDescriptor().orElse(null);
        if(descriptor==null || !descriptor.valid() || !descriptor.dimension().equals(member.dimension())
                || !descriptor.ownerPos().equals(member.ownerPos()) || !descriptor.ownerGeneration().equals(member.ownerGeneration())
                || descriptor.revision()!=member.revision() || !descriptor.min().equals(member.min())
                || !descriptor.maxInclusive().equals(member.maxInclusive()) || descriptor.partitionY()!=member.partitionY())return null;
        // 只核有限闭边界涉及的区块已加载，不取块扫描结构，也不强制加载缺块。
        for(int x=member.min().getX()>>4;x<=member.maxInclusive().getX()>>4;x++)
            for(int z=member.min().getZ()>>4;z<=member.maxInclusive().getZ()>>4;z++)if(!world.hasChunk(x,z))return null;
        return member;
    }
    /** 全域BER必须进入global列表，随后仍由有限气域AABB和距离裁剪；未知仅单格包围盒。 */
    @Override public boolean shouldRenderOffScreen(BoilerControllerBlockEntity be){return true;}
    @Override public AABB getRenderBoundingBox(BoilerControllerBlockEntity be) {
        var world=Minecraft.getInstance().level;
        return bounds(be,world,BoilerSurfaceSnapshots.capture(world));
    }
    static AABB bounds(BoilerControllerBlockEntity be,ClientLevel world,BoilerSurfaceSnapshot snapshot) {
        var member=geometry(be,world,snapshot);
        return member==null?new AABB(be.getBlockPos()):new AABB(member.min().getX()+1,member.partitionY()+1,member.min().getZ()+1,
                member.maxInclusive().getX(),member.maxInclusive().getY(),member.maxInclusive().getZ());
    }
    @Override public boolean shouldRender(BoilerControllerBlockEntity be,Vec3 camera) {
        var world=Minecraft.getInstance().level;var snapshot=BoilerSurfaceSnapshots.capture(world);
        return geometry(be,world,snapshot)!=null && withinDistance(bounds(be,world,snapshot),camera,getViewDistance());
    }
    /** 到真实汽域最近点的有限距离，不能按远端Controller中心或无限距离裁剪。 */
    static boolean withinDistance(AABB bounds,Vec3 camera,int range) {
        double dx=Math.max(Math.max(bounds.minX-camera.x,camera.x-bounds.maxX),0);
        double dy=Math.max(Math.max(bounds.minY-camera.y,camera.y-bounds.maxY),0);
        double dz=Math.max(Math.max(bounds.minZ-camera.z,camera.z-bounds.maxZ),0);
        return range>=0 && dx*dx+dy*dy+dz*dz<=(double)range*range;
    }
    @Override public void render(BoilerControllerBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int hostLight,int overlay) {
        BoilerSteamClientEvents.synchronizeWorld();var minecraft=Minecraft.getInstance();var level=minecraft.level;
        var snapshot=BoilerSurfaceSnapshots.capture(level);var sample=sample(be,level,snapshot);
        if(sample==null){invalidatePosition(be.getBlockPos());return;}
        var key=BoilerSteamVisualState.identity(BoilerSteamClientEvents.session(),sample.member());
        var mesh=BoilerSteamVisualState.CACHE.mesh(key,sample.member());if(mesh.quads().isEmpty())return;
        // 固定14片的真实采光点必须仍为已加载air；局部新障碍不借假光或结构扫描掩盖。
        for(var quad:mesh.quads())for(var p:quad.points())if(!level.hasChunkAt(p.lightingCell()) || !level.getBlockState(p.lightingCell()).isAir()) {
            BoilerSteamMaterials.invalidate(key);return;
        }
        double time=BoilerSteamVisualState.CLOCK.time(partial,minecraft.isPaused());
        var texture=BoilerSteamMaterials.texture(key,time/10,sample.ratio(),BoilerSteamVisualState.CLOCK.tick());if(texture==null)return;
        var vertex=buffers.getBuffer(RenderType.entityTranslucent(texture));
        double alpha=BoilerSteamVisualState.alpha(sample.concentration());
        for(var quad:mesh.quads())submitQuad(vertex,pose.last(),quad,alpha,time,p->LevelRenderer.getLightColor(level,p));
    }
    private static void invalidatePosition(BlockPos pos) {
        for(var key:BoilerSteamMaterials.keys())if(key.owner()==pos.asLong())BoilerSteamMaterials.invalidate(key);
    }
    /** 顶点局部位置/法线共用生产Pose；逐顶点只采气域air光，不使用宿主黑格或满亮。 */
    static void submitQuad(VertexConsumer vertex,PoseStack.Pose pose,BoilerSteamVisualState.Quad quad,double alpha,double time,ToIntFunction<BlockPos> lighting) {
        var normal=quad.normal();
        // 内部一帧10显示tick，80tick缓慢循环；竖片以每tick .012格上移，不称真实流速。
        double shift=(time*.012)%1;
        for(var p:quad.points())vertex.addVertex(pose,(float)p.x(),(float)p.y(),(float)p.z())
                .setColor(1f,1f,1f,(float)(alpha*quad.weight()))
                .setUv((float)p.u(),(float)(p.v()-(normal.getAxis()==net.minecraft.core.Direction.Axis.Y?0:shift)))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(lighting.applyAsInt(p.lightingCell()))
                .setNormal(pose,normal.getStepX(),normal.getStepY(),normal.getStepZ());
    }
}
