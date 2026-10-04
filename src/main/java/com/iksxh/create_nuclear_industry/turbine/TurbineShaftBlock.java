package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 水平前后端输出轴只在机器外侧暴露 Create 轴面，不通过内部转子串互连。 */
public abstract class TurbineShaftBlock<T extends TurbineShaftPowerSource> extends KineticBlock implements IBE<T> {
    private static final VoxelShape[] CONTROLLER_SHAPE = new VoxelShape[4];
    private static final VoxelShape[] OUTPUT_SHAPE = new VoxelShape[4];
    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int index = facing.get2DDataValue();
            CONTROLLER_SHAPE[index] = Shapes.or(
                    TurbinePartBlock.boxRotated(0, 0, 0, 16, 16, 1.28, facing),
                    TurbinePartBlock.boxRotated(1.92, 1.92, 1.28, 14.08, 14.08, 6.4, facing)).optimize();
            OUTPUT_SHAPE[index] = Shapes.or(
                    TurbinePartBlock.boxRotated(4.96, 4.96, 1.6, 11.04, 11.04, 16, facing),
                    TurbinePartBlock.boxRotated(0, 0, 14.08, 4, 16, 16, facing),
                    TurbinePartBlock.boxRotated(12, 0, 14.08, 16, 16, 16, facing),
                    TurbinePartBlock.boxRotated(4, 0, 14.08, 12, 4, 16, facing),
                    TurbinePartBlock.boxRotated(4, 12, 14.08, 12, 16, 16, facing)).optimize();
        }
    }
    protected TurbineShaftBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(defaultBlockState().setValue(TurbinePartBlock.FORMED, false)
                .setValue(TurbinePartBlock.MACHINE_FACING, Direction.NORTH));
    }
    protected abstract boolean rear();
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TurbinePartBlock.FORMED, TurbinePartBlock.MACHINE_FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(TurbinePartBlock.MACHINE_FACING,
                context.getHorizontalDirection().getOpposite());
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(TurbinePartBlock.MACHINE_FACING).getAxis();
    }
    /** 同轴翻向也会改变唯一外接面，Create 必须重新传播轴连接。 */
    @Override protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return oldState.getBlock() == newState.getBlock()
                && oldState.getValue(TurbinePartBlock.MACHINE_FACING)
                == newState.getValue(TurbinePartBlock.MACHINE_FACING);
    }
    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        Direction front = state.getValue(TurbinePartBlock.MACHINE_FACING);
        return face == (rear() ? front.getOpposite() : front);
    }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(TurbinePartBlock.FORMED)) return Shapes.block();
        int index = state.getValue(TurbinePartBlock.MACHINE_FACING).get2DDataValue();
        return rear() ? OUTPUT_SHAPE[index] : CONTROLLER_SHAPE[index];
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide && !old.is(state.getBlock())) TurbineStructure.invalidateNearby(level, pos);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide && !next.is(state.getBlock())) TurbineStructure.invalidateNearby(level, pos);
        super.onRemove(state, level, pos, next, moving);
    }

    /** 前端控制器单独携带库存，扳手只诊断或拆下，不打开 GUI。 */
    public static final class Controller extends TurbineShaftBlock<TurbineControllerBlockEntity> implements IWrenchable {
        private static final String PORTABLE_KEY = "CniTurbine";
        public Controller(Properties properties) { super(properties); }
        @Override protected boolean rear() { return false; }
        @Override public Class<TurbineControllerBlockEntity> getBlockEntityClass() { return TurbineControllerBlockEntity.class; }
        @Override public net.minecraft.world.level.block.entity.BlockEntityType<? extends TurbineControllerBlockEntity> getBlockEntityType() {
            return TurbineContent.CONTROLLER_BE.get();
        }
        @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
            if (!level.isClientSide && !next.is(state.getBlock())
                    && level.getBlockEntity(pos) instanceof TurbineControllerBlockEntity owner)
                owner.onControllerBroken(state.getValue(TurbinePartBlock.MACHINE_FACING));
            super.onRemove(state, level, pos, next, moving);
        }
        @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            List<ItemStack> drops = super.getDrops(state, builder);
            if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof TurbineControllerBlockEntity owner)
                for (ItemStack drop : drops) if (drop.is(TurbineContent.CONTROLLER_ITEM.get())) {
                    CompoundTag wrapper = new CompoundTag();
                    wrapper.put(PORTABLE_KEY, owner.savePortableData());
                    drop.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapper));
                }
            return drops;
        }
        @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof TurbineControllerBlockEntity owner) {
                CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                if (data != null && data.copyTag().contains(PORTABLE_KEY))
                    owner.loadPortableData(data.copyTag().getCompound(PORTABLE_KEY));
            }
        }
        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                               Player player, BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof TurbineControllerBlockEntity owner)
                player.displayClientMessage(owner.diagnostic(), true);
            return InteractionResult.SUCCESS;
        }
        @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
            Level level = context.getLevel();
            if (!level.isClientSide && context.getPlayer() != null
                    && level.getBlockEntity(context.getClickedPos()) instanceof TurbineControllerBlockEntity owner)
                context.getPlayer().displayClientMessage(owner.diagnostic(), true);
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

    public static final class Output extends TurbineShaftBlock<TurbineOutputShaftBlockEntity> {
        public Output(Properties properties) { super(properties); }
        @Override protected boolean rear() { return true; }
        @Override public Class<TurbineOutputShaftBlockEntity> getBlockEntityClass() { return TurbineOutputShaftBlockEntity.class; }
        @Override public net.minecraft.world.level.block.entity.BlockEntityType<? extends TurbineOutputShaftBlockEntity> getBlockEntityType() {
            return TurbineContent.OUTPUT_SHAFT_BE.get();
        }
    }
}
