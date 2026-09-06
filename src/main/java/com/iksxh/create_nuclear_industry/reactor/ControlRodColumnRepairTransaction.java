package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.content.ModItems;
import net.minecraft.world.item.ItemStack;

/**
 * 控制棒列合金钢板维修的加载器无关物品事务。
 *
 * <p>事务只读取控制棒列、输入栈和服务端失效阈值，返回下一列状态与输入余量；
 * 不直接修改仪表端口快照或玩家物品。服务端交互必须在成功后一次性提交返回状态。</p>
 */
public final class ControlRodColumnRepairTransaction {
    private static final double FULL_EPSILON = 1.0E-12D;
    private static final double REPAIR_PER_ROD = 0.25D;

    private ControlRodColumnRepairTransaction() {
    }

    /** 维修事务的成功结果或稳定失败原因。 */
    public static final class Result {
        private final Status status;
        private final ControlRodColumnState nextColumn;
        private final ItemStack remainingInput;
        private final boolean jammedCleared;

        private Result(
                Status status,
                ControlRodColumnState nextColumn,
                ItemStack remainingInput,
                boolean jammedCleared
        ) {
            this.status = status;
            this.nextColumn = nextColumn;
            this.remainingInput = remainingInput == null ? ItemStack.EMPTY : remainingInput.copy();
            this.jammedCleared = jammedCleared;
        }

        public Status status() {
            return status;
        }

        /** 返回维修后应写回的控制棒列；无效驱动器失败时返回 {@code null}。 */
        public ControlRodColumnState nextColumn() {
            return nextColumn;
        }

        /** 返回消耗一块钢板后的输入余量；失败时返回未改变的输入副本。 */
        public ItemStack remainingInput() {
            return remainingInput.copy();
        }

        /** 返回本次成功维修是否跨过失效阈值并解除卡死。 */
        public boolean jammedCleared() {
            return jammedCleared;
        }

        public boolean success() {
            return status == Status.REPAIRED;
        }
    }

    /** 维修结果的动作栏消息后缀。 */
    public enum Status {
        REPAIRED("repaired"),
        COLUMN_FULL("column_full"),
        EMPTY_INPUT("empty_input"),
        WRONG_REPAIR_ITEM("wrong_repair_item"),
        INVALID_DRIVE("invalid_drive");

        private final String translationKeySuffix;

        Status(String translationKeySuffix) {
            this.translationKeySuffix = translationKeySuffix;
        }

        /** 返回本地化动作栏键后缀，不改变事务结果。 */
        public String translationKeySuffix() {
            return translationKeySuffix;
        }
    }

    /**
     * 尝试用一块 {@code steel_plate} 修复控制棒列。
     *
     * <p>维修量与燃料列使用同一规则，固定为 {@code 0.25 / internalHeight}。
     * 卡死状态只有在维修后的完整度严格高于失效阈值时才解除；目标深度、实际深度
     * 和缓存热量始终由 {@link ControlRodStateTransitions} 原样保留。</p>
     *
     * @param current 当前服务端控制棒列状态，不会被修改
     * @param incoming 玩家手中输入栈，不会被修改
     * @param failureThreshold 服务端控制棒卡死失效阈值，范围为 [0,1]
     * @return 维修结果；失败时不消耗输入且返回原列
     */
    public static Result repair(
            ControlRodColumnState current,
            ItemStack incoming,
            double failureThreshold
    ) {
        requireColumn(current);
        requireUnitInterval("control rod failure threshold", failureThreshold);
        if (incoming == null || incoming.isEmpty()) {
            return failure(Status.EMPTY_INPUT, current, incoming);
        }
        if (!incoming.is(ModItems.STEEL_PLATE.get())) {
            return failure(Status.WRONG_REPAIR_ITEM, current, incoming);
        }
        if (current.integrity() >= 1.0D - FULL_EPSILON) {
            return failure(Status.COLUMN_FULL, current, incoming);
        }

        double repairAmount = REPAIR_PER_ROD / ReactorSnapshot.INTERNAL_HEIGHT;
        ControlRodColumnState repaired = ControlRodStateTransitions.repairIntegrity(
                current, repairAmount, failureThreshold);
        ItemStack remaining = incoming.copy();
        remaining.shrink(1);
        return new Result(Status.REPAIRED, repaired, remaining,
                current.jammed() && !repaired.jammed());
    }

    /** 生成无效驱动器失败结果，确保玩家输入不会被误消费。 */
    public static Result invalidDrive(ItemStack incoming) {
        return new Result(Status.INVALID_DRIVE, null, incoming, false);
    }

    private static Result failure(Status status, ControlRodColumnState current, ItemStack incoming) {
        return new Result(status, current, incoming, false);
    }

    private static void requireColumn(ControlRodColumnState current) {
        if (current == null) {
            throw new IllegalArgumentException("control rod column state is required");
        }
    }

    private static void requireUnitInterval(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]");
        }
    }
}
