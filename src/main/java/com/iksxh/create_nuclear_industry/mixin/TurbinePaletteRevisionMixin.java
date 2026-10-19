package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.turbine.TurbineBlockStateRevision;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在实际palette写入口前后推进瞬态版本，覆盖普通世界、直接分节/palette以及批量读取。
 * 这里只记版本，不读世界、不取消写入、不触发设备回调；客户端和生物群系容器也不改变原行为。
 * 写入深度处理onResize嵌套与并发；异常中止留下busy只会禁用复用，不会放过无效结构。
 */
@Mixin(PalettedContainer.class)
public abstract class TurbinePaletteRevisionMixin implements TurbineBlockStateRevision {
    @Unique private volatile long cni$turbineRevision;
    @Unique private volatile int cni$turbineWriters;

    @Override public long turbineRevision() { return cni$turbineRevision; }
    @Override public boolean turbineWriting() { return cni$turbineWriters != 0; }

    @Unique private synchronized void cni$turbineBeginWrite() {
        cni$turbineWriters++;
        cni$turbineRevision++;
    }
    @Unique private synchronized void cni$turbineEndWrite() {
        cni$turbineRevision++;
        cni$turbineWriters--;
    }

    @Inject(method={"getAndSet(ILjava/lang/Object;)Ljava/lang/Object;", "onResize(ILjava/lang/Object;)I"}, at=@At("HEAD"))
    private void cni$turbineReturningWriteBegin(CallbackInfoReturnable<?> ci) { cni$turbineBeginWrite(); }
    @Inject(method={"getAndSet(ILjava/lang/Object;)Ljava/lang/Object;", "onResize(ILjava/lang/Object;)I"}, at=@At("RETURN"))
    private void cni$turbineReturningWriteEnd(CallbackInfoReturnable<?> ci) { cni$turbineEndWrite(); }
    @Inject(method={"set(ILjava/lang/Object;)V", "read(Lnet/minecraft/network/FriendlyByteBuf;)V"}, at=@At("HEAD"))
    private void cni$turbineVoidWriteBegin(CallbackInfo ci) { cni$turbineBeginWrite(); }
    @Inject(method={"set(ILjava/lang/Object;)V", "read(Lnet/minecraft/network/FriendlyByteBuf;)V"}, at=@At("RETURN"))
    private void cni$turbineVoidWriteEnd(CallbackInfo ci) { cni$turbineEndWrite(); }
}
