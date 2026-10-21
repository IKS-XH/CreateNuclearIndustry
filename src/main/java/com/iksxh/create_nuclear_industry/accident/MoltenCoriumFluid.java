package com.iksxh.create_nuclear_industry.accident;

import com.iksxh.create_nuclear_industry.content.AccidentContent;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** 原生液面仅作投影；永远非源，流体tick不蔓延、不删块，不暴露桶或可输送库存。 */
public final class MoltenCoriumFluid extends BaseFlowingFluid.Flowing {
    public MoltenCoriumFluid(){super(new BaseFlowingFluid.Properties(AccidentContent.MOLTEN_TYPE,AccidentContent.MOLTEN_FLUID,AccidentContent.MOLTEN_FLUID)
            .block(AccidentContent.MOLTEN_CORIUM).tickRate(20));}
    @Override public boolean isSource(FluidState state){return false;}
    @Override public FluidState getSource(boolean falling){return defaultFluidState().setValue(LEVEL,8).setValue(FALLING,falling);}
    @Override public int getAmount(FluidState state){return 8;}
    @Override public Vec3 getFlow(BlockGetter level,BlockPos pos,FluidState state){return Vec3.ZERO;}
    @Override public void tick(Level level,BlockPos pos,FluidState state){}
    @Override public void randomTick(Level level,BlockPos pos,FluidState state,RandomSource random){}
    @Override protected boolean canBeReplacedWith(FluidState state,BlockGetter level,BlockPos pos,Fluid fluid,Direction direction){return false;}
    @Override protected boolean canConvertToSource(Level level){return false;}
}
