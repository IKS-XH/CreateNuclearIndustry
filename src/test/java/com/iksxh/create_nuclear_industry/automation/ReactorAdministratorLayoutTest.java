package com.iksxh.create_nuclear_industry.automation;
import com.iksxh.create_nuclear_industry.automation.client.ReactorAdministratorLayout;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** 最大196格均可点击且不遮挡全页固定停堆按钮。 */
class ReactorAdministratorLayoutTest {
    @Test void bothModesSynchronizeConfirmedFieldsWithoutOverwritingAnEditedDraft() {
        var edited=new ReactorAdministratorLayout.DraftFields("62","417");
        var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("Depth",80);tag.putInt("Heat",230);
        var confirmed=ReactorAdministratorLayout.DraftFields.confirmed(tag);
        for(var mode:new ReactorAutomationState.Mode[]{ReactorAutomationState.Mode.MANUAL,ReactorAutomationState.Mode.CONSTANT}) {
            var updated=edited.synchronize(confirmed,false).toDraft(mode);
            assertEquals(mode,updated.mode());assertEquals(80,updated.depthPercent());assertEquals(230,updated.heatHuPerTick());
            var preserved=edited.synchronize(confirmed,true).toDraft(mode);
            assertEquals(62,preserved.depthPercent());assertEquals(417,preserved.heatHuPerTick());
        }
    }
    @Test void all196CellsStayInsidePanelAndAwayFromStop() {
        for(int x=0;x<14;x++) for(int z=0;z<14;z++) {
            var r=ReactorAdministratorLayout.cell(14,14,x,z);
            assertFalse(r.intersects(ReactorAdministratorLayout.stop()));
            assertTrue(r.x()>=0 && r.y()>=0 && r.x()+r.width()<=312 && r.y()+r.height()<=224);
            assertTrue(r.contains(r.x()+.5,r.y()+.5));
        }
        assertFalse(ReactorAdministratorLayout.cell(14,14,0,0).intersects(ReactorAdministratorLayout.cell(14,14,1,0)));
        assertThrows(IllegalArgumentException.class,()->ReactorAdministratorLayout.cell(15,14,0,0));
    }
}
