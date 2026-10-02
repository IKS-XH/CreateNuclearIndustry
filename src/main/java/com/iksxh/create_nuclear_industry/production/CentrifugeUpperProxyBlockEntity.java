package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 上段仅提供 Create capability 发现所需的方块实体锚点，不保存机器状态、不参与 tick。 */
public final class CentrifugeUpperProxyBlockEntity extends BlockEntity {
    public CentrifugeUpperProxyBlockEntity(BlockPos pos, BlockState state) {
        super(FuelProcessingContent.CENTRIFUGE_UPPER_PROXY_BE.get(), pos, state);
    }
}
