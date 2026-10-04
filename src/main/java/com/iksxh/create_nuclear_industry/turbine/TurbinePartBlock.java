package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 普通构件的状态与碰撞入口；成型状态仅用于外观，服务端运行仍逐次核验机主。 */
public abstract class TurbinePartBlock extends Block {
    public enum Kind { CASING, WINDOW, ROTOR, INLET, EXHAUST }
    public enum RingRole implements StringRepresentable {
        TOP, UPPER_LEFT, UPPER_RIGHT, LEFT, RIGHT, LOWER_LEFT, LOWER_RIGHT, BOTTOM;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    public enum Diameter implements StringRepresentable {
        D3(3), D5(5), D7(7);
        private final int blocks;
        Diameter(int blocks) { this.blocks = blocks; }
        public int blocks() { return blocks; }
        public static Diameter of(int value) { return value == 5 ? D5 : value == 7 ? D7 : D3; }
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    public enum Side implements StringRepresentable {
        UP, DOWN, LEFT, RIGHT;
        public int x(int radius) { return this == LEFT ? -radius : this == RIGHT ? radius : 0; }
        public int y(int radius) { return this == DOWN ? -radius : this == UP ? radius : 0; }
        public static Side at(int x, int y) {
            return y > 0 ? UP : y < 0 ? DOWN : x < 0 ? LEFT : RIGHT;
        }
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** 可见搭建布局与运行 Form 分离；仅 FORMED 可以授予流体和动力。 */
    public static final BooleanProperty LOCATED = BooleanProperty.create("located");
    public static final DirectionProperty MACHINE_FACING = DirectionProperty.create("machine_facing", Direction.Plane.HORIZONTAL);
    public static final int LAST_INDEPENDENT_PIECE = TurbineGeometry.MAX_PIECE + 5;
    public static final IntegerProperty PIECE = IntegerProperty.create("piece", 0, LAST_INDEPENDENT_PIECE);
    public static final EnumProperty<Diameter> DIAMETER = EnumProperty.create("diameter", Diameter.class);
    public static final EnumProperty<Side> SIDE = EnumProperty.create("side", Side.class);
    public static final EnumProperty<RingRole> RING_ROLE = EnumProperty.create("ring_role", RingRole.class);
    public static final DirectionProperty OUTWARD = DirectionProperty.create("outward");

    protected abstract Kind kind();
    protected TurbinePartBlock(Properties properties) {
        super(properties.noOcclusion());
        BlockState state = defaultBlockState().setValue(FORMED, false).setValue(LOCATED, false)
                .setValue(MACHINE_FACING, Direction.NORTH);
        switch (kind()) {
            case CASING, WINDOW -> state = state.setValue(PIECE, 0);
            case ROTOR -> state = state.setValue(DIAMETER, Diameter.D3);
            case INLET, EXHAUST -> state = state.setValue(RING_ROLE, RingRole.TOP)
                    .setValue(OUTWARD, Direction.UP);
        }
        registerDefaultState(state);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, LOCATED, MACHINE_FACING);
        switch (kind()) {
            case CASING, WINDOW -> builder.add(PIECE);
            case ROTOR -> builder.add(DIAMETER);
            case INLET, EXHAUST -> builder.add(RING_ROLE, OUTWARD);
        }
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(MACHINE_FACING,
                context.getHorizontalDirection().getOpposite());
        if (kind() == Kind.INLET || kind() == Kind.EXHAUST)
            state = state.setValue(OUTWARD, context.getClickedFace());
        if (kind() == Kind.CASING || kind() == Kind.WINDOW)
            state = state.setValue(PIECE, independentPiece(context.getClickedFace()));
        return state;
    }

    /** 独立板保留编号编码世界外表面；真实几何编号完全沿用 01B。 */
    public static int independentPiece(Direction face) {
        return switch (face) {
            case DOWN -> TurbineGeometry.MAX_PIECE + 1;
            case NORTH -> TurbineGeometry.MAX_PIECE + 2;
            case SOUTH -> TurbineGeometry.MAX_PIECE + 3;
            case WEST -> TurbineGeometry.MAX_PIECE + 4;
            case EAST -> TurbineGeometry.MAX_PIECE + 5;
            default -> 0;
        };
    }

    public static Direction independentFace(int piece) {
        return switch (piece - TurbineGeometry.MAX_PIECE) {
            case 1 -> Direction.DOWN;
            case 2 -> Direction.NORTH;
            case 3 -> Direction.SOUTH;
            case 4 -> Direction.WEST;
            case 5 -> Direction.EAST;
            default -> Direction.UP;
        };
    }

    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                         CollisionContext context) {
        if (!state.getValue(LOCATED)) {
            if (kind() == Kind.CASING || kind() == Kind.WINDOW)
                return independentShape(independentFace(state.getValue(PIECE)));
            return unformedShape();
        }
        Direction facing = state.getValue(MACHINE_FACING);
        return switch (kind()) {
            case CASING, WINDOW -> TurbineCollision.shape(state.getValue(PIECE), facing);
            case ROTOR -> boxRotated(6, 6, 0, 10, 10, 16, facing);
            case INLET, EXHAUST -> sidePlate(roleSide(state.getValue(RING_ROLE)), facing);
        };
    }

    protected VoxelShape unformedShape() { return Shapes.block(); }

    private static VoxelShape independentShape(Direction face) {
        return switch (face) {
            case UP -> Block.box(0, 13, 0, 16, 16, 16);
            case DOWN -> Block.box(0, 0, 0, 16, 3, 16);
            case NORTH -> Block.box(0, 0, 0, 16, 16, 3);
            case SOUTH -> Block.box(0, 0, 13, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, 3, 16, 16);
            case EAST -> Block.box(13, 0, 0, 16, 16, 16);
        };
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                                         BlockPos pos, Player player, InteractionHand hand,
                                                         BlockHitResult hit) {
        return TurbinePlacement.useOn(stack, state, level, pos, player, hand, hit);
    }

    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (kind() != Kind.CASING && kind() != Kind.WINDOW)
            return InteractionResult.PASS;
        if (state.getValue(LOCATED)) return InteractionResult.SUCCESS;
        if (!context.getLevel().isClientSide) {
            Direction next = switch (independentFace(state.getValue(PIECE))) {
                case UP -> Direction.NORTH;
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.DOWN;
                case DOWN -> Direction.UP;
            };
            context.getLevel().setBlock(context.getClickedPos(),
                    state.setValue(PIECE, independentPiece(next)), 3);
        }
        return InteractionResult.SUCCESS;
    }

    private static Side roleSide(RingRole role) {
        return switch (role) {
            case TOP, UPPER_LEFT, UPPER_RIGHT -> Side.UP;
            case BOTTOM, LOWER_LEFT, LOWER_RIGHT -> Side.DOWN;
            case LEFT -> Side.LEFT;
            case RIGHT -> Side.RIGHT;
        };
    }

    static VoxelShape sidePlate(Side side, Direction facing) {
        return switch (side) {
            case UP -> boxRotated(0, 13, 0, 16, 16, 16, facing);
            case DOWN -> boxRotated(0, 0, 0, 16, 3, 16, facing);
            case LEFT -> boxRotated(0, 0, 0, 3, 16, 16, facing);
            case RIGHT -> boxRotated(13, 0, 0, 16, 16, 16, facing);
        };
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

    public static final class Casing extends TurbinePartBlock implements IWrenchable {
        public Casing(Properties properties) { super(properties); }
        @Override protected Kind kind() { return Kind.CASING; }
        @Override protected VoxelShape unformedShape() { return independentShape(Direction.UP); }
    }
    public static final class Window extends TurbinePartBlock implements IWrenchable {
        public Window(Properties properties) { super(properties); }
        @Override protected Kind kind() { return Kind.WINDOW; }
        @Override protected VoxelShape unformedShape() { return independentShape(Direction.UP); }
    }
    public static final class Rotor extends TurbinePartBlock implements EntityBlock {
        public Rotor(Properties properties) { super(properties); }
        @Override protected Kind kind() { return Kind.ROTOR; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TurbineRotorBlockEntity(pos, state);
        }
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

    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old,
                                     boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(state.getBlock())) {
            TurbineStructure.invalidateNearby(level, pos);
            TurbineAssembly.refreshNear(level, pos);
        }
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next,
                                      boolean moving) {
        if (!level.isClientSide && !next.is(state.getBlock())) {
            TurbineStructure.invalidateNearby(level, pos);
            TurbineAssembly.refreshAfterRemoval(level, pos);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TurbineAssembly.refreshNear(level, pos);
    }
}
