package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Client-side response application; only the transient Create display cache is touched. */
public final class ControlRodSliderClientHandler {
    private ControlRodSliderClientHandler() {
    }

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
