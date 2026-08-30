package com.iksxh.create_nuclear_industry.block;

import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.reactor.FuelRefuelingTransaction;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** 冷却剂或补料端口方块；具体端口类型由放置位置和结构扫描结果解释。 */
public final class ReactorPortBlock extends P1EntityBlockBase {
    public static final MapCodec<ReactorPortBlock> CODEC = simpleCodec(ReactorPortBlock::new);

    public ReactorPortBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorPortBlockEntity(pos, state);
    }

    /**
     * 处理玩家对顶部换料端口的手持物品交互。
     *
     * <p>客户端只返回预测成功，不拥有燃料或玩家库存状态；服务端验证端口绑定、局部
     * 裂变发热和物品类型后，才提交唯一反应堆快照，并把事务余量写回交互手。失败时
     * 返回可消费动作结果阻止错误物品继续执行原版 {@code useOn}，但不消耗物品。</p>
     */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack incoming,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!state.is(P1Blocks.REACTOR_REFUELING_PORT.get())) {
            return super.useItemOn(incoming, state, level, pos, player, hand, hit);
        }
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }
        if (player == null) {
            return ItemInteractionResult.CONSUME;
        }
        return handlePlayerInteraction(level, pos, player, hand, incoming);
    }

    /** 空手路径的兼容入口；正式 1.21.1 交互通常已经在 {@link #useItemOn} 中处理。 */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!state.is(P1Blocks.REACTOR_REFUELING_PORT.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player == null) {
            return InteractionResult.CONSUME;
        }
        return handlePlayerInteraction(level, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY)
                .result();
    }

    /** 在服务端执行一次玩家单列换料动作；交互手为空时取出，否则尝试装入一个组件。 */
    private static ItemInteractionResult handlePlayerInteraction(
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            ItemStack incoming
    ) {
        if (!(level.getBlockEntity(pos) instanceof ReactorPortBlockEntity port)) {
            player.displayClientMessage(message("invalid_port"), true);
            return ItemInteractionResult.CONSUME;
        }

        if (incoming == null || incoming.isEmpty()) {
            FuelRefuelingTransaction.Result result = port.tryExtractFuel();
            if (result.success()) {
                player.setItemInHand(hand, result.output());
            }
            player.displayClientMessage(messageFor(result), true);
            return ItemInteractionResult.CONSUME;
        }

        FuelRefuelingTransaction.Result result = port.tryInsertFuel(incoming);
        if (result.success()) {
            player.setItemInHand(hand, result.remainingInput());
        }
        player.displayClientMessage(messageFor(result), true);
        return ItemInteractionResult.CONSUME;
    }

    /** 将事务结果转换为不依赖客户端状态的动作栏提示。 */
    private static Component messageFor(FuelRefuelingTransaction.Result result) {
        if (result == null) {
            return message("invalid_port");
        }
        if (result.status() == FuelRefuelingTransaction.Status.INSERTED) {
            return message("inserted");
        }
        if (result.status() == FuelRefuelingTransaction.Status.REMOVED) {
            return message(FuelAssemblyItemCodec.isCooledSpentFuel(result.output())
                    ? "removed_cooled_spent"
                    : "removed_fuel");
        }
        return message(result.status().translationKeySuffix());
    }

    /** 构造换料事务的本地化消息，不把服务端拒绝原因翻译成客户端硬编码文本。 */
    private static Component message(String suffix) {
        return Component.translatable("message.create_nuclear_industry.refueling." + suffix);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
