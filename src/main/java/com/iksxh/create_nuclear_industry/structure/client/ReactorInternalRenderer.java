package com.iksxh.create_nuclear_industry.structure.client;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import java.util.function.ToIntFunction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import static com.iksxh.create_nuclear_industry.structure.client.ReactorAnimationVisualState.*;

/** 仪表owner唯一绘制内部辉光和液体显示并集；燃料包络不增容量，一次capture覆盖绘制，未知数据不保留。 */
public final class ReactorInternalRenderer implements BlockEntityRenderer<ReactorInstrumentPortBlockEntity> {
    public ReactorInternalRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public AABB getRenderBoundingBox(ReactorInstrumentPortBlockEntity instrument) {
        var level=instrument.getLevel();if(level==null)return new AABB(instrument.getBlockPos());
        var owner=ReactorRuntimeSnapshots.capture(level).findOwner(instrument.getBlockPos()).orElse(null);
        if(owner==null)return new AABB(instrument.getBlockPos());
        return new AABB(owner.origin().getX(),owner.origin().getY(),owner.origin().getZ(),
                owner.maxInclusive().getX()+1,owner.maxInclusive().getY()+1,owner.maxInclusive().getZ()+1);
    }
    @Override public void render(ReactorInstrumentPortBlockEntity instrument,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        ReactorAnimationClientEvents.synchronizeWorld();var level=instrument.getLevel();if(level==null || level!=Minecraft.getInstance().level)return;
        var snapshot=ReactorRuntimeSnapshots.capture(level);
        var owner=snapshot.findOwner(instrument.getBlockPos()).orElse(null);if(owner==null) { invalidatePosition(instrument.getBlockPos().asLong());ReactorGlowMeshCache.invalidatePosition(instrument.getBlockPos().asLong());return; }
        var key=identity(owner);
        if(!matches(level,owner.ownerPos(),"create_nuclear_industry:reactor_instrument_port")) { invalidate(key);ReactorGlowMeshCache.invalidatePosition(key.owner());return; }
        for(var column:owner.columns()) if(!matches(level,column)) { invalidate(key);ReactorGlowMeshCache.invalidatePosition(key.owner());return; }
        var base=instrument.getBlockPos();
        var glowMesh=ReactorGlowMeshCache.prepare(key,owner,level);
        if(glowMesh!=null)glowMesh.draw(pose.last(),ReactorInternalRenderBuffers.glow(buffers));
        double fill=fill(owner.coldCoolantMb(),owner.hotCoolantMb(),owner.coolantCapacityMb());if(fill<=0)return;
        var minecraft=Minecraft.getInstance();double time=CLOCK.time(partial,minecraft.isPaused());
        var mesh=mesh(key,owner,fill,time);if(mesh.isEmpty())return;
        // 更新纹理必须先于这个owner任何液体顶点；槽满只撤液，不撤燃料或棒体。
        var texture=ReactorAnimationMaterials.texture(key,phase(key,time,owner.convertedCoolantMbPerTick()),
                hotRatio(owner.coldCoolantMb(),owner.hotCoolantMb()),CLOCK.tick());if(texture==null)return;
        VertexConsumer vertex=liquidBuffer(buffers,texture);
        for(Face face:mesh) submitFace(vertex,pose.last(),face,base,p->LevelRenderer.getLightColor(level,p),owner.origin().getY(),owner.maxInclusive().getY());
    }
    /** 液体生产取缓冲入口；测试直接调用同一路径，不替换原生缓冲调度。 */
    static VertexConsumer liquidBuffer(MultiBufferSource buffers,ResourceLocation texture) {
        return buffers.getBuffer(ReactorInternalRenderBuffers.layer(texture));
    }
    /** 生产液面按缓存的同层合法空气坐标读取当前世界光；燃料包络不能采实体、floor到外壳或复用宿主光。 */
    static void submitFace(VertexConsumer vertex,PoseStack.Pose pose,Face face,BlockPos base,ToIntFunction<BlockPos> lighting,int minY,int maxY) {
        var cell=face.lightingCell();
        quad(vertex,pose,face,base.getX(),base.getY(),base.getZ(),lighting.applyAsInt(new BlockPos(cell.x(),cell.y(),cell.z())),minY,maxY);
    }
    /** 六面绕序均向外；UV以宿主局部格坐标重复，相邻格接缝连续，姿态矩阵同时变换位置与法线。 */
    private static void quad(VertexConsumer vertex,PoseStack.Pose pose,Face face,int bx,int by,int bz,int light,int minY,int maxY) {
        var c=face.cell();float x=c.x()-bx,z=c.z()-bz,low=(float)(c.y()-by+face.low()),high=(float)(c.y()-by+face.high());
        float[][] points;float nx=0,ny=0,nz=0;
        switch(face.side()) {
            case EAST -> {nx=1;points=new float[][]{{x+1,low,z+1},{x+1,low,z},{x+1,high,z},{x+1,high,z+1}};}
            case WEST -> {nx=-1;points=new float[][]{{x,low,z},{x,low,z+1},{x,high,z+1},{x,high,z}};}
            case SOUTH -> {nz=1;points=new float[][]{{x,low,z+1},{x+1,low,z+1},{x+1,high,z+1},{x,high,z+1}};}
            case NORTH -> {nz=-1;points=new float[][]{{x+1,low,z},{x,low,z},{x,high,z},{x+1,high,z}};}
            case UP -> {ny=1;points=new float[][]{{x,high,z},{x,high,z+1},{x+1,high,z+1},{x+1,high,z}};}
            default -> {ny=-1;points=new float[][]{{x,low,z+1},{x,low,z},{x+1,low,z},{x+1,low,z+1}};}
        }
        for(float[] point:points) {
            float u=ny!=0?point[0]:(nx!=0?point[2]:point[0]);float v=ny!=0?point[2]:point[1];
            // UV和切向端点维持格间连续；只把并集外露面沿自身法向内缩1/1024格，避开钢板共面。
            float epsilon=1f/1024;
            float alpha=ny!=0?.45f:(float)(.46+.10*clamp(((double)point[1]+by-minY)/Math.max(1,maxY-minY)));
            vertex.addVertex(pose,point[0]-nx*epsilon,point[1]-ny*epsilon,point[2]-nz*epsilon).setColor(1f,1f,1f,alpha)
                    .setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,nx,ny,nz);
        }
    }
}
