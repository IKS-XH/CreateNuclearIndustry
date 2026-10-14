package com.iksxh.create_nuclear_industry.automation;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** 验证冻结制造成本与方块资源。 */
class ReactorAutomationDataTest {
    @Test void shapedRecipeHasFrozenPatternAndIngredients() throws Exception {
        var recipe = JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/create_nuclear_industry/recipe/crafting/reactor/reactor_automation_port.json"))).getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("[\"STS\",\"PIP\",\"SCS\"]", recipe.get("pattern").toString());
        var key = recipe.getAsJsonObject("key");
        assertEquals("c:plates/steel", key.getAsJsonObject("S").get("tag").getAsString());
        assertEquals("create:electron_tube", key.getAsJsonObject("T").get("item").getAsString());
        assertEquals("create:precision_mechanism", key.getAsJsonObject("P").get("item").getAsString());
        assertEquals("create_nuclear_industry:industrial_sensor", key.getAsJsonObject("I").get("item").getAsString());
        assertEquals("create_nuclear_industry:reactor_casing", key.getAsJsonObject("C").get("item").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
    }
}
