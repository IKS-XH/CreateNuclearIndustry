package com.iksxh.create_nuclear_industry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iksxh.create_nuclear_industry.ponder.P1PonderPlugin;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** 新增教学的入口、客户端隔离、字幕索引与关键可见坐标；不重复模拟机器算法。 */
class PonderCurrentFeaturesSyncContractTest {
    private static final Path JAVA = Path.of("src/main/java/com/iksxh/create_nuclear_industry/ponder");
    private static final Path ASSETS = Path.of("src/main/resources/assets/create_nuclear_industry");
    private static final Map<String, Integer> NEW_SCENES = Map.of(
            "high_pressure_boiler_safety", 10, "experimental_reactor_safety", 9,
            "reactor_administrator_setup", 8, "reactor_administrator_operation", 7, "radiation_protection", 9);

    @Test void allNewEntrypointsUseExistingItemsAndAdministratorBelongsOnlyToAutomationPort() throws Exception {
        assertEquals(List.of("reactor_administrator_setup", "reactor_administrator_operation"), P1PonderPlugin.ADMINISTRATOR_SCENE_IDS);
        assertEquals(Set.of("radiation_meter", "radiation_helmet", "radiation_chestplate", "radiation_leggings", "radiation_boots",
                "cooled_spent_fuel_assembly", "accident_fuel_waste", "sealed_spent_fuel_cask", "dry_storage_rack", "shielding_concrete"),
                Set.copyOf(P1PonderPlugin.RADIATION_ENTRY_IDS));
        String plugin = source("P1PonderPlugin.java");
        assertTrue(plugin.contains("helper.forComponents(componentId(\"reactor_automation_port\"))"));
        assertFalse(plugin.contains("componentId(\"blaze_burner\")") || plugin.contains("componentId(\"create:blaze_burner\")"));
        assertTrue(plugin.contains("componentId(\"accident_fuel_waste\"), componentId(\"reactor_corium\")"));
        for (String entry : P1PonderPlugin.RADIATION_ENTRY_IDS)
            assertTrue(Files.exists(ASSETS.resolve("models/item/" + entry + ".json")), "入口必须有正式物品模型: " + entry);
    }

    @Test void bilingualTitlesAndBodiesHaveContiguousIdenticalIndices() throws Exception {
        JsonObject zh = language("zh_cn"), en = language("en_us");
        for (var entry : NEW_SCENES.entrySet()) {
            String prefix = "create_nuclear_industry.ponder." + entry.getKey() + ".";
            for (String suffix : List.of("header", "text_1")) {
                assertTrue(zh.has(prefix + suffix) && en.has(prefix + suffix));
            }
            for (int index = 1; index <= entry.getValue(); index++) {
                String key = prefix + "text_" + index;
                assertFalse(zh.get(key).getAsString().isBlank());
                assertFalse(en.get(key).getAsString().isBlank());
            }
            assertFalse(zh.has(prefix + "text_" + (entry.getValue() + 1)));
            assertFalse(en.has(prefix + "text_" + (entry.getValue() + 1)));
        }
        assertEquals(zh.keySet().stream().filter(k -> k.contains(".ponder.")).collect(java.util.stream.Collectors.toSet()),
                en.keySet().stream().filter(k -> k.contains(".ponder.")).collect(java.util.stream.Collectors.toSet()));
    }

    @Test void newTemplatesKeepUniqueBoundedCoordinatesAndRealPortRelationships() throws Exception {
        for (String scene : NEW_SCENES.keySet()) template(scene);
        Map<List<Integer>, CompoundTag> admin = template("reactor_administrator_setup");
        assertId(admin, 5, 2, 4, "reactor_automation_port");
        assertId(admin, 5, 2, 3, "create:blaze_burner");
        assertId(admin, 6, 3, 4, "reactor_instrument_port");
        assertId(admin, 6, 5, 6, "control_rod_drive");
        assertId(admin, 5, 2, 2, "minecraft:redstone_wire");
        assertId(admin, 6, 3, 2, "minecraft:redstone_torch");
        assertId(admin, 6, 3, 3, "minecraft:redstone_wire");
        assertEquals(admin, template("reactor_administrator_operation"), "两幕应复用同一接入几何");
        Map<List<Integer>, CompoundTag> radiation = template("radiation_protection");
        assertId(radiation, 7, 2, 7, "shielded_assembly_station");
        assertId(radiation, 7, 1, 7, "create:shaft");
        assertId(radiation, 3, 1, 8, "dry_storage_rack");
        // 屏蔽墙必须在样品(6,2,3)至观察侧(1,2,3)的同一水平路径上。
        assertId(radiation, 6, 2, 3, "create:depot");
        for (int y = 1; y <= 3; y++) for (int z = 2; z <= 4; z++) assertId(radiation, 3, y, z, "shielding_concrete");
        assertId(template("experimental_reactor_safety"), 10, 1, 7, "minecraft:tnt");
    }

    @Test void teachingUsesOnlyTemporaryDisplayInputsWithSingleParagraphClearance() throws Exception {
        for (String name : List.of("ReactorAdministratorPonderScenes.java", "ReactorSafetyPonderScenes.java", "RadiationPonderScenes.java")) {
            String code = withoutComments(source(name));
            assertTrue(code.contains("scene.idle(duration + 20)"), "正文需留出淡出净空: " + name);
            assertEquals(1, occurrences(code, "showOutlineWithText("), "所有正文应串行经过单一note入口: " + name);
            for (String forbidden : List.of("PacketDistributor", "Minecraft.getInstance", "ReactorAdministratorMenu.open", "ReactorAccidentService", "ReactorAccidentSavedData", "BoilerAccidentLifecycle", "RadiationClientEvents.receive", "setServerDisplayedDepthPercent", "setServerAutomationLocked", ".acceptMolten(", ".accept("))
                assertFalse(code.contains(forbidden), "教学不得进入正式服务端/全局显示状态: " + name + " " + forbidden);
        }
        String admin = withoutComments(source("ReactorAdministratorPonderScenes.java"));
        int setup = admin.indexOf("public static void setup(");
        int firstBody = admin.indexOf("note(scene, util.select().position(PORT)", setup);
        int showBurner = admin.indexOf("showSection(util.select().position(BURNER)", setup);
        assertTrue(firstBody >= 0 && showBurner > firstBody, "先看清接口，再显露会遮挡它的燃烧室");
        String prepare = admin.substring(admin.indexOf("private static CreateSceneBuilder prepare("), admin.indexOf("private static Selection circuit("));
        assertFalse(prepare.contains("position(BURNER)"), "共用准备阶段不能先遮挡接口");
        int operation = admin.indexOf("public static void operation(");
        assertTrue(admin.indexOf("signal(scene, util, true)", operation) < admin.indexOf("note(scene", operation), "讲运行模式前须显示运行许可和低急停");
        String safety = withoutComments(source("ReactorSafetyPonderScenes.java"));
        assertTrue(safety.contains("new BlockPos(5, 2, 7), Blocks.WATER"), "水须正面相邻下移后的熔融物");
        assertTrue(safety.contains("setBlock(MOLTEN.below(), AccidentContent.REACTOR_CORIUM"), "熔融物须原位凝固，不直接删为空气");
        assertTrue(safety.contains("payload(scene, SOLID)") && safety.contains("payload(scene, MOLTEN.below())"));
        assertTrue(safety.contains("AccidentContent.ACCIDENT_FIRE.get().defaultBlockState()") && safety.contains(".leftClick()"));
        assertFalse(safety.contains(".bind("), "事故火只做临时扑灭图示，不能绑定正式租约");
    }

    @Test void quickSwapAnimationHasOneInputClickAndKeepsArmAndMaintenanceSeparate() throws Exception {
        String source = source("P1PonderScenes.java");
        int begin = source.indexOf("// 枯竭列只需一次持新料右击");
        int end = source.indexOf("scene.world().modifyEntity(replacementFuel, Entity::discard)", begin);
        assertTrue(begin >= 0 && end > begin);
        String swap = source.substring(begin, end);
        assertEquals(1, occurrences(swap, ".rightClick()"));
        assertTrue(swap.contains(".withItem(freshFuel).rightClick()") && swap.contains(".withItem(cooledSpentFuel)"));
        assertFalse(swap.contains("ItemStack.EMPTY"), "快捷替换不能画中间空槽");
        assertTrue(source.contains("ModItems.STEEL_PLATE.get()"));
        assertTrue(source.contains("ArmBlockEntity.Phase.MOVE_TO_INPUT") && source.contains("ArmBlockEntity.Phase.MOVE_TO_OUTPUT"));
    }

    private static Map<List<Integer>, CompoundTag> template(String name) throws IOException {
        CompoundTag root;
        try (var input = Files.newInputStream(ASSETS.resolve("ponder/" + name + ".nbt"))) {
            root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
        }
        ListTag size = root.getList("size", Tag.TAG_INT), palette = root.getList("palette", Tag.TAG_COMPOUND), blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        Map<List<Integer>, CompoundTag> actual = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            ListTag pos = entry.getList("pos", Tag.TAG_INT);
            assertEquals(3, pos.size());
            var key = List.of(pos.getInt(0), pos.getInt(1), pos.getInt(2));
            for (int axis = 0; axis < 3; axis++) assertTrue(key.get(axis) >= 0 && key.get(axis) < size.getInt(axis));
            int state = entry.getInt("state");
            assertTrue(state >= 0 && state < palette.size());
            assertNull(actual.put(key, palette.getCompound(state)), "重复模板坐标: " + name + key);
        }
        return actual;
    }
    private static void assertId(Map<List<Integer>, CompoundTag> blocks, int x, int y, int z, String id) {
        CompoundTag block = blocks.get(List.of(x, y, z));
        assertNotNull(block, "缺少教学目标: " + List.of(x, y, z));
        assertEquals(id.contains(":") ? id : "create_nuclear_industry:" + id, block.getString("Name"));
    }
    private static JsonObject language(String locale) throws IOException { return JsonParser.parseString(Files.readString(ASSETS.resolve("lang/" + locale + ".json"))).getAsJsonObject(); }
    private static String source(String name) throws IOException { return Files.readString(JAVA.resolve(name)); }
    private static String withoutComments(String source) { return Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL).matcher(source).replaceAll(""); }
    private static int occurrences(String source, String part) { return source.split(Pattern.quote(part), -1).length - 1; }
}
