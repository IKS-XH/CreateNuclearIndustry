package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.turbine.StorageOnlyOrdinarySteamFluid;
import com.iksxh.create_nuclear_industry.turbine.TurbineControllerBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbineOutputShaftBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbinePortBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbineShaftBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineStructure;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** 六件汽轮机构件、两轴方块实体和仅用于管网的普通蒸汽。 */
public final class TurbineContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, CreateNuclearIndustry.MOD_ID);
    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4).requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<TurbinePartBlock.Casing> CASING = BLOCKS.register("turbine_casing", () -> new TurbinePartBlock.Casing(metal()));
    public static final DeferredBlock<TurbinePartBlock.Rotor> ROTOR = BLOCKS.register("turbine_rotor", () -> new TurbinePartBlock.Rotor(metal()));
    public static final DeferredBlock<TurbineShaftBlock.Controller> CONTROLLER = BLOCKS.register("turbine_controller", () -> new TurbineShaftBlock.Controller(metal()));
    public static final DeferredBlock<TurbineShaftBlock.Output> OUTPUT_SHAFT = BLOCKS.register("turbine_output_shaft", () -> new TurbineShaftBlock.Output(metal()));
    public static final DeferredBlock<TurbinePartBlock.Inlet> INLET = BLOCKS.register("turbine_inlet", () -> new TurbinePartBlock.Inlet(metal()));
    public static final DeferredBlock<TurbinePartBlock.Exhaust> EXHAUST = BLOCKS.register("turbine_exhaust", () -> new TurbinePartBlock.Exhaust(metal()));
    public static final DeferredItem<BlockItem> CASING_ITEM = item("turbine_casing", CASING, false);
    public static final DeferredItem<BlockItem> ROTOR_ITEM = item("turbine_rotor", ROTOR, false);
    public static final DeferredItem<BlockItem> CONTROLLER_ITEM = item("turbine_controller", CONTROLLER, true);
    public static final DeferredItem<BlockItem> OUTPUT_SHAFT_ITEM = item("turbine_output_shaft", OUTPUT_SHAFT, false);
    public static final DeferredItem<BlockItem> INLET_ITEM = item("turbine_inlet", INLET, false);
    public static final DeferredItem<BlockItem> EXHAUST_ITEM = item("turbine_exhaust", EXHAUST, false);
    private static DeferredItem<BlockItem> item(String id, DeferredBlock<? extends Block> block, boolean portable) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), portable ? new Item.Properties().stacksTo(1) : new Item.Properties()));
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineControllerBlockEntity>> CONTROLLER_BE =
            ENTITIES.register("turbine_controller", () -> BlockEntityType.Builder.of(
                    TurbineControllerBlockEntity::new, CONTROLLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineOutputShaftBlockEntity>> OUTPUT_SHAFT_BE =
            ENTITIES.register("turbine_output_shaft", () -> BlockEntityType.Builder.of(
                    TurbineOutputShaftBlockEntity::new, OUTPUT_SHAFT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbinePortBlockEntity>> PORT_BE =
            ENTITIES.register("turbine_port", () -> BlockEntityType.Builder.of(
                    TurbinePortBlockEntity::new, INLET.get(), EXHAUST.get()).build(null));

    public static final DeferredHolder<FluidType, FluidType> STEAM_TYPE = TYPES.register("steam", () -> {
        ResourceLocation still = ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "fluid/steam_still");
        ResourceLocation flowing = ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "fluid/steam_flow");
        return new FluidType(FluidType.Properties.create().descriptionId("fluid_type.create_nuclear_industry.steam")
                .density(-1000).viscosity(100).temperature(400)) {
            @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override public ResourceLocation getStillTexture() { return still; }
                    @Override public ResourceLocation getFlowingTexture() { return flowing; }
                });
            }
        };
    });
    public static final DeferredHolder<Fluid, StorageOnlyOrdinarySteamFluid> STEAM =
            FLUIDS.register("steam", StorageOnlyOrdinarySteamFluid::new);
    public static boolean isSupercritical(FluidStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(BoilerContent.SUPERCRITICAL_STEAM.get());
    }
    public static boolean isOrdinarySteam(FluidStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(STEAM.get());
    }
    private TurbineContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TYPES.register(bus); FLUIDS.register(bus);
        bus.addListener(TurbineContent::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
            if (side == null || !state.hasProperty(TurbinePartBlock.OUTWARD)
                    || state.getValue(TurbinePartBlock.OUTWARD) != side) return null;
            var owner = TurbineStructure.ownerForPort(level, pos, state, true);
            return owner == null ? null : owner.port(pos, side, true);
        }, INLET.get());
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
            if (side == null || !state.hasProperty(TurbinePartBlock.OUTWARD)
                    || state.getValue(TurbinePartBlock.OUTWARD) != side) return null;
            var owner = TurbineStructure.ownerForPort(level, pos, state, false);
            return owner == null ? null : owner.port(pos, side, false);
        }, EXHAUST.get());
    }
}
