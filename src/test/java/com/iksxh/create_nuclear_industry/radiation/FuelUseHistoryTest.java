package com.iksxh.create_nuclear_industry.radiation;

import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.reactor.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 使用历史与真实物品完整组件的回归入口。 */
class FuelUseHistoryTest {
    @Test void historyInterfaceExists() {
        assertDoesNotThrow(() -> Class.forName("com.iksxh.create_nuclear_industry.radiation.FuelUseHistory"));
    }
    @Test void fractionalBurnMarksOnlyCommittedFuel() {
        var original = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        var prepared = FuelUseHistory.prepareBurnCopy(original, 0.0001);
        assertEquals(0, prepared.getDamageValue());
        assertTrue(FuelUseHistory.isUsed(prepared));
        assertFalse(FuelUseHistory.isUsed(original), "未提交副本不能污染取消事务的原件");
    }
    @Test void heatWithoutBurnDoesNotMark() {
        var original = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        assertFalse(FuelUseHistory.isUsed(FuelUseHistory.prepareBurnCopy(original, 0)));
    }
    @Test void exhaustedConversionKeepsAllComponents() {
        var original = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        original.setDamageValue(original.getMaxDamage());
        original.set(DataComponents.CUSTOM_NAME, Component.literal("保留此名称"));
        var state = new FuelColumnState(FuelAssemblyItemCodec.simulationState(original), 1, 0);
        var converted = FuelRefuelingTransaction.extract(state, 0, original).output();
        assertEquals(original.get(DataComponents.CUSTOM_NAME), converted.get(DataComponents.CUSTOM_NAME));
    }
}
