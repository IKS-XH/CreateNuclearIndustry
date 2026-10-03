package com.iksxh.create_nuclear_industry.heat;

import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 单格设备权威所有者；服务端 tick 原子换热，客户端仅接收罐量、状态与热量快照。
 * 缓存 capability 每次验证实体仍加载且占据原位置，拆除/卸载后不能修改库存。
 */
public final class NuclearHeatExchangerBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private final HeatExchangerState ledger = new HeatExchangerState();
    private boolean available, removalHandled;
    private int capabilityEpoch;
    private boolean tickPaused;
    private BlockPos lastController;
    private int sizeLimit, waterLimit, viewHeat = -1, viewFlow;
    private double nominalFlow;
    private String viewStatus = "no_load";

    public NuclearHeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER_BE.get(), pos, state);
    }
    public HeatExchangerState ledger() { return ledger; }
    public boolean isRemovalHandled() { return removalHandled; }
    public void markRemovalHandled() { removalHandled = true; }
    public boolean current() {
        return available && level != null && !isRemoved() && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this;
    }

    /** 精确核对原版BE的距离票据、FULL状态、实体加载及世界边界门；不加载任何区块。 */
    public boolean canTick() {
        if (!(level instanceof ServerLevel server)) return level != null && level.isClientSide;
        var chunk = server.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        return chunk != null && server.shouldTickBlocksAt(worldPosition)
                && chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING)
                && server.areEntitiesLoaded(ChunkPos.asLong(worldPosition))
                && server.getWorldBorder().isWithinBounds(worldPosition);
    }

    /** 纯读取已付热；当上方负载消失时立即拒绝查询，不刷新账本或余热。 */
    public int publishedHeat() {
        return current() && canTick() && (level.isClientSide || HeatExchangerBoilerBridge.qualified(
                HeatExchangerBoilerBridge.controller(level, worldPosition))) ? viewHeat : -1;
    }

    /** 先记账、再同步外观、最后通知锅炉；回调能见到的热始终已支付。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, NuclearHeatExchangerBlockEntity machine) {
        if (level.isClientSide || !machine.current()) return;
        if (!machine.canTick()) { machine.pauseHeat(); return; }
        if (machine.tickPaused) {
            machine.tickPaused = false;
            machine.capabilityEpoch++;
            level.invalidateCapabilities(pos);
        }
        var controller = HeatExchangerBoilerBridge.controller(level, pos);
        BlockPos nextController = controller == null ? null : controller.getBlockPos();
        int previous = machine.viewHeat;
        var settings = HeatExchangerConfig.settings();
        machine.ledger.tick(level.getGameTime(), HeatExchangerBoilerBridge.qualified(controller), settings);
        machine.viewHeat = machine.ledger.heat();
        machine.viewFlow = machine.ledger.converted();
        machine.nominalFlow = settings.valid() ? settings.rate() / settings.density() : 0;
        machine.viewStatus = machine.ledger.status();
        machine.sizeLimit = controller == null ? 0 : controller.boiler.getMaxHeatLevelForBoilerSize(controller.getTotalTankSize());
        machine.waterLimit = controller == null ? 0 : controller.boiler.getMaxHeatLevelForWaterSupply();
        boolean hot = machine.viewHeat > 0;
        if (state.getValue(NuclearHeatExchangerBlock.LIT) != hot)
            level.setBlock(pos, state.setValue(NuclearHeatExchangerBlock.LIT, hot), 3);
        if (previous != machine.viewHeat || !java.util.Objects.equals(machine.lastController, nextController)) {
            HeatExchangerBoilerBridge.refresh(level, machine.lastController);
            HeatExchangerBoilerBridge.refresh(level, nextController);
        }
        machine.lastController = nextController;
        HeatExchangerBoilerBridge.track(machine, machine.viewHeat > 0);
        machine.setChanged();
        if (previous != machine.viewHeat || level.getGameTime() % 5 == 0) machine.syncView();
    }

    @Override public void onLoad() {
        super.onLoad();
        available = true;
        viewHeat = -1;
        var controller = HeatExchangerBoilerBridge.controller(level, worldPosition);
        lastController = controller == null ? null : controller.getBlockPos();
        HeatExchangerBoilerBridge.refresh(level, lastController);
    }

    /** 先让热回调与旧流体句柄不可用，再刷新跨区块控制器；本方法重复调用安全。 */
    public void suspend() {
        available = false;
        capabilityEpoch++;
        viewHeat = -1;
        HeatExchangerBoilerBridge.track(this, false);
        HeatExchangerBoilerBridge.refresh(level, lastController);
    }

    /**
     * 未卸载但停止 tick 的源仅撤销发布，不结算新热、不改 lastTick；恢复沿原时间戳散热。
     * 同时通知 NeoForge 缓存失效，防止 Create 管道恢复后永久持有旧 epoch 的空句柄。
     */
    void pauseHeat() {
        if (tickPaused) return;
        tickPaused = true;
        capabilityEpoch++;
        viewHeat = -1;
        if (level != null && !level.isClientSide) level.invalidateCapabilities(worldPosition);
        HeatExchangerBoilerBridge.refresh(level, lastController);
    }
    @Override public void onChunkUnloaded() { suspend(); super.onChunkUnloaded(); }
    @Override public void setRemoved() { suspend(); super.setRemoved(); }

    /** 顶面和无方向内部访问不提供流体口；其余五面共享热入冷出两罐。 */
    public IFluidHandler fluidPort(Direction side) {
        return side == null || side == Direction.UP ? null : new Port();
    }
    public CompoundTag savePortableData() { return ledger.save(); }
    public void loadPortableData(CompoundTag tag) {
        ledger.load(tag);
        viewHeat = -1;
        changed();
        HeatExchangerBoilerBridge.refresh(level, lastController);
    }
    private void changed() { setChanged(); syncView(); }
    private void syncView() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("HeatExchanger", ledger.save());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ledger.load(tag.getCompound("HeatExchanger"));
        viewHeat = -1;
        if (tag.contains("View")) readView(tag.getCompound("View"));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = super.getUpdateTag(registries);
        tag.put("HeatExchanger", ledger.save());
        var view = new CompoundTag();
        view.putInt("Heat", viewHeat);
        view.putInt("Flow", viewFlow);
        view.putDouble("Nominal", nominalFlow);
        view.putInt("Size", sizeLimit);
        view.putInt("Water", waterLimit);
        view.putString("Status", viewStatus);
        tag.put("View", view);
        return tag;
    }
    private void readView(CompoundTag tag) {
        viewHeat = tag.getInt("Heat"); viewFlow = tag.getInt("Flow"); nominalFlow = tag.getDouble("Nominal");
        sizeLimit = tag.getInt("Size"); waterLimit = tag.getInt("Water"); viewStatus = tag.getString("Status");
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        if (packet.getTag() != null) readView(packet.getTag().getCompound("View"));
    }

    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        String prefix = "gui.create_nuclear_industry.heat_exchanger.";
        tooltip.add(Component.translatable("block.create_nuclear_industry.nuclear_heat_exchanger"));
        tooltip.add(Component.translatable(prefix + "tanks", ledger.hot(), ledger.cold(), HeatExchangerState.CAPACITY));
        tooltip.add(Component.translatable(prefix + "state." + viewStatus));
        tooltip.add(Component.translatable(prefix + "flow", viewFlow, String.format(java.util.Locale.ROOT, "%.2f", nominalFlow)));
        tooltip.add(Component.translatable(prefix + "heat", Math.max(0, viewHeat), ledger.remainingTicks()));
        tooltip.add(Component.translatable(prefix + "limits", sizeLimit, waterLimit));
        tooltip.add(Component.translatable(prefix + "rated"));
        return true;
    }

    private final class Port implements IFluidHandler {
        // 同一实体经历卸载/恢复时，先前缓存的句柄也永久失效，不能随 onLoad 重新复活。
        private final int epoch = capabilityEpoch;
        private boolean valid() { return epoch == capabilityEpoch && current() && canTick() && !level.isClientSide; }
        @Override public int getTanks() { return valid() ? 2 : 0; }
        @Override public FluidStack getFluidInTank(int tank) {
            if (!valid() || tank < 0 || tank > 1) return FluidStack.EMPTY;
            int amount = tank == 0 ? ledger.hot() : ledger.cold();
            return amount == 0 ? FluidStack.EMPTY : new FluidStack(tank == 0
                    ? ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get() : ModFluids.COMPOUND_COOLANT_SOURCE.get(), amount);
        }
        @Override public int getTankCapacity(int tank) { return valid() && tank >= 0 && tank < 2 ? HeatExchangerState.CAPACITY : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return valid() && tank == 0 && ModFluids.isHotCompoundCoolant(stack); }
        @Override public int fill(FluidStack stack, FluidAction action) {
            if (!isFluidValid(0, stack)) return 0;
            int accepted = ledger.fillHot(stack.getAmount(), action.simulate());
            if (accepted > 0 && action.execute()) changed();
            return accepted;
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            return ModFluids.isCompoundCoolant(stack) ? drain(stack.getAmount(), action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            if (!valid()) return FluidStack.EMPTY;
            int taken = ledger.drainCold(amount, action.simulate());
            if (taken > 0 && action.execute()) changed();
            return taken == 0 ? FluidStack.EMPTY : new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), taken);
        }
    }
}
