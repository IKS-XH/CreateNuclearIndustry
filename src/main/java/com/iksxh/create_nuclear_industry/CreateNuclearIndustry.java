package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.ModBlocks;
import com.iksxh.create_nuclear_industry.content.ModCreativeTabs;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeContent;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeFluids;
import com.iksxh.create_nuclear_industry.p0probe.events.P0ProbeEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(CreateNuclearIndustry.MOD_ID)
public final class CreateNuclearIndustry {
    public static final String MOD_ID = "create_nuclear_industry";

    public CreateNuclearIndustry(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        P0ProbeContent.register(modEventBus);
        P0ProbeFluids.register(modEventBus);
        P0ProbeBlockEntities.register(modEventBus);
        modEventBus.addListener(CreateNuclearIndustry::registerP0Capabilities);
        NeoForge.EVENT_BUS.register(P0ProbeEvents.class);
    }

    private static void registerP0Capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                P0ProbeBlockEntities.P0_PROBE_ARM_TARGET.get(),
                (blockEntity, side) -> blockEntity.itemHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                P0ProbeBlockEntities.P0_PROBE_COLD_PORT.get(),
                (blockEntity, side) -> blockEntity.fluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                P0ProbeBlockEntities.P0_PROBE_HOT_PORT.get(),
                (blockEntity, side) -> blockEntity.fluidHandler()
        );
    }
}
