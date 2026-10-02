package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 直接使用注册的料浆与 Create 装桶方法验证原生桶接入和整桶守恒。 */
final class CentrifugeSlurryBucketTest {
    @Test
    void registeredSlurryFillsNativeBucketThroughCreate() {
        var slurry = FuelProcessingContent.URANIUM_SLURRY.get();
        var bucketItem = FuelProcessingContent.URANIUM_SLURRY_BUCKET.get();
        assertEquals(BucketItem.class, bucketItem.getClass());
        assertSame(bucketItem, slurry.getBucket());
        assertSame(FuelProcessingContent.URANIUM_SLURRY_BLOCK.get(),
                slurry.defaultFluidState().createLegacyBlock().getBlock());

        ItemStack empty = new ItemStack(Items.BUCKET);
        FluidStack available = new FluidStack(slurry, 1000);
        assertTrue(GenericItemFilling.canItemBeFilled(null, empty));
        int required = GenericItemFilling.getRequiredAmountForItem(null, empty, available.copy());
        assertEquals(1000, required);
        ItemStack filled = GenericItemFilling.fillItem(null, required, empty, available);
        assertTrue(empty.isEmpty());
        assertTrue(filled.is(bucketItem));
        assertTrue(available.isEmpty());
        var handler = filled.getCapability(Capabilities.FluidHandler.ITEM);
        assertNotNull(handler);
        assertTrue(handler.getFluidInTank(0).is(slurry));
        assertEquals(1000, handler.getFluidInTank(0).getAmount());
        assertTrue(handler.drain(1000, IFluidHandler.FluidAction.EXECUTE).is(slurry));
        assertTrue(handler.getContainer().is(Items.BUCKET));
    }

    @Test
    void slurryTransactionRequiresWholeBucketAndValidContainer() {
        var slurry = FuelProcessingContent.URANIUM_SLURRY.get();
        FluidTank tank = new FluidTank(2000, fluid -> fluid.is(slurry));
        ItemStack bucket = new ItemStack(Items.BUCKET);
        var item = bucket.getCapability(Capabilities.FluidHandler.ITEM);
        assertNotNull(item);
        assertTrue(CentrifugeContainerTransaction.transfer(tank, item).isEmpty());
        tank.fill(new FluidStack(slurry, 999), IFluidHandler.FluidAction.EXECUTE);
        assertTrue(CentrifugeContainerTransaction.transfer(tank, item).isEmpty());
        assertEquals(999, tank.getFluidAmount());
        assertTrue(item.getContainer().is(Items.BUCKET));

        tank.fill(new FluidStack(slurry, 1), IFluidHandler.FluidAction.EXECUTE);
        assertTrue(CentrifugeContainerTransaction.transfer(tank, new ItemStack(Items.IRON_INGOT)
                .getCapability(Capabilities.FluidHandler.ITEM)).isEmpty());
        assertEquals(1000, tank.getFluidAmount());
        ItemStack filled = CentrifugeContainerTransaction.transfer(tank, item);
        assertTrue(filled.is(FuelProcessingContent.URANIUM_SLURRY_BUCKET.get()));
        assertEquals(0, tank.getFluidAmount());

        tank.fill(new FluidStack(slurry, 1500), IFluidHandler.FluidAction.EXECUTE);
        var fullItem = filled.getCapability(Capabilities.FluidHandler.ITEM);
        assertNotNull(fullItem);
        assertTrue(CentrifugeContainerTransaction.transfer(tank, fullItem).isEmpty());
        assertEquals(1500, tank.getFluidAmount());
        assertTrue(fullItem.getContainer().is(FuelProcessingContent.URANIUM_SLURRY_BUCKET.get()));

        assertEquals(1000, fullItem.getFluidInTank(0).getAmount());

        FluidTank emptyInput = new FluidTank(2000, fluid -> fluid.is(slurry));
        var wrongFluid = new ItemStack(Items.WATER_BUCKET).getCapability(Capabilities.FluidHandler.ITEM);
        assertNotNull(wrongFluid);
        assertTrue(CentrifugeContainerTransaction.transfer(emptyInput, wrongFluid).isEmpty());
        assertEquals(0, emptyInput.getFluidAmount());
        assertTrue(wrongFluid.getContainer().is(Items.WATER_BUCKET));
        assertTrue(CentrifugeContainerTransaction.transfer(emptyInput, fullItem).is(Items.BUCKET));
        assertEquals(1000, emptyInput.getFluidAmount());
    }
}
