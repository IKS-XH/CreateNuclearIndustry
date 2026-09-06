package com.iksxh.create_nuclear_industry.reactor;

import net.minecraft.world.item.ItemStack;

/**
 * 燃料列的服务端原子取放事务。
 *
 * <p>事务只计算新状态和物品转移结果，不直接修改输入 {@link ItemStack}，也不拥有
 * 反应堆库存。调用方必须在 {@link Result#success()} 为真时一次性提交
 * {@link Result#nextColumn()} 和 {@link Result#remainingInput()}；失败结果原样保留
 * 当前列与输入栈，保证玩家和 Create 机械臂都能复用同一套回滚边界。</p>
 */
public final class FuelRefuelingTransaction {
    private static final double HEAT_EPSILON = 1.0E-12D;

    private FuelRefuelingTransaction() {
    }

    /** 事务的确定性结果；结果中的物品栈均为副本，调用方可安全地提交或回滚。 */
    public static final class Result {
        private final Status status;
        private final FuelColumnState nextColumn;
        private final ItemStack output;
        private final ItemStack remainingInput;

        private Result(
                Status status,
                FuelColumnState nextColumn,
                ItemStack output,
                ItemStack remainingInput
        ) {
            this.status = status;
            this.nextColumn = nextColumn;
            this.output = output == null ? ItemStack.EMPTY : output.copy();
            this.remainingInput = remainingInput == null ? ItemStack.EMPTY : remainingInput.copy();
        }

        public Status status() {
            return status;
        }

        public FuelColumnState nextColumn() {
            return nextColumn;
        }

        /** 取出事务的输出；装入事务和失败事务返回空栈。 */
        public ItemStack output() {
            return output.copy();
        }

        /** 装入事务消费一个组件后的输入余量；取出事务返回空栈。 */
        public ItemStack remainingInput() {
            return remainingInput.copy();
        }

        public boolean success() {
            return status == Status.INSERTED || status == Status.REMOVED;
        }
    }

    /** 换料事务的失败原因和成功方向。 */
    public enum Status {
        INSERTED("inserted"),
        REMOVED("removed_fuel"),
        COLUMN_OCCUPIED("column_occupied"),
        COLUMN_EMPTY("column_empty"),
        COLUMN_ACTIVE("column_active"),
        EMPTY_INPUT("empty_input"),
        WRONG_FUEL_ITEM("wrong_fuel_item"),
        EXHAUSTED_FUEL_INPUT("exhausted_fuel_input"),
        INVALID_PORT("invalid_port");

        private final String translationKeySuffix;

        Status(String translationKeySuffix) {
            this.translationKeySuffix = translationKeySuffix;
        }

        /** 返回动作栏消息的本地化键后缀，不改变事务状态本身。 */
        public String translationKeySuffix() {
            return translationKeySuffix;
        }
    }

    /**
     * 尝试向空燃料列装入一个新燃料组件。
     *
     * @param current 当前列状态；不会被修改
     * @param incoming 外部输入栈；成功时只消费其中一个组件
     * @param currentFissionHeatHu 当前列已结算裂变发热，单位为 HU/t
     * @return 成功时的新列状态和输入余量，失败时保留原状态且不消费输入
     */
    public static Result insert(
            FuelColumnState current,
            ItemStack incoming,
            double currentFissionHeatHu
    ) {
        requireColumn(current);
        requireHeat(currentFissionHeatHu);
        if (currentFissionHeatHu > HEAT_EPSILON) {
            return failure(Status.COLUMN_ACTIVE, current, incoming);
        }
        if (current.fuelAssembly().present()) {
            return failure(Status.COLUMN_OCCUPIED, current, incoming);
        }
        if (incoming == null || incoming.isEmpty()) {
            return failure(Status.EMPTY_INPUT, current, incoming);
        }
        if (!FuelAssemblyItemCodec.isFreshFuel(incoming)) {
            return failure(Status.WRONG_FUEL_ITEM, current, incoming);
        }

        FuelAssemblyState incomingState = FuelAssemblyItemCodec.readFreshFuel(incoming);
        if (incomingState.exhausted()) {
            return failure(Status.EXHAUSTED_FUEL_INPUT, current, incoming);
        }

        ItemStack remaining = incoming.copy();
        remaining.shrink(1);
        return new Result(
                Status.INSERTED,
                current.withFuelAssembly(incomingState),
                ItemStack.EMPTY,
                remaining
        );
    }

    /**
     * 尝试从停止放热的燃料列取出组件。
     *
     * <p>“停止放热”只检查当前裂变发热，不要求缓存余热已经归零；缓存余热仍属于
     * 仪表端口的列状态。正常燃料按原耐久取回，达到最大耐久的组件直接转换为冷却
     * 乏燃料，绝不生成热乏燃料。</p>
     *
     * @param current 当前列状态；不会被修改
     * @param currentFissionHeatHu 当前列已结算裂变发热，单位为 HU/t
     * @return 成功时的空燃料列和一个输出组件，失败时保留原列
     */
    public static Result extract(FuelColumnState current, double currentFissionHeatHu) {
        return extract(current, currentFissionHeatHu, ItemStack.EMPTY);
    }

    /**
     * 从停止放热的燃料列取出端口保存的精确物品栈。
     *
     * <p>精确栈用于保留自定义名称等全部数据组件。服务端调用方应先校验端口栈与
     * {@code current} 投影一致；不一致时返回失败，避免快照与物品发生静默分叉。</p>
     */
    public static Result extract(
            FuelColumnState current,
            double currentFissionHeatHu,
            ItemStack exactStoredFuel
    ) {
        requireColumn(current);
        requireHeat(currentFissionHeatHu);
        if (!current.fuelAssembly().present()) {
            return failure(Status.COLUMN_EMPTY, current, ItemStack.EMPTY);
        }
        if (currentFissionHeatHu > HEAT_EPSILON) {
            return failure(Status.COLUMN_ACTIVE, current, ItemStack.EMPTY);
        }

        ItemStack output;
        if (exactStoredFuel != null && !exactStoredFuel.isEmpty()) {
            if (!FuelAssemblyItemCodec.isValidStoredFuel(exactStoredFuel)) {
                return failure(Status.WRONG_FUEL_ITEM, current, ItemStack.EMPTY);
            }
            if (current.fuelAssembly().exhausted()) {
                output = FuelAssemblyItemCodec.isCooledSpentFuel(exactStoredFuel)
                        ? exactStoredFuel.copyWithCount(1)
                        : FuelAssemblyItemCodec.createCooledSpentFuel();
            } else {
                if (!FuelAssemblyItemCodec.isFreshFuel(exactStoredFuel)) {
                    return failure(Status.WRONG_FUEL_ITEM, current, ItemStack.EMPTY);
                }
                FuelAssemblyState exactState = FuelAssemblyItemCodec.readFreshFuel(exactStoredFuel);
                if (!exactState.equals(current.fuelAssembly())) {
                    return failure(Status.WRONG_FUEL_ITEM, current, ItemStack.EMPTY);
                }
                output = exactStoredFuel.copyWithCount(1);
            }
        } else {
            output = current.fuelAssembly().exhausted()
                    ? FuelAssemblyItemCodec.createCooledSpentFuel()
                    : FuelAssemblyItemCodec.writeFreshFuel(current.fuelAssembly());
        }
        return new Result(Status.REMOVED, current.withoutFuelAssembly(), output, ItemStack.EMPTY);
    }

    /** 生成未绑定端口的失败结果，供世界侧端口在不伪造列状态时使用。 */
    public static Result invalidPort(ItemStack incoming) {
        return new Result(Status.INVALID_PORT, FuelColumnState.empty(), ItemStack.EMPTY, copy(incoming));
    }

    private static Result failure(Status status, FuelColumnState current, ItemStack incoming) {
        return new Result(status, current, ItemStack.EMPTY, copy(incoming));
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack.copy();
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
