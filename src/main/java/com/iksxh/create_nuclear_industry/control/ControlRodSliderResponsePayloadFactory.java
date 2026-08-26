package com.iksxh.create_nuclear_industry.control;

import net.minecraft.core.BlockPos;

/** 为网络层和 Create 适配器统一构造滑块响应载荷的工厂。 */
public final class ControlRodSliderResponsePayloadFactory {
    private ControlRodSliderResponsePayloadFactory() {
    }

    /** 将服务端规则结果映射为不携带额外权威状态的线协议响应。 */
    public static ControlRodSliderResponsePayload from(BlockPos drivePos, ControlRodSliderResult result) {
        return new ControlRodSliderResponsePayload(
                drivePos,
                result.columnX(),
                result.columnZ(),
                result.depthPercent(),
                result.phase().wireCode(),
                result.status().wireCode(),
                result.dragId(),
                result.accepted() || result.authoritativeDepth()
        );
    }
}
