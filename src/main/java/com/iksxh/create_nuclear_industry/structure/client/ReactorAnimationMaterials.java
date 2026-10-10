package com.iksxh.create_nuclear_industry.structure.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import static com.iksxh.create_nuclear_industry.structure.client.ReactorAnimationVisualState.Identity;

/**
 * 客户端材质：prepare关闭原生解码缓冲并复制为不可变帧；render只更新有界槽。
 * 256个固定ID不会随心跳增长；回收仅在安全客户端tick或重载批次已提交后执行。
 */
public final class ReactorAnimationMaterials extends SimplePreparableReloadListener<ReactorAnimationMaterials.Frames> {
    private static final Logger LOGGER=LogUtils.getLogger();
    private static final String PREFIX = "create_nuclear_industry";
    private static Frames frames;
    private static final Set<Identity> invalid = new HashSet<>();
    private static final DynamicTexture[] textures = new DynamicTexture[256];
    private static final Pool POOL = new Pool(256, new Backend() {
        public void create(int slot) {
            textures[slot] = new DynamicTexture(16, 16, false);
            Minecraft.getInstance().getTextureManager().register(id(slot), textures[slot]);
        }
        public void update(int slot, int[] pixels) {
            NativeImage image = textures[slot].getPixels();
            for (int y=0;y<16;y++) for (int x=0;x<16;x++) image.setPixelRGBA(x,y,pixels[y*16+x]);
            textures[slot].upload();
        }
        public void release(int slot) {
            // TextureManager.release的safeClose同时关闭DynamicTexture持有的NativeImage。
            Minecraft.getInstance().getTextureManager().release(id(slot)); textures[slot] = null;
        }
    });
    private static ResourceLocation id(int slot) { return ResourceLocation.fromNamespaceAndPath(PREFIX,"dynamic/reactor_coolant/slot_"+slot); }

    /** 私有副本不暴露可变像素数组；只在本类内混色。 */
    protected static final class Frames {
        private final int[][] cold, hot;
        Frames(int[][] cold, int[][] hot) { this.cold = copy(cold); this.hot = copy(hot); }
        private static int[][] copy(int[][] source) { return Arrays.stream(source).map(int[]::clone).toArray(int[][]::new); }
    }
    @Override protected Frames prepare(ResourceManager resources, ProfilerFiller profiler) {
        try { return new Frames(read(resources,"coolant_cold"),read(resources,"coolant_hot")); }
        catch (IOException | IllegalArgumentException exception) {
            // prepare每次失败重载仅诊断一次；apply仍撤下旧材质，render不逐帧刷日志。
            LOGGER.warn("反应堆冷却液重载失败：{}:textures/block/reactor_animation/coolant_{cold,hot}.png；{}",PREFIX,exception.toString(),exception);
            return null;
        }
    }
    private static int[][] read(ResourceManager resources, String name) throws IOException {
        var path = ResourceLocation.fromNamespaceAndPath(PREFIX,"textures/block/reactor_animation/"+name+".png");
        try (InputStream stream=resources.open(path); NativeImage image=NativeImage.read(stream)) {
            if (image.getWidth()!=16 || image.getHeight()!=128) throw new IOException("反应堆帧表必须为16x128: "+path);
            int[][] result=new int[8][256];
            for(int f=0;f<8;f++) for(int y=0;y<16;y++) for(int x=0;x<16;x++) result[f][y*16+x]=image.getPixelRGBA(x,f*16+y);
            return result;
        }
    }
    @Override protected void apply(Frames prepared, ResourceManager resources, ProfilerFiller profiler) {
        // 应用在客户端线程；提交仍排队的透明顶点后再关闭旧GPU内容，坏资源也撤下旧材质。
        flushAndClear(); frames=prepared; ReactorAnimationVisualState.clear();
    }
    public static void flushAndClear() {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch(); POOL.clear(); invalid.clear();
    }
    public static void invalidate(Identity owner) { invalid.add(owner); }
    /** 只在下一安全tick按同次快照核对数值身份，不持有Level或BE。 */
    public static void boundary(ReactorRuntimeSnapshot snapshot, long tick) {
        Set<Identity> valid=new HashSet<>();
        for(Identity key:POOL.keys()) {
            var owner=snapshot.findOwner(net.minecraft.core.BlockPos.of(key.owner())).orElse(null);
            if(owner!=null && ReactorAnimationVisualState.identity(owner).equals(key) && !invalid.contains(key)) valid.add(key);
        }
        POOL.boundary(tick,valid); invalid.clear();
    }
    /** 调用者必须在提交本owner的第一个顶点之前取得纹理，满池安全返回null。 */
    public static ResourceLocation texture(Identity owner,double phase,double ratio,long tick) {
        if(frames==null || invalid.contains(owner)) return null;
        int slot=POOL.draw(owner,tick,pixels->mixInto(frames.cold,frames.hot,phase,ratio,pixels));
        return slot<0?null:id(slot);
    }
    /** NativeImage按ABGR排列，四通道独立插值，不能当ARGB拆分。 */
    public static int lerp(int a,int b,double t) {
        double amount=ReactorAnimationVisualState.clamp(t);int value=0;
        for(int shift=0;shift<32;shift+=8) value|=(int)Math.round(((a>>>shift)&255)*(1-amount)+((b>>>shift)&255)*amount)<<shift;
        return value;
    }
    public static void mixInto(int[][] cold,int[][] hot,double phase,double ratio,int[] pixels) {
        double frame=((phase%8)+8)%8;int current=(int)frame,next=(current+1)%8;double fraction=frame-current;
        for(int i=0;i<256;i++) pixels[i]=lerp(lerp(cold[current][i],cold[next][i],fraction),lerp(hot[current][i],hot[next][i],fraction),ratio);
    }
    public interface Backend { void create(int slot); void update(int slot,int[] pixels); void release(int slot); }
    /** 后端负责原生资源；核心决定固定槽归属。draw不驱逐别堆，同帧不会盗用已提交材质。 */
    public static final class Pool {
        private static final class Entry { final int slot; final int[] pixels=new int[256];long last;Entry(int slot,long tick){this.slot=slot;last=tick;}int slot(){return slot;}long last(){return last;} }
        private final int capacity; private final Backend backend;
        private final Map<Identity,Entry> owners=new HashMap<>();private final BitSet used=new BitSet();
        public Pool(int capacity,Backend backend) { if(capacity<1 || capacity>256) throw new IllegalArgumentException("纹理槽上限256");this.capacity=capacity;this.backend=backend; }
        private Entry reserve(Identity key,long tick) {
            Entry entry=owners.get(key);
            if(entry==null) { int slot=used.nextClearBit(0);if(slot>=capacity)return null;backend.create(slot);used.set(slot);entry=new Entry(slot,tick);owners.put(key,entry); }
            entry.last=tick;return entry;
        }
        /** 生产混色直接写槽自己的256像素缓冲，不为每堆每帧new像素数组。 */
        public int draw(Identity key,long tick,java.util.function.Consumer<int[]> writer) {
            Entry entry=reserve(key,tick);if(entry==null)return -1;
            writer.accept(entry.pixels);backend.update(entry.slot(),entry.pixels);return entry.slot();
        }
        public Set<Identity> keys() { return Set.copyOf(owners.keySet()); }
        public void boundary(long tick,Set<Identity> valid) {
            var iterator=owners.entrySet().iterator();
            while(iterator.hasNext()) { var entry=iterator.next();if(!valid.contains(entry.getKey()) || tick-entry.getValue().last()>20) {
                backend.release(entry.getValue().slot());used.clear(entry.getValue().slot());iterator.remove();
            } }
        }
        public void clear() { for(Entry entry:owners.values())backend.release(entry.slot());owners.clear();used.clear(); }
        public int size() { return owners.size(); }
    }
}
