package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.BasicMaterialContent;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.foundation.block.IBE;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 下段持有唯一动力和状态，上段只提供外形与指向有效下段的物料面。 */
public final class CentrifugeBlock extends HorizontalKineticBlock
        implements IBE<CentrifugeBlockEntity>, IWrenchable, IProxyHoveringInformation {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<DoubleBlockHalf> HALF =
            BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final ThreadLocal<Boolean> REMOVING_PAIR = ThreadLocal.withInitial(() -> false);

    public CentrifugeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }
    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && face == Direction.DOWN;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite());
    }
    /** 上段代理仅供 Create 发现 capability；下段继续独占动力实体与持久化状态。 */
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? IBE.super.newBlockEntity(pos, state)
                : new CentrifugeUpperProxyBlockEntity(pos, state);
    }
    /** IBE 的默认实现会为动力实体返回 Smart ticker；上段代理必须始终保持静态。 */
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? IBE.super.getTicker(level, state, type) : null;
    }
    @Override public Class<CentrifugeBlockEntity> getBlockEntityClass() { return CentrifugeBlockEntity.class; }
    @Override public BlockEntityType<? extends CentrifugeBlockEntity> getBlockEntityType() {
        return FuelProcessingContent.CENTRIFUGE_BE.get();
    }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }

    /** Create 护目镜瞄准上段时仍展示下段唯一账本。 */
    @Override public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER && owner(level, pos, state) != null
                ? pos.below() : pos;
    }

    /** 只接受方向一致且由下段确认配对的完整机器；旧单格不能借邻居读取别台库存。 */
    public static CentrifugeBlockEntity owner(LevelReader level, BlockPos pos, BlockState state) {
        if (!state.is(FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get())) return null;
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockState lower = level.getBlockState(lowerPos);
        if (!lower.is(FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get())
                || lower.getValue(HALF) != DoubleBlockHalf.LOWER) return null;
        if (!(level.getBlockEntity(lowerPos) instanceof CentrifugeBlockEntity machine)) return null;
        if (machine.isRemoved() || !machine.isPaired()) return null;
        BlockState upper = level.getBlockState(lowerPos.above());
        if (!upper.is(FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get())
                || upper.getValue(HALF) != DoubleBlockHalf.UPPER
                || upper.getValue(BlockStateProperties.HORIZONTAL_FACING)
                != lower.getValue(BlockStateProperties.HORIZONTAL_FACING)) return null;
        return machine;
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        CentrifugeBlockEntity machine = null;
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CentrifugeBlockEntity local) {
            machine = local;
        } else if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            // 上段只有无状态代理；掉落查询发生在移除之前，必须当场读取下段真实账本。
            var origin = builder.getOptionalParameter(LootContextParams.ORIGIN);
            if (origin != null) machine = owner(builder.getLevel(), BlockPos.containing(origin), state);
        }
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER && machine == null) return List.of();
        List<ItemStack> drops = super.getDrops(state, builder);
        if (machine != null) {
            for (ItemStack drop : drops) {
                if (drop.is(FuelProcessingContent.ENRICHMENT_CENTRIFUGE_ITEM.get()))
                    drop.set(DataComponents.CUSTOM_DATA, CustomData.of(portableTag(machine)));
            }
        }
        return drops;
    }

    /** 掉落入口与直接替换共同使用的纯快照，重复调用不会改变机器。 */
    static CompoundTag portableTag(CentrifugeBlockEntity machine) {
        CompoundTag wrapper = new CompoundTag();
        wrapper.put("CniCentrifuge", machine.savePortableData());
        return wrapper;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || state.getValue(HALF) != DoubleBlockHalf.LOWER) return;
        if (level.getBlockEntity(pos) instanceof CentrifugeBlockEntity machine) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().contains("CniCentrifuge"))
                machine.loadPortableData(data.copyTag().getCompound("CniCentrifuge"));
            machine.setPaired(true);
            level.invalidateCapabilities(pos);
            level.invalidateCapabilities(pos.above());
        }
    }

    /** 原版先移除再调用 playerDestroy；此处在有效所有者尚在时生成唯一快照。 */
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockEntity entity = state.getValue(HALF) == DoubleBlockHalf.LOWER ? level.getBlockEntity(pos) : null;
            CentrifugeBlockEntity machine = state.getValue(HALF) == DoubleBlockHalf.LOWER
                    ? entity instanceof CentrifugeBlockEntity local ? local : null : owner(level, pos, state);
            if (!player.isCreative() && state.canHarvestBlock(level, pos, player))
                Block.dropResources(state, level, pos, entity, player, player.getMainHandItem());
            if (machine != null) machine.markRemovalHandled();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                                        BlockEntity entity, ItemStack tool) {
        // 掉落已在 playerWillDestroy 完成；保留原版统计与疲劳，避免再次生成第二份物品。
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }

    /** 另一半仅静默移除；未经过掉落入口的直接替换仍保全唯一账本。 */
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && !level.restoringBlockSnapshots
                && !level.captureBlockSnapshots && !REMOVING_PAIR.get()) {
            REMOVING_PAIR.set(true);
            try {
                CentrifugeBlockEntity machine = null;
                if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
                    if (level.getBlockEntity(pos) instanceof CentrifugeBlockEntity local) machine = local;
                } else {
                    BlockPos lowerPos = pos.below();
                    BlockState lower = level.getBlockState(lowerPos);
                    if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER
                            && lower.getValue(BlockStateProperties.HORIZONTAL_FACING)
                            == state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                            && level.getBlockEntity(lowerPos) instanceof CentrifugeBlockEntity local
                            && local.isPaired()) machine = local;
                }
                if (machine != null && !machine.isRemovalHandled()) {
                    ItemStack portable = new ItemStack(FuelProcessingContent.ENRICHMENT_CENTRIFUGE_ITEM.get());
                    portable.set(DataComponents.CUSTOM_DATA, CustomData.of(portableTag(machine)));
                    machine.markRemovalHandled();
                    Block.popResource(level, pos, portable);
                }
                BlockPos otherPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
                BlockState other = level.getBlockState(otherPos);
                if (other.is(this) && other.getValue(HALF) != state.getValue(HALF)
                        && other.getValue(BlockStateProperties.HORIZONTAL_FACING)
                        == state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                    level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
                    level.invalidateCapabilities(otherPos);
                }
            } finally {
                REMOVING_PAIR.remove();
            }
        }
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock()) && !level.isClientSide) level.invalidateCapabilities(pos);
    }

    /** 原版在爆炸中先取掉落再调用本入口移除；此处标记已有掉落事务。 */
    @Override public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        CentrifugeBlockEntity machine = state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? level.getBlockEntity(pos) instanceof CentrifugeBlockEntity local ? local : null
                : owner(level, pos, state);
        if (machine != null) machine.markRemovalHandled();
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        if (owner(context.getLevel(), pos, state) == null) return InteractionResult.PASS;
        BlockState rotated = getRotatedBlockState(state, context.getClickedFace());
        if (rotated == state) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            BlockPos otherPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState other = context.getLevel().getBlockState(otherPos);
            context.getLevel().setBlock(pos, rotated, 3);
            context.getLevel().setBlock(otherPos, other.setValue(BlockStateProperties.HORIZONTAL_FACING,
                    rotated.getValue(BlockStateProperties.HORIZONTAL_FACING)), 3);
            context.getLevel().invalidateCapabilities(pos);
            context.getLevel().invalidateCapabilities(otherPos);
        }
        IWrenchable.playRotateSound(context.getLevel(), pos);
        return InteractionResult.SUCCESS;
    }

    /** Create 扳手先通过取消门，再在拆除前取一次快照；创造模式明确不回收物料。 */
    @Override public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return InteractionResult.SUCCESS;
        if (player != null && !player.isCreative()) {
            Block.getDrops(state, server, pos, level.getBlockEntity(pos), player, context.getItemInHand())
                    .forEach(drop -> player.getInventory().placeItemBackInInventory(drop));
        }
        CentrifugeBlockEntity machine = state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? level.getBlockEntity(pos) instanceof CentrifugeBlockEntity local ? local : null
                : owner(level, pos, state);
        if (machine != null) machine.markRemovalHandled();
        state.spawnAfterBreak(server, pos, ItemStack.EMPTY, true);
        level.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof WrenchItem) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean fluidContainer = stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
        CentrifugeBlockEntity machine = owner(level, pos, state);
        if (machine == null) return fluidContainer ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Direction side = hit.getDirection();
        if (stack.isEmpty()) {
            if (side.getAxis() != Direction.Axis.Y && !level.isClientSide)
                machine.extractOutputsToPlayer(player, side, state.getValue(HALF) == DoubleBlockHalf.UPPER);
            return side.getAxis() == Direction.Axis.Y ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                    : ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER
                && side == state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                && stack.is(BasicMaterialContent.HEAVY_BEARING.get())) {
            if (!level.isClientSide && machine.repairBearing() && !player.getAbilities().instabuild) stack.shrink(1);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (fluidContainer) {
            if (!level.isClientSide
                    && (side.getAxis() != Direction.Axis.Y
                    || state.getValue(HALF) == DoubleBlockHalf.UPPER && side == Direction.UP))
                machine.transferHeldBucket(player, hand, side, state.getValue(HALF) == DoubleBlockHalf.UPPER);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return stack.getItem() instanceof BlockItem ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                          BlockHitResult hit) {
        Direction side = hit.getDirection();
        CentrifugeBlockEntity machine = owner(level, pos, state);
        if (!player.getMainHandItem().isEmpty() || side.getAxis() == Direction.Axis.Y || machine == null)
            return InteractionResult.PASS;
        if (!level.isClientSide)
            machine.extractOutputsToPlayer(player, side, state.getValue(HALF) == DoubleBlockHalf.UPPER);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
