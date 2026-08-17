package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
