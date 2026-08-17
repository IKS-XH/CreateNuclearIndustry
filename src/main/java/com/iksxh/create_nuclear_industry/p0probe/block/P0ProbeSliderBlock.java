package com.iksxh.create_nuclear_industry.p0probe.block;

import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeSliderBlockEntity;
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

public final class P0ProbeSliderBlock extends BaseEntityBlock {
    public static final MapCodec<P0ProbeSliderBlock> CODEC = simpleCodec(P0ProbeSliderBlock::new);

    public P0ProbeSliderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new P0ProbeSliderBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != P0ProbeBlockEntities.P0_PROBE_SLIDER.get())
            return null;
        return (tickLevel, pos, tickState, blockEntity) -> ((P0ProbeSliderBlockEntity) blockEntity).tick();
    }
}
