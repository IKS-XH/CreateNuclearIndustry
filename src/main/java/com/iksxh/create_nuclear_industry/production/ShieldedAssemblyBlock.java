package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

/** 八格机器的唯一动力主控；expanded=false 只标识旧世界中的单格机器。 */
public final class ShieldedAssemblyBlock extends HorizontalKineticBlock
        implements IBE<ShieldedAssemblyBlockEntity>, IWrenchable {
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    private static final String PORTABLE_KEY = "CniShieldedAssembly";

    public ShieldedAssemblyBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(WORKING, false).setValue(EXPANDED, false));
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }
    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return state.getValue(EXPANDED) && face == Direction.DOWN;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WORKING, EXPANDED);
    }
    /** 放置前检查全部八格及玩家权限；原版在返回null时不消耗物品。 */
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        for (int part = 0; part < 8; part++) {
            BlockPos at = ShieldedAssemblyLayout.position(context.getClickedPos(), facing, part);
            if (!level.hasChunkAt(at) || !level.getBlockState(at).canBeReplaced(context)
                    || context.getPlayer() != null && (!level.mayInteract(context.getPlayer(), at)
                    || !context.getPlayer().mayUseItemAt(at, context.getClickedFace(), context.getItemInHand())))
                return null;
        }
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(EXPANDED, true);
    }
    @Override public Class<ShieldedAssemblyBlockEntity> getBlockEntityClass() { return ShieldedAssemblyBlockEntity.class; }
    @Override public BlockEntityType<? extends ShieldedAssemblyBlockEntity> getBlockEntityType() {
        return FuelProcessingContent.SHIELDED_ASSEMBLY_BE.get();
    }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }

    /** 载入旧便携账本后建立七个无库存代理；异常放置失败时清理已放代理和主控。 */
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !state.getValue(EXPANDED)
                || !(level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity machine)) return;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains(PORTABLE_KEY))
            machine.loadPortableData(data.copyTag().getCompound(PORTABLE_KEY));
        UUID owner = UUID.randomUUID();
        machine.setOwnerId(owner);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        List<BlockPos> placedParts = new ArrayList<>();
        for (int part = 1; part < 8; part++) {
            BlockPos at = ShieldedAssemblyLayout.position(pos, facing, part);
            BlockState proxy = FuelProcessingContent.SHIELDED_ASSEMBLY_PART.get().defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                    .setValue(ShieldedAssemblyPartBlock.PART, part);
            boolean placed = level.hasChunkAt(at) && level.getBlockState(at).canBeReplaced()
                    && level.setBlock(at, proxy, 3);
            if (placed) placedParts.add(at);
            if (!placed || !(level.getBlockEntity(at) instanceof ShieldedAssemblyPartBlockEntity proxyEntity)) {
                machine.markRemovalHandled();
                for (BlockPos made : placedParts)
                    if (level.getBlockState(made).is(FuelProcessingContent.SHIELDED_ASSEMBLY_PART.get()))
                        level.setBlock(made, Blocks.AIR.defaultBlockState(), 35);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 35);
                if (placer instanceof Player player && !player.getAbilities().instabuild)
                    player.getInventory().placeItemBackInInventory(stack.copyWithCount(1));
                return;
            }
            proxyEntity.bind(pos, owner);
            level.invalidateCapabilities(at);
        }
        level.invalidateCapabilities(pos);
    }

    /** 主控与代理的普通挖掘、扳手和异常替换共享这一份携带快照。 */
    public static ItemStack portable(ShieldedAssemblyBlockEntity machine) {
        ItemStack stack = new ItemStack(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get());
        CompoundTag wrapper = new CompoundTag();
        wrapper.put(PORTABLE_KEY, machine.savePortableData());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapper));
        return stack;
    }
    /** 通用掉落查询不产物；玩家预拆或onRemove负责唯一携物，避免destroyBlock(true)双掉。 */
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) { return List.of(); }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity machine) {
            if (!player.isCreative())
                Block.popResource(level, pos, portable(machine));
            machine.markRemovalHandled();
            ShieldedAssemblyStructure.removeParts(machine);
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
                && !level.captureBlockSnapshots && level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity machine) {
            if (!machine.isRemovalHandled()) {
                Block.popResource(level, pos, portable(machine));
                machine.markRemovalHandled();
            }
            ShieldedAssemblyStructure.removeParts(machine);
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
        return wrenchPickup(context, context.getClickedPos(), state);
    }
    /** 普通挖掘在跨区块未加载时拒绝，避免代理先消失而主控账本无法封存。 */
    public static void guardBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (state.is(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get())
                && state.getValue(EXPANDED)
                && level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity master
                && !ShieldedAssemblyStructure.allLoaded(master)) rejectUnloaded(event);
        if (state.is(FuelProcessingContent.SHIELDED_ASSEMBLY_PART.get())
                && level.getBlockEntity(pos) instanceof ShieldedAssemblyPartBlockEntity proxy) {
            BlockPos masterPos = proxy.masterPos();
            if (masterPos != null && !level.hasChunkAt(masterPos)) { rejectUnloaded(event); return; }
            ShieldedAssemblyBlockEntity master = ShieldedAssemblyStructure.master(level, pos, state);
            if (master != null && !ShieldedAssemblyStructure.allLoaded(master)) rejectUnloaded(event);
        }
    }
    private static void rejectUnloaded(BlockEvent.BreakEvent event) {
        event.setCanceled(true);
        if (event.getPlayer() != null) event.getPlayer().displayClientMessage(Component.translatable(
                "gui.create_nuclear_industry.shielded_assembly.wait.structure"), true);
    }
    static InteractionResult wrenchPickup(UseOnContext context, BlockPos pos, BlockState state) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
        ShieldedAssemblyBlockEntity machine = state.getBlock() instanceof ShieldedAssemblyBlock
                ? level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity found ? found : null
                : ShieldedAssemblyStructure.master(level, pos, state);
        Player player = context.getPlayer();
        if (machine == null || machine.expanded() && !ShieldedAssemblyStructure.allLoaded(machine)) {
            if (player != null) player.displayClientMessage(Component.translatable(
                    "gui.create_nuclear_industry.shielded_assembly.wait.structure"), true);
            return InteractionResult.SUCCESS;
        }
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled())
            return InteractionResult.SUCCESS;
        if (player != null && !player.isCreative()) player.getInventory().placeItemBackInInventory(portable(machine));
        machine.markRemovalHandled();
        ShieldedAssemblyStructure.removeParts(machine, pos);
        if (!pos.equals(machine.getBlockPos())) level.setBlock(machine.getBlockPos(), Blocks.AIR.defaultBlockState(), 35);
        level.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }
    static ItemInteractionResult useItem(ItemStack stack, Level level, BlockPos pos, BlockState state, Player player) {
        if (stack.getItem() instanceof WrenchItem) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        ShieldedAssemblyBlockEntity machine = state.getBlock() instanceof ShieldedAssemblyBlock
                ? level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity found ? found : null
                : ShieldedAssemblyStructure.master(level, pos, state);
        if (machine == null || !machine.acceptsInput(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            ItemStack remainder = machine.insert(stack);
            if (!player.getAbilities().instabuild) stack.shrink(stack.getCount() - remainder.getCount());
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    static InteractionResult useEmpty(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            ShieldedAssemblyBlockEntity machine = state.getBlock() instanceof ShieldedAssemblyBlock
                    ? level.getBlockEntity(pos) instanceof ShieldedAssemblyBlockEntity found ? found : null
                    : ShieldedAssemblyStructure.master(level, pos, state);
            if (machine != null) {
                int taken = machine.takeToPlayer(player, player.isShiftKeyDown());
                if (taken == 0) player.displayClientMessage(Component.translatable(
                        "gui.create_nuclear_industry.shielded_assembly.status", machine.waitStatus(),
                        machine.state().progress(), machine.state().work(), Math.round(machine.getSpeed())), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                         Player player, InteractionHand hand, BlockHitResult hit) {
        return useItem(stack, level, pos, state, player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                          BlockHitResult hit) {
        return useEmpty(level, pos, state, player);
    }
}
