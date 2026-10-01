package com.iksxh.create_nuclear_industry.production;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 使用 NeoForge 锁定版的实际桶包装器与流体罐检验整桶事务。 */
final class CentrifugeContainerTransactionTest {
    private static ItemStack exchange(FluidTank tank, ItemStack held) {
        return CentrifugeContainerTransaction.transfer(tank, new FluidBucketWrapper(held.copy()));
    }

    @Test
    void fullBucketNeedsWholeSpaceAndCorrectFluid() {
        FluidTank tank = new FluidTank(4000, fluid -> fluid.is(Fluids.WATER));
        tank.fill(new FluidStack(Fluids.WATER, 3500), IFluidHandler.FluidAction.EXECUTE);
        ItemStack held = new ItemStack(Items.WATER_BUCKET);
        assertTrue(exchange(tank, held).isEmpty());
        assertEquals(3500, tank.getFluidAmount());
        assertTrue(held.is(Items.WATER_BUCKET));

        tank.setFluid(FluidStack.EMPTY);
        FluidTank wrongFluid = new FluidTank(4000, fluid -> fluid.is(Fluids.LAVA));
        assertTrue(exchange(wrongFluid, held).isEmpty());
        assertEquals(0, wrongFluid.getFluidAmount());
        assertTrue(held.is(Items.WATER_BUCKET));

        ItemStack result = exchange(tank, held);
        assertTrue(result.is(Items.BUCKET));
        assertEquals(1000, tank.getFluidAmount());
    }

    @Test
    void emptyBucketNeedsFullAvailableBucket() {
        FluidTank tank = new FluidTank(4000, fluid -> fluid.is(Fluids.WATER));
        ItemStack held = new ItemStack(Items.BUCKET);
        assertTrue(exchange(tank, held).isEmpty());
        assertTrue(held.is(Items.BUCKET));
        tank.fill(new FluidStack(Fluids.WATER, 999), IFluidHandler.FluidAction.EXECUTE);
        assertTrue(exchange(tank, held).isEmpty());
        assertEquals(999, tank.getFluidAmount());
        assertTrue(held.is(Items.BUCKET));
        tank.fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE);
        ItemStack result = exchange(tank, held);
        assertTrue(result.is(Items.WATER_BUCKET));
        assertEquals(0, tank.getFluidAmount());
    }
}
