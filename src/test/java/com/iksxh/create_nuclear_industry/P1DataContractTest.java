package com.iksxh.create_nuclear_industry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证正式方块、物品、流体的资源链以及明确禁止的生存内容。 */
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
            P1ContentIds.STEEL_PLATE_ID,
            P1ContentIds.COMPOUND_COOLANT_BUCKET_ID
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
            Path model = ASSETS.resolve("models/item/" + id + ".json");
            assertTrue(Files.exists(model),
                    "missing item model: " + id);
            if (P1ContentIds.COMPOUND_COOLANT_BUCKET_ID.equals(id)) {
                String bucketModel = read("assets/create_nuclear_industry/models/item/" + id + ".json");
                assertTrue(bucketModel.contains("\"loader\": \"neoforge:fluid_container\""),
                        "coolant bucket must use NeoForge's dynamic fluid container loader: " + id);
                assertTrue(bucketModel.contains("\"parent\": \"neoforge:item/bucket\""),
                        "coolant bucket must use NeoForge's built-in bucket parent: " + id);
                assertTrue(bucketModel.contains("\"fluid\": \"create_nuclear_industry:compound_coolant\""),
                        "coolant bucket must point at the formal cold fluid: " + id);
                assertFalse(bucketModel.contains("\"parent\": \"minecraft:item/generated\""),
                        "coolant bucket must not use the generated item parent: " + id);
                assertFalse(bucketModel.contains("\"base\""),
                        "coolant bucket must not override the built-in bucket base texture: " + id);
                assertFalse(bucketModel.contains("\"cover\""),
                        "coolant bucket must not override the built-in bucket cover texture: " + id);
            } else {
                assertTrue(Files.exists(ASSETS.resolve("textures/item/" + id + ".png")),
                        "missing item texture: " + id);
            }
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
            assertTrue(Files.exists(ASSETS.resolve("textures/block/" + id + "_still.png")),
                    "missing block-atlas still fluid texture: " + id);
            assertTrue(Files.exists(ASSETS.resolve("textures/block/" + id + "_flow.png")),
                    "missing block-atlas flowing fluid texture: " + id);
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
                    JsonElement recipe = JsonParser.parseString(Files.readString(path));
                    Path relativePath = recipeDirectory.relativize(path);
                    Map<Path, Set<String>> approvedProductionReferences = Map.of(
                            Path.of("pressing", "steel_plate.json"),
                            Set.of(registryId(P1ContentIds.STEEL_PLATE_ID)),
                            Path.of("shielded_assembly", "fresh_fuel_assembly.json"),
                            Set.of(registryId(P1ContentIds.FRESH_FUEL_ASSEMBLY_ID)),
                            Path.of("mixing", "compound_coolant.json"),
                            Set.of(registryId(P1ContentIds.COMPOUND_COOLANT_ID)));
                    Set<String> protectedIds = Set.of(
                            registryId(P1ContentIds.FRESH_FUEL_ASSEMBLY_ID),
                            registryId(P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID),
                            registryId(P1ContentIds.STEEL_PLATE_ID),
                            registryId(P1ContentIds.COMPOUND_COOLANT_ID),
                            registryId(P1ContentIds.HOT_COMPOUND_COOLANT_ID),
                            registryId("contaminated_compound_coolant"),
                            registryId("coolant_purifier"),
                            registryId(P1ContentIds.REMOVED_ALLOY_STEEL_PLATE_ALIAS)
                    );
                    Set<String> approvedHere = approvedProductionReferences.getOrDefault(relativePath, Set.of());
                    boolean approvedCaskConsumption = relativePath.equals(
                            Path.of("shielded_assembly", "sealed_spent_fuel_cask.json"));
                    for (String id : protectedIds) {
                        boolean expected = approvedHere.contains(id)
                                || (approvedCaskConsumption
                                && id.equals(registryId(P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID)));
                        assertEquals(expected, containsExactString(recipe, id),
                                "P1 recipe reference must match the approved route exactly: " + id + " in " + path);
                    }
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }

        String steelPlateId = registryId(P1ContentIds.STEEL_PLATE_ID);
        JsonObject steelPress = JsonParser.parseString(Files.readString(
                recipeDirectory.resolve("pressing/steel_plate.json"))).getAsJsonObject();
        assertTrue(steelPress.getAsJsonArray("results").asList().stream().anyMatch(result ->
                        result.isJsonObject() && result.getAsJsonObject().has("id")
                                && steelPlateId.equals(result.getAsJsonObject().get("id").getAsString())),
                "钢板压片路线必须在results[].id产出钢板");

        String freshAssemblyId = registryId(P1ContentIds.FRESH_FUEL_ASSEMBLY_ID);
        JsonObject freshAssembly = JsonParser.parseString(Files.readString(
                recipeDirectory.resolve("shielded_assembly/fresh_fuel_assembly.json"))).getAsJsonObject();
        assertTrue(freshAssembly.has("result") && freshAssembly.getAsJsonObject("result").has("id")
                        && freshAssemblyId.equals(freshAssembly.getAsJsonObject("result").get("id").getAsString()),
                "新燃料组件装配路线必须在result.id产出批准组件");

        String coldCoolantId = registryId(P1ContentIds.COMPOUND_COOLANT_ID);
        JsonObject coldCoolant = JsonParser.parseString(Files.readString(
                recipeDirectory.resolve("mixing/compound_coolant.json"))).getAsJsonObject();
        assertTrue(coldCoolant.getAsJsonArray("results").asList().stream().anyMatch(result ->
                        result.isJsonObject() && result.getAsJsonObject().has("id")
                                && coldCoolantId.equals(result.getAsJsonObject().get("id").getAsString())),
                "冷态冷却剂配方必须在results[].id产出批准流体");

        Path sealedCaskPath = recipeDirectory.resolve("shielded_assembly/sealed_spent_fuel_cask.json");
        JsonObject sealedCask = JsonParser.parseString(Files.readString(sealedCaskPath)).getAsJsonObject();
        String cooledAssemblyId = registryId(P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID);
        JsonArray inputs = sealedCask.getAsJsonArray("inputs");
        boolean cooledAssemblyIsInput = false;
        for (JsonElement input : inputs) {
            if (input.isJsonObject() && input.getAsJsonObject().has("ingredient")) {
                JsonObject ingredient = input.getAsJsonObject().getAsJsonObject("ingredient");
                if (ingredient.has("item") && cooledAssemblyId.equals(ingredient.get("item").getAsString())) {
                    cooledAssemblyIsInput = true;
                    break;
                }
            }
        }
        assertTrue(cooledAssemblyIsInput,
                "冷却后乏燃料组件只允许作为已批准灌封配方的输入");
        assertFalse(containsExactString(sealedCask.getAsJsonObject("result"), cooledAssemblyId),
                "灌封配方不得把冷却后乏燃料组件作为产物");
        for (String similarSteelPlateId : List.of(
                "create_nuclear_industry:reinforced_steel_plate",
                "create_nuclear_industry:incomplete_reinforced_steel_plate")) {
            assertFalse(containsExactString(JsonParser.parseString("\"" + similarSteelPlateId + "\""),
                            steelPlateId),
                    "强化钢板的完整注册 ID 不得被识别成钢板: " + similarSteelPlateId);
        }
        assertTrue(containsExactString(JsonParser.parseString(
                        "{\"item\":\"create_nuclear_industry:hot_compound_coolant\"}"),
                        registryId(P1ContentIds.HOT_COMPOUND_COOLANT_ID)),
                "禁止路线扫描必须识别item字段中的精确注册 ID");
        assertTrue(containsExactString(JsonParser.parseString(
                        "{\"tag\":\"create_nuclear_industry:hot_compound_coolant\"}"),
                        registryId(P1ContentIds.HOT_COMPOUND_COOLANT_ID)),
                "禁止路线扫描必须识别tag字段中的精确注册 ID");
    }

    private static String registryId(String path) {
        return "create_nuclear_industry:" + path;
    }

    /** 递归检查 JSON 字符串值是否与完整注册 ID 精确相等，避免相似名称误命中。 */
    private static boolean containsExactString(JsonElement element, String expected) {
        if (element.isJsonPrimitive()) {
            return element.getAsJsonPrimitive().isString()
                    && expected.equals(element.getAsString());
        }
        if (element.isJsonArray()) {
            return element.getAsJsonArray().asList().stream()
                    .anyMatch(value -> containsExactString(value, expected));
        }
        if (element.isJsonObject()) {
            return element.getAsJsonObject().entrySet().stream()
                    .anyMatch(entry -> containsExactString(entry.getValue(), expected));
        }
        return false;
    }

    @Test
    void coldCoolantHasTheOnlyBucketAndLiquidBlockUsesTheFluidId() throws IOException {
        String fluids = readSource("com/iksxh/create_nuclear_industry/content/ModFluids.java");
        String items = readSource("com/iksxh/create_nuclear_industry/content/ModItems.java");
        String creativeTab = readSource("com/iksxh/create_nuclear_industry/content/ModCreativeTabs.java");

        assertTrue(items.contains("P1ContentIds.COMPOUND_COOLANT_BUCKET_ID"));
        assertTrue(items.contains("new BucketItem(ModFluids.COMPOUND_COOLANT_SOURCE.get()"));
        assertTrue(fluids.contains("LIQUID_BLOCKS.register("));
        assertTrue(fluids.contains("P1ContentIds.COMPOUND_COOLANT_ID"));
        assertTrue(fluids.contains(".bucket(() -> ModItems.COMPOUND_COOLANT_BUCKET.get())"));
        assertTrue(fluids.contains(".block(COMPOUND_COOLANT_BLOCK)"));
        assertTrue(fluids.contains("initializeClient(Consumer<IClientFluidTypeExtensions> consumer)"));
        assertTrue(fluids.contains("getStillTexture()"));
        assertTrue(fluids.contains("getFlowingTexture()"));
        assertTrue(fluids.contains("\"block/\" + id + \"_still\""));
        assertTrue(fluids.contains("\"block/\" + id + \"_flow\""));
        assertTrue(creativeTab.contains("ModItems.COMPOUND_COOLANT_BUCKET.get()"));
        assertFalse(items.contains("HOT_COMPOUND_COOLANT_BUCKET"));
        assertFalse(creativeTab.contains("HOT_COMPOUND_COOLANT_BUCKET"));
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(Path.of("src", "main", "java").resolve(relativePath));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(RESOURCES.resolve(relativePath));
    }
}
