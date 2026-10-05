package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 两端轴均不持有库存；前轴发布唯一总容量，后轴经机内连接传递同网转动。 */
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
        return owner != null && getBlockState().getValue(TurbineShaftBlock.END)
                == TurbineShaftBlock.End.FRONT ? owner.totalSu() : 0;
    }
    @Override protected float assignedRpm() {
        TurbineControllerBlockEntity owner = owner();
        return owner == null ? 0 : owner.signedRpm();
    }

    @Override protected BlockPos linkedShaft() {
        TurbineControllerBlockEntity owner = owner();
        if (owner == null || owner.currentForm() == null) return null;
        BlockPos other = getBlockState().getValue(TurbineShaftBlock.END) == TurbineShaftBlock.End.FRONT
                ? owner.currentForm().rear() : owner.currentForm().front();
        TurbineShaftBlock.End opposite = getBlockState().getValue(TurbineShaftBlock.END)
                == TurbineShaftBlock.End.FRONT ? TurbineShaftBlock.End.REAR : TurbineShaftBlock.End.FRONT;
        return owner.validShaft(other, opposite) ? other : null;
    }
}
