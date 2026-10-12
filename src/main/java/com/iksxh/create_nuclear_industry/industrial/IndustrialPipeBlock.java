package com.iksxh.create_nuclear_industry.industrial;

import com.iksxh.create_nuclear_industry.config.IndustrialFluidConfig;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 不存液的六向专用管段；连接几何与拓扑共用相邻可连接判断，原生管不参与专用路线。 */
public class IndustrialPipeBlock extends Block implements IWrenchable {
    public static final BooleanProperty[] CONNECTIONS = {BooleanProperty.create("down"), BooleanProperty.create("up"),
            BooleanProperty.create("north"), BooleanProperty.create("south"), BooleanProperty.create("west"), BooleanProperty.create("east")};
    private final boolean ceramic;
    public IndustrialPipeBlock(Properties properties, boolean ceramic) {
        super(properties); this.ceramic = ceramic;
        BlockState state = stateDefinition.any(); for (var property : CONNECTIONS) state = state.setValue(property, false);
        registerDefaultState(state);
    }
    public int flowLimit() { return (ceramic ? IndustrialFluidConfig.CERAMIC_FLOW : IndustrialFluidConfig.REINFORCED_FLOW).get(); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CONNECTIONS); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) state = state.setValue(CONNECTIONS[direction.ordinal()],
                IndustrialTopology.connectable(context.getLevel(), context.getClickedPos().relative(direction), direction.getOpposite()));
        return state;
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos other) {
        if (level instanceof ServerLevel server) IndustrialNetworkScheduler.invalidate(server, pos);
        return level instanceof Level world ? state.setValue(CONNECTIONS[direction.ordinal()], IndustrialTopology.connectable(world, other, direction.getOpposite())) : state;
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving); if (level instanceof ServerLevel server) IndustrialNetworkScheduler.invalidate(server, pos);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (level instanceof ServerLevel server) IndustrialNetworkScheduler.invalidate(server, pos); super.onRemove(state, level, pos, next, moving);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis singleAxis = null; boolean straight = true;
        for (Direction direction : Direction.values()) if (state.getValue(CONNECTIONS[direction.ordinal()])) {
            if (singleAxis == null) singleAxis = direction.getAxis(); else if (singleAxis != direction.getAxis()) straight = false;
        }
        // 0/单向/两端直线复用原item完整段：8像素管身，格边两端10像素、2像素厚法兰；不伪造连接。
        if (straight) return switch (singleAxis == null ? Direction.Axis.Z : singleAxis) {
            case X -> Shapes.or(box(0,4,4,16,12,12),box(0,3,3,2,13,13),box(14,3,3,16,13,13));
            case Y -> Shapes.or(box(4,0,4,12,16,12),box(3,0,3,13,2,13),box(3,14,3,13,16,13));
            case Z -> Shapes.or(box(4,4,0,12,12,16),box(3,3,0,13,13,2),box(3,3,14,13,13,16));
        };
        // 严格复用原Create的动态壳条件：任意轴之外超过两连接，才显示10像素casing。
        boolean casing = false;
        for (Direction.Axis axis : Direction.Axis.values()) {
            int outside = 0; for (Direction direction : Direction.values()) if (direction.getAxis() != axis && state.getValue(CONNECTIONS[direction.ordinal()])) outside++;
            if (outside > 2) casing = true;
        }
        VoxelShape shape = casing ? box(3,3,3,13,13,13) : box(4,4,4,12,12,12);
        for (Direction direction : Direction.values()) if (state.getValue(CONNECTIONS[direction.ordinal()])) shape = Shapes.or(shape, switch (direction) {
            case DOWN -> Shapes.or(box(4,0,4,12,4,12),box(3,0,3,13,2,13)); case UP -> Shapes.or(box(4,12,4,12,16,12),box(3,14,3,13,16,13));
            case NORTH -> Shapes.or(box(4,4,0,12,12,4),box(3,3,0,13,13,2)); case SOUTH -> Shapes.or(box(4,4,12,12,12,16),box(3,3,14,13,13,16));
            case WEST -> Shapes.or(box(0,4,4,4,12,12),box(0,3,3,2,13,13)); case EAST -> Shapes.or(box(12,4,4,16,12,12),box(14,3,3,16,13,13));
        });
        return shape;
    }
}
