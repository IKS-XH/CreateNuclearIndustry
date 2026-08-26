package com.iksxh.create_nuclear_industry.reactor;

/**
 * 燃料列中单个燃料组件的加载器无关耐久状态。
 * 列完整度和缓存热量故意不放在这里，而由 {@link FuelColumnState} 拥有。
 */
public record FuelAssemblyState(boolean present, int damage, int maxDamage) {
    public FuelAssemblyState {
        if (!present) {
            if (damage != 0 || maxDamage != 0) {
                throw new IllegalArgumentException("an empty fuel assembly state cannot contain durability");
            }
        } else {
            if (maxDamage <= 0) {
                throw new IllegalArgumentException("an installed fuel assembly must have positive max damage");
            }
            if (damage < 0 || damage > maxDamage) {
                throw new IllegalArgumentException("fuel assembly damage must be in [0, maxDamage]");
            }
        }
    }

    public static FuelAssemblyState empty() {
        return new FuelAssemblyState(false, 0, 0);
    }

    public static FuelAssemblyState installed(int maxDamage, int damage) {
        return new FuelAssemblyState(true, damage, maxDamage);
    }

    /** 返回仍可消耗的整数耐久；空组件返回 0。 */
    public int remainingDurability() {
        return present ? maxDamage - damage : 0;
    }

    /** 返回剩余耐久占最大耐久的比例，范围为 [0,1]。 */
    public double remainingFraction() {
        return present ? (double) remainingDurability() / maxDamage : 0.0D;
    }

    /** 判断已安装组件是否达到最大损伤。 */
    public boolean exhausted() {
        return present && damage == maxDamage;
    }
}
