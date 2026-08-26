package com.iksxh.create_nuclear_industry.p0probe.numeric;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** P0 单 tick 的冷却端口、局部移热能力、内部高度和 SCRAM 输入。 */
public record ReactorTickInput(
        List<CoolantPort> coolantPorts,
        Map<ColumnKey, Double> localCoolingCapacityHu,
        double internalHeight,
        boolean scram
) {
    public ReactorTickInput {
        if (coolantPorts == null || localCoolingCapacityHu == null) {
            throw new IllegalArgumentException("tick inputs are required");
        }
        if (!Double.isFinite(internalHeight) || internalHeight <= 0) {
            throw new IllegalArgumentException("internalHeight must be finite and positive");
        }
        List<CoolantPort> orderedPorts = new ArrayList<>(coolantPorts);
        orderedPorts.sort(Comparator.comparing(CoolantPort::connectionId).thenComparing(CoolantPort::kind));
        coolantPorts = List.copyOf(orderedPorts);
        TreeMap<ColumnKey, Double> capacities = new TreeMap<>();
        localCoolingCapacityHu.forEach((key, value) -> {
            if (key == null || value == null || !Double.isFinite(value) || value < 0) {
                throw new IllegalArgumentException("local coolant capacity must be finite and non-negative");
            }
            capacities.put(key, value);
        });
        localCoolingCapacityHu = Collections.unmodifiableMap(capacities);
    }

    /** 创建不提供冷却且未请求 SCRAM 的 P0 输入。 */
    public static ReactorTickInput noCooling(double internalHeight) {
        return new ReactorTickInput(List.of(), Map.of(), internalHeight, false);
    }

    /** 复制输入并替换 SCRAM 电平。 */
    public ReactorTickInput withScram(boolean value) {
        return new ReactorTickInput(coolantPorts, localCoolingCapacityHu, internalHeight, value);
    }
}
