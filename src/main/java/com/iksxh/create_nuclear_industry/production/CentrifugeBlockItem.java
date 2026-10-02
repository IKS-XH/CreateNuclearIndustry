package com.iksxh.create_nuclear_industry.production;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;

/** 单件放置上下两格；由 NeoForge 对两个 BlockSnapshot 统一做权限事件和取消回滚。 */
public final class CentrifugeBlockItem extends BlockItem {
    public CentrifugeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState lower = super.getPlacementState(context);
        if (lower == null) return null;
        Level level = context.getLevel();
        BlockPos upperPos = context.getClickedPos().above();
        if (level.isOutsideBuildHeight(upperPos) || !level.getBlockState(upperPos).canBeReplaced(context))
            return null;
        Player player = context.getPlayer();
        CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        BlockState upper = lower.setValue(CentrifugeBlock.HALF, DoubleBlockHalf.UPPER);
        return level.isUnobstructed(upper, upperPos, collision) ? lower : null;
    }

    @Override protected boolean placeBlock(BlockPlaceContext context, BlockState lower) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos upperPos = pos.above();
        BlockState upper = lower.setValue(CentrifugeBlock.HALF, DoubleBlockHalf.UPPER);
        BlockState oldLower = level.getBlockState(pos);
        if (!level.setBlock(pos, lower, 11)) return false;
        if (level.setBlock(upperPos, upper, 11)) return true;
        // 常规失败返回时 CommonHooks 不负责回滚；这里只撤销本次尚未提交的下段。
        level.setBlock(pos, oldLower, 11);
        return false;
    }
}
