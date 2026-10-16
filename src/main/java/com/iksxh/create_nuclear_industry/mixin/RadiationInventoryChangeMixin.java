package com.iksxh.create_nuclear_industry.mixin;
import com.iksxh.create_nuclear_industry.radiation.RadiationSourceIndex;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 原版及Create正常库存变更在setChanged后撤销区块投影；漏通知的公开库存仍低频审计。 */
@Mixin(BlockEntity.class)
public abstract class RadiationInventoryChangeMixin {
    @Inject(method="setChanged()V",at=@At("RETURN"))
    private void radiation$inventoryChanged(CallbackInfo ci) {
        var be=(BlockEntity)(Object)this;
        if(be.getLevel() instanceof ServerLevel level)RadiationSourceIndex.changed(level,be.getBlockPos());
    }
}
