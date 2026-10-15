package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.structure.ReactorDimensions;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 用真实计算器验证稳定反馈的分配预算；数值始终与冻结的 Java 基准严格比较。 */
class ReactorFissionFeedbackCacheTest {
    static final ReactorSimulationParameters PARAMETERS = ReactorSimulationParameters.defaults();

    static ReactorSnapshot large(double depth) {
        var fuels = new TreeMap<CoreColumnPosition, FuelColumnState>();
        var rods = new TreeMap<CoreColumnPosition, ControlRodColumnState>();
        for (int x=0;x<14;x++) for (int z=0;z<14;z++) {
            var pos = new CoreColumnPosition(x,z);
            if ((x+z)%5==0) rods.put(pos,new ControlRodColumnState(1,depth,depth,false,0));
            else fuels.put(pos,new FuelColumnState(FuelAssemblyState.installed(216000,0),1,0));
        }
        return new ReactorSnapshot(fuels,rods,0,0,0,false,java.util.Map.of(),false,false,new ReactorDimensions(16,16,16));
    }

    @Test void unchangedFeedbackAvoidsIterativeAllocationAndKeepsExactResults() {
        var s=large(.4);
        var expected=ReactorFissionReference.calculate(s,PARAMETERS);
        for(int i=0;i<100;i++) assertEquals(expected,ReactorFissionCalculator.calculate(s,PARAMETERS));
        var bean=(ThreadMXBean)ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        long id=Thread.currentThread().threadId();
        long before=bean.getThreadAllocatedBytes(id);
        for(int i=0;i<128;i++) assertEquals(expected,ReactorFissionCalculator.calculate(s,PARAMETERS));
        long bytes=bean.getThreadAllocatedBytes(id)-before;
        System.out.println("stable-feedback allocation bytes/call="+bytes/128);
        assertTrue(bytes/128<131072,"稳定反馈不应每次重建全部迭代集合；实际 bytes/call="+bytes/128);
    }

    private static Object cache() throws Exception {
        var field=ReactorFissionCalculator.class.getDeclaredField("CACHE");field.setAccessible(true);
        return ((ThreadLocal<?>)field.get(null)).get();
    }
    private static long count(String name) throws Exception {
        var cache=cache();var field=cache.getClass().getDeclaredField(name);field.setAccessible(true);return field.getLong(cache);
    }
    private static void clearCache() throws Exception {
        var field=ReactorFissionCalculator.class.getDeclaredField("CACHE");field.setAccessible(true);((ThreadLocal<?>)field.get(null)).remove();
    }

    @Test void sameFeedbackReusesSolveWhileFreshDamageHeightAndFuelAreSettled() throws Exception {
        clearCache();var s=large(.4);ReactorFissionCalculator.calculate(s,PARAMETERS);
        var fuels=new TreeMap<>(s.fuelColumns());var pos=fuels.firstKey();
        fuels.put(pos,new FuelColumnState(FuelAssemblyState.installed(216000,100),.5,20,.75,0));
        var changed=s.withColumns(fuels,s.controlRodColumns()).withDimensions(new ReactorDimensions(16,5,16));
        assertEquals(ReactorFissionReference.calculate(changed,PARAMETERS),ReactorFissionCalculator.calculate(changed,PARAMETERS));
        assertEquals(1,count("solves"));assertEquals(1,count("hits"));
        assertNotEquals(ReactorFissionCalculator.calculate(s,PARAMETERS),ReactorFissionCalculator.calculate(changed,PARAMETERS));
    }

    @Test void actualRodDepthUsableFuelTopologyAndEveryFeedbackParameterInvalidate() throws Exception {
        clearCache();var a=large(.4);ReactorFissionCalculator.calculate(a,PARAMETERS);
        var b=large(.4000000000000001);assertEquals(ReactorFissionReference.calculate(b,PARAMETERS),ReactorFissionCalculator.calculate(b,PARAMETERS));
        assertEquals(2,count("solves"));
        for(int i=0;i<5;i++) {
            var p=ReactorFissionDifferentialTest.change(PARAMETERS,i,i==3?.2:3);
            long before=count("solves");assertEquals(ReactorFissionReference.calculate(a,p),ReactorFissionCalculator.calculate(a,p));
            assertEquals(before+1,count("solves"));
        }
        var fuels=new TreeMap<>(a.fuelColumns());fuels.put(fuels.firstKey(),FuelColumnState.empty());
        b=a.withColumns(fuels,a.controlRodColumns());assertEquals(ReactorFissionReference.calculate(b,PARAMETERS),ReactorFissionCalculator.calculate(b,PARAMETERS));
        long builds=count("topologyBuilds");fuels.remove(fuels.firstKey());b=a.withColumns(fuels,a.controlRodColumns());
        assertEquals(ReactorFissionReference.calculate(b,PARAMETERS),ReactorFissionCalculator.calculate(b,PARAMETERS));assertEquals(builds+1,count("topologyBuilds"));
    }

    @Test void nonConvergedFeedbackPreservesOriginalLastIterationAndIsNotReused() throws Exception {
        clearCache();
        var f=new TreeMap<CoreColumnPosition,FuelColumnState>();var r=new TreeMap<CoreColumnPosition,ControlRodColumnState>();
        for(int z=0;z<2;z++){f.put(new CoreColumnPosition(1,z),new FuelColumnState(FuelAssemblyState.installed(100,0),1,0));r.put(new CoreColumnPosition(0,z),new ControlRodColumnState(1,0,0,false,0));}
        var s=new ReactorSnapshot(f,r,0,0,0,false);
        var p=ReactorFissionDifferentialTest.change(ReactorFissionDifferentialTest.change(PARAMETERS,3,.029575248012909942),4,2);
        for(int i=0;i<2;i++) assertEquals(ReactorFissionReference.calculate(s,p),ReactorFissionCalculator.calculate(s,p));
        assertEquals(2,count("solves"));assertEquals(0,count("hits"));
    }

    @Test void alternatingReactorsAndTopologyEvictionStayIsolatedAndBounded() throws Exception {
        clearCache();var a=large(.4);var b=large(.6);
        for(int i=0;i<10;i++){var s=i%2==0?a:b;assertEquals(ReactorFissionReference.calculate(s,PARAMETERS),ReactorFissionCalculator.calculate(s,PARAMETERS));}
        assertEquals(2,count("solves"));assertEquals(8,count("hits"));
        for(int i=0;i<24;i++){var s=ReactorFissionDifferentialTest.randomState(new java.util.Random(200+i),5+i%12,5,5+i%12);assertEquals(ReactorFissionReference.calculate(s,PARAMETERS),ReactorFissionCalculator.calculate(s,PARAMETERS));}
        Object cache=cache();var field=cache.getClass().getDeclaredField("layouts");field.setAccessible(true);
        var layouts=(java.util.List<?>)field.get(cache);assertEquals(8,layouts.size());
        for(Object topology:layouts){var saved=topology.getClass().getDeclaredField("saved");saved.setAccessible(true);assertTrue(((java.util.List<?>)saved.get(topology)).size()<=4);}
        assertEquals(ReactorFissionReference.calculate(a,PARAMETERS),ReactorFissionCalculator.calculate(a,PARAMETERS));
    }
}
