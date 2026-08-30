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
     * 将一个新燃料组件装入本列，同时清零上一组件遗留的小数燃耗余量；列完整度和
     * 缓存余热属于结构状态，换料不会伪造修复或删除它们。
     */
    public FuelColumnState withFuelAssembly(FuelAssemblyState nextFuelAssembly) {
        Objects.requireNonNull(nextFuelAssembly, "next fuel assembly");
        if (!nextFuelAssembly.present() || nextFuelAssembly.exhausted()) {
            throw new IllegalArgumentException("a loaded fuel assembly must have remaining durability");
        }
        return new FuelColumnState(nextFuelAssembly, integrity, cachedHeatHu, 0.0D);
    }

    /** 取出本列组件并保留列完整度和余热，防止换料事务无声清除结构状态。 */
    public FuelColumnState withoutFuelAssembly() {
        return new FuelColumnState(FuelAssemblyState.empty(), integrity, cachedHeatHu, 0.0D);
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
