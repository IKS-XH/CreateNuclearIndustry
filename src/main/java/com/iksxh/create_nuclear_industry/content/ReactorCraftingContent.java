package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 固定实验堆生存制造所需的独立材料身份；序列半成品只承载 Create 原生加工进度。 */
public final class ReactorCraftingContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);

    public static final DeferredItem<Item> STEEL_ROD = ITEMS.registerSimpleItem("steel_rod");
    public static final DeferredItem<Item> SEAL_RING = ITEMS.registerSimpleItem("seal_ring");
    public static final DeferredItem<Item> PRESSURE_FITTING = ITEMS.registerSimpleItem("pressure_fitting");
    public static final DeferredItem<Item> INDUSTRIAL_CERAMIC = ITEMS.registerSimpleItem("industrial_ceramic");
    public static final DeferredItem<Item> NEUTRON_ABSORBING_CERAMIC = ITEMS.registerSimpleItem("neutron_absorbing_ceramic");
    public static final DeferredItem<Item> SHIELDED_GLASS = ITEMS.registerSimpleItem("shielded_glass");

    public static final DeferredBlock<Block> SHIELDING_CONCRETE = BLOCKS.register("shielding_concrete",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_CONCRETE)
                    .strength(3.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> SHIELDING_CONCRETE_ITEM = ITEMS.register("shielding_concrete",
            () -> new BlockItem(SHIELDING_CONCRETE.get(), new Item.Properties()));

    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_SHIELDED_GLASS =
            ITEMS.registerItem("incomplete_shielded_glass", SequencedAssemblyItem::new);
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_REACTOR_INSTRUMENT_PORT =
            ITEMS.registerItem("incomplete_reactor_instrument_port", SequencedAssemblyItem::new);
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_REACTOR_REFUELING_PORT =
            ITEMS.registerItem("incomplete_reactor_refueling_port", SequencedAssemblyItem::new);
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_CONTROL_ROD =
            ITEMS.registerItem("incomplete_control_rod", SequencedAssemblyItem::new);
    public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_CONTROL_ROD_DRIVE =
            ITEMS.registerItem("incomplete_control_rod_drive", SequencedAssemblyItem::new);

    private ReactorCraftingContent() {}

    /** 将本批普通材料、混凝土方块和 Create 序列装配半成品接入模组注册总线。 */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
