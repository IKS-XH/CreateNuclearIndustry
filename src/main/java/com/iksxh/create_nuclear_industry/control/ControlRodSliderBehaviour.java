package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/** 单个控制棒驱动器的 Create ValueSettings 界面适配器。 */
public final class ControlRodSliderBehaviour extends ScrollValueBehaviour {
    private final ControlRodDriveBlockEntity drive;

    public ControlRodSliderBehaviour(ControlRodDriveBlockEntity drive) {
        super(net.minecraft.network.chat.Component.translatable(
                        "block.create_nuclear_industry.control_rod_drive"),
                drive,
                new CenteredSideValueBoxTransform());
        this.drive = drive;
        between(0, 100);
        withFormatter(value -> value + "%");
    }

    /** 驱动器展示缓存可以发送到客户端，但永远不作为反应堆状态持久化。 */
    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        if (clientPacket) {
            super.write(tag, registries, true);
        }
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        if (clientPacket) {
            super.read(tag, registries, true);
        }
    }

    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlDown) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide) {
            return;
        }
        ControlRodSliderResult result = ControlRodSliderService.commitFromCreate(
                serverPlayer,
                drive.getBlockPos(),
                valueSetting.row(),
                valueSetting.value());
        ControlRodSliderNetwork.sendResponse(serverPlayer,
                ControlRodSliderResponsePayloadFactory.from(drive.getBlockPos(), result));
    }

    @Override
    public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
        if (player.level() != null && player.level().isClientSide) {
            ControlRodSliderClientAdapter.begin(
                    drive.getBlockPos(), drive.clientColumnX(), drive.clientColumnZ(), getValue());
        }
    }

    @Override
    public void newSettingHovered(ValueSettings valueSetting) {
        if (getWorld() != null && getWorld().isClientSide) {
            ControlRodSliderClientAdapter.ensureStarted(
                    drive.getBlockPos(), drive.clientColumnX(), drive.clientColumnZ(),
                    getValue());
        }
    }

    public void setServerDisplayedValue(int depthPercent) {
        value = Math.max(0, Math.min(100, depthPercent));
    }

    /** 将服务端批准的深度写入客户端展示值，不修改仪表端口快照。 */
    public void setClientDisplayedValue(int depthPercent) {
        value = Math.max(0, Math.min(100, depthPercent));
    }
}
