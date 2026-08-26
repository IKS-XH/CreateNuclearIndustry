package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.block.ControlRodDriveBlock;
import com.iksxh.create_nuclear_industry.block.ReactorInstrumentPortBlock;
import com.iksxh.create_nuclear_industry.block.ReactorPortBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

/**
 * 正式 P1 方块及其方块物品的注册层。
 *
 * <p>注册 ID 统一来自 {@link P1ContentIds}；本类只建立 Deferred 注册对象，
 * 结构扫描、控制交互和服务端模拟由方块及方块实体层负责。</p>
 */
public final class P1Blocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);

    public static final DeferredBlock<Block> REACTOR_CASING = registerBlock(P1ContentIds.REACTOR_CASING_ID);
    public static final DeferredBlock<Block> REACTOR_WINDOW = registerBlock(
            P1ContentIds.REACTOR_WINDOW_ID, properties -> new Block(properties.noOcclusion()));
    public static final DeferredBlock<Block> REACTOR_INSTRUMENT_PORT = registerBlock(
            P1ContentIds.REACTOR_INSTRUMENT_PORT_ID, ReactorInstrumentPortBlock::new);
    public static final DeferredBlock<Block> REACTOR_COLD_PORT = registerBlock(
            P1ContentIds.REACTOR_COLD_PORT_ID, ReactorPortBlock::new);
    public static final DeferredBlock<Block> REACTOR_HOT_PORT = registerBlock(
            P1ContentIds.REACTOR_HOT_PORT_ID, ReactorPortBlock::new);
    public static final DeferredBlock<Block> REACTOR_REFUELING_PORT = registerBlock(
            P1ContentIds.REACTOR_REFUELING_PORT_ID, ReactorPortBlock::new);
    public static final DeferredBlock<Block> REACTOR_FUEL_ROD = registerBlock(P1ContentIds.REACTOR_FUEL_ROD_ID);
    public static final DeferredBlock<Block> CONTROL_ROD_DRIVE = registerBlock(
            P1ContentIds.CONTROL_ROD_DRIVE_ID, ControlRodDriveBlock::new);

    public static final DeferredItem<BlockItem> REACTOR_CASING_ITEM = registerBlockItem(
            P1ContentIds.REACTOR_CASING_ID, REACTOR_CASING);
    public static final DeferredItem<BlockItem> REACTOR_WINDOW_ITEM = registerBlockItem(
            P1ContentIds.REACTOR_WINDOW_ID, REACTOR_WINDOW);
    public static final DeferredItem<BlockItem> REACTOR_INSTRUMENT_PORT_ITEM = registerBlockItem(
            P1ContentIds.REACTOR_INSTRUMENT_PORT_ID, REACTOR_INSTRUMENT_PORT);
    public static final DeferredItem<BlockItem> REACTOR_COLD_PORT_ITEM = registerBlockItem(
            P1ContentIds.REACTOR_COLD_PORT_ID, REACTOR_COLD_PORT);
    public static final DeferredItem<BlockItem> REACTOR_HOT_PORT_ITEM = registerBlockItem(
            P1ContentIds.REACTOR_HOT_PORT_ID, REACTOR_HOT_PORT);
    public static final DeferredItem<BlockItem> REACTOR_REFUELING_PORT_ITEM = registerBlockItem(
            P1ContentIds.REACTOR_REFUELING_PORT_ID, REACTOR_REFUELING_PORT);
    public static final DeferredItem<BlockItem> REACTOR_FUEL_ROD_ITEM = registerBlockItem(
            P1ContentIds.REACTOR_FUEL_ROD_ID, REACTOR_FUEL_ROD);
    public static final DeferredItem<BlockItem> CONTROL_ROD_DRIVE_ITEM = registerBlockItem(
            P1ContentIds.CONTROL_ROD_DRIVE_ID, CONTROL_ROD_DRIVE);

    private P1Blocks() {
    }

    /** 将正式 P1 方块和方块物品提交到模组事件总线。 */
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    private static DeferredBlock<Block> registerBlock(String id) {
        return registerBlock(id, Block::new);
    }

    private static DeferredBlock<Block> registerBlock(String id, Function<BlockBehaviour.Properties, Block> factory) {
        return BLOCKS.register(id, () -> factory.apply(BlockBehaviour.Properties.of()
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()));
    }

    private static DeferredItem<BlockItem> registerBlockItem(String id, DeferredBlock<Block> block) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
