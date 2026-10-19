package com.iksxh.create_nuclear_industry.automation;

import com.iksxh.create_nuclear_industry.reactor.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** 当前版本非权威意图与初始化等待合同；世界/红石与真实付费另由独立真实域验证。 */
class ReactorAutomationResumeTest {
    private static final BlockPos PORT=new BlockPos(1,2,3),BURNER=new BlockPos(0,2,3);
    private static ReactorAutomationState running(ReactorAutomationState.Phase phase,int age) {
        return new ReactorAutomationState(true,phase,phase==ReactorAutomationState.Phase.MANUAL?"manual":"regulating",65,77,age,0,0,0,
                phase==ReactorAutomationState.Phase.MANUAL?ReactorAutomationState.Mode.MANUAL:ReactorAutomationState.Mode.CONSTANT,true);
    }
    private static CompoundTag saved(ReactorAutomationState state,boolean normal) {
        return ReactorAutomationResume.encode(state,new ReactorAutomationPortBlockEntity.Settings(state.requestedMode(),42,37),PORT,BURNER,normal);
    }
    @Test void bothRetainedModesResumeTheirOwnSettingsWithoutClosedLoopFields() {
        for(var phase:new ReactorAutomationState.Phase[]{ReactorAutomationState.Phase.MANUAL,ReactorAutomationState.Phase.CONSTANT}) {
            var tag=saved(running(phase,7),true);
            assertFalse(tag.contains("SettingReserve"));assertFalse(tag.contains("SettingMinimumHeat"));
            var loaded=ReactorAutomationResume.decode(tag,true,ReactorSnapshot.empty());
            assertNotNull(loaded.session());assertFalse(loaded.state().runPermission());
            assertEquals(phase,loaded.session().intent().phase());
            assertEquals(running(phase,7).requestedMode(),loaded.session().settings().mode());
            assertEquals(42,loaded.session().settings().manualDepthPercent());assertEquals(37,loaded.session().settings().targetHeatHuPerTick());
        }
    }
    @Test void removedOrInvalidSavedModesAndPhasesCannotResumeOrGrantPermission() {
        for(int mode:new int[]{3,4,-1,Integer.MAX_VALUE})for(String key:new String[]{"Mode","SettingMode"}) {
            var tag=saved(running(ReactorAutomationState.Phase.CONSTANT,7),true);tag.putInt(key,mode);
            var loaded=ReactorAutomationResume.decode(tag,true,ReactorSnapshot.empty());
            assertNull(loaded.session());assertFalse(loaded.state().runPermission());
            assertEquals(ReactorAutomationState.Phase.FAULT,loaded.state().phase());
        }
        for(String phase:new String[]{"LOAD_WARMUP","LOAD_FOLLOW"}) {
            var tag=saved(running(ReactorAutomationState.Phase.CONSTANT,7),true);tag.putString("Phase",phase);
            var loaded=ReactorAutomationResume.decode(tag,true,ReactorSnapshot.empty());
            assertNull(loaded.session());assertFalse(loaded.state().runPermission());
        }
    }
    @Test void normalManualKeepsIntentButCannotRestoreAuthority() {
        var loaded=ReactorAutomationResume.decode(saved(running(ReactorAutomationState.Phase.MANUAL,7),true),true,ReactorSnapshot.empty());
        assertNotNull(loaded.session());assertEquals(ReactorAutomationState.Phase.ARMING,loaded.state().phase());assertFalse(loaded.state().runPermission());
        assertEquals(ReactorAutomationState.Phase.MANUAL,loaded.session().intent().phase());assertEquals(65,loaded.session().intent().commandedDepthPercent());
        assertEquals(7,loaded.session().intent().adjustmentAge());assertFalse(loaded.session().intent().runPermission());assertNotEquals(77,loaded.state().authorityEpoch());
    }
    @Test void constantKeepsAdjustmentAgeAndOrdinaryAutosaveNeverGrantsResume() {
        var state=running(ReactorAutomationState.Phase.CONSTANT,9);
        var normal=ReactorAutomationResume.decode(saved(state,true),true,ReactorSnapshot.empty());
        assertNotNull(normal.session());assertEquals(9,normal.session().intent().adjustmentAge());assertEquals(ReactorAutomationState.Mode.CONSTANT,normal.session().intent().requestedMode());
        var ordinary=ReactorAutomationResume.decode(saved(state,false),true,ReactorSnapshot.empty());
        assertNull(ordinary.session());assertEquals(ReactorAutomationState.Phase.STOPPED,ordinary.state().phase());assertFalse(ordinary.state().runPermission());
    }
    @Test void stopFaultAndSavedScramStayInactive() {
        var state=running(ReactorAutomationState.Phase.MANUAL,7);
        var stopped=ReactorAutomationResume.decode(saved(state.stop("stopped",false),true),true,ReactorSnapshot.empty());
        assertNull(stopped.session());assertEquals(ReactorAutomationState.Phase.STOPPED,stopped.state().phase());
        var fault=ReactorAutomationResume.decode(saved(state.stop("hot_trip",true),true),true,ReactorSnapshot.empty());
        assertNull(fault.session());assertEquals(ReactorAutomationState.Phase.FAULT,fault.state().phase());assertEquals("hot_trip",fault.state().reason());assertFalse(fault.state().runPermission());
        var pos=new CoreColumnPosition(1,1);var snapshot=new ReactorSnapshot(Map.of(),Map.of(pos,new ControlRodColumnState(1,1,.4,false,0)),0,0,0,false,Map.of(pos,.8),true,false);
        var scram=ReactorAutomationResume.decode(saved(state,true),true,snapshot);
        assertNull(scram.session());assertTrue(scram.savedScram());assertEquals(ReactorAutomationState.Phase.FAULT,scram.state().phase());assertEquals("external_scram",scram.state().reason());
    }
    @Test void malformedCheckpointAndMissingCheckpointCannotGrantPermission() {
        for(String key:new String[]{"AdjustmentAge","CommandedDepth","Mode"}) {
            var tag=saved(running(ReactorAutomationState.Phase.CONSTANT,9),true);tag.putInt(key,-1);
            var invalid=ReactorAutomationResume.decode(tag,true,ReactorSnapshot.empty());
            assertNull(invalid.session());assertEquals(ReactorAutomationState.Phase.FAULT,invalid.state().phase());assertFalse(invalid.state().runPermission());
        }
        var malformed=saved(running(ReactorAutomationState.Phase.MANUAL,7),true);malformed.putString("Phase","CONSTANT");
        assertNull(ReactorAutomationResume.decode(malformed,true,ReactorSnapshot.empty()).session());
        var absent=ReactorAutomationResume.decode(null,true,ReactorSnapshot.empty());assertNull(absent.session());assertEquals(ReactorAutomationState.Phase.STOPPED,absent.state().phase());
        var nativeOwner=ReactorAutomationResume.decode(null,false,ReactorSnapshot.empty());assertFalse(nativeOwner.state().claimed());assertNull(nativeOwner.session());
    }
    @Test void initializationWaitUsesGameTicksAndCannotExtendOnRepeatedCalls() {
        var loaded=ReactorAutomationResume.decode(saved(running(ReactorAutomationState.Phase.CONSTANT,9),true),true,ReactorSnapshot.empty());
        assertNotNull(loaded.session());assertTrue(loaded.session().withinWait(100,40));assertTrue(loaded.session().withinWait(100,40));
        assertTrue(loaded.session().withinWait(139,40));assertFalse(loaded.session().withinWait(140,40));assertFalse(loaded.session().withinWait(140,40));
        var backward=ReactorAutomationResume.decode(saved(running(ReactorAutomationState.Phase.CONSTANT,9),true),true,ReactorSnapshot.empty()).session();
        assertTrue(backward.withinWait(100,40));assertFalse(backward.withinWait(99,40));
    }
    @Test void manualStopWithSafetyHighStaysStoppedAndKeepsSavedScram() {
        var pos=new CoreColumnPosition(1,1);var snapshot=new ReactorSnapshot(Map.of(),Map.of(pos,ControlRodColumnState.fullyInserted()),0,0,0,false,Map.of(pos,1.),true,false);
        var stop=ReactorAutomationResume.decode(saved(running(ReactorAutomationState.Phase.MANUAL,7).stop("stopped",false),true),true,snapshot);
        assertEquals(ReactorAutomationState.Phase.STOPPED,stop.state().phase());assertTrue(stop.savedScram());assertNull(stop.session());assertFalse(stop.state().runPermission());
    }
    @Test void sparseFuelStateRequiresFullSavedPhysicalLayoutWithoutInventingKeys() {
        var fuel=new CoreColumnPosition(1,1);var empty=new CoreColumnPosition(2,0);var rod=new CoreColumnPosition(1,0);
        var snapshot=ReactorSnapshot.empty().withColumns(Map.of(fuel,FuelColumnState.empty()),Map.of(rod,ControlRodColumnState.fullyInserted()));
        assertTrue(ReactorAutomationResume.matchesSavedLayout(Set.of(fuel,empty),Set.of(rod),Set.of(fuel,empty),snapshot));
        assertEquals(Set.of(fuel),snapshot.fuelColumns().keySet());
        assertFalse(ReactorAutomationResume.matchesSavedLayout(Set.of(fuel),Set.of(rod),Set.of(fuel,empty),snapshot));
        assertFalse(ReactorAutomationResume.matchesSavedLayout(Set.of(fuel,empty),Set.of(rod),null,snapshot));
    }
    @Test void sparseLayoutStillRejectsAlienNuclearStateAndMissingControlMapping() {
        var fuel=new CoreColumnPosition(1,1);var alien=new CoreColumnPosition(2,0);var rod=new CoreColumnPosition(1,0);
        var snapshot=ReactorSnapshot.empty().withColumns(Map.of(alien,FuelColumnState.empty()),Map.of(rod,ControlRodColumnState.fullyInserted()));
        assertFalse(ReactorAutomationResume.matchesSavedLayout(Set.of(fuel),Set.of(rod),Set.of(fuel),snapshot));
        assertFalse(ReactorAutomationResume.matchesSavedLayout(Set.of(alien),Set.of(),Set.of(alien),snapshot));
    }
}
