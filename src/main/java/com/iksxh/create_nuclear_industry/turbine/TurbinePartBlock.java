package com.iksxh.create_nuclear_industry.turbine;

import java.util.Locale;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * 六种构件共享的世界朝向、成型角色与逐格碰撞合同；库存和动力均不在普通构件上。
 * 成型角色由服务端结构核验后写入，不接受客户端外观状态作为运行凭据。
 */
public abstract class TurbinePartBlock extends Block {
    public enum Kind { CASING, ROTOR, INLET, EXHAUST }
    public enum RingRole implements StringRepresentable {
        TOP, UPPER_LEFT, UPPER_RIGHT, LEFT, RIGHT, LOWER_LEFT, LOWER_RIGHT, BOTTOM;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    public enum AxialRole implements StringRepresentable {
        FRONT, MIDDLE, REAR;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final DirectionProperty MACHINE_FACING = DirectionProperty.create("machine_facing", Direction.Plane.HORIZONTAL);
    public static final EnumProperty<RingRole> RING_ROLE = EnumProperty.create("ring_role", RingRole.class);
    public static final EnumProperty<AxialRole> AXIAL_ROLE = EnumProperty.create("axial_role", AxialRole.class);
    public static final DirectionProperty OUTWARD = DirectionProperty.create("outward", d -> d != Direction.DOWN);
    private static final VoxelShape[][] FORMED_CASING = new VoxelShape[RingRole.values().length][4];
    private static final VoxelShape[] FORMED_ROTOR = new VoxelShape[4];
    private static final VoxelShape[][] FORMED_PORT = new VoxelShape[RingRole.values().length][4];
    static {
        for (RingRole role : RingRole.values()) for (Direction facing : Direction.Plane.HORIZONTAL) {
            FORMED_CASING[role.ordinal()][facing.get2DDataValue()] = casingShape(role, facing);
            FORMED_PORT[role.ordinal()][facing.get2DDataValue()] = portShape(role, facing);
        }
        for (Direction facing : Direction.Plane.HORIZONTAL)
            FORMED_ROTOR[facing.get2DDataValue()] = rotorShape(facing);
    }
    protected abstract Kind kind();

    protected TurbinePartBlock(Properties properties) {
        super(properties.noOcclusion());
        Kind kind = kind();
        BlockState state = defaultBlockState().setValue(FORMED, false).setValue(MACHINE_FACING, Direction.NORTH);
        if (kind == Kind.CASING) state = state.setValue(RING_ROLE, RingRole.TOP).setValue(AXIAL_ROLE, AxialRole.MIDDLE);
        if (kind == Kind.INLET || kind == Kind.EXHAUST)
            state = state.setValue(RING_ROLE, RingRole.TOP).setValue(OUTWARD, Direction.UP);
        registerDefaultState(state);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, MACHINE_FACING);
        Kind kind = kind();
        if (kind == Kind.CASING) builder.add(RING_ROLE, AXIAL_ROLE);
        if (kind == Kind.INLET || kind == Kind.EXHAUST) builder.add(RING_ROLE, OUTWARD);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Kind kind = kind();
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(MACHINE_FACING, facing);
        if (kind == Kind.INLET || kind == Kind.EXHAUST) {
            Direction outward = context.getClickedFace() == Direction.DOWN ? Direction.UP : context.getClickedFace();
            state = state.setValue(OUTWARD, outward);
        }
        return state;
    }

    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }

    /** 成型筒体按本格可见截面碰撞，角格阶梯近似斜面，避免不可见整格阻挡。 */
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(FORMED)) return Shapes.block();
        int facing = state.getValue(MACHINE_FACING).get2DDataValue();
        return switch (kind()) {
            case CASING -> FORMED_CASING[state.getValue(RING_ROLE).ordinal()][facing];
            case ROTOR -> FORMED_ROTOR[facing];
            case INLET, EXHAUST -> FORMED_PORT[state.getValue(RING_ROLE).ordinal()][facing];
        };
    }

    private static VoxelShape rotorShape(Direction facing) {
        VoxelShape shape = Shapes.empty();
        double radius = 6.72;
        for (int i = 0; i < 8; i++) {
            double x0 = 8 - radius + i * radius / 4, x1 = x0 + radius / 4;
            double edge = Math.max(Math.abs(x0 - 8), Math.abs(x1 - 8));
            double halfY = Math.sqrt(radius * radius - edge * edge);
            if (halfY > 0) shape = Shapes.or(shape,
                    boxRotated(x0, 8 - halfY, .64, x1, 8 + halfY, 15.36, facing));
        }
        return shape.optimize();
    }

    private static VoxelShape portShape(RingRole role, Direction facing) {
        return switch (role) {
            case LEFT -> Shapes.or(boxRotated(0, 2.88, 2.88, 16, 13.12, 13.12, facing),
                    sidePortPlate(14.72, 16, facing)).optimize();
            case RIGHT -> Shapes.or(boxRotated(0, 2.88, 2.88, 16, 13.12, 13.12, facing),
                    sidePortPlate(0, 1.28, facing)).optimize();
            case TOP -> Shapes.or(boxRotated(2.88, 0, 2.88, 13.12, 16, 13.12, facing),
                    topPortPlate(facing)).optimize();
            default -> Shapes.block();
        };
    }

    private static VoxelShape sidePortPlate(double x0, double x1, Direction facing) {
        return Shapes.or(boxRotated(x0, 0, 0, x1, 2.4, 16, facing),
                boxRotated(x0, 13.6, 0, x1, 16, 16, facing),
                boxRotated(x0, 2.4, 0, x1, 13.6, 2.4, facing),
                boxRotated(x0, 2.4, 13.6, x1, 13.6, 16, facing));
    }

    private static VoxelShape topPortPlate(Direction facing) {
        return Shapes.or(boxRotated(0, 0, 0, 2.4, 1.28, 16, facing),
                boxRotated(13.6, 0, 0, 16, 1.28, 16, facing),
                boxRotated(2.4, 0, 0, 13.6, 1.28, 2.4, facing),
                boxRotated(2.4, 0, 13.6, 13.6, 1.28, 16, facing));
    }

    /** 以 2 像素阶梯内接 OBJ 的四条 45° 斜边；支脚宽高与 D 的本格网格一致。 */
    private static VoxelShape casingShape(RingRole role, Direction facing) {
        if (role == RingRole.TOP || role == RingRole.LEFT || role == RingRole.RIGHT) return Shapes.block();
        if (role == RingRole.BOTTOM) return boxRotated(0, 3, 0, 16, 16, 16, facing);
        VoxelShape shape = Shapes.empty();
        for (int step = 0; step < 8; step++) {
            double x0 = step * 2, x1 = x0 + 2;
            double y0 = 0, y1 = 16;
            switch (role) {
                case UPPER_LEFT -> y1 = Math.min(16, x0 + 8);
                case UPPER_RIGHT -> y1 = Math.min(16, 24 - x1);
                case LOWER_LEFT -> y0 = Math.max(3, 8 - x0);
                case LOWER_RIGHT -> y0 = Math.max(3, x1 - 8);
                default -> throw new IllegalStateException("unexpected ring role");
            }
            if (y1 > y0) shape = Shapes.or(shape, boxRotated(x0, y0, 0, x1, y1, 16, facing));
        }
        if (role == RingRole.LOWER_LEFT)
            shape = Shapes.or(shape, boxRotated(10.4, 0, 0, 13.6, 3, 16, facing));
        if (role == RingRole.LOWER_RIGHT)
            shape = Shapes.or(shape, boxRotated(2.4, 0, 0, 5.6, 3, 16, facing));
        return shape.optimize();
    }

    static VoxelShape boxRotated(double x0, double y0, double z0, double x1, double y1, double z1,
                                          Direction facing) {
        return switch (facing) {
            case NORTH -> Block.box(x0, y0, z0, x1, y1, z1);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Shapes.empty();
        };
    }

    public static final class Casing extends TurbinePartBlock {
        public Casing(Properties properties) { super(properties); }
        @Override protected Kind kind() { return Kind.CASING; }
    }
    public static final class Rotor extends TurbinePartBlock {
        public Rotor(Properties properties) { super(properties); }
        @Override protected Kind kind() { return Kind.ROTOR; }
    }
    public static final class Inlet extends TurbinePartBlock implements EntityBlock {
        public Inlet(Properties properties) { super(properties); }
        @Override protected Kind kind() { return Kind.INLET; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TurbinePortBlockEntity(pos, state);
        }
    }
    public static final class Exhaust extends TurbinePartBlock implements EntityBlock {
        public Exhaust(Properties properties) { super(properties); }
        @Override protected Kind kind() { return Kind.EXHAUST; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TurbinePortBlockEntity(pos, state);
        }
    }

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(state.getBlock())) TurbineStructure.invalidateNearby(level, pos);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide && !next.is(state.getBlock())) TurbineStructure.invalidateNearby(level, pos);
        super.onRemove(state, level, pos, next, moving);
    }
}
