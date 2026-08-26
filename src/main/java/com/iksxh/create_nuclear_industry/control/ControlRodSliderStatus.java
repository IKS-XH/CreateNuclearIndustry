package com.iksxh.create_nuclear_industry.control;

/** 服务端权威滑块协议返回的稳定结果码；线上的数值不能随意重排。 */
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
    OUT_OF_REACH(19, false),
    SCRAM_LOCKED(20, false);

    private final int wireCode;
    private final boolean accepted;

    ControlRodSliderStatus(int wireCode, boolean accepted) {
        this.wireCode = wireCode;
        this.accepted = accepted;
    }

    /** 返回稳定的线协议状态码。 */
    public int wireCode() {
        return wireCode;
    }

    /** 判断该状态是否表示请求已被服务端接受。 */
    public boolean accepted() {
        return accepted;
    }

    /** 将未知状态码安全映射为 INVALID_PACKET。 */
    public static ControlRodSliderStatus fromWireCode(int wireCode) {
        for (ControlRodSliderStatus status : values()) {
            if (status.wireCode == wireCode) {
                return status;
            }
        }
        return INVALID_PACKET;
    }
}
