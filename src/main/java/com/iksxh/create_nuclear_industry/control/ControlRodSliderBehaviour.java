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
        if (getWorld() != null && getWorld().isClientSide && !drive.automationLocked()) {
            ControlRodSliderClientAdapter.ensureStarted(
                    drive.getBlockPos(), drive.clientColumnX(), drive.clientColumnZ(),
                    getValue());
        }
    }

    /** 锁只限制输入，继续展示真实棒目标与管理员只读标记。 */
    @Override public boolean mayInteract(Player player) { return !drive.automationLocked() && super.mayInteract(player); }

    public void setServerDisplayedValue(int depthPercent) {
        value = Math.max(0, Math.min(100, depthPercent));
    }

    /** 将服务端批准的深度写入客户端展示值，不修改仪表端口快照。 */
    public void setClientDisplayedValue(int depthPercent) {
        value = Math.max(0, Math.min(100, depthPercent));
    }

    /**
     * 原生深度控件的本格几何；坐标/尺度同时供Create显示与testHit使用，不改变百分数或权威提交。
     * 锚点位于真实块面，使原生框/文字后置矩阵绘制到面外；顶底沿边居中并用等比字体扩大数值，椭球操作区避开方杆/端箍；四侧保留原生球形命中。
     */
    static final class CornerTransform extends CenteredSideValueBoxTransform {
        CornerTransform() {
            // 基类先虚调用getScale、随后设初始UP；在两阶段均使用确定尺度，不能读未初始化字段。
            scale = getScale();
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            return switch (direction) {
                case UP -> new Vec3(.5, 1, .0875);
                case DOWN -> new Vec3(.5, 0, .0875);
                case SOUTH -> new Vec3(.25, .75, 1);
                case NORTH -> new Vec3(.75, .75, 0);
                case EAST -> new Vec3(1, .75, .75);
                case WEST -> new Vec3(0, .75, .25);
            };
        }

        @Override
        public float getScale() {
            return direction == null || direction.getAxis() == Direction.Axis.Y ? .20f : .40f;
        }

        /** 顶底字号与基准尺度合计为原两倍；字体三个轴等比，四侧继续用Create默认值。 */
        @Override
        public float getFontScale() {
            return direction == null || direction.getAxis() == Direction.Axis.Y ? 1.8f / 64 : super.getFontScale();
        }

        /**
         * Create已将命中点减去blockPos；顶底只接受本格边缘严格椭球，法向仍用当前缓存尺度的一半。
         * 保留Sided面启用条件；四侧完全交回原生命中，不改变长按、百分数或服务端批准路径。
         */
        @Override
        public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 hit) {
            if (direction == null || direction.getAxis() != Direction.Axis.Y) {
                return super.testHit(level, pos, state, hit);
            }
            if (!isSideActive(state, direction)) {
                return false;
            }
            Vec3 center = getLocalOffset(level, pos, state);
            double x = (hit.x - center.x) / .35;
            double y = (hit.y - center.y) / (scale / 2.);
            double z = (hit.z - center.z) / .075;
            return x * x + y * y + z * z < 1;
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
