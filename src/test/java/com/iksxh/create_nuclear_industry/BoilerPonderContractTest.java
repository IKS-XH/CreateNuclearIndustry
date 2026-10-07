package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.ponder.P1PonderPlugin;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证锅炉Ponder入口、双语资源、完整模板索引及合法端口布局。 */
class BoilerPonderContractTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Set<String> BOILER_PARTS = Set.of(
            "create_nuclear_industry:high_pressure_boiler_casing",
            "create_nuclear_industry:high_pressure_boiler_window",
            "create_nuclear_industry:high_pressure_boiler_water_port",
            "create_nuclear_industry:high_pressure_boiler_steam_port",
            "create_nuclear_industry:high_pressure_boiler_hot_coolant_port",
            "create_nuclear_industry:high_pressure_boiler_cold_coolant_port",
            "create_nuclear_industry:boiler_safety_valve",
            "create_nuclear_industry:boiler_heat_exchange_section",
            "create_nuclear_industry:high_pressure_boiler_controller",
            "create_nuclear_industry:nuclear_heat_exchanger"
    );
    private static final List<String> SCENES = List.of("high_pressure_boiler_build",
            "high_pressure_boiler_operation", "high_pressure_boiler_steam", "high_pressure_boiler_shutdown");

    @Test
    void nineBoilerPartsShareTheFourOrderedStoryboards() throws IOException {
        assertEquals(List.of("high_pressure_boiler_casing", "high_pressure_boiler_window",
                "high_pressure_boiler_water_port", "high_pressure_boiler_steam_port",
                "high_pressure_boiler_hot_coolant_port", "high_pressure_boiler_cold_coolant_port",
                "boiler_safety_valve", "boiler_heat_exchange_section", "high_pressure_boiler_controller"),
                P1PonderPlugin.BOILER_ENTRY_IDS);
        assertEquals(SCENES, P1PonderPlugin.BOILER_SCENE_IDS);
        assertEquals(P1PonderPlugin.BOILER_ENTRY_IDS.size(), Set.copyOf(P1PonderPlugin.BOILER_ENTRY_IDS).size());

        for (String id : SCENES) {
            assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/ponder/" + id + ".nbt")),
                    "缺少独立锅炉故事线模板: " + id);
        }
    }

    @Test
    void allTemplatesHaveValidNbtIndicesAndLegalBoilerPorts() throws IOException {
        for (String scene : SCENES) {
            Path path = RESOURCES.resolve("assets/create_nuclear_industry/ponder/" + scene + ".nbt");
            CompoundTag root;
            try (var input = Files.newInputStream(path)) {
                root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            }
            ListTag dimensions = root.getList("size", Tag.TAG_INT);
            assertEquals(List.of(13, 9, 13), List.of(dimensions.getInt(0), dimensions.getInt(1), dimensions.getInt(2)));
            ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
            ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
            Set<String> names = new HashSet<>();
            Set<List<Integer>> pumpPositions = new HashSet<>();
            Set<String> referencedIds = new HashSet<>();
            Map<List<Integer>, String> actualBlocks = new HashMap<>();
            int drivenMotors = 0;
            for (int i = 0; i < palette.size(); i++) names.add(palette.getCompound(i).getString("Name"));
            assertTrue(names.containsAll(BOILER_PARTS), "锅炉部件 palette 不完整: " + scene);

            Set<List<Integer>> positions = new HashSet<>();
            for (int i = 0; i < blocks.size(); i++) {
                CompoundTag entry = blocks.getCompound(i);
                ListTag coordinates = entry.getList("pos", Tag.TAG_INT);
                List<Integer> pos = List.of(coordinates.getInt(0), coordinates.getInt(1), coordinates.getInt(2));
                assertTrue(positions.add(pos), "模板方块坐标重复: " + scene + " " + pos);
                assertTrue(pos.get(0) >= 0 && pos.get(0) < 13 && pos.get(1) >= 0 && pos.get(1) < 9
                        && pos.get(2) >= 0 && pos.get(2) < 13, "模板坐标越界: " + scene + " " + pos);
                int state = entry.getInt("state");
                assertTrue(state >= 0 && state < palette.size(), "palette 索引越界: " + scene + " " + pos);
                String id = palette.getCompound(state).getString("Name");
                referencedIds.add(id);
                actualBlocks.put(pos, id);
                CompoundTag properties = palette.getCompound(state).getCompound("Properties");
                assertPortLayout(scene, id, properties, pos);
                if (id.equals("create:fluid_pipe")) {
                    assertTrue(!(pos.get(0) >= 3 && pos.get(0) <= 9 && pos.get(1) >= 1 && pos.get(1) <= 5
                            && pos.get(2) >= 3 && pos.get(2) <= 9),
                            "外接管线不得穿入炉腔: " + scene + " " + pos);
                }
                if (id.equals("create:mechanical_pump")) {
                    pumpPositions.add(pos);
                    if (pos.equals(List.of(4, 1, 1)) || pos.equals(List.of(8, 0, 1)))
                        assertEquals("south", properties.getString("facing"), "给水/热液泵须向锅炉送液: " + scene);
                    if (pos.equals(List.of(4, 3, 1)) || pos.equals(List.of(8, 3, 1)))
                        assertEquals("north", properties.getString("facing"), "冷液泵须由锅炉回收液体: " + scene);
                }
                if (id.equals("create:creative_motor")) drivenMotors++;
            }
            if (scene.equals("high_pressure_boiler_operation") || scene.equals("high_pressure_boiler_shutdown")) {
                assertEquals(Set.of(List.of(4, 1, 1), List.of(8, 0, 1),
                        List.of(4, 3, 1), List.of(8, 3, 1)), pumpPositions, "流体回路泵位不完整: " + scene);
                assertEquals(4, drivenMotors, "每条液路应展示独立动力马达: " + scene);
            }
            assertTrue(referencedIds.containsAll(BOILER_PARTS), "模板未实际引用全部必要锅炉部件: " + scene);
            for (List<Integer> port : List.of(List.of(4, 1, 3), List.of(8, 2, 3)))
                assertBlock(actualBlocks, port, "create_nuclear_industry:high_pressure_boiler_water_port", scene);
            for (List<Integer> port : List.of(List.of(4, 0, 3), List.of(8, 0, 3)))
                assertBlock(actualBlocks, port, "create_nuclear_industry:high_pressure_boiler_hot_coolant_port", scene);
            for (List<Integer> port : List.of(List.of(4, 3, 3), List.of(8, 3, 3)))
                assertBlock(actualBlocks, port, "create_nuclear_industry:high_pressure_boiler_cold_coolant_port", scene);
            for (List<Integer> port : List.of(List.of(4, 4, 3), List.of(8, 4, 3)))
                assertBlock(actualBlocks, port, "create_nuclear_industry:high_pressure_boiler_steam_port", scene);
            assertBlock(actualBlocks, List.of(6, 2, 3), "create_nuclear_industry:high_pressure_boiler_controller", scene);
            assertBlock(actualBlocks, List.of(6, 6, 6), "create_nuclear_industry:boiler_safety_valve", scene);
            assertFalse(actualBlocks.containsKey(List.of(6, 7, 6)) || actualBlocks.containsKey(List.of(6, 8, 6)),
                    "安全阀上方必须留空: " + scene);

            long bottomExchangers = actualBlocks.entrySet().stream()
                    .filter(entry -> entry.getValue().equals("create_nuclear_industry:nuclear_heat_exchanger"))
                    .filter(entry -> entry.getKey().get(1) == 0 && entry.getKey().get(0) >= 4 && entry.getKey().get(0) <= 8
                            && entry.getKey().get(2) >= 4 && entry.getKey().get(2) <= 8)
                    .count();
            assertTrue(bottomExchangers > 0, "底面内部至少需要一台炉内换热器: " + scene);
            int partitionCells = 0;
            int reheatingSections = 0;
            for (int x = 4; x <= 8; x++) for (int z = 4; z <= 8; z++) {
                String id = actualBlocks.get(List.of(x, 3, z));
                if (id != null && (id.equals("create_nuclear_industry:boiler_heat_exchange_section")
                        || id.equals("create_nuclear_industry:high_pressure_boiler_casing"))) partitionCells++;
                if ("create_nuclear_industry:boiler_heat_exchange_section".equals(id)) reheatingSections++;
            }
            assertEquals(25, partitionCells, "隔层内部必须完整封闭: " + scene);
            assertTrue(reheatingSections > 0, "隔层必须至少包含一个再加热段: " + scene);
        }
    }

    private static void assertBlock(Map<List<Integer>, String> actualBlocks, List<Integer> position,
                                    String expectedId, String scene) {
        assertEquals(expectedId, actualBlocks.get(position), "模板实际方块缺失或错位: " + scene + " " + position);
    }

    @Test
    void everyStoryboardHasBilingualText() throws IOException {
        JsonObject chinese = JsonParser.parseString(Files.readString(
                RESOURCES.resolve("assets/create_nuclear_industry/lang/zh_cn.json"))).getAsJsonObject();
        JsonObject english = JsonParser.parseString(Files.readString(
                RESOURCES.resolve("assets/create_nuclear_industry/lang/en_us.json"))).getAsJsonObject();
        for (String scene : SCENES) {
            String header = "create_nuclear_industry.ponder." + scene + ".header";
            assertTrue(chinese.has(header) && !chinese.get(header).getAsString().isBlank(), "缺少中文标题: " + scene);
            assertTrue(english.has(header) && !english.get(header).getAsString().isBlank(), "缺少英文标题: " + scene);
            for (int line = 1; line <= textCount(scene); line++) {
                String key = "create_nuclear_industry.ponder." + scene + ".text_" + line;
                assertTrue(chinese.has(key) && !chinese.get(key).getAsString().isBlank(), "缺少中文正文: " + key);
                assertTrue(english.has(key) && !english.get(key).getAsString().isBlank(), "缺少英文正文: " + key);
            }
        }
    }

    private static void assertPortLayout(String scene, String id, CompoundTag properties, List<Integer> pos) {
        if (id.endsWith("_water_port")) {
            assertTrue(pos.get(1) < 3 && properties.getString("facing").equals("north"), "给水口须在水区并朝外: " + scene);
        } else if (id.endsWith("_steam_port")) {
            assertTrue(pos.get(1) > 3 && properties.getString("facing").equals("north"), "蒸汽口须在汽区并朝外: " + scene);
        } else if (id.endsWith("_hot_coolant_port")) {
            assertTrue(pos.get(1) == 0 && pos.get(2) == 3 && pos.get(0) > 3 && pos.get(0) < 9
                    && properties.getString("facing").equals("north"), "热液口须位于底边非角点并朝外: " + scene);
        } else if (id.endsWith("_cold_coolant_port")) {
            assertTrue(pos.get(1) == 3 && properties.getString("facing").equals("north"), "冷液口须与隔层同层并朝外: " + scene);
        } else if (id.endsWith("_controller")) {
            assertTrue(pos.get(1) < 3 && properties.getString("facing").equals("north"), "控制器须在水区侧面并朝外: " + scene);
        } else if (id.endsWith(":boiler_safety_valve")) {
            assertTrue(pos.get(1) == 6 && pos.get(0) > 3 && pos.get(0) < 9
                    && pos.get(2) > 3 && pos.get(2) < 9, "安全阀须位于顶面非棱边: " + scene);
        }
    }

    private static int textCount(String scene) {
        return switch (scene) {
            case "high_pressure_boiler_build" -> 7;
            case "high_pressure_boiler_operation" -> 8;
            case "high_pressure_boiler_steam" -> 5;
            case "high_pressure_boiler_shutdown" -> 5;
            default -> throw new IllegalArgumentException(scene);
        };
    }

}
