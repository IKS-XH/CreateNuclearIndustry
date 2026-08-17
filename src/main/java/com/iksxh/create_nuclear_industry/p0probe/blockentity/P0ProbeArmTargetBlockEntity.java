package com.iksxh.create_nuclear_industry.p0probe.blockentity;

import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class P0ProbeArmTargetBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public P0ProbeArmTargetBlockEntity(BlockPos pos, BlockState state) {
        super(P0ProbeBlockEntities.P0_PROBE_ARM_TARGET.get(), pos, state);
    }

    public IItemHandler itemHandler() {
        return inventory;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("P0Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("P0Inventory"))
            inventory.deserializeNBT(registries, tag.getCompound("P0Inventory"));
    }
}
