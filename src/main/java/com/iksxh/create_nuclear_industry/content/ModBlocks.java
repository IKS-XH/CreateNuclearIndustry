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

/**
 * 旧实验反应堆外壳的注册层。
 *
 * <p>该类保留早期样例内容，与正式 P1 反应堆注册分开；对象只有在模组事件总线
 * 完成 {@link #register(IEventBus)} 后才交给 NeoForge 建立正式注册对象。</p>
 */
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

    /** 将样例方块和对应方块物品挂入模组事件总线的注册阶段。 */
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
