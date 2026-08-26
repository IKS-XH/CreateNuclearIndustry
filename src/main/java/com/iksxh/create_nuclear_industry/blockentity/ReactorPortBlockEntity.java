package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 冷、热和补料端口共用的空壳方块实体；不拥有任何反应堆模拟状态。 */
public final class ReactorPortBlockEntity extends P1MinimalBlockEntity {
    public ReactorPortBlockEntity(BlockPos pos, BlockState state) {
        super(P1BlockEntities.REACTOR_PORT.get(), pos, state);
    }

    /** 读取仪表端口唯一权威快照，不创建端口本地副本。 */
    public ReactorSnapshot readAuthoritativeSnapshot(ReactorInstrumentPortBlockEntity owner) {
        if (owner == null) {
            throw new IllegalArgumentException("reactor instrument port is required");
        }
        return owner.snapshot();
    }
}
