package com.iksxh.create_nuclear_industry.control;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only transport adapter; it sends requests and never mutates a reactor snapshot. */
public final class ControlRodSliderClientAdapter {
    private static long nextDragId = 1L;
    private static ActiveDrag active;

    private ControlRodSliderClientAdapter() {
    }

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
     * Long-press Create panels do not call the behaviour's short-interact
     * hook. Start their authoritative drag from the first hover callback, but
     * let the server START response establish the initial cursor value before
     * sending a preview.
     */
    public static boolean ensureStarted(BlockPos drivePos, int columnX, int columnZ, int currentDepth) {
        if (active != null && active.drivePos().equals(drivePos)) {
            return false;
        }
        begin(drivePos, columnX, columnZ, currentDepth);
        return true;
    }

    public static void preview(BlockPos drivePos, int columnX, int columnZ, int depthPercent) {
        if (active == null || !active.drivePos().equals(drivePos)) {
            return;
        }
        PacketDistributor.sendToServer(ControlRodSliderPayload.preview(
                drivePos, columnX, columnZ, depthPercent, active.dragId()));
    }

    public static void commit(BlockPos drivePos, int columnX, int columnZ, int depthPercent) {
        if (active == null || !active.drivePos().equals(drivePos)) {
            return;
        }
        PacketDistributor.sendToServer(ControlRodSliderPayload.commit(
                drivePos, columnX, columnZ, depthPercent, active.dragId()));
        active = null;
    }

    public static void cancel(BlockPos drivePos, int columnX, int columnZ) {
        if (active == null || !active.drivePos().equals(drivePos)) {
            return;
        }
        PacketDistributor.sendToServer(ControlRodSliderPayload.cancel(
                drivePos, columnX, columnZ, active.dragId()));
        active = null;
    }

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
