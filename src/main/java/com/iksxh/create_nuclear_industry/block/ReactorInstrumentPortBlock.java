package com.iksxh.create_nuclear_industry.block;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.AllItems;

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
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ReactorInstrumentPortBlockEntity instrument) {
            instrument.updateRedstoneScram(level.hasNeighborSignal(pos));
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
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
