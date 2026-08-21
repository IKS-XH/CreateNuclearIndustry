package com.iksxh.create_nuclear_industry.control;

/** Stable result codes returned by the server-authoritative slider protocol. */
public enum ControlRodSliderStatus {
    ACCEPTED(0, true),
    CANCELLED(1, true),
    INVALID_PACKET(10, false),
    INVALID_PLAYER(11, false),
    INVALID_PHASE(12, false),
    INVALID_RANGE(13, false),
    INVALID_ROW(14, false),
    INVALID_STRUCTURE(15, false),
    INVALID_COLUMN(16, false),
    INVALID_STATE(17, false),
    STALE_SESSION(18, false),
    OUT_OF_REACH(19, false);

    private final int wireCode;
    private final boolean accepted;

    ControlRodSliderStatus(int wireCode, boolean accepted) {
        this.wireCode = wireCode;
        this.accepted = accepted;
    }

    public int wireCode() {
        return wireCode;
    }

    public boolean accepted() {
        return accepted;
    }

    public static ControlRodSliderStatus fromWireCode(int wireCode) {
        for (ControlRodSliderStatus status : values()) {
            if (status.wireCode == wireCode) {
                return status;
            }
        }
        return INVALID_PACKET;
    }
}
