package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 单格无 GUI 炉体；朝向只改变外观，顶进、侧出和底部热源的物理接口固定。 */
public final class FuelSinteringBlock extends BaseEntityBlock implements IWrenchable {
    public static final MapCodec<FuelSinteringBlock> CODEC = simpleCodec(FuelSinteringBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final String PORTABLE_KEY = "CniFuelSintering";

    public FuelSinteringBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelSinteringBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                              BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FuelProcessingContent.FUEL_SINTERING_BE.get(),
                FuelSinteringBlockEntity::serverTick);
    }

    /** 掉落查询只构造快照，不领取或清空实体；真实拆除入口负责恰好一次回收。 */
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof FuelSinteringBlockEntity machine)
            for (ItemStack drop : drops) if (drop.is(FuelProcessingContent.FUEL_SINTERING_FURNACE_ITEM.get()))
                drop.set(DataComponents.CUSTOM_DATA, CustomData.of(portableTag(machine)));
        return drops;
    }

    private static CompoundTag portableTag(FuelSinteringBlockEntity machine) {
        CompoundTag wrapper = new CompoundTag();
        wrapper.put(PORTABLE_KEY, machine.savePortableData());
        return wrapper;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FuelSinteringBlockEntity machine) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().contains(PORTABLE_KEY))
                machine.loadPortableData(data.copyTag().getCompound(PORTABLE_KEY));
        }
    }

    /** 原版 playerDestroy 在移除实体后运行，因此先掉落携物机器并标记本次事务。 */
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FuelSinteringBlockEntity machine) {
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

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && !level.restoringBlockSnapshots
                && !level.captureBlockSnapshots && level.getBlockEntity(pos) instanceof FuelSinteringBlockEntity machine
                && !machine.isRemovalHandled()) {
            ItemStack portable = new ItemStack(FuelProcessingContent.FUEL_SINTERING_FURNACE_ITEM.get());
            portable.set(DataComponents.CUSTOM_DATA, CustomData.of(portableTag(machine)));
            machine.markRemovalHandled();
            Block.popResource(level, pos, portable);
        }
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock()) && !level.isClientSide) level.invalidateCapabilities(pos);
    }

    /** 爆炸流程先查询 loot 再移除方块，登记已有掉落以免移除回调再生成第二台。 */
    @Override public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (level.getBlockEntity(pos) instanceof FuelSinteringBlockEntity machine) machine.markRemovalHandled();
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        BlockState rotated = getRotatedBlockState(state, context.getClickedFace());
        if (rotated == state) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) context.getLevel().setBlock(context.getClickedPos(), rotated, 3);
        IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
        return InteractionResult.SUCCESS;
    }

    /** 潜行扳手先过破坏事件，再以同一纯快照回收单件；原生移除不追加散落库存。 */
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
        if (level.getBlockEntity(pos) instanceof FuelSinteringBlockEntity machine) machine.markRemovalHandled();
        state.spawnAfterBreak(server, pos, ItemStack.EMPTY, true);
        level.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof WrenchItem) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (hit.getDirection() != Direction.UP || !stack.is(FuelProcessingContent.GREEN_FUEL_PELLET.get()))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FuelSinteringBlockEntity machine) {
            int accepted = machine.insert(stack);
            if (!player.getAbilities().instabuild) stack.shrink(accepted);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                          BlockHitResult hit) {
        Direction side = hit.getDirection();
        if (side == Direction.DOWN || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FuelSinteringBlockEntity machine)
            machine.takeToPlayer(player, side == Direction.UP);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
