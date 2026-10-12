package com.iksxh.create_nuclear_industry.industrial;

import java.util.function.BooleanSupplier;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** 只由本包调度器创建的单次源/目标许可；每个执行方向共享限额，过期无法取得裸库存。 */
final class IndustrialTransferPermit {
    private final IFluidHandler source, target;
    private final int limit;
    private final BooleanSupplier valid;
    private int removed, inserted;
    IndustrialTransferPermit(IFluidHandler source, IFluidHandler target, int limit, BooleanSupplier valid) {
        this.source = source; this.target = target; this.limit = limit; this.valid = valid;
    }
    int allow(IFluidHandler handler, boolean drain, int requested, boolean execute) {
        if (!valid.getAsBoolean() || handler != (drain ? source : target)) return 0;
        return Math.min(Math.max(0, requested), Math.max(0, limit - (drain ? removed : inserted)));
    }
    void record(boolean drain, int amount, boolean execute) {
        if (!execute) return;
        if (drain) removed += amount; else inserted += amount;
    }
}
