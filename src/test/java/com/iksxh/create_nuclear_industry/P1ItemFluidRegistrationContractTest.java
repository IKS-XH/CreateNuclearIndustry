package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证正式物品、冷/热流体注册、语言条目和创造标签页暴露关系。 */
class P1ItemFluidRegistrationContractTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");

    @Test
    void registersOnlyTheFormalP1ItemsAndTwoCoolantTypes() throws IOException {
        String items = readSource("com/iksxh/create_nuclear_industry/content/ModItems.java");
        String fluids = readSource("com/iksxh/create_nuclear_industry/content/ModFluids.java");

        for (String id : new String[]{
                P1ContentIds.FRESH_FUEL_ASSEMBLY_ID,
                P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID,
                P1ContentIds.CONTROL_ROD_ID,
                P1ContentIds.STEEL_PLATE_ID
        }) {
            assertTrue(items.contains("P1ContentIds." + constantFor(id)), "missing item registration: " + id);
        }
        assertTrue(fluids.contains("P1ContentIds.COMPOUND_COOLANT_ID"));
        assertTrue(fluids.contains("P1ContentIds.HOT_COMPOUND_COOLANT_ID"));
        assertTrue(items.contains("P1ContentIds.COMPOUND_COOLANT_BUCKET_ID"));
        assertTrue(items.contains("new BucketItem(ModFluids.COMPOUND_COOLANT_SOURCE.get()"));
        assertFalse(items.contains("ENGINEER_GOGGLES"), "the mod must not register Create goggles");
        assertFalse(items.contains("alloy_steel_plate"), "the removed steel alias must not return");
        assertFalse(fluids.contains("purifier"));
        assertFalse(fluids.contains("contaminated"));
    }

    @Test
    void freshFuelAssemblyUsesDurabilityWithoutComponentTemperatureOrIntegrity() throws IOException {
        String items = readSource("com/iksxh/create_nuclear_industry/content/ModItems.java");

        assertEquals(216_000, ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY);
        assertTrue(items.contains(".durability(FRESH_FUEL_ASSEMBLY_MAX_DURABILITY)"));
        assertFalse(items.contains("temperature"));
        assertFalse(items.contains("integrity"));
    }

    @Test
    void itemModelsAndTexturesExistForEachRegisteredItem() throws IOException {
        Map<String, String> textureIds = Map.of(
                P1ContentIds.FRESH_FUEL_ASSEMBLY_ID, "fresh_fuel_assembly",
                P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID, "cooled_spent_fuel_assembly",
                P1ContentIds.CONTROL_ROD_ID, "control_rod",
                P1ContentIds.STEEL_PLATE_ID, "steel_plate"
        );

        for (Map.Entry<String, String> entry : textureIds.entrySet()) {
            String model = readResource("assets/create_nuclear_industry/models/item/" + entry.getKey() + ".json");
            assertTrue(model.contains("create_nuclear_industry:item/" + entry.getValue()));
            assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/textures/item/" + entry.getValue() + ".png")));
        }
        for (String id : new String[]{P1ContentIds.COMPOUND_COOLANT_ID, P1ContentIds.HOT_COMPOUND_COOLANT_ID}) {
            assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/textures/fluid/" + id + "_still.png")));
            assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/textures/fluid/" + id + "_flow.png")));
            assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/textures/block/" + id + "_still.png")));
            assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/textures/block/" + id + "_flow.png")));
        }
        String bucketModel = readResource("assets/create_nuclear_industry/models/item/"
                + P1ContentIds.COMPOUND_COOLANT_BUCKET_ID + ".json");
        assertTrue(bucketModel.contains("\"loader\": \"neoforge:fluid_container\""));
        assertTrue(bucketModel.contains("\"parent\": \"neoforge:item/bucket\""));
        assertTrue(bucketModel.contains("\"fluid\": \"create_nuclear_industry:compound_coolant\""));
        assertFalse(bucketModel.contains("\"parent\": \"minecraft:item/generated\""));
        assertFalse(bucketModel.contains("\"base\""));
        assertFalse(bucketModel.contains("\"cover\""));
    }

    @Test
    void translationsCoverItemsAndFluidTypesInBothLanguages() throws IOException {
        String english = readResource("assets/create_nuclear_industry/lang/en_us.json");
        String chinese = readResource("assets/create_nuclear_industry/lang/zh_cn.json");

        for (String id : new String[]{
                P1ContentIds.FRESH_FUEL_ASSEMBLY_ID,
                P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID,
                P1ContentIds.CONTROL_ROD_ID,
                P1ContentIds.STEEL_PLATE_ID
        }) {
            assertTranslation(english, "item", id);
            assertTranslation(chinese, "item", id);
        }
        for (String id : new String[]{P1ContentIds.COMPOUND_COOLANT_ID, P1ContentIds.HOT_COMPOUND_COOLANT_ID}) {
            assertTranslation(english, "fluid_type", id);
            assertTranslation(chinese, "fluid_type", id);
        }
        assertTranslation(english, "item", P1ContentIds.COMPOUND_COOLANT_BUCKET_ID);
        assertTranslation(chinese, "item", P1ContentIds.COMPOUND_COOLANT_BUCKET_ID);
    }

    @Test
    void formalItemsAreExposedByTheMainCreativeTab() throws IOException {
        String creativeTab = readSource("com/iksxh/create_nuclear_industry/content/ModCreativeTabs.java");

        assertTrue(creativeTab.contains("ModItems.FRESH_FUEL_ASSEMBLY.get()"));
        assertTrue(creativeTab.contains("ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()"));
        assertTrue(creativeTab.contains("ModItems.CONTROL_ROD.get()"));
        assertTrue(creativeTab.contains("ModItems.STEEL_PLATE.get()"));
        assertTrue(creativeTab.contains("ModItems.COMPOUND_COOLANT_BUCKET.get()"));
        assertFalse(creativeTab.contains("ENGINEER_GOGGLES"));
        assertFalse(creativeTab.contains("HOT_COMPOUND_COOLANT_BUCKET"));
        assertFalse(creativeTab.contains("P0Probe"));
    }

    private static String constantFor(String id) {
        return switch (id) {
            case P1ContentIds.FRESH_FUEL_ASSEMBLY_ID -> "FRESH_FUEL_ASSEMBLY_ID";
            case P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID -> "COOLED_SPENT_FUEL_ASSEMBLY_ID";
            case P1ContentIds.CONTROL_ROD_ID -> "CONTROL_ROD_ID";
            case P1ContentIds.STEEL_PLATE_ID -> "STEEL_PLATE_ID";
            default -> throw new IllegalArgumentException("unmapped item ID: " + id);
        };
    }

    private static void assertTranslation(String source, String type, String id) {
        assertTrue(source.contains("\"" + type + "." + CreateNuclearIndustry.MOD_ID + "." + id + "\""),
                "missing translation: " + type + "." + id);
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(JAVA_SOURCES.resolve(relativePath));
    }

    private static String readResource(String relativePath) throws IOException {
        return Files.readString(RESOURCES.resolve(relativePath));
    }
}
