package com.iksxh.create_nuclear_industry.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import java.util.List;

/** P1 方块实体共享的持久化壳；故意不包含反应堆模拟状态。 */
public abstract class P1MinimalBlockEntity extends SmartBlockEntity {
    protected P1MinimalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 正式 P1 实体可作为 Create behaviour 宿主，但不因此新增 ticker。 */
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("P1DataVersion", 1);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
    }

    /** 为服务端契约测试导出与真实保存路径相同的附加 NBT。 */
    public CompoundTag saveForServerTest(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    /** 为服务端契约测试走与真实加载路径相同的附加 NBT 读取。 */
    public void loadForServerTest(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }
}
