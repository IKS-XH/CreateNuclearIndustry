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
import java.util.HashSet;

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
    private boolean sourceAuditPending;
    private long nextSourceAudit;

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
            float savedGeneratedRpm = generatedRpmSnapshot;
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
            sourceAuditPending = true;
            auditSourceChain(savedGeneratedRpm);
            return;
        }
        super.initialize();
    }

    /** 正常供汽、外部动力或加载回接改变转动时，只安排一次零自产轴的上游审计。 */
    @Override public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level != null && !level.isClientSide && getTheoreticalSpeed() != 0 && assignedSu() == 0) {
            sourceAuditPending = true;
            nextSourceAudit = 0;
        }
    }

    /**
     * 只沿已加载Source链寻找真实生成源；证明无源终点或环路后，交原生传播器撤销其依赖分支。
     * 未知/卸载/过长链保守等待，不扫描世界、不清整个网络，也不扣除其他成员的未加载容量。
     */
    private void auditSourceChain(float fallbackRpm) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return;
        KineticBlockEntity current = this;
        var seen = new HashSet<BlockPos>();
        for (int checked = 0; checked < 4096; checked++) {
            if (current.getGeneratedSpeed() != 0) { sourceAuditPending = false; return; }
            if (!seen.add(current.getBlockPos()) || current.source == null) {
                float previous = current.getTheoreticalSpeed();
                // 必须先断开旧Source再传播；否则环上的另一段会把正在撤销的节点重新当成上游。
                if (current.hasNetwork()) {
                    var network = current.getOrCreateNetwork();
                    network.sources.remove(current);
                    network.updateCapacity();
                }
                current.removeSource();
                current.setSpeed(previous != 0 ? previous : fallbackRpm);
                current.detachKinetics();
                current.setSpeed(0);
                current.onSpeedChanged(previous);
                current.sendData();
                current.attachKinetics();
                sourceAuditPending = false;
                return;
            }
            BlockPos at = current.source;
            var chunk = server.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4);
            if (chunk == null) break;
            var entity = chunk.getBlockEntity(at, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK);
            if (!(entity instanceof KineticBlockEntity next) || entity.isRemoved()) break;
            current = next;
        }
        sourceAuditPending = true;
        nextSourceAudit = level.getGameTime() + 20;
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
                if (su == 0 && (previousSu > 0 || Float.isNaN(previousSu))) {
                    if (hasNetwork()) {
                        var network = getOrCreateNetwork();
                        network.sources.remove(this);
                        network.updateCapacity();
                    }
                    // 本轴贡献已实际撤销；若仍被外源带动，保存时不能再把旧自产额度记入外源网络。
                    // 撤销前的标签仍保留原快照，供正常加载按Create账本核销，不能在write中盲目裁剪。
                    lastCapacityProvided = 0;
                    generatedRpmSnapshot = 0;
                    sourceAuditPending = true;
                    nextSourceAudit = 0;
                }
                updateGeneratedRotation();
            }
            if (sourceAuditPending && level.getGameTime() >= nextSourceAudit)
                auditSourceChain(rpm);
        }
    }
}
