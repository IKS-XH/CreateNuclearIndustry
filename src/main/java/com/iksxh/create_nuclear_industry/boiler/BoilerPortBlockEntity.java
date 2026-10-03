package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Create 管网识别端口所需的轻量实体；不保存或复制水汽库存。 */
public final class BoilerPortBlockEntity extends BlockEntity {
    public BoilerPortBlockEntity(BlockPos pos, BlockState state) {
        super(BoilerContent.PORT_BE.get(), pos, state);
    }
}
