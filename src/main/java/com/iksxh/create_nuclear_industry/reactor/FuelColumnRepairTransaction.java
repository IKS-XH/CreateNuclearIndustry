package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.content.ModItems;
import net.minecraft.world.item.ItemStack;

/**
 * 燃料列合金钢板维修的加载器无关物品事务。
 *
 * <p>事务只读取当前列、当前已结算裂变发热和玩家输入，返回完整度与输入余量；
 * 它不修改输入 {@link ItemStack}、反应堆快照或世界。服务端调用方必须在成功后
 * 一次性提交返回的列状态，失败结果则保留原列和原输入。</p>
 */
public final class FuelColumnRepairTransaction {
    private static final double HEAT_EPSILON = 1.0E-12D;
    private static final double REPAIR_PER_FUEL_ROD = 0.25D;

    private FuelColumnRepairTransaction() {
    }

    /** 维修事务的成功结果或稳定失败原因。 */
    public static final class Result {
        private final Status status;
        private final FuelColumnState nextColumn;
        private final ItemStack remainingInput;

        private Result(Status status, FuelColumnState nextColumn, ItemStack remainingInput) {
            this.status = status;
            this.nextColumn = nextColumn;
            this.remainingInput = remainingInput == null ? ItemStack.EMPTY : remainingInput.copy();
        }

        public Status status() {
            return status;
        }

        /** 返回维修成功后应提交的列状态；失败时与输入列相同。 */
        public FuelColumnState nextColumn() {
            return nextColumn;
        }

        /** 返回消耗一块钢板后的输入余量；失败时返回未改变的输入副本。 */
        public ItemStack remainingInput() {
            return remainingInput.copy();
        }

        public boolean success() {
            return status == Status.REPAIRED;
        }
    }

    /** 维修结果的动作栏消息后缀。 */
    public enum Status {
        REPAIRED("repaired"),
        COLUMN_FULL("column_full"),
        COLUMN_ACTIVE("column_active"),
        EMPTY_INPUT("empty_input"),
        WRONG_REPAIR_ITEM("wrong_repair_item"),
        INVALID_PORT("invalid_port");

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
     * 尝试用一块 {@code steel_plate} 修复燃料列。
     *
     * <p>维修量固定为单个燃料棒额定完整度的 25%，当前固定三格有效高度因此换算为
     * {@code 0.25 / ReactorSnapshot.INTERNAL_HEIGHT}。只检查当前 tick 的新生裂变热，
     * 不把缓存余热当作仍在放热；缓存余热、燃耗余量和组件耐久均原样保留。</p>
     *
     * @param current 当前服务端燃料列投影，不会被修改
     * @param incoming 玩家手中输入栈，不会被修改
     * @param currentFissionHeatHu 当前列已结算新生裂变热，单位为 HU/t
     * @return 维修结果；失败时不消耗输入且返回原列
     */
    public static Result repair(
            FuelColumnState current,
            ItemStack incoming,
            double currentFissionHeatHu
    ) {
        requireColumn(current);
        requireHeat(currentFissionHeatHu);
        if (incoming == null || incoming.isEmpty()) {
            return failure(Status.EMPTY_INPUT, current, incoming);
        }
        if (!incoming.is(ModItems.STEEL_PLATE.get())) {
            return failure(Status.WRONG_REPAIR_ITEM, current, incoming);
        }
        if (currentFissionHeatHu > HEAT_EPSILON) {
            return failure(Status.COLUMN_ACTIVE, current, incoming);
        }
        if (current.integrity() >= 1.0D - HEAT_EPSILON) {
            return failure(Status.COLUMN_FULL, current, incoming);
        }

        double repairAmount = REPAIR_PER_FUEL_ROD / ReactorSnapshot.INTERNAL_HEIGHT;
        FuelColumnState repaired = current.withIntegrity(
                Math.min(1.0D, current.integrity() + repairAmount));
        ItemStack remaining = incoming.copy();
        remaining.shrink(1);
        return new Result(Status.REPAIRED, repaired, remaining);
    }

    /** 生成未绑定端口的失败结果，确保玩家输入不会被误消费。 */
    public static Result invalidPort(FuelColumnState current, ItemStack incoming) {
        FuelColumnState safeColumn = current == null ? FuelColumnState.empty() : current;
        return failure(Status.INVALID_PORT, safeColumn, incoming);
    }

    private static Result failure(Status status, FuelColumnState current, ItemStack incoming) {
        return new Result(status, current, incoming);
    }

    private static void requireColumn(FuelColumnState current) {
        if (current == null) {
            throw new IllegalArgumentException("fuel column state is required");
        }
    }

    private static void requireHeat(double heatHu) {
        if (!Double.isFinite(heatHu) || heatHu < 0.0D) {
            throw new IllegalArgumentException("current fission heat must be finite and non-negative");
        }
    }
}
