package com.iksxh.create_nuclear_industry.automation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import org.junit.jupiter.api.Test;
import net.minecraft.core.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** 实际Create热级与Transform四象限合同，不冒称GPU或真实鼠标验收。 */
class ReactorAutomationBlazeTest {
    @Test void onlyBurningHeatLevelsCanWork() {
        for(var heat:HeatLevel.values()) assertEquals(heat==HeatLevel.FADING||heat==HeatLevel.KINDLED||heat==HeatLevel.SEETHING,ReactorAutomationBlazeBridge.dutyLevelValid(heat));
    }
    @Test void behaviourTypesAreDistinctAndFourCentersDoNotOverlap() {
        assertEquals(4,new HashSet<>(ReactorAutomationBehaviours.TYPES).size());
        for(var side:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}) {
            var centers=new ArrayList<net.minecraft.world.phys.Vec3>();
            for(int i=0;i<4;i++) {
                var t=new ReactorAutomationBehaviours.Transform(null,i); t.fromSide(side);
                var center=t.getLocalOffset(null,BlockPos.ZERO,null);
                assertTrue(centers.stream().allMatch(c->c.distanceTo(center)>.32)); centers.add(center);
            }
        }
    }
    @Test void huLocalBoardUsesOneTenHundredAndServerConfiguredLimit() {
        assertEquals(251,ReactorAutomationBehaviours.boundedHeat(250,51,0,1000000));
        assertEquals(260,ReactorAutomationBehaviours.boundedHeat(250,51,1,1000000));
        assertEquals(350,ReactorAutomationBehaviours.boundedHeat(250,51,2,1000000));
        assertEquals(0,ReactorAutomationBehaviours.boundedHeat(0,49,2,1000000));
        assertEquals(500,ReactorAutomationBehaviours.boundedHeat(490,52,1,500));
        assertEquals(1000100,ReactorAutomationBehaviours.boundedHeat(1000000,51,2,2000000));
    }
}
