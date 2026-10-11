package com.iksxh.create_nuclear_industry.boiler.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import static com.iksxh.create_nuclear_industry.boiler.client.BoilerSteamVisualState.Identity;

/**
 * 仅客户端汽雾材质：32个SVG导出帧复制到不可变像素，单炉混成一个16×16动态纹理。
 * 创建/上传/释放均在客户端线程；prepare解码只复制CPU数据，资源错误撤销整批雾材质。
 */
public final class BoilerSteamMaterials extends SimplePreparableReloadListener<BoilerSteamMaterials.Frames> {
    static final int SLOTS=64;
    private static final Logger LOGGER=LogUtils.getLogger();
    private static Frames frames;
    private static final DynamicTexture[] textures=new DynamicTexture[SLOTS];
    private static final Set<Identity> invalid=new HashSet<>();
    private static final Pool POOL=new Pool(SLOTS,new Backend() {
        public void create(int slot) {
            textures[slot]=new DynamicTexture(16,16,false);
            Minecraft.getInstance().getTextureManager().register(id(slot),textures[slot]);
        }
        public void update(int slot,int[] pixels) {
            NativeImage image=textures[slot].getPixels();
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)image.setPixelRGBA(x,y,pixels[y*16+x]);
            textures[slot].upload();
        }
        public void release(int slot) {
            // TextureManager.release同时关闭DynamicTexture及其NativeImage；不能重复close像素。
            Minecraft.getInstance().getTextureManager().release(id(slot));textures[slot]=null;
        }
    });
    static ResourceLocation id(int slot) {
        return ResourceLocation.fromNamespaceAndPath("create_nuclear_industry","dynamic/boiler_steam/slot_"+slot);
    }
    /** 四帧域保持私有副本；ABGR四通道插值包括透明遮罩，不把纹理透明度当库存。 */
    protected static final class Frames {
        private final int[][] normalStill,normalFlow,scStill,scFlow;
        Frames(int[][] normalStill,int[][] normalFlow,int[][] scStill,int[][] scFlow) {
            this.normalStill=copy(normalStill);this.normalFlow=copy(normalFlow);this.scStill=copy(scStill);this.scFlow=copy(scFlow);
        }
        private static int[][] copy(int[][] source) {
            if(source==null || source.length!=8 || Arrays.stream(source).anyMatch(a->a==null || a.length!=256))throw new IllegalArgumentException("汽雾必须为8帧16×16");
            return Arrays.stream(source).map(int[]::clone).toArray(int[][]::new);
        }
        /** phase单位为帧，双域固定权重表达翻卷，ratio才来自两种真实库存占比。 */
        void mix(double phase,double ratio,int[] pixels) {
            if(!Double.isFinite(phase))phase=0;
            double f=((phase%8)+8)%8;int a=(int)f,b=(a+1)%8;double t=f-a;
            for(int i=0;i<256;i++) {
                int normal=lerp(lerp(normalStill[a][i],normalStill[b][i],t),lerp(normalFlow[a][i],normalFlow[b][i],t),.65);
                int sc=lerp(lerp(scStill[a][i],scStill[b][i],t),lerp(scFlow[a][i],scFlow[b][i],t),.65);
                pixels[i]=lerp(normal,sc,ratio);
            }
        }
    }
    static int lerp(int a,int b,double t) {
        // 原生entityTranslucent按straight-alpha输出再乘SRC_ALPHA；透明黑不能直接稀释可见RGB。
        // 先按两端覆盖量插值RGB，再反除合计alpha；帧、雾纹域和库存配色共用此入口。
        double amount=BoilerSteamVisualState.clamp(t);
        double wa=((a>>>24)&255)*(1-amount),wb=((b>>>24)&255)*amount,alpha=wa+wb;
        int storedAlpha=(int)Math.round(alpha);
        if(storedAlpha==0)return 0;
        int value=storedAlpha<<24;
        for(int shift=0;shift<24;shift+=8)value|=(int)Math.round((((a>>>shift)&255)*wa+((b>>>shift)&255)*wb)/alpha)<<shift;
        return value;
    }
    @Override protected Frames prepare(ResourceManager resources,ProfilerFiller profiler) {
        try {return new Frames(read(resources,"normal","still"),read(resources,"normal","flow"),read(resources,"supercritical","still"),read(resources,"supercritical","flow"));}
        catch(IOException | IllegalArgumentException exception) {
            LOGGER.warn("锅炉汽雾32帧重载失败，撤下内部材质：{}",exception.toString(),exception);return null;
        }
    }
    private static int[][] read(ResourceManager resources,String type,String movement) throws IOException {
        int[][] result=new int[8][256];
        for(int frame=0;frame<8;frame++) {
            var path=ResourceLocation.fromNamespaceAndPath("create_nuclear_industry","textures/boiler_steam/"+type+"/"+movement+"_"+frame+".png");
            try(InputStream stream=resources.open(path);NativeImage image=NativeImage.read(stream)) {
                if(image.getWidth()!=16 || image.getHeight()!=16)throw new IOException("汽雾内部帧必须16×16："+path);
                for(int y=0;y<16;y++)for(int x=0;x<16;x++)result[frame][y*16+x]=image.getPixelRGBA(x,y);
            }
        }
        return result;
    }
    @Override protected void apply(Frames prepared,ResourceManager resources,ProfilerFiller profiler) {
        flushAndClear();frames=prepared;BoilerSteamVisualState.clear();
    }
    /** 先提交仅本批固定层的旧顶点再释放，不能结束其它Mod或反应堆批次。 */
    static void flushAndClear() {
        BoilerSteamRenderBuffers.flush(Minecraft.getInstance().renderBuffers().bufferSource()::endBatch);
        POOL.clear();invalid.clear();
    }
    static void invalidate(Identity key){invalid.add(key);}
    static Set<Identity> keys(){return POOL.keys();}
    /** tick边界按同次可靠L1清理；不在单owner渲染中释放正在提交的纹理。 */
    static void boundary(long tick,Set<Identity> valid) {
        Set<Identity> retained=new HashSet<>(valid);retained.removeAll(invalid);
        POOL.boundary(tick,retained);invalid.clear();
    }
    /** 满槽安全无雾而不抢占别炉；调用必须早于该炉任何顶点提交。 */
    static ResourceLocation texture(Identity key,double phase,double ratio,long tick) {
        if(frames==null || invalid.contains(key))return null;
        int slot=POOL.draw(key,tick,p->frames.mix(phase,ratio,p));return slot<0?null:id(slot);
    }
    interface Backend {void create(int slot);void update(int slot,int[] pixels);void release(int slot);}
    /** 有界材质池，draw不驱逐/释放，只有明确安全边界处理撤销和20显示tick失见。 */
    static final class Pool {
        private static final class Entry {
            final int slot;final int[] pixels=new int[256];long last;
            Entry(int slot,long tick){this.slot=slot;last=tick;}
        }
        private final int capacity;
        private final Backend backend;
        private final Map<Identity,Entry> owners=new HashMap<>();
        private final BitSet used=new BitSet();
        Pool(int capacity,Backend backend) {
            if(capacity<1 || capacity>SLOTS)throw new IllegalArgumentException("汽雾纹理槽上限64");this.capacity=capacity;this.backend=backend;
        }
        int draw(Identity key,long tick,Consumer<int[]> writer) {
            var entry=owners.get(key);
            if(entry==null) {
                int slot=used.nextClearBit(0);if(slot>=capacity)return -1;
                backend.create(slot);used.set(slot);entry=new Entry(slot,tick);owners.put(key,entry);
            }
            entry.last=tick;writer.accept(entry.pixels);backend.update(entry.slot,entry.pixels);return entry.slot;
        }
        Set<Identity> keys(){return Set.copyOf(owners.keySet());}
        void boundary(long tick,Set<Identity> valid) {
            var iterator=owners.entrySet().iterator();
            while(iterator.hasNext()) {
                var e=iterator.next();
                if(!valid.contains(e.getKey()) || tick-e.getValue().last>20) {
                    backend.release(e.getValue().slot);used.clear(e.getValue().slot);iterator.remove();
                }
            }
        }
        void clear(){for(var e:owners.values())backend.release(e.slot);owners.clear();used.clear();}
        int size(){return owners.size();}
    }
}
