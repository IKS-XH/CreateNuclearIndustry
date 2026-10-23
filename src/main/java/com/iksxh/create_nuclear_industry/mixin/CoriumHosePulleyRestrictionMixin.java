package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.accident.MoltenCoriumBlock;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 发现与既有出队目标均隐藏熔融格；旧队列不能借非零液面分支直接删掉本体。 */
@Mixin(value=FluidDrainingBehaviour.class,remap=false)
public abstract class CoriumHosePulleyRestrictionMixin {
    @Shadow it.unimi.dsi.fastutil.PriorityQueue<FluidManipulationBehaviour.BlockPosEntry> queue;
    @ModifyVariable(method="canPullFluidsFrom",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private BlockState corium$discovery(BlockState state){return state.getBlock() instanceof MoltenCoriumBlock?Blocks.AIR.defaultBlockState():state;}
    @Inject(method="pullNext",at=@At(value="INVOKE",target="Lit/unimi/dsi/fastutil/PriorityQueue;first()Ljava/lang/Object;"),cancellable=true)
    private void corium$queued(BlockPos root,boolean simulate,CallbackInfoReturnable<Boolean> callback){
        var level=((FluidDrainingBehaviour)(Object)this).getWorld();var pos=queue.first().pos();
        if(!(level instanceof net.minecraft.server.level.ServerLevel server)||!com.iksxh.create_nuclear_industry.accident.AccidentThermalEffects.loaded(server,pos)){callback.setReturnValue(false);return;}
        if(level.getBlockState(pos).getBlock() instanceof MoltenCoriumBlock){if(!simulate)queue.dequeue();callback.setReturnValue(false);}
    }
}
