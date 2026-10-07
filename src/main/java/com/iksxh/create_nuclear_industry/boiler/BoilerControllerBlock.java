package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 控制器为唯一携水汽库存的方块；普通掉落与潜行扳手共用纯快照。 */
public final class BoilerControllerBlock extends BaseEntityBlock implements IWrenchable {
    public static final MapCodec<BoilerControllerBlock> CODEC = simpleCodec(BoilerControllerBlock::new);
    private static final String PORTABLE_KEY = "CniBoiler";
    public BoilerControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BoilerPartBlock.FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BoilerPartBlock.FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BoilerPartBlock.FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BoilerControllerBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BoilerContent.CONTROLLER_BE.get(), BoilerControllerBlockEntity::serverTick);
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BoilerControllerBlockEntity owner)
            for (ItemStack drop : drops) if (drop.is(BoilerContent.CONTROLLER_ITEM.get())) {
                var wrapper = new CompoundTag();
                wrapper.put(PORTABLE_KEY, owner.savePortableData());
                drop.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapper));
            }
        return drops;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BoilerControllerBlockEntity owner) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().contains(PORTABLE_KEY))
                owner.loadPortableData(data.copyTag().getCompound(PORTABLE_KEY));
        }
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide) BoilerStructure.invalidateNearby(level, pos);
    }
    /** 手动扳手检查结构并向玩家报告；不打开物品界面或修改库存。 */
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        if (!level.isClientSide) player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                !(level.getBlockEntity(pos) instanceof BoilerControllerBlockEntity owner) || owner.currentForm() == null ? "gui.create_nuclear_industry.boiler.state.unformed"
                        : "gui.create_nuclear_industry.boiler.state.formed"), true);
        return InteractionResult.SUCCESS;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide && !state.is(next.getBlock())) BoilerStructure.invalidateNearby(level, pos);
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock()) && !level.isClientSide) level.invalidateCapabilities(pos);
    }
    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            var owner = level.getBlockEntity(context.getClickedPos()) instanceof BoilerControllerBlockEntity machine
                    ? machine : null;
            if (context.getPlayer() != null) {
                var issue = owner != null && owner.currentForm() != null ? null
                        : BoilerStructure.issue(level, context.getClickedPos());
                context.getPlayer().displayClientMessage(issue == null
                        ? net.minecraft.network.chat.Component.translatable("gui.create_nuclear_industry.boiler.state.formed")
                        : net.minecraft.network.chat.Component.translatable("gui.create_nuclear_industry.boiler.inspect",
                        net.minecraft.network.chat.Component.translatable("gui.create_nuclear_industry.boiler.issue." + issue.reason()),
                        issue.pos().getX(), issue.pos().getY(), issue.pos().getZ()), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        var event = new BlockEvent.BreakEvent(level, pos, state, player);
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
