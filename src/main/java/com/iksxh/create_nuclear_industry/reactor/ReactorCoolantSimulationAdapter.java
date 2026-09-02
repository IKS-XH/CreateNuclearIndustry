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
            ReactorCoolantLedger.Settlement settlement,
            double quantizedHeatRemainderHu
    ) {
        /** 兼容尚未暴露量化余数的旧调用方；旧结果默认没有安全余数。 */
        public Result(
                ReactorSnapshot nextSnapshot,
                ReactorCoolantLedger.Settlement settlement
        ) {
            this(nextSnapshot, settlement, 0.0D);
        }

        public Result {
            if (nextSnapshot == null || settlement == null) {
                throw new IllegalArgumentException("coolant conversion result is required");
            }
            requireFiniteNonNegative("quantized heat remainder", quantizedHeatRemainderHu);
            if (quantizedHeatRemainderHu
                    > settlement.remainingHeatHu() + WHOLE_MB_EPSILON) {
                throw new IllegalArgumentException(
                        "quantized heat remainder cannot exceed remaining heat");
            }
        }
    }

    /** 结算一次正式冷却剂转化并返回新的权威快照；输入快照保持只读。 */
    public static Result settle(ReactorSnapshot previous, TickInput input) {
        if (previous == null || input == null) {
            throw new IllegalArgumentException("previous snapshot and coolant input are required");
        }

        // 正式快照以整数 mB 保存流体；先得到本 tick 可用的整数容量，随后只把
        // 不足一个 mB 的量化余数标记为安全余热，真实容量不足仍必须进入损伤结算。
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
        double availableCold = previous.coldCoolantMb() + input.coldInAcceptedMb();
        double hotSpaceAfterOutput = input.hotInventoryCapacityMb() - previous.hotCoolantMb()
                + input.hotOutActualMb();
        double integerCoolingCapacityMb = Math.min(availableCold, Math.max(0.0D, hotSpaceAfterOutput));
        double quantizationRemainder = Math.max(0.0D,
                input.availableHeatHu() - wholeMillibucketHeat);
        double quantizedHeatRemainder = hasCapacityForQuantizedRemainder(
                input.availableHeatHu(), integerCoolingCapacityMb)
                ? Math.min(resultSettlement.remainingHeatHu(), quantizationRemainder) : 0.0D;
        return new Result(
                previous.withCoolantInventories(nextCold, nextHot),
                resultSettlement,
                quantizedHeatRemainder
        );
    }

    /** 判断当前是否存在真实整数 mB 冷却能力，使小数余数可以从真实短缺中分离。 */
    private static boolean hasCapacityForQuantizedRemainder(
            double availableHeatHu,
            double integerCoolingCapacityMb
    ) {
        // 只要本 tick 存在至少一个真实 mB 的冷却能力，就可以把热量除以吸热量后
        // 的小数部分单独归入量化余数；能力为零时（例如冷库存为空且热端堵塞），
        // 0.49 HU 仍然是完整的真实短缺，不能获得免损伤标记。
        return availableHeatHu > WHOLE_MB_EPSILON
                && integerCoolingCapacityMb > WHOLE_MB_EPSILON;
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
