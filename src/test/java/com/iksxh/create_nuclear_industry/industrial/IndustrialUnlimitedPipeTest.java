package com.iksxh.create_nuclear_industry.industrial;

import java.lang.reflect.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 无额度管段只承载几何与介质身份；并联只叠独立泵，串联和阀仍共享物理额度。 */
class IndustrialUnlimitedPipeTest {
    private void free(IndustrialFlowBudget.Path path,long node)throws Exception {
        try {var m=path.getClass().getDeclaredMethod("addUnbounded",long.class);m.setAccessible(true);m.invoke(path,node);}
        catch(NoSuchMethodException absent){fail("普通管仍参与数值限额，尚无无额度路径入口");}
    }
    private void free(IndustrialFlowBudget.Path path,IndustrialFlowBudget.Group group)throws Exception {
        try {var m=path.getClass().getDeclaredMethod("addUnboundedGroup",IndustrialFlowBudget.Group.class);m.setAccessible(true);m.invoke(path,group);}
        catch(NoSuchMethodException absent){fail("纯管压缩组仍参与流量计账");}
    }
    @Test void bothPipeTiersAllowFourParallelPumpsAcrossOneSharedTrunk()throws Exception {
        for(byte tier:new byte[]{0,1}) {
            var budget=new IndustrialFlowBudget();var group=new IndustrialFlowBudget.Group(new long[]{100,101},new byte[]{tier,tier});int total=0;
            for(long pump=1;pump<=4;pump++){var path=new IndustrialFlowBudget.Path();path.add(pump,16384);free(path,group);total+=budget.reserve(7,path,16384,true);}
            assertEquals(65536,total,"同一管道不能继续封顶8192或32768");
        }
    }
    @Test void oneTwoFourAndEightSeriesPumpsNeverMultiplyThroughput()throws Exception {
        for(int count:new int[]{1,2,4,8}) {
            var budget=new IndustrialFlowBudget();int total=0;
            for(int initiating=0;initiating<count;initiating++) {
                var path=new IndustrialFlowBudget.Path();for(int node=0;node<count;node++)path.add(node+1,16384);free(path,99);
                total+=budget.reserve(1,path,16384,true);
            }
            assertEquals(16384,total,"串联泵必须共用各自物理额度");
        }
    }
    @Test void allPipeActiveSourceRouteHasNoArtificialSharedCeiling()throws Exception {
        var budget=new IndustrialFlowBudget();var group=new IndustrialFlowBudget.Group(new long[]{1,2},new byte[]{0,1});var path=new IndustrialFlowBudget.Path();free(path,group);
        for(int source=0;source<4;source++)assertEquals(Integer.MAX_VALUE,budget.reserve(1,path,Integer.MAX_VALUE,true));
        assertEquals(Integer.MAX_VALUE,budget.charge(1,path,group.key(),Integer.MAX_VALUE));
    }
    @Test void rawFallbackPipeHasNoAccumulatingIntegerCeiling()throws Exception {
        var budget=new IndustrialFlowBudget();var path=new IndustrialFlowBudget.Path();free(path,1);
        for(int source=0;source<4;source++){assertEquals(Integer.MAX_VALUE,budget.reserve(1,path,Integer.MAX_VALUE,true));assertEquals(Integer.MAX_VALUE,budget.charge(1,path,1,Integer.MAX_VALUE));}
    }
    @Test void valveAndSlowSeriesPumpStillLimitUnlimitedPipeRoute()throws Exception {
        var budget=new IndustrialFlowBudget();var path=new IndustrialFlowBudget.Path();free(path,1);path.add(2,8192);path.add(3,4096);path.add(4,16384);
        assertEquals(4096,budget.reserve(1,path,50000,true));assertEquals(0,budget.reserve(1,path,1,false));
    }
    @Test void boundedDuplicateAlwaysWinsOverUnboundedGeometry()throws Exception {
        var budget=new IndustrialFlowBudget();var path=new IndustrialFlowBudget.Path();free(path,1);path.add(1,50);free(path,1);
        assertEquals(50,budget.reserve(1,path,100,true));assertEquals(0,budget.reserve(1,path,1,true));
    }
    @Test void demotedUnlimitedGroupRemainsUnmetered()throws Exception {
        var budget=new IndustrialFlowBudget();var group=new IndustrialFlowBudget.Group(new long[]{1,2},new byte[]{0,1});var path=new IndustrialFlowBudget.Path();free(path,group);path.demote(group);
        assertEquals(Integer.MAX_VALUE,budget.reserve(1,path,Integer.MAX_VALUE,true));assertEquals(Integer.MAX_VALUE,budget.reserve(1,path,Integer.MAX_VALUE,true));
    }
    @Test void unboundedPipeDoesNotEraseExistingBoundedUsage()throws Exception {
        var budget=new IndustrialFlowBudget();assertEquals(60,budget.reserveSingle(1,1,100,60,true));var pipe=new IndustrialFlowBudget.Path();free(pipe,1);
        assertEquals(1000,budget.reserve(1,pipe,1000,true));assertEquals(40,budget.reserveSingle(1,1,100,100,false));
    }
    @Test void reusedTemporaryPathCannotKeepUnlimitedFlagOnANewPump()throws Exception {
        var budget=new IndustrialFlowBudget();var path=new IndustrialFlowBudget.Path();free(path,1);budget.reserve(1,path,1000,true);path.clear();path.add(1,20);
        assertEquals(20,budget.reserve(1,path,100,true));assertEquals(0,budget.reserve(1,path,1,false));
    }
}
