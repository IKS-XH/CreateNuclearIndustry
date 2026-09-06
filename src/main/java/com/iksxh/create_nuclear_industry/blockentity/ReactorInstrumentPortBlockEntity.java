package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.control.ControlRodScramResult;
import com.iksxh.create_nuclear_industry.control.ControlRodScramService;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnFissionResult;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentTelemetry;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentGoggleDisplay;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentTelemetryNbtCodec;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshotNbtCodec;
import com.iksxh.create_nuclear_industry.reactor.ReactorControlRodTick;
import com.iksxh.create_nuclear_industry.reactor.ReactorCoolantLedger;
import com.iksxh.create_nuclear_industry.reactor.ReactorFissionCalculator;
import com.iksxh.create_nuclear_industry.reactor.ReactorServerTick;
import com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorInstrumentStructureSummary;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureScanner;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** 反应堆完整权威快照的唯一拥有者；历史契约短语为 {@code Sole authoritative owner}。 */
public final class ReactorInstrumentPortBlockEntity extends P1MinimalBlockEntity
        implements IHaveGoggleInformation {
    private static final String SNAPSHOT_KEY = "ReactorSnapshot";
    private static final String STRUCTURE_SUMMARY_KEY = "InstrumentStructureSummary";
    private static final String TELEMETRY_KEY = "InstrumentTelemetry";
    private static final String LEGACY_FUEL_MIGRATION_KEY = "LegacyFuelMigration";
    private static final String GOGGLE_KEY_PREFIX = "goggle.create_nuclear_industry.reactor.";
    private static final int TELEMETRY_SYNC_INTERVAL_TICKS = 5;
    private static final String TELEMETRY_UNAVAILABLE_REASON = "telemetry is unavailable";
    private static final double SAFETY_HEAT_EPSILON = 1.0E-12D;
    private static final Logger LOGGER = LogUtils.getLogger();

    private ReactorSnapshot snapshot = ReactorSnapshot.empty();
    private ReactorInstrumentTelemetry telemetry =
            ReactorInstrumentTelemetry.unavailable(TELEMETRY_UNAVAILABLE_REASON);
    private ReactorInstrumentTelemetry clientTelemetry =
            ReactorInstrumentTelemetry.unavailable(TELEMETRY_UNAVAILABLE_REASON);
    private ReactorInstrumentTelemetry lastSentTelemetry =
            ReactorInstrumentTelemetry.unavailable(TELEMETRY_UNAVAILABLE_REASON);
    private int telemetryTicksSinceLastSync = TELEMETRY_SYNC_INTERVAL_TICKS;
    private ReactorInstrumentStructureSummary clientStructureSummary =
            ReactorInstrumentStructureSummary.unavailable("structure summary is not synchronized");
    private ReactorStructureDefinition.ScanResult structureScan =
            ReactorStructureDefinition.ScanResult.notScanned();
    /** 等待对应换料端口加载后完成的一次性 v3 燃料迁移信封。 */
    private Map<CoreColumnPosition, FuelAssemblyState> pendingLegacyFuelAssemblies = Map.of();
    private BlockPos structureOrigin;
    private long structureScanCount;
    private final Set<ReactorPortBlockEntity> boundPorts =
            Collections.newSetFromMap(new IdentityHashMap<>());

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

    /**
     * 返回最近一次结构缓存对应的静态摘要；读取只发生在服务端已缓存数据上，不触发扫描。
     *
     * <p>容量从当前服务端配置读取，因此配置热更新后摘要会自然反映新的冷/热缓冲总容量；
     * 列和端口计数仍完全来自最近一次有效结构扫描。该摘要不属于反应堆运行快照。</p>
     */
    public ReactorInstrumentStructureSummary structureSummary() {
        return ReactorInstrumentStructureSummary.from(
                structureScan,
                P1ServerConfig.VALUES.coldInventoryCapacityMb.get().longValue(),
                P1ServerConfig.VALUES.hotInventoryCapacityMb.get().longValue()
        );
    }

    /** 返回服务端最近一次正式 tick 生成的动态遥测；未完成有效 tick 时返回不可用状态。 */
    public ReactorInstrumentTelemetry telemetry() {
        return telemetry;
    }

    /** 返回客户端最近一次更新包同步的动态遥测；该副本仅供显示层读取。 */
    public ReactorInstrumentTelemetry clientTelemetry() {
        return clientTelemetry;
    }

    /** 返回当前结构绑定的指定类型端口；结果只来自服务端缓存，不扫描世界。 */
    public List<ReactorPortBlockEntity> boundPorts(ReactorPortBlockEntity.BindingType type) {
        if (type == null) {
            return List.of();
        }
        return boundPorts.stream()
                .filter(port -> port.isBound() && port.boundOwner() == this
                        && port.binding().type() == type)
                .sorted(java.util.Comparator.comparingLong(port -> port.getBlockPos().asLong()))
                .toList();
    }

    /** 返回该仪表端口已建立的端口绑定数量，用于服务端诊断和回归测试。 */
    public int boundPortCount() {
        return boundPorts.size();
    }

    public void updateStructureCache(ReactorStructureScanner.WorldScanResult scan) {
        Objects.requireNonNull(scan, "structure scan is required");
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("structure cache can only be changed on the server");
        }
        clearControlRodTelemetry();
        structureScan = scan.contract();
        structureOrigin = scan.origin();
        structureScanCount++;
        if (structureScan.valid()) {
            initializeAndSyncControlRods();
            reconcilePortBindings();
            migratePendingFuelAssemblies();
            hydrateFuelProjectionsFromPorts();
            if (level != null && !level.isClientSide) {
                updateRedstoneScram(level.hasNeighborSignal(worldPosition));
            }
        } else {
            clearPortBindings(true);
        }
        invalidateTelemetry();
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
        if (!hydrateFuelProjectionsFromPorts()) {
            return false;
        }
        Map<CoreColumnPosition, ItemStack> beforePortItems = captureFuelPortItems();
        if (beforePortItems == null) {
            return false;
        }
        ReactorServerTick.Result result = ReactorServerTick.advance(
                snapshot,
                simulationParameters(),
                coolantInput()
        );
        Map<CoreColumnPosition, ItemStack> nextPortItems = prepareFuelPortCommit(
                beforePortItems, result.snapshot());
        if (nextPortItems == null) {
            return false;
        }
        boolean stateChanged = !result.snapshot().equals(snapshot);
        boolean portChanged = !sameFuelPortItems(beforePortItems, nextPortItems);
        for (Map.Entry<CoreColumnPosition, ItemStack> entry : nextPortItems.entrySet()) {
            ReactorPortBlockEntity port = findRefuelingPort(entry.getKey());
            if (port != null) {
                port.setFuelAssembly(entry.getValue());
            }
        }
        if (stateChanged) {
            setSnapshot(result.snapshot());
        }
        publishTelemetry(ReactorInstrumentTelemetry.from(result));
        return stateChanged || portChanged;
    }

    /**
     * 记录一次已经成功完成的正式 tick，并按固定上限向客户端发送更新。
     *
     * <p>遥测随 tick 变化时最多每五个游戏 tick 同步一次；没有变化则不发包。第一次有效
     * 遥测会立即发送，确保客户端不会一直停留在“等待运行数据”。</p>
     */
    private void publishTelemetry(ReactorInstrumentTelemetry nextTelemetry) {
        telemetry = Objects.requireNonNull(nextTelemetry, "reactor instrument telemetry is required");
        syncColumnTelemetryDisplays();
        telemetryTicksSinceLastSync = Math.min(
                TELEMETRY_SYNC_INTERVAL_TICKS, telemetryTicksSinceLastSync + 1);
        if (!telemetry.equals(lastSentTelemetry)
                && (!lastSentTelemetry.available()
                || telemetryTicksSinceLastSync >= TELEMETRY_SYNC_INTERVAL_TICKS)) {
            sendTelemetryUpdate();
        }
    }

    /** 结构扫描变化后立即使客户端遥测失效，禁止展示重扫前的陈旧 tick 数据。 */
    private void invalidateTelemetry() {
        telemetry = ReactorInstrumentTelemetry.unavailable(TELEMETRY_UNAVAILABLE_REASON);
        syncColumnTelemetryDisplays();
        sendTelemetryUpdate();
    }

    /** 同步当前遥测并重置节流计数；动态遥测不写入持久化 NBT。 */
    private void sendTelemetryUpdate() {
        lastSentTelemetry = telemetry;
        telemetryTicksSinceLastSync = 0;
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * 计算绑定燃料列当前的服务端裂变发热，单位为 HU/t。
     *
     * <p>换料端口调用此入口而不是接受客户端提供的“已停止”标记；计算只读取仪表端口
     * 的权威快照和服务器配置，不扫描世界，也不修改快照。</p>
     */
    public double currentFuelColumnFissionHeatHu(CoreColumnPosition position) {
        if (position == null || !structureScan.valid()) {
            return 0.0D;
        }
        FuelColumnFissionResult result = ReactorFissionCalculator.calculate(
                snapshot, simulationParameters()).columns().get(position);
        return result == null ? 0.0D : result.generatedHeatHu();
    }

    /**
     * 使用指定瞬态列投影计算当前列裂变发热，单位为 HU/t。
     *
     * <p>换料端口是燃料组件的唯一持久化所有者；端口刚装料、刚加载或尚未完成下一次
     * 正式 tick 时，仪表快照中的运行时投影可能尚未刷新。维修的“停止放热”校验必须
     * 把端口当前组件投影到本次只读计算中，不能因旧快照为空而错误放行。</p>
     */
    public double currentFuelColumnFissionHeatHu(
            CoreColumnPosition position,
            FuelColumnState projectedColumn
    ) {
        if (position == null || projectedColumn == null || !structureScan.valid()) {
            return 0.0D;
        }
        if (snapshot.controlRodColumns().containsKey(position)) {
            return 0.0D;
        }
        ReactorSnapshot projectedSnapshot = snapshot.withFuelColumn(position, projectedColumn);
        FuelColumnFissionResult result = ReactorFissionCalculator.calculate(
                projectedSnapshot, simulationParameters()).columns().get(position);
        return result == null ? 0.0D : result.generatedHeatHu();
    }

    /**
     * 只读判断当前权威状态是否允许未成型人工取料。
     *
     * <p>安全条件是服务端快照没有运行中的裂变发热，且融毁倒计时未建立；
     * {@code meltdownCountdownStarted} 同时覆盖正在倒计时和因 SCRAM/冷却暂停的状态。
     * 方法不扫描世界、不修改快照，缺少服务端环境、数值配置异常或计算异常时一律拒绝。</p>
     *
     * @return 当前服务端能够证明安全时为 {@code true}
     */
    public boolean isSafeForUnformedFuelExtraction() {
        if (level == null || level.isClientSide || snapshot == null
                || snapshot.meltdownCountdownStarted()) {
            return false;
        }
        try {
            return ReactorFissionCalculator.calculate(snapshot, simulationParameters())
                    .generatedHeatHu() <= SAFETY_HEAT_EPSILON;
        } catch (RuntimeException exception) {
            LOGGER.debug("unable to prove reactor safety for unformed fuel extraction", exception);
            return false;
        }
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
        for (ReactorPortBlockEntity port : boundPorts(ReactorPortBlockEntity.BindingType.COLD_COOLANT)) {
            ports.add(ReactorCoolantLedger.Port.cold(
                    portConnectionId(port), perPort));
        }
        for (ReactorPortBlockEntity port : boundPorts(ReactorPortBlockEntity.BindingType.HOT_COOLANT)) {
            ports.add(ReactorCoolantLedger.Port.hot(
                    portConnectionId(port), perPort));
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

    /**
     * 按最新结构扫描结果 reconcile 端口缓存；同值重扫保留原绑定，只有有效变化才通知 capability。
     *
     * <p>扫描结果可能先于部分跨区块端口方块实体到达；缺失端口不会伪造绑定，端口自身
     * 的 {@link ReactorPortBlockEntity#onLoad()} 会在实体真正加载后安排一次有界补偿重扫。</p>
     */
    private void reconcilePortBindings() {
        if (!structureScan.valid() || structureOrigin == null || level == null || level.isClientSide) {
            return;
        }
        Map<BlockPos, PortBindingSpec> expected = new LinkedHashMap<>();
        addExpectedPorts(expected, ReactorStructureDefinition.PortType.COLD_COOLANT,
                ReactorPortBlockEntity.BindingType.COLD_COOLANT, null);
        addExpectedPorts(expected, ReactorStructureDefinition.PortType.HOT_COOLANT,
                ReactorPortBlockEntity.BindingType.HOT_COOLANT, null);
        for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                : structureScan.columns().entrySet()) {
            if (entry.getValue().type() == ReactorStructureDefinition.ColumnType.FUEL) {
                expected.put(absolutePosition(entry.getValue().capPosition()),
                        new PortBindingSpec(ReactorPortBlockEntity.BindingType.REFUELING,
                                entry.getKey()));
            }
        }

        for (ReactorPortBlockEntity port : List.copyOf(boundPorts)) {
            PortBindingSpec spec = expected.get(port.getBlockPos());
            boolean currentEntity = !port.isRemoved()
                    && level.getBlockEntity(port.getBlockPos()) == port;
            if (!currentEntity || spec == null) {
                port.clearBinding(this, true);
                boundPorts.remove(port);
            }
        }

        for (Map.Entry<BlockPos, PortBindingSpec> entry : expected.entrySet()) {
            if (level.getBlockEntity(entry.getKey()) instanceof ReactorPortBlockEntity port
                    && port.bindTo(this, entry.getValue().type(), entry.getValue().column())) {
                boundPorts.add(port);
            }
        }
    }

    /**
     * 将旧快照中的燃料投影迁移到已加载的换料端口。
     *
     * <p>端口已有物品时端口优先，旧投影只作为诊断镜像丢弃；尚未加载端口的条目保留在
     * 迁移信封中，避免区块加载顺序造成燃料丢失。</p>
     */
    private void migratePendingFuelAssemblies() {
        if (pendingLegacyFuelAssemblies.isEmpty()) {
            return;
        }
        TreeMap<CoreColumnPosition, FuelAssemblyState> remaining =
                new TreeMap<>(pendingLegacyFuelAssemblies);
        for (Map.Entry<CoreColumnPosition, FuelAssemblyState> entry
                : pendingLegacyFuelAssemblies.entrySet()) {
            ReactorPortBlockEntity port = findRefuelingPort(entry.getKey());
            if (port == null) {
                continue;
            }
            ItemStack stored = port.fuelAssembly();
            if (stored.isEmpty()) {
                try {
                    port.setFuelAssembly(FuelAssemblyItemCodec.fromLegacyState(entry.getValue()));
                } catch (IllegalArgumentException exception) {
                    LOGGER.warn("无法迁移反应堆 {} 的旧燃料列 {}：{}",
                            worldPosition, entry.getKey(), exception.getMessage());
                }
            } else {
                LOGGER.warn("反应堆 {} 的换料端口 {} 已有燃料，保留端口物品并丢弃旧快照镜像",
                        worldPosition, entry.getKey());
            }
            remaining.remove(entry.getKey());
        }
        pendingLegacyFuelAssemblies = Map.copyOf(remaining);
    }

    /**
     * 从每个已绑定换料端口重建内存燃料投影，并补齐结构新增的空列。
     *
     * @return 所有燃料列端口均已加载且物品合法时返回 {@code true}
     */
    private boolean hydrateFuelProjectionsFromPorts() {
        if (!structureScan.valid() || structureOrigin == null || level == null || level.isClientSide) {
            return false;
        }
        TreeMap<CoreColumnPosition, FuelColumnState> nextFuelColumns = new TreeMap<>();
        for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                : structureScan.columns().entrySet()) {
            if (entry.getValue().type() != ReactorStructureDefinition.ColumnType.FUEL) {
                continue;
            }
            CoreColumnPosition position = entry.getKey();
            ReactorPortBlockEntity port = findRefuelingPort(position);
            if (port == null) {
                return false;
            }
            ItemStack stored = port.fuelAssembly();
            FuelColumnState current = snapshot.fuelColumns()
                    .getOrDefault(position, FuelColumnState.empty());
            if (stored.isEmpty() && current.fuelAssembly().present()) {
                try {
                    port.setFuelAssembly(FuelAssemblyItemCodec.fromLegacyState(current.fuelAssembly()));
                    stored = port.fuelAssembly();
                } catch (IllegalArgumentException exception) {
                    LOGGER.warn("无法将反应堆 {} 的旧燃料列 {} 写入换料端口：{}",
                            worldPosition, position, exception.getMessage());
                    return false;
                }
            }
            if (!FuelAssemblyItemCodec.isValidStoredFuel(stored)) {
                return false;
            }
            FuelColumnState projected = current.withFuelAssemblyProjection(
                    FuelAssemblyItemCodec.simulationState(stored));
            if (snapshot.fuelColumns().containsKey(position) || !stored.isEmpty()) {
                nextFuelColumns.put(position, projected);
            }
        }
        if (!nextFuelColumns.equals(snapshot.fuelColumns())) {
            setSnapshot(snapshot.withColumns(nextFuelColumns, snapshot.controlRodColumns()));
        }
        return true;
    }

    /** 返回当前结构绑定指定燃料列的唯一换料端口，不扫描世界。 */
    private ReactorPortBlockEntity findRefuelingPort(CoreColumnPosition position) {
        if (position == null) {
            return null;
        }
        for (ReactorPortBlockEntity port : boundPorts(
                ReactorPortBlockEntity.BindingType.REFUELING)) {
            if (position.equals(port.boundColumn())) {
                return port;
            }
        }
        return null;
    }

    /** 捕获快照已拥有或端口已装料的列物品，缺失端口时直接拒绝结算。 */
    private Map<CoreColumnPosition, ItemStack> captureFuelPortItems() {
        TreeMap<CoreColumnPosition, ItemStack> captured = new TreeMap<>();
        for (CoreColumnPosition position : snapshot.fuelColumns().keySet()) {
            ReactorStructureDefinition.ColumnMapping mapping = structureScan.columns().get(position);
            ReactorPortBlockEntity port = findRefuelingPort(position);
            if (mapping == null || mapping.type() != ReactorStructureDefinition.ColumnType.FUEL
                    || port == null || !FuelAssemblyItemCodec.isValidStoredFuel(port.fuelAssembly())) {
                return null;
            }
            captured.put(position, port.fuelAssembly());
        }
        return captured;
    }

    /** 在任何端口写入前准备完整下一状态；任一校验失败都会返回空并保持原状态。 */
    private Map<CoreColumnPosition, ItemStack> prepareFuelPortCommit(
            Map<CoreColumnPosition, ItemStack> beforeItems,
            ReactorSnapshot nextSnapshot
    ) {
        TreeMap<CoreColumnPosition, ItemStack> nextItems = new TreeMap<>();
        for (Map.Entry<CoreColumnPosition, ItemStack> entry : beforeItems.entrySet()) {
            CoreColumnPosition position = entry.getKey();
            ReactorPortBlockEntity port = findRefuelingPort(position);
            ItemStack beforeItem = entry.getValue();
            if (port == null || level.getBlockEntity(port.getBlockPos()) != port
                    || !port.isBoundTo(this, ReactorPortBlockEntity.BindingType.REFUELING, position)
                    || !ItemStack.matches(beforeItem, port.fuelAssembly())) {
                LOGGER.warn("反应堆 {} 燃料 tick 提交前端口 {} 状态已变化，取消整 tick",
                        worldPosition, position);
                return null;
            }
            FuelColumnState nextColumn = nextSnapshot.fuelColumns().get(position);
            if (nextColumn == null) {
                return null;
            }
            ItemStack nextItem;
            if (!nextColumn.fuelAssembly().present()) {
                nextItem = FuelAssemblyItemCodec.isCooledSpentFuel(beforeItem)
                        ? beforeItem.copyWithCount(1) : ItemStack.EMPTY;
            } else if (nextColumn.fuelAssembly().exhausted()) {
                nextItem = FuelAssemblyItemCodec.isCooledSpentFuel(beforeItem)
                        ? beforeItem.copyWithCount(1)
                        : FuelAssemblyItemCodec.createCooledSpentFuel();
            } else {
                if (!FuelAssemblyItemCodec.isFreshFuel(beforeItem)
                        || beforeItem.getMaxDamage() != nextColumn.fuelAssembly().maxDamage()) {
                    return null;
                }
                nextItem = FuelAssemblyItemCodec.copyWithDamage(
                        beforeItem, nextColumn.fuelAssembly().damage());
            }
            if (!FuelAssemblyItemCodec.isValidStoredFuel(nextItem)) {
                return null;
            }
            nextItems.put(position, nextItem);
        }
        return nextItems;
    }

    /** 比较本 tick 前后的端口物品，避免只有乏燃料转换时错误返回“无变化”。 */
    private static boolean sameFuelPortItems(
            Map<CoreColumnPosition, ItemStack> before,
            Map<CoreColumnPosition, ItemStack> after
    ) {
        if (!before.keySet().equals(after.keySet())) {
            return false;
        }
        for (CoreColumnPosition position : before.keySet()) {
            if (!ItemStack.matches(before.get(position), after.get(position))) {
                return false;
            }
        }
        return true;
    }

    /** 玩家或其他事务提交前再次确认端口实体、绑定和物品仍与读阶段一致。 */
    boolean validateFuelColumnCommit(
            ReactorPortBlockEntity port,
            CoreColumnPosition position,
            ItemStack expectedStored,
            FuelColumnState nextColumn
    ) {
        return level != null && !level.isClientSide
                && port != null
                && level.getBlockEntity(port.getBlockPos()) == port
                && port.isBoundTo(this, ReactorPortBlockEntity.BindingType.REFUELING, position)
                && ItemStack.matches(expectedStored, port.fuelAssembly())
                && nextColumn != null;
    }

    /** 将指定职责的局部端口位置转换为本结构的世界位置并加入预期绑定。 */
    private void addExpectedPorts(
            Map<BlockPos, PortBindingSpec> expected,
            ReactorStructureDefinition.PortType portType,
            ReactorPortBlockEntity.BindingType bindingType,
            CoreColumnPosition column
    ) {
        for (ReactorStructureDefinition.LocalPosition position : structureScan.ports()
                .getOrDefault(portType, List.of())) {
            expected.put(absolutePosition(position), new PortBindingSpec(bindingType, column));
        }
    }

    /** 将结构局部坐标转换成不可变世界坐标。 */
    private BlockPos absolutePosition(ReactorStructureDefinition.LocalPosition position) {
        return structureOrigin.offset(position.x(), position.y(), position.z()).immutable();
    }

    /** 将正式 tick 的燃料列遥测分发到对应换料端口，将控制棒列完整度分发到对应驱动器。 */
    private void syncColumnTelemetryDisplays() {
        if (level == null || level.isClientSide || !structureScan.valid() || structureOrigin == null) {
            return;
        }
        for (ReactorPortBlockEntity port : boundPorts(ReactorPortBlockEntity.BindingType.REFUELING)) {
            port.setServerFuelColumnTelemetry(findFuelColumnTelemetry(port.boundColumn()));
        }
        for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                : structureScan.columns().entrySet()) {
            if (entry.getValue().type() != ReactorStructureDefinition.ColumnType.CONTROL_ROD) {
                continue;
            }
            BlockPos drivePos = structureOrigin.offset(
                    entry.getValue().capPosition().x(),
                    entry.getValue().capPosition().y(),
                    entry.getValue().capPosition().z());
            if (level.getBlockEntity(drivePos) instanceof ControlRodDriveBlockEntity drive) {
                drive.setServerControlRodIntegrity(findControlRodIntegrity(entry.getKey()));
            }
        }
    }

    /** 清空旧结构中的控制棒驱动器完整度，避免结构重扫后继续显示旧列数据。 */
    private void clearControlRodTelemetry() {
        if (level == null || level.isClientSide || structureOrigin == null) {
            return;
        }
        for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                : structureScan.columns().entrySet()) {
            if (entry.getValue().type() != ReactorStructureDefinition.ColumnType.CONTROL_ROD) {
                continue;
            }
            BlockPos drivePos = structureOrigin.offset(
                    entry.getValue().capPosition().x(),
                    entry.getValue().capPosition().y(),
                    entry.getValue().capPosition().z());
            if (level.getBlockEntity(drivePos) instanceof ControlRodDriveBlockEntity drive) {
                drive.setServerControlRodIntegrity(null);
            }
        }
    }

    /** 从最近一次正式 tick 的稳定列表中查找指定燃料列，不为缺失列伪造运行数据。 */
    private ReactorInstrumentTelemetry.FuelColumnTelemetry findFuelColumnTelemetry(
            CoreColumnPosition position
    ) {
        if (position == null || !telemetry.available()) {
            return null;
        }
        return telemetry.fuelColumns().stream()
                .filter(column -> column.position().equals(position))
                .findFirst()
                .orElse(null);
    }

    /** 从最近一次正式 tick 的稳定列表中查找指定控制棒列完整度。 */
    private Double findControlRodIntegrity(CoreColumnPosition position) {
        if (position == null || !telemetry.available()) {
            return null;
        }
        return telemetry.controlRodColumns().stream()
                .filter(column -> column.position().equals(position))
                .map(ReactorInstrumentTelemetry.ControlRodColumnTelemetry::controlRodColumnIntegrity)
                .findFirst()
                .orElse(null);
    }

    /** 清理本仪表端口上一次扫描建立的全部运行时绑定。 */
    private void clearPortBindings(boolean structureInvalidated) {
        for (ReactorPortBlockEntity port : List.copyOf(boundPorts)) {
            port.clearBinding(this, structureInvalidated);
        }
        boundPorts.clear();
    }

    /** 从反向端口生命周期中移除已卸载端口，不改变其他端口或反应堆快照。 */
    void detachPort(ReactorPortBlockEntity port) {
        if (port != null) {
            boundPorts.remove(port);
        }
    }

    private static String portConnectionId(ReactorPortBlockEntity port) {
        return "port:" + port.getBlockPos().asLong();
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

    /**
     * 反应堆仪表端口被移除或区块卸载时，立即撤销所有端口的运行时所有权。
     *
     * <p>{@link P1MinimalBlockEntity} 的 Create 生命周期会在这里之后继续卸载 behaviour；
     * 快照本身不清空，只有运行时绑定和结构缓存失效，确保旧 handler 不能复活到新所有者。</p>
     */
    @Override
    public void invalidate() {
        // 区块卸载不是安全重扫，不能借生命周期事件改变未成型取料锁。
        clearPortBindings(false);
        clearControlRodTelemetry();
        structureScan = ReactorStructureDefinition.ScanResult.notScanned();
        structureOrigin = null;
        super.invalidate();
    }

    /**
     * 向 Create 护目镜叠加层提供最近一次服务端同步的静态摘要。
     *
     * <p>Create 已负责确认玩家佩戴护目镜并且正在观察本方块；此回调只读取客户端副本，
     * 不扫描世界、不发起请求、不写入快照或结构状态。字段顺序与单位由语言资源固定。</p>
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "structure_summary"));
        if (!clientStructureSummary.valid()) {
            tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "unavailable"));
            return true;
        }

        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "structure_size",
                clientStructureSummary.width(), clientStructureSummary.height(),
                clientStructureSummary.depth()));
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "fuel_columns",
                clientStructureSummary.fuelColumnCount()));
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "control_rod_columns",
                clientStructureSummary.controlRodColumnCount()));
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "cold_ports",
                clientStructureSummary.coldPortCount()));
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "hot_ports",
                clientStructureSummary.hotPortCount()));
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "fluid_capacity",
                clientStructureSummary.totalFluidCapacityMb()));
        ReactorInstrumentGoggleDisplay.appendDynamicTooltip(
                tooltip, clientStructureSummary, clientTelemetry);
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        CompoundTag snapshotTag = ReactorSnapshotNbtCodec.encode(snapshot);
        if (!pendingLegacyFuelAssemblies.isEmpty()) {
            net.minecraft.nbt.ListTag migration = new net.minecraft.nbt.ListTag();
            pendingLegacyFuelAssemblies.forEach((position, assembly) -> {
                CompoundTag entry = new CompoundTag();
                entry.putInt("X", position.x());
                entry.putInt("Z", position.z());
                entry.putBoolean("Present", assembly.present());
                entry.putInt("Damage", assembly.damage());
                entry.putInt("MaxDamage", assembly.maxDamage());
                migration.add(entry);
            });
            snapshotTag.put(LEGACY_FUEL_MIGRATION_KEY, migration);
        }
        tag.put(SNAPSHOT_KEY, snapshotTag);
        if (clientPacket) {
            tag.put(STRUCTURE_SUMMARY_KEY, structureSummary().writeSyncTag());
            tag.put(TELEMETRY_KEY, ReactorInstrumentTelemetryNbtCodec.encode(telemetry));
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        CompoundTag snapshotTag = tag.contains(SNAPSHOT_KEY)
                ? tag.getCompound(SNAPSHOT_KEY) : null;
        ReactorSnapshotNbtCodec.DecodedSnapshot decoded =
                ReactorSnapshotNbtCodec.decodeWithMigration(snapshotTag);
        snapshot = decoded.snapshot().withoutFuelAssemblies();
        TreeMap<CoreColumnPosition, FuelAssemblyState> pending =
                new TreeMap<>(decoded.legacyFuelAssemblies());
        if (snapshotTag != null && snapshotTag.contains(LEGACY_FUEL_MIGRATION_KEY)) {
            net.minecraft.nbt.ListTag migration = snapshotTag.getList(
                    LEGACY_FUEL_MIGRATION_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < migration.size(); index++) {
                CompoundTag entry = migration.getCompound(index);
                if (!entry.contains("X") || !entry.contains("Z")) {
                    continue;
                }
                try {
                    CoreColumnPosition position = new CoreColumnPosition(
                            entry.getInt("X"), entry.getInt("Z"));
                    int maxDamage = Math.max(0, entry.getInt("MaxDamage"));
                    if ((entry.getBoolean("Present") || maxDamage > 0) && maxDamage > 0) {
                        pending.put(position, FuelAssemblyState.installed(
                                maxDamage,
                                Math.max(0, Math.min(maxDamage, entry.getInt("Damage")))));
                    }
                } catch (IllegalArgumentException ignored) {
                    // 忽略超出固定堆芯坐标范围的迁移条目，不能让损坏 NBT 阻塞方块实体加载。
                }
            }
        }
        pendingLegacyFuelAssemblies = Map.copyOf(pending);
        if (clientPacket) {
            clientStructureSummary = ReactorInstrumentStructureSummary.readSyncTag(
                    tag.contains(STRUCTURE_SUMMARY_KEY)
                            ? tag.getCompound(STRUCTURE_SUMMARY_KEY)
                            : null);
            clientTelemetry = ReactorInstrumentTelemetryNbtCodec.decode(
                    tag.contains(TELEMETRY_KEY) ? tag.getCompound(TELEMETRY_KEY) : null);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put(SNAPSHOT_KEY, ReactorSnapshotNbtCodec.encode(snapshot));
        tag.put(STRUCTURE_SUMMARY_KEY, structureSummary().writeSyncTag());
        tag.put(TELEMETRY_KEY, ReactorInstrumentTelemetryNbtCodec.encode(telemetry));
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 结构端口的最小绑定描述，不保存世界对象或反应堆状态。 */
    private record PortBindingSpec(
            ReactorPortBlockEntity.BindingType type,
            CoreColumnPosition column
    ) {
    }
}
