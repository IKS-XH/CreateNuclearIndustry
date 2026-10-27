package com.iksxh.create_nuclear_industry.boiler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/** 无方向壳体；结构改动向附近控制器发失效通知，正常 tick 不重扫全部壳位。 */
public final class BoilerShellBlock extends Block {
    public BoilerShellBlock(Properties properties) { super(properties); }
    /** 已被干烧事故领取的部件不再掉落完整设备；正常采掘及其它块沿原掉落表。 */
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        if(BoilerAccidentLifecycle.consumedComponent(builder.getLevel(),BlockPos.containing(builder.getParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN)),state,
                builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)))return java.util.List.of();
        return super.getDrops(state,builder);
    }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        BoilerAccidentLifecycle.componentPlaced(level,pos,state,old);
        if (!level.isClientSide) BoilerStructure.invalidateNearby(level, pos);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide && !state.is(next.getBlock())) BoilerStructure.invalidateNearby(level, pos);
        super.onRemove(state, level, pos, next, moving);
    }
}
