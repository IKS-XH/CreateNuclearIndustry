package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 两端轴均不持有库存或 SU；逐次向有效侧控制器读取各自固定份额。 */
public final class TurbineOutputShaftBlockEntity extends TurbineShaftPowerSource {
    public TurbineOutputShaftBlockEntity(BlockPos pos, BlockState state) {
        super(TurbineContent.OUTPUT_SHAFT_BE.get(), pos, state);
    }
    private TurbineControllerBlockEntity owner() {
        if (level == null || level.isClientSide || !getBlockState().is(TurbineContent.OUTPUT_SHAFT.get())
                || !TurbineStructure.ticking(level, worldPosition)) return null;
        TurbineControllerBlockEntity candidate = TurbineStructure.ownerForPart(level, worldPosition);
        return candidate != null && candidate.validShaft(worldPosition,
                getBlockState().getValue(TurbineShaftBlock.END)) ? candidate : null;
    }
    @Override protected float assignedSu() {
        TurbineControllerBlockEntity owner = owner();
        return owner == null ? 0 : getBlockState().getValue(TurbineShaftBlock.END)
                == TurbineShaftBlock.End.FRONT ? owner.frontSu() : owner.rearSu();
    }
    @Override protected float assignedRpm() {
        TurbineControllerBlockEntity owner = owner();
        return owner == null ? 0 : owner.signedRpm();
    }
}
