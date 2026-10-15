package com.iksxh.create_nuclear_industry.structure.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.LightTexture;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** 镂空辉光的整列范围、颜色、数量和缓存预算；不修改任何服务端样本。 */
class ReactorGlowMeshCacheTest {
    static ReactorAnimationVisualState.Identity key(long owner) {
        return new ReactorAnimationVisualState.Identity(1,"minecraft:overworld",owner,new UUID(0,1),1);
    }
    static ReactorGlowMeshCache.Template template() {
        return ReactorGlowMeshCache.Template.capture(v->{
            for(int i=0;i<36;i++)for(float[] point:new float[][]{{.2f,0,.2f},{.3f,0,.2f},{.3f,1,.2f},{.2f,1,.2f}})
                v.addVertex(point[0],point[1],point[2]).setColor(255,255,255,255).setUv(.5f,.5f).setLight(LightTexture.FULL_BRIGHT).setNormal(0,0,-1);
        });
    }
    @Test void wholeColumnUsesOnly36QuadsAndFitsFirstAndLastBodyForAnyHeight() {
        for(int height:new int[]{1,3,14})for(boolean expanded:new boolean[]{false,true}) {
            var mesh=template();var cache=new ReactorGlowMeshCache.Cache(8,expanded?1<<20:0);
            var entry=cache.acquire(key(1),()->new int[]{-8,0,7,0,height},mesh,0);
            var sink=new Sink();entry.draw(new PoseStack().last(),new int[]{128},sink);
            assertEquals(144,sink.count,"整列不能再乘以体格高度");assertEquals(.125f,sink.minY,1e-6);assertEquals(height-.125f,sink.maxY,1e-6);
            assertEquals(255,sink.a);assertEquals(LightTexture.FULL_BRIGHT,sink.light);
        }
    }
    @Test void powerChangesRgbWithoutAlphaBlendingAndZeroHeatEmitsNoVertices() {
        var cache=new ReactorGlowMeshCache.Cache(8,1<<20);var entry=cache.acquire(key(1),()->new int[]{0,0,0,0,14},template(),0);
        var sink=new Sink();var pose=new PoseStack();entry.draw(pose.last(),new int[]{32},sink);int dim=sink.r;long builds=entry.poseBuilds;
        entry.draw(pose.last(),new int[]{166},sink);assertTrue(sink.r>dim);assertEquals(255,sink.a);assertEquals(builds,entry.poseBuilds,"功率不重建位置");
        int count=sink.count;entry.draw(pose.last(),new int[]{0},sink);assertEquals(count,sink.count,"停堆/无料不得提交黑色不透明遮挡物");
    }
    @Test void cameraChangesRebuildWhileStableViewAndBudgetOverflowRemainAllocationFree() {
        var mesh=template();var cache=new ReactorGlowMeshCache.Cache(8,144*3*4);var entry=cache.acquire(key(1),()->new int[]{0,0,0,0,14},mesh,0);
        var streamed=cache.acquire(key(2),()->new int[]{0,0,0,0,14},mesh,0);assertSame(entry,cache.acquire(key(1),()->{throw new AssertionError();},mesh,1));
        assertTrue(cache.positionBytes()<=144*3*4);var pose=new PoseStack();var a=new Sink();var b=new Sink();int[] alpha={166};
        for(int i=0;i<100;i++){entry.draw(pose.last(),alpha,a);streamed.draw(pose.last(),alpha,b);}
        assertEquals(a.maxY,b.maxY);assertEquals(a.r,b.r);assertEquals(a.count,b.count);
        long builds=entry.poseBuilds;float before=a.x;pose.translate(2,0,0);entry.draw(pose.last(),alpha,a);assertEquals(builds+1,entry.poseBuilds);assertEquals(before+2,a.x,1e-6);
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long id=Thread.currentThread().threadId(),start=bean.getThreadAllocatedBytes(id);
        for(int i=0;i<100;i++){entry.draw(pose.last(),alpha,a);streamed.draw(pose.last(),alpha,b);}
        assertTrue(bean.getThreadAllocatedBytes(id)-start<4096);
        cache.retain(k->true,22);assertEquals(0,cache.size());assertEquals(0,cache.positionBytes());
    }
    @Test void lowResourceIsNineFourSidedOpenTubesAndKeepsOriginalResources() throws Exception {
        var root=Path.of("src/main/resources/assets/create_nuclear_industry/models/block/reactor_animation");
        var lines=Files.readAllLines(root.resolve("mesh/fuel_rod_glow_low.obj"));
        assertEquals(36,lines.stream().filter(s->s.startsWith("f ")).count());assertEquals(9,lines.stream().filter(s->s.startsWith("g ")).distinct().count());
        for(String line:lines)if(line.startsWith("vn "))assertEquals(0,Double.parseDouble(line.split(" ")[2]),0);
        assertTrue(Files.readString(root.resolve("fuel_rod_glow_low.json")).contains("fuel_rod_glow_low.obj"));
        assertTrue(lines.contains("mtllib fuel_rod_glow.mtl"));
    }
    static final class Sink implements VertexConsumer {
        int count,r,a,light;float x,minY=Float.POSITIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY;
        public VertexConsumer addVertex(float x,float y,float z){this.x=x;count++;minY=Math.min(minY,y);maxY=Math.max(maxY,y);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){this.r=r;this.a=a;return this;}
        public VertexConsumer setUv(float u,float v){return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){light=u|(v<<16);return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
}
