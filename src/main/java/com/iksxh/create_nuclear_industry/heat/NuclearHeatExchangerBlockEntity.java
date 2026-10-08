package com.iksxh.create_nuclear_industry.heat;

import com.iksxh.create_nuclear_industry.goggle.GoggleTooltip;
import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerStructure;
import com.iksxh.create_nuclear_industry.compat.create.SharedFluidReceiver;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Fluid;
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
    private Object lastBasinOperator;
    private BlockPos lastController;
    private BlockPos boilerOwner;
    private int sizeLimit, waterLimit, viewHeat = -1, viewFlow;
    private double nominalFlow;
    private int viewLineCount = 1, viewLineHot, viewLineCold;
    private double viewBasinDemand;
    private int viewHotCapacity = HeatExchangerState.CAPACITY, viewColdCapacity = HeatExchangerState.CAPACITY;
    private String viewStatus = "no_load";
    private HeatExchangerMode viewMode = HeatExchangerMode.NUCLEAR;
    private static final TagKey<Block> COLD_SOURCES = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", "condensation_cold_sources"));

    public NuclearHeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER_BE.get(), pos, state);
    }
    public HeatExchangerState ledger() { return ledger; }
    /** 追踪当前Create操作器实例，仅在首次接入时额外唤醒其原生检查。 */
    boolean observeBasinOperator(Object operator) {
        if (lastBasinOperator == operator) return false;
        lastBasinOperator = operator;
        return operator != null;
    }
    /** 炉内归属改变先撤销独立能力及 Create 热贡献，库存由接管事务另行转移。 */
    public void setBoilerOwner(BoilerControllerBlockEntity owner) {
        BlockPos next = owner == null ? null : owner.getBlockPos();
        if (java.util.Objects.equals(next, boilerOwner)) return;
        boilerOwner = next; invalidateTopology();
    }
    /** 全组预检后无外部回调地先入控制器再清空本机，重复接管空库存不会增量。 */
    public void joinBoiler(BoilerControllerBlockEntity owner) {
        setBoilerOwner(owner); owner.ledger().importMember(ledger.hot(), ledger.cold(), ledger.reserve());
        ledger.relinquishToBoiler(); viewHeat = -1; viewFlow = 0; viewStatus = "in_boiler"; viewMode = HeatExchangerMode.NUCLEAR;
        setChanged(); if (level != null && level.getGameTime() % 5 == 0) syncView();
    }
    /** 成型查询不付款；独立线扫描之前就排除炉内成员，杜绝双重结算。 */
    public boolean inBoiler() { return level != null && !level.isClientSide && BoilerStructure.owner(level, worldPosition) != null; }
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
            for (int distance = 0; distance <= HeatExchangerConfig.settings().maxLineLength() + 1; distance++) {
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
        if (inBoiler()) return -1;
        HeatExchangerLine line = level != null && !level.isClientSide ? HeatExchangerLine.find(this) : null;
        return viewMode == HeatExchangerMode.NUCLEAR && current() && canTick()
                && (level.isClientSide || line != null && !line.conflict()
                && line.displayMode() == HeatExchangerMode.NUCLEAR)
                && (level.isClientSide || HeatExchangerBoilerBridge.qualified(
                HeatExchangerBoilerBridge.controller(level, worldPosition))) ? viewHeat : -1;
    }

    /** 内置锅炉改由控制器收热；旧调用入口不再发放第二份 HU。 */
    public double claimBoilerHeat(BoilerControllerBlockEntity owner, double requestHu) {
        return 0;
    }

    /** 先记账、再同步外观、最后通知锅炉；回调能见到的热始终已支付。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, NuclearHeatExchangerBlockEntity machine) {
        if (level.isClientSide || !machine.current()) return;
        machine.ledger.setSettings(HeatExchangerConfig.settings());
        machine.ledger.setCondensationSettings(HeatExchangerConfig.condensationSettings());
        if (!machine.canTick()) { machine.pauseHeat(); return; }
        if (machine.inBoiler()) {
            machine.viewHeat = -1; machine.viewStatus = "in_boiler"; HeatExchangerBoilerBridge.track(machine, false);
            if (level.getGameTime() % 5 == 0) machine.syncView(); return;
        }
        machine.setBoilerOwner(null);
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
        if (line.conflict()) {
            if (level.hasChunkAt(pos.above()) && level.getBlockEntity(pos.above()) instanceof
                    com.simibubi.create.content.processing.basin.BasinBlockEntity) {
                machine.ledger.tickBasin(level.getGameTime(), false, HeatExchangerConfig.settings(), line);
                boolean statusChanged = machine.updateBasinView(null, "basin_mode_conflict");
                HeatExchangerBasinBridge.scheduleMixerUpdate(machine, statusChanged);
            } else {
                machine.ledger.pauseMode(level.getGameTime(), "mode_conflict");
                machine.finishCondensationView(line, "mode_conflict");
            }
            return;
        }
        if (line.displayMode() == HeatExchangerMode.CONDENSATION) {
            line.adoptMode(HeatExchangerMode.CONDENSATION);
            var cfg = HeatExchangerConfig.condensationSettings();
            machine.ledger.tickCondensation(level.getGameTime(), machine.topColdSource(), cfg, line);
            // 仅成功转换达到当前阶段边界才改顶格；下一tick重新检查新水/空气阶段。
            if (machine.ledger.converted() > 0 && machine.ledger.condensation().exhausted(cfg)) {
                boolean evaporate = machine.ledger.condensation().source() == CondensationState.Source.WATER;
                level.setBlock(pos.above(), (evaporate ? Blocks.AIR : Blocks.WATER).defaultBlockState(), 3);
                machine.ledger.condensation().changedTo(evaporate ? CondensationState.Source.NONE : CondensationState.Source.WATER);
            }
            machine.finishCondensationView(line, machine.ledger.status());
            return;
        }
        line.adoptMode(HeatExchangerMode.NUCLEAR);
        machine.viewMode = HeatExchangerMode.NUCLEAR;
        if (level.hasChunkAt(pos.above()) && level.getBlockEntity(pos.above()) instanceof
                com.simibubi.create.content.processing.basin.BasinBlockEntity) {
            if (state.getValue(NuclearHeatExchangerBlock.LIT))
                level.setBlock(pos, state.setValue(NuclearHeatExchangerBlock.LIT, false), 3);
            HeatExchangerBoilerBridge.track(machine, false);
            HeatExchangerBasinBridge.tick(machine, HeatExchangerConfig.settings(), line);
            return;
        }
        machine.observeBasinOperator(null);
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

    /** 顶格只读检查；不加载区块，流水、雪层、含水方块和未定义标签扩展均无效。 */
    private CondensationState.Source topColdSource() {
        BlockPos above = worldPosition.above();
        if (level == null || !level.hasChunkAt(above)) return CondensationState.Source.NONE;
        BlockState state = level.getBlockState(above);
        if (!state.is(COLD_SOURCES)) return CondensationState.Source.NONE;
        if (state.is(Blocks.WATER) && state.getFluidState().isSource()) return CondensationState.Source.WATER;
        if (state.is(Blocks.SNOW_BLOCK)) return CondensationState.Source.SNOW;
        if (state.is(Blocks.ICE)) return CondensationState.Source.ICE;
        if (state.is(Blocks.PACKED_ICE)) return CondensationState.Source.PACKED_ICE;
        if (state.is(Blocks.BLUE_ICE)) return CondensationState.Source.BLUE_ICE;
        return CondensationState.Source.NONE;
    }

    /** 冷凝或冲突立即撤销两种锅炉热视图，独立同步模式、实际mB/t与暂停原因。 */
    private void finishCondensationView(HeatExchangerLine line, String status) {
        int previous = viewHeat;
        viewHeat = -1;
        viewMode = line.displayMode();
        viewFlow = ledger.converted();
        nominalFlow = HeatExchangerConfig.condensationSettings().rateMbPerTick();
        viewStatus = status;
        updateLineView(line);
        if (getBlockState().getValue(NuclearHeatExchangerBlock.LIT))
            level.setBlock(worldPosition, getBlockState().setValue(NuclearHeatExchangerBlock.LIT, false), 3);
        HeatExchangerBoilerBridge.track(this, false);
        if (previous > 0) HeatExchangerBoilerBridge.refresh(level, lastController);
        setChanged();
        if (previous > 0 || level.getGameTime() % 5 == 0) syncView();
    }

    @Override public void onLoad() {
        super.onLoad();
        available = true;
        BoilerStructure.invalidateNearby(level, worldPosition);
        if (level != null && !level.isClientSide) ledger.setSettings(HeatExchangerConfig.settings());
        if (level != null && !level.isClientSide) ledger.setCondensationSettings(HeatExchangerConfig.condensationSettings());
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
        boolean basinWasActive = viewStatus.startsWith("basin_");
        BoilerStructure.invalidateNearby(level, worldPosition);
        available = false;
        topologyReady = false;
        capabilityEpoch++;
        inventoryIdentity = new Object();
        viewHeat = -1;
        viewStatus = "line_unavailable";
        HeatExchangerBoilerBridge.unregisterLoaded(this);
        topologyChanged(level, worldPosition);
        HeatExchangerBoilerBridge.track(this, false);
        HeatExchangerBoilerBridge.refresh(level, lastController);
        if (basinWasActive) HeatExchangerBasinBridge.scheduleMixerUpdate(this, true);
    }

    /**
     * 未卸载但停止 tick 的源仅撤销发布，不结算新热、不改 lastTick；恢复沿原时间戳散热。
     * 同时通知 NeoForge 缓存失效，防止 Create 管道恢复后永久持有旧 epoch 的空句柄。
     */
    void pauseHeat() {
        if (tickPaused) return;
        boolean basinWasActive = viewStatus.startsWith("basin_");
        tickPaused = true;
        topologyReady = false;
        capabilityEpoch++;
        inventoryIdentity = new Object();
        viewHeat = -1;
        viewStatus = "line_unavailable";
        topologyChanged(level, worldPosition);
        if (level != null && !level.isClientSide) level.invalidateCapabilities(worldPosition);
        HeatExchangerBoilerBridge.refresh(level, lastController);
        if (basinWasActive) HeatExchangerBasinBridge.scheduleMixerUpdate(this, true);
    }
    @Override public void onChunkUnloaded() { suspend(); super.onChunkUnloaded(); }
    @Override public void setRemoved() { suspend(); super.setRemoved(); }

    /** 仅背面列尾输入热液、正面列首输出冷液；内部相接面没有外部流体口。 */
    public IFluidHandler fluidPort(Direction side) {
        if (side == null || !side.getAxis().isHorizontal()) return null;
        if (level != null && !level.isClientSide) ledger.setSettings(HeatExchangerConfig.settings());
        HeatExchangerLine line = HeatExchangerLine.find(this);
        return line != null && (line.inlet(this, side) || line.outlet(this, side)) ? new Port(side, line) : null;
    }
    private void updateLineView(HeatExchangerLine line) {
        updateLineView(line.count(), line.totalHot(), line.totalCold());
        viewMode = line.displayMode();
        viewHotCapacity = line.capacity(viewMode, true) / line.count();
        viewColdCapacity = line.capacity(viewMode, false) / line.count();
    }
    void updateLineView(int count, int hot, int cold) {
        viewLineCount = count;
        viewLineHot = hot;
        viewLineCold = cold;
    }
    public CompoundTag savePortableData() { return ledger.save(); }
    public void loadPortableData(CompoundTag tag) {
        ledger.load(tag);
        if (level != null && !level.isClientSide) ledger.setSettings(HeatExchangerConfig.settings());
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
        view.putInt("HotCapacity", viewHotCapacity);
        view.putInt("ColdCapacity", viewColdCapacity);
        view.putDouble("BasinDemand", viewBasinDemand);
        view.putString("Mode", viewMode.name());
        tag.put("View", view);
        return tag;
    }
    private void readView(CompoundTag tag) {
        viewMode = "CONDENSATION".equals(tag.getString("Mode")) ? HeatExchangerMode.CONDENSATION : HeatExchangerMode.NUCLEAR;
        viewHeat = tag.getInt("Heat"); viewFlow = tag.getInt("Flow"); nominalFlow = tag.getDouble("Nominal");
        sizeLimit = tag.getInt("Size"); waterLimit = tag.getInt("Water"); viewStatus = tag.getString("Status");
        viewLineCount = Math.max(1, tag.getInt("LineCount"));
        viewLineHot = tag.getInt("LineHot"); viewLineCold = tag.getInt("LineCold");
        viewHotCapacity = tag.contains("HotCapacity") ? tag.getInt("HotCapacity") : HeatExchangerState.CAPACITY;
        viewColdCapacity = tag.contains("ColdCapacity") ? tag.getInt("ColdCapacity") : HeatExchangerState.CAPACITY;
        viewBasinDemand = tag.getDouble("BasinDemand");
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        if (packet.getTag() != null) readView(packet.getTag().getCompound("View"));
    }

    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        String prefix = "gui.create_nuclear_industry.heat_exchanger.";
        tooltip.add(GoggleTooltip.indentFirstLine(Component.translatable("block.create_nuclear_industry.nuclear_heat_exchanger")));
        // 炉内机不再拥有独立罐或 Create 热级，避免继续展示接管前的直列缓存。
        if (viewStatus.equals("in_boiler")) {
            tooltip.add(Component.translatable(prefix + "state.in_boiler"));
            return true;
        }
        tooltip.add(Component.translatable(prefix + "mode." + viewMode.name().toLowerCase(java.util.Locale.ROOT)));
        if (viewMode == HeatExchangerMode.CONDENSATION) {
            tooltip.add(Component.translatable(prefix + "condensation_tanks", viewLineCount, viewLineHot, viewLineCold,
                    viewLineCount * viewHotCapacity, viewLineCount * viewColdCapacity));
            tooltip.add(Component.translatable(prefix + "state." + viewStatus));
            tooltip.add(Component.translatable(prefix + "condensation_flow", viewFlow, String.format(java.util.Locale.ROOT, "%.2f", nominalFlow)));
            return true;
        }
        if (viewStatus.equals("line_unavailable"))
            tooltip.add(Component.translatable(prefix + "local_tanks", ledger.hot(), ledger.cold(),
                    viewHotCapacity, viewColdCapacity));
        else tooltip.add(Component.translatable(prefix + "line_tanks", viewLineCount, viewLineHot, viewLineCold,
                viewLineCount * viewHotCapacity, viewLineCount * viewColdCapacity));
        tooltip.add(Component.translatable(prefix + "state." + viewStatus));
        if (viewStatus.startsWith("basin_")) {
            tooltip.add(Component.translatable(prefix + "basin_rate", String.format(java.util.Locale.ROOT, "%.2f", viewBasinDemand), viewFlow));
            return true;
        }
        tooltip.add(Component.translatable(prefix + "flow", viewFlow, String.format(java.util.Locale.ROOT, "%.2f", nominalFlow)));
        tooltip.add(Component.translatable(prefix + "heat", Math.max(0, viewHeat), ledger.remainingTicks()));
        tooltip.add(Component.translatable(prefix + "limits", sizeLimit, waterLimit));
        tooltip.add(Component.translatable(prefix + "rated"));
        return true;
    }

    /** 工作盆显示固定HU/t成本与本tick实际等量换液，避免展示不存在的预热储备。 */
    boolean updateBasinView(Double costHuPerTick) {
        return updateBasinView(costHuPerTick, ledger.status());
    }

    boolean updateBasinView(Double costHuPerTick, String status) {
        int previous = viewHeat;
        String previousStatus = viewStatus;
        viewHeat = -1;
        viewFlow = ledger.converted();
        nominalFlow = HeatExchangerConfig.settings().rate() / HeatExchangerConfig.settings().density();
        viewBasinDemand = costHuPerTick == null ? 0 : costHuPerTick;
        viewStatus = status;
        updateLineView(HeatExchangerLine.find(this));
        setChanged();
        if (previous != viewHeat || level.getGameTime() % 5 == 0) syncView();
        return previous != viewHeat || !java.util.Objects.equals(previousStatus, status);
    }

    private final class Port implements IFluidHandler, SharedFluidReceiver {
        // 同一实体经历卸载/恢复时，先前缓存的句柄也永久失效，不能随 onLoad 重新复活。
        private final int epoch = capabilityEpoch;
        private final Direction side;
        private final HeatExchangerLine initialLine;
        // Create在同一次调用中先模拟fill再读取共享限制；只保存句柄内的查询工质提示，
        // 不选择机器模式、不预约库存，也不持久化或改变冷源/尾量。
        private HeatExchangerMode queriedInputMode = HeatExchangerMode.EMPTY;
        private Port(Direction side, HeatExchangerLine line) { this.side = side; this.initialLine = line; }
        private HeatExchangerLine line() {
            if (epoch != capabilityEpoch || !current() || !canTick() || level.isClientSide) return null;
            HeatExchangerLine now = HeatExchangerLine.find(NuclearHeatExchangerBlockEntity.this);
            return initialLine.sameMembers(now) && !now.conflict() && (now.inlet(NuclearHeatExchangerBlockEntity.this, side)
                    || now.outlet(NuclearHeatExchangerBlockEntity.this, side)) ? now : null;
        }
        /** 多输入源共享整列热罐的稳定身份及实时总空位。 */
        @Override public SharedFluidReceiver.Limits sharedFluidLimits() {
            HeatExchangerLine now = line();
            if (now == null || !now.inlet(NuclearHeatExchangerBlockEntity.this, side)) return null;
            var candidate = queriedInputMode == HeatExchangerMode.EMPTY ? now.displayMode() : queriedInputMode;
            int space = now.accepts(candidate) ? now.inputSpace(candidate) : 0;
            return new SharedFluidReceiver.Limits(now.identity(), space, now.identity(), space);
        }
        @Override public int getTanks() { return line() == null ? 0 : 1; }
        @Override public FluidStack getFluidInTank(int tank) {
            HeatExchangerLine now = line();
            if (now == null || tank != 0) return FluidStack.EMPTY;
            boolean input = now.inlet(NuclearHeatExchangerBlockEntity.this, side);
            int amount = input ? now.totalHot() : now.totalCold();
            return amount == 0 ? FluidStack.EMPTY : new FluidStack(input
                    ? inputFluid(now.displayMode()) : outputFluid(now.displayMode()), amount);
        }
        @Override public int getTankCapacity(int tank) {
            HeatExchangerLine now = line();
            if (now == null || tank != 0) return 0;
            if (!now.inlet(NuclearHeatExchangerBlockEntity.this, side)) return now.coldCapacity();
            if (now.mode() != HeatExchangerMode.EMPTY) return now.hotCapacity();
            // 尚未选模式时不能把一种工质的容量当成另一种；有查询提示按该工质报告，
            // 无提示仅报告两种可能容量的上界，最终fill模拟仍按具体工质真实预检。
            return queriedInputMode != HeatExchangerMode.EMPTY ? now.capacity(queriedInputMode, true)
                    : Math.max(now.capacity(HeatExchangerMode.NUCLEAR, true), now.capacity(HeatExchangerMode.CONDENSATION, true));
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            HeatExchangerLine now = line();
            return now != null && tank == 0 && now.inlet(NuclearHeatExchangerBlockEntity.this, side)
                    && inputMode(stack) != HeatExchangerMode.EMPTY && now.accepts(inputMode(stack));
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            if (!isFluidValid(0, stack)) return 0;
            queriedInputMode = inputMode(stack);
            return line().fillInput(stack.getAmount(), inputMode(stack), action.simulate());
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            HeatExchangerLine now = line();
            return now != null && stack.is(outputFluid(now.displayMode())) ? drain(stack.getAmount(), action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            HeatExchangerLine now = line();
            if (now == null || !now.outlet(NuclearHeatExchangerBlockEntity.this, side)) return FluidStack.EMPTY;
            int taken = now.drainCold(amount, action.simulate());
            return taken == 0 ? FluidStack.EMPTY : new FluidStack(outputFluid(now.displayMode()), taken);
        }
    }
    private static HeatExchangerMode inputMode(FluidStack stack) {
        return stack.is(TurbineContent.STEAM.get()) ? HeatExchangerMode.CONDENSATION
                : ModFluids.isHotCompoundCoolant(stack) ? HeatExchangerMode.NUCLEAR : HeatExchangerMode.EMPTY;
    }
    private static Fluid inputFluid(HeatExchangerMode mode) {
        return mode == HeatExchangerMode.CONDENSATION ? TurbineContent.STEAM.get() : ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get();
    }
    private static Fluid outputFluid(HeatExchangerMode mode) {
        return mode == HeatExchangerMode.CONDENSATION ? Fluids.WATER : ModFluids.COMPOUND_COOLANT_SOURCE.get();
    }
}
