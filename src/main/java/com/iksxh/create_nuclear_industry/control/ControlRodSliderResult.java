package com.iksxh.create_nuclear_industry.control;

/** 与线协议响应分离的服务端规则结果，便于 GameTest 直接验证校验与状态转换。 */
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
    /** 返回状态码是否表示服务端接受了本次请求。 */
    public boolean accepted() {
        return status.accepted();
    }

    /** 从请求中保留阶段、列和拖动 ID，构造不接受的结果。 */
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

    /** 复制结果并附加服务端当前目标深度，用于客户端回滚展示。 */
    public ControlRodSliderResult withAuthoritativeDepth(int columnX, int columnZ, int depthPercent) {
        return new ControlRodSliderResult(
                phase, status, columnX, columnZ, depthPercent, dragId, reason, true);
    }
}
