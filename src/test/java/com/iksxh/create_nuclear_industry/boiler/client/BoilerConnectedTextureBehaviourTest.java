package com.iksxh.create_nuclear_industry.boiler.client;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.*;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.*;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import static org.junit.jupiter.api.Assertions.*;

/** 运行生产归属判断与Create原生上下文；不重复共享同步生命周期。 */
class BoilerConnectedTextureBehaviourTest {
    static final String CASING="create_nuclear_industry:high_pressure_boiler_casing", WINDOW="create_nuclear_industry:high_pressure_boiler_window";
    static final BlockPos MIN=new BlockPos(10,20,30), MAX=new BlockPos(17,27,39), OWNER=new BlockPos(10,21,31);
    static final UUID GENERATION=UUID.fromString("00000000-0000-0000-0000-000000000123");
    @AfterEach void clearPublication(){ BoilerSurfaceSnapshots.publish(null,BoilerSurfaceSnapshot.empty()); }
    static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("create_nuclear_industry",path);}
    static BoilerSurfaceSnapshot.Member member(BlockPos pos,Direction face,String material){
        return new BoilerSurfaceSnapshot.Member("minecraft:overworld",OWNER,GENERATION,3,MIN,MAX,23,material,pos,Set.of(face));
    }
    @Test void differentMaterialsConnectOnEveryRealPlaneIncludingIndependentExchanger(){
        var faces=List.of(Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP);
        var positions=List.of(new BlockPos(12,22,30),new BlockPos(12,22,39),new BlockPos(10,22,32),new BlockPos(17,22,32),new BlockPos(12,20,32),new BlockPos(12,27,32));
        for(int i=0;i<faces.size();i++){
            var f=faces.get(i);var a=positions.get(i);var b=f.getAxis()==Direction.Axis.X?a.south():a.east();
            assertTrue(BoilerConnectedTextureBehaviour.canConnect(member(a,f,CASING),member(b,f,"create_nuclear_industry:nuclear_heat_exchanger"),CASING,"create_nuclear_industry:nuclear_heat_exchanger",f));
        }
        assertFalse(BoilerConnectedTextures.supportsBlock("create_nuclear_industry:boiler_heat_exchange_section"));
    }
    @Test void everyIdentityComponentAndLocalReplacementRejects(){
        var a=member(new BlockPos(12,22,30),Direction.NORTH,CASING);var b=member(a.pos().east(),Direction.NORTH,CASING);
        var variants=List.of(
          new BoilerSurfaceSnapshot.Member("minecraft:the_nether",OWNER,GENERATION,3,MIN,MAX,23,CASING,b.pos(),b.outwardFaces()),
          new BoilerSurfaceSnapshot.Member(b.dimension(),OWNER.east(),GENERATION,3,MIN,MAX,23,CASING,b.pos(),b.outwardFaces()),
          new BoilerSurfaceSnapshot.Member(b.dimension(),OWNER,UUID.randomUUID(),3,MIN,MAX,23,CASING,b.pos(),b.outwardFaces()),
          new BoilerSurfaceSnapshot.Member(b.dimension(),OWNER,GENERATION,4,MIN,MAX,23,CASING,b.pos(),b.outwardFaces()),
          new BoilerSurfaceSnapshot.Member(b.dimension(),OWNER,GENERATION,3,MIN,MAX.east(),23,CASING,b.pos(),b.outwardFaces()),
          new BoilerSurfaceSnapshot.Member(b.dimension(),OWNER,GENERATION,3,MIN,MAX,24,CASING,b.pos(),b.outwardFaces()));
        for(var v:variants)assertFalse(BoilerConnectedTextureBehaviour.canConnect(a,v,CASING,CASING,Direction.NORTH));
        assertFalse(BoilerConnectedTextureBehaviour.canConnect(a,b,CASING,"minecraft:air",Direction.NORTH));
        assertFalse(BoilerConnectedTextureBehaviour.canConnect(a,null,CASING,CASING,Direction.NORTH));
        assertFalse(BoilerConnectedTextureBehaviour.canConnect(a,member(a.pos().east().south(),Direction.NORTH,CASING),CASING,CASING,Direction.NORTH));
        assertFalse(BoilerConnectedTextureBehaviour.canConnect(a,b,CASING,CASING,Direction.SOUTH));
    }
    @Test void windowDomainAllowsOnlyWindowDiagonalsAndRejectsDistantCells(){
        var p=new BlockPos(12,22,30);var a=member(p,Direction.NORTH,WINDOW);
        assertTrue(BoilerConnectedTextureBehaviour.canConnect(a,member(p.east().above(),Direction.NORTH,WINDOW),WINDOW,WINDOW,Direction.NORTH));
        assertFalse(BoilerConnectedTextureBehaviour.canConnect(a,member(p.east(2),Direction.NORTH,WINDOW),WINDOW,WINDOW,Direction.NORTH));
        assertFalse(BoilerConnectedTextureBehaviour.canConnect(a,member(p.east(),Direction.NORTH,CASING),WINDOW,CASING,Direction.NORTH));
        assertTrue(BoilerConnectedTextureBehaviour.canConnect(member(p,Direction.NORTH,CASING),member(p.east(),Direction.NORTH,WINDOW),CASING,WINDOW,Direction.NORTH));
    }
    @Test void nativeTwoByTwoAndEllContextsKeepConcaveCornerClosed(){
        var window=registeredState("high_pressure_boiler_window");var center=new BlockPos(13,24,30);
        var behaviour=new BoilerConnectedTextureBehaviour((sprite,partition)->null);
        for(boolean full:new boolean[]{true,false}){
            var positions=new ArrayList<BlockPos>(List.of(center,center.west(),center.below()));
            if(full)positions.add(center.west().below());
            var members=new HashMap<BlockPos,BoilerSurfaceSnapshot.Member>();for(var pos:positions)members.put(pos,member(pos,Direction.NORTH,WINDOW));
            var level=readableLevel(pos->members.containsKey(pos)?window:Blocks.AIR.defaultBlockState());
            BoilerSurfaceSnapshots.publish(level,new BoilerSurfaceSnapshot(members));
            BoilerConnectedTextures.withCapturedSnapshot(level,()->{
                var context=behaviour.buildContext(level,center,window,Direction.NORTH,AllCTTypes.OMNIDIRECTIONAL.getContextRequirement());
                assertTrue(context.down);assertTrue(context.right);assertEquals(full,context.bottomRight);
                assertSame(AllCTTypes.OMNIDIRECTIONAL,behaviour.getDataType(level,center,window,Direction.NORTH));return null;
            });
        }
    }
    @Test void actualPartitionYAndLocalIdGateMaterialMasks(){
        var casing=registeredState("high_pressure_boiler_casing");var p=new BlockPos(12,23,30);var q=p.above();
        var level=readableLevel(pos->casing);var behaviour=new BoilerConnectedTextureBehaviour((sprite,partition)->null);
        BoilerSurfaceSnapshots.publish(level,new BoilerSurfaceSnapshot(Map.of(p,member(p,Direction.NORTH,CASING),q,member(q,Direction.NORTH,CASING))));
        BoilerConnectedTextures.withCapturedSnapshot(level,()->{
            assertEquals(1<<Direction.NORTH.get3DDataValue(),behaviour.partitionFaces(level,p,casing));
            assertEquals(0,behaviour.partitionFaces(level,q,casing));
            assertEquals(0,behaviour.partitionFaces(level,p,Blocks.STONE.defaultBlockState()));
            assertNull(behaviour.getDataType(level,p,casing,Direction.UP));return null;
        });
        assertEquals(0,behaviour.partitionFaces(level,p,casing));
    }
    static BlockState registeredState(String name) {
        var block = BuiltInRegistries.BLOCK.get(id(name));
        assertEquals(id(name), BuiltInRegistries.BLOCK.getKey(block), "测试使用实际已注册的合法锅炉材质");
        return block.defaultBlockState();
    }

    /** 替代区块读取的真实ClientLevel子类；不创建世界，只由测试给出有限局部方块。 */
    static final class FixtureLevel extends ClientLevel {
        java.util.function.Function<BlockPos, BlockState> reader;

        private FixtureLevel() {
            super(null, null, null, null, 0, 0, null, null, false, 0L);
            throw new AssertionError("只允许Unsafe分配测试外部世界边界");
        }

        @Override public BlockState getBlockState(BlockPos pos) { return reader.apply(pos); }
    }

    static FixtureLevel readableLevel(java.util.function.Function<BlockPos, BlockState> reader) {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            FixtureLevel level = (FixtureLevel) ((Unsafe) field.get(null)).allocateInstance(FixtureLevel.class);
            level.reader = reader;
            return level;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("无法构造局部读取用ClientLevel", exception);
        }
    }

}
