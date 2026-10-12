package com.iksxh.create_nuclear_industry.industrial;

import com.iksxh.create_nuclear_industry.content.IndustrialFluidContent;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 六向阀与同级管共用连接；开度与红石门控只削减流量，不持有库存。 */
public final class IndustrialValveBlock extends IndustrialPipeBlock implements IBE<IndustrialValveBlockEntity> {
    public IndustrialValveBlock(Properties properties, boolean ceramic) {
        super(properties, ceramic); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(BlockStateProperties.AXIS); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context).setValue(BlockStateProperties.AXIS, context.getNearestLookingDirection().getAxis());
        for (Direction direction : Direction.values()) if (direction.getAxis() != state.getValue(BlockStateProperties.AXIS)) state = state.setValue(CONNECTIONS[direction.ordinal()], false);
        return state;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos other) {
        return super.updateShape(state, direction, neighbor, level, pos, other).setValue(CONNECTIONS[direction.ordinal()],
                direction.getAxis() == state.getValue(BlockStateProperties.AXIS) && level instanceof Level world && IndustrialTopology.connectable(world, other, direction.getOpposite()));
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return com.simibubi.create.AllShapes.FLUID_VALVE.get(state.getValue(BlockStateProperties.AXIS));
    }
    public Class<IndustrialValveBlockEntity> getBlockEntityClass() { return IndustrialValveBlockEntity.class; }
    public BlockEntityType<? extends IndustrialValveBlockEntity> getBlockEntityType() { return IndustrialFluidContent.VALVE_ENTITY.get(); }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighbor, boolean moving) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof IndustrialValveBlockEntity valve) valve.updateSignal();
    }
}
