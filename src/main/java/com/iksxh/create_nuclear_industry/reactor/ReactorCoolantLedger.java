package com.iksxh.create_nuclear_industry.reactor;

/**
 * Loader-independent, single-port coolant conservation ledger.
 *
 * <p>The caller supplies the coolant entering through the cold port and the
 * measured amount actually emitted by the hot port for the current tick. The
 * ledger also receives the total hot-inventory capacity, so a blocked hot port
 * may still use free internal buffer space, while a full buffer blocks further
 * conversion.</p>
 */
public final class ReactorCoolantLedger {
    private ReactorCoolantLedger() {
    }

    /** Persistent internal coolant inventories owned by the reactor snapshot. */
    public record Inventory(double coldCoolantMb, double hotCoolantMb) {
        public Inventory {
            requireFiniteNonNegative("cold coolant inventory", coldCoolantMb);
            requireFiniteNonNegative("hot coolant inventory", hotCoolantMb);
        }
    }

    /** One cold-port input and measured hot-port output for one tick. */
    public record Input(
            double availableHeatHu,
            double coldInAcceptedMb,
            double hotOutputCapacityMb,
            double hotOutActualMb,
            double hotInventoryCapacityMb,
            double coolantAbsorptionHuPerMb
    ) {
        public Input {
            requireFiniteNonNegative("available heat", availableHeatHu);
            requireFiniteNonNegative("accepted cold input", coldInAcceptedMb);
            requireFiniteNonNegative("hot output capacity", hotOutputCapacityMb);
            requireFiniteNonNegative("actual hot output", hotOutActualMb);
            requireFiniteNonNegative("hot inventory capacity", hotInventoryCapacityMb);
            if (hotOutActualMb > hotOutputCapacityMb) {
                throw new IllegalArgumentException("actual hot output cannot exceed hot output capacity");
            }
            if (!Double.isFinite(coolantAbsorptionHuPerMb) || coolantAbsorptionHuPerMb <= 0.0D) {
                throw new IllegalArgumentException("coolant absorption must be finite and positive");
            }
        }
    }

    /** Result of one conservative conversion attempt. */
    public record Settlement(
            Inventory nextInventory,
            double convertedCoolantMb,
            double hotOutActualMb,
            double removedHeatHu,
            double remainingHeatHu
    ) {
        public Settlement {
            if (nextInventory == null) {
                throw new IllegalArgumentException("next coolant inventory is required");
            }
            requireFiniteNonNegative("converted coolant", convertedCoolantMb);
            requireFiniteNonNegative("actual hot output", hotOutActualMb);
            requireFiniteNonNegative("removed heat", removedHeatHu);
            requireFiniteNonNegative("remaining heat", remainingHeatHu);
        }
    }

    /**
     * Settles one cold-port to hot-port conversion without touching fuel state.
     *
     * <p>The conversion amount is exactly:</p>
     *
     * <pre>
     * hotSpaceAfterOutputMb = hotInventoryCapacityMb
     *     - previousHotInventoryMb + hotOutActualMb
     * convertedCoolantMb = min(
     *     availableHeatHu / coolantAbsorptionHuPerMb,
     *     previousColdInventoryMb + coldInAcceptedMb,
     *     hotSpaceAfterOutputMb
     * )
     * </pre>
     *
     * <p>The output capacity validates the measured hot-port transfer; the
     * measured transfer itself is what frees hot-inventory space. Input
     * coolant that cannot be converted remains in the cold inventory, and
     * converted coolant is added to the hot inventory before the measured
     * hot output is deducted.</p>
     */
    public static Settlement settle(Inventory previous, Input input) {
        if (previous == null || input == null) {
            throw new IllegalArgumentException("previous inventory and input are required");
        }
        if (previous.hotCoolantMb() > input.hotInventoryCapacityMb()) {
            throw new IllegalArgumentException("hot inventory exceeds its configured capacity");
        }

        double availableCold = safeAdd(previous.coldCoolantMb(), input.coldInAcceptedMb());
        double hotSpaceAfterOutput = input.hotInventoryCapacityMb() - previous.hotCoolantMb()
                + input.hotOutActualMb();
        double heatLimitedCoolant = input.availableHeatHu() / input.coolantAbsorptionHuPerMb();
        double converted = Math.min(heatLimitedCoolant,
                Math.min(availableCold, Math.max(0.0D, hotSpaceAfterOutput)));
        converted = finiteNonNegative(converted);

        double removedHeat = converted * input.coolantAbsorptionHuPerMb();
        removedHeat = finiteNonNegative(Math.min(input.availableHeatHu(), removedHeat));
        double remainingHeat = finiteNonNegative(Math.max(0.0D, input.availableHeatHu() - removedHeat));
        double nextCold = availableCold - converted;
        double nextHot = previous.hotCoolantMb() + converted - input.hotOutActualMb();
        if (nextHot < 0.0D) {
            throw new IllegalArgumentException("actual hot output exceeds available hot coolant");
        }
        Inventory next = new Inventory(nextCold, nextHot);
        return new Settlement(next, converted, input.hotOutActualMb(), removedHeat, remainingHeat);
    }

    private static double safeAdd(double left, double right) {
        double result = left + right;
        if (!Double.isFinite(result) || result < 0.0D) {
            throw new IllegalArgumentException("coolant inventory sum must be finite and non-negative");
        }
        return result;
    }

    private static double finiteNonNegative(double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException("coolant settlement value must be finite and non-negative");
        }
        return value;
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
