package com.iksxh.create_nuclear_industry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iksxh.create_nuclear_industry.ponder.P1PonderPlugin;
import com.iksxh.create_nuclear_industry.turbine.TurbineGeometry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 依据汽轮机正式八棱几何检查三幕入口、模板构件与字幕资源。 */
class TurbinePonderContractTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Path SCENES = Path.of("src", "main", "java", "com", "iksxh",
            "create_nuclear_industry", "ponder", "TurbinePonderScenes.java");
    private static final List<String> STORYBOARDS = List.of(
            "steam_turbine_build", "steam_turbine_operation", "steam_turbine_efficiency");
    private static final List<String> COMPONENTS = List.of(
            "turbine_casing", "turbine_rotor", "turbine_inlet", "turbine_exhaust",
            "turbine_output_shaft", "turbine_controller", "turbine_window");
    private static final String NS = "create_nuclear_industry:";

    @Test
    void sevenComponentsShareTheThreeOrderedStoryboardsAndBilingualText() throws IOException {
        assertEquals(COMPONENTS, P1PonderPlugin.TURBINE_ENTRY_IDS);
        assertEquals(STORYBOARDS, P1PonderPlugin.TURBINE_SCENE_IDS);
        assertEquals(COMPONENTS.size(), Set.copyOf(COMPONENTS).size());

        JsonObject chinese = readLanguage("zh_cn.json");
        JsonObject english = readLanguage("en_us.json");
        Map<String, Integer> textCounts = Map.of("steam_turbine_build", 12,
                "steam_turbine_operation", 6, "steam_turbine_efficiency", 6);
        for (String scene : STORYBOARDS) {
            assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/ponder/" + scene + ".nbt")),
                    "缺少独立汽轮机模板: " + scene);
            assertTrue(hasText(chinese, scene, "header"), "缺少中文标题: " + scene);
            assertTrue(hasText(english, scene, "header"), "缺少英文标题: " + scene);
            for (int line = 1; line <= textCounts.get(scene); line++) {
                assertTrue(hasText(chinese, scene, "text_" + line), "缺少中文正文: " + scene + "#" + line);
                assertTrue(hasText(english, scene, "text_" + line), "缺少英文正文: " + scene + "#" + line);
            }
        }
    }

    @Test
    void templatesMatchTheThreeRealOctagonalTiersAndVisibleRoutes() throws IOException {
        Map<List<Integer>, BlockState> build = readTemplate("steam_turbine_build");
        assertEquals(List.of(26, 10, 15), dimensions("steam_turbine_build"));
        assertTrue(build.values().stream().map(BlockState::id).collect(java.util.stream.Collectors.toSet())
                .containsAll(COMPONENTS.stream().map(id -> NS + id).toList()));
        verifyTurbine(build, 8, 2, 3, 3, 5, 5);
        verifyTurbine(build, 12, 3, 5, 6, 8, 4);
        verifyTurbine(build, 20, 4, 7, 9, 11, 3);

        for (String scene : List.of("steam_turbine_operation", "steam_turbine_efficiency")) {
            Map<List<Integer>, BlockState> blocks = readTemplate(scene);
            assertEquals(List.of(15, 9, 11), dimensions(scene));
            verifyTurbine(blocks, 7, 2, 3, 3, 5, 3);
            assertId(blocks, List.of(7, 2, 2), "create:shaft", scene + " 前端外接轴");
            assertId(blocks, List.of(7, 2, 8), "create:shaft", scene + " 后端外接轴");
            assertId(blocks, List.of(12, 2, 5), "create:fluid_tank", scene + " 超临界蒸汽输入罐");
            assertId(blocks, List.of(2, 2, 4), "create:fluid_tank", scene + " 普通蒸汽排出罐");
            for (int x = 9; x <= 11; x++) assertId(blocks, List.of(x, 2, 5), "create:fluid_pipe", scene + " 输入管");
            for (int x = 3; x <= 5; x++) assertId(blocks, List.of(x, 2, 4), "create:fluid_pipe", scene + " 排出管");
            for (int x = 1; x <= 13; x++) for (int z = 2; z <= 9; z++)
                assertId(blocks, List.of(x, 0, z), "minecraft:polished_andesite", scene + " 承托地台");
            for (int x = 9; x <= 11; x++) {
                assertEquals("true", blocks.get(List.of(x, 2, 5)).properties().get("east"));
                assertEquals("true", blocks.get(List.of(x, 2, 5)).properties().get("west"));
            }
            assertFalse(blocks.keySet().stream().anyMatch(pos -> pos.get(0) >= 6 && pos.get(0) <= 8
                    && pos.get(1) >= 1 && pos.get(1) <= 3 && pos.get(2) >= 3 && pos.get(2) <= 7
                    && blocks.get(pos).id().equals("create:fluid_pipe")), "管线不得穿过汽轮机外壳: " + scene);
        }
    }

    @Test
    void displayedSectionsAreInitializedBeforeHidingAndTextHasTenTickClearance() throws IOException {
        String source = Files.readString(SCENES);
        assertTrue(source.contains("scene.idle(duration + 20);"), "正文时长须为额外10tick的API寿命留出净间隔");
        assertTrue(source.contains("showTier(CreateSceneBuilder scene"), "尺寸比较应复用按单台显示的阶段");
        assertTrue(source.contains(".rightClick().withItem(new ItemStack(TurbineContent.CASING_ITEM.get()))"),
                "补壳提示须展示手持汽轮机机壳");
        assertTrue(source.contains("scene.rotateCameraY(180);"), "对侧端口需切换镜头");
        String build = method(source, "public static void steamTurbineBuild(", "public static void steamTurbineOperation(");
        int lastRing = build.indexOf("Selection rearCap = slice(util, x, fy, fz, 3, 4, 4).substract(core);");
        int inletFocus = build.indexOf("Selection inlet =", lastRing);
        assertTrue(lastRing >= 0 && inletFocus > lastRing, "完整机壳的后端壳环必须在接口讲解前单独显示");
        for (int z = 0; z <= 4; z++) {
            assertTrue(build.contains("slice(util, x, fy, fz, 3, " + z + ", " + z + ").substract(core)"),
                    "搭建幕必须逐个显示轴向壳环，缺少z=" + z);
        }
        assertTrue(build.contains("centerOf(x + 1, fy, fz + 1)"), "沿轴延伸提示须指向已显示的壳体");
        assertTrue(source.contains("setTankFluid(scene, OP_OUTPUT_TANK, new FluidStack(TurbineContent.STEAM.get(), 802));"),
                "效率满额阶段须显示实际普通蒸汽累计量");
        int decayWait = source.indexOf("scene.idle(40);");
        int stopAxes = source.indexOf("scene.world().setKineticSpeed(axles, 0);");
        assertTrue(decayWait >= 0 && stopAxes > decayWait, "断汽后须先完整等待40tick窗口再清零两端轴速");
        assertEquals(3, countOccurrences(build, "showTier(scene, util,"), "尺寸对比必须逐档展示三台独立机组");
        String showTier = method(source, "private static void showTier(", "private static Selection machine(");
        int show = showTier.indexOf("var section = scene.world().showIndependentSection(tier, Direction.DOWN);");
        int mergeWait = showTier.indexOf("scene.idle(15);", show);
        int move = showTier.indexOf("scene.world().moveSection(section, ", mergeWait);
        int moveWait = showTier.indexOf("scene.idle(15);", move);
        int body = showTier.indexOf("noteWithBounds(scene, framedBounds, 75, text);", moveWait);
        int hide = showTier.indexOf("scene.world().hideIndependentSection(section, Direction.SOUTH);", body);
        int fadeWait = showTier.indexOf("scene.idle(15);", hide);
        assertTrue(show >= 0 && mergeWait > show && move > mergeWait && moveWait > move
                        && body > moveWait && hide > body && fadeWait > hide,
                "尺寸示例必须使用同一区段句柄，按显示、合并、移动、正文、隐藏和淡出时序执行");
        assertTrue(showTier.substring(move, moveWait).contains(", 15);"), "移动动画须保留15tick时长");
        assertFalse(source.contains("scene.world().hideSection(input"), "断汽应保持完整管路并清空临时汽罐");
        assertTrue(source.contains("setTankFluid(scene, OP_INPUT_TANK, FluidStack.EMPTY);"), "停机阶段应清空临时进汽罐");
    }

    private static void verifyTurbine(Map<List<Integer>, BlockState> blocks, int fx, int fy,
                                      int diameter, int rotors, int length, int fz) {
        int radius = (diameter - 1) / 2;
        String facing = "north";
        for (int z = 0; z < length; z++) {
            TurbineGeometry.Section section = z == 0 ? TurbineGeometry.Section.FRONT
                    : z == length - 1 ? TurbineGeometry.Section.REAR : TurbineGeometry.Section.MIDDLE;
            for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++) {
                if (!TurbineGeometry.footprint(diameter, x, y)) continue;
                List<Integer> pos = List.of(fx + x, fy + y, fz + z);
                if (z > 0 && z < length - 1 && TurbineGeometry.airSlot(diameter, x, y)) {
                    assertFalse(blocks.containsKey(pos), "中段内腔必须留空: " + pos);
                    continue;
                }
                BlockState state = blocks.get(pos);
                assertTrue(state != null, "汽轮机合法几何格缺失: " + pos);
                if (z == 0 || z == length - 1) {
                    if (x == 0 && y == 0) {
                        assertEquals(NS + "turbine_output_shaft", state.id(), "轴端位置错误: " + pos);
                        assertEquals(z == 0 ? "front" : "rear", state.properties().get("end"));
                    } else {
                        assertEquals(NS + "turbine_casing", state.id(), "端面必须封壳: " + pos);
                        assertEquals(Integer.toString(TurbineGeometry.pieceId(diameter, section, x, y)),
                                state.properties().get("piece"), "端盖片编号须来自正式几何表: " + pos);
                    }
                } else if (x == 0 && y == 0) {
                    assertEquals(NS + "turbine_rotor", state.id(), "中段轴心须连续放转子: " + pos);
                    assertEquals("d" + diameter, state.properties().get("diameter"));
                } else if (TurbineGeometry.shellSlot(diameter, x, y)) {
                    boolean controller = x == 0 && y == radius && z == middleRow(rotors, length);
                    boolean inlet = x == radius && y == 0 && z == middleRow(rotors, length);
                    boolean exhaust = x == -radius && y == 0 && z == 1;
                    boolean window = x == -radius && y == 0 && z == middleRow(rotors, length);
                    String expected = controller ? NS + "turbine_controller" : inlet ? NS + "turbine_inlet"
                            : exhaust ? NS + "turbine_exhaust" : window ? NS + "turbine_window" : NS + "turbine_casing";
                    assertEquals(expected, state.id(), "八棱外环或端口位置错误: " + pos);
                    if (window) assertEquals(Integer.toString(TurbineGeometry.pieceId(diameter,
                            TurbineGeometry.Section.MIDDLE, x, y)), state.properties().get("piece"));
                    if (inlet || exhaust) assertTrue(state.properties().containsKey("outward"), "端口须序列化外向: " + pos);
                    if (controller) assertTrue(state.properties().containsKey("side"), "控制器须序列化所在侧: " + pos);
                    if (!expected.equals(NS + "turbine_casing")) continue;
                    assertEquals(Integer.toString(TurbineGeometry.pieceId(diameter, section, x, y)),
                            state.properties().get("piece"), "机壳片编号须来自正式几何表: " + pos);
                } else {
                    assertFalse(state.id().startsWith(NS), "内腔不能填壳: " + pos);
                }
                assertEquals(facing, state.properties().get("machine_facing"), "机组朝向不一致: " + pos);
            }
        }
        int middle = middleRow(rotors, length);
        assertId(blocks, List.of(fx + radius, fy, fz + middle), NS + "turbine_inlet", "合法侧面中央进汽口");
        assertId(blocks, List.of(fx - radius, fy, fz + 1), NS + "turbine_exhaust", "合法近端排汽口");
        assertId(blocks, List.of(fx, fy + radius, fz + middle), NS + "turbine_controller", "合法中央控制器");
        assertId(blocks, List.of(fx - radius, fy, fz + middle), NS + "turbine_window", "观察窗必须位于合法中段侧槽");
        assertEquals("east", blocks.get(List.of(fx + radius, fy, fz + middle)).properties().get("outward"));
        assertEquals("west", blocks.get(List.of(fx - radius, fy, fz + 1)).properties().get("outward"));
    }

    private static int middleRow(int rotors, int length) {
        return length % 2 == 0 ? length / 2 : rotors / 2 + 1;
    }

    private static Map<List<Integer>, BlockState> readTemplate(String scene) throws IOException {
        Path path = RESOURCES.resolve("assets/create_nuclear_industry/ponder/" + scene + ".nbt");
        CompoundTag root;
        try (var input = Files.newInputStream(path)) {
            root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        }
        ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        Map<List<Integer>, BlockState> actual = new HashMap<>();
        Set<List<Integer>> coordinates = new HashSet<>();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = blocks.getCompound(i);
            ListTag p = block.getList("pos", Tag.TAG_INT);
            List<Integer> pos = List.of(p.getInt(0), p.getInt(1), p.getInt(2));
            assertTrue(coordinates.add(pos), "NBT坐标重复: " + scene + " " + pos);
            assertTrue(pos.get(0) >= 0 && pos.get(0) < 30 && pos.get(1) >= 0 && pos.get(1) < 13
                    && pos.get(2) >= 0 && pos.get(2) < 16, "NBT坐标越界: " + scene + " " + pos);
            int state = block.getInt("state");
            assertTrue(state >= 0 && state < palette.size(), "palette索引越界: " + scene + " " + pos);
            CompoundTag entry = palette.getCompound(state);
            CompoundTag properties = entry.getCompound("Properties");
            Map<String, String> values = new HashMap<>();
            for (String key : properties.getAllKeys()) values.put(key, properties.getString(key));
            actual.put(pos, new BlockState(entry.getString("Name"), Map.copyOf(values)));
        }
        return actual;
    }

    private static List<Integer> dimensions(String scene) throws IOException {
        Path path = RESOURCES.resolve("assets/create_nuclear_industry/ponder/" + scene + ".nbt");
        try (var input = Files.newInputStream(path)) {
            CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            ListTag size = root.getList("size", Tag.TAG_INT);
            return List.of(size.getInt(0), size.getInt(1), size.getInt(2));
        }
    }

    private static JsonObject readLanguage(String name) throws IOException {
        return JsonParser.parseString(Files.readString(
                RESOURCES.resolve("assets/create_nuclear_industry/lang/" + name))).getAsJsonObject();
    }

    private static boolean hasText(JsonObject language, String scene, String item) {
        String key = "create_nuclear_industry.ponder." + scene + "." + item;
        return language.has(key) && !language.get(key).getAsString().isBlank();
    }

    private static void assertId(Map<List<Integer>, BlockState> blocks, List<Integer> position,
                                 String expected, String description) {
        assertEquals(expected, blocks.get(position) == null ? null : blocks.get(position).id(), description + ": " + position);
    }

    private static String method(String source, String start, String end) {
        int begin = source.indexOf(start);
        int finish = source.indexOf(end, begin);
        assertTrue(begin >= 0 && finish > begin, "场景方法边界缺失: " + start);
        return source.substring(begin, finish);
    }

    private static int countOccurrences(String source, String searched) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(searched, index)) >= 0) {
            count++;
            index += searched.length();
        }
        return count;
    }

    private record BlockState(String id, Map<String, String> properties) {}
}
