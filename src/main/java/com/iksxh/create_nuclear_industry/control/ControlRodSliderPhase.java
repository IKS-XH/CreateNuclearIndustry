package com.iksxh.create_nuclear_industry.control;

/** 一次客户端控制棒拖动手势在线上传输的阶段。 */
public enum ControlRodSliderPhase {
    START(0),
    PREVIEW(1),
    COMMIT(2),
    CANCEL(3);

    private final int wireCode;

    ControlRodSliderPhase(int wireCode) {
        this.wireCode = wireCode;
    }

    /** 返回稳定的协议阶段编号。 */
    public int wireCode() {
        return wireCode;
    }

    /** 将协议编号转换为阶段；未知编号返回 {@code null}，由服务端拒绝。 */
    public static ControlRodSliderPhase fromWireCode(int wireCode) {
        for (ControlRodSliderPhase phase : values()) {
            if (phase.wireCode == wireCode) {
                return phase;
            }
        }
        return null;
    }
}
