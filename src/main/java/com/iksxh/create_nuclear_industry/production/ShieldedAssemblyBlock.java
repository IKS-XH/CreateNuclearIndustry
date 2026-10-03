package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 单格无GUI动力机器，水平朝向只影响外观；底部轴、顶部投料和水平出料始终固定。 */
public final class ShieldedAssemblyBlock extends HorizontalKineticBlock
        implements IBE<ShieldedAssemblyBlockEntity>, IWrenchable {
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    private static final String PORTABLE_KEY = "CniShieldedAssembly";

    public ShieldedAssemblyBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(WORKING, false));
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }
    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WORKING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite());
    }
    @Override public Class<ShieldedAssemblyBlockEntity> getBlockEntityClass() { return ShieldedAssemblyBlockEntity.class; }
    @Override public BlockEntityType<? extends ShieldedAssemblyBlockEntity> getBlockEntityType() {
        return FuelProcessingContent.SHIELDED_ASSEMBLY_BE.get();
    }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }

    /** 掉落查询仅给机器物品附加账本快照；实际移除回调负责恰好一次回收。 */
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ShieldedAssemblyBlockEntity machine)
            for (ItemStack drop : drops) if (drop.is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get()))
                drop.set(DataComponents.CUSTOM_DATA, CustomData.of(portableTag(machine)));
        return drops;
    }
    private static CompoundTag portableTag(ShieldedAssemblyBlockEntity machine) {
        CompoundTag tag = new CompoundTag();
        tag.put(PORTABLE_KEY, machine.savePortableData());
        return tag;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity machine) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().contains(PORTABLE_KEY))
                machine.loadPortableData(data.copyTag().getCompound(PORTABLE_KEY));
        }
    }

    /** 原版销毁回调在实体移除后才运行，先保存完整机器物品并阻止重复掉落。 */
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity machine) {
            if (!player.isCreative() && state.canHarvestBlock(level, pos, player))
                Block.dropResources(state, level, pos, machine, player, player.getMainHandItem());
            machine.markRemovalHandled();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                                        BlockEntity entity, ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && !level.restoringBlockSnapshots
                && !level.captureBlockSnapshots && level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity machine
                && !machine.isRemovalHandled()) {
            ItemStack portable = new ItemStack(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get());
            portable.set(DataComponents.CUSTOM_DATA, CustomData.of(portableTag(machine)));
            machine.markRemovalHandled();
            Block.popResource(level, pos, portable);
        }
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock()) && !level.isClientSide) level.invalidateCapabilities(pos);
    }
    @Override public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity machine) machine.markRemovalHandled();
        super.onBlockExploded(state, level, pos, explosion);
    }
    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        BlockState rotated = getRotatedBlockState(state, context.getClickedFace());
        if (rotated == state) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) context.getLevel().setBlock(context.getClickedPos(), rotated, 3);
        IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
        return InteractionResult.SUCCESS;
    }
    /** 潜行扳手经破坏事件门后收回单件携物机器，普通移除不再掉第二份。 */
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
        if (level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity machine) machine.markRemovalHandled();
        state.spawnAfterBreak(server, pos, ItemStack.EMPTY, true);
        level.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof WrenchItem) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean material = false;
        for (int slot = 0; slot < 4; slot++) if (ShieldedAssemblyBlockEntity.accepts(slot, stack)) material = true;
        if (!material) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity machine) {
            ItemStack remainder = machine.insert(stack);
            if (!player.getAbilities().instabuild) stack.shrink(stack.getCount() - remainder.getCount());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                          BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity machine) {
            int taken = machine.takeToPlayer(player, player.isShiftKeyDown());
            if (taken == 0) player.displayClientMessage(Component.translatable(
                    "gui.create_nuclear_industry.shielded_assembly.status", machine.waitStatus(), machine.state().progress(),
                    ShieldedAssemblyState.WORK, Math.round(machine.getSpeed())), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
