package com.iksxh.create_nuclear_industry.automation;

import org.junit.jupiter.api.Test;
import com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorCommands;
import static org.junit.jupiter.api.Assertions.*;

/** 完整草稿与正最低毛HU边界；旧HEAT/DEPTH没有当前菜单仍不得写任何模式。 */
class ReactorLoadFollowControlsTest {
    @Test void completeDraftRejectsInvalidReserveMinimumAndUpperBound() {
        var mode=ReactorAutomationState.Mode.LOAD_FOLLOW;
        assertEquals("",ReactorAdministratorCommands.validate(new ReactorAdministratorCommands.Draft(mode,100,0,20,2),1000));
        assertEquals("invalid_value",ReactorAdministratorCommands.validate(new ReactorAdministratorCommands.Draft(mode,100,0,100,2),1000));
        assertEquals("invalid_value",ReactorAdministratorCommands.validate(new ReactorAdministratorCommands.Draft(mode,100,0,20,0),1000));
        assertEquals("invalid_value",ReactorAdministratorCommands.validate(new ReactorAdministratorCommands.Draft(mode,100,0,20,1001),1000));
        assertFalse(ReactorAutomationService.applyCommand(null,null).accepted());
    }
    @Test void loadModeUsesItsOwnWarmupInsteadOfConstantZeroTargetStop() {
        var original=ReactorAutomationControllerTest.observation(ReactorAutomationControllerTest.snapshot(0,0),0);
        var sample=new ReactorAutomationObservation(original.snapshot(),original.reactorParameters(),original.coolantInput(),0,0,false,true,true,false,0,0,
                new ReactorAutomationPortBlockEntity.Settings(ReactorAutomationState.Mode.LOAD_FOLLOW,100,0,20,2),original.curve());
        var state=new ReactorAutomationState(true,ReactorAutomationState.Phase.ARMING,"arming",100,1,0,0,0,0,ReactorAutomationState.Mode.LOAD_FOLLOW,true);
        var decision=ReactorAutomationController.decide(state,sample,ReactorAutomationParameters.defaults());
        assertEquals(ReactorAutomationState.Phase.LOAD_WARMUP,decision.nextState().phase());
        assertTrue(decision.nextState().runPermission());assertTrue(decision.depthCommand().isEmpty());
    }
    @Test void completeLoadDraftAndBindingIntentRoundTripAllAuthorityFields() {
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
        var generation=java.util.UUID.randomUUID();var session=java.util.UUID.randomUUID();var position=new net.minecraft.core.BlockPos(4,5,6);
        try {
            var request=new ReactorAdministratorCommands.Request(7,session,position,position.east(),generation,41,22,9,ReactorAdministratorCommands.Action.APPLY,
                    new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.LOAD_FOLLOW,62,77,37,42));
            ReactorAutomationNetwork.Input.CODEC.encode(buffer,new ReactorAutomationNetwork.Input(request));
            assertEquals(request,ReactorAutomationNetwork.Input.CODEC.decode(buffer).request());
            var intent=new ReactorStressGaugeService.Intent(false,position,position.east(),generation,41,22);
            ReactorAutomationNetwork.Bind.CODEC.encode(buffer,new ReactorAutomationNetwork.Bind(intent));
            assertEquals(intent,ReactorAutomationNetwork.Bind.CODEC.decode(buffer).intent());
        } finally {buffer.release();}
    }
    @Test void sameModeConfirmationRefreshIncludesHiddenPreferencesAndDirtyDraftStaysFixed() {
        var old=new com.iksxh.create_nuclear_industry.automation.client.ReactorAdministratorLayout.DraftFields("100","0","20","2");
        var confirmed=new com.iksxh.create_nuclear_industry.automation.client.ReactorAdministratorLayout.DraftFields("80","30","37","42");
        var manual=old.synchronize(confirmed,ReactorAutomationState.Mode.MANUAL,false);
        assertEquals(37,manual.toDraft(ReactorAutomationState.Mode.MANUAL).reservePercent());assertEquals(42,manual.toDraft(ReactorAutomationState.Mode.MANUAL).minimumHeatHuPerTick());
        var load=old.synchronize(confirmed,ReactorAutomationState.Mode.LOAD_FOLLOW,false);
        assertEquals(80,load.toDraft(ReactorAutomationState.Mode.LOAD_FOLLOW).depthPercent());assertEquals(30,load.toDraft(ReactorAutomationState.Mode.LOAD_FOLLOW).heatHuPerTick());
        assertSame(old,old.synchronize(confirmed,ReactorAutomationState.Mode.MANUAL,true));
    }
}
