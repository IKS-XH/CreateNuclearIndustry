package com.iksxh.create_nuclear_industry.gametest.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 测试专用机主方块；动力状态只存于对应实体。 */
public final class TurbineProbeOwnerBlock extends Block implements EntityBlock {
    public TurbineProbeOwnerBlock(Properties properties) { super(properties); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbineProbeOwnerBlockEntity(pos, state);
    }
}
