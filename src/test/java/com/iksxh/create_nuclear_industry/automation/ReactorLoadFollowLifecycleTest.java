package com.iksxh.create_nuclear_industry.automation;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 新实例等待、容量一旦建立后失效即保护；不复用旧PI或跨表/跨网会话。 */
class ReactorLoadFollowLifecycleTest {
    private static ReactorStressSample s(double c,Long network,long tick) {return new ReactorStressSample(ReactorStressSample.Status.VALID,c,0,network,tick);}
    @Test void waitingIsFiniteAndReplacementNeverBecomesZeroCapacityWaiting() {
        var gauge=new Object();var session=new ReactorLoadFollowSession(gauge,UUID.randomUUID(),false);
        assertEquals("",session.acceptSample(gauge,s(0,null,10),1200));assertFalse(session.established());
        assertEquals("",session.acceptSample(gauge,s(0,null,1209),1200));
        assertEquals("stress_timeout",session.acceptSample(gauge,s(0,null,1210),1200));
        assertEquals("gauge_changed",session.acceptSample(new Object(),s(0,null,1210),1200));
    }
    @Test void capacityLossAndNetworkChangeAfterEstablishmentAreFaults() {
        var gauge=new Object();var session=new ReactorLoadFollowSession(gauge,UUID.randomUUID(),true);
        assertEquals("",session.acceptSample(gauge,s(100,1L,10),1200));assertTrue(session.established());
        assertEquals("stress_capacity_lost",session.acceptSample(gauge,s(0,1L,11),1200));
        assertEquals("stress_network_changed",session.acceptSample(gauge,s(100,2L,11),1200));
        assertFalse(session.pi().initialized());assertTrue(session.loading());
    }
    @Test void normalLoadSavePreservesPreferencesWithoutPiAndWithoutRunPermission() {
        var settings=new ReactorAutomationPortBlockEntity.Settings(ReactorAutomationState.Mode.LOAD_FOLLOW,61,123,37,42);
        var state=new ReactorAutomationState(true,ReactorAutomationState.Phase.LOAD_FOLLOW,"regulating",61,9,6,0,0,0,ReactorAutomationState.Mode.LOAD_FOLLOW,true);
        var nbt=ReactorAutomationResume.encode(state,settings,net.minecraft.core.BlockPos.ZERO,net.minecraft.core.BlockPos.ZERO.west(),true);
        var loaded=ReactorAutomationResume.decode(nbt,true,ReactorAutomationControllerTest.snapshot(0,0));
        assertNotNull(loaded.session());assertEquals(settings,loaded.session().settings());
        assertFalse(loaded.state().runPermission());assertEquals(61,loaded.state().commandedDepthPercent());
        assertFalse(nbt.contains("Integral"));assertFalse(nbt.contains("Network"));
    }
    @Test void zeroCapacityStillFixesKnownNetworkIdentityWhileNullWaitRemainsFinite() {
        var gauge=new Object();var session=new ReactorLoadFollowSession(gauge,UUID.randomUUID(),false);
        assertEquals("",session.acceptSample(gauge,s(0,null,1),1200));assertNull(session.network());
        assertEquals("",session.acceptSample(gauge,s(0,7L,2),1200));assertEquals(7L,session.network());assertFalse(session.established());
        assertEquals("stress_network_changed",session.acceptSample(gauge,s(0,8L,3),1200));
        var lost=new ReactorLoadFollowSession(gauge,UUID.randomUUID(),true);lost.acceptSample(gauge,s(0,7L,1),1200);
        assertEquals("stress_network_changed",lost.acceptSample(gauge,s(0,null,2),1200));
    }
}
