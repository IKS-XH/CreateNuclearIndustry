package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshotNbtCodec;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.Map;
import java.util.TreeMap;

/** Sole authoritative owner of the complete reactor snapshot. */
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

    /** Returns the immutable server-authoritative snapshot for this reactor. */
    public ReactorSnapshot snapshot() {
        return snapshot;
    }

    /** Replaces the authoritative snapshot and schedules persistence plus client sync. */
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
    }

    /**
     * A valid structure is the source of truth for which drives exist. A
     * newly discovered control-rod column gets one authoritative fully
     * inserted state; existing targets are preserved across rescans and only
     * mirrored into Create's transient display behaviour.
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
            setSnapshot(new ReactorSnapshot(
                    fuelColumns,
                    controlColumns,
                    snapshot.coldCoolantMb(),
                    snapshot.hotCoolantMb(),
                    snapshot.meltdownProgressTicks(),
                    snapshot.meltdownCountdownStarted()
            ));
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
