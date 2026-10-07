package com.iksxh.create_nuclear_industry.turbine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/**
 * 将服务端唯一机主的总 SU 从一个轴发布到 Create 动力网，并通过另一轴传播转动。
 * 容量单位 SU、转速单位 RPM；内部连接仅在服务端完整结构及相关区块有效时存在。
 */
public abstract class TurbineShaftPowerSource extends GeneratingKineticBlockEntity {
    private float lastAssignedSu = Float.NaN;
    private float lastAssignedRpm = Float.NaN;
    private BlockPos lastLinkedShaft;
    private float generatedRpmSnapshot;
    private boolean restoringSavedSource;

    protected TurbineShaftPowerSource(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 返回服务端核验后的本轴生成 SU；整机只能有一个非零发布轴。 */
    protected abstract float assignedSu();

    /** 返回本轴带符号生成 RPM，必须处于 Create 当前允许的转速范围。 */
    protected abstract float assignedRpm();

    /** 返回完整且可运行机组中的另一端轴坐标；失效时返回 null。 */
    protected abstract BlockPos linkedShaft();

    @Override
    public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
        BlockPos linked = linkedShaft();
        if (linked != null) neighbours.add(linked);
        // 断开后的首次传播仍需找到旧远端，供 Create 清除指向本轴的 Source 分支。
        if (lastLinkedShaft != null && !lastLinkedShaft.equals(linked) && level != null
                && level.hasChunkAt(lastLinkedShaft)) neighbours.add(lastLinkedShaft);
        return neighbours;
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState from, BlockState to,
                                     BlockPos diff, boolean connectedByAxis, boolean connectedByCogs) {
        BlockPos linked = linkedShaft();
        return linked != null && linked.equals(target.getBlockPos())
                && target instanceof TurbineShaftPowerSource other
                && worldPosition.equals(other.linkedShaft()) ? 1 : 0;
    }

    @Override
    public float getGeneratedSpeed() {
        if (restoringSavedSource) return generatedRpmSnapshot;
        return assignedSu() > 0 ? assignedRpm() : 0;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        // 与 Create 的 AddedCapacity 同存本机生成 RPM；Speed 可能是更快的外部网速。
        if (!clientPacket && lastCapacityProvided > 0 && generatedRpmSnapshot > 0
                && tag.contains("Network")) {
            tag.getCompound("Network").putFloat("TurbineGeneratedRpm", generatedRpmSnapshot);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        generatedRpmSnapshot = !clientPacket && tag.contains("Network")
                ? tag.getCompound("Network").getFloat("TurbineGeneratedRpm") : 0;
    }

    @Override
    public void initialize() {
        // 当前格式存档恢复后账本历史归零；让 Create 原生 addSilently 核销本轴保存的容量。
        if (level != null && !level.isClientSide && lastCapacityProvided > 0
                && generatedRpmSnapshot > 0 && hasNetwork() && getGeneratedSpeed() == 0) {
            restoringSavedSource = true;
            try {
                super.initialize();
            } finally {
                restoringSavedSource = false;
            }
            if (hasNetwork()) {
                var network = getOrCreateNetwork();
                network.sources.remove(this);
                network.updateCapacity();
            }
            // 当前恢复已清空供汽历史，但保存的Speed仍可能非零。初始化阶段须按保存转速拆源，
            // 否则父tick会从尚未清零的下游反向认领Source，形成没有生成源却持续转动的互指环。
            // 原生传播器只撤销本轴的依赖分支；其他真实生成源保留，并可在后续attach重新带动本轴。
            float previousSpeed = getTheoreticalSpeed();
            detachKinetics();
            removeSource();
            setSpeed(0);
            setNetwork(null);
            onSpeedChanged(previousSpeed);
            sendData();
            return;
        }
        super.initialize();
    }

    @Override
    public float calculateAddedStressCapacity() {
        float rpm = Math.abs(getGeneratedSpeed());
        float capacity = rpm == 0 ? 0 : assignedSu() / rpm;
        // Create 将此单位 RPM 容量再乘实际生成转速；直接返回 SU 会按当前 RPM 虚增。
        lastCapacityProvided = capacity;
        generatedRpmSnapshot = capacity > 0 ? rpm : 0;
        return capacity;
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide) {
            BlockPos linked = linkedShaft();
            if (lastLinkedShaft == null ? linked != null : !lastLinkedShaft.equals(linked)) {
                detachKinetics();
                if (lastLinkedShaft != null && lastLinkedShaft.equals(source)) removeSource();
                lastLinkedShaft = linked;
                attachKinetics();
            }
            float su = assignedSu();
            float rpm = assignedRpm();
            // 服务端逐 tick 查询唯一账本，失效及负载变化只在数值改变时通知 Create。
            if (Float.compare(su, lastAssignedSu) != 0 || Float.compare(rpm, lastAssignedRpm) != 0) {
                float previousSu = lastAssignedSu;
                float previousRpm = lastAssignedRpm;
                lastAssignedSu = su;
                lastAssignedRpm = rpm;
                // Create 的父 tick 校验可能先把无上游源的旧 speed 直接写成 0。
                // 随后同步账本的停机变化时，恢复最后生成转速作为传播器拆源前值，避免 0->0 跳过网络撤销。
                if (previousSu > 0 && su == 0 && previousRpm != 0 && !hasSource()
                        && hasNetwork() && getTheoreticalSpeed() == 0) {
                    setSpeed(previousRpm);
                }
                updateGeneratedRotation();
            }
        }
    }
}
