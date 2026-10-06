package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import com.iksxh.create_nuclear_industry.ponder.P1PonderPlugin;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 P1-PONDER-01 的直接入口集合、故事板身份、注册归属和客户端接线契约。 */
class P1Ponder01ContractTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

    /** 需求冻结的 11 个直接入口；使用字面量防止测试镜像生产列表。 */
    private static final List<String> EXPECTED_DIRECT_ENTRY_IDS = List.of(
            "reactor_casing",
            "reactor_window",
            "reactor_instrument_port",
            "reactor_cold_port",
            "reactor_hot_port",
            "reactor_refueling_port",
            "reactor_fuel_rod",
            "control_rod_drive",
            "fresh_fuel_assembly",
            "cooled_spent_fuel_assembly",
            "compound_coolant_bucket"
    );

    @Test
    void directEntriesAreTheExactUniqueFrozenSet() {
        assertEquals(EXPECTED_DIRECT_ENTRY_IDS, P1PonderPlugin.DIRECT_ENTRY_IDS);
        assertEquals(EXPECTED_DIRECT_ENTRY_IDS.size(),
                new HashSet<>(P1PonderPlugin.DIRECT_ENTRY_IDS).size(),
                "Ponder 直接入口不得重复");
    }

    @Test
    void everyDirectEntryBelongsToRegisteredP1Content() {
        Map<String, P1ContentIds.Entry> formalEntries = P1ContentIds.FORMAL_IDS.stream()
                .collect(Collectors.toUnmodifiableMap(P1ContentIds.Entry::id, Function.identity()));

        for (String id : P1PonderPlugin.DIRECT_ENTRY_IDS) {
            P1ContentIds.Entry entry = formalEntries.get(id);
            assertNotNull(entry, "直接入口必须属于正式 P1 注册内容: " + id);
            assertEquals(P1ContentIds.Owner.MOD, entry.owner(), "直接入口必须由本模组拥有: " + id);
            assertTrue(entry.kind() == P1ContentIds.Kind.BLOCK || entry.kind() == P1ContentIds.Kind.ITEM,
                    "直接入口必须是可触发的方块或物品: " + id);
        }
    }

    @Test
    void pluginRegistersFourNamespacedReactorStoryboardsWithIndependentTemplates() throws IOException {
        assertEquals(CreateNuclearIndustry.MOD_ID, new P1PonderPlugin().getModId());
        assertEquals(P1ContentIds.EXPERIMENTAL_REACTOR_ID, P1PonderPlugin.REACTOR_SCENE_ID);
        assertEquals(List.of(
                "experimental_reactor",
                "experimental_reactor_rods",
                "experimental_reactor_operation",
                "experimental_reactor_refueling"), P1PonderPlugin.REACTOR_SCENE_IDS);
        for (String id : P1PonderPlugin.REACTOR_SCENE_IDS) {
            assertEquals("create_nuclear_industry:" + id,
                    ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, id).toString());
            assertTrue(Files.exists(RESOURCES.resolve(
                            "assets/create_nuclear_industry/ponder/" + id + ".nbt")),
                    "每条反应堆故事线都必须有独立模板: " + id);
        }

        String pluginSource = Files.readString(MAIN_SOURCES.resolve(
                "com/iksxh/create_nuclear_industry/ponder/P1PonderPlugin.java"));
        assertEquals(4, occurrences(pluginSource, "reactorEntries.addStoryBoard("));
        assertTrue(pluginSource.contains("P1PonderScenes::experimentalReactorBasics"));
        assertTrue(pluginSource.contains("P1PonderScenes::experimentalReactorRods"));
        assertTrue(pluginSource.contains("P1PonderScenes::experimentalReactorOperation"));
        assertTrue(pluginSource.contains("P1PonderScenes::experimentalReactorRefueling"));

        String sceneSource = Files.readString(MAIN_SOURCES.resolve(
                "com/iksxh/create_nuclear_industry/ponder/P1PonderScenes.java"));
        // 保留四条故事线的方法完整性覆盖；字幕数量和文字不构成入口契约。
        for (String method : List.of("experimentalReactorBasics", "experimentalReactorRods",
                "experimentalReactorOperation", "experimentalReactorRefueling")) {
            assertTrue(storyBody(sceneSource, method).contains("scene.markAsFinished()"),
                    "故事线必须正常收尾: " + method);
        }
    }

    @Test
    void indirectAndExplicitlyExcludedIdsNeverBecomeDirectEntries() {
        Set<String> directEntries = Set.copyOf(P1PonderPlugin.DIRECT_ENTRY_IDS);
        Set<String> indirectEntries = Set.of(
                P1ContentIds.CONTROL_ROD_ID,
                P1ContentIds.STEEL_PLATE_ID,
                P1ContentIds.COMPOUND_COOLANT_ID,
                P1ContentIds.HOT_COMPOUND_COOLANT_ID,
                P1ContentIds.ENGINEER_GOGGLES_ID
        );
        Set<String> excludedEntries = Set.of(
                P1ContentIds.EXISTING_SAMPLE_REACTOR_CASING_ID,
                "main_coolant_pump",
                "pressure_pipe_tier_1",
                "pressure_valve_tier_1",
                "reactor_control_port",
                "high_pressure_boiler",
                "supercritical_steam_turbine",
                "dosimeter",
                "shielded_hot_cell",
                "coolant_purifier",
                "contaminated_compound_coolant",
                "reactor_interlock",
                "scram_interlock",
                "fire",
                "explosion",
                "pollution"
        );

        assertTrue(indirectEntries.stream().noneMatch(directEntries::contains));
        assertTrue(excludedEntries.stream().noneMatch(directEntries::contains));
        assertFalse(directEntries.contains(P1ContentIds.REMOVED_ALLOY_STEEL_PLATE_ALIAS));
    }

    @Test
    void directEntriesHaveTheExistingPonderTriggerResources() throws IOException {
        Path assets = RESOURCES.resolve("assets/create_nuclear_industry");
        for (String id : P1PonderPlugin.DIRECT_ENTRY_IDS) {
            P1ContentIds.Entry entry = P1ContentIds.FORMAL_IDS.stream()
                    .filter(candidate -> candidate.id().equals(id))
                    .findFirst()
                    .orElseThrow();
            if (entry.kind() == P1ContentIds.Kind.BLOCK) {
                assertTrue(Files.exists(assets.resolve("blockstates/" + id + ".json")),
                        "缺少直接入口方块状态资源: " + id);
                assertTrue(Files.exists(assets.resolve("models/item/" + id + ".json")),
                        "缺少直接入口方块物品模型: " + id);
            } else {
                assertTrue(Files.exists(assets.resolve("models/item/" + id + ".json")),
                        "缺少直接入口物品模型: " + id);
            }
        }
    }

    @Test
    void ponderPluginIsRegisteredExactlyOnceFromClientSetup() throws Exception {
        Method registerPonder = CreateNuclearIndustry.class
                .getDeclaredMethod("registerPonder", FMLClientSetupEvent.class);
        assertTrue(Modifier.isPrivate(registerPonder.getModifiers()));
        assertTrue(Modifier.isStatic(registerPonder.getModifiers()));

        String mainSource = Files.readString(MAIN_SOURCES.resolve(
                "com/iksxh/create_nuclear_industry/CreateNuclearIndustry.java"));
        assertTrue(mainSource.contains("modEventBus.addListener(CreateNuclearIndustry::registerPonder)"));
        assertEquals(1, occurrences(mainSource, "PonderIndex.addPlugin(new P1PonderPlugin())"));
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    private static String storyBody(String source, String methodName) {
        int start = source.indexOf("public static void " + methodName);
        assertTrue(start >= 0, "缺少 Ponder 故事线方法: " + methodName);
        int openingBrace = source.indexOf('{', start);
        int depth = 0;
        for (int index = openingBrace; index < source.length(); index++) {
            if (source.charAt(index) == '{') {
                depth++;
            } else if (source.charAt(index) == '}' && --depth == 0) {
                return source.substring(start, index + 1);
            }
        }
        throw new AssertionError("Ponder 故事线方法的大括号不完整: " + methodName);
    }
}
