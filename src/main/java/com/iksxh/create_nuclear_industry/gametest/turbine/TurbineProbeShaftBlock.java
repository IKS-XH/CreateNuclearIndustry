package com.iksxh.create_nuclear_industry.gametest.turbine;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** 测试专用实体轴，两端使用同一类型而通过 REAR 计算唯一 owner 位置。 */
public final class TurbineProbeShaftBlock extends RotatedPillarKineticBlock implements IBE<TurbineProbeShaftBlockEntity> {
    public static final BooleanProperty REAR = BooleanProperty.create("rear");

    public TurbineProbeShaftBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(REAR, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(REAR));
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public Class<TurbineProbeShaftBlockEntity> getBlockEntityClass() {
        return TurbineProbeShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TurbineProbeShaftBlockEntity> getBlockEntityType() {
        return TurbineProbeContent.shaftEntity();
    }
}
