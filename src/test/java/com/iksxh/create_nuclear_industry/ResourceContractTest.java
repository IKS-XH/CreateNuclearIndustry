package com.iksxh.create_nuclear_industry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceContractTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final String SAMPLE_ID = "create_nuclear_industry:experimental_reactor_casing";

    @Test
    void metadataDeclaresModAndRequiredCreateDependency() throws IOException {
        String metadata = read("META-INF/neoforge.mods.toml");

        assertTrue(metadata.contains("modId=\"create_nuclear_industry\""));
        assertTrue(metadata.contains("modId=\"create\""));
        assertTrue(metadata.contains("type=\"required\""));
        assertTrue(metadata.contains("versionRange=\"[6.0.10,6.1.0)\""));
    }

    @Test
    void buildAndMetadataUseCreateCompatibleNeoForgeVersion() throws IOException {
        String buildProperties = Files.readString(Path.of("gradle.properties"));
        String metadata = read("META-INF/neoforge.mods.toml");

        assertTrue(buildProperties.contains("neo_version=21.1.219"));
        assertTrue(metadata.contains("versionRange=\"[21.1.219,)\""));
    }

    @Test
    void translationsExposeTabAndSampleBlockInEnglishAndChinese() throws IOException {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            String translations = read("assets/create_nuclear_industry/lang/" + language + ".json");
            assertTrue(translations.contains("itemGroup.create_nuclear_industry.main"));
            assertTrue(translations.contains("block.create_nuclear_industry.experimental_reactor_casing"));
        }
    }

    @Test
    void sampleBlockHasCompleteModelChain() throws IOException {
        String blockState = read("assets/create_nuclear_industry/blockstates/experimental_reactor_casing.json");
        String itemModel = read("assets/create_nuclear_industry/models/item/experimental_reactor_casing.json");
        String blockModel = read("assets/create_nuclear_industry/models/block/experimental_reactor_casing.json");

        assertTrue(blockState.contains("create_nuclear_industry:block/experimental_reactor_casing"));
        assertTrue(itemModel.contains("create_nuclear_industry:block/experimental_reactor_casing"));
        assertTrue(blockModel.contains("minecraft:block/cube_bottom_top"));
    }

    @Test
    void sampleBlockDropsItselfAndRequiresAnIronPickaxe() throws IOException {
        String lootTable = read("data/create_nuclear_industry/loot_table/blocks/experimental_reactor_casing.json");
        String pickaxeTag = read("data/minecraft/tags/block/mineable/pickaxe.json");
        String ironToolTag = read("data/minecraft/tags/block/needs_iron_tool.json");

        assertTrue(lootTable.contains(SAMPLE_ID));
        assertTrue(pickaxeTag.contains(SAMPLE_ID));
        assertTrue(ironToolTag.contains(SAMPLE_ID));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(RESOURCES.resolve(relativePath));
    }
}
