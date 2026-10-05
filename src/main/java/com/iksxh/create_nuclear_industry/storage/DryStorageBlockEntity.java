package com.iksxh.create_nuclear_industry.storage;

import com.iksxh.create_nuclear_industry.config.SpentFuelStorageConfig;
import com.iksxh.create_nuclear_industry.content.SpentFuelStorageContent;
import com.iksxh.create_nuclear_industry.goggle.GoggleTooltip;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/** 无动力贮存架唯一服务端库存；各面访问同一十六槽账本，客户端只显示同步数量。 */
public final class DryStorageBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private final DryStorageState state = new DryStorageState();
    private int displayedCapacity = 16;
    public DryStorageBlockEntity(BlockPos pos, BlockState blockState) {
        super(SpentFuelStorageContent.DRY_STORAGE_BE.get(), pos, blockState);
    }
    public DryStorageState state() { return state; }
    private boolean current() { return level != null && !level.isClientSide && !isRemoved()
            && level.getBlockEntity(worldPosition) == this && getBlockState().is(SpentFuelStorageContent.DRY_STORAGE_RACK.get()); }
    public IItemHandler itemPort(Direction side) { return side == null ? null : new Port(side); }
    public static void serverTick(Level level, BlockPos pos, BlockState blockState, DryStorageBlockEntity rack) {
        if (rack.displayedCapacity != SpentFuelStorageConfig.rackSlots()) rack.changed();
    }
    /** 同步完整内容与容量，方块外观由权威库存比例派生，绝不反向推算桶数。 */
    public void changed() {
        if (!current()) return;
        displayedCapacity = SpentFuelStorageConfig.rackSlots();
        setChanged();
        BlockState before = getBlockState();
        int visible = state.storageLevel(displayedCapacity);
        if (before.getValue(DryStorageBlock.STORAGE_LEVEL) != visible)
            level.setBlock(worldPosition, before.setValue(DryStorageBlock.STORAGE_LEVEL, visible), 3);
        level.sendBlockUpdated(worldPosition, before, getBlockState(), 3);
    }
    public boolean insertOne(ItemStack offered) {
        if (!current()) return false;
        for (int slot = 0; slot < DryStorageState.MAX_SLOTS; slot++)
            if (state.insert(slot, offered, SpentFuelStorageConfig.rackSlots(), false).isEmpty()) { changed(); return true; }
        return false;
    }
    /** 先确认玩家背包接纳，后领取最后一个占用槽；满背包不会扣除桶。 */
    public boolean takeToPlayer(Player player) {
        if (!current()) return false;
        int slot = state.lastOccupied();
        ItemStack remaining = state.item(slot);
        if (remaining.isEmpty()) return false;
        player.getInventory().add(remaining);
        if (!remaining.isEmpty()) return false;
        state.extract(slot, 1, false);
        changed();
        return true;
    }
    /** 所有真实拆除入口领取同一份内容；清空后重复回调不会再次掉桶。 */
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (ItemStack item : state.drain()) Block.popResource(level, worldPosition, item);
        setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("DryStorage", state.save(registries));
        tag.putInt("Capacity", displayedCapacity);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state.load(tag.getCompound("DryStorage"), registries);
        displayedCapacity = Math.clamp(tag.getInt("Capacity"), 1, 16);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(GoggleTooltip.indentFirstLine(Component.translatable("block.create_nuclear_industry.dry_storage_rack")));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.storage.occupied", state.used(), displayedCapacity));
        tooltip.add(Component.translatable("gui.create_nuclear_industry.storage.sealed"));
        return true;
    }
    private final class Port implements IItemHandler {
        private final Direction side;
        private Port(Direction side) { this.side = side; }
        @Override public int getSlots() { return current() ? DryStorageState.MAX_SLOTS : 0; }
        @Override public ItemStack getStackInSlot(int slot) { return current() ? state.item(slot) : ItemStack.EMPTY; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) return stack.copy();
            ItemStack remainder = state.insert(slot, stack, SpentFuelStorageConfig.rackSlots(), simulate);
            if (!simulate && remainder.getCount() != stack.getCount()) changed();
            return remainder;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!current()) return ItemStack.EMPTY;
            ItemStack result = state.extract(slot, amount, simulate);
            if (!simulate && !result.isEmpty()) changed();
            return result;
        }
        @Override public int getSlotLimit(int slot) { return current() && slot >= 0 && slot < DryStorageState.MAX_SLOTS ? 1 : 0; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return current() && side != Direction.DOWN && slot >= 0 && slot < SpentFuelStorageConfig.rackSlots()
                    && SpentFuelPayload.isValid(stack);
        }
    }
}
