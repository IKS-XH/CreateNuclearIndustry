package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 仅为跨格叶片提供客户端渲染锚点；转子不持有流体、SU 或动画权威状态。 */
public final class TurbineRotorBlockEntity extends BlockEntity {
    public TurbineRotorBlockEntity(BlockPos pos, BlockState state) {
        super(TurbineContent.ROTOR_BE.get(), pos, state);
    }
}
