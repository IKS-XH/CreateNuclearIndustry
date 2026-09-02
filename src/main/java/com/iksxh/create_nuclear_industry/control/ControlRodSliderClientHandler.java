package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 客户端响应应用入口；只更新服务端最终批准的临时展示缓存。 */
public final class ControlRodSliderClientHandler {
    private ControlRodSliderClientHandler() {
    }

    /** 在客户端线程应用最终响应；拖动中的 Create 面板完全由本地鼠标状态驱动。 */
    public static void handle(ControlRodSliderResponsePayload response) {
        boolean applyFinalDisplay = ControlRodSliderClientAdapter.applyResponsePolicy(response);
        if (!applyFinalDisplay) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && response != null && response.drivePos() != null) {
            BlockEntity blockEntity = minecraft.level.getBlockEntity(response.drivePos());
            if (blockEntity instanceof ControlRodDriveBlockEntity drive) {
                drive.applyClientSliderResponse(response);
            }
        }
    }
}
