package com.iksxh.create_nuclear_industry.industrial;

import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 压缩边必须仍按原物理坐标共享额度；分割、合并和同tick回调不能刷新已用量。 */
class IndustrialGraphBudgetTest {
    private Object group(long[] nodes,byte[] tiers)throws Exception {
        Class<?> type;
        try{type=Class.forName(IndustrialFlowBudget.class.getName()+"$Group");}
        catch(ClassNotFoundException absent){fail("尚无共享静态管段预算");return null;}
        var c=type.getDeclaredConstructor(long[].class,byte[].class);c.setAccessible(true);return c.newInstance(nodes,tiers);
    }
    private void add(IndustrialFlowBudget.Path path,Object group,int low,int high)throws Exception {
        var m=IndustrialFlowBudget.Path.class.getDeclaredMethod("addGroup",group.getClass(),int.class,int.class);m.setAccessible(true);m.invoke(path,group,low,high);
    }
    private void flush(IndustrialFlowBudget budget)throws Exception {
        var m=IndustrialFlowBudget.class.getDeclaredMethod("flushGroups");m.setAccessible(true);m.invoke(budget);
    }
    private void retire(Object group)throws Exception {var f=group.getClass().getDeclaredField("live");f.setAccessible(true);f.setBoolean(group,false);}
    private int charge(IndustrialFlowBudget budget,long now,IndustrialFlowBudget.Path path,long key,int amount)throws Exception {
        var m=IndustrialFlowBudget.class.getDeclaredMethod("charge",long.class,IndustrialFlowBudget.Path.class,long.class,int.class);m.setAccessible(true);return (int)m.invoke(budget,now,path,key,amount);
    }
    @Test void sharedTrunkCannotBeSpentOncePerTarget()throws Exception {
        Object g=group(new long[]{1,2,3},new byte[]{0,0,0});var b=new IndustrialFlowBudget();var p=new IndustrialFlowBudget.Path();add(p,g,100,200);
        assertEquals(60,b.reserve(7,p,60,true));assertEquals(40,b.reserve(7,p,60,true));assertEquals(0,b.reserve(7,p,1,false));
    }
    @Test void differentTierHistoricalUsageDoesNotInventABottleneck()throws Exception {
        var b=new IndustrialFlowBudget();b.reserveSingle(3,1,100,5,true);b.reserveSingle(3,2,200,70,true);
        var g=group(new long[]{1,2},new byte[]{0,1});var p=new IndustrialFlowBudget.Path();add(p,g,100,200);
        assertEquals(95,b.reserve(3,p,200,false));assertEquals(90,b.reserve(3,p,90,true));
        assertEquals(5,b.reserveSingle(3,1,100,100,false));assertEquals(40,b.reserveSingle(3,2,200,200,false));
    }
    @Test void splitAndMergeKeepSameTickPhysicalUsage()throws Exception {
        var b=new IndustrialFlowBudget();var g=group(new long[]{1,2,3},new byte[]{0,1,0});var p=new IndustrialFlowBudget.Path();add(p,g,100,200);assertEquals(60,b.reserve(1,p,60,true));
        flush(b);retire(g);var right=group(new long[]{2,3},new byte[]{1,0});var q=new IndustrialFlowBudget.Path();add(q,right,100,200);
        assertEquals(40,b.reserve(1,q,100,true));flush(b);retire(right);
        var merged=group(new long[]{1,2,3},new byte[]{0,1,0});var z=new IndustrialFlowBudget.Path();add(z,merged,100,200);assertEquals(0,b.reserve(1,z,100,false));
    }
    @Test void configurationReadsUseBothTierCapacities()throws Exception {
        var b=new IndustrialFlowBudget();var g=group(new long[]{1,2},new byte[]{0,1});var p=new IndustrialFlowBudget.Path();add(p,g,100,200);b.reserve(1,p,60,true);
        var q=new IndustrialFlowBudget.Path();add(q,g,150,80);assertEquals(20,b.reserve(1,q,100,false));
    }
    @Test void simulateAcrossTicksDoesNotEraseOldUsage()throws Exception {
        var b=new IndustrialFlowBudget();var g=group(new long[]{1,2},new byte[]{0,0});var p=new IndustrialFlowBudget.Path();add(p,g,100,100);b.reserve(4,p,70,true);
        assertEquals(100,b.reserve(5,p,100,false));assertEquals(30,b.reserve(4,p,100,false));assertEquals(100,b.reserve(5,p,100,true));assertEquals(0,b.reserve(5,p,1,false));
    }
    @Test void overlappingInputOutputChargesMaximumNotSum()throws Exception {
        var b=new IndustrialFlowBudget();var g=group(new long[]{1,2},new byte[]{0,0});var p=new IndustrialFlowBudget.Path();add(p,g,100,100);
        p.cross(1,70);p.cross(1,40);int amount=p.takeCrossed(1);assertEquals(70,amount);assertEquals(70,charge(b,1,p,1,amount));assertEquals(30,b.reserve(1,p,100,false));
    }
    @Test void retiredPreparedPathChargesEveryOriginalMember()throws Exception {
        var b=new IndustrialFlowBudget();var g=group(new long[]{1,2,3},new byte[]{0,1,0});var p=new IndustrialFlowBudget.Path();add(p,g,100,200);
        retire(g);assertEquals(55,charge(b,1,p,1,55));for(long node:new long[]{1,2,3})assertEquals(node==2?145:45,b.reserveSingle(1,node,node==2?200:100,200,false));
    }
    @Test void randomRetopologyAndRawFallbackMatchPhysicalLedger()throws Exception {
        var random=new Random(73191);var b=new IndustrialFlowBudget();Map<Long,Integer> used=new HashMap<>();long tick=1;
        for(int step=0;step<3000;step++) {
            flush(b);if(step%17==0){tick++;used.clear();}
            long first=1+random.nextInt(12);int count=1+random.nextInt(4);long[] ns=new long[count];byte[] ts=new byte[count];
            int low=50+random.nextInt(200),high=100+random.nextInt(300);int expected=500;
            for(int i=0;i<count;i++){ns[i]=first+i;ts[i]=(byte)(ns[i]%2);int cap=ts[i]==0?low:high;expected=Math.min(expected,Math.max(0,cap-used.getOrDefault(ns[i],0)));}
            var g=group(ns,ts);var p=new IndustrialFlowBudget.Path();add(p,g,low,high);boolean execute=random.nextBoolean();
            assertEquals(expected,b.reserve(tick,p,500,execute),"步骤"+step);if(execute)for(long n:ns)used.merge(n,expected,Integer::sum);
            flush(b);retire(g);long n=1+random.nextInt(15);int cap=100+random.nextInt(300),request=random.nextInt(50);int raw=Math.min(request,Math.max(0,cap-used.getOrDefault(n,0)));
            assertEquals(raw,b.reserveSingle(tick,n,cap,request,true));if(raw>0)used.merge(n,raw,Integer::sum);
        }
    }
    @Test void reentrantSingleMemberChargeKeepsOtherMembersFullyCharged()throws Exception {
        var b=new IndustrialFlowBudget();var g=group(new long[]{1,2},new byte[]{0,0});var p=new IndustrialFlowBudget.Path();add(p,g,100,100);
        assertEquals(100,b.reserve(1,p,100,false));b.reserveSingle(1,1,100,80,true);
        assertEquals(20,charge(b,1,p,1,70));assertEquals(0,b.reserveSingle(1,1,100,100,false));assertEquals(30,b.reserveSingle(1,2,100,100,false));
    }
    @Test void demotionKeepsTierSpecificCapacitiesAndDuplicateMin()throws Exception {
        var g=group(new long[]{1,2},new byte[]{1,0});var p=new IndustrialFlowBudget.Path();add(p,g,100,200);
        var m=p.getClass().getDeclaredMethod("demote",g.getClass());m.setAccessible(true);m.invoke(p,g);p.add(2,70);
        assertEquals(200,p.capacity(1));assertEquals(70,p.capacity(2));var b=new IndustrialFlowBudget();assertEquals(70,b.reserve(1,p,100,false));
    }
    @Test void ordinaryNodeOverlappingAnActiveGroupSeesMaterializedUsage()throws Exception {
        var b=new IndustrialFlowBudget();var old=group(new long[]{1,2},new byte[]{0,0});var p=new IndustrialFlowBudget.Path();add(p,old,100,100);b.reserve(1,p,60,true);
        var next=group(new long[]{3,4},new byte[]{0,0});var q=new IndustrialFlowBudget.Path();add(q,next,100,100);q.add(2,100);assertEquals(40,b.reserve(1,q,100,false));
    }
}
