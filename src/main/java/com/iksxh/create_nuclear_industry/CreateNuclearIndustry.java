package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.ModBlocks;
import com.iksxh.create_nuclear_industry.content.BasicMaterialContent;
import com.iksxh.create_nuclear_industry.content.ModCreativeTabs;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.OreContent;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.iksxh.create_nuclear_industry.production.CentrifugeBlock;
import com.iksxh.create_nuclear_industry.production.ShieldedAssemblyArmInteractionPoint;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.iksxh.create_nuclear_industry.worldgen.OreGenerationFilter;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.content.P1BlockEntities;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderNetwork;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeContent;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeFluids;
import com.iksxh.create_nuclear_industry.p0probe.events.P0ProbeEvents;
import com.iksxh.create_nuclear_industry.ponder.P1PonderPlugin;
import com.iksxh.create_nuclear_industry.reactor.ReactorCoolantFluidHandler;
import com.iksxh.create_nuclear_industry.reactor.FuelRefuelingArmInteractionPoint;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
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
        OreContent.register(modEventBus);
        BasicMaterialContent.register(modEventBus);
        FuelProcessingContent.register(modEventBus);
        BlockMovementChecks.registerMovementAllowedCheck((state, level, pos) ->
                state.is(FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get())
                        ? BlockMovementChecks.CheckResult.FAIL : BlockMovementChecks.CheckResult.PASS);
        OreGenerationFilter.register(modEventBus);
        ModBlocks.register(modEventBus);
        P1Blocks.register(modEventBus);
        P1BlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        P0ProbeContent.register(modEventBus);
        P0ProbeFluids.register(modEventBus);
        P0ProbeBlockEntities.register(modEventBus);
        FuelRefuelingArmInteractionPoint.register(modEventBus);
        ShieldedAssemblyArmInteractionPoint.register(modEventBus);
        modEventBus.addListener(ControlRodSliderNetwork::registerPayloads);
        modEventBus.addListener(CreateNuclearIndustry::registerPonder);
        modEventBus.addListener(CreateNuclearIndustry::registerP0Capabilities);
        modEventBus.addListener(CreateNuclearIndustry::registerP1Capabilities);
        modEventBus.addListener(CreateNuclearIndustry::registerCentrifugeCapabilities);
        modEventBus.addListener(CreateNuclearIndustry::registerFuelSinteringCapabilities);
        modEventBus.addListener(CreateNuclearIndustry::registerShieldedAssemblyCapabilities);
        NeoForge.EVENT_BUS.register(P0ProbeEvents.class);
        NeoForge.EVENT_BUS.register(ReactorStructureLifecycle.class);
        NeoForge.EVENT_BUS.register(ControlRodSliderNetwork.class);
    }

    /**
     * 仅在客户端初始化阶段接入 Ponder；教学场景只操作临时演示世界，不参与服务端模拟。
     */
    private static void registerPonder(FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new P1PonderPlugin());
    }

    /** 注册 P0 历史探针的物品与流体 capability，不把探针状态接入正式 P1 反应堆。 */
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

    /** 上下两格按位置代理到唯一有效下段；每个返回的 handler 自身也逐次复核配对。 */
    private static void registerCentrifugeCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
            var machine = CentrifugeBlock.owner(level, pos, state);
            return machine == null ? null : machine.fluidPort(side,
                    state.getValue(CentrifugeBlock.HALF) == DoubleBlockHalf.UPPER);
        }, FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get());
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
            var machine = CentrifugeBlock.owner(level, pos, state);
            return machine == null ? null : machine.itemPort(side,
                    state.getValue(CentrifugeBlock.HALF) == DoubleBlockHalf.UPPER);
        }, FuelProcessingContent.ENRICHMENT_CENTRIFUGE.get());
    }

    /** 炉体只开放顶面输入与四侧输出；底面留给 Create 燃烧室。 */
    private static void registerFuelSinteringCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FuelProcessingContent.FUEL_SINTERING_BE.get(),
                (machine, side) -> machine.itemPort(side));
    }

    /** 装配台顶面只收四料，水平四面共享唯一成品槽，底部轴和未指定面无物料能力。 */
    private static void registerShieldedAssemblyCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FuelProcessingContent.SHIELDED_ASSEMBLY_BE.get(),
                (machine, side) -> machine.itemPort(side));
    }
}
