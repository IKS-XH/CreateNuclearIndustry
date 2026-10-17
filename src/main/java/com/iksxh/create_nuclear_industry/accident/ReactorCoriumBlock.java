package com.iksxh.create_nuclear_industry.accident;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 普通镐可回收的单件废料残骸；本体不掉免费废料，掉落仅来自方块实体真实库存。 */
public final class ReactorCoriumBlock extends BaseEntityBlock {
    public static final MapCodec<ReactorCoriumBlock> CODEC=simpleCodec(ReactorCoriumBlock::new);
    public ReactorCoriumBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ReactorCoriumBlockEntity(pos,state); }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof ReactorCoriumBlockEntity corium) {
            var payload=corium.take();if(!payload.isEmpty())Block.popResource(level,pos,payload);
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
