package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnRepairTransaction;
import com.iksxh.create_nuclear_industry.reactor.FuelRefuelingTransaction;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentGoggleDisplay;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentTelemetry;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;

/** 冷、热和补料端口共用的空壳方块实体；不拥有任何反应堆模拟状态。 */
public final class ReactorPortBlockEntity extends P1MinimalBlockEntity
        implements IHaveGoggleInformation {
    private static final String FUEL_COLUMN_BOUND_KEY = "FuelColumnBound";
    private static final String FUEL_COLUMN_POSITION_X_KEY = "FuelColumnPositionX";
    private static final String FUEL_COLUMN_POSITION_Z_KEY = "FuelColumnPositionZ";
    private static final String FUEL_COLUMN_INTEGRITY_KEY = "FuelColumnIntegrity";
    private static final String FUEL_COLUMN_HEAT_KEY = "FuelColumnHeatHuPerTick";
    private static final String FUEL_ASSEMBLY_KEY = "FuelAssembly";
    private static final String UNFORMED_EXTRACTION_ALLOWED_KEY = "UnformedExtractionAllowed";
    private Binding binding;
    private ReactorInstrumentPortBlockEntity boundOwner;
    private ItemStack fuelAssembly = ItemStack.EMPTY;
    private boolean unformedExtractionAllowed;
    private boolean serverFuelColumnBound;
    private boolean clientFuelColumnBound;
    private ReactorInstrumentTelemetry.FuelColumnTelemetry serverFuelColumnTelemetry;
    private ReactorInstrumentTelemetry.FuelColumnTelemetry clientFuelColumnTelemetry;

    public ReactorPortBlockEntity(BlockPos pos, BlockState state) {
        super(P1BlockEntities.REACTOR_PORT.get(), pos, state);
    }

    /** 返回该端口当前的服务端绑定；绑定缓存不属于反应堆持久化状态。 */
    public Binding binding() {
        return binding;
    }

    /** 返回端口是否已经绑定到一个有效的仪表端口。 */
    public boolean isBound() {
        return binding != null && boundOwner != null && !isRemoved()
                && !boundOwner.isRemoved() && boundOwner.structureValid();
    }

    /** 返回客户端最近一次服务端同步的换料列绑定标记，供 Create 机械臂选点预检使用。 */
    public boolean isClientFuelColumnBound() {
        return clientFuelColumnBound;
    }

    /** 返回共享全堆账本或单列状态的唯一权威所有者。 */
    public ReactorInstrumentPortBlockEntity boundOwner() {
        return boundOwner;
    }

    /** 返回端口唯一持有的完整燃料物品栈；调用方获得副本，不得直接修改端口状态。 */
    public ItemStack fuelAssembly() {
        return fuelAssembly.copy();
    }

    /**
     * 返回未成型人工取料许可；该布尔值只表示访问权限，不是热量、完整度或模拟状态副本。
     */
    public boolean isUnformedExtractionAllowed() {
        return unformedExtractionAllowed;
    }

    /**
     * 替换端口保存的单件燃料物品栈。
     *
     * <p>端口只接受空栈、正式新燃料或冷却乏燃料，且始终固定为一件；所有原版耐久和
     * 数据组件随栈一起保存。该入口不注册 ItemHandler，因此普通漏斗不能绕过换料事务。</p>
     */
    public void setFuelAssembly(ItemStack nextFuelAssembly) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("fuel assembly can only be changed on the server");
        }
        ItemStack normalized = nextFuelAssembly == null
                ? ItemStack.EMPTY : nextFuelAssembly.copy();
        if (!FuelAssemblyItemCodec.isValidStoredFuel(normalized)) {
            throw new IllegalArgumentException("refueling port accepts one valid fuel assembly");
        }
        if (ItemStack.matches(fuelAssembly, normalized)) {
            return;
        }
        fuelAssembly = normalized;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** 返回换料端口绑定的燃料列；冷/热端口使用全堆账本时返回 {@code null}。 */
    public CoreColumnPosition boundColumn() {
        return binding == null ? null : binding.column();
    }

    /** 返回该方块状态对应的端口职责，非正式端口状态返回 {@code null}。 */
    public BindingType bindingType() {
        if (getBlockState().is(P1Blocks.REACTOR_COLD_PORT.get())) {
            return BindingType.COLD_COOLANT;
        }
        if (getBlockState().is(P1Blocks.REACTOR_HOT_PORT.get())) {
            return BindingType.HOT_COOLANT;
        }
        if (getBlockState().is(P1Blocks.REACTOR_REFUELING_PORT.get())) {
            return BindingType.REFUELING;
        }
        return null;
    }

    /** 判断端口是否绑定到指定仪表端口和列，避免调用方按世界坐标自行推导所有权。 */
    public boolean isBoundTo(
            ReactorInstrumentPortBlockEntity owner,
            BindingType expectedType,
            CoreColumnPosition expectedColumn
    ) {
        return isBound() && boundOwner == owner && binding.type() == expectedType
                && java.util.Objects.equals(binding.column(), expectedColumn);
    }

    /** 返回客户端最近一次同步的单列燃料遥测；服务端只读显示不使用该副本。 */
    public ReactorInstrumentTelemetry.FuelColumnTelemetry clientFuelColumnTelemetry() {
        return clientFuelColumnTelemetry;
    }

    /**
     * 同步该换料端口所属燃料列的客户端显示数据。
     *
     * <p>只有仪表端口服务端权威循环可以调用；空遥测表示结构有效但尚未完成一次成功
     * 正式 tick，客户端应显示等待态。此数据仅进入客户端更新包，不写入端口持久化 NBT。</p>
     */
    void setServerFuelColumnTelemetry(
            ReactorInstrumentTelemetry.FuelColumnTelemetry telemetry
    ) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("fuel column telemetry can only be changed on the server");
        }
        boolean bound = binding != null
                && binding.type() == BindingType.REFUELING
                && binding.column() != null;
        if (serverFuelColumnBound == bound
                && Objects.equals(serverFuelColumnTelemetry, telemetry)) {
            return;
        }
        serverFuelColumnBound = bound;
        serverFuelColumnTelemetry = bound ? telemetry : null;
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    /**
     * 由仪表端口在服务端结构重扫后写入绑定缓存。
     * 冷/热端口的列为空，表示它们只能访问该仪表端口拥有的全堆冷却剂账本；
     * 换料端口必须提供一个燃料列坐标。绑定缓存不写入 NBT，区块加载后由结构重扫重建。
     */
    public boolean bindTo(
            ReactorInstrumentPortBlockEntity owner,
            BindingType expectedType,
            CoreColumnPosition column
    ) {
        if (owner == null || expectedType == null
                || (level != null && level.isClientSide)
                || !owner.structureValid()
                || bindingType() != expectedType
                || (expectedType == BindingType.REFUELING) != (column != null)
                || (expectedType != BindingType.REFUELING && column != null)) {
            return false;
        }
        if (boundOwner != null && boundOwner != owner && !canReclaimBinding()) {
            return false;
        }
        ReactorInstrumentPortBlockEntity previousOwner = boundOwner;
        Binding previousBinding = binding;
        boolean changed = !isBoundTo(owner, expectedType, column);
        binding = new Binding(owner.getBlockPos(), expectedType, column);
        boundOwner = owner;
        if (expectedType == BindingType.REFUELING) {
            setUnformedExtractionAllowed(owner.isSafeForUnformedFuelExtraction());
        }
        if (previousOwner != null && previousOwner != owner) {
            previousOwner.detachPort(this);
        }
        if (changed && level != null && !level.isClientSide) {
            invalidateCapabilityAndNetwork(previousBinding, expectedType);
        }
        return true;
    }

    /** 清除指定仪表端口留下的旧绑定，防止结构失效后继续暴露 capability。 */
    public void clearBinding(ReactorInstrumentPortBlockEntity owner) {
        clearBinding(owner, false);
    }

    /**
     * 清除运行时绑定，并在结构确实失效时根据旧仪表状态更新未成型取料锁。
     *
     * @param owner 触发解绑的仪表端口；为空时只执行保守清理
     * @param structureInvalidated 是否由服务端结构失效/重扫导致，而不是区块生命周期卸载
     */
    public void clearBinding(
            ReactorInstrumentPortBlockEntity owner,
            boolean structureInvalidated
    ) {
        if (owner == null || boundOwner == owner) {
            boolean changed = binding != null || boundOwner != null;
            Binding previousBinding = binding;
            ReactorInstrumentPortBlockEntity previousOwner = boundOwner;
            if (structureInvalidated && previousOwner != null
                    && previousBinding != null
                    && previousBinding.type() == BindingType.REFUELING) {
                setUnformedExtractionAllowed(previousOwner.isSafeForUnformedFuelExtraction());
            }
            binding = null;
            boundOwner = null;
            serverFuelColumnBound = false;
            serverFuelColumnTelemetry = null;
            if (previousOwner != null) {
                previousOwner.detachPort(this);
            }
            if (changed && level != null && !level.isClientSide) {
                invalidateCapabilityAndNetwork(previousBinding, null);
                sendData();
            }
        }
    }

    /** 读取仪表端口唯一权威快照，不创建端口本地副本。 */
    public ReactorSnapshot readAuthoritativeSnapshot(ReactorInstrumentPortBlockEntity owner) {
        if (owner == null) {
            throw new IllegalArgumentException("reactor instrument port is required");
        }
        return owner.snapshot();
    }

    /**
     * 只计算机械臂装料结果，不修改端口物品或反应堆快照。
     *
     * <p>Create 机械臂会先以 {@code simulate=true} 预检目标；该入口必须和正式提交使用同一
     * 服务端绑定、裂变发热和端口物品校验，避免预检成功后把无效状态暴露给机械臂。</p>
     */
    public FuelRefuelingTransaction.Result simulateInsertFuel(ItemStack incoming) {
        return evaluateInsertFuel(incoming).result();
    }

    /** 在服务端尝试向该端口绑定的空燃料列装入一个组件，并原子更新端口物品和投影。 */
    public FuelRefuelingTransaction.Result tryInsertFuel(ItemStack incoming) {
        FuelTransactionEvaluation evaluation = evaluateInsertFuel(incoming);
        FuelRefuelingTransaction.Result result = evaluation.result();
        if (!result.success()) {
            return result;
        }
        if (!evaluation.owner().validateFuelColumnCommit(
                this, evaluation.column(), evaluation.expectedStored(), result.nextColumn())) {
            return FuelRefuelingTransaction.invalidPort(incoming);
        }
        return commitFuelTransaction(
                evaluation,
                incoming,
                incoming.copyWithCount(1),
                result);
    }

    /** 只计算机械臂取料结果，不修改端口物品或反应堆快照。 */
    public FuelRefuelingTransaction.Result simulateExtractFuel() {
        return evaluateExtractFuel().result();
    }

    /** 在服务端尝试取出该端口绑定燃料列中的组件或冷却乏燃料。 */
    public FuelRefuelingTransaction.Result tryExtractFuel() {
        FuelTransactionEvaluation evaluation = evaluateExtractFuel();
        FuelRefuelingTransaction.Result result = evaluation.result();
        if (!result.success()) {
            return result;
        }
        if (!evaluation.owner().validateFuelColumnCommit(
                this, evaluation.column(), evaluation.expectedStored(), result.nextColumn())) {
            return FuelRefuelingTransaction.invalidPort(ItemStack.EMPTY);
        }
        return commitFuelTransaction(evaluation, ItemStack.EMPTY, ItemStack.EMPTY, result);
    }

    /** 只计算合金钢板维修结果，不修改端口物品或反应堆快照。 */
    public FuelColumnRepairTransaction.Result simulateRepairFuelColumn(ItemStack incoming) {
        return evaluateRepairFuelColumn(incoming).result();
    }

    /**
     * 在服务端原子提交一次燃料列维修。
     *
     * <p>先锁定端口、绑定、输入栈和瞬态列投影，再校验目标列当前没有新生裂变热；
     * 完整度更新只写回仪表端口唯一快照，钢板余量由调用方根据事务结果交给玩家。</p>
     */
    public FuelColumnRepairTransaction.Result tryRepairFuelColumn(ItemStack incoming) {
        FuelColumnRepairEvaluation evaluation = evaluateRepairFuelColumn(incoming);
        FuelColumnRepairTransaction.Result result = evaluation.result();
        if (!result.success()) {
            return result;
        }
        if (evaluation.owner() == null || !evaluation.owner().validateFuelColumnCommit(
                this, evaluation.column(), evaluation.expectedStored(), result.nextColumn())) {
            return FuelColumnRepairTransaction.invalidPort(evaluation.beforeColumn(), incoming);
        }
        return commitFuelColumnRepair(evaluation, incoming, result);
    }

    /** 根据当前服务端快照计算装料结果；计算阶段不产生任何世界副作用。 */
    private FuelTransactionEvaluation evaluateInsertFuel(ItemStack incoming) {
        if (!isRefuelingBindingUsable()) {
            return FuelTransactionEvaluation.invalid(incoming);
        }
        ReactorSnapshot before = boundOwner.snapshot();
        CoreColumnPosition column = binding.column();
        FuelRefuelingTransaction.Result result = FuelRefuelingTransaction.insert(
                currentFuelColumn(before, column),
                incoming,
                boundOwner.currentFuelColumnFissionHeatHu(column));
        return new FuelTransactionEvaluation(
                boundOwner, before, column, fuelAssembly.copy(), result);
    }

    /** 根据当前服务端快照计算取料结果；计算阶段不产生任何世界副作用。 */
    private FuelTransactionEvaluation evaluateExtractFuel() {
        if (!isRefuelingBindingUsable()) {
            return FuelTransactionEvaluation.invalid(ItemStack.EMPTY);
        }
        ReactorSnapshot before = boundOwner.snapshot();
        CoreColumnPosition column = binding.column();
        FuelRefuelingTransaction.Result result = FuelRefuelingTransaction.extract(
                currentFuelColumn(before, column),
                boundOwner.currentFuelColumnFissionHeatHu(column),
                fuelAssembly);
        return new FuelTransactionEvaluation(
                boundOwner, before, column, fuelAssembly.copy(), result);
    }

    /** 根据端口当前组件和仪表权威快照计算维修结果；计算阶段不产生世界副作用。 */
    private FuelColumnRepairEvaluation evaluateRepairFuelColumn(ItemStack incoming) {
        if (!isRepairBindingUsable()) {
            return FuelColumnRepairEvaluation.invalid(incoming);
        }
        ReactorSnapshot before = boundOwner.snapshot();
        CoreColumnPosition column = binding.column();
        FuelColumnState current = currentFuelColumn(before, column);
        FuelColumnRepairTransaction.Result result = FuelColumnRepairTransaction.repair(
                current,
                incoming,
                boundOwner.currentFuelColumnFissionHeatHu(column, current));
        return new FuelColumnRepairEvaluation(
                boundOwner, before, column, fuelAssembly.copy(), current, result);
    }

    /**
     * 在所有提交前置条件已确认后一次性写入端口和快照；异常时恢复读阶段的两个状态。
     * 机械臂只接收返回栈，因此失败不会吞掉输入或把半提交物品留在端口中。
     */
    private FuelRefuelingTransaction.Result commitFuelTransaction(
            FuelTransactionEvaluation evaluation,
            ItemStack originalInput,
            ItemStack nextStored,
            FuelRefuelingTransaction.Result result
    ) {
        try {
            setFuelAssembly(nextStored);
            evaluation.owner().setSnapshot(
                    evaluation.before().withFuelColumn(evaluation.column(), result.nextColumn()));
            return result;
        } catch (RuntimeException exception) {
            setFuelAssembly(evaluation.expectedStored());
            evaluation.owner().setSnapshot(evaluation.before());
            return FuelRefuelingTransaction.invalidPort(originalInput);
        }
    }

    /** 在所有提交前置条件已确认后只写回新的单列完整度；异常时恢复原快照。 */
    private FuelColumnRepairTransaction.Result commitFuelColumnRepair(
            FuelColumnRepairEvaluation evaluation,
            ItemStack originalInput,
            FuelColumnRepairTransaction.Result result
    ) {
        try {
            evaluation.owner().setSnapshot(
                    evaluation.before().withFuelColumn(evaluation.column(), result.nextColumn()));
            return result;
        } catch (RuntimeException exception) {
            evaluation.owner().setSnapshot(evaluation.before());
            return FuelColumnRepairTransaction.invalidPort(evaluation.beforeColumn(), originalInput);
        }
    }

    /** 两阶段机械臂事务的读阶段快照；其中物品栈均只在内部使用副本。 */
    private record FuelTransactionEvaluation(
            ReactorInstrumentPortBlockEntity owner,
            ReactorSnapshot before,
            CoreColumnPosition column,
            ItemStack expectedStored,
            FuelRefuelingTransaction.Result result
    ) {
        private static FuelTransactionEvaluation invalid(ItemStack incoming) {
            return new FuelTransactionEvaluation(
                    null,
                    null,
                    null,
                    incoming == null ? ItemStack.EMPTY : incoming.copy(),
                    FuelRefuelingTransaction.invalidPort(incoming));
        }
    }

    /** 维修事务读阶段的服务端快照、端口栈和目标列投影。 */
    private record FuelColumnRepairEvaluation(
            ReactorInstrumentPortBlockEntity owner,
            ReactorSnapshot before,
            CoreColumnPosition column,
            ItemStack expectedStored,
            FuelColumnState beforeColumn,
            FuelColumnRepairTransaction.Result result
    ) {
        private static FuelColumnRepairEvaluation invalid(ItemStack incoming) {
            return new FuelColumnRepairEvaluation(
                    null,
                    null,
                    null,
                    ItemStack.EMPTY,
                    FuelColumnState.empty(),
                    FuelColumnRepairTransaction.invalidPort(FuelColumnState.empty(), incoming));
        }
    }

    /**
     * 在服务端从未成型或暂未绑定的换料端口取出精确物品栈。
     *
     * <p>该路径不访问仪表端口、不触发结构扫描、不修改 {@link ReactorSnapshot}；只有
     * 结构失效前由服务端确认安全并持久化许可时才允许清空端口。调用方必须先确保玩家
     * 交互手为空，再把返回栈一次性交给玩家，失败时返回空栈且端口保持不变。</p>
     */
    public ItemStack tryExtractUnformedFuel() {
        if (level == null || level.isClientSide || isBound()
                || !unformedExtractionAllowed || fuelAssembly.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack output = fuelAssembly.copy();
        setFuelAssembly(ItemStack.EMPTY);
        return output;
    }

    /** 换料端口先显示本地燃料耐久，再显示已同步的列遥测；冷/热端口不产生此文本。 */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (bindingType() != BindingType.REFUELING) {
            return false;
        }
        tooltip.add(Component.translatable("goggle.create_nuclear_industry.reactor.fuel_column_summary"));
        if (fuelAssembly.isEmpty()) {
            tooltip.add(Component.translatable(
                    "goggle.create_nuclear_industry.reactor.fuel_assembly_empty"));
        } else {
            ReactorInstrumentGoggleDisplay.appendFuelAssemblyTooltip(tooltip, fuelAssembly);
        }
        if (!clientFuelColumnBound) {
            tooltip.add(Component.translatable(
                    "goggle.create_nuclear_industry.reactor.fuel_column_data_unavailable"));
            return true;
        }
        if (clientFuelColumnTelemetry == null) {
            tooltip.add(Component.translatable(
                    "goggle.create_nuclear_industry.reactor.runtime_data_waiting"));
            return true;
        }
        ReactorInstrumentGoggleDisplay.appendFuelColumnTooltip(tooltip, clientFuelColumnTelemetry);
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put(FUEL_ASSEMBLY_KEY, fuelAssembly.saveOptional(registries));
        if (!clientPacket) {
            tag.putBoolean(UNFORMED_EXTRACTION_ALLOWED_KEY, unformedExtractionAllowed);
        }
        if (clientPacket) {
            tag.putBoolean(FUEL_COLUMN_BOUND_KEY, serverFuelColumnBound);
            if (serverFuelColumnTelemetry != null) {
                tag.putInt(FUEL_COLUMN_POSITION_X_KEY,
                        serverFuelColumnTelemetry.position().x());
                tag.putInt(FUEL_COLUMN_POSITION_Z_KEY,
                        serverFuelColumnTelemetry.position().z());
                tag.putDouble(FUEL_COLUMN_INTEGRITY_KEY,
                        serverFuelColumnTelemetry.fuelColumnIntegrity());
                tag.putDouble(FUEL_COLUMN_HEAT_KEY,
                        serverFuelColumnTelemetry.generatedFissionHeatHuPerTick());
            }
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        fuelAssembly = tag.contains(FUEL_ASSEMBLY_KEY)
                ? ItemStack.parseOptional(registries, tag.getCompound(FUEL_ASSEMBLY_KEY))
                : ItemStack.EMPTY;
        if (!clientPacket) {
            // 02A 端口没有该字段时默认拒绝，必须等待服务端安全重绑定重新确认。
            unformedExtractionAllowed = tag.contains(UNFORMED_EXTRACTION_ALLOWED_KEY)
                    && tag.getBoolean(UNFORMED_EXTRACTION_ALLOWED_KEY);
            return;
        }
        clientFuelColumnBound = tag.getBoolean(FUEL_COLUMN_BOUND_KEY);
        clientFuelColumnTelemetry = null;
        if (!clientFuelColumnBound || !tag.contains(FUEL_COLUMN_POSITION_X_KEY)
                || !tag.contains(FUEL_COLUMN_POSITION_Z_KEY)
                || !tag.contains(FUEL_COLUMN_INTEGRITY_KEY)
                || !tag.contains(FUEL_COLUMN_HEAT_KEY)) {
            return;
        }
        try {
            clientFuelColumnTelemetry = new ReactorInstrumentTelemetry.FuelColumnTelemetry(
                    new CoreColumnPosition(
                            tag.getInt(FUEL_COLUMN_POSITION_X_KEY),
                            tag.getInt(FUEL_COLUMN_POSITION_Z_KEY)),
                    tag.getDouble(FUEL_COLUMN_INTEGRITY_KEY),
                    tag.getDouble(FUEL_COLUMN_HEAT_KEY));
        } catch (IllegalArgumentException exception) {
            clientFuelColumnBound = false;
        }
    }

    /** 只有有效结构中的真实换料端口才能提交列状态事务。 */
    private boolean isRefuelingBindingUsable() {
        return (level == null || !level.isClientSide)
                && boundOwner != null
                && structureBindingIsValid()
                && binding != null
                && binding.type() == BindingType.REFUELING
                && binding.column() != null
                && FuelAssemblyItemCodec.isValidStoredFuel(fuelAssembly);
    }

    /** 维修必须绑定有效燃料列；空燃料端口也可维修结构完整度，但不会因此补入燃料。 */
    private boolean isRepairBindingUsable() {
        return (level == null || !level.isClientSide)
                && boundOwner != null
                && structureBindingIsValid()
                && binding != null
                && binding.type() == BindingType.REFUELING
                && binding.column() != null
                && FuelAssemblyItemCodec.isValidStoredFuel(fuelAssembly);
    }

    /** 仅在逻辑服务端由绑定生命周期调用，禁止客户端更新危险锁。 */
    private void setUnformedExtractionAllowed(boolean allowed) {
        if (level != null && level.isClientSide) {
            throw new IllegalStateException("unformed extraction lock can only change on the server");
        }
        if (unformedExtractionAllowed == allowed) {
            return;
        }
        unformedExtractionAllowed = allowed;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** 从端口物品构造当前列的瞬态模拟投影；旧快照镜像仅用于迁移兼容。 */
    FuelColumnState currentFuelColumn(
            ReactorSnapshot snapshot,
            CoreColumnPosition column
    ) {
        FuelColumnState base = snapshot.fuelColumns().getOrDefault(column, FuelColumnState.empty());
        if (fuelAssembly.isEmpty() && base.fuelAssembly().present()) {
            return base;
        }
        return base.withFuelAssemblyProjection(FuelAssemblyItemCodec.simulationState(fuelAssembly));
    }

    /** 再次检查仪表端口和绑定记录，避免失效结构留下的旧调用修改快照。 */
    private boolean structureBindingIsValid() {
        return boundOwner.structureValid() && isBoundTo(
                boundOwner, BindingType.REFUELING, binding == null ? null : binding.column());
    }

    /** 判断旧所有者是否已经移除、失效或不再占有世界中的仪表位置。 */
    private boolean canReclaimBinding() {
        if (boundOwner == null) {
            return true;
        }
        if (boundOwner.isRemoved() || !boundOwner.structureValid()) {
            return true;
        }
        var ownerLevel = boundOwner.getLevel();
        return ownerLevel == null
                || ownerLevel.getBlockEntity(boundOwner.getBlockPos()) != boundOwner;
    }

    /** 在绑定边沿同时刷新 NeoForge capability 缓存和相邻 Create 管网。 */
    private void invalidateCapabilityAndNetwork(
            Binding previousBinding,
            BindingType nextType
    ) {
        level.invalidateCapabilities(worldPosition);
        if (isFluidBinding(previousBinding == null ? null : previousBinding.type())
                || isFluidBinding(nextType)) {
            ReactorStructureLifecycle.notifyFluidNetworkAround(level, worldPosition);
        }
    }

    /** 冷/热端口需要 Create 管网重新发现，换料端口不参与流体网络。 */
    private static boolean isFluidBinding(BindingType type) {
        return type == BindingType.COLD_COOLANT || type == BindingType.HOT_COOLANT;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ReactorStructureLifecycle.scheduleRescanAround(level, worldPosition);
        }
    }

    /**
     * 端口被移除或区块卸载时撤销反向所有者引用，避免旧 handler 在新结构上继续写入。
     */
    @Override
    public void invalidate() {
        ReactorInstrumentPortBlockEntity owner = boundOwner;
        clearBinding(owner);
        if (owner != null) {
            owner.detachPort(this);
        }
        super.invalidate();
    }

    /** 端口绑定的职责；冷/热端口只使用全堆账本，换料端口只使用单列状态。 */
    public enum BindingType {
        COLD_COOLANT,
        HOT_COOLANT,
        REFUELING
    }

    /** 服务端结构映射生成的不可变绑定记录。 */
    public record Binding(
            BlockPos instrumentPortPos,
            BindingType type,
            CoreColumnPosition column
    ) {
        public Binding {
            if (instrumentPortPos == null || type == null
                    || (type == BindingType.REFUELING) != (column != null)) {
                throw new IllegalArgumentException("port binding fields are inconsistent");
            }
            instrumentPortPos = instrumentPortPos.immutable();
        }

        /** 冷/热端口访问共享全堆账本，换料端口访问唯一燃料列。 */
        public boolean usesGlobalLedger() {
            return type != BindingType.REFUELING;
        }
    }
}
