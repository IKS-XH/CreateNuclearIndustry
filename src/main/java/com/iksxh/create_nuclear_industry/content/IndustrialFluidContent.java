package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.industrial.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** 仅注册两管、单泵和两阀；不新增设备端口、储罐或工业原材料。 */
public final class IndustrialFluidContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateNuclearIndustry.MOD_ID);
    private static BlockBehaviour.Properties properties() { return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3).requiresCorrectToolForDrops().noOcclusion(); }
    public static final DeferredBlock<IndustrialPipeBlock> REINFORCED_PIPE = BLOCKS.register("reinforced_fluid_pipe", () -> new IndustrialPipeBlock(properties(), false));
    public static final DeferredBlock<IndustrialPipeBlock> CERAMIC_PIPE = BLOCKS.register("ceramic_fluid_pipe", () -> new IndustrialPipeBlock(properties(), true));
    public static final DeferredBlock<IndustrialPumpBlock> PUMP = BLOCKS.register("industrial_fluid_pump", () -> new IndustrialPumpBlock(properties()));
    public static final DeferredBlock<IndustrialValveBlock> REINFORCED_VALVE = BLOCKS.register("reinforced_fluid_valve", () -> new IndustrialValveBlock(properties(), false));
    public static final DeferredBlock<IndustrialValveBlock> CERAMIC_VALVE = BLOCKS.register("ceramic_fluid_valve", () -> new IndustrialValveBlock(properties(), true));
    public static final DeferredItem<BlockItem> REINFORCED_PIPE_ITEM = ITEMS.register("reinforced_fluid_pipe", () -> new BlockItem(REINFORCED_PIPE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CERAMIC_PIPE_ITEM = ITEMS.register("ceramic_fluid_pipe", () -> new BlockItem(CERAMIC_PIPE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> PUMP_ITEM = ITEMS.register("industrial_fluid_pump", () -> new BlockItem(PUMP.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> REINFORCED_VALVE_ITEM = ITEMS.register("reinforced_fluid_valve", () -> new BlockItem(REINFORCED_VALVE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CERAMIC_VALVE_ITEM = ITEMS.register("ceramic_fluid_valve", () -> new BlockItem(CERAMIC_VALVE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialPumpBlockEntity>> PUMP_ENTITY = ENTITIES.register("industrial_fluid_pump", () -> BlockEntityType.Builder.of(IndustrialPumpBlockEntity::new, PUMP.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialValveBlockEntity>> VALVE_ENTITY = ENTITIES.register("industrial_fluid_valve", () -> BlockEntityType.Builder.of(IndustrialValveBlockEntity::new, REINFORCED_VALVE.get(), CERAMIC_VALVE.get()).build(null));
    private IndustrialFluidContent() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); }
}
