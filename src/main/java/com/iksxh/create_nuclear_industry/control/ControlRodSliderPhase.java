package com.iksxh.create_nuclear_industry.control;

/** Wire phases for one client-side control-rod drag gesture. */
public enum ControlRodSliderPhase {
    START(0),
    PREVIEW(1),
    COMMIT(2),
    CANCEL(3);

    private final int wireCode;

    ControlRodSliderPhase(int wireCode) {
        this.wireCode = wireCode;
    }

    public int wireCode() {
        return wireCode;
    }

    public static ControlRodSliderPhase fromWireCode(int wireCode) {
        for (ControlRodSliderPhase phase : values()) {
            if (phase.wireCode == wireCode) {
                return phase;
            }
        }
        return null;
    }
}
