package com.iksxh.create_nuclear_industry.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** P1 方块实体方块的共享数据壳，只提供模型渲染和实体创建约束。 */
public abstract class P1EntityBlockBase extends BaseEntityBlock {
    protected P1EntityBlockBase(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();
}
