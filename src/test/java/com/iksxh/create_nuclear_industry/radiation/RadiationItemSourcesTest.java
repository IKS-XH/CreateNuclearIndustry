package com.iksxh.create_nuclear_industry.radiation;
import com.iksxh.create_nuclear_industry.content.*;
import com.iksxh.create_nuclear_industry.storage.SpentFuelPayload;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** 真实数据组件嵌套、非法桶与使用历史，不以物品名字推断载荷。 */
class RadiationItemSourcesTest {
    private final RadiationParameters p=RadiationParameters.DEFAULT;
    private ItemStack spent(){return new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get());}
    @Test void configuredZeroStrengthStillHasRadioactivePayload() {
        var zero=RadiationParametersTest.change("spentFuelDosePerSecond",0.0);
        var summary=RadiationItemSources.describe(spent(),zero);
        assertEquals(0,summary.dosePerSecond());assertTrue(summary.radioactivePayload());
    }
    @Test void hugeFiniteStrengthDoesNotOverflowSourceSummary() {
        var huge=RadiationParametersTest.change("spentFuelDosePerSecond",Double.MAX_VALUE);
        var box=new ItemStack(Items.SHULKER_BOX);box.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(spent(),spent())));
        var summary=RadiationItemSources.describe(box,huge);
        assertTrue(Double.isFinite(summary.unshieldedDosePerSecond()));assertTrue(Double.isFinite(summary.dosePerSecond()));
    }
    @Test void newAndRawMaterialsAreZeroUsedAndSpentAreOneAndTwo() {
        for(ItemStack s:List.of(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get()),new ItemStack(FuelProcessingContent.DEPLETED_URANIUM_DUST.get()),new ItemStack(BasicMaterialContent.LEAD_INGOT.get())))assertEquals(0,RadiationItemSources.describe(s,p).dosePerSecond());
        assertEquals(1,RadiationItemSources.describe(FuelUseHistory.markUsedCopy(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())),p).dosePerSecond());
        assertEquals(2,RadiationItemSources.describe(spent(),p).dosePerSecond());
    }
    @Test void invalidCaskDoesNotEraseSource() {
        var invalid=new ItemStack(SpentFuelStorageContent.SEALED_SPENT_FUEL_CASK.get());
        invalid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(spent(),spent())));
        var summary=RadiationItemSources.describe(invalid,p);assertTrue(summary.radioactivePayload());assertEquals(4,summary.dosePerSecond());
        assertEquals(0,RadiationItemSources.describe(new ItemStack(SpentFuelStorageContent.SEALED_SPENT_FUEL_CASK.get()),p).dosePerSecond());
    }
    @Test void nestedContainersAndSixteenCasksCountPayloadOnce() {
        var cask=SpentFuelPayload.seal(spent());var box=new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(java.util.Collections.nCopies(16,cask)));
        var summary=RadiationItemSources.describe(box,p);assertEquals(32,summary.unshieldedDosePerSecond());assertEquals(1.6,summary.dosePerSecond(),1e-12);assertTrue(summary.complete());
    }
    @Test void iterativeTraversalCanResumeWithoutRecursionOrPretendingEmpty() {
        ItemStack nested=spent();for(int i=0;i<300;i++){var outer=new ItemStack(Items.SHULKER_BOX);outer.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(nested)));nested=outer;}
        var cursor=new RadiationItemSources.Cursor(nested,p);cursor.advance(10);assertFalse(cursor.summary().complete());cursor.advance(400);assertTrue(cursor.summary().complete());assertEquals(2,cursor.summary().dosePerSecond());
    }
}
