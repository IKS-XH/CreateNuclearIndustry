package com.iksxh.create_nuclear_industry.automation;
import com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorCommands;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** 原子草稿不能以一个合法字段掩盖另一个非法字段。 */
class ReactorAdministratorCommandsTest {
    @Test void removedAndInvalidWireModesNeverBecomeRunnable() {
        for(int mode:new int[]{3,4,127}) {
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
            var request=new ReactorAdministratorCommands.Request(1,java.util.UUID.randomUUID(),net.minecraft.core.BlockPos.ZERO,
                    new net.minecraft.core.BlockPos(1,0,0),java.util.UUID.randomUUID(),5,8,1,ReactorAdministratorCommands.Action.APPLY,
                    new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,30,400));
            try {
                ReactorAutomationNetwork.Input.CODEC.encode(buffer,new ReactorAutomationNetwork.Input(request));
                // 定位真实协议中的模式VarInt；保留其余合法字段，使拒绝原因只来自模式。
                var cursor=new net.minecraft.network.RegistryFriendlyByteBuf(buffer.duplicate(),net.minecraft.core.RegistryAccess.EMPTY);
                cursor.readVarInt();cursor.readUUID();cursor.readBlockPos();cursor.readBlockPos();
                cursor.readUUID();cursor.readLong();cursor.readLong();cursor.readLong();cursor.readVarInt();
                buffer.setByte(cursor.readerIndex(),mode);
                var decoded=ReactorAutomationNetwork.Input.CODEC.decode(buffer).request();
                assertFalse(ReactorAdministratorCommands.validate(decoded.draft(),1000).isEmpty(),
                        "已删除或非法模式不得经CODEC变成合法运行草稿："+mode);
            } finally {buffer.release();}
        }
    }
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
    @Test void retainedApplyModesRoundTripAndConsumeTheirWholePayload() {
        for(var mode:new ReactorAutomationState.Mode[]{ReactorAutomationState.Mode.MANUAL,ReactorAutomationState.Mode.CONSTANT}) {
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
            var draft=new ReactorAdministratorCommands.Draft(mode,62,417);
            var request=new ReactorAdministratorCommands.Request(3,java.util.UUID.randomUUID(),net.minecraft.core.BlockPos.ZERO,
                    new net.minecraft.core.BlockPos(1,0,0),java.util.UUID.randomUUID(),7,11,2,ReactorAdministratorCommands.Action.APPLY,draft);
            try {
                ReactorAutomationNetwork.Input.CODEC.encode(buffer,new ReactorAutomationNetwork.Input(request));
                var decoded=ReactorAutomationNetwork.Input.CODEC.decode(buffer).request();
                assertEquals(draft,decoded.draft());assertEquals("",ReactorAdministratorCommands.validate(decoded.draft(),1000));
                assertEquals(0,buffer.readableBytes(),"完整APPLY不得遗留被删除字段或丢失尾字段");
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
