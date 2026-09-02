package com.iksxh.create_nuclear_industry.control;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/** 客户端传输适配器；只发送请求，永远不直接修改反应堆快照。 */
public final class ControlRodSliderClientAdapter {
    private static long nextDragId = 1L;
    private static ActiveDrag active;

    private ControlRodSliderClientAdapter() {
    }

    /** 为一次客户端拖动建立非权威会话并发送 START 请求。 */
    public static void begin(BlockPos drivePos, int columnX, int columnZ, int currentDepth) {
        if (active != null && !active.drivePos().equals(drivePos)) {
            cancel(active.drivePos(), active.columnX(), active.columnZ());
        }
        long dragId = nextDragId++;
        if (dragId == 0L) {
            dragId = nextDragId++;
        }
        active = new ActiveDrag(drivePos.immutable(), columnX, columnZ, dragId);
        PacketDistributor.sendToServer(ControlRodSliderPayload.start(
                drivePos, columnX, columnZ, currentDepth, dragId));
    }

    /** Create 长按面板从第一次悬停回调开始建立临时服务端会话。 */
    public static boolean ensureStarted(BlockPos drivePos, int columnX, int columnZ, int currentDepth) {
        if (active != null && active.drivePos().equals(drivePos)) {
            return false;
        }
        begin(drivePos, columnX, columnZ, currentDepth);
        return true;
    }

    /** 发送自定义提交请求；会话保留到服务端最终响应，以便校验 dragId。 */
    public static void commit(BlockPos drivePos, int columnX, int columnZ, int depthPercent) {
        if (active == null || !active.drivePos().equals(drivePos)) {
            return;
        }
        PacketDistributor.sendToServer(ControlRodSliderPayload.commit(
                drivePos, columnX, columnZ, depthPercent, active.dragId()));
    }

    /** 发送取消请求；会话保留到服务端最终响应，以便校验 dragId。 */
    public static void cancel(BlockPos drivePos, int columnX, int columnZ) {
        if (active == null || !active.drivePos().equals(drivePos)) {
            return;
        }
        PacketDistributor.sendToServer(ControlRodSliderPayload.cancel(
                drivePos, columnX, columnZ, active.dragId()));
    }

    /** 清理面板异常关闭、玩家退出或其他客户端上下文切换留下的临时会话。 */
    static void clearSession() {
        active = null;
    }

    /**
     * 应用服务端响应策略并按需结束当前会话。
     *
     * @return 是否允许本次响应更新驱动器的最终展示缓存
     */
    static boolean applyResponsePolicy(ControlRodSliderResponsePayload response) {
        ControlRodSliderClientResponsePolicy.Session session = active == null
                ? null
                : new ControlRodSliderClientResponsePolicy.Session(active.drivePos(), active.dragId());
        ControlRodSliderClientResponsePolicy.Decision decision =
                ControlRodSliderClientResponsePolicy.decide(session, response);
        if (decision.clearSession() && matchesActiveSession(response)) {
            active = null;
        }
        return decision.applyFinalDisplay();
    }

    private static boolean matchesActiveSession(ControlRodSliderResponsePayload response) {
        return active != null
                && response != null
                && active.drivePos().equals(response.drivePos())
                && active.dragId() == response.dragId();
    }

    private record ActiveDrag(BlockPos drivePos, int columnX, int columnZ, long dragId) {
    }
}
