package com.iksxh.create_nuclear_industry.industrial;

import com.iksxh.create_nuclear_industry.config.IndustrialFluidConfig;
import com.iksxh.create_nuclear_industry.content.IndustrialFluidContent;
import com.iksxh.create_nuclear_industry.goggle.GoggleTooltip;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/** 唯一泵周转库存所有者；管段零库存，服务端统一调度，停转和配置缩小均保留余量。 */
public final class IndustrialPumpBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {
    private FluidStack cache = FluidStack.EMPTY;
    private int actualFlow;
    private int bottleneck, publishedBottleneck;
    private String status = "stopped";
    private int sourceCursor, targetCursor;
    public IndustrialPumpBlockEntity(BlockPos pos, BlockState state) { super(IndustrialFluidContent.PUMP_ENTITY.get(), pos, state); }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
    @Override public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level instanceof ServerLevel server) IndustrialNetworkScheduler.invalidate(server, worldPosition);
    }
    @Override public float calculateStressApplied() { return lastStressApplied = IndustrialFluidConfig.STRESS.get().floatValue(); }
    public int flowLimit() { return IndustrialFlowBudget.pumpLimit(IndustrialFluidConfig.PUMP_FLOW.get(), IndustrialFluidConfig.RATED_RPM.get(), getSpeed()); }
    public Direction outputDirection() {
        Direction facing = getBlockState().getValue(IndustrialPumpBlock.FACING);
        // Create轴旋转正负约定：轴正方向转速为正时朝FACING轴向抽出，翻向或反转都会反转流向。
        return getSpeed() >= 0 ? facing : facing.getOpposite();
    }
    public FluidStack cachedFluid() { return cache.copy(); }
    void setCache(FluidStack next) { cache = next.copy(); setChanged(); }
    int nextCursor(int size, boolean source) { return size == 0 ? 0 : Math.floorMod(source ? sourceCursor++ : targetCursor++, size); }
    void resetBottleneck() { bottleneck = flowLimit(); }
    void considerBottleneck(int capacity) { bottleneck = Math.min(bottleneck, Math.max(0, capacity)); }
    void display(int actual, String reason) {
        if (actualFlow != actual || !status.equals(reason) || publishedBottleneck != bottleneck) { actualFlow = actual; status = reason; publishedBottleneck = bottleneck; sendData(); }
    }
    public int actualFlow() { return actualFlow; }
    @Override public void onLoad() { super.onLoad(); if (level instanceof ServerLevel server) IndustrialNetworkScheduler.register(server, this); }
    @Override public void invalidate() { if (level instanceof ServerLevel server) IndustrialNetworkScheduler.unregister(server, this); super.invalidate(); }
    public CompoundTag portableData() {
        CompoundTag tag = new CompoundTag(); tag.putString("id", "create_nuclear_industry:industrial_fluid_pump");
        if (level != null && !cache.isEmpty()) tag.put("Cache", cache.save(level.registryAccess())); return tag;
    }
    public void restorePortable(CompoundTag tag) {
        if (level == null || level.isClientSide) return;
        cache = tag.contains("Cache") ? FluidStack.parseOptional(level.registryAccess(), tag.getCompound("Cache")) : FluidStack.EMPTY; setChanged();
    }
    /** 原生Ctrl选取在clone后追加NBT时调用此入口；持液只由唯一掉落portableData搬移，世界保存仍保留Cache。 */
    @Override public void removeComponentsFromTag(CompoundTag tag) { super.removeComponentsFromTag(tag); tag.remove("Cache"); }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean packet) {
        super.write(tag, registries, packet); if (!cache.isEmpty()) tag.put("Cache", cache.save(registries));
        if (packet) { tag.putInt("ActualFlow", actualFlow); tag.putInt("Bottleneck", publishedBottleneck); tag.putString("Status", status); }
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean packet) {
        super.read(tag, registries, packet); cache = tag.contains("Cache") ? FluidStack.parseOptional(registries, tag.getCompound("Cache")) : FluidStack.EMPTY;
        if (packet) { actualFlow = tag.getInt("ActualFlow"); publishedBottleneck = tag.getInt("Bottleneck"); status = tag.getString("Status"); }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        tooltip.add(GoggleTooltip.indentFirstLine(Component.translatable("industrial.create_nuclear_industry.pump", actualFlow, flowLimit(), cache.getAmount())));
        tooltip.add(Component.translatable("industrial.create_nuclear_industry.bottleneck", publishedBottleneck));
        tooltip.add(Component.translatable("industrial.create_nuclear_industry.state." + status)); return true;
    }
}
