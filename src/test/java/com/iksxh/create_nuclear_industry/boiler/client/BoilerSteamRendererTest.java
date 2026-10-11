package com.iksxh.create_nuclear_industry.boiler.client;

import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.junit.jupiter.api.Test;
import java.util.*;
import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerSurfaceDescriptor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import sun.misc.Unsafe;
import static org.junit.jupiter.api.Assertions.*;

/** 先覆盖实际材质核心和批次入口，最终BER入口在真实容量ABI交接后补齐。 */
class BoilerSteamRendererTest {
    @Test void actualPixelMixerUsesAllFourFrameDomainsAndLoopsSmoothly() {
        var normalStill=frames(0xff102030);var normalFlow=frames(0xff506070);
        var scStill=frames(0xff8090a0);var scFlow=frames(0xffc0d0e0);
        var frames=new BoilerSteamMaterials.Frames(normalStill,normalFlow,scStill,scFlow);
        int[] normal=new int[256],sc=new int[256],mixed=new int[256],loop=new int[256];
        frames.mix(0,0,normal);frames.mix(0,1,sc);frames.mix(0,.5,mixed);frames.mix(8,0,loop);
        assertArrayEquals(normal,loop);
        assertEquals(0xff3a4a5a,normal[0]);
        assertEquals(0xffaabaca,sc[0]);
        assertEquals(0xff728292,mixed[0]);
        normalStill[0][0]=0;frames.mix(0,0,loop);assertArrayEquals(normal,loop);
        int[][] moving=frames(0);moving[7][0]=0xffffffff;
        var animated=new BoilerSteamMaterials.Frames(moving,moving,moving,moving);
        animated.mix(7.5,.2,loop);assertEquals(0x80ffffff,loop[0]);
        animated.mix(-.5,.2,mixed);assertArrayEquals(loop,mixed);
        // straight-alpha目标颜色必须保留白色；两端不同alpha时RGB按可见覆盖量加权。
        assertEquals(0x80ffffff,BoilerSteamMaterials.lerp(0xffffffff,0, .5));
        assertEquals(0x60aa0055,BoilerSteamMaterials.lerp(0x80ff0000,0x400000ff,.5));
        assertEquals(0,BoilerSteamMaterials.lerp(0x00ffffff,0x000000ff,.5));
    }
    @Test void poolNeverStealsSubmittedTextureAndReleasesOnlyAtSafeBoundary() {
        var log=new ArrayList<String>();var pool=new BoilerSteamMaterials.Pool(1,new BoilerSteamMaterials.Backend() {
            public void create(int slot){log.add("create");}
            public void update(int slot,int[] pixels){log.add("update");}
            public void release(int slot){log.add("release");}
        });
        var a=BoilerSteamVisualState.identity(1,BoilerSteamVisualStateTest.member());
        var b=BoilerSteamVisualState.identity(2,BoilerSteamVisualStateTest.member());
        assertEquals(0,pool.draw(a,0,p->Arrays.fill(p,0)));assertEquals(-1,pool.draw(b,0,p->{}));
        assertEquals(List.of("create","update"),log);
        pool.boundary(0,Set.of());assertEquals(List.of("create","update","release"),log);
        assertEquals(0,pool.draw(b,1,p->{}));pool.clear();assertEquals(0,pool.size());
    }
    @Test void productionBatchUsesNativeTranslucentAndOnlyFinishesAtBerBoundary() {
        var finished=new ArrayList<RenderType>();
        BoilerSteamRenderBuffers.finish(RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS,finished::add);assertTrue(finished.isEmpty());
        BoilerSteamRenderBuffers.finish(RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES,finished::add);
        assertEquals(BoilerSteamMaterials.SLOTS,finished.size());
        assertSame(RenderType.entityTranslucent(BoilerSteamMaterials.id(0)),finished.get(0));
    }
    @Test void productionSamplerReadsRealClientPacketCapacityAndIndependentStocksAfterIdentityGate() {
        var fixture=fixture();var be=fixture.be;var world=fixture.world;var snapshot=fixture.snapshot;
        var sample=BoilerSteamRenderer.sample(be,world,snapshot);
        assertNotNull(sample);assertEquals(.5,sample.concentration());assertEquals(.75,sample.ratio());
        // 模拟默认客户端容量不同，生产仍只读已完成readClient的实际9000mB，而非DEFAULT容量。
        assertNotEquals(be.ledger().steamCapacity(),be.syncedSteamCapacityMb());
        be.readClient(packet(fixture.descriptor,0,0,9000),RegistryAccess.EMPTY);
        assertNull(BoilerSteamRenderer.sample(be,world,snapshot));
        be.readClient(packet(fixture.descriptor,1000,0,0),RegistryAccess.EMPTY);
        assertNull(BoilerSteamRenderer.sample(be,world,snapshot));
        be.readClient(packet(fixture.descriptor,0,9000,9000),RegistryAccess.EMPTY);
        assertEquals(1,BoilerSteamRenderer.sample(be,world,snapshot).ratio());
        assertEquals(1,BoilerSteamRenderer.sample(be,world,snapshot).concentration());
    }
    @Test void actualSamplerRejectsRevokedOldGeometryWrongObjectWorldAndLocalBlock() {
        var f=fixture();
        assertNull(BoilerSteamRenderer.sample(f.be,f.world,BoilerSurfaceSnapshot.empty()));
        assertNull(BoilerSteamRenderer.sample(f.be,world(),f.snapshot));
        f.world.slot=null;assertNull(BoilerSteamRenderer.sample(f.be,f.world,f.snapshot));f.world.slot=f.be;
        f.world.state=Blocks.STONE.defaultBlockState();assertNull(BoilerSteamRenderer.sample(f.be,f.world,f.snapshot));
        f.world.state=BoilerConnectedTextureBehaviourTest.registeredState("high_pressure_boiler_controller");
        var d=f.descriptor;
        var changed=new BoilerSurfaceDescriptor(d.dimension(),d.ownerPos(),d.ownerGeneration(),d.revision()+1,true,d.min(),d.maxInclusive(),d.partitionY(),d.members());
        f.be.readClient(packet(changed,1000,3500,9000),RegistryAccess.EMPTY);
        assertNull(BoilerSteamRenderer.sample(f.be,f.world,f.snapshot));
        f.be.readClient(packet(d,1000,3500,9000),RegistryAccess.EMPTY);
        f.world.loaded=false;assertNull(BoilerSteamRenderer.sample(f.be,f.world,f.snapshot));
    }
    @Test void productionBoundsAndDistanceCoverFarSteamRoomButRemainFinite() {
        var f=fixture();var bounds=BoilerSteamRenderer.bounds(f.be,f.world,f.snapshot);
        assertEquals(45,bounds.minY);assertEquals(50,bounds.maxY);assertEquals(11,bounds.minX);assertEquals(20,bounds.maxX);
        assertTrue(new BoilerSteamRenderer(null).shouldRenderOffScreen(f.be));
        assertTrue(BoilerSteamRenderer.withinDistance(bounds,new Vec3(19,49,19),0));
        assertFalse(BoilerSteamRenderer.withinDistance(bounds,new Vec3(1000,49,19),64));
        assertEquals(new net.minecraft.world.phys.AABB(f.be.getBlockPos()),BoilerSteamRenderer.bounds(f.be,f.world,BoilerSurfaceSnapshot.empty()));
    }
    @Test void actualSubmissionKeepsLocalPoseAirLightAlphaAndSlowUpwardUv() throws Exception {
        var mesh=BoilerSteamVisualState.buildMesh(BoilerSteamVisualStateTest.member());var quad=mesh.quads().getFirst();
        var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(100,-20,50);
        var vertices=new VertexRecord();var lightCells=new ArrayList<BlockPos>();int airLight=net.minecraft.client.renderer.LightTexture.pack(0,12);
        BoilerSteamRenderer.submitQuad(vertices,pose.last(),quad,.18,10,p->{lightCells.add(p);return airLight;});
        assertEquals(4,vertices.entries.size());assertEquals(quad.points().stream().map(BoilerSteamVisualState.Point::lightingCell).toList(),lightCells);
        for(var vertex:vertices.entries){assertEquals(airLight,vertex.light);assertEquals(32,vertex.a);assertEquals(-1,vertex.nx);assertEquals(0,vertex.ny);}
        var first=quad.points().getFirst();var written=vertices.entries.getFirst();
        assertEquals((float)first.x()+100,written.x);assertEquals((float)first.y()-20,written.y);assertEquals((float)first.z()+50,written.z);
        assertEquals((float)(first.v()-.12),written.v);
        // 离线证据直接消费真实生产Mesh/Frames/alpha；没有GPU、玻璃排序或气室实测光的验收含义。
        var root=java.nio.file.Path.of("src/main/resources/assets/create_nuclear_industry/textures/boiler_steam");
        var frames=new BoilerSteamMaterials.Frames(readFrames(root,"normal","still"),readFrames(root,"normal","flow"),
                readFrames(root,"supercritical","still"),readFrames(root,"supercritical","flow"));
        int[] pixels=new int[256];frames.mix(3.25,.48,pixels);
        var evidence=new LinkedHashMap<String,Object>();evidence.put("kind","生产CPU材质/正交雾片采样；无GPU/实测光证明");
        evidence.put("phase",3.25);evidence.put("supercriticalRatio",.48);evidence.put("pixelsABGR",pixels);
        evidence.put("lowDensity",.22);evidence.put("highDensity",.82);
        evidence.put("lowAlpha",BoilerSteamVisualState.alpha(.22));evidence.put("highAlpha",BoilerSteamVisualState.alpha(.82));
        evidence.put("sideWeights",mesh.quads().stream().filter(q->q.normal().getAxis()==net.minecraft.core.Direction.Axis.Z).map(BoilerSteamVisualState.Quad::weight).toList());
        evidence.put("quadCount",mesh.quads().size());evidence.put("steamBounds",List.of(mesh.bounds().minX,mesh.bounds().minY,mesh.bounds().minZ,mesh.bounds().maxX,mesh.bounds().maxY,mesh.bounds().maxZ));
        var output=java.nio.file.Path.of("build/reports/art/ART-BOILER-06R1/consumer/production-offline-sample.json");java.nio.file.Files.createDirectories(output.getParent());
        java.nio.file.Files.writeString(output,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(evidence));
    }
    /** 只记录原生VertexConsumer默认Pose方法最终提交属性，不复制生产顶点/UV算法。 */
    static final class VertexRecord implements com.mojang.blaze3d.vertex.VertexConsumer {
        static final class Entry{float x,y,z,u,v,nx,ny,nz;int a,light;}
        final List<Entry> entries=new ArrayList<>();Entry current;
        public VertexRecord addVertex(float x,float y,float z){current=new Entry();current.x=x;current.y=y;current.z=z;entries.add(current);return this;}
        public VertexRecord setColor(int r,int g,int b,int a){current.a=a;return this;}
        public VertexRecord setUv(float u,float v){current.u=u;current.v=v;return this;}
        public VertexRecord setUv1(int u,int v){return this;}
        public VertexRecord setUv2(int u,int v){current.light=u|(v<<16);return this;}
        public VertexRecord setNormal(float x,float y,float z){current.nx=x;current.ny=y;current.nz=z;return this;}
    }
    private static int[][] readFrames(java.nio.file.Path root,String type,String flow) throws Exception {
        int[][] result=new int[8][256];
        for(int f=0;f<8;f++)try(var input=java.nio.file.Files.newInputStream(root.resolve(type).resolve(flow+"_"+f+".png"));var image=com.mojang.blaze3d.platform.NativeImage.read(input)) {
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)result[f][y*16+x]=image.getPixelRGBA(x,y);
        }
        return result;
    }
    /** 通过真实Controller.readClient完成库存/容量/描述读包；只替代有限世界槽位，不反射控制器状态。 */
    private static Fixture fixture() {
        var m=BoilerSteamVisualStateTest.member();var members=new ArrayList<BoilerSurfaceDescriptor.SurfaceMember>();
        for(var p:BlockPos.betweenClosed(m.min(),m.maxInclusive())) {
            var faces=BoilerSurfaceDescriptor.outwardFaces(p,m.min(),m.maxInclusive());if(faces.isEmpty())continue;
            members.add(new BoilerSurfaceDescriptor.SurfaceMember(p,p.equals(m.ownerPos())?m.expectedBlockId():"create_nuclear_industry:high_pressure_boiler_casing",faces));
        }
        var descriptor=new BoilerSurfaceDescriptor(m.dimension(),m.ownerPos(),m.ownerGeneration(),m.revision(),true,m.min(),m.maxInclusive(),m.partitionY(),members);
        var world=world();world.state=BoilerConnectedTextureBehaviourTest.registeredState("high_pressure_boiler_controller");
        var be=new BoilerControllerBlockEntity(m.ownerPos(),world.state);be.addBehaviours(new ArrayList<>());be.setLevel(world);world.slot=be;
        be.readClient(packet(descriptor,1125,3375,9000),RegistryAccess.EMPTY);
        return new Fixture(be,world,descriptor,new BoilerSurfaceSnapshot(Map.of(m.ownerPos(),m)));
    }
    private static CompoundTag packet(BoilerSurfaceDescriptor descriptor,int normal,int sc,int capacity) {
        var tag=new CompoundTag();var ledger=new CompoundTag();ledger.putInt("NormalSteam",normal);ledger.putInt("SupercriticalSteam",sc);
        ledger.putInt("WaterCells",9);ledger.putInt("SteamCells",9);tag.put("Boiler",ledger);
        tag.put("BoilerSurface",descriptor.encode());tag.putInt("SteamCapacity",capacity);tag.putString("Status","redstone");return tag;
    }
    private record Fixture(BoilerControllerBlockEntity be,FixtureWorld world,BoilerSurfaceDescriptor descriptor,BoilerSurfaceSnapshot snapshot) {}
    /** 外部世界边界的有限fixture，不构造存档、网络或光照引擎。 */
    static final class FixtureWorld extends ClientLevel {
        BlockEntity slot;BlockState state;boolean loaded;
        private FixtureWorld(){super(null,null,null,null,0,0,null,null,false,0);throw new AssertionError();}
        @Override public ResourceKey<Level> dimension(){return Level.OVERWORLD;}
        @Override public boolean hasChunk(int x,int z){return loaded;}
        @Override public boolean hasChunkAt(BlockPos pos){return loaded;}
        @Override public BlockEntity getBlockEntity(BlockPos pos){return slot!=null && pos.equals(slot.getBlockPos())?slot:null;}
        @Override public BlockState getBlockState(BlockPos pos){return slot!=null && pos.equals(slot.getBlockPos())?state:Blocks.AIR.defaultBlockState();}
    }
    private static FixtureWorld world() {
        try {var field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
            var world=(FixtureWorld)((Unsafe)field.get(null)).allocateInstance(FixtureWorld.class);world.loaded=true;return world;
        } catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static int[][] frames(int pixel){int[][] data=new int[8][256];for(int[] row:data)Arrays.fill(row,pixel);return data;}
}
