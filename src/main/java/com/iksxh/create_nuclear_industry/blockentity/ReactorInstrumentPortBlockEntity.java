package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.goggle.GoggleTooltip;
import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.content.P1ContentIds;
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
import com.iksxh.create_nuclear_industry.reactor.ReactorFullShutdownAssessment;
import com.iksxh.create_nuclear_industry.reactor.ReactorFullShutdownResult;
import com.iksxh.create_nuclear_industry.reactor.ReactorServerTick;
import com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters;
import com.iksxh.create_nuclear_industry.reactor.MeltdownStatus;
import com.iksxh.create_nuclear_industry.reactor.ReactorMeltdownEvent;
import com.iksxh.create_nuclear_industry.reactor.ReactorMeltdownEvents;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorInstrumentStructureSummary;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureScanner;
import com.iksxh.create_nuclear_industry.structure.ReactorDisassemblyPlan;
import com.iksxh.create_nuclear_industry.structure.ReactorSurfaceDescriptor;
import com.iksxh.create_nuclear_industry.structure.ReactorSurfaceDescriptorFactory;
import com.iksxh.create_nuclear_industry.structure.ReactorSurfaceSyncEvents;
import com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor;
import com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptorFactory;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private static final String SURFACE_KEY = "ReactorSurface";
    private static final String LEGACY_FUEL_MIGRATION_KEY = "LegacyFuelMigration";
    private static final String GOGGLE_KEY_PREFIX = "goggle.create_nuclear_industry.reactor.";
    private static final int TELEMETRY_SYNC_INTERVAL_TICKS = 5;
    private static final String TELEMETRY_UNAVAILABLE_REASON = "telemetry is unavailable";
    private static final double SAFETY_HEAT_EPSILON = 1.0E-12D;
    private static final Logger LOGGER = LogUtils.getLogger();

    private ReactorSnapshot snapshot = ReactorSnapshot.empty();
    private com.iksxh.create_nuclear_industry.automation.ReactorAutomationState automationState = com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.initial();
    private com.iksxh.create_nuclear_industry.automation.ReactorAutomationBinding automationBinding;
    private java.util.UUID automationGeneration = java.util.UUID.randomUUID();
    private com.iksxh.create_nuclear_industry.automation.ReactorAutomationObservation automationObservation;
    private com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.Session automationResumeSession;
    private com.iksxh.create_nuclear_industry.automation.ReactorLoadFollowSession automationLoadFollowSession;
    public com.iksxh.create_nuclear_industry.automation.ReactorLoadFollowSession automationLoadFollowSession() {return automationLoadFollowSession;}
    /** 当前服务端实例的暂态闭环；清理时立即释放同网登记，NBT不保存积分/表引用。 */
    public void setAutomationLoadFollowSession(com.iksxh.create_nuclear_industry.automation.ReactorLoadFollowSession session) {
        if(session==null)com.iksxh.create_nuclear_industry.automation.ReactorStressGaugeService.release(this);
        automationLoadFollowSession=session;
    }
    private CompoundTag automationNormalStopCheckpoint;
    private boolean automationSavedScramAwaitingHigh;
    private boolean automationInactiveLoadPending;
    private long automationInactiveLoadStarted=Long.MIN_VALUE;
    private java.util.Set<CoreColumnPosition> automationLoadedFuelLayout;
    /** 服务端本次NBT读取的完整物理FUEL位置副本；null代表缺失/坏证据，扫描刷新不覆盖它。 */
    public java.util.Set<CoreColumnPosition> automationLoadedFuelLayout() {return automationLoadedFuelLayout;}
    /** 新实例加载交接，只有当前核验通过并真实付费后才恢复运行；不持久化本对象。 */
    public com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.Session automationResumeSession() {return automationResumeSession;}
    public void setAutomationResumeSession(com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.Session session) {automationResumeSession=session;}
    /** 正常停服事件捕获的非权威信封；用于保存顺序，不是旧运行许可或付热收据。 */
    public CompoundTag automationNormalStopCheckpoint() {return automationNormalStopCheckpoint;}
    public void setAutomationNormalStopCheckpoint(CompoundTag checkpoint) {automationNormalStopCheckpoint=checkpoint;}
    private final com.iksxh.create_nuclear_industry.automation.ReactorAutomationController.CurveCache automationCurve = new com.iksxh.create_nuclear_industry.automation.ReactorAutomationController.CurveCache();
    private long automationTickTime = Long.MIN_VALUE;
    private Object automationBurner;
    private com.iksxh.create_nuclear_industry.automation.ReactorAutomationPortBlockEntity automationPort;
    /** 已加载管理员实例身份，坐标相同的替换也必须换代。 */
    public Object automationBurner() { return automationBurner; }
    public void setAutomationBurner(Object burner) { automationBurner = burner; }
    /** 已加载接口的实例身份；同坐标新BE必须先重新建立安全授权。 */
    public com.iksxh.create_nuclear_industry.automation.ReactorAutomationPortBlockEntity automationPort() { return automationPort; }
    public void setAutomationPort(com.iksxh.create_nuclear_industry.automation.ReactorAutomationPortBlockEntity port) { automationPort = port; }
    private final java.util.ArrayDeque<Double> automationHeatWindow = new java.util.ArrayDeque<>();
    public void recordAutomationDisplayHeat(double heat, int window) {
        automationHeatWindow.addLast(heat); while (automationHeatWindow.size() > window) automationHeatWindow.removeFirst();
    }
    public double automationAverageHeat() { return automationHeatWindow.stream().mapToDouble(Double::doubleValue).average().orElse(0); }

    /** 最后已验证的占据证据只供缺区块拆除防护；不能用于成型、库存或客户端显示。 */
    private ReactorSurfaceDescriptor breakSafetySurface;
    private java.util.Set<CoreColumnPosition> breakSafetyFuelColumns = java.util.Set.of();
    /** 当前NBT的占据证据须等首次有效扫描确认，不能用FULL可用性或失败扫描提前撤销。 */
    private boolean breakSafetyAwaitingRestoreScan;
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
    /** 每次新建 BE 具有独立展示身份，不持久化到库存或热工快照。 */
    private final java.util.UUID surfaceGeneration = java.util.UUID.randomUUID();
    private ReactorSurfaceDescriptor surfaceDescriptor;
    private static final String RUNTIME_KEY = "ReactorRuntime";
    private ReactorRuntimeDescriptor runtimeDescriptor;
    private long runtimeRevision;
    private long runtimeSample;
    private int runtimeTicksSinceLastSync;
    /** 正式结算内的 hydrate/setSnapshot 只能发送旧完整样本；通用外部快照写入必须撤销。 */
    private boolean runtimeSettlementInProgress;
    private boolean surfaceChunkUnloaded;
    private final Set<ReactorPortBlockEntity> boundPorts =
            Collections.newSetFromMap(new IdentityHashMap<>());
    /** 服务端当前绑定的逐列索引；仅随重扫/解绑维护，不持久化，命中仍须核对实时身份。 */
    private final Map<CoreColumnPosition, ReactorPortBlockEntity> refuelingPortsByColumn = new HashMap<>();

    public ReactorInstrumentPortBlockEntity(BlockPos pos, BlockState state) {
        super(P1BlockEntities.REACTOR_INSTRUMENT_PORT.get(), pos, state);
    }

    /** 仅服务端控制服务写入；控制权与授权必须先于邻居通知，客户端不能使用此状态提交。 */
    public com.iksxh.create_nuclear_industry.automation.ReactorAutomationState automationState() { return automationState; }
    public void setAutomationState(com.iksxh.create_nuclear_industry.automation.ReactorAutomationState state) {
        if (level != null && level.isClientSide) throw new IllegalStateException("客户端不能提交管理员状态");
        // 原GUI STOP/保护决定立即取消待续接；普通运行交接由Resume先清session再提交，不能续旧意图。
        if(state.phase()==com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.Phase.STOPPED
                ||state.phase()==com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.Phase.FAULT) {
            automationResumeSession=null;automationInactiveLoadPending=false;
        }
        if(state.requestedMode()!=com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.Mode.LOAD_FOLLOW
                ||state.phase()==com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.Phase.STOPPED
                ||state.phase()==com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.Phase.FAULT)setAutomationLoadFollowSession(null);
        if(!com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.normalShutdownActive(this))automationNormalStopCheckpoint=null;
        automationState = state;
        setChanged();
        syncControlRodDrives();
    }
    public com.iksxh.create_nuclear_industry.automation.ReactorAutomationBinding automationBinding() { return automationBinding; }
    public void setAutomationBinding(com.iksxh.create_nuclear_industry.automation.ReactorAutomationBinding binding) { automationBinding = binding; }
    public java.util.UUID automationGeneration() { return automationGeneration; }
    public com.iksxh.create_nuclear_industry.automation.ReactorAutomationObservation automationObservation() { return automationObservation; }
    public void setAutomationObservation(com.iksxh.create_nuclear_industry.automation.ReactorAutomationObservation sample) { automationObservation = sample; }
    public com.iksxh.create_nuclear_industry.automation.ReactorAutomationController.CurveCache automationCurve() { return automationCurve; }
    /** 同一游戏tick最多一次管理员调节，显式诊断结算不加快租约或复位计数。 */
    public boolean beginAutomationTick(long time) { if (automationTickTime == time) return false; automationTickTime = time; return true; }
    public ReactorSimulationParameters automationSimulationParameters() { return simulationParameters(); }
    public ReactorServerTick.CoolantInput automationCoolantInput() { return coolantInput(); }

    /** 返回该反应堆不可变的服务端权威快照。 */
    public ReactorSnapshot snapshot() {
        return snapshot;
    }

    private final com.iksxh.create_nuclear_industry.automation.ReactorAutomationHeatSupply.Account automationHeatAccount =
            new com.iksxh.create_nuclear_industry.automation.ReactorAutomationHeatSupply.Account();
    /** 服务端实例内的已支付短期凭据；不持久化，不由显示包恢复。 */
    public com.iksxh.create_nuclear_industry.automation.ReactorAutomationHeatSupply.Account automationHeatAccount() { return automationHeatAccount; }

    /** 替换权威快照，并安排持久化与客户端同步。 */
    public void setSnapshot(ReactorSnapshot snapshot) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("reactor state can only be changed on the server");
        }
        this.snapshot = Objects.requireNonNull(snapshot, "reactor snapshot is required");
        if (!runtimeSettlementInProgress) invalidateRuntimeDescriptor();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncControlRodDrives();
        }
    }

    /**
     * 仅供服务端普通流体事务替换共享冷/热库存，输入单位 mB；不接收列、控制、热工或容量。
     *
     * <p>可靠 L1 与已有可用 L2 仍匹配当前结构和 owner 时，完整保留上一正式结算信封，
     * 不推进 sample/revision/time，也不把新库存拼入旧热工。客户端重复样本不续租，
     * 已过期显示不能因此复活；下一正式成功结算才投影完整新样本。其他情况仍撤销 L2，
     * 原权威库存持久化、更新包及控制棒同步保持。</p>
     *
     * @param coldCoolantMb 替换后的非负冷液库存，单位 mB
     * @param hotCoolantMb 替换后的非负热液库存，单位 mB
     */
    public void setCoolantInventories(long coldCoolantMb, long hotCoolantMb) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("reactor state can only be changed on the server");
        }
        boolean retainCompletedSample = canRetainRuntimeSampleForInventory();
        snapshot = snapshot.withCoolantInventories(coldCoolantMb, hotCoolantMb);
        if (!retainCompletedSample) invalidateRuntimeDescriptor();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncControlRodDrives();
        }
    }

    /** 只核对现有服务端缓存和信封身份，不重扫、不查加载区块，也不恢复不可用样本。 */
    private boolean canRetainRuntimeSampleForInventory() {
        if (!(level instanceof ServerLevel) || isRemoved() || !structureScan.valid()
                || !snapshotMatchesStructure() || structureOrigin == null
                || surfaceDescriptor == null || !surfaceDescriptor.valid()
                || runtimeDescriptor == null || !runtimeDescriptor.available()) return false;
        String dimension = level.dimension().location().toString();
        return surfaceDescriptor.dimension().equals(dimension) && runtimeDescriptor.dimension().equals(dimension)
                && surfaceDescriptor.ownerPos().equals(worldPosition) && runtimeDescriptor.ownerPos().equals(worldPosition)
                && surfaceDescriptor.ownerGeneration().equals(surfaceGeneration)
                && runtimeDescriptor.ownerGeneration().equals(surfaceGeneration)
                && runtimeDescriptor.geometryRevision() == surfaceDescriptor.revision()
                && surfaceDescriptor.origin().equals(structureOrigin)
                && runtimeDescriptor.origin().equals(surfaceDescriptor.origin())
                && runtimeDescriptor.maxInclusive().equals(surfaceDescriptor.maxInclusive());
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
        return structureScan.valid() && structureChunksAvailable();
    }

    /** 仅检查缓存包围盒相交的至多四个区块是否可读，不访问方块或强制加载。 */
    public boolean structureChunksAvailable() {
        if (level == null || level.isClientSide || structureOrigin == null) return true;
        var dimensions = structureScan.dimensions();
        for (int x = structureOrigin.getX() >> 4;
             x <= (structureOrigin.getX() + dimensions.width() - 1) >> 4; x++) {
            for (int z = structureOrigin.getZ() >> 4;
                 z <= (structureOrigin.getZ() + dimensions.depth() - 1) >> 4; z++) {
                if (!level.hasChunk(x, z)) return false;
            }
        }
        return true;
    }

    /**
     * 返回最近一次结构缓存对应的静态摘要；读取只发生在服务端已缓存数据上，不触发扫描。
     *
     * <p>容量从当前服务端配置和最近一次有效结构扫描中的主体坐标派生，因此配置热更新后
     * 摘要会自然反映新的共享容量；该读取不会重新扫描世界，也不属于反应堆运行快照。</p>
     */
    public ReactorInstrumentStructureSummary structureSummary() {
        return ReactorInstrumentStructureSummary.from(
                structureScan,
                P1ServerConfig.VALUES.coolantCapacityPerEmptyBlockMb.get().longValue()
        );
    }

    /** 返回当前有效结构和服务端配置派生的共享容量；无效结构返回零。 */
    public long coolantCapacityMb() {
        ReactorInstrumentStructureSummary summary = structureSummary();
        return summary.valid() ? summary.coolantCapacityMb() : 0L;
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
        invalidateRuntimeDescriptor();
        if (!structureScan.equals(scan.contract()) || !Objects.equals(structureOrigin, scan.origin())) {
            // 初次合法扫描可建立新代际；交接一旦验证当前结构，后续真实变化必须先撤反馈，不能沿旧绑定首付。
            if(com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.hasVerifiedStructure(this))
                com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.fail(this,"structure_changed");
            // 读档的无权STOP/FAULT首次建缓存不是运行期结构变动；真实失败仍由有界加载门转入保护。
            if (automationState.claimed() && !automationInactiveLoadPending && !com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.pending(this)) com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.revoke(this, "structure_changed");
            com.iksxh.create_nuclear_industry.control.ControlRodSliderService.revokeOwnerSessions(this);
            automationGeneration = java.util.UUID.randomUUID();
            automationObservation = null;
            automationCurve.clear();
        }
        structureScan = scan.contract();
        structureOrigin = scan.origin();
        com.iksxh.create_nuclear_industry.radiation.RadiationContainment.remember(this);
        structureScanCount++;
        if (structureScan.valid()) {
            // 无核状态的初始化/安全清空可绑定新尺寸并保留纯冷液；真实核状态禁止借重扫热重组。
            if (!snapshot.dimensions().equals(structureScan.dimensions())) {
                if (!canInitializeScannedDimensions()) {
                    structureScan = new ReactorStructureDefinition.ScanResult(false,
                            "existing reactor state dimensions differ; destructive maintenance is required",
                            ReactorStructureDefinition.DiagnosticCode.DIMENSIONS, Map.of(), Map.of());
                    clearPortBindings(true);
                    updateSurfaceDescriptor(new ReactorStructureScanner.WorldScanResult(null, structureScan));
                    invalidateTelemetry();
                    return;
                }
                setSnapshot(snapshot.withDimensions(structureScan.dimensions()));
            }
            if(com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.pending(this)
                    &&!com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.matchingColumns(this))
                com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.fail(this,"structure_changed");
            if(automationInactiveLoadPending&&!com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.matchingColumns(this)) {
                automationInactiveLoadPending=false;
                com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.fail(this,"structure_changed");
            }
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
        updateSurfaceDescriptor(scan);
        if (structureScan.valid() && surfaceDescriptor != null && surfaceDescriptor.valid()) {
            breakSafetySurface = surfaceDescriptor;
            breakSafetyAwaitingRestoreScan = false;
            breakSafetyFuelColumns = structureScan.columns().entrySet().stream()
                    .filter(entry -> entry.getValue().type() == ReactorStructureDefinition.ColumnType.FUEL)
                    .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toUnmodifiableSet());
            setChanged();
        }
        invalidateTelemetry();
    }

    /**
     * 在服务端已接受的几何扫描上判断初始化尺寸是否安全；容量及冷液单位均为mB。
     *
     * <p>完全停机清空与首次成型之间可能已有冷端实际注液；仅无任何列、热液、控制或
     * 事故状态且无待迁移燃料时允许原量保留冷液。新容量不足时拒绝，不能截断库存或
     * 把真实核状态重组到另一尺寸。本判定不扫描、不写状态，调用前必须已有有效扫描。</p>
     */
    private boolean canInitializeScannedDimensions() {
        return snapshot.fuelColumns().isEmpty()
                && snapshot.controlRodColumns().isEmpty()
                && snapshot.hotCoolantMb() == 0L
                && !snapshot.scramRequested()
                && snapshot.scramSavedTargetDepths().isEmpty()
                && snapshot.meltdownProgressTicks() == 0L
                && !snapshot.meltdownCountdownStarted()
                && !snapshot.meltdownEventPublished()
                && pendingLegacyFuelAssemblies.isEmpty()
                && snapshot.coldCoolantMb() <= coolantCapacityMb();
    }

    /**
     * 缺FULL或尚未完成恢复扫描时，原验证组件只能保守拒绝拆除；不读取其他区块。
     * 当前NBT恢复的证据必须匹配本仪表坐标、维度及权威外尺寸，绝不据此宣告成型。
     */
    public boolean protectsUnavailableComponent(BlockPos pos, BlockState state) {
        var geometry = breakSafetySurface;
        if (level == null || level.isClientSide || geometry == null || !geometry.valid()
                || !geometry.ownerPos().equals(worldPosition)
                || !geometry.dimension().equals(level.dimension().location().toString())) return false;
        var origin = geometry.origin();
        var max = geometry.maxInclusive();
        var dimensions = snapshot.dimensions();
        if (max.getX() - origin.getX() + 1 != dimensions.width()
                || max.getY() - origin.getY() + 1 != dimensions.height()
                || max.getZ() - origin.getZ() + 1 != dimensions.depth()) return false;
        boolean available = true;
        for (int x = origin.getX() >> 4; x <= max.getX() >> 4; x++) {
            for (int z = origin.getZ() >> 4; z <= max.getZ() >> 4; z++) available &= level.hasChunk(x, z);
        }
        if (available && (structureScan.valid() || !breakSafetyAwaitingRestoreScan
                && structureScan.diagnosticCode() != ReactorStructureDefinition.DiagnosticCode.CHUNKS_UNAVAILABLE)) return false;
        String actualId = blockId(state);
        if (geometry.members().stream().anyMatch(member -> member.pos().equals(pos)
                && member.expectedBlockId().equals(actualId))) return true;
        int x = pos.getX() - origin.getX(), y = pos.getY() - origin.getY(), z = pos.getZ() - origin.getZ();
        return x > 0 && x < dimensions.width() - 1 && z > 0 && z < dimensions.depth() - 1
                && y > 0 && y < dimensions.height() - 1
                && id(P1ContentIds.REACTOR_FUEL_ROD_ID).equals(actualId)
                && breakSafetyFuelColumns.contains(new CoreColumnPosition(x - 1, z - 1));
    }

    /** 返回当前实例最近收到或生成的不可变显示信封；未知数据以 Optional.empty 表示。 */
    public java.util.Optional<ReactorSurfaceDescriptor> surfaceDescriptor() {
        return java.util.Optional.ofNullable(surfaceDescriptor);
    }

    /** 服务端最新完整结算或客户端最近收到信封；不可用样本不能用于动画，不触发计算。 */
    public java.util.Optional<ReactorRuntimeDescriptor> runtimeDescriptor() {
        return java.util.Optional.ofNullable(runtimeDescriptor);
    }

    /** 撤销仅影响 L2，保留权威快照、旧遥测和 L1；重复早退不重复发送相同失效。 */
    private void invalidateRuntimeDescriptor() {
        if (runtimeDescriptor == null || !runtimeDescriptor.available()) return;
        if (level != null && level.isClientSide) { runtimeDescriptor = null; return; }
        runtimeDescriptor = ReactorRuntimeDescriptor.unavailable(runtimeDescriptor.dimension(), worldPosition,
                surfaceGeneration, ++runtimeRevision, surfaceDescriptor == null ? 0 : surfaceDescriptor.revision(),
                runtimeSample, level == null ? runtimeDescriptor.serverGameTime() : Math.max(0, level.getGameTime()));
        runtimeTicksSinceLastSync = 0;
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** 仅扫描缓存变化驱动表面投影；相同几何保留对象及版本，避免遥测触发模型全域刷新。 */
    private void updateSurfaceDescriptor(ReactorStructureScanner.WorldScanResult scan) {
        if (level == null || level.isClientSide) return;
        long revision = surfaceDescriptor == null ? 0 : surfaceDescriptor.revision();
        ReactorSurfaceDescriptor next = ReactorSurfaceDescriptorFactory.project(
                level, worldPosition, surfaceGeneration, revision, scan);
        if (next.sameGeometry(surfaceDescriptor)) return;
        surfaceDescriptor = next.withRevision(revision + 1);
    }

    public ControlRodScramResult updateRedstoneScram(boolean powered) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("SCRAM can only be changed on the server");
        }
        // 加载冻结窗口只采样真实电平，不先全插再提棒；保存的SCRAM另锁住初始化低沿，须真实高后才可释放。
        if(com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.pending(this)
                ||com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.normalShutdownActive(this)
                ||automationSavedScramAwaitingHigh&&!powered)
            return new ControlRodScramResult(snapshot.scramRequested()?com.iksxh.create_nuclear_industry.control.ControlRodScramStatus.SCRAM_ALREADY_ACTIVE:com.iksxh.create_nuclear_industry.control.ControlRodScramStatus.SCRAM_NOT_ACTIVE,false,false,0,"administrator initializes current redstone feedback");
        if(powered)automationSavedScramAwaitingHigh=false;
        return ControlRodScramService.apply(this, powered);
    }

    /**
     * 在破坏事件写入前只读构造一个停机拆除计划。
     *
     * <p>该阶段先按当前结构映射逐列枚举顶部换料端口，再从精确物品栈构造无副作用的
     * 燃料投影，并用该投影完成危险/完全停机判定。任何端口、绑定、物品或配置无法
     * 证明时返回拒绝计划；方法本身不写快照、端口、事件总线或世界。</p>
     *
     * @param brokenPos 即将被玩家破坏的世界坐标
     * @param brokenState BreakEvent 携带的方块状态
     * @return 当前仪表端口确实拥有该位置时返回计划，否则返回 {@code null}
     */
    public ReactorDisassemblyPlan prepareDisassemblyPlan(
            BlockPos brokenPos,
            BlockState brokenState
    ) {
        if (!(level instanceof ServerLevel) || level.isClientSide
                || brokenPos == null || brokenState == null
                || isRemoved()
                || level.getBlockEntity(worldPosition) != this
                || !structureScan.valid() || structureOrigin == null) {
            return null;
        }

        String expectedId = cachedComponentId(brokenPos);
        if (expectedId == null) {
            return null;
        }
        if (!expectedId.equals(blockId(brokenState))
                || !expectedId.equals(blockId(level.getBlockState(brokenPos)))) {
            return rejectedDisassemblyPlan(brokenPos, brokenState, ReactorFullShutdownResult.invalid());
        }

        List<ReactorDisassemblyPlan.PortCapture> fuelPorts = captureFuelPortsForReset();
        if (fuelPorts == null) {
            return rejectedDisassemblyPlan(
                    brokenPos, brokenState, ReactorFullShutdownResult.invalid());
        }

        ReactorSimulationParameters parameters;
        ReactorSnapshot projectedSnapshot;
        ReactorFullShutdownResult assessment;
        try {
            parameters = simulationParameters();
            projectedSnapshot = projectFuelSnapshot(fuelPorts);
            assessment = ReactorFullShutdownAssessment.assess(projectedSnapshot, parameters);
        } catch (RuntimeException exception) {
            LOGGER.debug("无法评估反应堆 {} 的停机拆除状态", worldPosition, exception);
            return rejectedDisassemblyPlan(brokenPos, brokenState, ReactorFullShutdownResult.invalid());
        }
        if (!assessment.inputValid()) {
            return rejectedDisassemblyPlan(brokenPos, brokenState, assessment);
        }

        boolean dangerous = assessment.generatedHeatHu() > SAFETY_HEAT_EPSILON
                || projectedSnapshot.meltdownCountdownStarted();
        if (dangerous) {
            boolean alreadyCommitted = projectedSnapshot.meltdownEventPublished()
                    || projectedSnapshot.meltdownProgressTicks() >= parameters.meltdownCountdownTicks();
            ReactorSnapshot nextSnapshot = alreadyCommitted
                    ? snapshot
                    : projectedSnapshot.withMeltdown(parameters.meltdownCountdownTicks(), true)
                            .withMeltdownEventPublished(true);
            return createDisassemblyPlan(
                    ReactorDisassemblyPlan.Action.DANGEROUS,
                    brokenPos,
                    brokenState,
                    expectedId,
                    snapshot,
                    projectedSnapshot,
                    nextSnapshot,
                    assessment,
                    fuelPorts,
                    pendingLegacyFuelAssemblies,
                    !alreadyCommitted
            );
        }

        if (!assessment.fullyStopped()) {
            return rejectedDisassemblyPlan(brokenPos, brokenState, assessment);
        }
        if (expectedId.equals(id(P1ContentIds.REACTOR_INSTRUMENT_PORT_ID))) {
            return createDisassemblyPlan(
                    ReactorDisassemblyPlan.Action.INSTRUMENT_MAINTENANCE,
                    brokenPos,
                    brokenState,
                    expectedId,
                    snapshot,
                    projectedSnapshot,
                    projectedSnapshot,
                    assessment,
                    fuelPorts,
                    pendingLegacyFuelAssemblies,
                    false
            );
        }

        return createDisassemblyPlan(
                ReactorDisassemblyPlan.Action.FULL_SHUTDOWN_RESET,
                brokenPos,
                brokenState,
                expectedId,
                snapshot,
                projectedSnapshot,
                // 原端口在Post重扫前仍可能注液，清空时保留实际尺寸避免退回五格占位。
                ReactorSnapshot.empty().withDimensions(snapshot.dimensions()),
                assessment,
                fuelPorts,
                pendingLegacyFuelAssemblies,
                false
        );
    }

    /**
     * 提交已经完成全量预检的危险或安全拆除计划；提交期间仍保持原方块存在。
     *
     * @param plan 当前仪表端口生成的预检计划
     * @return 写入成功或本计划无需写入时返回 {@code true}
     */
    public boolean commitDisassemblyPlan(ReactorDisassemblyPlan plan) {
        if (plan == null || plan.owner() != this
                || !validateDisassemblyPlan(plan)) {
            return false;
        }
        try {
            switch (plan.action()) {
                case DANGEROUS -> {
                    if (!snapshot.equals(plan.nextSnapshot())) {
                        setSnapshot(plan.nextSnapshot());
                    }
                }
                case FULL_SHUTDOWN_RESET -> {
                    for (ReactorDisassemblyPlan.PortCapture capture : plan.fuelPorts()) {
                        capture.port().setFuelAssembly(ItemStack.EMPTY);
                    }
                    pendingLegacyFuelAssemblies = Map.of();
                    setSnapshot(plan.nextSnapshot());
                    invalidateTelemetry();
                }
                case INSTRUMENT_MAINTENANCE -> {
                    if (!snapshot.equals(plan.nextSnapshot())) {
                        setSnapshot(plan.nextSnapshot());
                    }
                }
                case REJECTED -> {
                    return false;
                }
            }
            return true;
        } catch (RuntimeException exception) {
            LOGGER.warn("反应堆 {} 的停机拆除提交失败", worldPosition, exception);
            return false;
        }
    }

    /** 所有拆除计划提交、后检和事件发布均成功后才撤销保护，失败回滚保留原证据。 */
    public void finishDisassemblySafetyEvidence(ReactorDisassemblyPlan plan) {
        if (plan.owner() != this || plan.action() != ReactorDisassemblyPlan.Action.FULL_SHUTDOWN_RESET
                || !snapshot.equals(plan.nextSnapshot())) throw new IllegalArgumentException("invalid completed shutdown reset");
        breakSafetySurface = null;
        breakSafetyFuelColumns = java.util.Set.of();
        breakSafetyAwaitingRestoreScan = false;
        setChanged();
    }

    /** 验证提交后的计划状态，确认没有留下半清空快照或端口物品。 */
    public boolean verifyDisassemblyPlan(ReactorDisassemblyPlan plan) {
        if (plan == null || plan.owner() != this) {
            return false;
        }
        return switch (plan.action()) {
            case DANGEROUS, INSTRUMENT_MAINTENANCE -> snapshot.equals(plan.nextSnapshot())
                    && sameFuelPortCaptures(plan.fuelPorts(), captureFuelPortsForReset())
                    && pendingLegacyFuelAssemblies.equals(plan.pendingLegacyFuelAssemblies());
            case FULL_SHUTDOWN_RESET -> verifyFullShutdownReset(plan);
            case REJECTED -> false;
        };
    }

    /**
     * 恢复一次失败事务的精确快照、迁移信封、端口物品和运行时显示。
     *
     * <p>调用方只在本次 BreakEvent 事务失败时使用；恢复操作继续在服务端执行，失败会
     * 记录日志但不会再次发布融毁事件。</p>
     */
    public void rollbackDisassemblyPlan(ReactorDisassemblyPlan plan) {
        if (plan == null || plan.owner() != this
                || (plan.action() != ReactorDisassemblyPlan.Action.DANGEROUS
                && plan.action() != ReactorDisassemblyPlan.Action.INSTRUMENT_MAINTENANCE
                && plan.action() != ReactorDisassemblyPlan.Action.FULL_SHUTDOWN_RESET)) {
            return;
        }
        try {
            for (ReactorDisassemblyPlan.PortCapture capture : plan.fuelPorts()) {
                capture.port().setFuelAssembly(capture.fuelAssembly());
            }
            pendingLegacyFuelAssemblies = Map.copyOf(plan.pendingLegacyFuelAssemblies());
            if (!snapshot.equals(plan.beforeSnapshot())) {
                setSnapshot(plan.beforeSnapshot());
            }
            telemetry = plan.beforeTelemetry();
            lastSentTelemetry = plan.beforeLastSentTelemetry();
            telemetryTicksSinceLastSync = plan.beforeTelemetryTicksSinceLastSync();
            syncColumnTelemetryDisplays();
            if (level != null && !level.isClientSide) {
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("反应堆 {} 的停机拆除回滚失败", worldPosition, exception);
        }
    }

    /** 在所有者计划已提交且原方块仍存在时发布一次既有危险事件占位。 */
    public boolean publishDangerousDisassembly(ReactorDisassemblyPlan plan) {
        if (plan == null || plan.owner() != this
                || plan.action() != ReactorDisassemblyPlan.Action.DANGEROUS
                || !plan.publishDangerousEvent()) {
            return false;
        }
        return ReactorMeltdownEvents.publish(
                ReactorMeltdownEvent.Reason.DANGEROUS_DISASSEMBLY,
                level,
                structureOrigin,
                worldPosition,
                plan.nextSnapshot()
        );
    }

    /**
     * 保留 P1-MAINT-01 的直接危险提交兼容入口；真实 BreakEvent 使用上面的多所有者事务。
     */
    public boolean tryCommitDangerousDisassembly(BlockPos brokenPos, BlockState brokenState) {
        ReactorDisassemblyPlan plan = prepareDisassemblyPlan(brokenPos, brokenState);
        if (plan == null || plan.action() != ReactorDisassemblyPlan.Action.DANGEROUS) {
            return false;
        }
        try {
            if (!commitDisassemblyPlan(plan)
                    || !verifyDisassemblyPlan(plan)
                    || !plan.publishDangerousEvent()
                    || !publishDangerousDisassembly(plan)) {
                rollbackDisassemblyPlan(plan);
                return false;
            }
            return true;
        } catch (RuntimeException exception) {
            rollbackDisassemblyPlan(plan);
            LOGGER.warn("反应堆 {} 的危险拆除兼容提交失败", worldPosition, exception);
            return false;
        }
    }

    /** 只推进控制棒执行器状态，不执行完整热、冷却和损伤模拟。 */
    public boolean tickControlRods() {
        if (level != null && level.isClientSide) {
            return false;
        }
        if (!structureValid()) {
            return false;
        }
        if(com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.pending(this)
                ||automationInactiveLoadPending||com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.normalShutdownActive(this))return false;
        ReactorSnapshot next = ReactorControlRodTick.advance(snapshot);
        if (next == snapshot) {
            return false;
        }
        setSnapshot(next);
        return true;
    }

    /** 执行一个服务端 tick 的完整 P1 反应堆权威循环。 */
    public boolean tickReactor() {
        boolean previousSettlementScope = runtimeSettlementInProgress;
        runtimeSettlementInProgress = true;
        try {
            if(com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.normalShutdownActive(this))return false;
            if(automationInactiveLoadPending&&!finishInactiveAutomationLoad())return false;
            if(com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.pending(this)) {
                com.iksxh.create_nuclear_industry.automation.ReactorAutomationParameters resumeParameters;
                try {resumeParameters=com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.parameters();}
                catch(IllegalArgumentException invalidConfig) {com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.fail(this,"invalid_value");return false;}
                boolean fuelReady=structureValid()&&snapshotMatchesStructure()&&hydrateFuelProjectionsFromPorts();
                // 交接未完成则整条棒推进/燃耗/热工/维护账本冻结，不能用旧ARMING基准把真实棒位改成100%。
                if(!com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.allowSettlement(this,resumeParameters,level.getGameTime(),fuelReady))return false;
            }
            if (level == null || level.isClientSide || !structureValid()
                    || !snapshotMatchesStructure()) {
                invalidateRuntimeDescriptor();
                com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.onTickFailure(this, level == null ? 0 : level.getGameTime());
                return false;
            }
            if (!hydrateFuelProjectionsFromPorts()) {
                invalidateRuntimeDescriptor();
                com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.onTickFailure(this, level == null ? 0 : level.getGameTime());
                return false;
            }
            com.iksxh.create_nuclear_industry.automation.ReactorAutomationParameters automationParameters = null;
            try { automationParameters = com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.parameters(); }
            catch (IllegalArgumentException invalidConfig) {
                // 非法管理员配置只能降权，不能阻断未任职堆原热工结算或默默提棒。
                com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.onTickFailure(this, level.getGameTime());
            }
            // 输入投影完成后处理安全交接；管理员先一次提交目标，再运行原控制/裂变/冷却顺序。
            if (automationParameters != null) com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.beforeReactorTick(this, automationParameters, level.getGameTime());
            Map<CoreColumnPosition, ItemStack> beforePortItems = captureFuelPortItems();
            if (beforePortItems == null) {
                invalidateRuntimeDescriptor();
                com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.onTickFailure(this, level == null ? 0 : level.getGameTime());
                return false;
            }
            ReactorSimulationParameters parameters = simulationParameters();
            ReactorServerTick.CoolantInput tickCoolantInput = coolantInput();
            boolean resumingFirstSettlement=com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.firstSettlement(this);
            ReactorServerTick.Result result = ReactorServerTick.advance(
                    snapshot,
                    parameters,
                    tickCoolantInput,
                    com.iksxh.create_nuclear_industry.automation.ReactorAutomationHeatSupply.requestedHeat(this,level.getGameTime())
            );
            Map<CoreColumnPosition, ItemStack> nextPortItems = prepareFuelPortCommit(
                    beforePortItems, result.snapshot(), result.fission().columns());
            if (nextPortItems == null) {
                invalidateRuntimeDescriptor();
                com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.onTickFailure(this, level == null ? 0 : level.getGameTime());
                return false;
            }
            // 只有从未完成状态跨入 COMPLETE 才允许发布；持久化标记覆盖重复 tick 和重载。
            boolean enteredComplete = !snapshot.meltdownEventPublished()
                    && snapshot.meltdownProgressTicks() < parameters.meltdownCountdownTicks()
                    && result.meltdown().status() == MeltdownStatus.COMPLETE;
            ReactorSnapshot nextSnapshot = enteredComplete
                    ? result.snapshot().withMeltdownEventPublished(true)
                    : result.snapshot();
            boolean stateChanged = !nextSnapshot.equals(snapshot);
            boolean portChanged = !sameFuelPortItems(beforePortItems, nextPortItems);
            for (Map.Entry<CoreColumnPosition, ItemStack> entry : nextPortItems.entrySet()) {
                ReactorPortBlockEntity port = findRefuelingPort(entry.getKey());
                if (port != null) {
                    port.setFuelAssembly(entry.getValue());
                }
            }
            if (stateChanged) {
                setSnapshot(nextSnapshot);
            }
            if (enteredComplete) {
                ReactorMeltdownEvents.publish(
                        ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE,
                        level,
                        structureOrigin,
                        worldPosition,
                        nextSnapshot
                );
            }
            // 提交和事件保持原顺序；只有全部正式阶段完成才冻结同龄样本，write 永远不拼装字段。
            boolean runtimeRecovered = runtimeDescriptor == null || !runtimeDescriptor.available();
            try {
                ReactorRuntimeDescriptor nextRuntime = ReactorRuntimeDescriptorFactory.project(surfaceDescriptor, structureScan, result,
                        tickCoolantInput, runtimeRevision + 1, runtimeSample + 1, Math.max(0, level.getGameTime()));
                runtimeDescriptor = nextRuntime;
                runtimeRevision = nextRuntime.revision();
                runtimeSample = nextRuntime.sample();
                runtimeTicksSinceLastSync++;
            } catch (RuntimeException displayFailure) {
                // 协议预算或投影缺口只撤销显示，不能把已提交正式 tick 变成玩法失败。
                invalidateRuntimeDescriptor();
            }
            if(resumingFirstSettlement) {
                // 首次恢复的当前duty只能由已提交的真实维护付款证明；不让after保护读到尚未出票的冷燃烧室。
                com.iksxh.create_nuclear_industry.automation.ReactorAutomationHeatSupply.commitPayment(this,result,level.getGameTime());
                com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.finishPayment(this,result);
            }
            if (automationParameters != null) com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.afterReactorTick(this, result, automationParameters, level.getGameTime());
            publishTelemetry(ReactorInstrumentTelemetry.from(result));
            // 原遥测包已经携带新完整样本时无需追加；零变化成功 tick 仍按五成功 tick 续租。
            if (runtimeTicksSinceLastSync > 0 && (runtimeRecovered || runtimeTicksSinceLastSync >= 5)) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                runtimeTicksSinceLastSync = 0;
            }
            com.iksxh.create_nuclear_industry.automation.ReactorAutomationHeatSupply.commitPayment(this,result,level.getGameTime());
            if(resumingFirstSettlement)com.iksxh.create_nuclear_industry.automation.ReactorAutomationBlazeBridge.notifyPermission(this,automationBinding==null?null:automationBinding.burnerPos());
            return stateChanged || portChanged;
        } catch (RuntimeException exception) {
            invalidateRuntimeDescriptor();
            com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.onTickFailure(this, level == null ? 0 : level.getGameTime());
            throw exception;
        } finally {
            runtimeSettlementInProgress = previousSettlementScope;
        }
    }

    /** 已保存STOP/FAULT仅等待当前首扫和燃料投影，不授许可、不结算；真实失败或现行初始化窗口届满仍保护。 */
    private boolean finishInactiveAutomationLoad() {
        if(level==null||level.isClientSide)return false;
        long time=level.getGameTime();if(automationInactiveLoadStarted==Long.MIN_VALUE)automationInactiveLoadStarted=time;
        String failure="";int timeout;
        try {timeout=com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.parameters().armingTimeoutTicks();}
        catch(IllegalArgumentException invalidConfig) {failure="invalid_value";timeout=0;}
        if(failure.isEmpty()&&(time<automationInactiveLoadStarted||time-automationInactiveLoadStarted>=timeout))failure="arming_timeout";
        if(failure.isEmpty()&&!structureValid()&&structureScanCount>0
                &&structureScan.diagnosticCode()!=ReactorStructureDefinition.DiagnosticCode.CHUNKS_UNAVAILABLE)failure="structure_changed";
        if(!failure.isEmpty()) {
            automationInactiveLoadPending=false;
            com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.fail(this,failure);return false;
        }
        if(!structureValid()||!snapshotMatchesStructure()||!hydrateFuelProjectionsFromPorts())return false;
        automationInactiveLoadPending=false;return true;
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
            runtimeTicksSinceLastSync = 0;
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

    /** 结构编辑后，列状态可能残留；实际尺寸或列角色不匹配缓存时暂不推进模拟。 */
    private boolean snapshotMatchesStructure() {
        return snapshot.dimensions().equals(structureScan.dimensions())
                && snapshot.fuelColumns().keySet().stream().allMatch(position ->
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
                coolantCapacityMb(),
                Math.max(1.0E-12D, P1ServerConfig.VALUES.coolantAbsorptionHuPerMb.get())
        );
    }

    private static ReactorSimulationParameters simulationParameters() {
        P1ServerConfig.DamageMultipliers damageMultipliers = P1ServerConfig.damageMultipliers();
        return new ReactorSimulationParameters(
                P1ServerConfig.VALUES.baseHeatPerFuelBlockHuPerTick.get(),
                P1ServerConfig.VALUES.fuelBurnTimeHours.get(),
                P1ServerConfig.VALUES.damageHeatThresholdHuPerTick.get(),
                P1ServerConfig.VALUES.damageRatePerTickHuLoad.get(),
                damageMultipliers.heatMultiplier(),
                damageMultipliers.burnMultiplier(),
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

    /** 构造包含回滚所需运行时副本的不可变预检计划。 */
    private ReactorDisassemblyPlan createDisassemblyPlan(
            ReactorDisassemblyPlan.Action action,
            BlockPos brokenPos,
            BlockState brokenState,
            String expectedId,
            ReactorSnapshot beforeSnapshot,
            ReactorSnapshot projectedSnapshot,
            ReactorSnapshot nextSnapshot,
            ReactorFullShutdownResult assessment,
            List<ReactorDisassemblyPlan.PortCapture> fuelPorts,
            Map<CoreColumnPosition, FuelAssemblyState> pendingFuel,
            boolean publishDangerousEvent
    ) {
        return new ReactorDisassemblyPlan(
                action,
                this,
                brokenPos,
                brokenState,
                expectedId,
                structureOrigin,
                beforeSnapshot,
                projectedSnapshot,
                nextSnapshot,
                assessment,
                fuelPorts,
                pendingFuel,
                telemetry,
                lastSentTelemetry,
                telemetryTicksSinceLastSync,
                publishDangerousEvent
        );
    }

    /** 构造不会携带任何可提交状态的拒绝计划，供生命周期统一取消原事件。 */
    private ReactorDisassemblyPlan rejectedDisassemblyPlan(
            BlockPos brokenPos,
            BlockState brokenState,
            ReactorFullShutdownResult assessment
    ) {
        return createDisassemblyPlan(
                ReactorDisassemblyPlan.Action.REJECTED,
                brokenPos,
                brokenState,
                blockId(brokenState),
                snapshot,
                snapshot,
                snapshot,
                assessment,
                List.of(),
                pendingLegacyFuelAssemblies,
                false
        );
    }

    /**
     * 按当前有效结构映射逐一读取燃料列顶部端口，不依赖可能缺项的绑定集合。
     *
     * @return 每根燃料列的端口和精确物品副本；任一端口无法验证时返回 {@code null}
     */
    private List<ReactorDisassemblyPlan.PortCapture> captureFuelPortsForReset() {
        if (level == null || level.isClientSide || !structureScan.valid()
                || structureOrigin == null) {
            return null;
        }
        List<ReactorDisassemblyPlan.PortCapture> captures = new ArrayList<>();
        for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                : structureScan.columns().entrySet()) {
            if (entry.getValue().type() != ReactorStructureDefinition.ColumnType.FUEL) {
                continue;
            }
            CoreColumnPosition column = entry.getKey();
            BlockPos portPos = absolutePosition(entry.getValue().capPosition());
            if (!(level.getBlockEntity(portPos) instanceof ReactorPortBlockEntity port)
                    || level.getBlockEntity(portPos) != port
                    || !port.isBoundTo(this, ReactorPortBlockEntity.BindingType.REFUELING, column)) {
                return null;
            }
            ItemStack stored = port.fuelAssembly();
            if (!FuelAssemblyItemCodec.isValidStoredFuel(stored)) {
                return null;
            }
            captures.add(new ReactorDisassemblyPlan.PortCapture(column, port, stored));
        }
        return List.copyOf(captures);
    }

    /** 用读阶段捕获的精确端口物品创建当前燃料投影，不写入快照或端口。 */
    private ReactorSnapshot projectFuelSnapshot(
            List<ReactorDisassemblyPlan.PortCapture> fuelPorts
    ) {
        TreeMap<CoreColumnPosition, FuelColumnState> nextFuelColumns =
                new TreeMap<>(snapshot.fuelColumns());
        for (ReactorDisassemblyPlan.PortCapture capture : fuelPorts) {
            FuelAssemblyState projectedFuel = FuelAssemblyItemCodec.simulationState(
                    capture.fuelAssembly());
            FuelColumnState current = nextFuelColumns.get(capture.column());
            if (current != null) {
                nextFuelColumns.put(
                        capture.column(), current.withFuelAssemblyProjection(projectedFuel));
            } else if (projectedFuel.present()) {
                nextFuelColumns.put(
                        capture.column(), FuelColumnState.empty()
                                .withFuelAssemblyProjection(projectedFuel));
            }
        }
        return snapshot.withColumns(nextFuelColumns, snapshot.controlRodColumns());
    }

    /**
     * 在事务提交前再次确认原方块、缓存结构、所有权和完全停机证明没有变化。
     * 这是服务端单线程事件中的最后安全门，也是多所有者提交的共同前置条件。
     */
    private boolean validateDisassemblyPlan(ReactorDisassemblyPlan plan) {
        if (!(level instanceof ServerLevel) || level.isClientSide || isRemoved()
                || level.getBlockEntity(worldPosition) != this
                || !structureScan.valid() || structureOrigin == null
                || !Objects.equals(structureOrigin, plan.structureOrigin())
                || !plan.expectedComponentId().equals(cachedComponentId(plan.brokenPos()))
                || !plan.expectedComponentId().equals(blockId(plan.brokenState()))
                || !plan.expectedComponentId().equals(blockId(level.getBlockState(plan.brokenPos())))
                || !plan.beforeSnapshot().equals(snapshot)) {
            return false;
        }

        if (plan.action() == ReactorDisassemblyPlan.Action.REJECTED) {
            return false;
        }
        List<ReactorDisassemblyPlan.PortCapture> currentFuelPorts = captureFuelPortsForReset();
        if (currentFuelPorts == null
                || !sameFuelPortCaptures(plan.fuelPorts(), currentFuelPorts)
                || !pendingLegacyFuelAssemblies.equals(plan.pendingLegacyFuelAssemblies())) {
            return false;
        }
        ReactorSnapshot currentProjected;
        ReactorFullShutdownResult current;
        try {
            currentProjected = projectFuelSnapshot(currentFuelPorts);
            current = ReactorFullShutdownAssessment.assess(
                    currentProjected, simulationParameters());
        } catch (RuntimeException exception) {
            return false;
        }
        if (!current.inputValid()
                || !currentProjected.equals(plan.projectedSnapshot())
                || !current.equals(plan.shutdownAssessment())) {
            return false;
        }
        boolean dangerous = current.generatedHeatHu() > SAFETY_HEAT_EPSILON
                || currentProjected.meltdownCountdownStarted();
        if (plan.action() == ReactorDisassemblyPlan.Action.DANGEROUS) {
            if (!dangerous) {
                return false;
            }
        } else if (dangerous || !current.fullyStopped()) {
            return false;
        }
        return true;
    }

    /** 比较读阶段与当前结构的端口实体、列坐标及完整物品组件。 */
    private boolean sameFuelPortCaptures(
            List<ReactorDisassemblyPlan.PortCapture> expected,
            List<ReactorDisassemblyPlan.PortCapture> actual
    ) {
        if (expected == null || actual == null || expected.size() != actual.size()) {
            return false;
        }
        for (int index = 0; index < expected.size(); index++) {
            ReactorDisassemblyPlan.PortCapture first = expected.get(index);
            ReactorDisassemblyPlan.PortCapture second = actual.get(index);
            if (!first.column().equals(second.column())
                    || first.port() != second.port()
                    || !ItemStack.matches(first.fuelAssembly(), second.fuelAssembly())) {
                return false;
            }
        }
        return true;
    }

    /** 检查安全重置已清除快照、迁移信封、所有端口物品和运行时遥测。 */
    private boolean verifyFullShutdownReset(ReactorDisassemblyPlan plan) {
        if (!snapshot.equals(plan.nextSnapshot())
                || !pendingLegacyFuelAssemblies.isEmpty()
                || telemetry.available()) {
            return false;
        }
        for (ReactorDisassemblyPlan.PortCapture capture : plan.fuelPorts()) {
            if (!capture.port().fuelAssembly().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 根据最近一次有效结构缓存确认待破坏方块是八类固定组件之一。 */
    private String cachedComponentId(BlockPos brokenPos) {
        int localX = brokenPos.getX() - structureOrigin.getX();
        int localY = brokenPos.getY() - structureOrigin.getY();
        int localZ = brokenPos.getZ() - structureOrigin.getZ();
        ReactorStructureDefinition.LocalPosition local =
                new ReactorStructureDefinition.LocalPosition(localX, localY, localZ);
        if (!structureScan.dimensions().contains(local)) {
            return null;
        }

        if (structureScan.dimensions().boundary(local) && surfaceDescriptor != null && surfaceDescriptor.valid()) {
            // 使用同次成型冻结的表面ID，合法窗口不局限于原五格示例位置。
            for (var member : surfaceDescriptor.members()) {
                if (member.pos().equals(brokenPos)) return member.expectedBlockId();
            }
        }
        for (Map.Entry<ReactorStructureDefinition.PortType, List<ReactorStructureDefinition.LocalPosition>> entry
                : structureScan.ports().entrySet()) {
            if (entry.getValue().contains(local)) {
                return switch (entry.getKey()) {
                    case INSTRUMENT -> id(P1ContentIds.REACTOR_INSTRUMENT_PORT_ID);
                    case COLD_COOLANT -> id(P1ContentIds.REACTOR_COLD_PORT_ID);
                    case HOT_COOLANT -> id(P1ContentIds.REACTOR_HOT_PORT_ID);
                    case AUTOMATION -> id("reactor_automation_port");
                };
            }
        }
        for (ReactorStructureDefinition.ColumnMapping column : structureScan.columns().values()) {
            if (column.bodyPositions().contains(local)) {
                return column.type() == ReactorStructureDefinition.ColumnType.FUEL
                        ? id(P1ContentIds.REACTOR_FUEL_ROD_ID) : null;
            }
            if (column.capPosition().equals(local)) {
                return switch (column.type()) {
                    case FUEL -> id(P1ContentIds.REACTOR_REFUELING_PORT_ID);
                    case CONTROL_ROD -> id(P1ContentIds.CONTROL_ROD_DRIVE_ID);
                    case EMPTY -> id(P1ContentIds.REACTOR_CASING_ID);
                };
            }
        }
        return structureScan.dimensions().boundary(local) ? id(P1ContentIds.REACTOR_CASING_ID) : null;
    }

    private static String blockId(BlockState state) {
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? "" : key.toString();
    }

    private static String id(String path) {
        return "create_nuclear_industry:" + path;
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
            BlockPos drivePos = structureOrigin.offset(
                    entry.getValue().capPosition().x(),
                    entry.getValue().capPosition().y(),
                    entry.getValue().capPosition().z());
            if (level.getBlockEntity(drivePos) instanceof ControlRodDriveBlockEntity drive) {
                drive.setServerAutomationLocked(automationState.rodsLocked());
                drive.setServerColumnHint(entry.getKey().x(), entry.getKey().z());
                if (state == null) {
                    drive.setServerDisplayedDepthPercent(100);
                    drive.setServerControlRodIntegrity(null);
                } else {
                    drive.setServerDisplayedDepthPercent(toPercent(state.targetDepth()));
                }
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
        refuelingPortsByColumn.clear();
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
                if (entry.getValue().type() == ReactorPortBlockEntity.BindingType.REFUELING) {
                    refuelingPortsByColumn.put(entry.getValue().column(), port);
                }
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

    /** 按列读取服务端绑定索引；保留结构、职责、归属及当前BE身份校验，不加载缺失区块。 */
    private ReactorPortBlockEntity findRefuelingPort(CoreColumnPosition position) {
        if (position == null || level == null || level.isClientSide) {
            return null;
        }
        ReactorPortBlockEntity port = refuelingPortsByColumn.get(position);
        if (port == null || port.bindingType() != ReactorPortBlockEntity.BindingType.REFUELING
                || !port.isBoundTo(this, ReactorPortBlockEntity.BindingType.REFUELING, position)
                || level.getBlockEntity(port.getBlockPos()) != port) {
            return null;
        }
        return port;
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
            ReactorSnapshot nextSnapshot,
            Map<CoreColumnPosition, FuelColumnFissionResult> fissionColumns
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
                        : FuelAssemblyItemCodec.createCooledSpentFuel(beforeItem);
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
            // 正燃耗包含不足1耐久的小数；只准备副本，任一端口拒绝则整组不提交。
            FuelColumnFissionResult fission = fissionColumns.get(position);
            if (!nextItem.isEmpty() && fission != null)
                nextItem = com.iksxh.create_nuclear_industry.radiation.FuelUseHistory.prepareBurnCopy(
                        nextItem, fission.plannedFuelBurnUnits());
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
            if (level.hasChunkAt(drivePos) && level.getBlockEntity(drivePos) instanceof ControlRodDriveBlockEntity drive) {
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
        refuelingPortsByColumn.clear();
        for (ReactorPortBlockEntity port : List.copyOf(boundPorts)) {
            port.clearBinding(this, structureInvalidated);
        }
        boundPorts.clear();
    }

    /** 从反向端口生命周期中移除已卸载端口，不改变其他端口或反应堆快照。 */
    void detachPort(ReactorPortBlockEntity port) {
        if (port != null) {
            boundPorts.remove(port);
            // clearBinding先清空端口自身绑定，再回调本方法；按实例移除，旧BE不能撤销同坐标新BE。
            refuelingPortsByColumn.values().removeIf(boundPort -> boundPort == port);
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
        surfaceChunkUnloaded = false;
        ReactorSurfaceSyncEvents.publish(this, false);
        if (level != null && !level.isClientSide) {
            ReactorStructureLifecycle.registerOwner(this);
            ReactorStructureLifecycle.scheduleRescanAround(level, worldPosition);
        }
    }

    /** 客户端清理区块先调用此钩子再 setRemoved；显式保留卸载原因，不依赖事件总线次序。 */
    @Override
    public void onChunkUnloaded() {
        if(!com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.normalShutdownActive(this))automationInactiveLoadPending=false;
        com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.onTickFailure(this, level == null ? 0 : level.getGameTime());
        ReactorStructureLifecycle.unregisterOwner(this);
        invalidateRuntimeDescriptor();
        surfaceChunkUnloaded = true;
        ReactorSurfaceSyncEvents.publish(this, true, true);
        super.onChunkUnloaded();
    }

    /**
     * 反应堆仪表端口被移除或区块卸载时，立即撤销所有端口的运行时所有权。
     *
     * <p>{@link P1MinimalBlockEntity} 的 Create 生命周期会在这里之后继续卸载 behaviour；
     * 快照本身不清空，只有运行时绑定和结构缓存失效，确保旧 handler 不能复活到新所有者。</p>
     */
    @Override
    public void invalidate() {
        if(!com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.normalShutdownActive(this))automationInactiveLoadPending=false;
        com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.onTickFailure(this, level == null ? 0 : level.getGameTime());
        ReactorStructureLifecycle.unregisterOwner(this);
        invalidateRuntimeDescriptor();
        ReactorSurfaceSyncEvents.publish(this, true, surfaceChunkUnloaded);
        // 区块卸载不是安全重扫，不能借生命周期事件改变未成型取料锁。
        clearPortBindings(false);
        clearControlRodTelemetry();
        structureScan = ReactorStructureDefinition.ScanResult.notScanned();
        structureOrigin = null;
        if (level != null && !level.isClientSide && surfaceDescriptor != null && surfaceDescriptor.valid()) {
            surfaceDescriptor = ReactorSurfaceDescriptor.unavailable(level.dimension().location().toString(),
                    worldPosition, surfaceGeneration, surfaceDescriptor.revision() + 1);
        }
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
        tooltip.add(GoggleTooltip.indentFirstLine(Component.translatable(GOGGLE_KEY_PREFIX + "structure_summary")));
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
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "empty_columns",
                clientStructureSummary.emptyColumnCount()));
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "coolant_space_blocks",
                clientStructureSummary.coolantSpaceBlockCount()));
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "capacity_per_space_block",
                clientStructureSummary.coolantCapacityPerEmptyBlockMb()));
        tooltip.add(Component.translatable(GOGGLE_KEY_PREFIX + "fluid_capacity",
                clientStructureSummary.coolantCapacityMb()));
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
        tag.putBoolean("AutomationClaimed", automationState.claimed());
        tag.putLong("AutomationEpoch", automationState.authorityEpoch());
        if(!clientPacket)tag.put(com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.KEY,com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.checkpointForSave(this));
        tag.put(SNAPSHOT_KEY, snapshotTag);
        if (!clientPacket && breakSafetySurface != null) {
            tag.put("BreakSafetySurface", breakSafetySurface.encode());
            var fuelColumns = new net.minecraft.nbt.ListTag();
            for (var column : breakSafetyFuelColumns) {
                var entry = new CompoundTag();
                entry.putInt("X", column.x()); entry.putInt("Z", column.z());
                fuelColumns.add(entry);
            }
            tag.put("BreakSafetyFuelColumns", fuelColumns);
        }
        if (clientPacket) {
            tag.put(STRUCTURE_SUMMARY_KEY, structureSummary().writeSyncTag());
            tag.put(TELEMETRY_KEY, ReactorInstrumentTelemetryNbtCodec.encode(telemetry));
            if (surfaceDescriptor != null) tag.put(SURFACE_KEY, surfaceDescriptor.encode());
            if (runtimeDescriptor != null) tag.put(RUNTIME_KEY, runtimeDescriptor.encode());
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
        if (!clientPacket) {
            setAutomationLoadFollowSession(null);
            com.iksxh.create_nuclear_industry.automation.ReactorAutomationHeatSupply.invalidate(this);
            // 当前正常停服只恢复原意图；旧许可/样本/热票据不恢复，实际棒位由完整核快照保持。
            var loaded=com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.decode(
                    tag.contains(com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.KEY)?tag.getCompound(com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume.KEY):null,
                    tag.getBoolean("AutomationClaimed"),snapshot);
            automationState=loaded.state();automationResumeSession=loaded.session();automationSavedScramAwaitingHigh=loaded.savedScram();automationNormalStopCheckpoint=null;
            automationInactiveLoadPending=loaded.state().claimed()&&loaded.session()==null;automationInactiveLoadStarted=Long.MIN_VALUE;
            automationGeneration = java.util.UUID.randomUUID();
            automationBinding = null; automationObservation = null; automationBurner = null; automationPort = null; automationCurve.clear(); automationHeatWindow.clear();
            automationTickTime=Long.MIN_VALUE;
            breakSafetySurface = ReactorSurfaceDescriptor.decode(tag.contains("BreakSafetySurface")
                    ? tag.getCompound("BreakSafetySurface") : null).orElse(null);
            breakSafetyAwaitingRestoreScan = breakSafetySurface != null;
            var protectedColumns = new java.util.HashSet<CoreColumnPosition>();
            var list = tag.getList("BreakSafetyFuelColumns", net.minecraft.nbt.Tag.TAG_COMPOUND);
            // 原拆除防护仍按既有容错读；04续接单独要求完整、类型正确、无重复且属于同尺寸的本版本证据。
            boolean layoutValid=breakSafetySurface!=null&&breakSafetySurface.valid()
                    &&tag.get("BreakSafetyFuelColumns") instanceof net.minecraft.nbt.ListTag rawLayout
                    &&(rawLayout.isEmpty()||rawLayout.getElementType()==net.minecraft.nbt.Tag.TAG_COMPOUND)
                    &&list.size()<=snapshot.dimensions().columnCount()
                    &&breakSafetySurface.maxInclusive().getX()-breakSafetySurface.origin().getX()+1==snapshot.dimensions().width()
                    &&breakSafetySurface.maxInclusive().getY()-breakSafetySurface.origin().getY()+1==snapshot.dimensions().height()
                    &&breakSafetySurface.maxInclusive().getZ()-breakSafetySurface.origin().getZ()+1==snapshot.dimensions().depth();
            if (list.size() <= snapshot.dimensions().columnCount()) {
                for (int index = 0; index < list.size(); index++) {
                    var entry = list.getCompound(index);
                    int x = entry.getInt("X"), z = entry.getInt("Z");
                    boolean inside=x>=0&&x<snapshot.dimensions().internalWidth()&&z>=0&&z<snapshot.dimensions().internalDepth();
                    if(!inside||!entry.contains("X",net.minecraft.nbt.Tag.TAG_INT)||!entry.contains("Z",net.minecraft.nbt.Tag.TAG_INT)
                            ||!entry.getAllKeys().equals(java.util.Set.of("X","Z")))layoutValid=false;
                    if(inside&&!protectedColumns.add(new CoreColumnPosition(x,z)))layoutValid=false;
                }
            }
            breakSafetyFuelColumns = java.util.Set.copyOf(protectedColumns);
            automationLoadedFuelLayout=layoutValid?java.util.Set.copyOf(protectedColumns):null;
        }
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
            surfaceDescriptor = ReactorSurfaceDescriptor.decode(
                    tag.contains(SURFACE_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)
                            ? tag.getCompound(SURFACE_KEY) : null).orElse(null);
            runtimeDescriptor = ReactorRuntimeDescriptor.decode(
                    tag.contains(RUNTIME_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)
                            ? tag.getCompound(RUNTIME_KEY) : null).orElse(null);
            ReactorSurfaceSyncEvents.publish(this, false);
        } else {
            // 服务端加载仅恢复权威状态，显示需等待本实例新的成功结算。
            runtimeDescriptor = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put(SNAPSHOT_KEY, ReactorSnapshotNbtCodec.encode(snapshot));
        tag.put(STRUCTURE_SUMMARY_KEY, structureSummary().writeSyncTag());
        tag.put(TELEMETRY_KEY, ReactorInstrumentTelemetryNbtCodec.encode(telemetry));
        if (surfaceDescriptor != null) tag.put(SURFACE_KEY, surfaceDescriptor.encode());
        if (runtimeDescriptor != null) tag.put(RUNTIME_KEY, runtimeDescriptor.encode());
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
