package com.iksxh.create_nuclear_industry.p0probe.block;

import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** P0 红石、NBT、锁定与修复回归探针方块，不是 P1 状态源。 */
public final class P0ProbeBlock extends BaseEntityBlock {
    public static final MapCodec<P0ProbeBlock> CODEC = simpleCodec(P0ProbeBlock::new);

    public P0ProbeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new P0ProbeBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.getBlockEntity(pos) instanceof P0ProbeBlockEntity probe) {
            probe.updateRedstone(level.hasNeighborSignal(pos));
        }
    }
}
