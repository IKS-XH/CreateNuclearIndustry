package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 两端独立 Create 轴与侧面非动力控制器的方块入口。 */
public final class TurbineShaftBlock {
    public enum End implements StringRepresentable {
        FRONT, REAR;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    public static final EnumProperty<End> END = EnumProperty.create("end", End.class);
    private TurbineShaftBlock() {}

    /** 控制器保留旧注册 ID/库存 NBT，但不再继承动力源或暴露 Create 轴。 */
    public static final class Controller extends Block implements IBE<TurbineControllerBlockEntity>, IWrenchable {
        private static final String PORTABLE_KEY = "CniTurbine";
        public Controller(Properties properties) {
            super(properties.noOcclusion());
            registerDefaultState(defaultBlockState().setValue(TurbinePartBlock.FORMED, false)
                    .setValue(TurbinePartBlock.LOCATED, false)
                    .setValue(TurbinePartBlock.MACHINE_FACING, Direction.NORTH)
                    .setValue(TurbinePartBlock.SIDE, TurbinePartBlock.Side.RIGHT));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TurbinePartBlock.FORMED, TurbinePartBlock.LOCATED,
                    TurbinePartBlock.MACHINE_FACING, TurbinePartBlock.SIDE);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction facing = context.getHorizontalDirection().getOpposite();
            Direction surface = context.getClickedFace();
            if (surface.getAxis().isHorizontal() && surface.getAxis() == facing.getAxis())
                facing = facing.getClockWise();
            TurbinePartBlock.Side side = surface == Direction.UP ? TurbinePartBlock.Side.UP
                    : surface == Direction.DOWN ? TurbinePartBlock.Side.DOWN
                    : surface == facing.getClockWise() ? TurbinePartBlock.Side.RIGHT
                    : TurbinePartBlock.Side.LEFT;
            return defaultBlockState().setValue(TurbinePartBlock.MACHINE_FACING, facing)
                    .setValue(TurbinePartBlock.SIDE, side);
        }
        @Override public Class<TurbineControllerBlockEntity> getBlockEntityClass() {
            return TurbineControllerBlockEntity.class;
        }
        @Override public net.minecraft.world.level.block.entity.BlockEntityType<? extends TurbineControllerBlockEntity>
                getBlockEntityType() { return TurbineContent.CONTROLLER_BE.get(); }
        @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                             CollisionContext context) {
            return TurbinePartBlock.sidePlate(state.getValue(TurbinePartBlock.SIDE),
                    state.getValue(TurbinePartBlock.MACHINE_FACING));
        }
        @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old,
                                      boolean moving) {
            super.onPlace(state, level, pos, old, moving);
            if (!level.isClientSide && !old.is(state.getBlock())) {
                TurbineStructure.invalidateNearby(level, pos);
                TurbineAssembly.refreshNear(level, pos);
            }
        }
        @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next,
                                       boolean moving) {
            if (!level.isClientSide && !next.is(state.getBlock())
                    && level.getBlockEntity(pos) instanceof TurbineControllerBlockEntity owner)
                owner.onControllerBroken(state.getValue(TurbinePartBlock.MACHINE_FACING));
            super.onRemove(state, level, pos, next, moving);
        }
        @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            List<ItemStack> drops = super.getDrops(state, builder);
            if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                    instanceof TurbineControllerBlockEntity owner)
                for (ItemStack drop : drops) if (drop.is(TurbineContent.CONTROLLER_ITEM.get())) {
                    CompoundTag wrapper = new CompoundTag();
                    wrapper.put(PORTABLE_KEY, owner.savePortableData());
                    drop.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapper));
                }
            return drops;
        }
        @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer,
                                          ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof TurbineControllerBlockEntity owner) {
                CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                if (data != null && data.copyTag().contains(PORTABLE_KEY))
                    owner.loadPortableData(data.copyTag().getCompound(PORTABLE_KEY));
            }
        }
        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                               Player player, BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof TurbineControllerBlockEntity owner) {
                player.displayClientMessage(owner.diagnostic(), true);
                owner.highlightIssue(player);
            }
            return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof TurbineControllerBlockEntity owner) {
                if (owner.currentForm() == null) {
                    level.setBlock(pos, state.setValue(TurbinePartBlock.MACHINE_FACING,
                            state.getValue(TurbinePartBlock.MACHINE_FACING).getClockWise()), 3);
                    TurbineStructure.invalidateNearby(level, pos);
                }
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(owner.diagnostic(), true);
                    owner.highlightIssue(context.getPlayer());
                }
            }
            return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
            Level level = context.getLevel();
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            BlockPos pos = context.getClickedPos();
            Player player = context.getPlayer();
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) return InteractionResult.SUCCESS;
            if (player != null && !player.isCreative())
                Block.getDrops(state, server, pos, level.getBlockEntity(pos), player, context.getItemInHand())
                        .forEach(drop -> player.getInventory().placeItemBackInInventory(drop));
            state.spawnAfterBreak(server, pos, ItemStack.EMPTY, true);
            level.destroyBlock(pos, false);
            IWrenchable.playRemoveSound(level, pos);
            return InteractionResult.SUCCESS;
        }
    }

    /** 同一输出轴 ID 根据端位只向本端机外暴露轴，两个网络不通过机器内部相连。 */
    public static final class Output extends KineticBlock implements IBE<TurbineOutputShaftBlockEntity> {
        public Output(Properties properties) {
            super(properties.noOcclusion());
            registerDefaultState(defaultBlockState().setValue(TurbinePartBlock.FORMED, false)
                    .setValue(TurbinePartBlock.LOCATED, false)
                    .setValue(TurbinePartBlock.MACHINE_FACING, Direction.NORTH).setValue(END, End.FRONT));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TurbinePartBlock.FORMED, TurbinePartBlock.LOCATED,
                    TurbinePartBlock.MACHINE_FACING, END);
        }
        @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                                             BlockPos pos, Player player, InteractionHand hand,
                                                             BlockHitResult hit) {
            return TurbinePlacement.useOn(stack, state, level, pos, player, hand, hit);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(TurbinePartBlock.MACHINE_FACING,
                    context.getHorizontalDirection().getOpposite());
        }
        @Override public Direction.Axis getRotationAxis(BlockState state) {
            return state.getValue(TurbinePartBlock.MACHINE_FACING).getAxis();
        }
        @Override protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
            return oldState.getBlock() == newState.getBlock()
                    && oldState.getValue(TurbinePartBlock.MACHINE_FACING)
                    == newState.getValue(TurbinePartBlock.MACHINE_FACING)
                    && oldState.getValue(END) == newState.getValue(END);
        }
        @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state,
                                                   Direction face) {
            Direction front = state.getValue(TurbinePartBlock.MACHINE_FACING);
            return face == (state.getValue(END) == End.FRONT ? front : front.getOpposite());
        }
        @Override public Class<TurbineOutputShaftBlockEntity> getBlockEntityClass() {
            return TurbineOutputShaftBlockEntity.class;
        }
        @Override public net.minecraft.world.level.block.entity.BlockEntityType<? extends TurbineOutputShaftBlockEntity>
                getBlockEntityType() { return TurbineContent.OUTPUT_SHAFT_BE.get(); }
        @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                             CollisionContext context) {
            if (!state.getValue(TurbinePartBlock.LOCATED)) return Shapes.block();
            Direction facing = state.getValue(TurbinePartBlock.MACHINE_FACING);
            double plateStart = state.getValue(END) == End.FRONT ? 0 : 13;
            return Shapes.or(TurbinePartBlock.boxRotated(0, 0, plateStart,
                            16, 16, plateStart + 3, facing),
                    TurbinePartBlock.boxRotated(4, 4, 0, 12, 12, 16, facing)).optimize();
        }
        @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old,
                                      boolean moving) {
            super.onPlace(state, level, pos, old, moving);
            if (!level.isClientSide && !old.is(state.getBlock())) {
                TurbineStructure.invalidateNearby(level, pos);
                TurbineAssembly.refreshNear(level, pos);
            }
        }
        @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next,
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
}
