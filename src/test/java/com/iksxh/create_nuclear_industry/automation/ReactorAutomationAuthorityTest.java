package com.iksxh.create_nuclear_industry.automation;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
/** 绑定戳与持锁状态的纯合同；玩家/世界入口另由真实服务端场景覆盖。 */
class ReactorAutomationAuthorityTest {
    @Test void rejectsOldGenerationEpochAndDifferentPorts() {
        var b=new ReactorAutomationBinding("minecraft:overworld",BlockPos.ZERO,UUID.randomUUID(),new BlockPos(1,0,0),new BlockPos(2,0,0),7);
        var r=new ReactorAutomationService.Request(b.burnerPos(),b.portPos(),b.ownerGeneration(),7,ReactorAutomationService.Command.HEAT,250,1);
        assertTrue(ReactorAutomationService.matches(r,b));
        assertFalse(ReactorAutomationService.matches(r,b.withEpoch(8)));
        assertFalse(ReactorAutomationService.matches(r,new ReactorAutomationBinding(b.dimension(),b.ownerPos(),UUID.randomUUID(),b.portPos(),b.burnerPos(),7)));
    }
    @Test void onlyManualRunningUnlocksIndividualRods() {
        for(var phase:ReactorAutomationState.Phase.values()) {
            var s=new ReactorAutomationState(true,phase,"",100,1,0,0,0,0,ReactorAutomationState.Mode.STOP,false);
            assertEquals(phase!=ReactorAutomationState.Phase.MANUAL,s.rodsLocked());
        }
        assertFalse(ReactorAutomationState.initial().rodsLocked());
    }
    @Test void faultStopKeepsClaimAndChangesAuthorityEpoch() {
        var state=new ReactorAutomationState(true,ReactorAutomationState.Phase.CONSTANT,"",50,9,0,0,0,0,ReactorAutomationState.Mode.CONSTANT,true);
        var stopped=state.stop("duty_lost",true);
        assertTrue(stopped.claimed()); assertFalse(stopped.runPermission());assertEquals(100,stopped.commandedDepthPercent()); assertEquals(10,stopped.authorityEpoch());
    }
}
