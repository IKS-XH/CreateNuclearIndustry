package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P1BlockRegistrationContractTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");
    private static final String BLOCK_ASSET_ROOT = "assets/create_nuclear_industry/";
    private static final String DATA_ROOT = "data/create_nuclear_industry/";

    private static final Map<String, String> BLOCK_FIELDS = Map.of(
            P1ContentIds.REACTOR_CASING_ID, "REACTOR_CASING",
            P1ContentIds.REACTOR_WINDOW_ID, "REACTOR_WINDOW",
            P1ContentIds.REACTOR_INSTRUMENT_PORT_ID, "REACTOR_INSTRUMENT_PORT",
            P1ContentIds.REACTOR_COLD_PORT_ID, "REACTOR_COLD_PORT",
            P1ContentIds.REACTOR_HOT_PORT_ID, "REACTOR_HOT_PORT",
            P1ContentIds.REACTOR_REFUELING_PORT_ID, "REACTOR_REFUELING_PORT",
            P1ContentIds.REACTOR_FUEL_ROD_ID, "REACTOR_FUEL_ROD",
            P1ContentIds.CONTROL_ROD_DRIVE_ID, "CONTROL_ROD_DRIVE"
    );

    @Test
    void registersEveryFormalP1BlockAndItsBlockItem() throws IOException {
        String source = readSource("com/iksxh/create_nuclear_industry/content/P1Blocks.java");

        for (Map.Entry<String, String> entry : BLOCK_FIELDS.entrySet()) {
            String id = entry.getKey();
            String field = entry.getValue();
            assertTrue(source.contains("P1ContentIds." + constantFor(id)), "missing block ID: " + id);
            assertTrue(source.contains("DeferredBlock<Block> " + field), "missing block registration: " + id);
            assertTrue(source.contains("DeferredItem<BlockItem> " + field + "_ITEM"),
                    "missing block item registration: " + id);
        }
        assertFalse(source.contains("BlockEntity"), "P1-DATA-03 must not add block entity behavior");
        assertFalse(source.contains("tick("), "P1-DATA-03 must not add runtime ticking");
        assertFalse(source.contains("alloy_steel_plate"), "removed steel alias must not return");
    }

    @Test
    void everyFormalBlockHasBlockStateModelsItemModelsTexturesAndSelfDrop() throws IOException {
        for (String id : BLOCK_FIELDS.keySet()) {
            String blockState = readResource(BLOCK_ASSET_ROOT + "blockstates/" + id + ".json");
            String blockModel = readResource(BLOCK_ASSET_ROOT + "models/block/" + id + ".json");
            String itemModel = readResource(BLOCK_ASSET_ROOT + "models/item/" + id + ".json");
            String loot = readResource(DATA_ROOT + "loot_table/blocks/" + id + ".json");

            assertTrue(blockState.contains("create_nuclear_industry:block/" + id));
            assertTrue(itemModel.contains("create_nuclear_industry:block/" + id));
            assertTrue(loot.contains("create_nuclear_industry:" + id));
            assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/textures/block/" + id + ".png"))
                            || Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/textures/block/" + id + "_side.png")),
                    "missing side/all texture for " + id);
            assertTrue(blockModel.contains("minecraft:block/"), "missing vanilla block model parent for " + id);
        }

        assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/textures/block/reactor_casing_bottom.png")));
        for (String id : new String[]{
                P1ContentIds.REACTOR_CASING_ID,
                P1ContentIds.REACTOR_INSTRUMENT_PORT_ID,
                P1ContentIds.REACTOR_COLD_PORT_ID,
                P1ContentIds.REACTOR_HOT_PORT_ID,
                P1ContentIds.REACTOR_REFUELING_PORT_ID,
                P1ContentIds.REACTOR_FUEL_ROD_ID,
                P1ContentIds.CONTROL_ROD_DRIVE_ID
        }) {
            assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/textures/block/" + id + "_top.png")),
                    "missing top texture for " + id);
        }
    }

    @Test
    void reactorWindowUsesTransparentWorldRenderingAndDisablesOcclusion() throws IOException {
        String blocks = readSource("com/iksxh/create_nuclear_industry/content/P1Blocks.java");
        String model = readResource(BLOCK_ASSET_ROOT + "models/block/" + P1ContentIds.REACTOR_WINDOW_ID + ".json");

        assertTrue(blocks.contains("P1ContentIds.REACTOR_WINDOW_ID, properties -> new Block(properties.noOcclusion())"));
        assertTrue(model.contains("\"render_type\": \"translucent\""));
    }

    @Test
    void formalBlocksAreInBothToolTagsAndTheCreativeTab() throws IOException {
        String pickaxe = readResource("data/minecraft/tags/block/mineable/pickaxe.json");
        String iron = readResource("data/minecraft/tags/block/needs_iron_tool.json");
        String creativeTab = readSource("com/iksxh/create_nuclear_industry/content/ModCreativeTabs.java");

        for (Map.Entry<String, String> entry : BLOCK_FIELDS.entrySet()) {
            String id = entry.getKey();
            String field = entry.getValue();
            assertTrue(pickaxe.contains("create_nuclear_industry:" + id));
            assertTrue(iron.contains("create_nuclear_industry:" + id));
            assertTrue(creativeTab.contains("P1Blocks." + field + "_ITEM.get()"),
                    "missing creative-tab entry: " + id);
        }
    }

    private static String constantFor(String id) {
        return switch (id) {
            case P1ContentIds.REACTOR_CASING_ID -> "REACTOR_CASING_ID";
            case P1ContentIds.REACTOR_WINDOW_ID -> "REACTOR_WINDOW_ID";
            case P1ContentIds.REACTOR_INSTRUMENT_PORT_ID -> "REACTOR_INSTRUMENT_PORT_ID";
            case P1ContentIds.REACTOR_COLD_PORT_ID -> "REACTOR_COLD_PORT_ID";
            case P1ContentIds.REACTOR_HOT_PORT_ID -> "REACTOR_HOT_PORT_ID";
            case P1ContentIds.REACTOR_REFUELING_PORT_ID -> "REACTOR_REFUELING_PORT_ID";
            case P1ContentIds.REACTOR_FUEL_ROD_ID -> "REACTOR_FUEL_ROD_ID";
            case P1ContentIds.CONTROL_ROD_DRIVE_ID -> "CONTROL_ROD_DRIVE_ID";
            default -> throw new IllegalArgumentException("unmapped block ID: " + id);
        };
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(JAVA_SOURCES.resolve(relativePath));
    }

    private static String readResource(String relativePath) throws IOException {
        return Files.readString(RESOURCES.resolve(relativePath));
    }
}
