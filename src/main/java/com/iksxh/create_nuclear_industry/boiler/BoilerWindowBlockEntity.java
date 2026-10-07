package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 窗口只保存显示比例，不提供流体能力，也不保存任何第二份库存或热。 */
public final class BoilerWindowBlockEntity extends BlockEntity {
    private float fill;
    public BoilerWindowBlockEntity(BlockPos pos, BlockState state) { super(BoilerContent.WINDOW_BE.get(), pos, state); }
    public float fill() { return fill; }
    public void setFill(float value) {
        value = Math.clamp(value, 0, 1);
        if (Math.abs(value - fill) < .002f) return;
        fill = value;
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { CompoundTag t = new CompoundTag(); t.putFloat("Fill", fill); return t; }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); fill = Math.clamp(tag.getFloat("Fill"), 0, 1); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
