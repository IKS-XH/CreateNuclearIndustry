package com.iksxh.create_nuclear_industry.control;

import net.minecraft.core.BlockPos;

/**
 * 控制棒滑块客户端响应策略。
 *
 * <p>该类不接触 Minecraft 客户端对象，只决定网络响应能否更新最终展示缓存。
 * 拖动中的面板值和物理鼠标由 Create 本地维护；服务端响应只有在最终阶段且匹配
 * 当前驱动器与拖动 ID 时，才可以结束当前会话并更新展示缓存。</p>
 */
final class ControlRodSliderClientResponsePolicy {
    private ControlRodSliderClientResponsePolicy() {
    }

    /** 响应处理动作；IGNORE 不得触碰驱动器展示值或会话。 */
    enum Decision {
        IGNORE(false, false),
        CLEAR_SESSION(false, true),
        APPLY_FINAL_DISPLAY(true, true);

        private final boolean applyFinalDisplay;
        private final boolean clearSession;

        Decision(boolean applyFinalDisplay, boolean clearSession) {
            this.applyFinalDisplay = applyFinalDisplay;
            this.clearSession = clearSession;
        }

        boolean applyFinalDisplay() {
            return applyFinalDisplay;
        }

        boolean clearSession() {
            return clearSession;
        }
    }

    /** 客户端当前拖动会话的最小匹配信息。dragId 在一次客户端进程内单调递增。 */
    record Session(BlockPos drivePos, long dragId) {
        Session {
            drivePos = drivePos == null ? null : drivePos.immutable();
        }
    }

    /**
     * 判断服务端响应的应用范围。
     *
     * <p>START/PREVIEW 只用于服务端会话确认，永远不能回写滑块值。COMMIT/CANCEL
     * 必须匹配当前同一驱动器的 dragId；否则视为旧会话迟到响应。无活动会话时，
     * 只有携带成功或权威深度的最终响应可以更新展示缓存。</p>
     */
    static Decision decide(Session active, ControlRodSliderResponsePayload response) {
        if (response == null || response.drivePos() == null) {
            return Decision.IGNORE;
        }

        ControlRodSliderPhase phase = response.phase();
        if (phase == null) {
            return Decision.IGNORE;
        }
        if (phase == ControlRodSliderPhase.START || phase == ControlRodSliderPhase.PREVIEW) {
            if (active != null && active.drivePos() != null
                    && active.drivePos().equals(response.drivePos())
                    && active.dragId() == response.dragId()
                    && !response.accepted()) {
                return Decision.CLEAR_SESSION;
            }
            return Decision.IGNORE;
        }

        boolean hasFinalValue = response.accepted() || response.hasAuthoritativeDepth();
        if (active != null && active.drivePos() != null
                && active.drivePos().equals(response.drivePos())) {
            if (active.dragId() != response.dragId()) {
                return Decision.IGNORE;
            }
            return hasFinalValue ? Decision.APPLY_FINAL_DISPLAY : Decision.CLEAR_SESSION;
        }

        return hasFinalValue ? Decision.APPLY_FINAL_DISPLAY : Decision.IGNORE;
    }
}
