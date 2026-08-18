package com.iksxh.create_nuclear_industry.reactor;

/**
 * Loader-independent durability state for the single fuel assembly held by a fuel column.
 * Column integrity and heat are deliberately not stored here.
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

    public int remainingDurability() {
        return present ? maxDamage - damage : 0;
    }

    public double remainingFraction() {
        return present ? (double) remainingDurability() / maxDamage : 0.0D;
    }

    public boolean exhausted() {
        return present && damage == maxDamage;
    }
}
