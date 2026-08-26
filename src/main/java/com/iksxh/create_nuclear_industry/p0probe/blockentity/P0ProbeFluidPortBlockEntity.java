package com.iksxh.create_nuclear_industry.p0probe.blockentity;

import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** P0 单端口流体 capability 实体；冷端只填充，热端只排出。 */
public final class P0ProbeFluidPortBlockEntity extends BlockEntity {
    private final boolean coldInput;
    private final PortTank tank;

    public P0ProbeFluidPortBlockEntity(BlockPos pos, BlockState state, boolean coldInput) {
        super(coldInput ? com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities.P0_PROBE_COLD_PORT.get()
                : com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities.P0_PROBE_HOT_PORT.get(), pos, state);
        this.coldInput = coldInput;
        this.tank = new PortTank(
                1000,
                coldInput,
                coldInput ? P0ProbeFluids::isCold : P0ProbeFluids::isHot,
                this::setChanged
        );
    }

    /** 返回用于 capability 回归的内部流体槽。 */
    public IFluidHandler fluidHandler() {
        return tank;
    }

    public FluidTank tank() {
        return tank;
    }

    public boolean coldInput() {
        return coldInput;
    }

    /** 为测试夹具直接设置端口流体，不代表正式 P1 流体事务入口。 */
    public void setTestFluid(FluidStack stack) {
        tank.setFluid(stack);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("P0Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("P0ColdInput", coldInput);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("P0Tank"))
            tank.readFromNBT(registries, tag.getCompound("P0Tank"));
    }

    private static final class PortTank extends FluidTank {
        private final boolean allowFill;
        private final boolean allowDrain;
        private final java.util.function.Predicate<FluidStack> validator;
        private final Runnable changed;

        private PortTank(int capacity, boolean allowFill, java.util.function.Predicate<FluidStack> validator, Runnable changed) {
            super(capacity, validator);
            this.allowFill = allowFill;
            this.allowDrain = !allowFill;
            this.validator = validator;
            this.changed = changed;
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return validator.test(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return allowFill ? super.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return allowDrain ? super.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return allowDrain ? super.drain(maxDrain, action) : FluidStack.EMPTY;
        }

        @Override
        protected void onContentsChanged() {
            changed.run();
        }
    }
}
