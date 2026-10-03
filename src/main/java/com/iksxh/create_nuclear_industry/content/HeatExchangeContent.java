package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlock;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import com.iksxh.create_nuclear_industry.heat.HeatExchangerBoilerBridge;
import com.simibubi.create.api.boiler.BoilerHeater;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.*;

/** 单格换热器的注册与公开锅炉能力接线；实体是唯一流体和热量所有者。 */
public final class HeatExchangeContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateNuclearIndustry.MOD_ID);
    public static final DeferredBlock<NuclearHeatExchangerBlock> NUCLEAR_HEAT_EXCHANGER = BLOCKS.register("nuclear_heat_exchanger",
            () -> new NuclearHeatExchangerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(4).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> NUCLEAR_HEAT_EXCHANGER_ITEM = ITEMS.register("nuclear_heat_exchanger",
            () -> new BlockItem(NUCLEAR_HEAT_EXCHANGER.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearHeatExchangerBlockEntity>> NUCLEAR_HEAT_EXCHANGER_BE =
            ENTITIES.register("nuclear_heat_exchanger", () -> BlockEntityType.Builder.of(
                    NuclearHeatExchangerBlockEntity::new, NUCLEAR_HEAT_EXCHANGER.get()).build(null));
    private HeatExchangeContent() {}

    /** 注册完成后才向 Create 注册真实方块身份，不暴露工作盆热级属性。 */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(HeatExchangeContent::setup);
        bus.addListener(HeatExchangeContent::capabilities);
    }
    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> BoilerHeater.REGISTRY.register(NUCLEAR_HEAT_EXCHANGER.get(), HeatExchangerBoilerBridge::heat));
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, NUCLEAR_HEAT_EXCHANGER_BE.get(),
                (machine, side) -> machine.fluidPort(side));
    }
}
