package com.iksxh.create_nuclear_industry.block;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** 控制棒驱动器方块；其交互行为由对应方块实体的 Create behaviour 提供。 */
public final class ControlRodDriveBlock extends P1EntityBlockBase {
    public static final MapCodec<ControlRodDriveBlock> CODEC = simpleCodec(ControlRodDriveBlock::new);

    public ControlRodDriveBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ControlRodDriveBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
