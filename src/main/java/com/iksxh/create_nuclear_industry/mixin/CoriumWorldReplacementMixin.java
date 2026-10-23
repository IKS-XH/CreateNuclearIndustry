package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.accident.MoltenCoriumEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 在真实chunk写前保护已登记熔融物，授权自动写在同一入口逐格计账；不覆盖任意section直写。 */
@Mixin(LevelChunk.class)
public abstract class CoriumWorldReplacementMixin {
    @Shadow @Final private Level level;
    @Shadow public abstract BlockState getBlockState(BlockPos pos);
    @Inject(method="setBlockState",at=@At("HEAD"),cancellable=true)
    private void corium$guard(BlockPos pos,BlockState next,boolean moving,CallbackInfoReturnable<BlockState> callback){
        if(!MoltenCoriumEffects.beforeWrite(level,pos,getBlockState(pos),next))callback.setReturnValue(null);
    }
}
