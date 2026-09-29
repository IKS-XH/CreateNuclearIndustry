package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.List;

/** 三矿资源入口；仅注册矿石、粗矿块和粗矿，粉碎产物沿用 Create 身份。 */
public final class OreContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);
    public static final Mineral LEAD = mineral("lead", Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK);
    public static final Mineral TIN = mineral("tin", Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK);
    public static final Mineral URANIUM = mineral("uranium", Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK);
    public static final List<Mineral> MINERALS = List.of(LEAD, TIN, URANIUM);

    /** 每种矿的注册引用；客户端展示和服务端采集使用同一组正式身份。 */
    public record Mineral(String name, DeferredBlock<Block> ore, DeferredBlock<Block> deepslateOre,
                          DeferredBlock<Block> rawBlock, DeferredItem<Item> raw) {}

    private OreContent() {}

    /** 在模组事件总线注册12个物品身份和其中9个方块，不接入反应堆状态。 */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private static Mineral mineral(String name, Block stone, Block deep, Block storage) {
        return new Mineral(name, block(name + "_ore", stone), block("deepslate_" + name + "_ore", deep),
                block("raw_" + name + "_block", storage), ITEMS.registerSimpleItem("raw_" + name));
    }

    private static DeferredBlock<Block> block(String name, Block reference) {
        // 只复制原版物理属性；用普通 Block 保证无额外挖矿经验，工具门由标签控制。
        DeferredBlock<Block> block = BLOCKS.register(name,
                () -> new Block(BlockBehaviour.Properties.ofFullCopy(reference).requiresCorrectToolForDrops()));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
