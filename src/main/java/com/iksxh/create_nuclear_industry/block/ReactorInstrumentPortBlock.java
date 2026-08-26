package com.iksxh.create_nuclear_industry.block;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorControlRodTicker;
import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDiagnostics;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.AllItems;

/**
 * 反应堆唯一仪表端口的方块入口。
 *
 * <p>仪表端口方块实体拥有结构缓存和反应堆快照；本方块只负责创建实体、挂接
 * 服务端 ticker、监听红石 SCRAM 信号以及响应 Create 扳手的显式重扫。</p>
 */
public final class ReactorInstrumentPortBlock extends P1EntityBlockBase implements IWrenchable {
    public static final MapCodec<ReactorInstrumentPortBlock> CODEC = simpleCodec(ReactorInstrumentPortBlock::new);

    public ReactorInstrumentPortBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorInstrumentPortBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (type != P1BlockEntities.REACTOR_INSTRUMENT_PORT.get()) {
            return null;
        }
        return ReactorControlRodTicker::advance;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        // 红石状态只在服务端写入 SCRAM；客户端不拥有反应堆控制状态。
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ReactorInstrumentPortBlockEntity instrument) {
            instrument.updateRedstoneScram(level.hasNeighborSignal(pos));
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        // 放置或替换后立即采样一次，确保初始高电平不会等到下一次邻居更新。
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())
                && level.getBlockEntity(pos) instanceof ReactorInstrumentPortBlockEntity instrument) {
            instrument.updateRedstoneScram(level.hasNeighborSignal(pos));
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!context.getItemInHand().is(AllItems.WRENCH.get())) {
            return InteractionResult.PASS;
        }
        // 客户端只确认交互，真正扫描和消息内容由服务端决定。
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        var scan = ReactorStructureLifecycle.rescanInstrumentPortNow(
                context.getLevel(), context.getClickedPos());
        if (scan != null) {
            player.displayClientMessage(ReactorStructureDiagnostics.message(scan), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
