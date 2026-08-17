package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Shared empty shell for cold, hot and refueling ports; it owns no simulation state. */
public final class ReactorPortBlockEntity extends P1MinimalBlockEntity {
    public ReactorPortBlockEntity(BlockPos pos, BlockState state) {
        super(P1BlockEntities.REACTOR_PORT.get(), pos, state);
    }
}
