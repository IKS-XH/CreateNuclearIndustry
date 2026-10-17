package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 锁定NeoForge21.1.219原生拆除post返回边界；所有监听器完成后、原方块移除前提交本模组事务。 */
@Mixin(value=CommonHooks.class,remap=false)
public abstract class ReactorBreakFinalizationMixin {
    @ModifyExpressionValue(method="fireBlockBreak",at=@At(value="INVOKE",
            target="Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;"))
    private static Event accident$afterFinalBreakResult(Event posted) {
        if(posted instanceof BlockEvent.BreakEvent event)ReactorStructureLifecycle.finishBlockBreak(event);
        return posted;
    }
}
