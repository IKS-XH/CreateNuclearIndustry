package com.iksxh.create_nuclear_industry.p0probe.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.p0probe.arm.P0ProbeArmInteractionPoint;
import com.iksxh.create_nuclear_industry.p0probe.block.P0ProbeArmTargetBlock;
import com.iksxh.create_nuclear_industry.p0probe.block.P0ProbeBlock;
import com.iksxh.create_nuclear_industry.p0probe.block.P0ProbeFluidPortBlock;
import com.iksxh.create_nuclear_industry.p0probe.block.P0ProbeGoggleBlock;
import com.iksxh.create_nuclear_industry.p0probe.block.P0ProbeSliderBlock;
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
 * Deliberately isolated registrations used only by the P0 API GameTests.
 * These are not added to the normal creative tab. The small survival recipes are
 * temporary test access only and must not be treated as P1 gameplay content.
 */
public final class P0ProbeContent {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);

    public static final DeferredBlock<Block> P0_PROBE_BLOCK = BLOCKS.register(
            "p0_probe_block",
            () -> new P0ProbeBlock(probeProperties())
    );
    public static final DeferredBlock<Block> P0_PROBE_SLIDER_BLOCK = BLOCKS.register(
            "p0_probe_slider_block",
            () -> new P0ProbeSliderBlock(probeProperties())
    );
    public static final DeferredBlock<Block> P0_PROBE_GOGGLE_BLOCK = BLOCKS.register(
            "p0_probe_goggle_block",
            () -> new P0ProbeGoggleBlock(probeProperties())
    );
    public static final DeferredBlock<Block> P0_PROBE_ARM_TARGET = BLOCKS.register(
            "p0_probe_arm_target",
            () -> new P0ProbeArmTargetBlock(probeProperties())
    );
    public static final DeferredBlock<Block> P0_PROBE_COLD_PORT = BLOCKS.register(
            "p0_probe_cold_port",
            () -> new P0ProbeFluidPortBlock(probeProperties(), true)
    );
    public static final DeferredBlock<Block> P0_PROBE_HOT_PORT = BLOCKS.register(
            "p0_probe_hot_port",
            () -> new P0ProbeFluidPortBlock(probeProperties(), false)
    );

    public static final DeferredItem<BlockItem> P0_PROBE_BLOCK_ITEM = blockItem("p0_probe_block", P0_PROBE_BLOCK);
    public static final DeferredItem<BlockItem> P0_PROBE_SLIDER_BLOCK_ITEM = blockItem("p0_probe_slider_block", P0_PROBE_SLIDER_BLOCK);
    public static final DeferredItem<BlockItem> P0_PROBE_GOGGLE_BLOCK_ITEM = blockItem("p0_probe_goggle_block", P0_PROBE_GOGGLE_BLOCK);
    public static final DeferredItem<BlockItem> P0_PROBE_ARM_TARGET_ITEM = blockItem("p0_probe_arm_target", P0_PROBE_ARM_TARGET);
    public static final DeferredItem<BlockItem> P0_PROBE_COLD_PORT_ITEM = blockItem("p0_probe_cold_port", P0_PROBE_COLD_PORT);
    public static final DeferredItem<BlockItem> P0_PROBE_HOT_PORT_ITEM = blockItem("p0_probe_hot_port", P0_PROBE_HOT_PORT);
    public static final DeferredItem<Item> P0_PROBE_REPAIR_ITEM = ITEMS.register(
            "p0_probe_repair_item",
            () -> new Item(new Item.Properties())
    );

    private P0ProbeContent() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        P0ProbeArmInteractionPoint.register(modEventBus);
    }

    private static BlockBehaviour.Properties probeProperties() {
        return BlockBehaviour.Properties.of()
                .strength(1.0F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    private static DeferredItem<BlockItem> blockItem(String id, DeferredBlock<Block> block) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
