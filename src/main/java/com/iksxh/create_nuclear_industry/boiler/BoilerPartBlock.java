package com.iksxh.create_nuclear_industry.boiler;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/** 控制器与两类端口共用的水平 facing 契约，方向始终指向炉壳外侧。 */
public final class BoilerPartBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private BoilerPartBlock() {}
}
