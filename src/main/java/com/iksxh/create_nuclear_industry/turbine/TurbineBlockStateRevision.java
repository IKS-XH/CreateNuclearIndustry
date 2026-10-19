package com.iksxh.create_nuclear_industry.turbine;

/**
 * 底层方块状态容器的瞬态变更见证，供服务端汽轮机几何及工业管网静态图复用。
 * 版本无单位、只比较相等；写入期间不能发布或使用缓存，不承担区块/红石/流体交易许可。
 * 由Mixin为容器提供，未提供此见证的世界读取必须回退原完整检查，不写入NBT或网络。
 */
public interface TurbineBlockStateRevision {
    long turbineRevision();
    boolean turbineWriting();
}
