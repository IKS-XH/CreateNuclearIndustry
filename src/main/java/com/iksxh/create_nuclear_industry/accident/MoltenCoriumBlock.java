package com.iksxh.create_nuclear_industry.accident;

import com.iksxh.create_nuclear_industry.content.AccidentContent;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/** 只在真实世界出现时绑定独立记录；普通放块/流体不能替换，禁原生反应和流体调度。 */
public final class MoltenCoriumBlock extends LiquidBlock {
    public MoltenCoriumBlock(Properties properties){super(AccidentContent.MOLTEN_FLUID.get(),properties);}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){if(level instanceof ServerLevel server)MoltenCoriumEffects.appeared(server,pos);}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,net.minecraft.world.level.block.Block block,BlockPos from,boolean moving){}
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){return state;}
    @Override protected boolean canBeReplaced(BlockState state,BlockPlaceContext context){return false;}
    @Override protected boolean canBeReplaced(BlockState state,Fluid fluid){return false;}
    @Override public ItemStack pickupBlock(Player player,LevelAccessor level,BlockPos pos,BlockState state){return ItemStack.EMPTY;}
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){MoltenCoriumEffects.contact(level,pos,entity);}
}
