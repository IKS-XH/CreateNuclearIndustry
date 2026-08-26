package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.control.ControlRodScramResult;
import com.iksxh.create_nuclear_industry.control.ControlRodScramService;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshotNbtCodec;
import com.iksxh.create_nuclear_industry.reactor.ReactorControlRodTick;
import com.iksxh.create_nuclear_industry.reactor.ReactorCoolantLedger;
import com.iksxh.create_nuclear_industry.reactor.ReactorServerTick;
import com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 反应堆完整权威快照的唯一拥有者；历史契约短语为 {@code Sole authoritative owner}。 */
public final class ReactorInstrumentPortBlockEntity extends P1MinimalBlockEntity {
    private static final String SNAPSHOT_KEY = "ReactorSnapshot";

    private ReactorSnapshot snapshot = ReactorSnapshot.empty();
    private ReactorStructureDefinition.ScanResult structureScan =
            ReactorStructureDefinition.ScanResult.notScanned();
    private BlockPos structureOrigin;
    private long structureScanCount;

    public ReactorInstrumentPortBlockEntity(BlockPos pos, BlockState state) {
        super(P1BlockEntities.REACTOR_INSTRUMENT_PORT.get(), pos, state);
    }

    /** 返回该反应堆不可变的服务端权威快照。 */
    public ReactorSnapshot snapshot() {
        return snapshot;
    }

    /** 替换权威快照，并安排持久化与客户端同步。 */
    public void setSnapshot(ReactorSnapshot snapshot) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("reactor state can only be changed on the server");
        }
        this.snapshot = Objects.requireNonNull(snapshot, "reactor snapshot is required");
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncControlRodDrives();
        }
    }

    public ReactorStructureDefinition.ScanResult structureScan() {
        return structureScan;
    }

    public BlockPos structureOrigin() {
        return structureOrigin;
    }

    public long structureScanCount() {
        return structureScanCount;
    }

    public boolean structureValid() {
        return structureScan.valid();
    }

    public void updateStructureCache(ReactorStructureScanner.WorldScanResult scan) {
        Objects.requireNonNull(scan, "structure scan is required");
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("structure cache can only be changed on the server");
        }
        structureScan = scan.contract();
        structureOrigin = scan.origin();
        structureScanCount++;
        initializeAndSyncControlRods();
        if (level != null && !level.isClientSide && structureScan.valid()) {
            updateRedstoneScram(level.hasNeighborSignal(worldPosition));
        }
    }

    public ControlRodScramResult updateRedstoneScram(boolean powered) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("SCRAM can only be changed on the server");
        }
        return ControlRodScramService.apply(this, powered);
    }

    /** 只推进控制棒执行器状态，不执行完整热、冷却和损伤模拟。 */
    public boolean tickControlRods() {
        if (level != null && level.isClientSide) {
            return false;
        }
        if (!structureScan.valid()) {
            return false;
        }
        ReactorSnapshot next = ReactorControlRodTick.advance(snapshot);
        if (next == snapshot) {
            return false;
        }
        setSnapshot(next);
        return true;
    }

    /** 执行一个服务端 tick 的完整 P1 反应堆权威循环。 */
    public boolean tickReactor() {
        if (level == null || level.isClientSide || !structureScan.valid()
                || !snapshotMatchesStructure()) {
            return false;
        }
        ReactorServerTick.Result result = ReactorServerTick.advance(
                snapshot,
                simulationParameters(),
                coolantInput()
        );
        if (result.snapshot().equals(snapshot)) {
            return false;
        }
        setSnapshot(result.snapshot());
        return true;
    }

    /** 结构编辑后，列状态可能残留；列角色不匹配当前结构时暂不推进模拟。 */
    private boolean snapshotMatchesStructure() {
        return snapshot.fuelColumns().keySet().stream().allMatch(position ->
                structureScan.columns().get(position) != null
                        && structureScan.columns().get(position).type()
                        == ReactorStructureDefinition.ColumnType.FUEL)
                && snapshot.controlRodColumns().keySet().stream().allMatch(position ->
                structureScan.columns().get(position) != null
                        && structureScan.columns().get(position).type()
                        == ReactorStructureDefinition.ColumnType.CONTROL_ROD);
    }

    private ReactorServerTick.CoolantInput coolantInput() {
        double perPort = Math.max(0.0D, P1ServerConfig.VALUES.perPortFlowMbPerTick.get());
        List<ReactorCoolantLedger.Port> ports = new ArrayList<>();
        for (ReactorStructureDefinition.LocalPosition position :
                structureScan.ports().getOrDefault(
                        ReactorStructureDefinition.PortType.COLD_COOLANT, List.of())) {
            ports.add(ReactorCoolantLedger.Port.cold(
                    "cold:" + position.x() + ":" + position.y() + ":" + position.z(), perPort));
        }
        for (ReactorStructureDefinition.LocalPosition position :
                structureScan.ports().getOrDefault(
                        ReactorStructureDefinition.PortType.HOT_COOLANT, List.of())) {
            ports.add(ReactorCoolantLedger.Port.hot(
                    "hot:" + position.x() + ":" + position.y() + ":" + position.z(), perPort));
        }
        return new ReactorServerTick.CoolantInput(
                ReactorCoolantLedger.summarizePorts(ports, perPort),
                Math.max(0L, P1ServerConfig.VALUES.hotInventoryCapacityMb.get().longValue()),
                Math.max(1.0E-12D, P1ServerConfig.VALUES.coolantAbsorptionHuPerMb.get())
        );
    }

    private static ReactorSimulationParameters simulationParameters() {
        return new ReactorSimulationParameters(
                P1ServerConfig.VALUES.baseHeatPerFuelBlockHuPerTick.get(),
                P1ServerConfig.VALUES.fuelBurnTimeHours.get(),
                P1ServerConfig.VALUES.damageHeatThresholdHuPerTick.get(),
                P1ServerConfig.VALUES.damageRatePerTickHuLoad.get(),
                P1ServerConfig.VALUES.damageTransferRate.get(),
                P1ServerConfig.VALUES.controlRodFailureThreshold.get(),
                P1ServerConfig.VALUES.meltdownTriggerFraction.get(),
                Math.max(1, P1ServerConfig.VALUES.meltdownCountdownTicks.get()),
                P1ServerConfig.VALUES.controlResponseExponent.get(),
                P1ServerConfig.VALUES.overclockHeatMultiplier.get(),
                P1ServerConfig.VALUES.overclockBurnMultiplier.get(),
                P1ServerConfig.VALUES.overclockFeedbackGain.get(),
                P1ServerConfig.VALUES.overclockFeedbackExponent.get(),
                P1ServerConfig.VALUES.totalHeatMultiplierCap.get()
        );
    }

    /**
     * 有效结构决定控制棒驱动器；新发现的控制棒列默认完全插入，已有目标在重扫时保留，
     * 并仅同步到 Create 的临时显示状态。
     */
    private void initializeAndSyncControlRods() {
        if (!structureScan.valid() || structureOrigin == null || level == null || level.isClientSide) {
            return;
        }

        TreeMap<CoreColumnPosition, ControlRodColumnState> controlColumns =
                new TreeMap<>(snapshot.controlRodColumns());
        TreeMap<CoreColumnPosition, com.iksxh.create_nuclear_industry.reactor.FuelColumnState> fuelColumns =
                new TreeMap<>(snapshot.fuelColumns());
        boolean changed = false;

        for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                : structureScan.columns().entrySet()) {
            if (entry.getValue().type() != ReactorStructureDefinition.ColumnType.CONTROL_ROD) {
                continue;
            }
            changed |= fuelColumns.remove(entry.getKey()) != null;
            if (!controlColumns.containsKey(entry.getKey())) {
                controlColumns.put(entry.getKey(), ControlRodColumnState.fullyInserted());
                changed = true;
            }
        }

        if (changed) {
            setSnapshot(snapshot.withColumns(fuelColumns, controlColumns));
        }

        syncControlRodDrives();
    }

    private void syncControlRodDrives() {
        if (!structureScan.valid() || structureOrigin == null || level == null || level.isClientSide) {
            return;
        }

        for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                : structureScan.columns().entrySet()) {
            if (entry.getValue().type() != ReactorStructureDefinition.ColumnType.CONTROL_ROD) {
                continue;
            }
            ControlRodColumnState state = snapshot.controlRodColumns().get(entry.getKey());
            if (state == null) {
                continue;
            }
            BlockPos drivePos = structureOrigin.offset(
                    entry.getValue().capPosition().x(),
                    entry.getValue().capPosition().y(),
                    entry.getValue().capPosition().z());
            if (level.getBlockEntity(drivePos) instanceof ControlRodDriveBlockEntity drive) {
                drive.setServerColumnHint(entry.getKey().x(), entry.getKey().z());
                drive.setServerDisplayedDepthPercent(toPercent(state.targetDepth()));
            }
        }
    }

    private static int toPercent(double depth) {
        return (int) Math.round(depth * 100.0D);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ReactorStructureLifecycle.scheduleRescanAround(level, worldPosition);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put(SNAPSHOT_KEY, ReactorSnapshotNbtCodec.encode(snapshot));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        snapshot = ReactorSnapshotNbtCodec.decode(
                tag.contains(SNAPSHOT_KEY) ? tag.getCompound(SNAPSHOT_KEY) : null);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put(SNAPSHOT_KEY, ReactorSnapshotNbtCodec.encode(snapshot));
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
