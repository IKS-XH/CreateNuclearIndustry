package com.iksxh.create_nuclear_industry.industrial;

import java.lang.reflect.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 跳过依据仅为本tick自身已耗尽额度及无周转缓存，不能缓存端点库存或刷新账本。 */
class IndustrialPumpSkipTest {
    private boolean skip(IndustrialFlowBudget budget,long tick,long pos,int capacity,boolean cache)throws Exception {
        try {var m=budget.getClass().getDeclaredMethod("canSkipPump",long.class,long.class,int.class,boolean.class);m.setAccessible(true);return (boolean)m.invoke(budget,tick,pos,capacity,cache);}
        catch(NoSuchMethodException absent){fail("尚无保守串联泵耗尽跳过判断");return false;}
    }
    @Test void freshPumpNeverSkipped()throws Exception {assertFalse(skip(new IndustrialFlowBudget(),1,1,16384,false));}
    @Test void earlierSeriesTransferExhaustsEveryEqualSpeedMember()throws Exception {
        var budget=new IndustrialFlowBudget();var path=new IndustrialFlowBudget.Path();for(int i=1;i<=8;i++)path.add(i,16384);
        assertEquals(16384,budget.reserve(1,path,16384,true));for(int i=1;i<=8;i++)assertTrue(skip(budget,1,i,16384,false));
    }
    @Test void cachedFluidAlwaysKeepsOriginalDispatch()throws Exception {
        var budget=new IndustrialFlowBudget();budget.reserveSingle(1,1,100,100,true);assertFalse(skip(budget,1,1,100,true));
    }
    @Test void partialUsageAndHigherCurrentSpeedDoNotSkip()throws Exception {
        var budget=new IndustrialFlowBudget();budget.reserveSingle(1,1,100,100,true);assertTrue(skip(budget,1,1,100,false));assertFalse(skip(budget,1,1,200,false));
    }
    @Test void nextTickRestoresDispatchWithoutErasingOldTick()throws Exception {
        var budget=new IndustrialFlowBudget();budget.reserveSingle(1,1,100,100,true);assertFalse(skip(budget,2,1,100,false));assertTrue(skip(budget,1,1,100,false));
    }
    @Test void readOnlySkipProbeDoesNotMaterializeOrEraseCompressedUsage()throws Exception {
        var budget=new IndustrialFlowBudget();var group=new IndustrialFlowBudget.Group(new long[]{1,2},new byte[]{0,0});var path=new IndustrialFlowBudget.Path();path.addGroup(group,100,100);budget.reserve(1,path,100,true);
        assertTrue(skip(budget,1,2,100,false));assertEquals(0,budget.reserve(1,path,1,false));budget.flushGroups();assertTrue(skip(budget,1,2,100,false));
    }
    @Test void independentParallelPumpStillRuns()throws Exception {
        var budget=new IndustrialFlowBudget();budget.reserveSingle(1,1,16384,16384,true);assertTrue(skip(budget,1,1,16384,false));assertFalse(skip(budget,1,2,16384,false));
    }
    @Test void lowSpeedCannotRestoreAlreadySpentPhysicalQuota()throws Exception {
        var budget=new IndustrialFlowBudget();budget.reserveSingle(1,1,16384,8192,true);assertTrue(skip(budget,1,1,4096,false));assertFalse(skip(budget,1,1,16384,false));
    }
}
