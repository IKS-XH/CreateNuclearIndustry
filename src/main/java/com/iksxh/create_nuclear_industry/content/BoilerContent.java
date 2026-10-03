package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlock;
import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerPartBlock;
import com.iksxh.create_nuclear_industry.boiler.BoilerStructure;
import com.iksxh.create_nuclear_industry.boiler.BoilerShellBlock;
import com.iksxh.create_nuclear_industry.boiler.StorageOnlySteamFluid;
import com.iksxh.create_nuclear_industry.boiler.BoilerPortBlock;
import com.iksxh.create_nuclear_industry.boiler.BoilerPortBlockEntity;
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

/** 七种锅炉部件和仅用于罐/管网的超临界蒸汽注册；无桶或世界放置入口。 */
public final class BoilerContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, CreateNuclearIndustry.MOD_ID);
    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4).requiresCorrectToolForDrops();
    }
    public static final DeferredBlock<BoilerShellBlock> CASING = BLOCKS.register("high_pressure_boiler_casing", () -> new BoilerShellBlock(metal()));
    public static final DeferredBlock<BoilerShellBlock> WINDOW = BLOCKS.register("high_pressure_boiler_window", () -> new BoilerShellBlock(metal().noOcclusion()));
    public static final DeferredBlock<BoilerPortBlock> WATER_PORT = BLOCKS.register("high_pressure_boiler_water_port", () -> new BoilerPortBlock(metal(), true));
    public static final DeferredBlock<BoilerPortBlock> STEAM_PORT = BLOCKS.register("high_pressure_boiler_steam_port", () -> new BoilerPortBlock(metal(), false));
    public static final DeferredBlock<BoilerControllerBlock> CONTROLLER = BLOCKS.register("high_pressure_boiler_controller", () -> new BoilerControllerBlock(metal()));
    public static final DeferredBlock<BoilerShellBlock> SAFETY_VALVE = BLOCKS.register("boiler_safety_valve", () -> new BoilerShellBlock(metal()));
    public static final DeferredBlock<BoilerShellBlock> HEAT_SECTION = BLOCKS.register("boiler_heat_exchange_section", () -> new BoilerShellBlock(metal()));
    public static final DeferredItem<BlockItem> CASING_ITEM = item("high_pressure_boiler_casing", CASING, false);
    public static final DeferredItem<BlockItem> WINDOW_ITEM = item("high_pressure_boiler_window", WINDOW, false);
    public static final DeferredItem<BlockItem> WATER_PORT_ITEM = item("high_pressure_boiler_water_port", WATER_PORT, false);
    public static final DeferredItem<BlockItem> STEAM_PORT_ITEM = item("high_pressure_boiler_steam_port", STEAM_PORT, false);
    public static final DeferredItem<BlockItem> CONTROLLER_ITEM = item("high_pressure_boiler_controller", CONTROLLER, true);
    public static final DeferredItem<BlockItem> SAFETY_VALVE_ITEM = item("boiler_safety_valve", SAFETY_VALVE, false);
    public static final DeferredItem<BlockItem> HEAT_SECTION_ITEM = item("boiler_heat_exchange_section", HEAT_SECTION, false);
    private static DeferredItem<BlockItem> item(String id, DeferredBlock<? extends Block> block, boolean portable) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), portable ? new Item.Properties().stacksTo(1) : new Item.Properties()));
    }
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerControllerBlockEntity>> CONTROLLER_BE =
            ENTITIES.register("high_pressure_boiler_controller", () -> BlockEntityType.Builder.of(
                    BoilerControllerBlockEntity::new, CONTROLLER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerPortBlockEntity>> PORT_BE =
            ENTITIES.register("high_pressure_boiler_port", () -> BlockEntityType.Builder.of(
                    BoilerPortBlockEntity::new, WATER_PORT.get(), STEAM_PORT.get()).build(null));
    public static final DeferredHolder<FluidType, FluidType> SUPERCRITICAL_TYPE = TYPES.register("supercritical_steam", () -> {
        ResourceLocation still = ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "fluid/supercritical_steam_still");
        ResourceLocation flowing = ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "fluid/supercritical_steam_flow");
        return new FluidType(FluidType.Properties.create().descriptionId("fluid_type.create_nuclear_industry.supercritical_steam")
                .density(-1000).viscosity(100).temperature(900)) {
            @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override public ResourceLocation getStillTexture() { return still; }
                    @Override public ResourceLocation getFlowingTexture() { return flowing; }
                });
            }
        };
    });
    public static final DeferredHolder<Fluid, StorageOnlySteamFluid> SUPERCRITICAL_STEAM =
            FLUIDS.register("supercritical_steam", StorageOnlySteamFluid::new);
    public static boolean isSteam(FluidStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(SUPERCRITICAL_STEAM.get());
    }
    private BoilerContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TYPES.register(bus); FLUIDS.register(bus);
        bus.addListener(BoilerContent::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
            var owner = BoilerStructure.owner(level, pos, true);
            return owner == null ? null : owner.port(pos, side, true);
        }, WATER_PORT.get());
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
            var owner = BoilerStructure.owner(level, pos, false);
            return owner == null ? null : owner.port(pos, side, false);
        }, STEAM_PORT.get());
    }
}
