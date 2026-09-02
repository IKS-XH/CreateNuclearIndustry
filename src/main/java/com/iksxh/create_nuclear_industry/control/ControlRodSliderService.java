package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.ControlRodStateTransitions;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端专属控制边界。
 *
 * <p>客户端提供的列提示、深度和拖动阶段都必须重新校验；驱动器只作为交互目标，
 * 不保存第二份反应堆快照。提交成功后，只有仪表端口的快照会发生权威状态变化。</p>
 */
public final class ControlRodSliderService {
    private static final int MAX_INTERACTION_RANGE = 20;
    private static final Map<UUID, DragSession> ACTIVE_DRAGS = new HashMap<>();

    private ControlRodSliderService() {
    }

    /** 统一处理滑块阶段，并在拒绝时尽可能返回服务端当前目标深度。 */
    public static ControlRodSliderResult handle(Player player, ControlRodSliderPayload payload) {
        if (payload == null || payload.phase() == null) {
            return restoreAuthoritativeDepth(player, payload,
                    ControlRodSliderResult.rejected(payload, ControlRodSliderStatus.INVALID_PHASE,
                    "unknown slider phase"));
        }
        ControlRodSliderResult result = switch (payload.phase()) {
            case START -> start(player, payload);
            case PREVIEW -> preview(player, payload);
            case COMMIT -> commit(player, payload);
            case CANCEL -> cancel(player, payload);
        };
        return restoreAuthoritativeDepth(player, payload, result);
    }

    /** Create 最终 ValueSettingsPacket 使用的提交入口。 */
    public static ControlRodSliderResult commitFromCreate(
            Player player,
            BlockPos drivePos,
            int row,
            int depthPercent
    ) {
        long dragId = activeDragIdForCreate(player, drivePos);
        ControlRodSliderPayload payload = new ControlRodSliderPayload(
                drivePos, -1, -1, row, depthPercent,
                ControlRodSliderPhase.COMMIT.wireCode(), dragId);
        return handle(player, payload);
    }

    /** Create 提交包没有自定义 dragId；从同一驱动器的临时会话补齐响应关联 ID。 */
    private static long activeDragIdForCreate(Player player, BlockPos drivePos) {
        if (player == null || drivePos == null) {
            return 0L;
        }
        DragSession session = ACTIVE_DRAGS.get(player.getUUID());
        return session != null && drivePos.equals(session.drivePos()) ? session.dragId() : 0L;
    }

    /** 清除玩家退出或会话结束后的活动拖动状态。 */
    public static void clearSession(Player player) {
        if (player != null) {
            ACTIVE_DRAGS.remove(player.getUUID());
        }
    }

    /** 返回当前服务端内存中的活动拖动会话数量，供回归测试观察。 */
    public static int activeSessionCount() {
        return ACTIVE_DRAGS.size();
    }

    private static ControlRodSliderResult start(Player player, ControlRodSliderPayload payload) {
        Validation validation = validate(player, payload, true);
        if (!validation.valid()) {
            return validation.rejection();
        }
        if (payload.dragId() == 0L) {
            return reject(payload, ControlRodSliderStatus.INVALID_PACKET,
                    "drag start requires a non-zero drag id");
        }

        Target target = validation.target();
        ACTIVE_DRAGS.put(player.getUUID(), new DragSession(
                target.drive().getBlockPos(), target.column(), payload.dragId()));
        int current = toPercent(target.state().targetDepth());
        return accepted(payload, target.column(), current);
    }

    private static ControlRodSliderResult preview(Player player, ControlRodSliderPayload payload) {
        Validation validation = validate(player, payload, true);
        if (!validation.valid()) {
            return validation.rejection();
        }
        Target target = validation.target();
        DragSession session = ACTIVE_DRAGS.get(player.getUUID());
        if (session == null || !session.matches(target, payload.dragId())) {
            return reject(payload, ControlRodSliderStatus.STALE_SESSION,
                    "slider preview has no matching active drag");
        }
        return accepted(payload, target.column(), payload.depthPercent());
    }

    private static ControlRodSliderResult commit(Player player, ControlRodSliderPayload payload) {
        Validation validation = validate(player, payload, true);
        if (!validation.valid()) {
            return validation.rejection();
        }
        Target target = validation.target();
        DragSession session = ACTIVE_DRAGS.get(player.getUUID());
        if (payload.dragId() != 0L) {
            if (session == null || !session.matches(target, payload.dragId())) {
                return reject(payload, ControlRodSliderStatus.STALE_SESSION,
                        "slider commit has no matching active drag");
            }
        } else if (session != null && !session.matches(target, session.dragId())) {
            return reject(payload, ControlRodSliderStatus.STALE_SESSION,
                    "another slider drag is active for this player");
        }

        ReactorSnapshot snapshot = target.owner().snapshot();
        ControlRodColumnState current = snapshot.controlRodColumns().get(target.column());
        if (current == null || current.jammed()) {
            return reject(payload, ControlRodSliderStatus.INVALID_STATE,
                    "control rod is no longer movable");
        }
        ControlRodColumnState updated = ControlRodStateTransitions.requestTargetDepth(
                current, payload.depthPercent() / 100.0D);
        Map<CoreColumnPosition, ControlRodColumnState> columns = new java.util.TreeMap<>(
                snapshot.controlRodColumns());
        columns.put(target.column(), updated);
        target.owner().setSnapshot(snapshot.withColumns(snapshot.fuelColumns(), columns));
        target.drive().setServerDisplayedDepthPercent(payload.depthPercent());
        ACTIVE_DRAGS.remove(player.getUUID());
        return accepted(payload, target.column(), payload.depthPercent());
    }

    private static ControlRodSliderResult cancel(Player player, ControlRodSliderPayload payload) {
        Validation validation = validate(player, payload, false);
        if (!validation.valid()) {
            return validation.rejection();
        }
        Target target = validation.target();
        DragSession session = ACTIVE_DRAGS.get(player.getUUID());
        if (session == null || !session.matches(target, payload.dragId())) {
            return reject(payload, ControlRodSliderStatus.STALE_SESSION,
                    "slider cancel has no matching active drag");
        }
        ACTIVE_DRAGS.remove(player.getUUID());
        return new ControlRodSliderResult(
                ControlRodSliderPhase.CANCEL,
                ControlRodSliderStatus.CANCELLED,
                target.column().x(),
                target.column().z(),
                toPercent(target.state().targetDepth()),
                payload.dragId(),
                "slider drag cancelled",
                true
        );
    }

    private static Validation validate(Player player, ControlRodSliderPayload payload,
                                       boolean requireMovable) {
        if (player == null) {
            return invalid(payload, ControlRodSliderStatus.INVALID_PLAYER, "player is required");
        }
        if (payload == null || payload.drivePos() == null) {
            return invalid(payload, ControlRodSliderStatus.INVALID_PACKET, "drive position is required");
        }
        if (payload.row() != 0) {
            return invalid(payload, ControlRodSliderStatus.INVALID_ROW, "only slider row 0 is valid");
        }
        if (payload.depthPercent() < 0 || payload.depthPercent() > 100) {
            return invalid(payload, ControlRodSliderStatus.INVALID_RANGE,
                    "target depth must be in the inclusive range 0..100");
        }
        if (!validColumnHint(payload.columnX(), payload.columnZ())) {
            return invalid(payload, ControlRodSliderStatus.INVALID_COLUMN,
                    "control rod column coordinates must be both -1 or both in 0..2");
        }
        if (player.isSpectator() || !(player.level() instanceof ServerLevel level)) {
            return invalid(payload, ControlRodSliderStatus.INVALID_PLAYER,
                    "only a live server player may operate the slider");
        }
        if (!player.canInteractWithBlock(payload.drivePos(), MAX_INTERACTION_RANGE)) {
            return invalid(payload, ControlRodSliderStatus.OUT_OF_REACH,
                    "control rod drive is out of reach");
        }
        Target target = findTarget(level, payload.drivePos());
        if (target == null) {
            return invalid(payload, ControlRodSliderStatus.INVALID_STRUCTURE,
                    "drive is not bound to one valid reactor structure");
        }
        if (!matchesColumnHint(payload, target.column())) {
            return invalid(payload, ControlRodSliderStatus.INVALID_COLUMN,
                    "client column does not match the server structure mapping");
        }
        if (requireMovable && target.owner().snapshot().scramActive()) {
            return invalid(payload, ControlRodSliderStatus.SCRAM_LOCKED,
                    "control rod slider is locked while SCRAM is active");
        }
        if (requireMovable && target.state().jammed()) {
            return invalid(payload, ControlRodSliderStatus.INVALID_STATE,
                    "control rod is jammed and read-only");
        }
        return new Validation(target, null);
    }

    private static Target findTarget(ServerLevel level, BlockPos drivePos) {
        BlockEntity driveEntity = level.getBlockEntity(drivePos);
        if (!(driveEntity instanceof ControlRodDriveBlockEntity drive)) {
            return null;
        }

        Target found = null;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockEntity candidate = level.getBlockEntity(drivePos.offset(dx, dy, dz));
                    if (!(candidate instanceof ReactorInstrumentPortBlockEntity owner)
                            || !owner.structureValid()
                            || owner.structureOrigin() == null) {
                        continue;
                    }
                    for (Map.Entry<CoreColumnPosition, ReactorStructureDefinition.ColumnMapping> entry
                            : owner.structureScan().columns().entrySet()) {
                        ReactorStructureDefinition.ColumnMapping mapping = entry.getValue();
                        if (mapping.type() != ReactorStructureDefinition.ColumnType.CONTROL_ROD) {
                            continue;
                        }
                        BlockPos mappedDrive = owner.structureOrigin().offset(
                                mapping.capPosition().x(), mapping.capPosition().y(), mapping.capPosition().z());
                        if (!mappedDrive.equals(drivePos)) {
                            continue;
                        }
                        ControlRodColumnState state = owner.snapshot().controlRodColumns().get(entry.getKey());
                        if (state == null) {
                            return null;
                        }
                        Target next = new Target(owner, drive, entry.getKey(), state);
                        if (found != null && !found.sameOwner(next)) {
                            return null;
                        }
                        found = next;
                    }
                }
            }
        }
        return found;
    }

    private static boolean validColumnHint(int columnX, int columnZ) {
        return (columnX == -1 && columnZ == -1)
                || (columnX >= 0 && columnX < CoreColumnPosition.GRID_SIZE
                && columnZ >= 0 && columnZ < CoreColumnPosition.GRID_SIZE);
    }

    private static boolean matchesColumnHint(ControlRodSliderPayload payload, CoreColumnPosition column) {
        return payload.columnX() == -1
                || (payload.columnX() == column.x() && payload.columnZ() == column.z());
    }

    private static ControlRodSliderResult accepted(ControlRodSliderPayload payload,
                                                    CoreColumnPosition column, int depthPercent) {
        return new ControlRodSliderResult(
                payload.phase(),
                ControlRodSliderStatus.ACCEPTED,
                column.x(),
                column.z(),
                depthPercent,
                payload.dragId(),
                "accepted",
                true
        );
    }

    /**
     * 被拒绝的请求不得让客户端预览停留在服务端未接受的值上。若请求仍能定位到
     * 可到达且有效的驱动器，返回其当前目标作为回滚值；否则客户端保留最近缓存。
     */
    private static ControlRodSliderResult restoreAuthoritativeDepth(
            Player player,
            ControlRodSliderPayload payload,
            ControlRodSliderResult result
    ) {
        if (result == null || result.accepted() || result.authoritativeDepth()
                || player == null || payload == null || payload.drivePos() == null
                || !(player.level() instanceof ServerLevel level)
                || player.isSpectator()
                || !player.canInteractWithBlock(payload.drivePos(), MAX_INTERACTION_RANGE)) {
            return result;
        }
        Target target = findTarget(level, payload.drivePos());
        return target == null
                ? result
                : result.withAuthoritativeDepth(
                        target.column().x(), target.column().z(), toPercent(target.state().targetDepth()));
    }

    private static ControlRodSliderResult reject(ControlRodSliderPayload payload,
                                                  ControlRodSliderStatus status, String reason) {
        return ControlRodSliderResult.rejected(payload, status, reason);
    }

    private static Validation invalid(ControlRodSliderPayload payload,
                                      ControlRodSliderStatus status, String reason) {
        return new Validation(null, reject(payload, status, reason));
    }

    private static int toPercent(double depth) {
        return (int) Math.round(depth * 100.0D);
    }

    private record Validation(Target target, ControlRodSliderResult rejection) {
        private boolean valid() {
            return target != null;
        }
    }

    private record Target(
            ReactorInstrumentPortBlockEntity owner,
            ControlRodDriveBlockEntity drive,
            CoreColumnPosition column,
            ControlRodColumnState state
    ) {
        private boolean sameOwner(Target other) {
            return owner.getBlockPos().equals(other.owner.getBlockPos());
        }
    }

    private record DragSession(BlockPos drivePos, CoreColumnPosition column, long dragId) {
        private boolean matches(Target target, long requestedDragId) {
            return dragId == requestedDragId
                    && drivePos.equals(target.drive().getBlockPos())
                    && column.equals(target.column());
        }
    }
}
