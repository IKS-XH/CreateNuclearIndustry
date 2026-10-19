package com.iksxh.create_nuclear_industry.industrial;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 路径共享保持原root/source/target顺序和重复节点，不能借去重改变收费路线。 */
class IndustrialPathAllocationTest {
    private Class<?> required(String name) {
        try{return Class.forName(name);}catch(ClassNotFoundException absent){fail("尚无路径共享实现："+name);return null;}
    }
    @SuppressWarnings("unchecked")
    @Test void joinedPathPreservesOrderDuplicatesAndReadOnlyIdentity() throws Exception {
        var type=required("com.iksxh.create_nuclear_industry.industrial.IndustrialNetworkScheduler$JoinedPath");
        var constructor=type.getDeclaredConstructor(BlockPos.class,List.class,List.class);constructor.setAccessible(true);
        BlockPos root=BlockPos.ZERO,a=new BlockPos(1,0,0),b=new BlockPos(2,0,0);List<BlockPos> source=List.of(a,b),target=List.of(b,a);
        var path=(List<BlockPos>)constructor.newInstance(root,source,target);assertEquals(List.of(root,a,b,b,a),path);assertSame(source.getFirst(),path.get(1));
        assertThrows(UnsupportedOperationException.class,()->path.set(0,a));assertThrows(IndexOutOfBoundsException.class,()->path.get(5));
    }
    @SuppressWarnings("unchecked")
    @Test void parentPathMaterializesOnlyAtEndpointAndReusesItsSnapshot() throws Exception {
        var type=required("com.iksxh.create_nuclear_industry.industrial.IndustrialTopology$PathLink");
        var constructor=type.getDeclaredConstructor(BlockPos.class,type);constructor.setAccessible(true);Method method=type.getDeclaredMethod("flatten");method.setAccessible(true);
        Object first=constructor.newInstance(BlockPos.ZERO,null),last=constructor.newInstance(new BlockPos(1,0,0),first);
        var path=(List<BlockPos>)method.invoke(last);assertEquals(List.of(BlockPos.ZERO,new BlockPos(1,0,0)),path);assertSame(path,method.invoke(last));
        assertThrows(UnsupportedOperationException.class,()->path.add(BlockPos.ZERO));
    }
    @Test void nestedTransactionLeasesStayIndependent() throws Exception {
        Class<?> type=required("com.iksxh.create_nuclear_industry.industrial.IndustrialNetworkScheduler$World");
        var constructor=type.getDeclaredConstructor();constructor.setAccessible(true);Object world=constructor.newInstance();
        Method borrow=type.getDeclaredMethod("borrowPath"),release=type.getDeclaredMethod("releasePath",IndustrialFlowBudget.Path.class);borrow.setAccessible(true);release.setAccessible(true);
        var outer=(IndustrialFlowBudget.Path)borrow.invoke(world);outer.add(1,9);var inner=(IndustrialFlowBudget.Path)borrow.invoke(world);inner.add(2,7);
        assertNotSame(outer,inner);release.invoke(world,inner);assertEquals(9,outer.capacity(1));assertEquals(0,outer.capacity(2));release.invoke(world,outer);
        var next=(IndustrialFlowBudget.Path)borrow.invoke(world);assertEquals(0,next.capacity(1));release.invoke(world,next);
    }
    @SuppressWarnings("unchecked")
    @Test void routePairCacheReusesGeometryAndLeavesOldViewsStable() throws Exception {
        Class<?> type=required("com.iksxh.create_nuclear_industry.industrial.IndustrialNetworkScheduler$Routes");
        var constructor=type.getDeclaredConstructor(BlockPos.class,long.class,net.minecraft.core.Direction.class,IndustrialTopology.Side.class,IndustrialTopology.Side.class);constructor.setAccessible(true);
        var a=new IndustrialTopology.Endpoint(new BlockPos(3,0,0),net.minecraft.core.Direction.WEST,List.of(new BlockPos(1,0,0)));
        var b=new IndustrialTopology.Endpoint(new BlockPos(4,0,0),net.minecraft.core.Direction.EAST,List.of(new BlockPos(2,0,0)));
        var side=new IndustrialTopology.Side(List.of(a,b),Set.of(),Set.of(),"ready");Object routes=constructor.newInstance(BlockPos.ZERO,1L,net.minecraft.core.Direction.NORTH,side,side);
        Method path=type.getDeclaredMethod("path",IndustrialTopology.Endpoint.class,IndustrialTopology.Endpoint.class);path.setAccessible(true);
        var first=(List<BlockPos>)path.invoke(routes,a,b);assertSame(first,path.invoke(routes,a,b));var other=(List<BlockPos>)path.invoke(routes,null,a);
        assertEquals(List.of(BlockPos.ZERO,new BlockPos(1,0,0),new BlockPos(2,0,0)),first);assertEquals(List.of(BlockPos.ZERO,new BlockPos(1,0,0)),other);
        assertThrows(UnsupportedOperationException.class,()->first.clear());
    }
}
