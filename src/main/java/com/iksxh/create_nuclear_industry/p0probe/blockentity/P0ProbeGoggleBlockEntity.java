package com.iksxh.create_nuclear_industry.p0probe.blockentity;

import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** P0 护目镜信息提供实体，只验证 Create 显示适配，不拥有服务端模拟状态。 */
public final class P0ProbeGoggleBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    public P0ProbeGoggleBlockEntity(BlockPos pos, BlockState state) {
        super(P0ProbeBlockEntities.P0_PROBE_GOGGLE.get(), pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("P0 goggle probe: detailed information"));
        tooltip.add(Component.literal(isPlayerSneaking ? "mode=sneaking" : "mode=normal"));
        return true;
    }
}
