package com.iksxh.create_nuclear_industry.mixin;

import com.iksxh.create_nuclear_industry.compat.create.SharedFluidFillPlan;
import com.iksxh.create_nuclear_industry.compat.create.SharedFluidReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.fluids.FluidNetwork;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 仅修正本模组共享接收口在 Create 分流模拟中的重复容量许诺。
 * 不改变执行顺序或真实记账；其他流体处理器完全沿用原调用。
 */
@Mixin(value = FluidNetwork.class, remap = false)
abstract class FluidNetworkSharedFillMixin {
    /**
     * 锁定 Create 6.0.10 的唯一 fill 调用点，目标缺失或增多时必须在加载阶段显错。
     * Share 引用仅属于本次 tick 方法调用；内部 while 重分配共用计划，返回或异常后
     * 自然释放，不跨管网、不跨 tick 保留，也不影响普通 capability 模拟查询。
     */
    @WrapOperation(
            method = "tick()V",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;"
                    + "fill(Lnet/neoforged/neoforge/fluids/FluidStack;"
                    + "Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I", remap = false),
            remap = false, require = 1, expect = 1, allow = 1)
    private int createNuclearIndustry$limitSharedFill(
            IFluidHandler target, FluidStack stack, FluidAction action, Operation<Integer> original,
            @Share("sharedFluidFillPlan") LocalRef<SharedFluidFillPlan> planRef) {
        int accepted = original.call(target, stack, action);
        if (action.execute()) {
            planRef.set(null);
            return accepted;
        }
        if (!(target instanceof SharedFluidReceiver receiver)) {
            return accepted;
        }
        SharedFluidReceiver.Limits limits = receiver.sharedFluidLimits();
        if (limits == null) {
            return accepted;
        }
        SharedFluidFillPlan plan = planRef.get();
        if (plan == null) {
            if (accepted == 0) return 0;
            plan = new SharedFluidFillPlan();
            planRef.set(plan);
        }
        return plan.limit(target, limits, accepted);
    }
}
