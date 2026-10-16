package com.iksxh.create_nuclear_industry.mixin;
import com.iksxh.create_nuclear_industry.radiation.RadiationItemSources;
import com.iksxh.create_nuclear_industry.config.RadiationConfig;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 只保护原版掉落中的实际放射性载荷；虚空和管理员移除仍按原版执行。 */
@Mixin(ItemEntity.class)
public abstract class RadiationItemEntityMixin {
    @Inject(method="setItem",at=@At("RETURN"))
    private void radiation$payloadChanged(net.minecraft.world.item.ItemStack stack,CallbackInfo ci) {
        com.iksxh.create_nuclear_industry.radiation.RadiationSourceIndex.entityChanged((ItemEntity)(Object)this);
    }
    @Inject(method="hurt",at=@At("HEAD"),cancellable=true)
    private void radiation$protect(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci) {
        var summary=RadiationItemSources.describe(((ItemEntity)(Object)this).getItem(),RadiationConfig.parameters());
        if((summary.radioactivePayload()||!summary.complete())
                &&(source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypeTags.IS_EXPLOSION)||source.is(DamageTypes.CACTUS))) ci.setReturnValue(false);
    }
}
