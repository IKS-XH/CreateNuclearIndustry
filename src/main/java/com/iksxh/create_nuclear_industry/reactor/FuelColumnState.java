package com.iksxh.create_nuclear_industry.reactor;

import java.util.Objects;

/** 服务端权威反应堆快照中由单个燃料列拥有的不可变状态。 */
public record FuelColumnState(
        FuelAssemblyState fuelAssembly,
        double integrity,
        double cachedHeatHu,
        double fuelBurnRemainder
) {
    /** 兼容尚未持久化小数燃耗余量的旧快照构造方式。 */
    public FuelColumnState(
            FuelAssemblyState fuelAssembly,
            double integrity,
            double cachedHeatHu
    ) {
        this(fuelAssembly, integrity, cachedHeatHu, 0.0D);
    }

    public FuelColumnState {
        Objects.requireNonNull(fuelAssembly, "fuelAssembly");
        requireUnitInterval("fuel column integrity", integrity);
        requireFiniteNonNegative("fuel column cached heat", cachedHeatHu);
        requireUnitInterval("fuel burn remainder", fuelBurnRemainder);
    }

    public static FuelColumnState empty() {
        return new FuelColumnState(FuelAssemblyState.empty(), 1.0D, 0.0D);
    }

    public boolean hasUsableFuel() {
        return fuelAssembly.present() && !fuelAssembly.exhausted();
    }

    public boolean isEffectiveFuel() {
        return hasUsableFuel() && integrity > 0.0D;
    }

    /**
     * 应用一个 tick 的小数燃耗，同时保留 ItemStack 暴露的整数耐久语义。
     * 模拟器以整根燃料组件的比例报告燃耗；小数部分保存在燃料列中，避免逐 tick 舍弃，
     * 从而使组件按配置寿命耗尽。
     */
    public FuelColumnState burnFraction(double fractionOfAssembly) {
        if (!Double.isFinite(fractionOfAssembly) || fractionOfAssembly < 0.0D) {
            throw new IllegalArgumentException("fuel burn fraction must be finite and non-negative");
        }
        if (!hasUsableFuel() || fractionOfAssembly == 0.0D) {
            return this;
        }

        double durabilityUnits = fractionOfAssembly * fuelAssembly.maxDamage() + fuelBurnRemainder;
        if (!Double.isFinite(durabilityUnits) || durabilityUnits < 0.0D) {
            throw new IllegalArgumentException("fuel burn durability units must be finite and non-negative");
        }
        int wholeDamage = (int) Math.min(
                (long) fuelAssembly.maxDamage() - fuelAssembly.damage(),
                (long) Math.floor(durabilityUnits + 1.0E-12D));
        int nextDamage = fuelAssembly.damage() + Math.max(0, wholeDamage);
        double nextRemainder = nextDamage >= fuelAssembly.maxDamage()
                ? 0.0D
                : Math.max(0.0D, durabilityUnits - wholeDamage);
        return new FuelColumnState(
                FuelAssemblyState.installed(fuelAssembly.maxDamage(), nextDamage),
                integrity,
                cachedHeatHu,
                Math.min(1.0D, nextRemainder)
        );
    }

    private static void requireUnitInterval(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]");
        }
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
