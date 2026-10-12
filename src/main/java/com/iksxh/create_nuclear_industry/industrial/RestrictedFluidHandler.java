package com.iksxh.create_nuclear_industry.industrial;

import com.iksxh.create_nuclear_industry.compat.create.SharedFluidReceiver;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** 标准能力保持读库存和普通液交易；受限工质只允许带有效路径许可的内部交易。 */
public final class RestrictedFluidHandler implements IFluidHandler, SharedFluidReceiver {
    private final IFluidHandler inventory;
    private RestrictedFluidHandler(IFluidHandler inventory) { this.inventory = inventory; }
    public static IFluidHandler wrap(IFluidHandler handler) {
        return handler == null || handler instanceof RestrictedFluidHandler ? handler : new RestrictedFluidHandler(handler);
    }
    public int getTanks() { return inventory.getTanks(); }
    public FluidStack getFluidInTank(int tank) { return inventory.getFluidInTank(tank).copy(); }
    public int getTankCapacity(int tank) { return inventory.getTankCapacity(tank); }
    public boolean isFluidValid(int tank, FluidStack stack) { return !RestrictionPolicy.restricted(stack) && inventory.isFluidValid(tank, stack); }
    public int fill(FluidStack stack, FluidAction action) { return RestrictionPolicy.restricted(stack) ? 0 : inventory.fill(stack, action); }
    public FluidStack drain(FluidStack stack, FluidAction action) { return RestrictionPolicy.restricted(stack) ? FluidStack.EMPTY : inventory.drain(stack, action); }
    public FluidStack drain(int amount, FluidAction action) {
        FluidStack simulated = inventory.drain(amount, FluidAction.SIMULATE);
        return RestrictionPolicy.restricted(simulated) ? FluidStack.EMPTY : action.simulate() ? simulated : inventory.drain(amount, action);
    }
    public Limits sharedFluidLimits() { return inventory instanceof SharedFluidReceiver receiver ? receiver.sharedFluidLimits() : null; }

    /** 每次操作都复核许可；返回的对象不暴露原库存，旧许可跨tick或路径变化立即失效。 */
    IFluidHandler authorized(IndustrialTransferPermit permit) {
        return new IFluidHandler() {
            public int getTanks() { return RestrictedFluidHandler.this.getTanks(); }
            public FluidStack getFluidInTank(int tank) { return RestrictedFluidHandler.this.getFluidInTank(tank); }
            public int getTankCapacity(int tank) { return RestrictedFluidHandler.this.getTankCapacity(tank); }
            public boolean isFluidValid(int tank, FluidStack stack) { return inventory.isFluidValid(tank, stack); }
            public int fill(FluidStack stack, FluidAction action) {
                int n = permit.allow(RestrictedFluidHandler.this, false, stack.getAmount(), action.execute());
                int accepted = n == 0 ? 0 : inventory.fill(stack.copyWithAmount(n), action);
                permit.record(false, accepted, action.execute()); return accepted;
            }
            public FluidStack drain(FluidStack stack, FluidAction action) {
                int n = permit.allow(RestrictedFluidHandler.this, true, stack.getAmount(), action.execute());
                FluidStack result = n == 0 ? FluidStack.EMPTY : inventory.drain(stack.copyWithAmount(n), action);
                permit.record(true, result.getAmount(), action.execute()); return result;
            }
            public FluidStack drain(int amount, FluidAction action) {
                int n = permit.allow(RestrictedFluidHandler.this, true, amount, action.execute());
                FluidStack result = n == 0 ? FluidStack.EMPTY : inventory.drain(n, action);
                permit.record(true, result.getAmount(), action.execute()); return result;
            }
        };
    }
}
