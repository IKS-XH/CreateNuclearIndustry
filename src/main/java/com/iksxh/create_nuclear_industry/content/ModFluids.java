package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * 冷、热复合冷却剂的 FluidType、流体源/流动态和可放置方块注册层。
 *
 * <p>冷却剂的流体注册 ID 与 {@link P1ContentIds} 保持一致，热态没有桶物品，
 * 只能作为模拟和管路中的流体状态使用。客户端纹理只在 FluidType 的客户端扩展
 * 初始化阶段提供，流体身份和事务判断仍由服务端注册对象决定。</p>
 */
public final class ModFluids {
    private static final int COOLANT_DENSITY = 1000;
    private static final int COOLANT_VISCOSITY = 1000;
    private static final int COLD_COOLANT_TEMPERATURE = 300;
    private static final int HOT_COOLANT_TEMPERATURE = 900;

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(
            NeoForgeRegistries.Keys.FLUID_TYPES,
            CreateNuclearIndustry.MOD_ID
    );
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(
            BuiltInRegistries.FLUID,
            CreateNuclearIndustry.MOD_ID
    );
    public static final DeferredRegister.Blocks LIQUID_BLOCKS = DeferredRegister.createBlocks(
            CreateNuclearIndustry.MOD_ID
    );

    public static final DeferredHolder<FluidType, FluidType> COMPOUND_COOLANT_TYPE = FLUID_TYPES.register(
            P1ContentIds.COMPOUND_COOLANT_ID,
            () -> coolantType(P1ContentIds.COMPOUND_COOLANT_ID, COLD_COOLANT_TEMPERATURE)
    );
    public static final DeferredHolder<FluidType, FluidType> HOT_COMPOUND_COOLANT_TYPE = FLUID_TYPES.register(
            P1ContentIds.HOT_COMPOUND_COOLANT_ID,
            () -> coolantType(P1ContentIds.HOT_COMPOUND_COOLANT_ID, HOT_COOLANT_TEMPERATURE)
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> COMPOUND_COOLANT_SOURCE = FLUIDS.register(
            P1ContentIds.COMPOUND_COOLANT_ID,
            () -> new BaseFlowingFluid.Source(compoundCoolantProperties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> COMPOUND_COOLANT_FLOWING = FLUIDS.register(
            P1ContentIds.COMPOUND_COOLANT_ID + "_flowing",
            () -> new BaseFlowingFluid.Flowing(compoundCoolantProperties())
    );

    /** 冷态流体的标准可放置方块；这里不注册对应的 BlockItem。 */
    public static final DeferredBlock<LiquidBlock> COMPOUND_COOLANT_BLOCK = LIQUID_BLOCKS.register(
            P1ContentIds.COMPOUND_COOLANT_ID,
            () -> new LiquidBlock(COMPOUND_COOLANT_SOURCE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER))
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> HOT_COMPOUND_COOLANT_SOURCE = FLUIDS.register(
            P1ContentIds.HOT_COMPOUND_COOLANT_ID,
            () -> new BaseFlowingFluid.Source(hotCompoundCoolantProperties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> HOT_COMPOUND_COOLANT_FLOWING = FLUIDS.register(
            P1ContentIds.HOT_COMPOUND_COOLANT_ID + "_flowing",
            () -> new BaseFlowingFluid.Flowing(hotCompoundCoolantProperties())
    );

    private ModFluids() {
    }

    /** 按 NeoForge 注册顺序提交 FluidType、流体和液体方块注册表。 */
    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        LIQUID_BLOCKS.register(modEventBus);
    }

    /** 判断流体栈是否为冷态复合冷却剂的源流体或流动态。 */
    public static boolean isCompoundCoolant(FluidStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.is(COMPOUND_COOLANT_SOURCE.get()) || stack.is(COMPOUND_COOLANT_FLOWING.get()));
    }

    /** 判断流体栈是否为热态复合冷却剂的源流体或流动态。 */
    public static boolean isHotCompoundCoolant(FluidStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.is(HOT_COMPOUND_COOLANT_SOURCE.get())
                || stack.is(HOT_COMPOUND_COOLANT_FLOWING.get()));
    }

    private static FluidType coolantType(String id, int temperature) {
        ResourceLocation stillTexture = ResourceLocation.fromNamespaceAndPath(
                CreateNuclearIndustry.MOD_ID, "block/" + id + "_still");
        ResourceLocation flowingTexture = ResourceLocation.fromNamespaceAndPath(
                CreateNuclearIndustry.MOD_ID, "block/" + id + "_flow");

        return new FluidType(FluidType.Properties.create()
                .descriptionId("fluid_type." + CreateNuclearIndustry.MOD_ID + "." + id)
                .density(COOLANT_DENSITY)
                .viscosity(COOLANT_VISCOSITY)
                .temperature(temperature)) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return stillTexture;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return flowingTexture;
                    }
                });
            }
        };
    }

    private static BaseFlowingFluid.Properties compoundCoolantProperties() {
        return new BaseFlowingFluid.Properties(
                COMPOUND_COOLANT_TYPE,
                COMPOUND_COOLANT_SOURCE,
                COMPOUND_COOLANT_FLOWING
        )
                .bucket(() -> ModItems.COMPOUND_COOLANT_BUCKET.get())
                .block(COMPOUND_COOLANT_BLOCK)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5);
    }

    private static BaseFlowingFluid.Properties hotCompoundCoolantProperties() {
        return new BaseFlowingFluid.Properties(
                HOT_COMPOUND_COOLANT_TYPE,
                HOT_COMPOUND_COOLANT_SOURCE,
                HOT_COMPOUND_COOLANT_FLOWING
        )
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                .tickRate(5);
    }
}
