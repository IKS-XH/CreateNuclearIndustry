package com.iksxh.create_nuclear_industry.automation;
import net.minecraft.core.BlockPos;
import java.util.UUID;
/** 服务端绑定身份；维度、所有者实例代际、两端坐标和控制会话代际必须全部匹配。 */
public record ReactorAutomationBinding(String dimension, BlockPos ownerPos, UUID ownerGeneration,
        BlockPos portPos, BlockPos burnerPos,long authorityEpoch) {
    public ReactorAutomationBinding {
        ownerPos=ownerPos.immutable(); portPos=portPos.immutable(); burnerPos=burnerPos.immutable();
    }
    public ReactorAutomationBinding withEpoch(long epoch) {
        return new ReactorAutomationBinding(dimension,ownerPos,ownerGeneration,portPos,burnerPos,epoch);
    }
}
