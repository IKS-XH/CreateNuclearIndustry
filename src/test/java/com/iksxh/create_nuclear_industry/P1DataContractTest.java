package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P1DataContractTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Path ASSETS = RESOURCES.resolve("assets/create_nuclear_industry");
    private static final Path DATA = RESOURCES.resolve("data");

    private static final List<String> BLOCK_IDS = List.of(
            P1ContentIds.REACTOR_CASING_ID,
            P1ContentIds.REACTOR_WINDOW_ID,
            P1ContentIds.REACTOR_INSTRUMENT_PORT_ID,
            P1ContentIds.REACTOR_COLD_PORT_ID,
            P1ContentIds.REACTOR_HOT_PORT_ID,
            P1ContentIds.REACTOR_REFUELING_PORT_ID,
            P1ContentIds.REACTOR_FUEL_ROD_ID,
            P1ContentIds.CONTROL_ROD_DRIVE_ID
    );
    private static final List<String> ITEM_IDS = List.of(
            P1ContentIds.FRESH_FUEL_ASSEMBLY_ID,
            P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID,
            P1ContentIds.CONTROL_ROD_ID,
            P1ContentIds.STEEL_PLATE_ID
    );
    private static final List<String> FLUID_IDS = List.of(
            P1ContentIds.COMPOUND_COOLANT_ID,
            P1ContentIds.HOT_COMPOUND_COOLANT_ID
    );

    @Test
    void everyFormalBlockHasTheCompleteResourceChain() throws IOException {
        for (String id : BLOCK_IDS) {
            assertTrue(Files.exists(ASSETS.resolve("blockstates/" + id + ".json")),
                    "missing blockstate: " + id);
            assertTrue(Files.exists(ASSETS.resolve("models/block/" + id + ".json")),
                    "missing block model: " + id);
            assertTrue(Files.exists(ASSETS.resolve("models/item/" + id + ".json")),
                    "missing block item model: " + id);
            assertTrue(Files.exists(DATA.resolve("create_nuclear_industry/loot_table/blocks/" + id + ".json")),
                    "missing block loot table: " + id);
            assertTrue(read("assets/create_nuclear_industry/lang/en_us.json")
                            .contains("\"block.create_nuclear_industry." + id + "\""),
                    "missing English block language key: " + id);
            assertTrue(read("assets/create_nuclear_industry/lang/zh_cn.json")
                            .contains("\"block.create_nuclear_industry." + id + "\""),
                    "missing Chinese block language key: " + id);
        }
    }

    @Test
    void everyFormalItemAndFluidHasItsApplicableResourceAndLanguageContract() throws IOException {
        String english = read("assets/create_nuclear_industry/lang/en_us.json");
        String chinese = read("assets/create_nuclear_industry/lang/zh_cn.json");

        for (String id : ITEM_IDS) {
            assertTrue(Files.exists(ASSETS.resolve("models/item/" + id + ".json")),
                    "missing item model: " + id);
            assertTrue(Files.exists(ASSETS.resolve("textures/item/" + id + ".png")),
                    "missing item texture: " + id);
            assertTrue(english.contains("\"item.create_nuclear_industry." + id + "\""),
                    "missing English item language key: " + id);
            assertTrue(chinese.contains("\"item.create_nuclear_industry." + id + "\""),
                    "missing Chinese item language key: " + id);
        }
        for (String id : FLUID_IDS) {
            assertTrue(Files.exists(ASSETS.resolve("textures/fluid/" + id + "_still.png")),
                    "missing still fluid texture: " + id);
            assertTrue(Files.exists(ASSETS.resolve("textures/fluid/" + id + "_flow.png")),
                    "missing flowing fluid texture: " + id);
            assertTrue(english.contains("\"fluid_type.create_nuclear_industry." + id + "\""),
                    "missing English fluid language key: " + id);
            assertTrue(chinese.contains("\"fluid_type.create_nuclear_industry." + id + "\""),
                    "missing Chinese fluid language key: " + id);
        }
    }

    @Test
    void applicableTagsCoverBlocksAndSteelPlate() throws IOException {
        String pickaxe = read("data/minecraft/tags/block/mineable/pickaxe.json");
        String ironTool = read("data/minecraft/tags/block/needs_iron_tool.json");
        String steelPlate = read("data/c/tags/item/plates/steel.json");

        for (String id : BLOCK_IDS) {
            assertTrue(pickaxe.contains("create_nuclear_industry:" + id),
                    "missing pickaxe tag member: " + id);
            assertTrue(ironTool.contains("create_nuclear_industry:" + id),
                    "missing needs-iron-tool tag member: " + id);
        }
        assertTrue(steelPlate.contains("create_nuclear_industry:" + P1ContentIds.STEEL_PLATE_ID));
    }

    @Test
    void noP1SurvivalRecipesOrPollutedCoolantRouteWereAdded() throws IOException {
        Path recipeDirectory = DATA.resolve("create_nuclear_industry/recipe");
        if (!Files.exists(recipeDirectory)) {
            return;
        }
        try (var files = Files.walk(recipeDirectory)) {
            files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try {
                    String source = Files.readString(path);
                    for (String id : List.of(
                            P1ContentIds.FRESH_FUEL_ASSEMBLY_ID,
                            P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID,
                            P1ContentIds.STEEL_PLATE_ID,
                            P1ContentIds.COMPOUND_COOLANT_ID,
                            P1ContentIds.HOT_COMPOUND_COOLANT_ID,
                            "contaminated_compound_coolant",
                            "coolant_purifier"
                    )) {
                        assertFalse(source.contains(id), "P1 recipe route was added for " + id + " in " + path);
                    }
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(RESOURCES.resolve(relativePath));
    }
}
