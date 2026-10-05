package com.iksxh.create_nuclear_industry.gametest.turbine;

import com.iksxh.create_nuclear_industry.turbine.TurbineShaftPowerSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 探针实体轴从唯一机主读取总容量及结构有效的内部连接。 */
public final class TurbineProbeShaftBlockEntity extends TurbineShaftPowerSource {
    private int updatePacketCount;

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

    @Override
    protected BlockPos linkedShaft() {
        if (level == null || level.isClientSide || !getBlockState().is(TurbineProbeContent.shaft())) return null;
        boolean rear = getBlockState().getValue(TurbineProbeShaftBlock.REAR);
        BlockPos ownerPos = worldPosition.west().north(rear ? 5 : 0);
        return level.hasChunkAt(ownerPos)
                && level.getBlockEntity(ownerPos) instanceof TurbineProbeOwnerBlockEntity owner
                ? owner.otherFor(worldPosition) : null;
    }

    /** 仅供定向 GameTest 确认 Create 源撤销时是否向下游发送实体更新包。 */
    @Override
    public void sendData() {
        updatePacketCount++;
        super.sendData();
    }

    public int updatePacketCount() { return updatePacketCount; }
}
