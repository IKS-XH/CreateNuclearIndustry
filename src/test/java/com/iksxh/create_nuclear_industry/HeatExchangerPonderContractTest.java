package com.iksxh.create_nuclear_industry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iksxh.create_nuclear_industry.ponder.P1PonderPlugin;
import com.iksxh.create_nuclear_industry.ponder.HeatExchangerPonderScenes;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderSceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证换热器五幕入口、真实端口布局、Create动力链、顶部负载和双语正文时序合同。 */
class HeatExchangerPonderContractTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Path SCENES = Path.of("src", "main", "java", "com", "iksxh",
            "create_nuclear_industry", "ponder", "HeatExchangerPonderScenes.java");
    private static final List<String> SCENE_IDS = List.of(
            "nuclear_heat_exchanger_introduction", "nuclear_heat_exchanger_heating",
            "nuclear_heat_exchanger_boiler", "nuclear_heat_exchanger_processing",
            "nuclear_heat_exchanger_condensation");
    private static final String NS = "create_nuclear_industry:";

    @Test
    void exchangerEntryHasFiveIndependentBilingualStoryboards() throws IOException {
        assertEquals(SCENE_IDS, P1PonderPlugin.HEAT_EXCHANGER_SCENE_IDS);
        JsonObject chinese = readLanguage("zh_cn.json");
        JsonObject english = readLanguage("en_us.json");
        Map<String, Integer> bodyCounts = Map.of(
                SCENE_IDS.get(0), 3, SCENE_IDS.get(1), 5,
                SCENE_IDS.get(2), 3, SCENE_IDS.get(3), 4, SCENE_IDS.get(4), 3);
        for (String scene : SCENE_IDS) {
            assertTrue(Files.exists(RESOURCES.resolve("assets/create_nuclear_industry/ponder/" + scene + ".nbt")),
                    "缺少独立换热器模板: " + scene);
            for (String locale : List.of("zh_cn.json", "en_us.json")) {
                JsonObject language = locale.equals("zh_cn.json") ? chinese : english;
                assertText(language, scene, "header");
                for (int line = 1; line <= bodyCounts.get(scene); line++)
                    assertText(language, scene, "text_" + line);
            }
        }
    }

    @Test
    void templatesKeepBothPortsVisibleAndMatchTheBoilerAndLoadLayouts() throws IOException {
        Map<List<Integer>, BlockState> introduction = readTemplate(SCENE_IDS.get(0));
        assertEquals(List.of(11, 7, 13), dimensions(SCENE_IDS.get(0)));
        assertId(introduction, List.of(5, 1, 5), NS + "nuclear_heat_exchanger", "首幕单台本体");
        assertId(introduction, List.of(5, 1, 6), NS + "nuclear_heat_exchanger", "首幕同向串联本体");
        assertId(introduction, List.of(5, 1, 1), "create:fluid_tank", "首幕冷液外端储罐");
        assertId(introduction, List.of(5, 1, 11), "create:fluid_tank", "首幕热液外端储罐");
        assertEquals("north", introduction.get(List.of(5, 1, 5)).properties().get("facing"));
        assertEquals("north", introduction.get(List.of(5, 1, 6)).properties().get("facing"));

        Map<List<Integer>, BlockState> heating = readTemplate(SCENE_IDS.get(1));
        assertEquals(List.of(15, 9, 13), dimensions(SCENE_IDS.get(1)));
        assertId(heating, List.of(7, 1, 5), NS + "nuclear_heat_exchanger", "直列前端换热器");
        assertId(heating, List.of(7, 1, 6), NS + "nuclear_heat_exchanger", "直列后端换热器");
        assertId(heating, List.of(6, 2, 5), "create:fluid_tank", "Create锅炉储罐组");
        for (int x = 6; x <= 7; x++) for (int z = 5; z <= 6; z++)
            assertId(heating, List.of(x, 2, z), "create:fluid_tank", "Create锅炉2×2储罐组");
        assertEquals(1L, heating.values().stream().filter(state -> state.id().equals("create:steam_engine")).count(),
                "Create锅炉应连接蒸汽机");
        assertEquals("wall", heating.get(List.of(5, 2, 5)).properties().get("face"));
        assertEquals("west", heating.get(List.of(5, 2, 5)).properties().get("facing"));
        assertId(heating, List.of(3, 2, 5), "create:powered_shaft", "蒸汽机计算所得动力轴位置");
        assertEquals("y", heating.get(List.of(3, 2, 5)).properties().get("axis"), "动力轴必须与水平facing轴垂直");
        assertEquals("north", heating.get(List.of(7, 1, 5)).properties().get("facing"));
        assertId(heating, List.of(7, 1, 1), "create:fluid_tank", "冷液前端储罐");
        assertId(heating, List.of(7, 1, 10), "create:fluid_tank", "热液后端储罐");

        Map<List<Integer>, BlockState> boiler = readTemplate(SCENE_IDS.get(2));
        assertEquals(List.of(15, 8, 13), dimensions(SCENE_IDS.get(2)));
        assertId(boiler, List.of(7, 1, 7), NS + "nuclear_heat_exchanger", "底层非边框内置换热器");
        assertId(boiler, List.of(7, 3, 7), NS + "boiler_heat_exchange_section", "上方再加热隔层");
        assertId(boiler, List.of(7, 1, 5), NS + "high_pressure_boiler_hot_coolant_port", "底层热液口");
        assertId(boiler, List.of(6, 3, 5), NS + "high_pressure_boiler_cold_coolant_port", "隔层冷液口");
        assertId(boiler, List.of(6, 3, 1), "create:fluid_tank", "锅炉冷液储罐实际位置");
        String source = Files.readString(SCENES);
        assertTrue(source.contains("BOILER_COLD_TANK = new BlockPos(6, 3, 1)"),
                "Java冷液罐常量须绑定模板实际坐标");
        for (int x = 6; x <= 8; x++) for (int z = 6; z <= 8; z++) {
            String expected = x == 7 && z == 7
                    ? NS + "boiler_heat_exchange_section" : NS + "high_pressure_boiler_casing";
            assertId(boiler, List.of(x, 3, z), expected, "锅炉隔层内部完整格");
        }
        for (int y = 1; y <= 5; y++) for (int x = 5; x <= 9; x++) for (int z = 5; z <= 9; z++) {
            boolean edge = x == 5 || x == 9 || z == 5 || z == 9 || y == 1 || y == 5;
            if (!edge) continue;
            String expected = switch (x + "," + y + "," + z) {
                case "7,1,5" -> NS + "high_pressure_boiler_hot_coolant_port";
                case "7,1,7" -> NS + "nuclear_heat_exchanger";
                case "6,3,5" -> NS + "high_pressure_boiler_cold_coolant_port";
                case "6,2,5" -> NS + "high_pressure_boiler_water_port";
                case "8,2,5" -> NS + "high_pressure_boiler_controller";
                case "6,4,5" -> NS + "high_pressure_boiler_steam_port";
                case "7,5,7" -> NS + "boiler_safety_valve";
                default -> NS + "high_pressure_boiler_casing";
            };
            assertId(boiler, List.of(x, y, z), expected, "锅炉合法边框或端口");
        }
        assertEquals("north", boiler.get(List.of(7, 1, 5)).properties().get("facing"));
        assertEquals("north", boiler.get(List.of(6, 3, 5)).properties().get("facing"));
        for (int y = 1; y <= 5; y++) {
            BlockState front = boiler.get(List.of(7, y, 5));
            assertNotNull(front, "锅炉剖面须保留可遮回的前壁/端口: y=" + y);
        }

        Map<List<Integer>, BlockState> processing = readTemplate(SCENE_IDS.get(3));
        assertId(processing, List.of(7, 1, 5), NS + "nuclear_heat_exchanger", "加工热源");
        assertId(processing, List.of(7, 2, 5), "create:basin", "工作盆");
        assertFalse(processing.containsKey(List.of(7, 3, 5)), "搅拌器与盆之间应保留一格空气");
        assertId(processing, List.of(7, 4, 5), "create:mechanical_mixer", "搅拌器");
        assertId(processing, List.of(7, 5, 5), "create:shaft", "随搅拌器上移的转轴");
        assertId(processing, List.of(7, 1, 10), "create:fluid_tank", "后侧热液输入罐");
        assertId(processing, List.of(7, 1, 1), "create:fluid_tank", "前侧冷液回流罐");
        assertTrue(Files.readString(SCENES).contains("FuelProcessingContent.FUEL_SINTERING_FURNACE.get().defaultBlockState()"),
                "烧结炉应通过 Ponder 临时世界替换顶部工作盆");

        Map<List<Integer>, BlockState> condensation = readTemplate(SCENE_IDS.get(4));
        assertId(condensation, List.of(7, 1, 5), NS + "nuclear_heat_exchanger", "蒸汽冷凝换热器");
        assertId(condensation, List.of(7, 2, 5), "minecraft:snow_block", "顶部冷源初始状态");
        assertId(condensation, List.of(7, 1, 10), "create:fluid_tank", "后侧蒸汽入口罐");
        assertId(condensation, List.of(7, 1, 1), "create:fluid_tank", "前侧冷凝水出口罐");
        assertTrue(source.contains("Blocks.PACKED_ICE") && source.contains("Blocks.BLUE_ICE"),
                "冷凝演示须覆盖浮冰和蓝冰");
    }

    @Test
    void sceneNotesHaveAVisibleTargetAndClearanceBetweenParagraphs() throws IOException {
        String source = Files.readString(SCENES);
        int pipeSelectionStart = source.indexOf("Selection pipes =", source.indexOf("nuclearHeatExchangerIntroduction"));
        int tankSelectionStart = source.indexOf("Selection tanks =", pipeSelectionStart);
        String introductionPipes = source.substring(pipeSelectionStart, tankSelectionStart);
        assertTrue(introductionPipes.contains("fromTo(5, 1, 7, 5, 1, 10)"),
                "首幕管道选择须从第二台换热器之后开始");
        assertFalse(introductionPipes.contains("fromTo(5, 1, 6, 5, 1, 10)"),
                "首个管路展示段不能提前包含第二台换热器");
        assertTrue(source.indexOf("showSection(pipes.add(tanks)")
                        < source.indexOf("showSection(util.select().position(5, 1, 6)"),
                "第二台换热器应在管路段之后单独揭示");
        assertTrue(source.contains("scene.idle(duration + 20);"), "正文间应留出 Ponder 1.0.82 生命周期净空");
        assertTrue(source.contains("tank.setController(CREATE_TANK_CONTROLLER);") && source.contains("tank.setWidth(2);")
                        && source.contains("tank.setHeight(1);")
                        && source.contains("FluidTankBlockEntity controller = tank.getControllerBE();")
                        && source.contains("controller.getTankInventory()"),
                "Ponder虚拟世界须用Create方块实体API把四格储罐初始化为同一锅炉罐组");
        assertTrue(source.contains("SteamEngineBlock.getShaftPos(engineState, CREATE_ENGINE)"),
                "动力轴位置须由Create蒸汽机API计算");
        assertTrue(source.contains("controller.boiler.attachedEngines = 1;")
                        && source.contains("controller.boiler.activeHeat = heated ? 1 : 0;")
                        && source.contains("controller.boiler.waterSupply = 10;")
                        && source.contains("shaft.update(CREATE_ENGINE, 1, efficiency);"),
                "Create锅炉与动力轴须初始化有效供水、热级、引擎和原生轴关联");
        assertTrue(source.contains("engine.getShaft();") && source.contains("scene.world().setKineticSpeed(shaft, 64);"),
                "蒸汽机Renderer须读取已关联轴的转动状态");
        int coolantExhausted = source.indexOf("setTankFluid(scene, LINE_HOT_TANK, FluidStack.EMPTY);");
        int residualHeatWait = source.indexOf("scene.idle(40);", coolantExhausted);
        int boilerShutdown = source.indexOf("setCreateBoilerPower(scene, false);", residualHeatWait);
        assertTrue(coolantExhausted >= 0 && residualHeatWait > coolantExhausted && boilerShutdown > residualHeatWait,
                "供热幕应在热液耗尽后保留既有余热窗口，再停止锅炉输出");
        assertTrue(source.contains("scene.world().setBlock(PROCESS_BASIN,"),
                "加工幕须在同一热冷回路上依次更换顶部设备");
        assertTrue(source.contains("scene.world().setBlock(CONDENSE_SOURCE, Blocks.WATER.defaultBlockState(), false);"),
                "冷凝幕须明确展示水源阶段");
        assertTrue(source.contains("scene.world().showSection(machine.add(pipes).add(tanks).add(coldSource), Direction.DOWN);"),
                "顶部冷源必须在首个可见段中显示，后续方块替换才能被看到");
        assertFalse(source.contains("only a visual example"), "正文和注释不引入开发者示意语句");
    }

    @Test
    void boilerCutawayClearsTheActualCameraRaysAndRestoresItsSelection() throws Exception {
        // 使用锁定Ponder的真实Selection实现和生产选区，不用源码字符串代替空间遮挡验证。
        var constructor = PonderSceneBuildingUtil.class.getDeclaredConstructor(BoundingBox.class);
        constructor.setAccessible(true);
        SceneBuildingUtil util = constructor.newInstance(new BoundingBox(0, 0, 0, 14, 7, 12));
        var method = HeatExchangerPonderScenes.class.getDeclaredMethod("boilerCutaway", SceneBuildingUtil.class);
        method.setAccessible(true);
        Selection cutaway = (Selection) method.invoke(null, util);
        Map<List<Integer>, BlockState> template = readTemplate(SCENE_IDS.get(2));
        Set<List<Integer>> visible = new HashSet<>(template.keySet());
        cutaway.forEach(pos -> visible.remove(List.of(pos.getX(), pos.getY(), pos.getZ())));
        // 管路在核心讲解阶段一起移开，之后才恢复端口；这里保守地将所有剩余模板格当作实心遮挡。
        visible.removeIf(pos -> pos.get(2) < 5 && pos.get(1) > 0);
        for (int y : List.of(1, 3)) {
            BlockPos core = new BlockPos(7, y, 7);
            assertFalse(cutaway.test(core), "剖面不能移走中心设备");
            assertClearCameraFaces(visible, core);
        }
        assertFalse(cutaway.test(new BlockPos(5, 2, 8)), "远侧墙须保留位置参照");
        assertFalse(cutaway.test(new BlockPos(7, 1, 9)), "远侧底边须保留层位参照");
        assertTrue(cutaway.test(new BlockPos(7, 1, 5)), "正对本体的热液口须先移开");
        assertTrue(cutaway.test(new BlockPos(9, 2, 7)), "另一面近侧墙须移开");
        assertTrue(cutaway.test(new BlockPos(7, 5, 7)), "俯视顶盖须移开");
        Selection ports = util.select().position(7, 1, 5).add(util.select().position(6, 3, 5));
        Selection lastRestore = cutaway.copy().substract(ports);
        ports.forEach(pos -> visible.add(List.of(pos.getX(), pos.getY(), pos.getZ())));
        lastRestore.forEach(pos -> visible.add(List.of(pos.getX(), pos.getY(), pos.getZ())));
        Set<List<Integer>> restoredBoiler = new HashSet<>(visible);
        restoredBoiler.removeIf(pos -> pos.get(1) == 0 || pos.get(2) < 5);
        Set<List<Integer>> originalBoiler = new HashSet<>(template.keySet());
        originalBoiler.removeIf(pos -> pos.get(1) == 0 || pos.get(2) < 5);
        assertEquals(originalBoiler, restoredBoiler, "两阶段恢复须返回完整模板锅炉");
        assertTrue(cutaway.test(new BlockPos(7, 1, 5)), "copy后扣除端口不能改变原剖面选区");
    }

    /** 按Ponder默认俯仰-35°、偏航145°，检查核心三面各九条射线；只验证方块遮挡，不替代播放验收。 */
    private static void assertClearCameraFaces(Set<List<Integer>> visible, BlockPos core) {
        double yaw = Math.toRadians(145);
        double pitch = Math.toRadians(35);
        double dx = Math.sin(yaw) * Math.cos(pitch);
        double dy = Math.sin(pitch);
        double dz = Math.cos(yaw) * Math.cos(pitch);
        for (int face = 0; face < 3; face++) for (double a : List.of(0.15, 0.5, 0.85))
            for (double b : List.of(0.15, 0.5, 0.85)) {
                double x = core.getX() + (face == 0 ? 1.001 : a);
                double y = core.getY() + (face == 1 ? 1.001 : face == 0 ? a : b);
                double z = core.getZ() + (face == 2 ? -0.001 : b);
                for (double distance = 0; distance < 12; distance += 0.05) {
                    List<Integer> cell = List.of((int) Math.floor(x + dx * distance),
                            (int) Math.floor(y + dy * distance), (int) Math.floor(z + dz * distance));
                    assertFalse(visible.contains(cell), "核心 " + core + " 的可见面被模板格遮挡: " + cell);
                }
            }
    }

    private static JsonObject readLanguage(String name) throws IOException {
        return JsonParser.parseString(Files.readString(RESOURCES.resolve("assets/create_nuclear_industry/lang/" + name)))
                .getAsJsonObject();
    }

    private static void assertText(JsonObject language, String scene, String item) {
        String key = "create_nuclear_industry.ponder." + scene + "." + item;
        assertTrue(language.has(key) && !language.get(key).getAsString().isBlank(), "缺少教学文案: " + key);
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
        try (var input = Files.newInputStream(RESOURCES.resolve("assets/create_nuclear_industry/ponder/" + scene + ".nbt"))) {
            ListTag size = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap()).getList("size", Tag.TAG_INT);
            return List.of(size.getInt(0), size.getInt(1), size.getInt(2));
        }
    }

    private static void assertId(Map<List<Integer>, BlockState> blocks, List<Integer> position,
                                 String expected, String description) {
        assertEquals(expected, blocks.get(position) == null ? null : blocks.get(position).id(),
                description + ": " + position);
    }

    private record BlockState(String id, Map<String, String> properties) {}
}
