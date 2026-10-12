package com.iksxh.create_nuclear_industry.industrial;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 校验仅五种可制造内容，原料数量和朝向资源；不把资源合同当作客户端视觉验收。 */
class IndustrialDataContractTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    @Test void fiveShapedRecipesHaveFrozenCountsAndCosts() throws Exception {
        List<String> names = List.of("reinforced_fluid_pipe", "ceramic_fluid_pipe", "industrial_fluid_pump", "reinforced_fluid_valve", "ceramic_fluid_valve");
        List<List<String>> patterns = List.of(List.of("RPR", "BBB", " P "), List.of("TTT", "TCT", "TTT"),
                List.of("PPP", "FMF", "RSP"), List.of(" S ", "PMP", " T "), List.of(" C ", "TM ", " C "));
        for (int i = 0; i < names.size(); i++) {
            var json = JsonParser.parseString(Files.readString(RESOURCES.resolve("data/create_nuclear_industry/recipe/" + names.get(i) + ".json"))).getAsJsonObject();
            assertEquals("minecraft:crafting_shaped", json.get("type").getAsString());
            assertEquals(patterns.get(i), json.getAsJsonArray("pattern").asList().stream().map(e -> e.getAsString()).toList());
            assertEquals(i < 2 ? 8 : 1, json.getAsJsonObject("result").get("count").getAsInt());
            assertTrue(Files.exists(RESOURCES.resolve("data/create_nuclear_industry/loot_table/blocks/" + names.get(i) + ".json")));
            assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/models/item/" + names.get(i) + ".json")));
        }
        var pump = JsonParser.parseString(Files.readString(RESOURCES.resolve("data/create_nuclear_industry/recipe/industrial_fluid_pump.json"))).getAsJsonObject();
        assertEquals("create:mechanical_pump", pump.getAsJsonObject("key").getAsJsonObject("M").get("item").getAsString());
        assertEquals("create_nuclear_industry:reinforced_steel_plate", pump.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString());
    }
    @Test void pumpHasSixFacingsAndPipesHaveAllSixConnectionArms() throws Exception {
        var pump = JsonParser.parseString(Files.readString(RESOURCES.resolve("assets/create_nuclear_industry/blockstates/industrial_fluid_pump.json"))).getAsJsonObject();
        assertEquals(6, pump.getAsJsonObject("variants").size());
        for (String name : List.of("reinforced_fluid_pipe", "ceramic_fluid_pipe", "reinforced_fluid_valve", "ceramic_fluid_valve")) {
            var pipe = JsonParser.parseString(Files.readString(RESOURCES.resolve("assets/create_nuclear_industry/blockstates/" + name + ".json"))).getAsJsonObject();
            for (var part : pipe.getAsJsonArray("multipart")) {
                var condition = part.getAsJsonObject().getAsJsonObject("when");
                assertNotNull(condition);
                assertTrue(validCondition(condition));
                assertTrue(part.getAsJsonObject().getAsJsonObject("apply").get("model").getAsString().startsWith("create_nuclear_industry:"));
            }
        }
    }
    /** 递归核原生multipart逻辑，允许AND/OR但不允许不存在的状态属性。 */
    private static boolean validCondition(com.google.gson.JsonObject condition) {
        for (var entry : condition.entrySet()) {
            if (entry.getKey().equals("AND") || entry.getKey().equals("OR")) {
                if (!entry.getValue().isJsonArray() || entry.getValue().getAsJsonArray().isEmpty()) return false;
                for (var nested : entry.getValue().getAsJsonArray()) if (!nested.isJsonObject() || !validCondition(nested.getAsJsonObject())) return false;
            } else if (!List.of("axis", "down", "up", "north", "south", "west", "east").contains(entry.getKey())) return false;
        }
        return true;
    }
}
