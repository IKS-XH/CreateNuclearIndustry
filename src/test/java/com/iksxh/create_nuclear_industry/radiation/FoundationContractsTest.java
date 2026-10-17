package com.iksxh.create_nuclear_industry.radiation;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 各任务实施前的契约存在性门；算法和真实世界断言在各专项测试中扩展。 */
class FoundationContractsTest {
    @Test void sourceAndDoseInterfacesExist() {
        for (String name : new String[]{"RadiationParameters", "RadiationItemSources", "RadiationSourceIndex",
                "RadiationContainment", "RadiationDoseState", "RadiationDoseCalculator", "RadiationExposurePayload"})
            assertDoesNotThrow(() -> Class.forName("com.iksxh.create_nuclear_industry.radiation." + name));
    }
    @Test void equipmentHasExactlyFiveShapedRecipes() throws Exception {
        Path root = Path.of("src/main/resources/data/create_nuclear_industry/recipe/crafting");
        try (var files = Files.list(root)) {
            var recipes = files.filter(p -> p.getFileName().toString().startsWith("radiation_")).toList();
            assertEquals(5, recipes.size());
            for (Path p : recipes) assertTrue(Files.readString(p).contains("minecraft:crafting_shaped"));
        }
    }
}
