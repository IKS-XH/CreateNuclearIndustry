package com.iksxh.create_nuclear_industry.compat.create;

import java.util.Objects;

/**
 * 本模组流体接收口提供给 Create 分流模拟的只读共享限制。
 * 服务端使用；所有数量单位为 mB，不预约库存、不消耗物理口配额。
 */
public interface SharedFluidReceiver {
    /** 返回当前可接收口的限制；输出口或失效句柄返回 null，仍由原 fill 决定接收量。 */
    Limits sharedFluidLimits();

    /**
     * 一次同步分流期间身份保持不变的容量与配额快照。
     * 库存身份用于合并多个物理口；配额身份用于合并同一物理口的多个面。
     * 两类身份均按对象引用比较，不能用值相等代替同一库存或同一预算。
     */
    record Limits(Object inventoryIdentity, long inventorySpaceMb,
                  Object flowIdentity, int flowSpaceMb) {
        public Limits {
            Objects.requireNonNull(inventoryIdentity, "共享库存身份不能为空");
            Objects.requireNonNull(flowIdentity, "物理口预算身份不能为空");
            if (inventorySpaceMb < 0L || flowSpaceMb < 0) {
                throw new IllegalArgumentException("共享接收空间和物理口配额不能为负数");
            }
        }
    }
}
