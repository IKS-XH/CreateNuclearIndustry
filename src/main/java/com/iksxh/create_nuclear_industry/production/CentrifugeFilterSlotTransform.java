package com.iksxh.create_nuclear_industry.production;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 将原生过滤槽放到各物料面的上缘角落，避开物流管道占用的中央截面。 */
final class CentrifugeFilterSlotTransform extends ValueBoxTransform.Sided {
    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(1, 15, 15.5);
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction side) {
        return side != Direction.DOWN;
    }
}
