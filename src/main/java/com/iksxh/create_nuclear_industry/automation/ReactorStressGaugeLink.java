package com.iksxh.create_nuclear_industry.automation;

import net.minecraft.core.BlockPos;

/** 玩家确认的表位置，仅维度和坐标可保存；不包含实例、动力网身份、应力样本或运行权。 */
public record ReactorStressGaugeLink(String dimension,BlockPos position) {
    public ReactorStressGaugeLink {
        if(dimension==null||dimension.isBlank()||position==null)throw new IllegalArgumentException("应力表位置非法");
        position=position.immutable();
    }
}
