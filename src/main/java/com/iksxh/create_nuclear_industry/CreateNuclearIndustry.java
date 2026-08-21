package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.ModBlocks;
import com.iksxh.create_nuclear_industry.content.ModCreativeTabs;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderNetwork;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeContent;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeFluids;
import com.iksxh.create_nuclear_industry.p0probe.events.P0ProbeEvents;
import com.iksxh.create_nuclear_industry.reactor.ReactorCoolantFluidHandler;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(CreateNuclearIndustry.MOD_ID)
public final class CreateNuclearIndustry {
    public static final String MOD_ID = "create_nuclear_industry";

    public CreateNuclearIndustry(IEventBus modEventBus, ModContainer modContainer) {
        P1ServerConfig.register(modContainer);
        ModFluids.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        P1Blocks.register(modEventBus);
        P1BlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        P0ProbeContent.register(modEventBus);
        P0ProbeFluids.register(modEventBus);
        P0ProbeBlockEntities.register(modEventBus);
        modEventBus.addListener(ControlRodSliderNetwork::registerPayloads);
        modEventBus.addListener(CreateNuclearIndustry::registerP0Capabilities);
        modEventBus.addListener(CreateNuclearIndustry::registerP1Capabilities);
        NeoForge.EVENT_BUS.register(P0ProbeEvents.class);
        NeoForge.EVENT_BUS.register(ReactorStructureLifecycle.class);
        NeoForge.EVENT_BUS.register(ControlRodSliderNetwork.class);
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

    private static void registerP1Capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                P1BlockEntities.REACTOR_PORT.get(),
                (blockEntity, side) -> ReactorCoolantFluidHandler.forPort(blockEntity)
        );
    }
}
