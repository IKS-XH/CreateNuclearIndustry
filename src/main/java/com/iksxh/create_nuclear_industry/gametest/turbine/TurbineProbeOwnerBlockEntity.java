package com.iksxh.create_nuclear_industry.gametest.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 探针唯一服务端账本。前端位于机主东侧，后端向南五格；东侧四格模拟独立机身。
 * 两端只能各取总 SU 的一半，任何轴或机身所在区块失载时两端均返回零。
 */
public final class TurbineProbeOwnerBlockEntity extends BlockEntity {
    private float totalSu = 32768;
    private boolean supplied = true;

    public TurbineProbeOwnerBlockEntity(BlockPos pos, BlockState state) {
        super(TurbineProbeContent.ownerEntity(), pos, state);
    }

    public void setSupply(float su, boolean supplied) {
        this.totalSu = su;
        this.supplied = supplied;
        setChanged();
    }

    /** 返回指定端的 SU。每次查询验证实际加载、红石和完整机身，避免保留失效快照。 */
    public float shareFor(BlockPos shaftPos) {
        if (!(level instanceof ServerLevel server) || !supplied || totalSu <= 0
                || level.hasNeighborSignal(worldPosition)) return 0;
        BlockPos front = worldPosition.east();
        BlockPos rear = front.south(5);
        if (!shaftPos.equals(front) && !shaftPos.equals(rear)) return 0;
        if (!loaded(server, worldPosition) || !loaded(server, front) || !loaded(server, rear)
                || !(level.getBlockEntity(front) instanceof TurbineProbeShaftBlockEntity)
                || !(level.getBlockEntity(rear) instanceof TurbineProbeShaftBlockEntity)) return 0;
        for (int z = 1; z <= 4; z++) {
            BlockPos casing = front.east().south(z);
            if (!loaded(server, casing) || !level.getBlockState(casing).is(Blocks.IRON_BLOCK)) return 0;
        }
        return totalSu * 0.5f;
    }

    private static boolean loaded(ServerLevel server, BlockPos pos) {
        return server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null
                && server.shouldTickBlocksAt(pos);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("TotalSu", totalSu);
        tag.putBoolean("Supplied", supplied);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        totalSu = tag.getFloat("TotalSu");
        supplied = tag.getBoolean("Supplied");
    }
}
