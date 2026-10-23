package com.iksxh.create_nuclear_industry.accident;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 冻结加载时间、火点上限和终态收据；世界写预算与物料由同域真实服另证。 */
class AccidentThermalStateTest {
    @Test void configuredParametersFreezeDamageAndIgnitionAcrossCurrentSave() {
        var parameters=new AccidentThermalParameters(4800,8,6,450,3.5,7);
        var state=AccidentThermalState.load(new AccidentThermalState(parameters).save());
        assertEquals(parameters,state.parameters);assertEquals(3.5,state.parameters.solidDamageHp());assertEquals(7,state.parameters.solidIgniteSeconds());
        assertThrows(IllegalArgumentException.class,()->new AccidentThermalParameters(2400,4,9,600,2,3));
        assertThrows(IllegalArgumentException.class,()->new AccidentThermalParameters(2400,4,8,601,2,3));
    }
    @Test void solidUses2400LoadedTicksAndWaterFourTimes() {
        var state=new AccidentThermalState(AccidentThermalParameters.DEFAULT);var s=state.startSolid(BlockPos.ZERO);
        for(int i=0;i<2399;i++)state.advanceSolid(s,i,true,false);
        assertEquals(1,s.remaining);state.advanceSolid(s,2399,true,false);assertEquals(0,s.remaining);
        var wet=state.startSolid(BlockPos.ZERO.above());for(int i=0;i<600;i++)state.advanceSolid(wet,i,true,true);
        assertEquals(0,wet.remaining);
    }
    @Test void unavailablePausesAndNoOfflineCatchupOrSameTickDoubleAdvance() {
        var state=new AccidentThermalState(AccidentThermalParameters.DEFAULT);var s=state.startSolid(BlockPos.ZERO);
        state.advanceSolid(s,1,true,false);state.advanceSolid(s,1,true,true);assertEquals(2399,s.remaining);
        state.advanceSolid(s,90000,false,true);assertEquals(2399,s.remaining);
        state.advanceSolid(s,90001,true,false);assertEquals(2398,s.remaining);
        var restored=AccidentThermalState.load(state.save());assertEquals(2398,restored.solid(BlockPos.ZERO).remaining);
    }
    @Test void firesCapAtEightExpireAt600AndNeverReplanExtinguishedPosition() {
        var state=new AccidentThermalState(AccidentThermalParameters.DEFAULT);
        for(int i=0;i<8;i++)assertNotNull(state.planFire(new BlockPos(i,0,0)));
        assertNull(state.planFire(new BlockPos(9,0,0)));var f=state.fires().getFirst();f.attempted=true;f.placed=true;
        for(int i=0;i<599;i++)state.advanceFire(f,i,true);assertTrue(f.dangerous());
        state.advanceFire(f,599,true);assertFalse(f.dangerous());assertFalse(f.cleaned);
        state.cleaned(f);assertNull(state.planFire(f.pos));
        var other=state.fires().get(1);state.endFire(other);state.cleaned(other);
        var restored=AccidentThermalState.load(state.save());assertTrue(restored.fire(other.id).terminal);assertNull(restored.planFire(other.pos));
    }
    @Test void solidReceiptCannotReheatOrContainPayload() {
        var state=new AccidentThermalState(AccidentThermalParameters.DEFAULT);var s=state.startSolid(BlockPos.ZERO);
        state.endSolid(s);assertSame(s,state.startSolid(BlockPos.ZERO));assertFalse(state.active());
        var saved=state.save();assertFalse(saved.toString().contains("Payload"));
        assertEquals(0,AccidentThermalState.load(saved).startSolid(BlockPos.ZERO).remaining);
    }
}
