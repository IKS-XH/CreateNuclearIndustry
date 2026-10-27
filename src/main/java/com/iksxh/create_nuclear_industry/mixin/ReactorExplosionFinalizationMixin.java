package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.structure.ReactorExplosionLifecycle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 锁定1.21.1原生最终块列表：Detonate已返回，洗牌已完成，任何块的loot和删除尚未开始。 */
@Mixin(Explosion.class)
public abstract class ReactorExplosionFinalizationMixin {
    @Shadow @Final private Level level;

    @Inject(method = "finalizeExplosion", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/Util;shuffle(Ljava/util/List;Lnet/minecraft/util/RandomSource;)V",
            shift = At.Shift.AFTER))
    private void accident$beforeFirstBlock(boolean particles, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            var explosion = (Explosion) (Object) this;
            // 失败时同时撤销服务端后续删块与ClientboundExplodePacket拟发送列表；
            // 继续原生空循环和profiler.pop，不能提前返回造成客户端幽灵块或profiler栈失衡。
            if (!ReactorExplosionLifecycle.beforeBlockDestruction(serverLevel,
                    explosion.getBlockInteraction(), explosion.getToBlow())) explosion.clearToBlow();
        }
    }
}
