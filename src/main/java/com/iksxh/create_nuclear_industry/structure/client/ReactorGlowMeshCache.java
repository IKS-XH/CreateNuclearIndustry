package com.iksxh.create_nuclear_industry.structure.client;

import com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.*;
import java.util.function.*;
import static com.iksxh.create_nuclear_industry.structure.client.ReactorAnimationVisualState.*;

/**
 * 仅客户端线程使用的整列镂空辉光缓存；每根燃料列固定九根四侧面细管。
 * 不持有世界、BE或运行描述符；身份包含会话、维度、owner、generation和geometry。
 * 镂空层使用全亮采光与不透明深度写入；热量只调RGB，不参与透明排序。
 */
final class ReactorGlowMeshCache {
    private static final Cache CACHE=new Cache(8,16L*1024*1024);
    private static Template template;
    private ReactorGlowMeshCache() {}

    /** 调用者已验证本地成员；热量只更新列亮度，不失效几何。无辉光时不取得渲染缓冲。 */
    static Entry prepare(Identity key,ReactorRuntimeDescriptor owner,Level level) {
        BlockPos reference=null;
        for(var column:owner.columns())if(column instanceof ReactorRuntimeDescriptor.FuelColumn fuel
                && glow(fuel.fuelUsable(),fuel.fissionHeatHuPerTick())>0 && !fuel.bodyPositions().isEmpty()) {
            reference=fuel.bodyPositions().getFirst();break;
        }
        if(reference==null)return null;
        if(template==null) {
            var state=level.getBlockState(reference);
            template=Template.capture(v->CachedBuffers.partial(ReactorAnimationModels.GLOW_LOW,state).disableDiffuse()
                    .color(255,255,255,255).light(LightTexture.FULL_BRIGHT).overlay(OverlayTexture.NO_OVERLAY)
                    .renderInto(new PoseStack(),v));
        }
        Entry entry=CACHE.acquire(key,()->cells(owner),template,CLOCK.tick());
        if(entry.alpha.length!=owner.columns().size())entry.alpha=new int[owner.columns().size()];
        for(int i=0;i<entry.alpha.length;i++)entry.alpha[i]=owner.columns().get(i) instanceof ReactorRuntimeDescriptor.FuelColumn fuel
                ?(int)Math.round(glow(fuel.fuelUsable(),fuel.fissionHeatHuPerTick())*255):0;
        return entry;
    }
    /** 每列五整数：相对owner的首格XYZ、运行样本列索引、格数；扫描契约保证同列连续竖直。 */
    private static int[] cells(ReactorRuntimeDescriptor owner) {
        int count=0;
        for(var column:owner.columns())if(column instanceof ReactorRuntimeDescriptor.FuelColumn fuel && !fuel.bodyPositions().isEmpty())count++;
        int[] cells=new int[Math.multiplyExact(count,5)];int index=0;
        for(int i=0;i<owner.columns().size();i++)if(owner.columns().get(i) instanceof ReactorRuntimeDescriptor.FuelColumn fuel && !fuel.bodyPositions().isEmpty()) {
            var first=fuel.bodyPositions().getFirst();
            cells[index++]=first.getX()-owner.ownerPos().getX();cells[index++]=first.getY()-owner.ownerPos().getY();
            cells[index++]=first.getZ()-owner.ownerPos().getZ();cells[index++]=i;cells[index++]=fuel.bodyPositions().size();
        }
        return cells;
    }
    static void invalidatePosition(long owner) { CACHE.remove(k->k.owner()==owner); }
    /** 完整快照是唯一身份依据；不使用心跳或燃料库存作为静态几何键。 */
    static void boundary(ReactorRuntimeSnapshot snapshot) {
        CACHE.retain(k->snapshot.findOwner(BlockPos.of(k.owner())).map(owner->identity(owner).equals(k)).orElse(false),CLOCK.tick());
    }
    static void clear() { CACHE.clear(); }
    /** 模型烘焙完成后的资源重载门；下一次绘制重新捕获atlas UV，旧模板不跨重载使用。 */
    static void reload() { clear();template=null; }

    /** 8个owner与16MiB展开位置双重上限；超预算模型按同一顶点算法流式提交，不丢面。 */
    static final class Cache {
        private final int capacity;private final long budget;
        private final LinkedHashMap<Identity,Entry> entries=new LinkedHashMap<>(16,.75f,true);
        private long bytes;
        Cache(int capacity,long budget) {
            if(capacity<1 || budget<0)throw new IllegalArgumentException("缓存上限非法");
            this.capacity=capacity;this.budget=budget;
        }
        Entry acquire(Identity key,Supplier<int[]> geometry,Template template,long tick) {
            Entry entry=entries.get(key);
            if(entry!=null) {entry.last=tick;return entry;}
            int[] cells=geometry.get();
            long cost=(long)(cells.length/5)*template.vertexCount()*3*Float.BYTES;
            while(entries.size()>=capacity) {
                var first=entries.entrySet().iterator();bytes-=first.next().getValue().bytes();first.remove();
            }
            // 多座大堆同时可见时，预算不足的新条目走流式路径，不能为争抢位置缓存逐帧驱逐并分配大数组。
            boolean expanded=cost<=budget-bytes && cost/Float.BYTES<=Integer.MAX_VALUE;
            entry=new Entry(cells,template,expanded,tick);entries.put(key,entry);bytes+=entry.bytes();return entry;
        }
        void retain(Predicate<Identity> valid,long tick) {
            var iterator=entries.entrySet().iterator();while(iterator.hasNext()) {
                var item=iterator.next();if(!valid.test(item.getKey()) || tick-item.getValue().last>20) {
                    bytes-=item.getValue().bytes();iterator.remove();
                }
            }
        }
        void remove(Predicate<Identity> invalid) {
            var iterator=entries.entrySet().iterator();while(iterator.hasNext()) {var item=iterator.next();if(invalid.test(item.getKey())) {bytes-=item.getValue().bytes();iterator.remove();}}
        }
        int size() { return entries.size(); }
        long positionBytes() { return bytes; }
        void clear() { entries.clear();bytes=0; }
    }

    /** owner局部整列范围与可选展开位置；pose变化重算，亮度变化不动位置。 */
    static final class Entry {
        private final int[] cells;private final Template template;
        private final float[] positions,normals;
        private final Matrix4f previous=new Matrix4f(),cellPose=new Matrix4f();
        private final Matrix3f previousNormal=new Matrix3f();
        private final Vector4f position=new Vector4f();private final Vector3f normal=new Vector3f();
        private boolean initialized;
        private long last;long poseBuilds;
        private int[] alpha=new int[0];
        Entry(int[] cells,Template template,boolean expanded,long tick) {
            this.cells=cells;this.template=template;last=tick;
            positions=expanded?new float[cells.length/5*template.vertexCount()*3]:null;
            normals=new float[template.vertexCount()*3];
        }
        long bytes() { return positions==null?0:(long)positions.length*Float.BYTES; }
        void draw(PoseStack.Pose pose,VertexConsumer consumer) { draw(pose,alpha,consumer); }
        void draw(PoseStack.Pose pose,int[] alpha,VertexConsumer consumer) {
            boolean changed=!initialized || !previous.equals(pose.pose()) || !previousNormal.equals(pose.normal());
            if(changed) {
                previous.set(pose.pose());previousNormal.set(pose.normal());initialized=true;poseBuilds++;
                for(int i=0;i<template.vertexCount();i++) {
                    int source=i*12,target=i*3;
                    normal.set(template.vertices[source+9],template.vertices[source+10],template.vertices[source+11]).mul(previousNormal);
                    normals[target]=normal.x;normals[target+1]=normal.y;normals[target+2]=normal.z;
                }
                if(positions!=null)for(int cell=0;cell<cells.length;cell+=5)transform(cell,positions,cell/5*template.vertexCount()*3);
            }
            for(int cell=0;cell<cells.length;cell+=5) {
                int a=alpha[cells[cell+3]];if(a<=0)continue;
                int[] colors=template.colors(a);
                if(positions==null)cellPose.set(previous).translate(cells[cell],cells[cell+1],cells[cell+2]);
                for(int i=0;i<template.vertexCount();i++) {
                    int source=i*12,n=i*3;
                    float x,y,z;
                    if(positions!=null) {int p=cell/5*template.vertexCount()*3+n;x=positions[p];y=positions[p+1];z=positions[p+2];}
                    else {position.set(template.vertices[source],columnY(cell,template.vertices[source+1]),template.vertices[source+2],1).mul(cellPose);x=position.x;y=position.y;z=position.z;}
                    // 完整顶点入口命中原生BufferBuilder的BLOCK快速路径；其他VertexConsumer仍按原接口分派。
                    consumer.addVertex(x,y,z,colors[i],template.vertices[source+3],template.vertices[source+4],
                            OverlayTexture.NO_OVERLAY,LightTexture.FULL_BRIGHT,normals[n],normals[n+1],normals[n+2]);
                }
            }
        }
        /** 模型Y为0..1，整列映射到首格1/8至末格7/8；仅有水平法线，不受竖直拉伸影响。 */
        private float columnY(int cell,float y) { return .125f+y*(cells[cell+4]-.25f); }
        private void transform(int cell,float[] target,int offset) {
            cellPose.set(previous).translate(cells[cell],cells[cell+1],cells[cell+2]);
            for(int i=0;i<template.vertexCount();i++) {
                int source=i*12;position.set(template.vertices[source],columnY(cell,template.vertices[source+1]),template.vertices[source+2],1).mul(cellPose);
                target[offset++]=position.x;target[offset++]=position.y;target[offset++]=position.z;
            }
        }
    }

    /** 一次SBB捕获的不可变模板；每档热量亮度缓存RGB，顶点alpha恒为255以使用镂空深度。 */
    static final class Template {
        private final float[] vertices;private final int[][] colors=new int[256][];
        private Template(float[] vertices) {this.vertices=vertices;}
        static Template capture(Consumer<VertexConsumer> source) {
            var capture=new Capture();source.accept(capture);
            if(capture.size%4!=0)throw new IllegalArgumentException("辉光必须为完整QUADS");
            return new Template(Arrays.copyOf(capture.data,capture.size*12));
        }
        int vertexCount() {return vertices.length/12;}
        private int[] colors(int alpha) {
            int[] result=colors[alpha];if(result!=null)return result;
            result=new int[vertexCount()];float amount=alpha/255f;
            for(int i=0;i<result.length;i++) {int p=i*12;
                result[i]=0xff000000|((int)(vertices[p+5]*amount*255f)<<16)
                        |((int)(vertices[p+6]*amount*255f)<<8)|(int)(vertices[p+7]*amount*255f);
            }
            return colors[alpha]=result;
        }
    }
    /** 仅在烘焙模板首次需要时使用；保留SBB浮点颜色，避免捕获时提前量化亮度。 */
    private static final class Capture implements VertexConsumer {
        private float[] data=new float[12*512];private int size,p;
        public VertexConsumer addVertex(float x,float y,float z) {
            if((size+1)*12>data.length)data=Arrays.copyOf(data,data.length*2);
            p=size++*12;data[p]=x;data[p+1]=y;data[p+2]=z;return this;
        }
        public VertexConsumer setColor(int r,int g,int b,int a) {return setColor(r/255f,g/255f,b/255f,a/255f);}
        @Override public VertexConsumer setColor(float r,float g,float b,float a) {data[p+5]=r;data[p+6]=g;data[p+7]=b;data[p+8]=a;return this;}
        public VertexConsumer setUv(float u,float v) {data[p+3]=u;data[p+4]=v;return this;}
        public VertexConsumer setUv1(int u,int v) {return this;}
        public VertexConsumer setUv2(int u,int v) {return this;}
        public VertexConsumer setNormal(float x,float y,float z) {data[p+9]=x;data[p+10]=y;data[p+11]=z;return this;}
    }
}
