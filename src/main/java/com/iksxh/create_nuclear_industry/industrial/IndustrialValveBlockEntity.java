package com.iksxh.create_nuclear_industry.industrial;

import com.iksxh.create_nuclear_industry.content.IndustrialFluidContent;
import com.iksxh.create_nuclear_industry.goggle.GoggleTooltip;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Create世界内百分比控件；服务端实际红石优先于开度，客户端仅显示同步结果。 */
public final class IndustrialValveBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private ScrollValueBehaviour control;
    private boolean powered;
    private boolean missingSignalChunk;
    public IndustrialValveBlockEntity(BlockPos pos, BlockState state) { super(IndustrialFluidContent.VALVE_ENTITY.get(), pos, state); }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        control = new ScrollValueBehaviour(Component.translatable("industrial.create_nuclear_industry.opening"), this, new ValueBoxTransform.Sided() {
            @Override protected Vec3 getSouthLocation() { return new Vec3(.5, .5, .72); }
            @Override protected boolean isSideActive(BlockState state, Direction side) { return true; }
        });
        control.between(0, 100).withFormatter(v -> v + "%").withCallback(v -> {
            if (level instanceof ServerLevel server) { IndustrialNetworkScheduler.invalidate(server, worldPosition); setChanged(); sendData(); }
        });
        control.setValue(100); behaviours.add(control);
    }
    public int opening() { return control == null ? 100 : control.getValue(); }
    public int effectiveLimit() {
        if (level == null || level.isClientSide) return 0;
        updateSignal();
        if (powered || missingSignalChunk) return 0;
        return (int) ((long) ((IndustrialPipeBlock) getBlockState().getBlock()).flowLimit() * opening() / 100);
    }
    public void updateSignal() {
        if (!(level instanceof ServerLevel server)) return;
        LoadedSignals signals = new LoadedSignals(server);
        boolean next = signals.hasNeighborSignal(worldPosition);
        if (powered != next || missingSignalChunk != signals.missing) {
            powered = next; missingSignalChunk = signals.missing;
            IndustrialNetworkScheduler.invalidate(server, worldPosition); sendData();
        }
    }
    /** 保留SignalGetter原生弱/强红石规则，但每一级读取均只访问已加载表，包括强导体的二阶查询。 */
    private static final class LoadedSignals implements net.minecraft.world.level.SignalGetter {
        private final ServerLevel world;
        private boolean missing;
        private LoadedSignals(ServerLevel world) { this.world = world; }
        public BlockState getBlockState(BlockPos pos) {
            if (!world.hasChunkAt(pos)) missing = true;
            return IndustrialTopology.stateAt(world, pos);
        }
        public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos) {
            if (!world.hasChunkAt(pos)) missing = true;
            return IndustrialTopology.entityAt(world, pos);
        }
        public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        public int getHeight() { return world.getHeight(); }
        public int getMinBuildHeight() { return world.getMinBuildHeight(); }
    }
    @Override public void tick() { super.tick(); if (level != null && !level.isClientSide) updateSignal(); }
    @Override public void onLoad() { super.onLoad(); updateSignal(); }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean packet) { super.write(tag, registries, packet); tag.putBoolean("Powered", powered); tag.putBoolean("SignalChunkMissing", missingSignalChunk); }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean packet) { super.read(tag, registries, packet); powered = tag.getBoolean("Powered"); missingSignalChunk = tag.getBoolean("SignalChunkMissing"); }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(GoggleTooltip.indentFirstLine(Component.translatable("industrial.create_nuclear_industry.valve", opening(), powered || missingSignalChunk ? 0 : opening())));
        if (missingSignalChunk) tooltip.add(Component.translatable("industrial.create_nuclear_industry.state.unloaded_redstone")); return true;
    }
}
