package com.iksxh.create_nuclear_industry;

import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 装配用法教学合同：验证唯一入口、实际模板归属和物流面，不运行正式生产tick。 */
class ShieldedAssemblyPonderContractTest {
    private static final Path ROOT = Path.of("src/main/resources/assets/create_nuclear_industry");
    private static final List<String> IDS = List.of("placement");
    private static final String NS = "create_nuclear_industry:";

    @Test void placementIsTheOnlyStoryboardWithBilingualParagraphsAndStationEntry() throws Exception {
        String plugin = Files.readString(Path.of("src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderPlugin.java"));
        assertTrue(plugin.contains("componentId(\"shielded_assembly_station\")"));
        assertFalse(plugin.contains("componentId(\"shielded_assembly_part\")"));
        for (String id : IDS) {
            assertTrue(plugin.contains("\"shielded_assembly_" + id + "\""));
            assertTrue(plugin.contains("ShieldedAssemblyPonderScenes::" + id));
            for (String locale : List.of("zh_cn", "en_us")) {
                var lang = JsonParser.parseString(Files.readString(ROOT.resolve("lang/" + locale + ".json"))).getAsJsonObject();
                for (String suffix : List.of("header", "text_1", "text_2", "text_3"))
                    assertTrue(lang.has("create_nuclear_industry.ponder.shielded_assembly_" + id + "." + suffix));
            }
        }
        for (String removed : List.of("manufacture", "sealing")) {
            assertFalse(plugin.contains("shielded_assembly_" + removed), "不应注册配方教学");
            assertFalse(plugin.contains("ShieldedAssemblyPonderScenes::" + removed));
            assertFalse(Files.exists(ROOT.resolve("ponder/shielded_assembly_" + removed + ".nbt")), "弃用模板应删除");
            for (String locale : List.of("zh_cn", "en_us")) {
                var lang = JsonParser.parseString(Files.readString(ROOT.resolve("lang/" + locale + ".json"))).getAsJsonObject();
                assertTrue(lang.keySet().stream().noneMatch(key -> key.startsWith(
                        "create_nuclear_industry.ponder.shielded_assembly_" + removed + ".")), "弃用字幕应删除");
            }
        }
    }

    @Test void templatesKeepEightOwnedPartsBottomShaftAndExteriorLogistics() throws Exception {
        for (String id : IDS) {
            Path file = ROOT.resolve("ponder/shielded_assembly_" + id + ".nbt");
            assertTrue(Files.exists(file), "缺少模板 " + id);
            CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
            ListTag entries = root.getList("blocks", Tag.TAG_COMPOUND);
            Map<BlockPos, CompoundTag> states = new HashMap<>(), data = new HashMap<>();
            for (int i = 0; i < entries.size(); i++) {
                CompoundTag e = entries.getCompound(i);
                ListTag p = e.getList("pos", Tag.TAG_INT);
                BlockPos pos = new BlockPos(p.getInt(0), p.getInt(1), p.getInt(2));
                CompoundTag state = palette.getCompound(e.getInt("state"));
                assertNull(states.put(pos, state), "坐标重复");
                data.put(pos, e.getCompound("nbt"));
                assertTrue(pos.getY() > 0 || state.getString("Name").equals("minecraft:polished_andesite"));
            }
            BlockPos master = new BlockPos(3, 2, 3);
            UUID owner = data.get(master).getUUID("ShieldedAssemblyOwner");
            for (int part = 0; part < 8; part++) {
                BlockPos pos = master.offset(part & 1, part >> 2, (part >> 1) & 1);
                var props = states.get(pos).getCompound("Properties");
                assertEquals("north", props.getString("facing"));
                assertEquals(NS + (part == 0 ? "shielded_assembly_station" : "shielded_assembly_part"), states.get(pos).getString("Name"));
                if (part == 0) assertEquals("true", props.getString("expanded"));
                else {
                    assertEquals(Integer.toString(part), props.getString("part"));
                    assertEquals(master.asLong(), data.get(pos).getLong("MasterPos"));
                    assertEquals(owner, data.get(pos).getUUID("OwnerId"));
                    assertFalse(data.get(pos).contains("ShieldedAssembly"), "代理不能有第二份库存");
                }
            }
            assertEquals("create:shaft", states.get(master.below()).getString("Name"));
            assertEquals("y", states.get(master.below()).getCompound("Properties").getString("axis"));
            for (BlockPos pos : List.of(new BlockPos(3, 2, 2), new BlockPos(3, 3, 2), new BlockPos(2, 2, 3), new BlockPos(2, 3, 3))) {
                var props = states.get(pos).getCompound("Properties");
                assertEquals("false", props.getString("extracting"));
                assertEquals(pos.getX() == 2 ? "west" : "north", props.getString("facing"));
            }
            assertEquals("true", states.get(new BlockPos(4, 2, 2)).getCompound("Properties").getString("extracting"));
            assertEquals("west", states.get(new BlockPos(4, 1, 2)).getCompound("Properties").getString("facing"));
            assertEquals("minecraft:barrel", states.get(new BlockPos(3, 1, 2)).getString("Name"));
            assertEquals("create:chute", states.get(new BlockPos(4, 4, 4)).getString("Name"));
        }
    }

    @Test void usagePageKeepsFadeClearanceWithoutRecipeMethodsOrSnapshots() throws Exception {
        Path file = Path.of("src/main/java/com/iksxh/create_nuclear_industry/ponder/ShieldedAssemblyPonderScenes.java");
        assertTrue(Files.exists(file));
        String source = Files.readString(file);
        assertTrue(source.contains("scene.idle(duration + 20)"));
        assertFalse(source.contains("void manufacture(") || source.contains("void sealing(")
                || source.contains("snapshot(") || source.contains("SpentFuelPayload"), "用法页不保留配方快照");
        assertFalse(source.contains(".advance(") || source.contains("serverTick(") || source.contains("loadPortableData("));
    }
}
