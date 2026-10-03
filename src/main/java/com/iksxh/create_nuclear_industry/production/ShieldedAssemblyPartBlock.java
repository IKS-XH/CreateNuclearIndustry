package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

/** 七个无动力外壳代理，part编号和朝向必须与主控归属一致。 */
public final class ShieldedAssemblyPartBlock extends Block implements EntityBlock, IWrenchable, IProxyHoveringInformation {
    public static final IntegerProperty PART = IntegerProperty.create("part", 1, 7);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    public ShieldedAssemblyPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(PART, 1).setValue(WORKING, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, PART, WORKING);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShieldedAssemblyPartBlockEntity(pos, state);
    }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
    /** 仅在主控尚不可见时重排归属检查，不让从属运行任何制造或动力逻辑。 */
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ShieldedAssemblyPartBlockEntity proxy
                && !proxy.verifyOwnerIfLoaded()) level.scheduleTick(pos, this, 40);
    }
    @Override public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        ShieldedAssemblyBlockEntity master = ShieldedAssemblyStructure.master(level, pos, state);
        return master == null ? pos : master.getBlockPos();
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) { return List.of(); }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            ShieldedAssemblyBlockEntity master = ShieldedAssemblyStructure.master(level, pos, state);
            if (master != null && !master.isRemovalHandled() && ShieldedAssemblyStructure.allLoaded(master)) {
                if (!player.isCreative())
                    Block.popResource(level, pos, ShieldedAssemblyBlock.portable(master));
                master.markRemovalHandled();
                ShieldedAssemblyStructure.removeParts(master, pos);
                level.setBlock(master.getBlockPos(), Blocks.AIR.defaultBlockState(), 35);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                                        BlockEntity entity, ItemStack tool) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && !ShieldedAssemblyStructure.removing()
                && !level.restoringBlockSnapshots && !level.captureBlockSnapshots) {
            ShieldedAssemblyBlockEntity master = ShieldedAssemblyStructure.master(level, pos, state);
            if (master != null && ShieldedAssemblyStructure.allLoaded(master)) {
                if (!master.isRemovalHandled()) {
                    Block.popResource(level, pos, ShieldedAssemblyBlock.portable(master));
                    master.markRemovalHandled();
                }
                ShieldedAssemblyStructure.removeParts(master, pos);
                level.setBlock(master.getBlockPos(), Blocks.AIR.defaultBlockState(), 35);
            }
        }
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock()) && !level.isClientSide) level.invalidateCapabilities(pos);
    }
    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!context.getLevel().isClientSide && context.getPlayer() != null)
            context.getPlayer().displayClientMessage(Component.translatable(
                    "gui.create_nuclear_industry.shielded_assembly.relocate"), true);
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        return ShieldedAssemblyBlock.wrenchPickup(context, context.getClickedPos(), state);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                         Player player, InteractionHand hand, BlockHitResult hit) {
        return ShieldedAssemblyBlock.useItem(stack, level, pos, state, player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                          BlockHitResult hit) {
        return ShieldedAssemblyBlock.useEmpty(level, pos, state, player);
    }
}
