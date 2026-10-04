package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** 后轴不持有库存或 SU；逐次向已加载且核验有效的前端控制器读取固定份额。 */
public final class TurbineOutputShaftBlockEntity extends TurbineShaftPowerSource {
    public TurbineOutputShaftBlockEntity(BlockPos pos, BlockState state) {
        super(TurbineContent.OUTPUT_SHAFT_BE.get(), pos, state);
    }
    private TurbineControllerBlockEntity owner() {
        if (level == null || level.isClientSide || !getBlockState().is(TurbineContent.OUTPUT_SHAFT.get())
                || !TurbineStructure.ticking(level, worldPosition)) return null;
        Direction facing = getBlockState().getValue(TurbinePartBlock.MACHINE_FACING);
        for (int length = 3; length <= 18; length++) {
            BlockPos front = worldPosition.relative(facing, length - 1);
            if (!TurbineStructure.ticking(level, front)) continue;
            if (level.getBlockEntity(front) instanceof TurbineControllerBlockEntity candidate
                    && candidate.validRear(worldPosition)) return candidate;
        }
        return null;
    }
    @Override protected float assignedSu() {
        TurbineControllerBlockEntity owner = owner();
        return owner == null ? 0 : owner.rearSu();
    }
    @Override protected float assignedRpm() {
        TurbineControllerBlockEntity owner = owner();
        return owner == null ? 0 : owner.signedRpm();
    }
}
