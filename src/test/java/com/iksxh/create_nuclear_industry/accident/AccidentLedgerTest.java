package com.iksxh.create_nuclear_industry.accident;

import com.iksxh.create_nuclear_industry.radiation.FuelUseHistory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 纯账本只验证真实托管和收据；注册物品、世界所有权与破坏另由隔离真实服验证。 */
class AccidentLedgerTest {
    @Test void thermalActivityDoesNotExtendOldCompletionAndBudgetIsShared() {
        var ledger=new ReactorAccidentLedger(java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),"minecraft:overworld","COUNTDOWN_COMPLETE",
                BlockPos.ZERO,BlockPos.ZERO,BlockPos.ZERO,AccidentParameters.DEFAULT,0,0,0,java.util.List.of(),java.util.List.of());
        ledger.shockComplete=true;ledger.thermal.startSolid(BlockPos.ZERO);
        assertTrue(ledger.complete());assertTrue(ledger.thermal.active());
        var restored=ReactorAccidentLedger.load(ledger.save(RegistryAccess.EMPTY),RegistryAccess.EMPTY);
        assertTrue(restored.complete());assertTrue(restored.thermal.active());
        var data=new ReactorAccidentSavedData();assertEquals(64,data.availableBudget(1,64));
        data.spendBudget(40);assertEquals(24,data.availableBudget(1,64));data.spendBudget(24);
        assertEquals(0,data.availableBudget(1,64));assertEquals(64,data.availableBudget(2,64));
    }
    @Test void completedReceiptIsCachedAndRebuiltFromCurrentFormat() throws Exception {
        var action=new ReactorAccidentLedger.Action(BlockPos.ZERO,0,false,null);action.done=true;
        var ledger=new ReactorAccidentLedger(java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),"minecraft:overworld","COUNTDOWN_COMPLETE",
                BlockPos.ZERO,BlockPos.ZERO,BlockPos.ZERO.offset(4,4,4),AccidentParameters.DEFAULT,0,0,0,java.util.List.of(),java.util.List.of(action));
        ledger.shockComplete=true;assertTrue(ledger.complete());
        var saved=ledger.save(RegistryAccess.EMPTY);
        // 测试列表拒绝任何重扫，用于证明已完成收据的普通tick只读取终态缓存。
        var field=ReactorAccidentLedger.class.getField("actions");field.setAccessible(true);
        field.set(ledger,new java.util.AbstractList<ReactorAccidentLedger.Action>() {
            @Override public ReactorAccidentLedger.Action get(int index){throw new AssertionError("终态不应重扫动作");}
            @Override public int size(){throw new AssertionError("终态不应重扫动作");}
        });
        for(int i=0;i<20;i++)assertTrue(ledger.complete());
        var restored=ReactorAccidentLedger.load(saved,RegistryAccess.EMPTY);assertTrue(restored.complete());
        saved.getList("Actions",10).getCompound(0).putBoolean("Done",false);
        assertFalse(ReactorAccidentLedger.load(saved,RegistryAccess.EMPTY).complete(),"加载必须重建，不能沿用已完成缓存");
    }
    @Test void custodyConservesPhysicalStacksAcrossReplay() {
        var column = new ReactorAccidentLedger.Column(BlockPos.ZERO, BlockPos.ZERO.above(), java.util.UUID.randomUUID(),
                FuelUseHistory.markUsedCopy(new ItemStack(Items.DIAMOND_SWORD)), true);
        assertFalse(column.settled());
        assertEquals(1, column.payload().getCount());
        var restored = ReactorAccidentLedger.Column.load(column.save(RegistryAccess.EMPTY), RegistryAccess.EMPTY);
        assertTrue(FuelUseHistory.isUsed(restored.payload()));
        assertTrue(restored.waste());
        restored.finish(); restored.finish();
        assertTrue(restored.settled());
        assertTrue(restored.payload().isEmpty());
        assertFalse(column.payload().isEmpty());
    }
    @Test void frozenParametersRejectUnboundedGroups() {
        assertEquals(64, AccidentParameters.DEFAULT.worldActionsPerTick());
        assertEquals(32, AccidentParameters.DEFAULT.externalBlocks());
        assertThrows(IllegalArgumentException.class, () -> new AccidentParameters(true, 3, 32, 8, 64));
        assertThrows(IllegalArgumentException.class, () -> new AccidentParameters(true, 2, 33, 8, 64));
        assertThrows(IllegalArgumentException.class, () -> new AccidentParameters(true, 2, 32, 9, 64));
        assertThrows(IllegalArgumentException.class, () -> new AccidentParameters(true, 2, 32, 8, 65));
        assertEquals(AccidentParameters.DEFAULT, AccidentParameters.load(AccidentParameters.DEFAULT.save()));
    }
}
