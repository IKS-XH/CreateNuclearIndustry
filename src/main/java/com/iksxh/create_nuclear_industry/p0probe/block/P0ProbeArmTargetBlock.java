package com.iksxh.create_nuclear_industry.p0probe.block;

import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeArmTargetBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** P0 机械臂物品事务探针方块，不参与正式反应堆结构。 */
public final class P0ProbeArmTargetBlock extends BaseEntityBlock {
    public static final MapCodec<P0ProbeArmTargetBlock> CODEC = simpleCodec(P0ProbeArmTargetBlock::new);

    public P0ProbeArmTargetBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new P0ProbeArmTargetBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
