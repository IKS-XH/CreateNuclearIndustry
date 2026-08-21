package com.iksxh.create_nuclear_industry.control;

import net.minecraft.core.BlockPos;

/** Small package-private-free factory for response construction shared by network and Create adapters. */
public final class ControlRodSliderResponsePayloadFactory {
    private ControlRodSliderResponsePayloadFactory() {
    }

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
