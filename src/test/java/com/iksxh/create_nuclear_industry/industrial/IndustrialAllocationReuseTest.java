package com.iksxh.create_nuclear_industry.industrial;

import java.lang.management.ManagementFactory;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 与原装箱算法差分，另外检查临时容器重入及稳定状态的真实线程分配量。 */
class IndustrialAllocationReuseTest {
    private static final class Original {
        long tick=Long.MIN_VALUE;final Map<Long,Integer> used=new HashMap<>();
        int reserve(long now,List<IndustrialFlowBudget.Limit> path,int requested,boolean execute) {
            if(requested<=0||path.isEmpty())return 0;var first=path.getFirst();
            if(first.capacity()<=0||now==tick&&used.getOrDefault(first.node(),0)>=first.capacity())return 0;
            Map<Long,Integer> capacities=new HashMap<>();for(var limit:path){if(limit.capacity()<=0)return 0;capacities.merge(limit.node(),limit.capacity(),Math::min);}
            int amount=requested;for(var e:capacities.entrySet())amount=Math.min(amount,Math.max(0,e.getValue()-(now==tick?used.getOrDefault(e.getKey(),0):0)));
            if(execute&&amount>0){if(now!=tick){used.clear();tick=now;}for(long node:capacities.keySet())used.merge(node,amount,Math::addExact);}return amount;
        }
    }
    @Test void randomizedChargesExactlyMatchOriginal() {
        var original=new Original();var actual=new IndustrialFlowBudget();var random=new Random(0x54414c4cL);
        for(int n=0;n<15000;n++) {
            List<IndustrialFlowBudget.Limit> path=new ArrayList<>();int length=random.nextInt(19);
            for(int i=0;i<length;i++){long node=switch(random.nextInt(30)){case 0->Long.MIN_VALUE;case 1->Long.MAX_VALUE;default->random.nextInt(13)-6;};int capacity=random.nextInt(100)==0?Integer.MAX_VALUE:random.nextInt(120)-3;path.add(new IndustrialFlowBudget.Limit(node,capacity));}
            long now=random.nextInt(9)-2;int requested=random.nextInt(100)==0?Integer.MAX_VALUE:random.nextInt(150)-3;boolean execute=random.nextBoolean();
            assertEquals(original.reserve(now,path,requested,execute),actual.reserve(now,path,requested,execute),"步骤"+n);
        }
    }
    @Test void callbackWhileReadingAPathDoesNotClobberOuterTemporaryState() {
        var b=new IndustrialFlowBudget();assertEquals(4,b.reserve(7,List.of(new IndustrialFlowBudget.Limit(9,10)),4,true));boolean[] entered={false};
        var path=new AbstractList<IndustrialFlowBudget.Limit>() {
            public int size(){return 2;}
            public IndustrialFlowBudget.Limit get(int index){if(index==1&&!entered[0]){entered[0]=true;assertEquals(3,b.reserve(7,List.of(new IndustrialFlowBudget.Limit(9,10)),3,true));}return new IndustrialFlowBudget.Limit(index+1,10);}
        };
        assertEquals(5,b.reserve(7,path,5,true));assertEquals(3,b.reserve(7,List.of(new IndustrialFlowBudget.Limit(9,10)),10,false));
        assertEquals(5,b.reserve(7,List.of(new IndustrialFlowBudget.Limit(1,10)),10,false));assertEquals(5,b.reserve(7,List.of(new IndustrialFlowBudget.Limit(2,10)),10,false));
    }
    @Test void steadyBudgetReservationAvoidsPerNodeBoxedMaps() {
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();assertTrue(bean.isThreadAllocatedMemorySupported());bean.setThreadAllocatedMemoryEnabled(true);
        var b=new IndustrialFlowBudget();var path=new ArrayList<IndustrialFlowBudget.Limit>();for(int i=0;i<16;i++)path.add(new IndustrialFlowBudget.Limit(300+i,1000));
        for(int i=0;i<15000;i++)b.reserve(1,path,7,false);long id=Thread.currentThread().threadId(),before=bean.getThreadAllocatedBytes(id);
        for(int i=0;i<25000;i++)assertEquals(7,b.reserve(1,path,7,false));double bytes=(bean.getThreadAllocatedBytes(id)-before)/25000.0;
        assertTrue(bytes<128,"每次预算仍大量分配："+bytes+" bytes/call");
    }
    @Test void preparedAndSingleNodeEntriesMatchOriginalAcrossTicks() {
        var original=new Original();var actual=new IndustrialFlowBudget();var path=new IndustrialFlowBudget.Path();var random=new Random(0x5052455041524544L);
        for(int step=0;step<10000;step++) {
            path.clear();var limits=new ArrayList<IndustrialFlowBudget.Limit>();
            for(int i=0,n=random.nextInt(14);i<n;i++){long node=random.nextInt(9)-4;int capacity=random.nextInt(150)-2;path.add(node,capacity);limits.add(new IndustrialFlowBudget.Limit(node,capacity));}
            long now=random.nextInt(7);int requested=random.nextInt(180)-2;boolean execute=random.nextBoolean();
            assertEquals(original.reserve(now,limits,requested,execute),actual.reserve(now,path,requested,execute));
            long node=random.nextInt(9)-4;int capacity=random.nextInt(150)-2;
            assertEquals(original.reserve(now,List.of(new IndustrialFlowBudget.Limit(node,capacity)),requested,execute),actual.reserveSingle(now,node,capacity,requested,execute));
        }
    }
    @Test void reusedPathPreservesMinimumCapacityAndMaximumCrossingWithoutOldState() {
        var path=new IndustrialFlowBudget.Path();path.add(1,20);path.add(2,30);path.add(1,10);
        assertEquals(10,path.capacity(1));path.cross(1,7);path.cross(1,3);assertEquals(7,path.takeCrossed(1));assertEquals(0,path.takeCrossed(1));
        path.clear();path.add(2,6);assertEquals(0,path.capacity(1));assertEquals(6,path.capacity(2));assertEquals(0,path.takeCrossed(2));
        assertEquals(6,new IndustrialFlowBudget().reserve(1,path,20,true));
    }
}
