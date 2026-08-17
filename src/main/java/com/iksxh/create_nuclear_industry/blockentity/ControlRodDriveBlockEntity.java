package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Empty server-persisted shell; slider behavior is intentionally deferred. */
public final class ControlRodDriveBlockEntity extends P1MinimalBlockEntity {
    public ControlRodDriveBlockEntity(BlockPos pos, BlockState state) {
        super(P1BlockEntities.CONTROL_ROD_DRIVE.get(), pos, state);
    }
}
