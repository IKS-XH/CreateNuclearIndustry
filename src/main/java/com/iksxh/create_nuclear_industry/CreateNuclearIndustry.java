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

/**
 * Create: Nuclear Industry 的 NeoForge 模组入口。
 *
 * <p>本类只负责配置、注册表、事件总线、网络 payload 与能力接线；反应堆权威状态由
 * 仪表端口方块实体拥有，不能在入口类中缓存或复制。</p>
 */
@Mod(CreateNuclearIndustry.MOD_ID)
public final class CreateNuclearIndustry {
    public static final String MOD_ID = "create_nuclear_industry";

    /** 在模组事件总线上完成配置、注册对象、网络协议和能力入口的装配。 */
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

    /**
     * 将正式冷却剂 capability 委托给端口的共享流体账本；端口 capability 不拥有独立库存。
     */
    private static void registerP1Capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                P1BlockEntities.REACTOR_PORT.get(),
                (blockEntity, side) -> ReactorCoolantFluidHandler.forPort(blockEntity)
        );
    }
}
