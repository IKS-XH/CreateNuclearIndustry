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
 * 仅前端发布总 SU；两端通过受同一完整性条件约束的内部传动相连。
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

    /** 返回指定端的生成 SU；红石和断供只撤销本机产能。 */
    public float shareFor(BlockPos shaftPos) {
        if (!shaftPos.equals(worldPosition.east()) || !supplied || totalSu <= 0
                || level.hasNeighborSignal(worldPosition) || !validAssembly()) return 0;
        return totalSu;
    }

    /** 结构完整且所有相关区块实际 tick 时提供双向内部邻接，停机仍保留轴传动。 */
    public BlockPos otherFor(BlockPos shaftPos) {
        if (!validAssembly()) return null;
        BlockPos front = worldPosition.east();
        BlockPos rear = front.south(5);
        return shaftPos.equals(front) ? rear : shaftPos.equals(rear) ? front : null;
    }

    private boolean validAssembly() {
        if (!(level instanceof ServerLevel server)) return false;
        BlockPos front = worldPosition.east();
        BlockPos rear = front.south(5);
        if (!loaded(server, worldPosition) || !loaded(server, front) || !loaded(server, rear)
                || !(level.getBlockEntity(front) instanceof TurbineProbeShaftBlockEntity)
                || !(level.getBlockEntity(rear) instanceof TurbineProbeShaftBlockEntity)) return false;
        for (int z = 1; z <= 4; z++) {
            BlockPos casing = front.east().south(z);
            if (!loaded(server, casing) || !level.getBlockState(casing).is(Blocks.IRON_BLOCK)) return false;
        }
        return true;
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
