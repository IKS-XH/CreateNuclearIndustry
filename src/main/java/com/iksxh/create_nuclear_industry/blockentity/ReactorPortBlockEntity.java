package com.iksxh.create_nuclear_industry.blockentity;

import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.FuelRefuelingTransaction;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** 冷、热和补料端口共用的空壳方块实体；不拥有任何反应堆模拟状态。 */
public final class ReactorPortBlockEntity extends P1MinimalBlockEntity {
    private Binding binding;
    private ReactorInstrumentPortBlockEntity boundOwner;

    public ReactorPortBlockEntity(BlockPos pos, BlockState state) {
        super(P1BlockEntities.REACTOR_PORT.get(), pos, state);
    }

    /** 返回该端口当前的服务端绑定；绑定缓存不属于反应堆持久化状态。 */
    public Binding binding() {
        return binding;
    }

    /** 返回端口是否已经绑定到一个有效的仪表端口。 */
    public boolean isBound() {
        return binding != null && boundOwner != null;
    }

    /** 返回共享全堆账本或单列状态的唯一权威所有者。 */
    public ReactorInstrumentPortBlockEntity boundOwner() {
        return boundOwner;
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
        if (boundOwner != null && boundOwner != owner) {
            return false;
        }
        boolean changed = !isBoundTo(owner, expectedType, column);
        binding = new Binding(owner.getBlockPos(), expectedType, column);
        boundOwner = owner;
        if (changed && level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
        }
        return true;
    }

    /** 清除指定仪表端口留下的旧绑定，防止结构失效后继续暴露 capability。 */
    public void clearBinding(ReactorInstrumentPortBlockEntity owner) {
        if (owner == null || boundOwner == owner) {
            boolean changed = binding != null || boundOwner != null;
            binding = null;
            boundOwner = null;
            if (changed && level != null && !level.isClientSide) {
                level.invalidateCapabilities(worldPosition);
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
     * 在服务端尝试向该端口绑定的空燃料列装入一个组件。
     *
     * <p>当前任务只提供事务入口；玩家右键和 Create 机械臂分别在后续任务调用此入口。
     * 成功提交前不修改输入栈，成功后只替换仪表端口快照中的目标列。</p>
     */
    public FuelRefuelingTransaction.Result tryInsertFuel(ItemStack incoming) {
        if (!isRefuelingBindingUsable()) {
            return FuelRefuelingTransaction.invalidPort(incoming);
        }
        ReactorSnapshot before = boundOwner.snapshot();
        CoreColumnPosition column = binding.column();
        FuelColumnState current = before.fuelColumns().getOrDefault(column, FuelColumnState.empty());
        FuelRefuelingTransaction.Result result = FuelRefuelingTransaction.insert(
                current, incoming, boundOwner.currentFuelColumnFissionHeatHu(column));
        if (result.success()) {
            boundOwner.setSnapshot(before.withFuelColumn(column, result.nextColumn()));
        }
        return result;
    }

    /** 在服务端尝试取出该端口绑定燃料列中的组件或冷却乏燃料。 */
    public FuelRefuelingTransaction.Result tryExtractFuel() {
        if (!isRefuelingBindingUsable()) {
            return FuelRefuelingTransaction.invalidPort(ItemStack.EMPTY);
        }
        ReactorSnapshot before = boundOwner.snapshot();
        CoreColumnPosition column = binding.column();
        FuelColumnState current = before.fuelColumns().getOrDefault(column, FuelColumnState.empty());
        FuelRefuelingTransaction.Result result = FuelRefuelingTransaction.extract(
                current, boundOwner.currentFuelColumnFissionHeatHu(column));
        if (result.success()) {
            boundOwner.setSnapshot(before.withFuelColumn(column, result.nextColumn()));
        }
        return result;
    }

    /** 只有有效结构中的真实换料端口才能提交列状态事务。 */
    private boolean isRefuelingBindingUsable() {
        return (level == null || !level.isClientSide)
                && boundOwner != null
                && structureBindingIsValid()
                && binding != null
                && binding.type() == BindingType.REFUELING
                && binding.column() != null;
    }

    /** 再次检查仪表端口和绑定记录，避免失效结构留下的旧调用修改快照。 */
    private boolean structureBindingIsValid() {
        return boundOwner.structureValid() && isBoundTo(
                boundOwner, BindingType.REFUELING, binding == null ? null : binding.column());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ReactorStructureLifecycle.scheduleRescanAround(level, worldPosition);
        }
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
