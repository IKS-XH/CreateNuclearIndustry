package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.accident.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** 事故废料与携带单件载荷的残骸；外观使用原版资源，无独立GUI或清理耗材。 */
public final class AccidentContent {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE,CreateNuclearIndustry.MOD_ID);
    public static final DeferredItem<Item> ACCIDENT_FUEL_WASTE=ITEMS.registerSimpleItem("accident_fuel_waste",new Item.Properties().stacksTo(1).fireResistant());
    public static final DeferredBlock<ReactorCoriumBlock> REACTOR_CORIUM=BLOCKS.register("reactor_corium",
            ()->new ReactorCoriumBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(3).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> REACTOR_CORIUM_ITEM=ITEMS.register("reactor_corium",()->new BlockItem(REACTOR_CORIUM.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ReactorCoriumBlockEntity>> CORIUM_BE=ENTITIES.register("reactor_corium",
            ()->BlockEntityType.Builder.of(ReactorCoriumBlockEntity::new,REACTOR_CORIUM.get()).build(null));
    public static final DeferredBlock<AccidentFireBlock> ACCIDENT_FIRE=BLOCKS.register("accident_fire",
            ()->new AccidentFireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FIRE).noLootTable()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<AccidentFireBlock.Lease>> FIRE_BE=ENTITIES.register("accident_fire",
            ()->BlockEntityType.Builder.of(AccidentFireBlock.Lease::new,ACCIDENT_FIRE.get()).build(null));
    private AccidentContent() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); BLOCKS.register(bus); ENTITIES.register(bus); }
}
