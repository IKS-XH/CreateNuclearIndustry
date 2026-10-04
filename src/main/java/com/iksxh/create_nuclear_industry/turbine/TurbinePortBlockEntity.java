package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 给 Create 流体管道提供端口方块实体身份；此实体不保存流体。
 * 进排汽能力始终由服务端控制器的唯一账本代理，卸载后旧能力失效。
 */
public final class TurbinePortBlockEntity extends BlockEntity {
    public TurbinePortBlockEntity(BlockPos pos, BlockState state) {
        super(TurbineContent.PORT_BE.get(), pos, state);
    }
}
