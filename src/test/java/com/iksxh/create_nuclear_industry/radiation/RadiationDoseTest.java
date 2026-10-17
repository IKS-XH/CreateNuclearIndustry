package com.iksxh.create_nuclear_industry.radiation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** 剂量阈值、恢复边界、禁用、持久化与伤害区计时独立验证。 */
class RadiationDoseTest {
    private final RadiationParameters p=RadiationParameters.DEFAULT;
    @Test void oneDosePerSecondReachesAllThresholdsAndCaps() {
        var state=RadiationDoseState.ZERO;
        for(int second=1;second<=120;second++) {
            state=RadiationDoseCalculator.advance(state,1,20,p);
            assertEquals(Math.min(second,100),state.dose(),1e-12);
            if(second<80)assertEquals(0,state.damageTicks());
            if(second==80)assertEquals(20,state.damageTicks());
        }
    }
    @Test void recoveryWaitsExactlyOneHundredOnlineTicks() {
        var initial=new RadiationDoseState(60,0,0);
        var waiting=RadiationDoseCalculator.advance(initial,.01,100,p);assertEquals(60,waiting.dose());assertEquals(100,waiting.safeTicks());
        var recovery=RadiationDoseCalculator.advance(waiting,.01,20,p);assertEquals(59,recovery.dose());
        var exposed=RadiationDoseCalculator.advance(recovery,.010001,20,p);assertEquals(0,exposed.safeTicks());assertTrue(exposed.dose()>59);
    }
    @Test void doseLifecycleAndDisabledRecovery() {
        var original=new RadiationDoseState(85,40,39);assertEquals(original,RadiationDoseState.load(original.save(),p));
        var disabled=RadiationParametersTest.change("enabled",false);
        var safe=RadiationDoseCalculator.advance(original,10,60,disabled);assertEquals(85,safe.dose());assertEquals(0,safe.damageTicks());
        assertEquals(84,RadiationDoseCalculator.advance(safe,10,20,disabled).dose());
        assertEquals(0,RadiationDoseCalculator.advance(new RadiationDoseState(80,0,39),0,120,p).damageTicks());
    }
    @Test void fullSuitStillAbsorbsFivePercent() {assertEquals(1,RadiationDoseCalculator.advance(RadiationDoseState.ZERO,10*.05,40,p).dose(),1e-12);}
}
