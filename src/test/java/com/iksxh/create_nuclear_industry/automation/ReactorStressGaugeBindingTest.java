package com.iksxh.create_nuclear_industry.automation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 实例代际、游戏时钟寿命及同世界网络互斥；真实玩家/持物/距离在本域GameTest覆盖。 */
class ReactorStressGaugeBindingTest {
    @Test void selectionDoesNotSurviveReplacementDimensionChangeOrExpiry() {
        assertTrue(ReactorStressGaugeService.selectionValid(100,1300,true,true));
        assertFalse(ReactorStressGaugeService.selectionValid(100,1301,true,true));
        assertFalse(ReactorStressGaugeService.selectionValid(100,99,true,true));
        assertFalse(ReactorStressGaugeService.selectionValid(100,101,false,true));
        assertFalse(ReactorStressGaugeService.selectionValid(100,101,true,false));
    }
    @Test void aCurrentNetworkHasOnlyOneRegulatorAndReleaseIsScoped() {
        var registry=new ReactorStressGaugeService.Registry();var world=new Object();var a=new Object();var b=new Object();
        assertTrue(registry.claim(world,1L,a));assertTrue(registry.claim(world,1L,a));
        assertFalse(registry.claim(world,1L,b));assertTrue(registry.claim(new Object(),1L,b));
        registry.release(b);assertFalse(registry.claim(world,1L,b));
        registry.release(a);assertTrue(registry.claim(world,1L,b));
        assertFalse(registry.claim(world,null,a));
    }
}
