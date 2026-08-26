package com.iksxh.create_nuclear_industry.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** NeoForge 服务端 ticker 到正式反应堆权威循环的桥接。 */
public final class ReactorControlRodTicker {
    private ReactorControlRodTicker() {
    }

    public static <T extends BlockEntity> void advance(
            Level level,
            BlockPos pos,
            BlockState state,
            T blockEntity
    ) {
        if (blockEntity instanceof ReactorInstrumentPortBlockEntity instrument) {
            instrument.tickReactor();
        }
    }
}
