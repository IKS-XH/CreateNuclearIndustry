package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.ModBlocks;
import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P1RegistrationContractTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");

    private static final Set<String> EXPECTED_LOCAL_IDS = Set.of(
            P1ContentIds.EXPERIMENTAL_REACTOR_ID,
            P1ContentIds.REACTOR_CASING_ID,
            P1ContentIds.REACTOR_WINDOW_ID,
            P1ContentIds.REACTOR_INSTRUMENT_PORT_ID,
            P1ContentIds.REACTOR_COLD_PORT_ID,
            P1ContentIds.REACTOR_HOT_PORT_ID,
            P1ContentIds.REACTOR_REFUELING_PORT_ID,
            P1ContentIds.REACTOR_FUEL_ROD_ID,
            P1ContentIds.CONTROL_ROD_DRIVE_ID,
            P1ContentIds.FRESH_FUEL_ASSEMBLY_ID,
            P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID,
            P1ContentIds.CONTROL_ROD_ID,
            P1ContentIds.STEEL_PLATE_ID,
            P1ContentIds.COMPOUND_COOLANT_ID,
            P1ContentIds.HOT_COMPOUND_COOLANT_ID
    );

    @Test
    void formalP1IdsMatchApprovedLocalAndExternalSet() {
        Set<String> localIds = P1ContentIds.FORMAL_IDS.stream()
                .filter(entry -> entry.owner() == P1ContentIds.Owner.MOD)
                .map(P1ContentIds.Entry::id)
                .collect(Collectors.toUnmodifiableSet());
        Set<String> externalIds = P1ContentIds.FORMAL_IDS.stream()
                .filter(entry -> entry.owner() != P1ContentIds.Owner.MOD)
                .map(P1ContentIds.Entry::id)
                .collect(Collectors.toUnmodifiableSet());

        assertEquals(EXPECTED_LOCAL_IDS, localIds);
        assertEquals(Set.of(P1ContentIds.ENGINEER_GOGGLES_ID), externalIds);
        assertEquals(P1ContentIds.FORMAL_IDS.size(), localIds.size() + externalIds.size());
    }

    @Test
    void engineerGogglesAreExternalAndExistingSampleIsNotRenamed() {
        assertEquals("create:goggles", P1ContentIds.ENGINEER_GOGGLES_ID);
        assertFalse(P1ContentIds.FORMAL_IDS.stream()
                .anyMatch(entry -> entry.id().equals("goggles")));
        assertEquals("experimental_reactor_casing", ModBlocks.EXPERIMENTAL_REACTOR_CASING_ID);
        assertEquals(ModBlocks.EXPERIMENTAL_REACTOR_CASING_ID,
                P1ContentIds.EXISTING_SAMPLE_REACTOR_CASING_ID);
    }

    @Test
    void prohibitedP1ContentIsAbsentAndOldSteelAliasIsNotAccepted() {
        assertTrue(P1ContentIds.FORMAL_IDS.stream()
                .noneMatch(entry -> P1ContentIds.isProhibited(entry.id())));
        assertTrue(P1ContentIds.isProhibited(P1ContentIds.REMOVED_ALLOY_STEEL_PLATE_ALIAS));
        assertFalse(P1ContentIds.FORMAL_IDS.stream()
                .anyMatch(entry -> entry.id().equals(P1ContentIds.REMOVED_ALLOY_STEEL_PLATE_ALIAS)));
    }

    @Test
    void formalLocalIdsHaveEnglishAndChineseTranslationKeys() throws IOException {
        String english = read("assets/create_nuclear_industry/lang/en_us.json");
        String chinese = read("assets/create_nuclear_industry/lang/zh_cn.json");

        for (P1ContentIds.Entry entry : P1ContentIds.FORMAL_IDS) {
            if (entry.owner() != P1ContentIds.Owner.MOD || entry.kind() == P1ContentIds.Kind.MULTIBLOCK) {
                continue;
            }
            String type = entry.kind() == P1ContentIds.Kind.BLOCK ? "block"
                    : entry.kind() == P1ContentIds.Kind.ITEM ? "item" : "fluid_type";
            String key = type + "." + CreateNuclearIndustry.MOD_ID + "." + entry.id();
            assertTrue(english.contains("\"" + key + "\""), "missing English key: " + key);
            assertTrue(chinese.contains("\"" + key + "\""), "missing Chinese key: " + key);
        }
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(RESOURCES.resolve(relativePath));
    }
}
