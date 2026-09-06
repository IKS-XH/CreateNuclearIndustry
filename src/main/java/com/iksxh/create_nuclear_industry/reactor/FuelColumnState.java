package com.iksxh.create_nuclear_industry.reactor;

import java.util.Objects;

/**
 * 服务端权威反应堆快照中由单个燃料列拥有的不可变状态。
 *
 * <p>{@code cachedHeatHu} 是该列尚未从热工账本移除的总热量，单位为 HU；
 * {@code quantizedHeatRemainderHu} 是其中由整数 mB 冷却剂量化产生、不得造成损伤或传播
 * 的安全子集。两者都由仪表端口快照持有，客户端只读取派生遥测，且安全子集不能大于总缓存。</p>
 */
public record FuelColumnState(
        FuelAssemblyState fuelAssembly,
        double integrity,
        double cachedHeatHu,
        double fuelBurnRemainder,
        double quantizedHeatRemainderHu
) {
    /** 兼容尚未持久化小数燃耗余量的旧快照构造方式。 */
    public FuelColumnState(
            FuelAssemblyState fuelAssembly,
            double integrity,
            double cachedHeatHu
    ) {
        this(fuelAssembly, integrity, cachedHeatHu, 0.0D, 0.0D);
    }

    /** 兼容已经持久化小数燃耗余量、但尚未持久化热量余数的旧快照构造方式。 */
    public FuelColumnState(
            FuelAssemblyState fuelAssembly,
            double integrity,
            double cachedHeatHu,
            double fuelBurnRemainder
    ) {
        this(fuelAssembly, integrity, cachedHeatHu, fuelBurnRemainder, 0.0D);
    }

    public FuelColumnState {
        Objects.requireNonNull(fuelAssembly, "fuelAssembly");
        requireUnitInterval("fuel column integrity", integrity);
        requireFiniteNonNegative("fuel column cached heat", cachedHeatHu);
        requireUnitInterval("fuel burn remainder", fuelBurnRemainder);
        requireFiniteNonNegative("fuel column quantized heat remainder", quantizedHeatRemainderHu);
        if (quantizedHeatRemainderHu > cachedHeatHu + 1.0E-12D) {
            throw new IllegalArgumentException("quantized heat remainder cannot exceed cached heat");
        }
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
        return new FuelColumnState(nextFuelAssembly, integrity, cachedHeatHu, 0.0D,
                quantizedHeatRemainderHu);
    }

    /**
     * 更新服务端模拟使用的燃料投影，但不重置燃耗小数余量。
     *
     * <p>投影来自换料端口持有的 {@link net.minecraft.world.item.ItemStack}，不是第二份
     * 持久化库存；允许空或已耗尽投影，是为了让端口加载和乏燃料转换仍能经过同一 tick
     * 结算路径。</p>
     */
    public FuelColumnState withFuelAssemblyProjection(FuelAssemblyState nextFuelAssembly) {
        return new FuelColumnState(
                Objects.requireNonNull(nextFuelAssembly, "next fuel assembly"),
                integrity,
                cachedHeatHu,
                fuelBurnRemainder,
                quantizedHeatRemainderHu
        );
    }

    /** 移除运行时燃料投影而保留燃耗余量，供 v4 快照去除非权威字段。 */
    public FuelColumnState withoutFuelAssemblyProjection() {
        return withFuelAssemblyProjection(FuelAssemblyState.empty());
    }

    /** 取出本列组件并保留列完整度和余热，防止换料事务无声清除结构状态。 */
    public FuelColumnState withoutFuelAssembly() {
        return new FuelColumnState(FuelAssemblyState.empty(), integrity, cachedHeatHu, 0.0D,
                quantizedHeatRemainderHu);
    }

    /**
     * 只替换燃料列完整度，保留组件投影、缓存余热和两个小数余量。
     *
     * <p>维修事务通过此入口提交，确保钢板维修不会补充燃料耐久、清除余热或回退
     * 燃耗结算所需的小数状态。</p>
     */
    public FuelColumnState withIntegrity(double nextIntegrity) {
        return new FuelColumnState(
                fuelAssembly,
                nextIntegrity,
                cachedHeatHu,
                fuelBurnRemainder,
                quantizedHeatRemainderHu
        );
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
                Math.min(1.0D, nextRemainder),
                quantizedHeatRemainderHu
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
