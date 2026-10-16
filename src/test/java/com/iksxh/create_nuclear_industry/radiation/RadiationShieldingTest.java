package com.iksxh.create_nuclear_industry.radiation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** 外壳审计预算的跨tick生命周期，防止旧incomplete标志污染后续无源恢复。 */
class RadiationShieldingTest {
    @Test void newSamplingTickClearsPriorIncompleteBudget() throws Exception {
        var containment=new RadiationContainment();containment.beginSampling(5);
        var incomplete=RadiationContainment.class.getDeclaredField("workComplete");incomplete.setAccessible(true);incomplete.setBoolean(containment,false);
        containment.beginSampling(5);assertFalse(containment.workComplete());
        containment.beginSampling(6);assertTrue(containment.workComplete());
    }
}
