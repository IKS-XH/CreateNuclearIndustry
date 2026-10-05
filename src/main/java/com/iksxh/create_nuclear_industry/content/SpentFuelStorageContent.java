package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.storage.DryStorageBlock;
import com.iksxh.create_nuclear_industry.storage.DryStorageBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 基础封存内容独立注册入口；普通材料、单件封装桶及贮存架共用正式身份。 */
public final class SpentFuelStorageContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CreateNuclearIndustry.MOD_ID);
    public static final DeferredItem<Item> GLASS_DUST = ITEMS.registerSimpleItem("glass_dust");
    public static final DeferredItem<Item> VITRIFICATION_MEDIUM = ITEMS.registerSimpleItem("vitrification_medium");
    public static final DeferredItem<Item> LEAD_SHIELDING_CASK = ITEMS.registerSimpleItem("lead_shielding_cask");
    public static final DeferredItem<Item> SEALED_SPENT_FUEL_CASK = ITEMS.registerSimpleItem("sealed_spent_fuel_cask", new Item.Properties().stacksTo(1));
    public static final DeferredBlock<DryStorageBlock> DRY_STORAGE_RACK = BLOCKS.register("dry_storage_rack",
            () -> new DryStorageBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> DRY_STORAGE_RACK_ITEM = ITEMS.register("dry_storage_rack",
            () -> new BlockItem(DRY_STORAGE_RACK.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DryStorageBlockEntity>> DRY_STORAGE_BE = ENTITIES.register("dry_storage_rack",
            () -> BlockEntityType.Builder.of(DryStorageBlockEntity::new, DRY_STORAGE_RACK.get()).build(null));
    private SpentFuelStorageContent() {}
    public static void register(IEventBus bus) {
        ITEMS.register(bus); BLOCKS.register(bus); ENTITIES.register(bus);
        bus.addListener(SpentFuelStorageContent::registerCapabilities);
    }
    /** 任一面均代理同一账本；底面在实体端只开放取出。 */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DRY_STORAGE_BE.get(), DryStorageBlockEntity::itemPort);
    }
}
