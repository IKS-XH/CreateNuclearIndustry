package com.iksxh.create_nuclear_industry.p0probe.block;

import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeGoggleBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** P0 Create 护目镜信息回归探针方块，只验证显示适配边界。 */
public final class P0ProbeGoggleBlock extends BaseEntityBlock {
    public static final MapCodec<P0ProbeGoggleBlock> CODEC = simpleCodec(P0ProbeGoggleBlock::new);

    public P0ProbeGoggleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new P0ProbeGoggleBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
