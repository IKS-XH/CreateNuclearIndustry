package com.iksxh.create_nuclear_industry.mixin;
import com.iksxh.create_nuclear_industry.radiation.RadiationContainment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 在实际方块提交之后通知屏蔽缓存，覆盖命令/爆炸/玩家；取消事件不提前改变屏蔽。 */
@Mixin(LevelChunk.class)
public abstract class RadiationBlockChangeMixin {
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void radiation$changed(BlockPos pos,BlockState state,boolean moving,CallbackInfoReturnable<BlockState> ci) {
        if(ci.getReturnValue()!=null && !ci.getReturnValue().equals(state)
                && ((LevelChunk)(Object)this).getLevel() instanceof ServerLevel level)
            {RadiationContainment.changed(level,pos);
                com.iksxh.create_nuclear_industry.radiation.RadiationSourceIndex.changed(level,pos);
                com.iksxh.create_nuclear_industry.radiation.RadiationSourceIndex.shieldingChanged(level,pos);}
    }
}
