package com.iksxh.create_nuclear_industry.heat;

import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerStructure;
import com.iksxh.create_nuclear_industry.compat.create.SharedFluidReceiver;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
    private Object inventoryIdentity = new Object();
    private boolean tickPaused;
    private boolean topologyReady;
    private BlockPos lastController;
    private int sizeLimit, waterLimit, viewHeat = -1, viewFlow;
    private double nominalFlow;
    private int viewLineCount = 1, viewLineHot, viewLineCold;
    private String viewStatus = "no_load";

    public NuclearHeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER_BE.get(), pos, state);
    }
    public HeatExchangerState ledger() { return ledger; }
    public boolean isRemovalHandled() { return removalHandled; }
    public void markRemovalHandled() { removalHandled = true; }
    Object inventoryIdentity() { return inventoryIdentity; }
    /** 直列交易先标记持久化，视图由设备 tick 合并同步，避免每台转换重复广播。 */
    void markInventoryChanged() { setChanged(); }

    /** 放拆、转向及成员恢复时撤销旧拓扑句柄；物理库存和绝对余热期限不变。 */
    void invalidateTopology() {
        capabilityEpoch++;
        inventoryIdentity = new Object();
        viewHeat = -1;
        viewStatus = "line_unavailable";
        if (level != null && !level.isClientSide) level.invalidateCapabilities(worldPosition);
        HeatExchangerBoilerBridge.track(this, false);
        HeatExchangerBoilerBridge.refresh(level, lastController);
    }

    /** 只访问已加载位置；轴向和可能被旋转接入的邻近直列都刷新能力缓存。 */
    static void topologyChanged(Level level, BlockPos changed) {
        if (level == null || level.isClientSide) return;
        var affected = new java.util.HashSet<NuclearHeatExchangerBlockEntity>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int distance = 0; distance <= HeatExchangerLine.MAX_LENGTH + 1; distance++) {
                BlockPos cursor = changed.relative(direction, distance);
                // 卸载通知时本机区块可能已从查询表移除；继续扫描可触及另一侧仍加载的端点。
                if (!level.hasChunkAt(cursor)) continue;
                if (level.getBlockEntity(cursor) instanceof NuclearHeatExchangerBlockEntity machine)
                    affected.add(machine);
            }
        }
        affected.forEach(NuclearHeatExchangerBlockEntity::invalidateTopology);
        for (var machine : affected) {
            HeatExchangerLine line = HeatExchangerLine.find(machine);
            if (line != null) machine.updateLineView(line);
            else machine.updateLineView(1, machine.ledger.hot(), machine.ledger.cold());
            machine.syncView();
        }
        // 管道可能先于尚未 onLoad 的成员放置；能力失效后必须请 Create 重算邻管连接。
        var pipes = new java.util.HashMap<BlockPos, Direction>();
        for (var machine : affected) for (Direction direction : Direction.Plane.HORIZONTAL)
            pipes.put(machine.worldPosition.relative(direction), direction.getOpposite());
        for (var entry : pipes.entrySet()) {
            BlockPos pipe = entry.getKey();
            if (!level.hasChunkAt(pipe)) continue;
            var pipeState = level.getBlockState(pipe);
            if (pipeState.getBlock() instanceof FluidPipeBlock fluidPipe) {
                // Create 的连接开关存在方块状态中，单纯传播 Flow 不会重开曾因空能力关闭的面。
                var refreshed = fluidPipe.updateBlockState(pipeState, entry.getValue(), null, level, pipe);
                if (refreshed != pipeState) level.setBlock(pipe, refreshed, 3);
                pipeState = refreshed;
            }
            if (BlockEntityBehaviour.get(level, pipe, FluidTransportBehaviour.TYPE) != null)
                FluidPropagator.propagateChangedPipe(level, pipe, pipeState);
        }
    }
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
        return current() && canTick() && (level.isClientSide || HeatExchangerLine.find(this) != null)
                && (level.isClientSide || HeatExchangerBoilerBridge.qualified(
                HeatExchangerBoilerBridge.controller(level, worldPosition))) ? viewHeat : -1;
    }

    /** 锅炉热段对正且源仍可 tick 才领取实付 HU；其他查询与同 tick 重复申请返回零。 */
    public double claimBoilerHeat(BoilerControllerBlockEntity owner, double requestHu) {
        if (!current() || !canTick() || level.isClientSide || requestHu <= 0
                || BoilerStructure.ownerOfSection(level, worldPosition.above()) != owner) return 0;
        HeatExchangerLine line = HeatExchangerLine.find(this);
        if (line == null) { pauseHeat(); return 0; }
        double paid = ledger.claimDedicated(level.getGameTime(), requestHu, HeatExchangerConfig.settings(), line);
        viewHeat = -1;
        viewFlow = ledger.converted();
        viewStatus = ledger.status();
        updateLineView(line);
        if (paid > 0 || viewFlow > 0) {
            setChanged();
            syncView();
        }
        return paid;
    }

    /** 先记账、再同步外观、最后通知锅炉；回调能见到的热始终已支付。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, NuclearHeatExchangerBlockEntity machine) {
        if (level.isClientSide || !machine.current()) return;
        if (!machine.canTick()) { machine.pauseHeat(); return; }
        HeatExchangerLine line = HeatExchangerLine.find(machine);
        if (line == null) { machine.pauseHeat(); return; }
        if (!machine.topologyReady) {
            // 首次全列可 tick 可能晚于 onLoad；此时重算 Create 管状态才看得到有效能力。
            machine.topologyReady = true;
            topologyChanged(level, pos);
        }
        if (machine.tickPaused) {
            machine.tickPaused = false;
            machine.capabilityEpoch++;
            level.invalidateCapabilities(pos);
        }
        // 专用热段由控制器按需求结算；源先 tick 时不能抢先走原生锅炉账本。
        if (BoilerStructure.ownerOfSection(level, pos.above()) != null) {
            machine.viewHeat = -1;
            machine.updateLineView(line);
            if (machine.viewStatus.equals("line_unavailable")) machine.viewStatus = "no_load";
            if (level.getGameTime() % 5 == 0) machine.syncView();
            HeatExchangerBoilerBridge.track(machine, false);
            return;
        }
        var controller = HeatExchangerBoilerBridge.controller(level, pos);
        BlockPos nextController = controller == null ? null : controller.getBlockPos();
        int previous = machine.viewHeat;
        var settings = HeatExchangerConfig.settings();
        machine.ledger.tick(level.getGameTime(), HeatExchangerBoilerBridge.qualified(controller), settings, line);
        machine.viewHeat = machine.ledger.heat();
        machine.viewFlow = machine.ledger.converted();
        machine.nominalFlow = settings.valid() ? settings.rate() / settings.density() : 0;
        machine.viewStatus = machine.ledger.status();
        machine.updateLineView(line);
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
        topologyReady = false;
        viewHeat = -1;
        HeatExchangerBoilerBridge.registerLoaded(this);
        topologyChanged(level, worldPosition);
        var controller = HeatExchangerBoilerBridge.controller(level, worldPosition);
        lastController = controller == null ? null : controller.getBlockPos();
        HeatExchangerBoilerBridge.refresh(level, lastController);
    }

    /** 先让热回调与旧流体句柄不可用，再刷新跨区块控制器；本方法重复调用安全。 */
    public void suspend() {
        available = false;
        topologyReady = false;
        capabilityEpoch++;
        inventoryIdentity = new Object();
        viewHeat = -1;
        HeatExchangerBoilerBridge.unregisterLoaded(this);
        topologyChanged(level, worldPosition);
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
        topologyReady = false;
        capabilityEpoch++;
        inventoryIdentity = new Object();
        viewHeat = -1;
        viewStatus = "line_unavailable";
        topologyChanged(level, worldPosition);
        if (level != null && !level.isClientSide) level.invalidateCapabilities(worldPosition);
        HeatExchangerBoilerBridge.refresh(level, lastController);
    }
    @Override public void onChunkUnloaded() { suspend(); super.onChunkUnloaded(); }
    @Override public void setRemoved() { suspend(); super.setRemoved(); }

    /** 仅背面列尾输入热液、正面列首输出冷液；内部相接面没有外部流体口。 */
    public IFluidHandler fluidPort(Direction side) {
        if (side == null || !side.getAxis().isHorizontal()) return null;
        HeatExchangerLine line = HeatExchangerLine.find(this);
        return line != null && (line.inlet(this, side) || line.outlet(this, side)) ? new Port(side, line) : null;
    }
    private void updateLineView(HeatExchangerLine line) {
        updateLineView(line.count(), line.totalHot(), line.totalCold());
    }
    void updateLineView(int count, int hot, int cold) {
        viewLineCount = count;
        viewLineHot = hot;
        viewLineCold = cold;
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
        view.putInt("LineCount", viewLineCount);
        view.putInt("LineHot", viewLineHot);
        view.putInt("LineCold", viewLineCold);
        tag.put("View", view);
        return tag;
    }
    private void readView(CompoundTag tag) {
        viewHeat = tag.getInt("Heat"); viewFlow = tag.getInt("Flow"); nominalFlow = tag.getDouble("Nominal");
        sizeLimit = tag.getInt("Size"); waterLimit = tag.getInt("Water"); viewStatus = tag.getString("Status");
        viewLineCount = Math.max(1, tag.getInt("LineCount"));
        viewLineHot = tag.getInt("LineHot"); viewLineCold = tag.getInt("LineCold");
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        if (packet.getTag() != null) readView(packet.getTag().getCompound("View"));
    }

    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        String prefix = "gui.create_nuclear_industry.heat_exchanger.";
        tooltip.add(Component.translatable("block.create_nuclear_industry.nuclear_heat_exchanger"));
        if (viewStatus.equals("line_unavailable"))
            tooltip.add(Component.translatable(prefix + "local_tanks", ledger.hot(), ledger.cold(),
                    HeatExchangerState.CAPACITY));
        else tooltip.add(Component.translatable(prefix + "line_tanks", viewLineCount, viewLineHot, viewLineCold,
                viewLineCount * HeatExchangerState.CAPACITY));
        tooltip.add(Component.translatable(prefix + "state." + viewStatus));
        tooltip.add(Component.translatable(prefix + "flow", viewFlow, String.format(java.util.Locale.ROOT, "%.2f", nominalFlow)));
        tooltip.add(Component.translatable(prefix + "heat", Math.max(0, viewHeat), ledger.remainingTicks()));
        tooltip.add(Component.translatable(prefix + "limits", sizeLimit, waterLimit));
        tooltip.add(Component.translatable(prefix + "rated"));
        return true;
    }

    private final class Port implements IFluidHandler, SharedFluidReceiver {
        // 同一实体经历卸载/恢复时，先前缓存的句柄也永久失效，不能随 onLoad 重新复活。
        private final int epoch = capabilityEpoch;
        private final Direction side;
        private final HeatExchangerLine initialLine;
        private Port(Direction side, HeatExchangerLine line) { this.side = side; this.initialLine = line; }
        private HeatExchangerLine line() {
            if (epoch != capabilityEpoch || !current() || !canTick() || level.isClientSide) return null;
            HeatExchangerLine now = HeatExchangerLine.find(NuclearHeatExchangerBlockEntity.this);
            return initialLine.sameMembers(now) && (now.inlet(NuclearHeatExchangerBlockEntity.this, side)
                    || now.outlet(NuclearHeatExchangerBlockEntity.this, side)) ? now : null;
        }
        /** 多输入源共享整列热罐的稳定身份及实时总空位。 */
        @Override public SharedFluidReceiver.Limits sharedFluidLimits() {
            HeatExchangerLine now = line();
            if (now == null || !now.inlet(NuclearHeatExchangerBlockEntity.this, side)) return null;
            int space = now.capacity() - now.totalHot();
            return new SharedFluidReceiver.Limits(now.identity(), space, now.identity(), space);
        }
        @Override public int getTanks() { return line() == null ? 0 : 1; }
        @Override public FluidStack getFluidInTank(int tank) {
            HeatExchangerLine now = line();
            if (now == null || tank != 0) return FluidStack.EMPTY;
            boolean input = now.inlet(NuclearHeatExchangerBlockEntity.this, side);
            int amount = input ? now.totalHot() : now.totalCold();
            return amount == 0 ? FluidStack.EMPTY : new FluidStack(input
                    ? ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get() : ModFluids.COMPOUND_COOLANT_SOURCE.get(), amount);
        }
        @Override public int getTankCapacity(int tank) {
            HeatExchangerLine now = line();
            return now != null && tank == 0 ? now.capacity() : 0;
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            HeatExchangerLine now = line();
            return now != null && tank == 0 && now.inlet(NuclearHeatExchangerBlockEntity.this, side)
                    && ModFluids.isHotCompoundCoolant(stack);
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            if (!isFluidValid(0, stack)) return 0;
            return line().fillHot(stack.getAmount(), action.simulate());
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            return ModFluids.isCompoundCoolant(stack) ? drain(stack.getAmount(), action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            HeatExchangerLine now = line();
            if (now == null || !now.outlet(NuclearHeatExchangerBlockEntity.this, side)) return FluidStack.EMPTY;
            int taken = now.drainCold(amount, action.simulate());
            return taken == 0 ? FluidStack.EMPTY : new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), taken);
        }
    }
}
