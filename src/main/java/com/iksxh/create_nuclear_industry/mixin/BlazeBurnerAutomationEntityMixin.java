package com.iksxh.create_nuclear_industry.mixin;
import com.iksxh.create_nuclear_industry.automation.*;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import java.util.List;
/** 仅扩展燃烧室behaviour/tick和显示同步；原燃料、机械臂、比较器和热级代码照常执行。 */
@Mixin(BlazeBurnerBlockEntity.class)
public abstract class BlazeBurnerAutomationEntityMixin implements ReactorAutomationBlazeBridge.Access,IHaveGoggleInformation {
    @Unique private CompoundTag cniView=new CompoundTag();
    @Shadow protected int remainingBurnTime;
    @Shadow protected BlazeBurnerBlockEntity.FuelType activeFuel;
    @Unique private boolean cniPaidThisTick;
    /** 先消费一次真实付热；原tick仍完整运行，仅跳过普通燃料的单次扣时。 */
    @Inject(method="tick",at=@At("HEAD")) private void cniConsumeHeat(CallbackInfo ci) {
        cniPaidThisTick=ReactorAutomationHeatSupply.consumeForBurner((BlazeBurnerBlockEntity)(Object)this);
    }
    @Redirect(method="tick",at=@At(value="FIELD",target="Lcom/simibubi/create/content/processing/burner/BlazeBurnerBlockEntity;remainingBurnTime:I",opcode=181))
    private void cniNormalCountdown(BlazeBurnerBlockEntity burner,int next) {
        // SPECIAL到期的NORMAL5000赋值保留；只有已付热NORMAL的减一操作暂停。
        if(!cniPaidThisTick || activeFuel!=BlazeBurnerBlockEntity.FuelType.NORMAL || next!=remainingBurnTime-1) remainingBurnTime=next;
    }
    @Inject(method="getHeatLevel",at=@At("RETURN"),cancellable=true)
    private void cniNuclearHeatLevel(CallbackInfoReturnable<BlazeBurnerBlock.HeatLevel> ci) {
        if(ReactorAutomationHeatSupply.displayingNuclear((BlazeBurnerBlockEntity)(Object)this)) ci.setReturnValue(BlazeBurnerBlock.HeatLevel.KINDLED);
    }
    public CompoundTag cniAutomationView() {return cniView==null?new CompoundTag():cniView;}
    public void cniAutomationView(CompoundTag view) {cniView=view.copy();}
    @Inject(method="addBehaviours",at=@At("TAIL")) private void cniControls(List<BlockEntityBehaviour> behaviours,CallbackInfo ci) {ReactorAutomationBehaviours.add((BlazeBurnerBlockEntity)(Object)this,behaviours);}
    /** 正常有燃料/创造燃烧室有早返回，必须覆盖所有RETURN而非只覆盖耗尽末尾。 */
    @Inject(method="tick",at=@At("RETURN")) private void cniTick(CallbackInfo ci) {ReactorAutomationBlazeBridge.tick((BlazeBurnerBlockEntity)(Object)this);}
    @Inject(method="write",at=@At("TAIL")) private void cniWrite(CompoundTag tag,HolderLookup.Provider lookup,boolean packet,CallbackInfo ci) {if(packet) tag.put("CniAutomationView",cniAutomationView());}
    @Inject(method="read",at=@At("TAIL")) private void cniRead(CompoundTag tag,HolderLookup.Provider lookup,boolean packet,CallbackInfo ci) {if(packet) {cniAutomationView(tag.getCompound("CniAutomationView"));ReactorAutomationBehaviours.refresh((BlazeBurnerBlockEntity)(Object)this,cniAutomationView());}}
    public boolean addToGoggleTooltip(List<Component> tooltip,boolean sneaking) {return ReactorAutomationGoggles.append(tooltip,cniAutomationView());}
}
