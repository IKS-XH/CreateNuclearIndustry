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

    /**
     * Create 长按面板不一定调用 behaviour 的短交互回调，因此从第一次悬停回调开始
     * 拖动；先等待服务端 START 响应建立初始光标值，再发送预览。
     */
    public static boolean ensureStarted(BlockPos drivePos, int columnX, int columnZ, int currentDepth) {
        if (active != null && active.drivePos().equals(drivePos)) {
            return false;
        }
        begin(drivePos, columnX, columnZ, currentDepth);
        return true;
    }

    /** 在活动会话内发送客户端预览值；预览不写入服务端快照。 */
    public static void preview(BlockPos drivePos, int columnX, int columnZ, int depthPercent) {
        if (active == null || !active.drivePos().equals(drivePos)) {
            return;
        }
        PacketDistributor.sendToServer(ControlRodSliderPayload.preview(
                drivePos, columnX, columnZ, depthPercent, active.dragId()));
    }

    /** 发送提交请求并结束本地拖动会话。 */
    public static void commit(BlockPos drivePos, int columnX, int columnZ, int depthPercent) {
        if (active == null || !active.drivePos().equals(drivePos)) {
            return;
        }
        PacketDistributor.sendToServer(ControlRodSliderPayload.commit(
                drivePos, columnX, columnZ, depthPercent, active.dragId()));
        active = null;
    }

    /** 发送取消请求并丢弃本地拖动会话。 */
    public static void cancel(BlockPos drivePos, int columnX, int columnZ) {
        if (active == null || !active.drivePos().equals(drivePos)) {
            return;
        }
        PacketDistributor.sendToServer(ControlRodSliderPayload.cancel(
                drivePos, columnX, columnZ, active.dragId()));
        active = null;
    }

    /** 根据服务端结果结束已接受、被拒绝或已提交/取消的本地会话。 */
    static void finishFromServer(ControlRodSliderResponsePayload response) {
        if (active == null || response == null || !active.drivePos().equals(response.drivePos())) {
            return;
        }
        if (!response.accepted() || response.phase() == ControlRodSliderPhase.COMMIT
                || response.phase() == ControlRodSliderPhase.CANCEL) {
            active = null;
        }
    }

    private record ActiveDrag(BlockPos drivePos, int columnX, int columnZ, long dragId) {
    }
}
