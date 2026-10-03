package com.iksxh.create_nuclear_industry.boiler;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;

/** 水汽口只有位置身份，真实能力与库存始终由控制器代理。 */
public final class BoilerPortBlock extends BaseEntityBlock {
    private static final MapCodec<BoilerPortBlock> WATER_CODEC = simpleCodec(p -> new BoilerPortBlock(p, true));
    private static final MapCodec<BoilerPortBlock> STEAM_CODEC = simpleCodec(p -> new BoilerPortBlock(p, false));
    private final boolean water;
    public BoilerPortBlock(Properties properties, boolean water) {
        super(properties);
        this.water = water;
        registerDefaultState(defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return water ? WATER_CODEC : STEAM_CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BoilerPartBlock.FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BoilerPartBlock.FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BoilerPortBlockEntity(pos, state); }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide) BoilerStructure.invalidateNearby(level, pos);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide && !state.is(next.getBlock())) BoilerStructure.invalidateNearby(level, pos);
        super.onRemove(state, level, pos, next, moving);
    }
}
