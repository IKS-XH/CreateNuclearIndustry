package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderBehaviour;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderResponsePayload;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Create slider host; all reactor state remains owned by the instrument port. */
public final class ControlRodDriveBlockEntity extends P1MinimalBlockEntity {
    private ControlRodSliderBehaviour slider;
    private int clientColumnX = -1;
    private int clientColumnZ = -1;

    public ControlRodDriveBlockEntity(BlockPos pos, BlockState state) {
        super(P1BlockEntities.CONTROL_ROD_DRIVE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        slider = new ControlRodSliderBehaviour(this);
        behaviours.add(slider);
    }

    public ControlRodSliderBehaviour slider() {
        return slider;
    }

    public int clientColumnX() {
        return clientColumnX;
    }

    public int clientColumnZ() {
        return clientColumnZ;
    }

    /** The structure scan supplies the mapping; the drive does not infer it locally. */
    public void setServerColumnHint(int columnX, int columnZ) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("server slider column cannot be changed on the client");
        }
        if (clientColumnX == columnX && clientColumnZ == columnZ) {
            return;
        }
        clientColumnX = columnX;
        clientColumnZ = columnZ;
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    /** Updates only the non-persistent server-side value shown by Create's widget. */
    public void setServerDisplayedDepthPercent(int depthPercent) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("server slider display cannot be changed on the client");
        }
        int clamped = Math.max(0, Math.min(100, depthPercent));
        if (slider.getValue() == clamped) {
            return;
        }
        slider.setServerDisplayedValue(clamped);
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    /** Applies a server response to the client-side presentation cache only. */
    public void applyClientSliderResponse(ControlRodSliderResponsePayload response) {
        if (level == null || !level.isClientSide || response == null) {
            return;
        }
        clientColumnX = response.columnX();
        clientColumnZ = response.columnZ();
        if (response.accepted() || response.hasAuthoritativeDepth()) {
            slider.setClientDisplayedValue(response.depthPercent());
        }
    }

    /** Reads the single authoritative snapshot without creating a drive-local copy. */
    public ReactorSnapshot readAuthoritativeSnapshot(ReactorInstrumentPortBlockEntity owner) {
        if (owner == null) {
            throw new IllegalArgumentException("reactor instrument port is required");
        }
        return owner.snapshot();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket) {
            tag.putInt("ControlRodColumnX", clientColumnX);
            tag.putInt("ControlRodColumnZ", clientColumnZ);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (clientPacket) {
            clientColumnX = tag.contains("ControlRodColumnX") ? tag.getInt("ControlRodColumnX") : -1;
            clientColumnZ = tag.contains("ControlRodColumnZ") ? tag.getInt("ControlRodColumnZ") : -1;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ReactorStructureLifecycle.rescanAroundNow(level, worldPosition);
        }
    }
}
