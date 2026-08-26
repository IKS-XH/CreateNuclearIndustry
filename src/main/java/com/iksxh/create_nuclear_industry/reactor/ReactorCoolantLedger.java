package com.iksxh.create_nuclear_industry.reactor;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * 加载器无关、单 tick 的冷却剂守恒账本。
 *
 * <p>调用方提供冷端进入量、热端本 tick 实际排出量和热库存总容量。热端阻塞时，
 * 仍可使用内部剩余空间；热库存已满则停止继续转化。流体量单位为 mB，热量单位
 * 为 HU。</p>
 */
public final class ReactorCoolantLedger {
    public static final double DEFAULT_PER_PORT_FLOW_MB = 128.0D;

    private ReactorCoolantLedger() {
    }

    /** 单个反应堆冷却剂连接的方向。 */
    public enum PortKind {
        COLD_INPUT,
        HOT_OUTPUT
    }

    /** 一条观测到的冷却剂连接；重复 connection ID 只计为一个端口。 */
    public record Port(String connectionId, PortKind kind, double availableMb) {
        public Port {
            if (connectionId == null || connectionId.isBlank() || kind == null) {
                throw new IllegalArgumentException("coolant port identity and kind are required");
            }
            requireFiniteNonNegative("port flow", availableMb);
        }

        public static Port cold(String connectionId, double availableMb) {
            return new Port(connectionId, PortKind.COLD_INPUT, availableMb);
        }

        public static Port hot(String connectionId, double availableMb) {
            return new Port(connectionId, PortKind.HOT_OUTPUT, availableMb);
        }
    }

    /** 一次 tick 去重并按端口限流后的冷却剂流量摘要。流量单位为 mB/t。 */
    public record PortSummary(
            double coldInputMb,
            double hotOutputCapacityMb,
            double coldFlowCapacityMb,
            double hotFlowCapacityMb,
            int uniqueColdPortCount,
            int uniqueHotPortCount,
            int duplicatePortCount,
            double perPortFlowMb
    ) {
        public PortSummary {
            requireFiniteNonNegative("cold input", coldInputMb);
            requireFiniteNonNegative("hot output capacity", hotOutputCapacityMb);
            requireFiniteNonNegative("cold flow capacity", coldFlowCapacityMb);
            requireFiniteNonNegative("hot flow capacity", hotFlowCapacityMb);
            requireFiniteNonNegative("per-port flow", perPortFlowMb);
            if (uniqueColdPortCount < 0 || uniqueHotPortCount < 0 || duplicatePortCount < 0) {
                throw new IllegalArgumentException("port counts must be non-negative");
            }
        }

        /** 在热量、库存和吸收能力限制之前的实际平衡流量，单位为 mB。 */
        public double balancedFlowMb() {
            return Math.min(coldInputMb, hotOutputCapacityMb);
        }
    }

    /** 由反应堆快照拥有的持久化内部冷却剂库存，单位为 mB。 */
    public record Inventory(double coldCoolantMb, double hotCoolantMb) {
        public Inventory {
            requireFiniteNonNegative("cold coolant inventory", coldCoolantMb);
            requireFiniteNonNegative("hot coolant inventory", hotCoolantMb);
        }
    }

    /** 使用固定默认单端口上限汇总冷、热端口；重复连接只计数一次且不增加吞吐。 */
    public static PortSummary summarizePorts(Collection<Port> ports) {
        return summarizePorts(ports, DEFAULT_PER_PORT_FLOW_MB);
    }

    /**
     * 使用调用方提供的单端口上限汇总唯一端口。
     * 实际流量是每个端口观测值限流后的总和；理论容量等于唯一端口数乘以上限，
     * 故意不存在额外的反应堆总流量上限。
     */
    public static PortSummary summarizePorts(Collection<Port> ports, double perPortFlowMb) {
        if (ports == null) {
            throw new IllegalArgumentException("coolant ports are required");
        }
        requireFiniteNonNegative("per-port flow", perPortFlowMb);

        Map<String, Port> unique = new TreeMap<>();
        int duplicatePortCount = 0;
        for (Port port : ports) {
            if (port == null) {
                throw new IllegalArgumentException("coolant ports must not contain null");
            }
            Port previous = unique.putIfAbsent(port.connectionId(), port);
            if (previous != null) {
                duplicatePortCount++;
                // 重复观测不得相加；保留较大的单次观测，避免输入顺序改变去重流量。
                if (port.availableMb() > previous.availableMb()
                        || (port.availableMb() == previous.availableMb()
                        && port.kind().compareTo(previous.kind()) < 0)) {
                    unique.put(port.connectionId(), port);
                }
            }
        }

        double coldInput = 0.0D;
        double hotOutputCapacity = 0.0D;
        int uniqueColdPortCount = 0;
        int uniqueHotPortCount = 0;
        for (Port port : unique.values()) {
            double capped = Math.min(port.availableMb(), perPortFlowMb);
            if (port.kind() == PortKind.COLD_INPUT) {
                coldInput = safeAdd(coldInput, capped);
                uniqueColdPortCount++;
            } else {
                hotOutputCapacity = safeAdd(hotOutputCapacity, capped);
                uniqueHotPortCount++;
            }
        }

        double coldFlowCapacity = uniqueColdPortCount * perPortFlowMb;
        double hotFlowCapacity = uniqueHotPortCount * perPortFlowMb;
        return new PortSummary(coldInput, hotOutputCapacity, coldFlowCapacity, hotFlowCapacity,
                uniqueColdPortCount, uniqueHotPortCount, duplicatePortCount, perPortFlowMb);
    }

    /** 一次 tick 的冷端输入和热端实际输出观测。流体量单位为 mB，热量单位为 HU。 */
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

    /** 一次保守转化尝试的库存、转化量和剩余热量结果。 */
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
     * 结算一次冷端到热端的转化，不修改燃料状态。
     *
     * <p>转化量严格按以下公式取最小值：</p>
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
     * <p>输出容量用于验证热端实测转移量，实测转移量本身才会释放热库存空间。
     * 暂时无法转化的输入保留在冷库存；转化量先加入热库存，再扣除实测热端输出。</p>
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
