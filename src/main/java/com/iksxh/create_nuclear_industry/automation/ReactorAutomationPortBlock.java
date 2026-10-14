package com.iksxh.create_nuclear_industry.automation;

import com.iksxh.create_nuclear_industry.block.P1EntityBlockBase;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 可选侧壁接口；完整立方体碰撞，成型和危险拆除仍由原结构所有者负责。 */
public final class ReactorAutomationPortBlock extends P1EntityBlockBase {
    public static final MapCodec<ReactorAutomationPortBlock> CODEC=simpleCodec(ReactorAutomationPortBlock::new);
    public ReactorAutomationPortBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ReactorAutomationPortBlockEntity(pos,state); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public void onPlace(BlockState state, Level level,BlockPos pos,BlockState old,boolean moved) {
        super.onPlace(state,level,pos,old,moved);
        if (!state.is(old.getBlock())) ReactorStructureLifecycle.scheduleRescanAround(level,pos);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved) {
        // 在super移除BE及触发邻居重入前完成撤权，队列重扫不能合并掉一次真实拆装。
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ReactorAutomationPortBlockEntity port) port.invalidateOwner();
        super.onRemove(state,level,pos,next,moved);
        if (!state.is(next.getBlock())) ReactorStructureLifecycle.scheduleRescanAround(level,pos);
    }
}
