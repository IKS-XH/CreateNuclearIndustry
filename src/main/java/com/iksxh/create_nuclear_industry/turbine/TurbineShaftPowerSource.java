package com.iksxh.create_nuclear_industry.turbine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 将服务端唯一机主分配的单轴 SU 发布到 Create 动力网。
 * 子类逐次提供该轴当前份额及生成 RPM；份额失效时必须返回零，不得保存上次额度。
 */
public abstract class TurbineShaftPowerSource extends GeneratingKineticBlockEntity {
    private float lastAssignedSu = Float.NaN;
    private float lastAssignedRpm = Float.NaN;

    protected TurbineShaftPowerSource(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 返回服务端核验结构、停机及区块状态后的本轴 SU；另一轴不向本轴重分配。 */
    protected abstract float assignedSu();

    /** 返回本轴带符号生成 RPM，必须处于 Create 当前允许的转速范围。 */
    protected abstract float assignedRpm();

    @Override
    public float getGeneratedSpeed() {
        return assignedSu() > 0 ? assignedRpm() : 0;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float rpm = Math.abs(getGeneratedSpeed());
        float capacity = rpm == 0 ? 0 : assignedSu() / rpm;
        // Create 将此单位 RPM 容量再乘生成转速；直接返回 SU 会按 128 倍虚增。
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide) {
            float su = assignedSu();
            float rpm = assignedRpm();
            // 每 tick 检查权威机主；仅份额或转速变动时通知 Create，避免稳定状态反复发包。
            if (Float.compare(su, lastAssignedSu) != 0 || Float.compare(rpm, lastAssignedRpm) != 0) {
                lastAssignedSu = su;
                lastAssignedRpm = rpm;
                updateGeneratedRotation();
            }
        }
    }
}
