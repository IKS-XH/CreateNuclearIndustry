package com.iksxh.create_nuclear_industry.p0probe.numeric;

/** P0 原型观测到的一条冷却剂连接，流体量单位为 mB。 */
public record CoolantPort(String connectionId, Kind kind, double availableMb) {
    /** P0 冷却剂连接的输入/输出方向。 */
    public enum Kind { COLD_INPUT, HOT_OUTPUT }

    public CoolantPort {
        if (connectionId == null || connectionId.isBlank() || kind == null) {
            throw new IllegalArgumentException("coolant port identity and kind are required");
        }
        if (!Double.isFinite(availableMb) || availableMb < 0) {
            throw new IllegalArgumentException("coolant port amount must be finite and non-negative");
        }
    }

    /** 创建冷端输入连接。 */
    public static CoolantPort cold(String id, double availableMb) {
        return new CoolantPort(id, Kind.COLD_INPUT, availableMb);
    }

    /** 创建热端输出连接。 */
    public static CoolantPort hot(String id, double outputCapacityMb) {
        return new CoolantPort(id, Kind.HOT_OUTPUT, outputCapacityMb);
    }
}
