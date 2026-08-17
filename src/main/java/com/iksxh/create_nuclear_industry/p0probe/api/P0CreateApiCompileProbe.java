package com.iksxh.create_nuclear_industry.p0probe.api;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

/**
 * Compile-only P0 probe. Nothing in this class is registered or invoked by the mod entry point.
 * Its purpose is to make accidental imports from old Forge/Create versions fail the build.
 */
public final class P0CreateApiCompileProbe {
    private P0CreateApiCompileProbe() {
    }

    public static IFluidHandler blockFluidCapability(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
    }

    public static IFluidHandler combineFluidHandlers(IFluidHandler... handlers) {
        return new CombinedTankWrapper(handlers);
    }

    public static ScrollValueBehaviour createServerValidatedSlider(String label, SmartBlockEntity blockEntity,
                                                                     ValueBoxTransform transform) {
        return new ScrollValueBehaviour(net.minecraft.network.chat.Component.literal(label), blockEntity, transform)
                .between(0, 100)
                .withCallback(value -> blockEntity.setChanged())
                .withClientCallback(value -> { });
    }

    public static boolean addServerSnapshotToGoggleTooltip(IHaveGoggleInformation information,
                                                            List<net.minecraft.network.chat.Component> tooltip,
                                                            boolean sneaking) {
        return information.addToGoggleTooltip(tooltip, sneaking);
    }

    public static ArmInteractionPoint locateArmPoint(Level level, BlockPos pos, BlockState state) {
        return ArmInteractionPoint.create(level, pos, state);
    }

    public static ArmInteractionPointType registerArmPointType(ResourceLocation id, ArmInteractionPointType type) {
        return Registry.register(CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE, id, type);
    }

    public static ItemStack simulateArmInsert(ArmInteractionPoint point, ArmBlockEntity arm, ItemStack stack) {
        return point.insert(arm, stack, true);
    }

    public static ItemStack executeArmInsert(ArmInteractionPoint point, ArmBlockEntity arm, ItemStack stack) {
        return point.insert(arm, stack, false);
    }

    public static ItemStack simulateArmExtract(ArmInteractionPoint point, ArmBlockEntity arm, int slot, int amount) {
        return point.extract(arm, slot, amount, true);
    }

    public static boolean rightClickIsServerSide(PlayerInteractEvent.RightClickBlock event) {
        return event.getSide().isServer();
    }

    public static boolean cancelBreakBeforeDrops(BlockEvent.BreakEvent event) {
        event.setCanceled(true);
        return event.isCanceled();
    }

    public static boolean readRedstone(Level level, BlockPos pos) {
        return level.hasNeighborSignal(pos);
    }

    public static void registerEventListener(IEventBus modEventBus) {
        modEventBus.addListener((PlayerInteractEvent.RightClickBlock event) -> { });
        modEventBus.addListener((BlockEvent.BreakEvent event) -> { });
    }

    public static ClientboundBlockEntityDataPacket updatePacket(SyncedBlockEntity blockEntity) {
        return blockEntity.getUpdatePacket();
    }

    public static final class NbtProbeBlockEntity extends SyncedBlockEntity {
        public NbtProbeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.putDouble("P0ProbeValue", 1.0);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            tag.getDouble("P0ProbeValue");
        }
    }

    public static final class RedstoneProbeBlock extends Block {
        public RedstoneProbeBlock(Properties properties) {
            super(properties);
        }

        @Override
        public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                     BlockPos fromPos, boolean isMoving) {
            if (!level.isClientSide && level.hasNeighborSignal(pos)) {
                level.getBlockTicks();
            }
        }
    }

    public static final class ArmPointTypeProbe extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return false;
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return null;
        }
    }
}
