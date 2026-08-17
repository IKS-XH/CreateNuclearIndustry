package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final String EXPERIMENTAL_REACTOR_CASING_ID = "experimental_reactor_casing";

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);

    public static final DeferredBlock<Block> EXPERIMENTAL_REACTOR_CASING = BLOCKS.register(
            EXPERIMENTAL_REACTOR_CASING_ID,
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())
    );

    public static final DeferredItem<BlockItem> EXPERIMENTAL_REACTOR_CASING_ITEM = ITEMS.register(
            EXPERIMENTAL_REACTOR_CASING_ID,
            () -> new BlockItem(EXPERIMENTAL_REACTOR_CASING.get(), new Item.Properties())
    );

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
