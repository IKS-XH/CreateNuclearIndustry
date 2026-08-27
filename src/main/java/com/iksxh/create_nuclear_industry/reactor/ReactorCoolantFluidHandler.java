package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 单个冷态或热态反应堆端口的正式 P1 capability 适配器。
 *
 * <p>处理器不拥有独立 tank；所有读写都经过仪表端口的共享权威快照，因此多个
 * 端口不能产生彼此独立的冷却剂库存。流量限制按物理端口、按服务端 tick 计。</p>
 */
public final class ReactorCoolantFluidHandler implements IFluidHandler {
    private static final Map<ReactorPortBlockEntity, ReactorCoolantPortFlowBudget> PORT_BUDGETS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final ReactorInstrumentPortBlockEntity owner;
    private final ReactorPortBlockEntity sourcePort;
    private final boolean coldInput;
    private final ReactorCoolantPortFlowBudget flowBudget;
    private final Long capacityOverrideMb;

    private ReactorCoolantFluidHandler(
            ReactorPortBlockEntity sourcePort,
            ReactorInstrumentPortBlockEntity owner,
            boolean coldInput,
            ReactorCoolantPortFlowBudget flowBudget,
            Long capacityOverrideMb
    ) {
        if (owner == null || flowBudget == null
                || (capacityOverrideMb != null && capacityOverrideMb < 0L)) {
            throw new IllegalArgumentException("reactor owner, flow budget and capacity are required");
        }
        this.sourcePort = sourcePort;
        this.owner = owner;
        this.coldInput = coldInput;
        this.flowBudget = flowBudget;
        this.capacityOverrideMb = capacityOverrideMb;
    }

    /** 仅为绑定到有效反应堆所有者的冷/热端口创建 capability。 */
    public static ReactorCoolantFluidHandler forPort(ReactorPortBlockEntity port) {
        if (port == null) {
            return null;
        }
        boolean cold = port.getBlockState().is(P1Blocks.REACTOR_COLD_PORT.get());
        boolean hot = port.getBlockState().is(P1Blocks.REACTOR_HOT_PORT.get());
        if (!cold && !hot) {
            return null;
        }
        ReactorPortBlockEntity.BindingType expectedType = cold
                ? ReactorPortBlockEntity.BindingType.COLD_COOLANT
                : ReactorPortBlockEntity.BindingType.HOT_COOLANT;
        ReactorInstrumentPortBlockEntity owner = port.boundOwner();
        if (owner == null || !port.isBoundTo(owner, expectedType, null)) {
            return null;
        }
        return new ReactorCoolantFluidHandler(port, owner, cold, budgetFor(port), null);
    }

    /** 使用实时服务端配置中的流量上限和库存容量。 */
    public static ReactorCoolantFluidHandler forOwner(
            ReactorInstrumentPortBlockEntity owner,
            boolean coldInput
    ) {
        return new ReactorCoolantFluidHandler(null, owner, coldInput,
                new ReactorCoolantPortFlowBudget(), null);
    }

    /** 为确定性测试提供显式库存容量覆盖值。 */
    public static ReactorCoolantFluidHandler forOwner(
            ReactorInstrumentPortBlockEntity owner,
            boolean coldInput,
            long capacityOverrideMb
    ) {
        return new ReactorCoolantFluidHandler(null, owner, coldInput,
                new ReactorCoolantPortFlowBudget(), capacityOverrideMb);
    }

    public ReactorInstrumentPortBlockEntity owner() {
        return owner;
    }

    public boolean coldInput() {
        return coldInput;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank != 0 || !isCapabilityAvailable()) {
            return FluidStack.EMPTY;
        }
        long amount = coldInput ? owner.snapshot().coldCoolantMb() : owner.snapshot().hotCoolantMb();
        int visibleAmount = (int) Math.min(Integer.MAX_VALUE, amount);
        return visibleAmount == 0
                ? FluidStack.EMPTY
                : new FluidStack(coldInput ? ModFluids.COMPOUND_COOLANT_SOURCE.get()
                        : ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), visibleAmount);
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 && isCapabilityAvailable()
                ? (int) Math.min(Integer.MAX_VALUE, configuredCapacityMb()) : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && isCapabilityAvailable()
                && (coldInput ? ModFluids.isCompoundCoolant(stack) : false);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!isCapabilityAvailable() || !coldInput || !ModFluids.isCompoundCoolant(resource)) {
            return 0;
        }
        long current = owner.snapshot().coldCoolantMb();
        long available = Math.max(0L, configuredCapacityMb() - current);
        int requested = (int) Math.min((long) resource.getAmount(), available);
        int accepted = flowBudget.reserve(currentServerTick(), requested,
                configuredFlowLimitMbPerTick(), action == FluidAction.EXECUTE);
        if (accepted > 0 && action == FluidAction.EXECUTE) {
            ReactorSnapshot snapshot = owner.snapshot();
            owner.setSnapshot(snapshot.withCoolantInventories(
                    snapshot.coldCoolantMb() + accepted, snapshot.hotCoolantMb()));
        }
        return accepted;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (!isDrainRequestValid(resource)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (!isCapabilityAvailable() || coldInput || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        long current = owner.snapshot().hotCoolantMb();
        int requested = (int) Math.min((long) maxDrain, current);
        int drained = flowBudget.reserve(currentServerTick(), requested,
                configuredFlowLimitMbPerTick(), action == FluidAction.EXECUTE);
        if (drained == 0) {
            return FluidStack.EMPTY;
        }
        if (action == FluidAction.EXECUTE) {
            ReactorSnapshot snapshot = owner.snapshot();
            owner.setSnapshot(snapshot.withCoolantInventories(
                    snapshot.coldCoolantMb(), snapshot.hotCoolantMb() - drained));
        }
        return new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), drained);
    }

    private boolean isDrainRequestValid(FluidStack resource) {
        return isCapabilityAvailable() && !coldInput && ModFluids.isHotCompoundCoolant(resource);
    }

    /** 判断从世界 capability 取得的处理器是否仍对应当前有效绑定。 */
    private boolean isCapabilityAvailable() {
        if (sourcePort == null) {
            return true;
        }
        ReactorPortBlockEntity.BindingType expectedType = coldInput
                ? ReactorPortBlockEntity.BindingType.COLD_COOLANT
                : ReactorPortBlockEntity.BindingType.HOT_COOLANT;
        return owner.structureValid() && sourcePort.isBoundTo(owner, expectedType, null);
    }

    private long configuredCapacityMb() {
        if (capacityOverrideMb != null) {
            return capacityOverrideMb;
        }
        return coldInput
                ? P1ServerConfig.VALUES.coldInventoryCapacityMb.get().longValue()
                : P1ServerConfig.VALUES.hotInventoryCapacityMb.get().longValue();
    }

    private int configuredFlowLimitMbPerTick() {
        return Math.max(0, P1ServerConfig.VALUES.perPortFlowMbPerTick.get());
    }

    private long currentServerTick() {
        var level = owner.getLevel();
        return level == null ? 0L : level.getGameTime();
    }

    private static ReactorCoolantPortFlowBudget budgetFor(ReactorPortBlockEntity port) {
        synchronized (PORT_BUDGETS) {
            return PORT_BUDGETS.computeIfAbsent(port, ignored -> new ReactorCoolantPortFlowBudget());
        }
    }

}
