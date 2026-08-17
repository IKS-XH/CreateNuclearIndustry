package com.iksxh.create_nuclear_industry.p0probe.block;

import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeFluidPortBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class P0ProbeFluidPortBlock extends BaseEntityBlock {
    public static final MapCodec<P0ProbeFluidPortBlock> CODEC = simpleCodec(properties -> new P0ProbeFluidPortBlock(properties, true));
    private final boolean cold;

    public P0ProbeFluidPortBlock(BlockBehaviour.Properties properties, boolean cold) {
        super(properties);
        this.cold = cold;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new P0ProbeFluidPortBlockEntity(pos, state, cold);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
