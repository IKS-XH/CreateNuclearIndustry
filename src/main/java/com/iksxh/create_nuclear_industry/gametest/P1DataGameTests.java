package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1DataGameTests {
    private static final String TEMPLATE = "p0_probe_empty";

    private P1DataGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void coldCoolantBucketPlacesAndPicksUpTheFormalLiquidBlock(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            BucketItem bucket = ModItems.COMPOUND_COOLANT_BUCKET.get();
            FluidStack contained = FluidUtil.getFluidContained(new ItemStack(bucket)).orElse(FluidStack.EMPTY);
            require(helper, bucket.content == ModFluids.COMPOUND_COOLANT_SOURCE.get(),
                    "coolant bucket is not bound to the cold source fluid");
            require(helper, contained.is(ModFluids.COMPOUND_COOLANT_SOURCE.get()),
                    "coolant bucket capability exposes the wrong fluid");
            require(helper, contained.getAmount() == FluidType.BUCKET_VOLUME,
                    "coolant bucket capacity is not one standard bucket");

            BlockPos fluidPos = new BlockPos(1, 1, 1);
            helper.setBlock(fluidPos, Blocks.AIR.defaultBlockState());
            FluidActionResult emptied = FluidUtil.tryPlaceFluid(
                    null,
                    helper.getLevel(),
                    InteractionHand.MAIN_HAND,
                    helper.absolutePos(fluidPos),
                    new ItemStack(bucket),
                    contained
            );
            require(helper, emptied.isSuccess(), "coolant bucket could not place its liquid block");
            require(helper, emptied.getResult().is(Items.BUCKET),
                    "placing coolant did not return an empty bucket");
            require(helper, helper.getBlockState(fluidPos).is(ModFluids.COMPOUND_COOLANT_BLOCK.get()),
                    "placed coolant did not use the compound_coolant liquid block");

            FluidActionResult refilled = FluidUtil.tryPickUpFluid(
                    new ItemStack(Items.BUCKET),
                    null,
                    helper.getLevel(),
                    helper.absolutePos(fluidPos),
                    Direction.UP
            );
            require(helper, refilled.isSuccess(), "empty bucket could not pick up the coolant source");
            require(helper, refilled.getResult().is(ModItems.COMPOUND_COOLANT_BUCKET.get()),
                    "picking up coolant did not return the formal coolant bucket");
            require(helper, helper.getBlockState(fluidPos).isAir(),
                    "picking up coolant did not remove the liquid source block");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void coldCoolantBucketFillsCreateFluidTankCapability(GameTestHelper helper) {
        BlockPos tankPos = new BlockPos(3, 1, 3);
        helper.setBlock(tankPos, AllBlocks.FLUID_TANK.get().defaultBlockState());
        helper.runAfterDelay(5, () -> {
            IFluidHandler tank = helper.getLevel().getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    helper.absolutePos(tankPos),
                    Direction.UP
            );
            require(helper, tank != null, "Create Fluid Tank did not expose a fluid capability");

            FluidActionResult result = FluidUtil.tryEmptyContainer(
                    new ItemStack(ModItems.COMPOUND_COOLANT_BUCKET.get()),
                    tank,
                    FluidType.BUCKET_VOLUME,
                    null,
                    true
            );
            require(helper, result.isSuccess(), "Create Fluid Tank rejected the coolant bucket");
            require(helper, result.getResult().is(Items.BUCKET),
                    "Create Fluid Tank did not return an empty bucket");
            require(helper, tank.getFluidInTank(0).is(ModFluids.COMPOUND_COOLANT_SOURCE.get()),
                    "Create Fluid Tank stored the wrong fluid identity");
            require(helper, tank.getFluidInTank(0).getAmount() == FluidType.BUCKET_VOLUME,
                    "Create Fluid Tank did not receive the complete bucket volume");

            FluidStack simulated = new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 1);
            require(helper, tank.fill(simulated, FluidAction.SIMULATE) == 1,
                    "Create Fluid Tank could not identify the formal coolant for a follow-up transfer");
            helper.succeed();
        });
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
