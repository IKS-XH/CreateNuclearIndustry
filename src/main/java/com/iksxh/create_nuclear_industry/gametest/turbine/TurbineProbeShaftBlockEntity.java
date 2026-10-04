package com.iksxh.create_nuclear_industry.gametest.turbine;

import com.iksxh.create_nuclear_industry.turbine.TurbineShaftPowerSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 探针实体轴只向唯一机主取当前份额，不缓存 SU 或替另一端领取。 */
public final class TurbineProbeShaftBlockEntity extends TurbineShaftPowerSource {
    public TurbineProbeShaftBlockEntity(BlockPos pos, BlockState state) {
        super(TurbineProbeContent.shaftEntity(), pos, state);
    }

    @Override
    protected float assignedSu() {
        if (level == null || level.isClientSide || !getBlockState().is(TurbineProbeContent.shaft())) return 0;
        boolean rear = getBlockState().getValue(TurbineProbeShaftBlock.REAR);
        BlockPos ownerPos = worldPosition.west().north(rear ? 5 : 0);
        if (!level.hasChunkAt(ownerPos)) return 0;
        return level.getBlockEntity(ownerPos) instanceof TurbineProbeOwnerBlockEntity owner
                ? owner.shareFor(worldPosition) : 0;
    }

    @Override
    protected float assignedRpm() { return 128; }
}
