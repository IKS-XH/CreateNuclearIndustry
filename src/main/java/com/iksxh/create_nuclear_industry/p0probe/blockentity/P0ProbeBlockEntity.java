package com.iksxh.create_nuclear_industry.p0probe.blockentity;

import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** P0 红石、锁定、完整度和 NBT 同步探针实体，不属于正式 P1 状态模型。 */
public final class P0ProbeBlockEntity extends SyncedBlockEntity {
    private int counter;
    private boolean redstonePowered;
    private boolean breakLocked = true;
    private float integrity = 1.0F;

    public P0ProbeBlockEntity(BlockPos pos, BlockState state) {
        super(P0ProbeBlockEntities.P0_PROBE.get(), pos, state);
    }

    public void setCounter(int counter) {
        this.counter = counter;
        notifyUpdate();
    }

    public int counter() {
        return counter;
    }

    public void updateRedstone(boolean powered) {
        if (redstonePowered == powered)
            return;
        redstonePowered = powered;
        notifyUpdate();
    }

    public boolean redstonePowered() {
        return redstonePowered;
    }

    public void setBreakLocked(boolean breakLocked) {
        this.breakLocked = breakLocked;
        notifyUpdate();
    }

    public boolean breakLocked() {
        return breakLocked;
    }

    public void setIntegrity(float integrity) {
        this.integrity = Math.max(0.0F, Math.min(1.0F, integrity));
        notifyUpdate();
    }

    public float integrity() {
        return integrity;
    }

    /** 使用一个 P0 修复物品增加 0.25 完整度，并将结果限制在 [0,1]。 */
    public boolean repairByP0Item() {
        if (integrity >= 1.0F)
            return false;
        setIntegrity(integrity + 0.25F);
        return true;
    }

    /** 导出 P0 回归测试使用的附加 NBT。 */
    public CompoundTag p0Save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    /** 读取 P0 回归测试提供的附加 NBT，并保留旧字段默认值策略。 */
    public void p0Load(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("P0Counter", counter);
        tag.putBoolean("P0RedstonePowered", redstonePowered);
        tag.putBoolean("P0BreakLocked", breakLocked);
        tag.putFloat("P0Integrity", integrity);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        counter = tag.getInt("P0Counter");
        redstonePowered = tag.getBoolean("P0RedstonePowered");
        breakLocked = !tag.contains("P0BreakLocked") || tag.getBoolean("P0BreakLocked");
        integrity = Math.max(0.0F, Math.min(1.0F, tag.contains("P0Integrity") ? tag.getFloat("P0Integrity") : 1.0F));
    }
}
