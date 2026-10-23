package com.iksxh.create_nuclear_industry.accident;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** 普通镐可回收的单件废料残骸；本体不掉免费废料，掉落仅来自方块实体真实库存。 */
public final class ReactorCoriumBlock extends BaseEntityBlock {
    public static final MapCodec<ReactorCoriumBlock> CODEC=simpleCodec(ReactorCoriumBlock::new);
    public static final BooleanProperty HOT=BooleanProperty.create("hot");
    public ReactorCoriumBlock(Properties properties) { super(properties);registerDefaultState(stateDefinition.any().setValue(HOT,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(HOT);}
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity){AccidentThermalEffects.contactSolid(level,pos,entity);super.stepOn(level,pos,state,entity);}
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){AccidentThermalEffects.contactSolid(level,pos,entity);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ReactorCoriumBlockEntity(pos,state); }
    /** 原生服务端在最终取消/权限检查后进入；创造拆除只清实际库存和热引用，不向onRemove留下掉落。 */
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide&&player.isCreative()&&level.getBlockEntity(pos) instanceof ReactorCoriumBlockEntity corium)corium.take();
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof ReactorCoriumBlockEntity corium) {
            var payload=corium.take();if(!payload.isEmpty())Block.popResource(level,pos,payload);
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
