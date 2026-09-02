package com.iksxh.create_nuclear_industry.control;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证控制棒滑块客户端只接受匹配会话的最终响应。 */
class ControlRodSliderClientResponsePolicyTest {
    private static final BlockPos DRIVE = new BlockPos(4, 5, 6);

    @Test
    void acceptedStartAndPreviewNeverUpdateTheDisplay() {
        ControlRodSliderClientResponsePolicy.Session session = session(17L);

        assertEquals(ControlRodSliderClientResponsePolicy.Decision.IGNORE,
                ControlRodSliderClientResponsePolicy.decide(session, response(
                        ControlRodSliderPhase.START, ControlRodSliderStatus.ACCEPTED, 17L, 80, true)));
        assertEquals(ControlRodSliderClientResponsePolicy.Decision.IGNORE,
                ControlRodSliderClientResponsePolicy.decide(session, response(
                        ControlRodSliderPhase.PREVIEW, ControlRodSliderStatus.ACCEPTED, 17L, 20, true)));
    }

    @Test
    void lateResponseFromOldDragIsIgnoredForNewSession() {
        ControlRodSliderClientResponsePolicy.Session current = session(22L);

        assertEquals(ControlRodSliderClientResponsePolicy.Decision.IGNORE,
                ControlRodSliderClientResponsePolicy.decide(current, response(
                        ControlRodSliderPhase.COMMIT, ControlRodSliderStatus.ACCEPTED, 21L, 90, true)));
        assertEquals(ControlRodSliderClientResponsePolicy.Decision.APPLY_FINAL_DISPLAY,
                ControlRodSliderClientResponsePolicy.decide(current, response(
                        ControlRodSliderPhase.COMMIT, ControlRodSliderStatus.ACCEPTED, 22L, 60, true)));
    }

    @Test
    void rejectedFinalResponseRollsBackOnlyAfterTheSessionEnds() {
        ControlRodSliderClientResponsePolicy.Session session = session(31L);

        assertEquals(ControlRodSliderClientResponsePolicy.Decision.APPLY_FINAL_DISPLAY,
                ControlRodSliderClientResponsePolicy.decide(session, response(
                        ControlRodSliderPhase.COMMIT, ControlRodSliderStatus.SCRAM_LOCKED,
                        31L, 100, true)));
        assertEquals(ControlRodSliderClientResponsePolicy.Decision.CLEAR_SESSION,
                ControlRodSliderClientResponsePolicy.decide(session, response(
                        ControlRodSliderPhase.COMMIT, ControlRodSliderStatus.INVALID_STATE,
                        31L, 0, false)));
    }

    @Test
    void directCreateCommitCanApplyWhenNoCustomDragSessionExists() {
        assertEquals(ControlRodSliderClientResponsePolicy.Decision.APPLY_FINAL_DISPLAY,
                ControlRodSliderClientResponsePolicy.decide(null, response(
                        ControlRodSliderPhase.COMMIT, ControlRodSliderStatus.ACCEPTED, 0L, 60, true)));
    }

    private static ControlRodSliderClientResponsePolicy.Session session(long dragId) {
        return new ControlRodSliderClientResponsePolicy.Session(DRIVE, dragId);
    }

    private static ControlRodSliderResponsePayload response(
            ControlRodSliderPhase phase,
            ControlRodSliderStatus status,
            long dragId,
            int depthPercent,
            boolean authoritativeDepth
    ) {
        return new ControlRodSliderResponsePayload(
                DRIVE, 1, 2, depthPercent, phase.wireCode(), status.wireCode(), dragId, authoritativeDepth);
    }
}
