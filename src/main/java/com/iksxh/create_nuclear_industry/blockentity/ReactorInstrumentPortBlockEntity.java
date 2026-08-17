package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Sole future owner of the complete reactor snapshot. */
public final class ReactorInstrumentPortBlockEntity extends P1MinimalBlockEntity {
    public ReactorInstrumentPortBlockEntity(BlockPos pos, BlockState state) {
        super(P1BlockEntities.REACTOR_INSTRUMENT_PORT.get(), pos, state);
    }
}
