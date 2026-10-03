package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组创造模式标签页的注册入口。
 *
 * <p>标签页只组织可见的物品列表，不拥有反应堆运行状态；图标和展示物品通过
 * Deferred 注册对象读取，避免在注册阶段提前创建未完成注册的实例。</p>
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB,
            CreateNuclearIndustry.MOD_ID
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_nuclear_industry.main"))
                    .icon(() -> new ItemStack(ModBlocks.EXPERIMENTAL_REACTOR_CASING_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.EXPERIMENTAL_REACTOR_CASING_ITEM.get());
                        output.accept(P1Blocks.REACTOR_CASING_ITEM.get());
                        output.accept(P1Blocks.REACTOR_WINDOW_ITEM.get());
                        output.accept(P1Blocks.REACTOR_INSTRUMENT_PORT_ITEM.get());
                        output.accept(P1Blocks.REACTOR_COLD_PORT_ITEM.get());
                        output.accept(P1Blocks.REACTOR_HOT_PORT_ITEM.get());
                        output.accept(P1Blocks.REACTOR_REFUELING_PORT_ITEM.get());
                        output.accept(P1Blocks.REACTOR_FUEL_ROD_ITEM.get());
                        output.accept(P1Blocks.CONTROL_ROD_DRIVE_ITEM.get());
                        output.accept(ModItems.FRESH_FUEL_ASSEMBLY.get());
                        output.accept(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get());
                        output.accept(ModItems.CONTROL_ROD.get());
                        output.accept(ModItems.STEEL_PLATE.get());
                        output.accept(ModItems.LAPIS_DUST.get());
                        output.accept(ModItems.COMPOUND_COOLANT_BUCKET.get());
                        for (OreContent.Mineral mineral : OreContent.MINERALS) {
                            output.accept(mineral.ore().get());
                            output.accept(mineral.deepslateOre().get());
                            output.accept(mineral.raw().get());
                            output.accept(mineral.rawBlock().get());
                        }
                        output.accept(BasicMaterialContent.LEAD_INGOT.get());
                        output.accept(BasicMaterialContent.TIN_INGOT.get());
                        output.accept(BasicMaterialContent.LEAD_PLATE.get());
                        output.accept(BasicMaterialContent.TIN_PLATE.get());
                        output.accept(BasicMaterialContent.LEAD_NUGGET.get());
                        output.accept(BasicMaterialContent.TIN_NUGGET.get());
                        output.accept(BasicMaterialContent.IRON_DUST.get());
                        output.accept(BasicMaterialContent.COAL_DUST.get());
                        output.accept(BasicMaterialContent.CHARCOAL_DUST.get());
                        output.accept(BasicMaterialContent.STEEL_DUST.get());
                        output.accept(BasicMaterialContent.STEEL_INGOT.get());
                        output.accept(BasicMaterialContent.TIN_WIRE.get());
                        output.accept(BasicMaterialContent.INDUSTRIAL_SENSOR.get());
                        output.accept(BasicMaterialContent.RADIATION_SENSOR.get());
                        output.accept(BasicMaterialContent.QUARTZ_DUST.get());
                        output.accept(BasicMaterialContent.REFRACTORY_BRICK.get());
                        output.accept(BasicMaterialContent.HEAVY_BEARING.get());
                        output.accept(BasicMaterialContent.SOLDER_INGOT.get());
                        output.accept(BasicMaterialContent.FUEL_CLADDING_TUBE.get());
                        output.accept(BasicMaterialContent.STEEL_MESH.get());
                        output.accept(BasicMaterialContent.STEEL_GRATE.get());
                        output.accept(FuelProcessingContent.URANIUM_CONCENTRATE.get());
                        output.accept(FuelProcessingContent.URANIUM_TAILINGS.get());
                        output.accept(FuelProcessingContent.URANIUM_TAILINGS_BRICK_ITEM.get());
                        output.accept(FuelProcessingContent.LOW_ENRICHED_URANIUM_DUST.get());
                        output.accept(FuelProcessingContent.DEPLETED_URANIUM_DUST.get());
                        output.accept(FuelProcessingContent.URANIUM_SLURRY_BUCKET.get());
                        output.accept(FuelProcessingContent.ENRICHMENT_CENTRIFUGE_ITEM.get());
                        output.accept(FuelProcessingContent.GREEN_FUEL_PELLET.get());
                        output.accept(FuelProcessingContent.SINTERED_FUEL_PELLET.get());
                        output.accept(FuelProcessingContent.FUEL_SINTERING_FURNACE_ITEM.get());
                        output.accept(FuelProcessingContent.SHIELDED_ASSEMBLY_STATION_ITEM.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    /** 在模组注册阶段注册创造模式标签页。 */
    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
