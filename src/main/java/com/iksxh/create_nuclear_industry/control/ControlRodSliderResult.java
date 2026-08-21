package com.iksxh.create_nuclear_industry.control;

/** Server result kept separate from the wire response so GameTests can exercise the rules directly. */
public record ControlRodSliderResult(
        ControlRodSliderPhase phase,
        ControlRodSliderStatus status,
        int columnX,
        int columnZ,
        int depthPercent,
        long dragId,
        String reason,
        boolean authoritativeDepth
) {
    public boolean accepted() {
        return status.accepted();
    }

    public static ControlRodSliderResult rejected(
            ControlRodSliderPayload payload,
            ControlRodSliderStatus status,
            String reason
    ) {
        ControlRodSliderPhase phase = payload == null || payload.phase() == null
                ? ControlRodSliderPhase.CANCEL : payload.phase();
        int columnX = payload == null ? -1 : payload.columnX();
        int columnZ = payload == null ? -1 : payload.columnZ();
        int depth = payload == null ? 0 : payload.depthPercent();
        long dragId = payload == null ? 0L : payload.dragId();
        return new ControlRodSliderResult(phase, status, columnX, columnZ, depth, dragId, reason, false);
    }

    public ControlRodSliderResult withAuthoritativeDepth(int columnX, int columnZ, int depthPercent) {
        return new ControlRodSliderResult(
                phase, status, columnX, columnZ, depthPercent, dragId, reason, true);
    }
}
