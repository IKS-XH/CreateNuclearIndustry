package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 客户端响应应用入口；只更新临时的 Create 展示缓存。 */
public final class ControlRodSliderClientHandler {
    private ControlRodSliderClientHandler() {
    }

    /** 在客户端线程应用驱动器值和已打开 ValueSettings 面板的显示同步。 */
    public static void handle(ControlRodSliderResponsePayload response) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && response != null && response.drivePos() != null) {
            BlockEntity blockEntity = minecraft.level.getBlockEntity(response.drivePos());
            if (blockEntity instanceof ControlRodDriveBlockEntity drive) {
                drive.applyClientSliderResponse(response);
            }
            if (response.accepted() || response.hasAuthoritativeDepth()) {
                ControlRodSliderScreenSync.refresh(response.drivePos(), response.depthPercent());
            }
        }
        ControlRodSliderClientAdapter.finishFromServer(response);
    }
}
