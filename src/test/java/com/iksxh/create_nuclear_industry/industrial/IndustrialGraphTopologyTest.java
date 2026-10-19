package com.iksxh.create_nuclear_industry.industrial;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 图模型只改变搜索载体；原方向、节点封顶、端点路径及FIFO选择仍可独立差分。 */
class IndustrialGraphTopologyTest {
    private Class<?> type(String name) {
        try{return Class.forName("com.iksxh.create_nuclear_industry.industrial.IndustrialNetworkGraph$"+name);}
        catch(ClassNotFoundException absent){fail("尚无端口管网图模型");return null;}
    }
    private Object construct(String name,Class<?>[] signature,Object... args)throws Exception {
        var c=type(name).getDeclaredConstructor(signature);c.setAccessible(true);return c.newInstance(args);
    }
    private Object cell(BlockPos p,int kind,Direction.Axis axis,int tier)throws Exception {return construct("Cell",new Class[]{BlockPos.class,int.class,Direction.Axis.class,int.class},p,kind,axis,tier);}
    private Object face(BlockPos p,Direction side)throws Exception {return construct("Face",new Class[]{BlockPos.class,Direction.class},p,side);}
    private Object model(Map<BlockPos,Object> nodes,Set<Object> endpoints)throws Exception {return construct("Model",new Class[]{Map.class,Set.class},nodes,endpoints);}
    private IndustrialTopology.Side search(Object model,Map<BlockPos,Direction> pumps,int max)throws Exception {
        var m=type("Model").getDeclaredMethod("search",BlockPos.class,Direction.class,boolean.class,Map.class,int.class);m.setAccessible(true);
        return (IndustrialTopology.Side)m.invoke(model,BlockPos.ZERO,Direction.EAST,true,pumps,max);
    }
    @Test void longLoadedLineHasNo256DistanceCutoff()throws Exception {
        var nodes=new LinkedHashMap<BlockPos,Object>();for(int x=1;x<=350;x++){var p=new BlockPos(x,0,0);nodes.put(p,cell(p,0,null,0));}
        var m=model(nodes,Set.of(face(new BlockPos(351,0,0),Direction.WEST)));var side=search(m,Map.of(),4096);
        assertEquals("ready",side.failure());assertEquals(350,side.nodes().size());assertEquals(350,side.endpoints().getFirst().path().size());
    }
    @Test void straightInteriorCompressesButPortAndBranchStayCritical()throws Exception {
        var nodes=new LinkedHashMap<BlockPos,Object>();for(int x=1;x<=20;x++){var p=new BlockPos(x,0,0);nodes.put(p,cell(p,0,null,x%2));}
        var m=model(nodes,Set.of(face(new BlockPos(21,0,0),Direction.WEST),face(new BlockPos(20,1,0),Direction.DOWN)));
        var f=type("Model").getDeclaredField("groups");f.setAccessible(true);Map<?,?> groups=(Map<?,?>)f.get(m);
        assertEquals(20,groups.size());assertEquals(3,new HashSet<>(groups.values()).size());assertEquals(2,search(m,Map.of(),4096).endpoints().size());
    }
    @Test void stoppedAndReversePumpRemainDirectionalBarriers()throws Exception {
        var n=new LinkedHashMap<BlockPos,Object>();for(int x=1;x<=3;x++){var p=new BlockPos(x,0,0);n.put(p,cell(p,x==2?2:0,x==2?Direction.Axis.X:null,0));}
        var m=model(n,Set.of(face(new BlockPos(4,0,0),Direction.WEST)));var pump=new BlockPos(2,0,0);
        assertEquals("ready",search(m,Map.of(pump,Direction.EAST),4096).failure());assertTrue(search(m,Map.of(pump,Direction.WEST),4096).endpoints().isEmpty());assertTrue(search(m,Map.of(),4096).endpoints().isEmpty());
    }
    @Test void illegalValveSideDoesNotOpenAConnection()throws Exception {
        var a=new BlockPos(1,0,0);var b=new BlockPos(2,0,0);var m=model(Map.of(a,cell(a,0,null,0),b,cell(b,1,Direction.Axis.Y,0)),Set.of(face(new BlockPos(3,0,0),Direction.WEST)));
        assertTrue(search(m,Map.of(),4096).endpoints().isEmpty());
    }
    @Test void nodeSafetyCapStillStopsLargeRoutes()throws Exception {
        var n=new LinkedHashMap<BlockPos,Object>();for(int x=1;x<=20;x++){var p=new BlockPos(x,0,0);n.put(p,cell(p,0,null,0));}
        var s=search(model(n,Set.of(face(new BlockPos(21,0,0),Direction.WEST))),Map.of(),8);assertEquals("too_large",s.failure());assertTrue(s.endpoints().isEmpty());assertEquals(9,s.nodes().size());
    }
    @Test void endpointNeverActsAsBridge()throws Exception {
        var a=new BlockPos(1,0,0);var b=new BlockPos(3,0,0);var m=model(Map.of(a,cell(a,0,null,0),b,cell(b,0,null,0)),Set.of(face(new BlockPos(2,0,0),Direction.WEST),face(new BlockPos(4,0,0),Direction.WEST)));
        var s=search(m,Map.of(),4096);assertEquals(1,s.endpoints().size());assertEquals(new BlockPos(2,0,0),s.endpoints().getFirst().position());assertEquals(Set.of(a),s.nodes());
    }
    @Test void equalLengthLoopKeepsOriginalDirectionFifoChoice()throws Exception {
        var nodes=new LinkedHashMap<BlockPos,Object>();int[][] points={{1,0},{1,-1},{2,-1},{3,-1},{3,0},{1,1},{2,1},{3,1}};
        for(var p:points){var pos=new BlockPos(p[0],0,p[1]);nodes.put(pos,cell(pos,0,null,0));}
        var m=model(nodes,Set.of(face(new BlockPos(4,0,0),Direction.WEST),face(BlockPos.ZERO,Direction.EAST)));var s=search(m,Map.of(),4096);
        assertEquals(1,s.endpoints().size());assertEquals(List.of(new BlockPos(1,0,0),new BlockPos(1,0,-1),new BlockPos(2,0,-1),new BlockPos(3,0,-1),new BlockPos(3,0,0)),s.endpoints().getFirst().path());
    }
    @Test void cycleWithoutEndpointTerminatesWithoutInventingOutput()throws Exception {
        var nodes=new LinkedHashMap<BlockPos,Object>();int[][] points={{1,0},{1,-1},{2,-1},{3,-1},{3,0},{1,1},{2,1},{3,1}};
        for(var p:points){var pos=new BlockPos(p[0],0,p[1]);nodes.put(pos,cell(pos,0,null,0));}
        var s=search(model(nodes,Set.of()),Map.of(),4096);assertEquals("blocked",s.failure());assertEquals(8,s.nodes().size());assertTrue(s.endpoints().isEmpty());
    }
    @Test void sharedPhysicalGraphDoesNotBridgeAcrossTheRootPump()throws Exception {
        var a=new BlockPos(1,0,0);var b=new BlockPos(-1,0,0);var m=model(Map.of(BlockPos.ZERO,cell(BlockPos.ZERO,2,Direction.Axis.X,0),a,cell(a,0,null,0),b,cell(b,0,null,0)),Set.of(face(new BlockPos(-2,0,0),Direction.EAST)));
        var s=search(m,Map.of(BlockPos.ZERO,Direction.EAST),4096);assertEquals(Set.of(a),s.nodes());assertTrue(s.endpoints().isEmpty());
    }
}
