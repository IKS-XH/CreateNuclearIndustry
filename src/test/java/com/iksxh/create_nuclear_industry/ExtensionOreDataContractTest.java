package com.iksxh.create_nuclear_industry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** 对实际打包资源验证矿物入口，拦截漏模型、错误分布、错误标签和压缩增殖。 */
class ExtensionOreDataContractTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final String NS = "create_nuclear_industry";
    private static final List<String> METALS = List.of("lead", "tin", "uranium");

    @Test
    void oreAndRawResourceReferencesResolve() throws Exception {
        for (String metal : METALS) {
            for (String id : List.of(metal + "_ore", "deepslate_" + metal + "_ore", "raw_" + metal + "_block")) {
                JsonObject state = json("assets/" + NS + "/blockstates/" + id + ".json");
                String model = state.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString();
                assertTrue(Files.exists(resource(model, "models", ".json")), model);
                JsonObject blockModel = json("assets/" + NS + "/models/block/" + id + ".json");
                assertTrue(Files.exists(resource(blockModel.getAsJsonObject("textures").get("all").getAsString(), "textures", ".png")));
                assertEquals(NS + ":block/" + id, json("assets/" + NS + "/models/item/" + id + ".json").get("parent").getAsString());
                json("data/" + NS + "/loot_table/blocks/" + id + ".json");
            }
            JsonObject raw = json("assets/" + NS + "/models/item/raw_" + metal + ".json");
            assertTrue(Files.exists(resource(raw.getAsJsonObject("textures").get("layer0").getAsString(), "textures", ".png")));
        }
    }

    @Test
    void generationResourcesKeepSingleVeinAndApprovedDistribution() throws Exception {
        int[][] limits = {{-48, 32, 6, 7}, {-16, 112, 12, 8}, {-64, -16, 2, 4}};
        for (int i = 0; i < METALS.size(); i++) {
            String metal = METALS.get(i);
            JsonObject configured = json("data/" + NS + "/worldgen/configured_feature/ore_" + metal + ".json").getAsJsonObject("config");
            assertEquals(2, configured.getAsJsonArray("targets").size());
            for (int form = 0; form < 2; form++) {
                var target = configured.getAsJsonArray("targets").get(form).getAsJsonObject();
                assertEquals("minecraft:" + (form == 0 ? "stone" : "deepslate") + "_ore_replaceables",
                        target.getAsJsonObject("target").get("tag").getAsString());
                assertEquals(NS + ":" + (form == 0 ? "" : "deepslate_") + metal + "_ore",
                        target.getAsJsonObject("state").get("Name").getAsString());
            }
            assertEquals(limits[i][3], configured.get("size").getAsInt());
            assertEquals(i == 2 ? .25 : 0, configured.get("discard_chance_on_air_exposure").getAsDouble());
            var placed = json("data/" + NS + "/worldgen/placed_feature/ore_" + metal + ".json").getAsJsonArray("placement");
            assertEquals(NS + ":ore_generation", placed.get(0).getAsJsonObject().get("type").getAsString());
            assertEquals("auto", placed.get(0).getAsJsonObject().get("mode").getAsString());
            assertEquals("c:ores/" + metal, placed.get(0).getAsJsonObject().get("ore_tag").getAsString());
            assertEquals(limits[i][2], placed.get(1).getAsJsonObject().get("count").getAsInt());
            JsonObject height = placed.get(3).getAsJsonObject().getAsJsonObject("height");
            assertEquals("minecraft:trapezoid", height.get("type").getAsString());
            assertEquals(0, height.get("plateau").getAsInt());
            assertEquals(limits[i][0], height.getAsJsonObject("min_inclusive").get("absolute").getAsInt());
            assertEquals(limits[i][1], height.getAsJsonObject("max_inclusive").get("absolute").getAsInt());
        }
    }

    @Test
    void compressionAndTagGraphCannotCreateMaterial() throws Exception {
        for (String metal : METALS) {
            JsonObject pack = json("data/" + NS + "/recipe/raw_" + metal + "_block.json");
            assertEquals(3, pack.getAsJsonArray("pattern").size());
            pack.getAsJsonArray("pattern").forEach(row -> assertEquals("###", row.getAsString()));
            assertEquals("c:raw_materials/" + metal, pack.getAsJsonObject("key").getAsJsonObject("#").get("tag").getAsString());
            assertEquals(1, pack.getAsJsonObject("result").get("count").getAsInt());
            JsonObject unpack = json("data/" + NS + "/recipe/raw_" + metal + "_from_block.json");
            assertEquals(1, unpack.getAsJsonArray("ingredients").size());
            assertEquals(9, unpack.getAsJsonObject("result").get("count").getAsInt());
            for (String shape : List.of("ores", "raw_materials", "crushed_raw_materials")) {
                JsonObject child = json("data/c/tags/item/" + shape + "/" + metal + ".json");
                assertFalse(child.get("replace").getAsBoolean());
                assertTrue(json("data/c/tags/item/" + shape + ".json").getAsJsonArray("values").asList().stream()
                        .anyMatch(value -> value.getAsString().equals("#c:" + shape + "/" + metal)));
            }
        }
    }

    @Test
    void toolsTranslationsAndVanillaLootSemanticsArePresent() throws Exception {
        for (String metal : METALS) {
            String tier = metal.equals("tin") ? "stone" : "iron";
            for (String id : List.of(metal + "_ore", "deepslate_" + metal + "_ore", "raw_" + metal + "_block")) {
                for (String tag : List.of("mineable/pickaxe", "needs_" + tier + "_tool")) {
                    assertTrue(json("data/minecraft/tags/block/" + tag + ".json").getAsJsonArray("values").asList().stream()
                            .anyMatch(value -> value.getAsString().equals(NS + ":" + id)), id + " " + tag);
                }
                for (String language : List.of("zh_cn", "en_us")) {
                    assertTrue(json("assets/" + NS + "/lang/" + language + ".json").has("block." + NS + "." + id));
                    assertTrue(json("assets/" + NS + "/lang/" + language + ".json").has("item." + NS + ".raw_" + metal));
                }
                if (!id.endsWith("_block")) {
                    var entry = json("data/" + NS + "/loot_table/blocks/" + id + ".json").getAsJsonArray("pools")
                            .get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
                    assertEquals("minecraft:alternatives", entry.get("type").getAsString());
                    var functions = entry.getAsJsonArray("children").get(1).getAsJsonObject().getAsJsonArray("functions");
                    assertEquals("minecraft:ore_drops", functions.get(0).getAsJsonObject().get("formula").getAsString());
                    assertEquals("minecraft:fortune", functions.get(0).getAsJsonObject().get("enchantment").getAsString());
                    assertEquals("minecraft:explosion_decay", functions.get(1).getAsJsonObject().get("function").getAsString());
                }
            }
        }
    }

    private static JsonObject json(String path) throws Exception {
        Path file = ROOT.resolve(path);
        assertTrue(Files.exists(file), "缺失本批资源: " + file);
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static Path resource(String id, String directory, String extension) {
        String[] parts = id.split(":", 2);
        return ROOT.resolve("assets/" + parts[0] + "/" + directory + "/" + parts[1] + extension);
    }
}
