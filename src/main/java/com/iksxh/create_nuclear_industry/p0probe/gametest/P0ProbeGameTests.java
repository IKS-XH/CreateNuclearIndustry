package com.iksxh.create_nuclear_industry.p0probe.gametest;

import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeArmTargetBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeFluidPortBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeGoggleBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeSliderBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeContent;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeFluids;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import com.simibubi.create.AllBlocks;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P0ProbeGameTests {
    private static final String TEMPLATE = "p0_probe_empty";

    private P0ProbeGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void probeBlockNbtRedstoneAndBreak(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        helper.setBlock(relative, P0ProbeContent.P0_PROBE_BLOCK.get().defaultBlockState());
        helper.runAfterDelay(2, () -> {
            P0ProbeBlockEntity probe = requireBlockEntity(helper, relative, P0ProbeBlockEntity.class);
            probe.setCounter(17);
            probe.setBreakLocked(true);
            probe.setIntegrity(0.5F);

            CompoundTag saved = probe.p0Save(helper.getLevel().registryAccess());
            probe.setCounter(0);
            probe.setBreakLocked(false);
            probe.setIntegrity(1.0F);
            probe.p0Load(saved, helper.getLevel().registryAccess());
            require(helper, probe.counter() == 17, "P0 block counter did not round-trip through NBT");
            require(helper, probe.breakLocked(), "P0 break lock did not round-trip through NBT");
            require(helper, Math.abs(probe.integrity() - 0.5F) < 0.001F, "P0 integrity did not round-trip through NBT");

            BlockPos absolute = helper.absolutePos(relative);
            BlockPos power = relative.west();
            helper.setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
            helper.runAfterDelay(2, () -> {
                require(helper, probe.redstonePowered(), "P0 probe did not observe neighbor redstone power");

                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                BlockState state = helper.getLevel().getBlockState(absolute);
                BlockEvent.BreakEvent lockedBreak = new BlockEvent.BreakEvent(helper.getLevel(), absolute, state, player);
                NeoForge.EVENT_BUS.post(lockedBreak);
                require(helper, lockedBreak.isCanceled(), "Locked P0 probe break was not intercepted");

                probe.setBreakLocked(false);
                BlockEvent.BreakEvent openBreak = new BlockEvent.BreakEvent(helper.getLevel(), absolute, state, player);
                NeoForge.EVENT_BUS.post(openBreak);
                require(helper, !openBreak.isCanceled(), "Unlocked P0 probe break remained intercepted");
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void probeSliderServerValueAndSyncTag(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        helper.setBlock(relative, P0ProbeContent.P0_PROBE_SLIDER_BLOCK.get().defaultBlockState());
        helper.runAfterDelay(2, () -> {
            P0ProbeSliderBlockEntity slider = requireBlockEntity(helper, relative, P0ProbeSliderBlockEntity.class);
            slider.slider.setValue(73);
            require(helper, slider.value() == 73, "Create slider callback did not set server value");

            CompoundTag updateTag = slider.getUpdateTag(helper.getLevel().registryAccess());
            slider.slider.setValue(0);
            slider.handleUpdateTag(updateTag, helper.getLevel().registryAccess());
            require(helper, slider.value() == 73, "Slider value was not present in the synced update tag");

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            slider.slider.setValueSettings(player, new ValueSettings(0, 41), false);
            require(helper, slider.value() == 41, "Create ValueSettings server path did not update slider");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void probeGoggleInformation(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        helper.setBlock(relative, P0ProbeContent.P0_PROBE_GOGGLE_BLOCK.get().defaultBlockState());
        helper.runAfterDelay(2, () -> {
            P0ProbeGoggleBlockEntity goggle = requireBlockEntity(helper, relative, P0ProbeGoggleBlockEntity.class);
            List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
            boolean displayed = ((IHaveGoggleInformation) goggle).addToGoggleTooltip(tooltip, false);
            require(helper, displayed, "P0 goggle provider did not opt in to tooltip display");
            require(helper, tooltip.stream().anyMatch(component -> component.getString().contains("detailed information")),
                    "P0 goggle tooltip did not contain detailed information");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void probeArmItemTransactionAndRollback(GameTestHelper helper) {
        BlockPos target = new BlockPos(2, 1, 2);
        BlockPos armPos = new BlockPos(2, 1, 4);
        helper.setBlock(target, P0ProbeContent.P0_PROBE_ARM_TARGET.get().defaultBlockState());
        helper.setBlock(armPos, AllBlocks.MECHANICAL_ARM.get().defaultBlockState());
        helper.runAfterDelay(3, () -> {
            P0ProbeArmTargetBlockEntity targetEntity = requireBlockEntity(helper, target, P0ProbeArmTargetBlockEntity.class);
            IItemHandler handler = helper.getLevel().getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(target),
                    Direction.UP
            );
            require(helper, handler != null, "P0 arm target ItemHandler capability was not exposed");

            BlockEntity armEntity = helper.getBlockEntity(armPos);
            require(helper, armEntity instanceof ArmBlockEntity, "Create mechanical arm BlockEntity was not created");
            ArmInteractionPoint point = ArmInteractionPoint.create(
                    helper.getLevel(),
                    helper.absolutePos(target),
                    helper.getLevel().getBlockState(helper.absolutePos(target))
            );
            require(helper, point != null, "Create did not create an ArmInteractionPoint for P0 arm target");

            ItemStack input = new ItemStack(Items.IRON_INGOT, 3);
            ItemStack simulatedRemainder = point.insert((ArmBlockEntity) armEntity, input, true);
            require(helper, simulatedRemainder.isEmpty(), "Arm simulation incorrectly rejected a valid input");
            require(helper, handler.getStackInSlot(0).isEmpty(), "Arm simulation mutated the target inventory");

            ItemStack executedRemainder = point.insert((ArmBlockEntity) armEntity, input, false);
            require(helper, executedRemainder.isEmpty(), "Arm execute unexpectedly returned a remainder");
            require(helper, handler.getStackInSlot(0).getCount() == 3, "Arm execute did not commit the item transaction");

            ItemStack rollback = point.insert((ArmBlockEntity) armEntity, new ItemStack(Items.GOLD_INGOT), false);
            require(helper, rollback.getCount() == 1, "Arm rejected-item rollback lost the remainder");
            require(helper, handler.getStackInSlot(0).getCount() == 3, "Arm rejected-item rollback mutated the target");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void probeFluidCapabilitiesAndMultiPortAggregation(GameTestHelper helper) {
        BlockPos coldA = new BlockPos(1, 1, 2);
        BlockPos coldB = new BlockPos(2, 1, 2);
        BlockPos hot = new BlockPos(3, 1, 2);
        helper.setBlock(coldA, P0ProbeContent.P0_PROBE_COLD_PORT.get().defaultBlockState());
        helper.setBlock(coldB, P0ProbeContent.P0_PROBE_COLD_PORT.get().defaultBlockState());
        helper.setBlock(hot, P0ProbeContent.P0_PROBE_HOT_PORT.get().defaultBlockState());
        helper.runAfterDelay(2, () -> {
            IFluidHandler coldHandlerA = fluidHandler(helper, coldA);
            IFluidHandler coldHandlerB = fluidHandler(helper, coldB);
            IFluidHandler hotHandler = fluidHandler(helper, hot);
            CombinedTankWrapper combined = new CombinedTankWrapper(coldHandlerA, coldHandlerB);
            FluidStack cold = new FluidStack(P0ProbeFluids.COLD_SOURCE.get(), 1500);

            require(helper, combined.fill(cold, FluidAction.SIMULATE) == 1500, "Cold multi-port simulation did not aggregate capacity");
            require(helper, coldHandlerA.getFluidInTank(0).isEmpty() && coldHandlerB.getFluidInTank(0).isEmpty(),
                    "Cold multi-port simulation mutated a port");
            require(helper, combined.fill(cold, FluidAction.EXECUTE) == 1500, "Cold multi-port execute lost fluid");
            require(helper, coldHandlerA.getFluidInTank(0).getAmount() == 1000, "First cold port did not receive its capacity");
            require(helper, coldHandlerB.getFluidInTank(0).getAmount() == 500, "Second cold port did not receive the remainder");

            P0ProbeFluidPortBlockEntity hotEntity = requireBlockEntity(helper, hot, P0ProbeFluidPortBlockEntity.class);
            hotEntity.setTestFluid(new FluidStack(P0ProbeFluids.HOT_SOURCE.get(), 700));
            require(helper, hotHandler.fill(new FluidStack(P0ProbeFluids.HOT_SOURCE.get(), 100), FluidAction.EXECUTE) == 0,
                    "Hot output accepted input fluid");
            require(helper, hotHandler.drain(400, FluidAction.SIMULATE).getAmount() == 400,
                    "Hot output simulation did not expose available fluid");
            require(helper, hotHandler.drain(400, FluidAction.EXECUTE).getAmount() == 400,
                    "Hot output execute did not drain fluid");
            require(helper, hotHandler.drain(1000, FluidAction.SIMULATE).getAmount() == 300,
                    "Hot output amount after drain was incorrect");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void probeRepairRightClickItemInteraction(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        helper.setBlock(relative, P0ProbeContent.P0_PROBE_BLOCK.get().defaultBlockState());
        helper.runAfterDelay(2, () -> {
            P0ProbeBlockEntity probe = requireBlockEntity(helper, relative, P0ProbeBlockEntity.class);
            probe.setIntegrity(0.5F);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(P0ProbeContent.P0_PROBE_REPAIR_ITEM.get()));
            BlockPos absolute = helper.absolutePos(relative);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
            PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(
                    player,
                    InteractionHand.MAIN_HAND,
                    absolute,
                    hit
            );
            NeoForge.EVENT_BUS.post(event);
            require(helper, event.isCanceled(), "P0 repair right-click was not consumed");
            require(helper, event.getCancellationResult().consumesAction(), "P0 repair did not return a successful interaction");
            require(helper, Math.abs(probe.integrity() - 0.75F) < 0.001F, "P0 repair did not increase integrity by one plate");
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "P0 repair item was not consumed");
            helper.succeed();
        });
    }

    private static IFluidHandler fluidHandler(GameTestHelper helper, BlockPos pos) {
        IFluidHandler handler = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(pos),
                Direction.UP
        );
        require(helper, handler != null, "Fluid capability missing at " + pos);
        return handler;
    }

    private static <T extends BlockEntity> T requireBlockEntity(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        require(helper, type.isInstance(blockEntity), "Expected " + type.getSimpleName() + " at " + pos + ", got " + blockEntity);
        return type.cast(blockEntity);
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition)
            helper.fail(message);
    }
}
