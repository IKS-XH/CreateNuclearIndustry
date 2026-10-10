package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 单个控制棒驱动器的 Create ValueSettings 界面适配器。 */
public final class ControlRodSliderBehaviour extends ScrollValueBehaviour {
    private final ControlRodDriveBlockEntity drive;

    public ControlRodSliderBehaviour(ControlRodDriveBlockEntity drive) {
        super(net.minecraft.network.chat.Component.translatable(
                        "block.create_nuclear_industry.control_rod_drive"),
                drive,
                new CornerTransform());
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

    /**
     * 原生深度控件的本格几何；坐标/尺度同时供Create显示与testHit使用，不改变百分数或权威提交。
     * 锚点位于真实块面，使原生框/文字后置矩阵绘制到面外；切向避开方杆/端箍，保留六面与严格球形命中。
     */
    static final class CornerTransform extends CenteredSideValueBoxTransform {
        CornerTransform() {
            // 基类先虚调用getScale、随后设初始UP；在两阶段均使用确定尺度，不能读未初始化字段。
            scale = getScale();
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            return switch (direction) {
                case UP -> new Vec3(.10, 1, .10);
                case DOWN -> new Vec3(.10, 0, .10);
                case SOUTH -> new Vec3(.25, .75, 1);
                case NORTH -> new Vec3(.75, .75, 0);
                case EAST -> new Vec3(1, .75, .75);
                case WEST -> new Vec3(0, .75, .25);
            };
        }

        @Override
        public float getScale() {
            return direction == null || direction.getAxis() == Direction.Axis.Y ? .18f : .40f;
        }

        @Override
        public CornerTransform fromSide(Direction side) {
            super.fromSide(side);
            // Create原fromSide只更方向；显示transform和testHit都读缓存，切面后必须同步一次。
            scale = getScale();
            return this;
        }
    }
}
