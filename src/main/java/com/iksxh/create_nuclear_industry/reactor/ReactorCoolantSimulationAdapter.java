package com.iksxh.create_nuclear_industry.reactor;

/**
 * 将加载器无关冷却剂账本桥接到正式反应堆快照。
 * 流体 capability 只报告整数 mB 转移，因此不足一个 mB 转化量的热不会被舍入丢失，
 * 而是继续作为剩余热量返回给后续热工阶段。
 */
public final class ReactorCoolantSimulationAdapter {
    private static final double WHOLE_MB_EPSILON = 1.0E-9D;

    private ReactorCoolantSimulationAdapter() {
    }

    public record TickInput(
            double availableHeatHu,
            ReactorCoolantLedger.PortSummary ports,
            double coldInAcceptedMb,
            double hotOutActualMb,
            long hotInventoryCapacityMb,
            double coolantAbsorptionHuPerMb
    ) {
        public TickInput {
            requireFiniteNonNegative("available heat", availableHeatHu);
            if (ports == null) {
                throw new IllegalArgumentException("coolant port summary is required");
            }
            requireWholeMb("accepted cold input", coldInAcceptedMb);
            requireWholeMb("actual hot output", hotOutActualMb);
            if (coldInAcceptedMb > ports.coldInputMb() + WHOLE_MB_EPSILON) {
                throw new IllegalArgumentException("accepted cold input exceeds observed cold capacity");
            }
            if (hotOutActualMb > ports.hotOutputCapacityMb() + WHOLE_MB_EPSILON) {
                throw new IllegalArgumentException("actual hot output exceeds observed hot capacity");
            }
            if (hotInventoryCapacityMb < 0L) {
                throw new IllegalArgumentException("hot inventory capacity must be non-negative");
            }
            requirePositiveFinite("coolant absorption", coolantAbsorptionHuPerMb);
        }
    }

    public record Result(
            ReactorSnapshot nextSnapshot,
            ReactorCoolantLedger.Settlement settlement
    ) {
        public Result {
            if (nextSnapshot == null || settlement == null) {
                throw new IllegalArgumentException("coolant conversion result is required");
            }
        }
    }

    /** 结算一次正式冷却剂转化并返回新的权威快照；输入快照保持只读。 */
    public static Result settle(ReactorSnapshot previous, TickInput input) {
        if (previous == null || input == null) {
            throw new IllegalArgumentException("previous snapshot and coolant input are required");
        }

        // 正式快照以整数 mB 保存流体；账本结果保留不足一个 mB 的热量余数，交给
        // 后续热工状态继续携带，避免静默丢失热量。
        double wholeMillibucketHeat = wholeMillibucketHeat(
                input.availableHeatHu(), input.coolantAbsorptionHuPerMb());
        ReactorCoolantLedger.Settlement ledgerSettlement = ReactorCoolantLedger.settle(
                new ReactorCoolantLedger.Inventory(
                        previous.coldCoolantMb(), previous.hotCoolantMb()),
                new ReactorCoolantLedger.Input(
                        wholeMillibucketHeat,
                        input.coldInAcceptedMb(),
                        input.ports().hotOutputCapacityMb(),
                        input.hotOutActualMb(),
                        input.hotInventoryCapacityMb(),
                        input.coolantAbsorptionHuPerMb()
                )
        );

        long nextCold = wholeLong("next cold inventory", ledgerSettlement.nextInventory().coldCoolantMb());
        long nextHot = wholeLong("next hot inventory", ledgerSettlement.nextInventory().hotCoolantMb());
        ReactorCoolantLedger.Settlement resultSettlement = new ReactorCoolantLedger.Settlement(
                ledgerSettlement.nextInventory(),
                ledgerSettlement.convertedCoolantMb(),
                ledgerSettlement.hotOutActualMb(),
                ledgerSettlement.removedHeatHu(),
                input.availableHeatHu() - ledgerSettlement.removedHeatHu()
        );
        return new Result(
                previous.withCoolantInventories(nextCold, nextHot),
                resultSettlement
        );
    }

    private static double wholeMillibucketHeat(double availableHeatHu, double absorptionHuPerMb) {
        double wholeMb = Math.floor(availableHeatHu / absorptionHuPerMb + WHOLE_MB_EPSILON);
        if (!Double.isFinite(wholeMb) || wholeMb < 0.0D) {
            throw new IllegalArgumentException("whole millibucket heat budget must be finite and non-negative");
        }
        return Math.min(availableHeatHu, wholeMb * absorptionHuPerMb);
    }

    private static long wholeLong(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > Long.MAX_VALUE) {
            throw new IllegalArgumentException(name + " must fit in a non-negative long");
        }
        long rounded = Math.round(value);
        if (Math.abs(value - rounded) > WHOLE_MB_EPSILON) {
            throw new IllegalArgumentException(name + " must be a whole millibucket amount");
        }
        return rounded;
    }

    private static void requireWholeMb(String name, double value) {
        requireFiniteNonNegative(name, value);
        if (Math.abs(value - Math.rint(value)) > WHOLE_MB_EPSILON) {
            throw new IllegalArgumentException(name + " must be a whole millibucket amount");
        }
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static void requirePositiveFinite(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
