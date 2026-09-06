package com.iksxh.create_nuclear_industry.block;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderService;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnRepairTransaction;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 控制棒驱动器方块；其交互行为由对应方块实体的 Create behaviour 提供。 */
public final class ControlRodDriveBlock extends P1EntityBlockBase {
    public static final MapCodec<ControlRodDriveBlock> CODEC = simpleCodec(ControlRodDriveBlock::new);

    public ControlRodDriveBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ControlRodDriveBlockEntity(pos, state);
    }

    /**
     * 处理玩家使用合金钢板维修控制棒列的服务端入口。
     *
     * <p>只有正式注册的 {@code steel_plate} 被本方块拦截；其他物品继续交给原版和
     * Create 交互链。客户端只返回预测结果，控制棒列定位、失效阈值和物品消耗全部由
     * 服务端事务完成。</p>
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
        if (!incoming.is(ModItems.STEEL_PLATE.get())) {
            return super.useItemOn(incoming, state, level, pos, player, hand, hit);
        }
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }
        if (player == null) {
            return ItemInteractionResult.CONSUME;
        }

        ControlRodColumnRepairTransaction.Result result =
                ControlRodSliderService.repairFromPlayer(player, pos, incoming);
        if (result.success()) {
            player.setItemInHand(hand, result.remainingInput());
        }
        player.displayClientMessage(repairMessageFor(result), true);
        return ItemInteractionResult.CONSUME;
    }

    /** 将控制棒维修事务结果转换为本地化动作栏提示。 */
    private static Component repairMessageFor(ControlRodColumnRepairTransaction.Result result) {
        if (result == null) {
            return repairMessage("invalid_drive");
        }
        if (result.success() && result.jammedCleared()) {
            return repairMessage("unjammed");
        }
        return repairMessage(result.status().translationKeySuffix());
    }

    /** 构造控制棒维修提示，不把服务端拒绝原因硬编码到客户端。 */
    private static Component repairMessage(String suffix) {
        return Component.translatable("message.create_nuclear_industry.control_rod_repair." + suffix);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
