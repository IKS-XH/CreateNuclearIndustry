package com.iksxh.create_nuclear_industry.p0probe.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.core.registries.BuiltInRegistries;

/** Minimal source/flowing fluids used by the P0 capability tests. */
public final class P0ProbeFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(
            NeoForgeRegistries.Keys.FLUID_TYPES,
            CreateNuclearIndustry.MOD_ID
    );
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(
            BuiltInRegistries.FLUID,
            CreateNuclearIndustry.MOD_ID
    );

    public static final DeferredHolder<FluidType, FluidType> COLD_TYPE = FLUID_TYPES.register(
            "p0_probe_cold",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.create_nuclear_industry.p0_probe_cold")
                    .density(1000)
                    .viscosity(1000)
                    .temperature(300))
    );
    public static final DeferredHolder<FluidType, FluidType> HOT_TYPE = FLUID_TYPES.register(
            "p0_probe_hot",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.create_nuclear_industry.p0_probe_hot")
                    .density(1000)
                    .viscosity(1000)
                    .temperature(900))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> COLD_SOURCE = FLUIDS.register(
            "p0_probe_cold",
            () -> new BaseFlowingFluid.Source(coldProperties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> COLD_FLOWING = FLUIDS.register(
            "p0_probe_cold_flowing",
            () -> new BaseFlowingFluid.Flowing(coldProperties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> HOT_SOURCE = FLUIDS.register(
            "p0_probe_hot",
            () -> new BaseFlowingFluid.Source(hotProperties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> HOT_FLOWING = FLUIDS.register(
            "p0_probe_hot_flowing",
            () -> new BaseFlowingFluid.Flowing(hotProperties())
    );

    private P0ProbeFluids() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }

    public static boolean isCold(FluidStack stack) {
        return !stack.isEmpty() && (stack.is(COLD_SOURCE.get()) || stack.is(COLD_FLOWING.get()));
    }

    public static boolean isHot(FluidStack stack) {
        return !stack.isEmpty() && (stack.is(HOT_SOURCE.get()) || stack.is(HOT_FLOWING.get()));
    }

    private static BaseFlowingFluid.Properties coldProperties() {
        return new BaseFlowingFluid.Properties(COLD_TYPE, COLD_SOURCE, COLD_FLOWING)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5);
    }

    private static BaseFlowingFluid.Properties hotProperties() {
        return new BaseFlowingFluid.Properties(HOT_TYPE, HOT_SOURCE, HOT_FLOWING)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5);
    }
}
