package com.iksxh.create_nuclear_industry.p0probe.numeric;

public record CoolantPort(String connectionId, Kind kind, double availableMb) {
    public enum Kind { COLD_INPUT, HOT_OUTPUT }

    public CoolantPort {
        if (connectionId == null || connectionId.isBlank() || kind == null) {
            throw new IllegalArgumentException("coolant port identity and kind are required");
        }
        if (!Double.isFinite(availableMb) || availableMb < 0) {
            throw new IllegalArgumentException("coolant port amount must be finite and non-negative");
        }
    }

    public static CoolantPort cold(String id, double availableMb) {
        return new CoolantPort(id, Kind.COLD_INPUT, availableMb);
    }

    public static CoolantPort hot(String id, double outputCapacityMb) {
        return new CoolantPort(id, Kind.HOT_OUTPUT, outputCapacityMb);
    }
}
