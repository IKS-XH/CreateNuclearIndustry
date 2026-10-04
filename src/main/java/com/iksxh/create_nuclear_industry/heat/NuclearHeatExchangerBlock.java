package com.iksxh.create_nuclear_industry.heat;

import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 单格无 GUI 换热器；朝向决定前冷出、后热入及水平直列，顶面独立供热。 */
public final class NuclearHeatExchangerBlock extends BaseEntityBlock implements IWrenchable {
    public static final MapCodec<NuclearHeatExchangerBlock> CODEC = simpleCodec(NuclearHeatExchangerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final String PORTABLE_KEY = "CniHeatExchanger";

    public NuclearHeatExchangerBlock(Properties properties) {
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
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide && (!oldState.is(this) || oldState.getValue(FACING) != state.getValue(FACING)))
            NuclearHeatExchangerBlockEntity.topologyChanged(level, pos);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NuclearHeatExchangerBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                              BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER_BE.get(),
                NuclearHeatExchangerBlockEntity::serverTick);
    }

    /** 掉落查询只构造快照，不领取或清空实体；真实拆除入口负责恰好一次回收。 */
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof NuclearHeatExchangerBlockEntity machine)
            for (ItemStack drop : drops) if (drop.is(HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER_ITEM.get()))
                drop.set(DataComponents.CUSTOM_DATA, CustomData.of(portableTag(machine)));
        return drops;
    }

    private static CompoundTag portableTag(NuclearHeatExchangerBlockEntity machine) {
        CompoundTag wrapper = new CompoundTag();
        wrapper.put(PORTABLE_KEY, machine.savePortableData());
        return wrapper;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof NuclearHeatExchangerBlockEntity machine) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().contains(PORTABLE_KEY))
                machine.loadPortableData(data.copyTag().getCompound(PORTABLE_KEY));
        }
    }

    /** 掉落仅由原版破坏流程或扳手触发；邻居替换回调不额外掉第二台。 */
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock()) && !level.isClientSide) {
            level.invalidateCapabilities(pos);
            NuclearHeatExchangerBlockEntity.topologyChanged(level, pos);
        }
    }
    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        BlockState rotated = getRotatedBlockState(state, context.getClickedFace());
        if (rotated == state) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            context.getLevel().setBlock(context.getClickedPos(), rotated, 3);
            NuclearHeatExchangerBlockEntity.topologyChanged(context.getLevel(), context.getClickedPos());
        }
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
        if (level.getBlockEntity(pos) instanceof NuclearHeatExchangerBlockEntity machine) machine.markRemovalHandled();
        state.spawnAfterBreak(server, pos, ItemStack.EMPTY, true);
        level.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

}
