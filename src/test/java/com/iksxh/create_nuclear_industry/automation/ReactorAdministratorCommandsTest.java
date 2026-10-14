package com.iksxh.create_nuclear_industry.automation;
import com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorCommands;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** 原子草稿不能以一个合法字段掩盖另一个非法字段。 */
class ReactorAdministratorCommandsTest {
    @Test void projectionBudgetRejectsNestedInventoriesNonFiniteAndDuplicateCells() {
        var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("ColumnsX",14);tag.putInt("ColumnsZ",14);
        var cells=new net.minecraft.nbt.ListTag();
        for(int x=0;x<14;x++)for(int z=0;z<14;z++){var cell=new net.minecraft.nbt.CompoundTag();cell.putInt("X",x);cell.putInt("Z",z);cell.putString("Role","empty");cells.add(cell);}
        tag.put("Columns",cells);assertDoesNotThrow(()->new com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorProjection(tag));
        cells.add(cells.get(0).copy());assertThrows(IllegalArgumentException.class,()->new com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorProjection(tag));cells.remove(cells.size()-1);
        var duplicate=tag.copy();duplicate.getList("Columns",net.minecraft.nbt.Tag.TAG_COMPOUND).set(1,cells.get(0).copy());assertThrows(IllegalArgumentException.class,()->new com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorProjection(duplicate));
        tag.putDouble("ActualHeat",Double.NaN);assertThrows(IllegalArgumentException.class,()->new com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorProjection(tag));tag.remove("ActualHeat");
        tag.put("FuelAssembly",new net.minecraft.nbt.CompoundTag());assertThrows(IllegalArgumentException.class,()->new com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorProjection(tag));
    }
    @Test void independentActionsRoundTripWithoutSettingsDraft() {
        for(var action:com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorCommands.Action.values()) {
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
            var draft=action==com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorCommands.Action.APPLY?
                    new com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.CONSTANT,30,400):null;
            var request=new com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorCommands.Request(1,java.util.UUID.randomUUID(),net.minecraft.core.BlockPos.ZERO,new net.minecraft.core.BlockPos(1,0,0),java.util.UUID.randomUUID(),5,8,1,action,draft);
            try {
                ReactorAutomationNetwork.Input.CODEC.encode(buffer,new ReactorAutomationNetwork.Input(request));
                var decoded=ReactorAutomationNetwork.Input.CODEC.decode(buffer).request();
                assertEquals(action,decoded.action());assertEquals(request.settingsRevision(),decoded.settingsRevision());
                assertEquals(request.sessionId(),decoded.sessionId());
                if(draft!=null)assertEquals(draft,decoded.draft());
            } finally {buffer.release();}
        }
    }
    @Test void rejectsWholeDraftWhenAnyFieldIsInvalid() {
        for (var d : new ReactorAdministratorCommands.Draft[]{
                new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.STOP,50,10),
                new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,-1,10),
                new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.CONSTANT,101,10),
                new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.CONSTANT,50,1001),
                new ReactorAdministratorCommands.Draft(null,50,10)}) assertFalse(ReactorAdministratorCommands.validate(d,1000).isEmpty());
        assertEquals("",ReactorAdministratorCommands.validate(new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,100,0),1000));
    }
}
