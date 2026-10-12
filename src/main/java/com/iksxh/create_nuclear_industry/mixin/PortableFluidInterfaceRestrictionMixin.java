package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.industrial.RestrictedFluidHandler;
import com.simibubi.create.content.contraptions.actors.psi.PortableFluidInterfaceBlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PSI同库标准交易补口；保留原连接/keepAlive，只包装底层引用，不禁止整罐构件移动。 */
@Mixin(value = PortableFluidInterfaceBlockEntity.InterfaceFluidHandler.class, remap = false)
abstract class PortableFluidInterfaceRestrictionMixin {
    @Shadow private IFluidHandler wrapped;
    @Inject(method = "<init>(Lcom/simibubi/create/content/contraptions/actors/psi/PortableFluidInterfaceBlockEntity;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;)V",
            at = @At("TAIL"), remap = false, require = 1, expect = 1, allow = 1)
    private void createNuclearIndustry$wrapMountedInventory(PortableFluidInterfaceBlockEntity owner, IFluidHandler inventory, CallbackInfo callback) {
        wrapped = RestrictedFluidHandler.wrap(wrapped);
    }
}
