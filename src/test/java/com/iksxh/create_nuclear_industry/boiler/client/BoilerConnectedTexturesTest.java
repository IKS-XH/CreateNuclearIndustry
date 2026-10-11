package com.iksxh.create_nuclear_industry.boiler.client;
import com.mojang.blaze3d.platform.NativeImage;
import com.simibubi.create.foundation.block.connected.*;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.iksxh.create_nuclear_industry.boiler.client.BoilerConnectedTextureBehaviourTest.*;

/** 测试真实登记、模型gather作用域及两条quad消费路径，不复制原生索引算法。 */
class BoilerConnectedTexturesTest {
    @AfterEach void clearPublication(){BoilerSurfaceSnapshots.publish(null,BoilerSurfaceSnapshot.empty());}
    @Test void registrationKeepsBothSideEntriesAndExactlyEightBlockKeys(){
        var registration=new BoilerConnectedTextures.Registration();var calls=new ArrayList<String>();var models=new HashMap<ResourceLocation,BakedModel>();
        java.util.function.BiFunction<ResourceLocation,ResourceLocation,CTSpriteShiftEntry> shifts=(a,b)->{
            calls.add(a+" -> "+b);return new CTSpriteShiftEntry(b.getPath().endsWith("window_frame")?AllCTTypes.OMNIDIRECTIONAL:AllCTTypes.RECTANGLE);
        };
        java.util.function.BiConsumer<ResourceLocation,com.tterrag.registrate.util.nullness.NonNullFunction<BakedModel,? extends BakedModel>> register=(key,factory)->assertNull(models.put(key,factory.apply(new SixFaceModel())));
        registration.initialize(shifts,register);registration.initialize(shifts,register);
        assertEquals(9,calls.size());assertEquals(8,models.size());
        assertEquals(Set.of(id("high_pressure_boiler_casing"),id("high_pressure_boiler_window"),id("high_pressure_boiler_controller"),id("high_pressure_boiler_water_port"),id("high_pressure_boiler_steam_port"),id("high_pressure_boiler_hot_coolant_port"),id("high_pressure_boiler_cold_coolant_port"),id("boiler_safety_valve")),models.keySet());
        assertTrue(calls.contains(id("block/high_pressure_boiler/casing_side")+" -> "+id("block/boiler_ct/side")));
        assertTrue(calls.contains(id("block/high_pressure_boiler/casing_side")+" -> "+id("block/boiler_ct/partition_side")));
        assertNotSame(registration.shiftForSprite(id("block/high_pressure_boiler/casing_side"),false),registration.shiftForSprite(id("block/high_pressure_boiler/casing_side"),true));
        assertNull(registration.shiftForSprite(id("block/high_pressure_boiler/valve_body"),false));
        assertSame(AllCTTypes.OMNIDIRECTIONAL,BoilerConnectedTextures.createShift(id("block/high_pressure_boiler/window_frame"),id("block/boiler_ct/window_frame")).getType());
        assertSame(AllCTTypes.RECTANGLE,BoilerConnectedTextures.createShift(id("block/high_pressure_boiler/casing_side"),id("block/boiler_ct/side")).getType());
    }
    @Test void captureIsOnceAndNestedFailureRestoresOuterWorld(){
        var outer=readableLevel(pos->Blocks.AIR.defaultBlockState());var inner=readableLevel(pos->Blocks.AIR.defaultBlockState());
        var p=new BlockPos(12,23,30);var old=new BoilerSurfaceSnapshot(Map.of(p,member(p,Direction.NORTH,CASING)));
        BoilerSurfaceSnapshots.publish(outer,old);
        BoilerConnectedTextures.withCapturedSnapshot(outer,()->{
            BoilerSurfaceSnapshots.publish(inner,BoilerSurfaceSnapshot.empty());assertSame(old,BoilerConnectedTextures.currentSnapshot(outer));
            assertSame(BoilerSurfaceSnapshot.empty(),BoilerConnectedTextures.currentSnapshot(inner));
            assertThrows(IllegalStateException.class,()->BoilerConnectedTextures.withCapturedSnapshot(inner,()->{throw new IllegalStateException("测试嵌套退出");}));
            assertSame(old,BoilerConnectedTextures.currentSnapshot(outer));return null;
        });
        assertSame(BoilerSurfaceSnapshot.empty(),BoilerConnectedTextures.currentSnapshot(outer));
    }
    @Test void actualGatherFiltersBothInterfaceDirectionsAndNeverReadsWorldDuringQuads(){
        var window=registeredState("high_pressure_boiler_window");var p=new BlockPos(12,23,30);var q=p.east();
        var states=Map.of(p,window,q,window);var level=readableLevel(pos->states.getOrDefault(pos,Blocks.AIR.defaultBlockState()));
        BoilerSurfaceSnapshots.publish(level,new BoilerSurfaceSnapshot(Map.of(p,member(p,Direction.NORTH,WINDOW),q,member(q,Direction.NORTH,WINDOW))));
        var original=new SixFaceModel();var model=BoilerConnectedTextures.wrapModel(original,new BoilerConnectedTextureBehaviour((sprite,partition)->null));
        ModelData data=model.getModelData(level,p,window,ModelData.EMPTY),other=model.getModelData(level,q,window,ModelData.EMPTY);
        BoilerSurfaceSnapshots.publish(null,BoilerSurfaceSnapshot.empty());level.reader=pos->{throw new AssertionError("quad不得读取世界");};
        assertTrue(model.getQuads(window,Direction.EAST,RandomSource.create(),data,null).isEmpty());
        assertTrue(model.getQuads(window,Direction.WEST,RandomSource.create(),other,null).isEmpty());
        assertEquals(1,model.getQuads(window,Direction.NORTH,RandomSource.create(),data,null).size());
        assertEquals(1,model.getQuads(window,Direction.SOUTH,RandomSource.create(),data,null).size());
        assertEquals(5,model.getQuads(window,null,RandomSource.create(),data,null).size());assertEquals(6,original.quads.size());
        assertEquals(6,model.getQuads(window,null,RandomSource.create(),ModelData.EMPTY,null).size());
    }
    @Test void partitionShiftUsesGatheredModelDataAfterPublicationRevocation(){
        var casing=registeredState("high_pressure_boiler_casing");var p=new BlockPos(12,23,30);var q=p.above();
        var level=readableLevel(pos->casing);BoilerSurfaceSnapshots.publish(level,new BoilerSurfaceSnapshot(Map.of(p,member(p,Direction.NORTH,CASING),q,member(q,Direction.NORTH,CASING))));
        var side=new CTSpriteShiftEntry(AllCTTypes.RECTANGLE);var partition=new CTSpriteShiftEntry(AllCTTypes.RECTANGLE);
        var behaviour=new BoilerConnectedTextureBehaviour((sprite,isPartition)->sprite.equals(id("block/high_pressure_boiler/casing_side"))?(isPartition?partition:side):null);
        try(var contents=new SpriteContents(id("block/high_pressure_boiler/casing_side"),new FrameSize(16,16),new NativeImage(16,16,false),ResourceMetadata.EMPTY)){
            var sprite=new TestSprite(contents);var seen=new ArrayList<CTSpriteShiftEntry>();
            var original=new SixFaceModel(){@Override public List<BakedQuad> getQuads(BlockState s,Direction f,RandomSource r){seen.add(behaviour.getShift(s,Direction.NORTH,sprite));return List.of();}};
            var model=BoilerConnectedTextures.wrapModel(original,behaviour);var atPartition=model.getModelData(level,p,casing,ModelData.EMPTY);var above=model.getModelData(level,q,casing,ModelData.EMPTY);
            BoilerSurfaceSnapshots.publish(null,BoilerSurfaceSnapshot.empty());level.reader=pos->{throw new AssertionError("不可重查隔层");};
            model.getQuads(casing,Direction.NORTH,RandomSource.create(),atPartition,null);assertSame(partition,seen.removeFirst());
            model.getQuads(casing,Direction.NORTH,RandomSource.create(),above,null);assertSame(side,seen.removeFirst());
            assertSame(side,behaviour.getShift(casing,Direction.NORTH,sprite));
            assertNull(behaviour.getShift(casing,Direction.NORTH,null));
        }
    }
    @Test void actualGatherFailureRestoresScopeAndUnknownPublicationClearsOldMasks(){
        var window=registeredState("high_pressure_boiler_window");var p=new BlockPos(12,23,30);var q=p.east();
        var level=readableLevel(pos->window);var snapshot=new BoilerSurfaceSnapshot(Map.of(p,member(p,Direction.NORTH,WINDOW),q,member(q,Direction.NORTH,WINDOW)));
        BoilerSurfaceSnapshots.publish(level,snapshot);
        var behaviour=new BoilerConnectedTextureBehaviour((sprite,partition)->null);
        var model=BoilerConnectedTextures.wrapModel(new SixFaceModel(),behaviour);
        var old=model.getModelData(level,p,window,ModelData.EMPTY);
        level.reader=pos->{throw new IllegalStateException("模型构建失败");};
        assertThrows(IllegalStateException.class,()->model.getModelData(level,p,window,old));
        assertSame(BoilerSurfaceSnapshot.empty(),BoilerConnectedTextures.currentSnapshot(level));
        level.reader=pos->window;BoilerSurfaceSnapshots.publish(null,BoilerSurfaceSnapshot.empty());
        var fallback=model.getModelData(level,p,window,old);
        assertEquals(6,model.getQuads(window,null,RandomSource.create(),fallback,null).size());
    }
    /** 最小原模型保留共享列表；CT真实包装执行所有过滤。 */
    static class SixFaceModel implements BakedModel{
        final List<BakedQuad> quads=Arrays.stream(Direction.values()).map(f->new BakedQuad(new int[32],-1,f,null,true)).toList();
        @Override public List<BakedQuad> getQuads(BlockState s,Direction f,RandomSource r){return f==null?quads:quads.stream().filter(q->q.getDirection()==f).toList();}
        @Override public boolean useAmbientOcclusion(){return false;}
        @Override public boolean isGui3d(){return true;}
        @Override public boolean usesBlockLight(){return true;}
        @Override public boolean isCustomRenderer(){return false;}
        @Override public TextureAtlasSprite getParticleIcon(){return null;}
        @Override public ItemOverrides getOverrides(){return ItemOverrides.EMPTY;}
    }
    static class TestSprite extends TextureAtlasSprite{
        TestSprite(SpriteContents c){super(ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png"),c,16,16,0,0);}
    }
}
