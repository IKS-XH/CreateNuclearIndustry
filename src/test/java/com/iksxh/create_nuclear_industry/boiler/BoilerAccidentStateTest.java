package com.iksxh.create_nuclear_industry.boiler;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.iksxh.create_nuclear_industry.boiler.BoilerAccidentState.Cause.*;

/** 验证加载时钟、恢复迟滞、冻结方向及能量之外的事故决定，不依赖真实世界或旧格式。 */
class BoilerAccidentStateTest {
    private final BoilerAccidentState.Parameters p=BoilerAccidentState.DEFAULT;
    @Test void coldEmptyIsSafeAndContinuousDryFiringNeedsFourHundredLoadedTicks(){
        var s=new BoilerAccidentState();assertEquals(NONE,s.advance(1,true,0,false,p));
        for(int n=1;n<400;n++)assertEquals(NONE,s.advance(n+1,true,0,true,p));
        assertEquals(DRY,s.advance(401,true,0,true,p));
        s.advance(402,true,0,false,p);assertEquals(0,s.dryTicks());
    }
    @Test void sameTickSaveAndUnloadedGapDoNotInventExposure(){
        var s=new BoilerAccidentState();s.advance(1,true,1.1,true,p);s.advance(1,true,1.1,true,p);
        assertEquals(1,s.dryTicks());assertEquals(1,s.pressureTicks());
        var r=new BoilerAccidentState();r.load(s.save());r.advance(1,true,1.1,true,p);r.advance(5000,false,1.1,true,p);
        assertEquals(1,r.pressureTicks());r.advance(9000,true,1.1,true,p);assertEquals(2,r.pressureTicks());
        r.advance(9001,true,.7,false,p);assertEquals(0,r.dryTicks());assertEquals(2,r.pressureTicks());
    }
    @Test void overpressurePausesUntilFortyTicksBelowRecoveryAndTripsAtTwoHundred(){
        var s=new BoilerAccidentState();for(int n=1;n<=100;n++)assertEquals(NONE,s.advance(n,true,1.1,false,p));
        for(int n=101;n<=180;n++)s.advance(n,true,.97,false,p);assertEquals(100,s.pressureTicks());
        for(int n=181;n<220;n++)s.advance(n,true,.95,false,p);assertEquals(100,s.pressureTicks());
        s.advance(220,true,.95,false,p);assertEquals(0,s.pressureTicks());
        for(int n=221;n<420;n++)assertEquals(NONE,s.advance(n,true,1.01,false,p));
        assertEquals(BURST,s.advance(420,true,1.01,false,p));
        assertEquals(BURST,new BoilerAccidentState().advance(1,true,1.5,false,p));
    }
    @Test void dangerousBreakPrefersBurstAndSubmittedIdentityPreventsRepeating(){
        var s=new BoilerAccidentState();assertEquals(NONE,s.dangerous(1,false,p));
        assertEquals(DRY,s.dangerous(.6,true,p));assertEquals(BURST,s.dangerous(1.01,true,p));
        UUID id=UUID.randomUUID();s.submitted(id);assertEquals(NONE,s.advance(2,true,2,true,p));
        var r=new BoilerAccidentState();r.load(s.save());assertEquals(id,r.pending());assertEquals(s.save(),r.save());
        r.finished(UUID.randomUUID());assertEquals(id,r.pending());r.finished(id);assertNull(r.pending());
    }
    @Test void frozenParametersRoundTripAndPressureRadiusAreBounded(){
        assertTrue(p.valid());assertEquals(p,BoilerAccidentState.loadParameters(BoilerAccidentState.saveParameters(p)));
        assertEquals(2,p.radius(1));assertEquals(4,p.radius(1.5));assertEquals(6,p.radius(5));
        assertFalse(new BoilerAccidentState.Parameters(1,1,200,.95,40,400,2,4,6,16).valid());
        assertFalse(new BoilerAccidentState.Parameters(1,1.5,200,1,40,400,2,4,6,16).valid());
        assertFalse(new BoilerAccidentState.Parameters(1,1.5,200,.95,40,400,7,4,6,16).valid());
    }
    @Test void actualRelocationDetachesOnlyAnAlreadySubmittedReceipt(){
        var s=new BoilerAccidentState();s.advance(1,true,1.1,true,p);s.detachPending();
        assertEquals(1,s.pressureTicks());assertEquals(1,s.dryTicks());
        s.submitted(UUID.randomUUID());s.detachPending();assertNull(s.pending());
        assertEquals(0,s.pressureTicks());assertEquals(0,s.dryTicks());
    }
    @Test void everyDirectionStartsOnItsLargeBoilerFaceAndExcludesTheBack(){
        var f=new BoilerStructure.Form(BlockPos.ZERO,new BlockPos(15,15,15),8,1372,1176,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());
        for(var d:Direction.values()){
            var origin=BoilerAccidentLifecycle.faceCenter(f,d);
            assertTrue(origin.getX()==0||origin.getX()==15||origin.getY()==0||origin.getY()==15||origin.getZ()==0||origin.getZ()==15);
            var l=new BoilerAccidentLedger(UUID.randomUUID(),UUID.randomUUID(),BlockPos.ZERO,origin,d,true,1.5,p,List.of());
            assertTrue(l.inJet(Vec3.atCenterOf(origin.relative(d,2))));
            assertFalse(l.inJet(Vec3.atCenterOf(origin.relative(d.getOpposite(),2))));
            assertFalse(l.inJet(Vec3.atCenterOf(origin.relative(d,5))));
        }
    }
}
