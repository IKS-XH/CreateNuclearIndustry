package com.iksxh.create_nuclear_industry.equipment;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 验证材料守恒、原生升级输入与原版占位资源；实际装备行为由隔离GameTest验证。 */
class DepletedUraniumDataContractTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final String NS = "create_nuclear_industry";
    private static final List<String> EQUIPMENT = List.of("helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "axe", "shovel", "hoe");

    @Test
    void dustSmeltsOneToOneAtExistingMetalTimings() throws Exception {
        for (String kind : List.of("smelting", "blasting")) {
            var recipe = recipe(kind + "/depleted_uranium_ingot_from_dust");
            assertEquals("minecraft:" + kind, recipe.get("type").getAsString());
            assertEquals(NS + ":depleted_uranium_dust", recipe.getAsJsonObject("ingredient").get("item").getAsString());
            assertResult(recipe, "depleted_uranium_ingot", 1);
            assertEquals(kind.equals("smelting") ? 200 : 100, recipe.get("cookingtime").getAsInt());
            assertEquals(0.1, recipe.get("experience").getAsDouble());
        }
    }

    @Test
    void storageRoundTripConservesNineIngots() throws Exception {
        var pack = recipe("crafting/depleted_uranium_block");
        assertEquals("minecraft:crafting_shaped", pack.get("type").getAsString());
        assertEquals(List.of("III", "III", "III"), java.util.stream.StreamSupport.stream(pack.getAsJsonArray("pattern").spliterator(), false).map(e -> e.getAsString()).toList());
        assertEquals(NS + ":depleted_uranium_ingot", pack.getAsJsonObject("key").getAsJsonObject("I").get("item").getAsString());
        assertResult(pack, "depleted_uranium_block", 1);
        var unpack = recipe("crafting/depleted_uranium_ingots_from_block");
        assertEquals("minecraft:crafting_shapeless", unpack.get("type").getAsString());
        assertEquals(1, unpack.getAsJsonArray("ingredients").size());
        assertEquals(NS + ":depleted_uranium_block", unpack.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString());
        assertResult(unpack, "depleted_uranium_ingot", 9);
    }

    @Test
    void nineNativeUpgradesRequireTemplateDiamondAndOneIngot() throws Exception {
        for (String part : EQUIPMENT) {
            var recipe = recipe("smithing/depleted_uranium_" + part);
            assertEquals("minecraft:smithing_transform", recipe.get("type").getAsString());
            assertEquals("minecraft:netherite_upgrade_smithing_template", recipe.getAsJsonObject("template").get("item").getAsString());
            assertEquals("minecraft:diamond_" + part, recipe.getAsJsonObject("base").get("item").getAsString());
            assertEquals(NS + ":depleted_uranium_ingot", recipe.getAsJsonObject("addition").get("item").getAsString());
            assertResult(recipe, "depleted_uranium_" + part, 1);
        }
        try (var files = Files.walk(ROOT.resolve("data/" + NS + "/recipe"))) {
            for (Path path : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                var json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if (json.get("type").getAsString().startsWith("minecraft:crafting")) {
                    var result = json.get("result");
                    if (result != null && result.isJsonObject() && result.getAsJsonObject().has("id")) {
                        String id = result.getAsJsonObject().get("id").getAsString();
                        assertFalse(EQUIPMENT.stream().anyMatch(part -> id.equals(NS + ":depleted_uranium_" + part)), path.toString());
                    }
                }
            }
        }
    }

    @Test
    void vanillaPlaceholdersAndBothLanguagesCoverEveryNewIdentity() throws Exception {
        var zh = json("assets/" + NS + "/lang/zh_cn.json");
        var en = json("assets/" + NS + "/lang/en_us.json");
        for (String part : EQUIPMENT) {
            String id = "depleted_uranium_" + part;
            assertEquals("minecraft:item/netherite_" + part, json("assets/" + NS + "/models/item/" + id + ".json").get("parent").getAsString());
            assertTrue(zh.has("item." + NS + "." + id));
            assertTrue(en.has("item." + NS + "." + id));
        }
        assertEquals("minecraft:item/iron_ingot", json("assets/" + NS + "/models/item/depleted_uranium_ingot.json").get("parent").getAsString());
        assertEquals("minecraft:block/iron_block", json("assets/" + NS + "/models/block/depleted_uranium_block.json").get("parent").getAsString());
        assertEquals(NS + ":block/depleted_uranium_block", json("assets/" + NS + "/blockstates/depleted_uranium_block.json").getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
        assertEquals(NS + ":block/depleted_uranium_block", json("assets/" + NS + "/models/item/depleted_uranium_block.json").get("parent").getAsString());
        for (var lang : List.of(zh, en)) {
            assertTrue(lang.has("item." + NS + ".depleted_uranium_ingot"));
            assertTrue(lang.has("block." + NS + ".depleted_uranium_block"));
        }
        var loot = json("data/" + NS + "/loot_table/blocks/depleted_uranium_block.json");
        assertEquals(NS + ":depleted_uranium_block", loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
    }

    private static void assertResult(JsonObject recipe, String id, int count) {
        var result = recipe.getAsJsonObject("result");
        assertEquals(NS + ":" + id, result.get("id").getAsString());
        assertEquals(count, result.has("count") ? result.get("count").getAsInt() : 1);
        assertFalse(result.has("components"), "升级结果不得覆盖底材组件");
    }
    private static JsonObject recipe(String id) throws Exception { return json("data/" + NS + "/recipe/" + id + ".json"); }
    private static JsonObject json(String path) throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
}
