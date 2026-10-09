package com.iksxh.create_nuclear_industry;

import com.google.gson.JsonParser;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 烧结教学合同：读取真实模板和双语资源，验证热源高度及物流能力方向，不启动客户端。 */
class FuelSinteringPonderContractTest {
    private static final Path ROOT = Path.of("src/main/resources/assets/create_nuclear_industry");
    private static final List<String> IDS = List.of("fuel_sintering_operation", "fuel_sintering_automation");
    private static final String NS = "create_nuclear_industry:";

    @Test void independentStoryboardsHaveBilingualParagraphsAndEntry() throws Exception {
        String plugin = Files.readString(Path.of("src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderPlugin.java"));
        for (String id : IDS) {
            assertTrue(plugin.contains('"' + id + '"'), "烧结炉缺少独立入口 " + id);
            for (String locale : List.of("zh_cn", "en_us")) {
                var language = JsonParser.parseString(Files.readString(ROOT.resolve("lang/" + locale + ".json"))).getAsJsonObject();
                for (String suffix : List.of("header", "text_1", "text_2", "text_3")) {
                    String key = "create_nuclear_industry.ponder." + id + "." + suffix;
                    assertTrue(language.has(key) && !language.get(key).getAsString().isBlank(), key);
                }
            }
        }
        assertTrue(plugin.contains("FuelSinteringPonderScenes::fuelSinteringOperation"));
        assertTrue(plugin.contains("FuelSinteringPonderScenes::fuelSinteringAutomation"));
    }

    @Test void templatesKeepHeatAboveBaseAndLogisticsOnLegalFaces() throws Exception {
        for (String id : IDS) {
            Path path = ROOT.resolve("ponder/" + id + ".nbt");
            assertTrue(Files.exists(path), "缺少独立模板 " + id);
            CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
            ListTag list = root.getList("blocks", Tag.TAG_COMPOUND);
            Map<List<Integer>, CompoundTag> blocks = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                ListTag p = entry.getList("pos", Tag.TAG_INT);
                var pos = List.of(p.getInt(0), p.getInt(1), p.getInt(2));
                assertNull(blocks.put(pos, palette.getCompound(entry.getInt("state"))), "坐标重复");
                assertTrue(pos.get(1) >= 1 || palette.getCompound(entry.getInt("state")).getString("Name").equals("minecraft:polished_andesite"));
            }
            assertEquals(NS + "fuel_sintering_furnace", blocks.get(List.of(3, 2, 3)).getString("Name"));
            assertEquals("create:blaze_burner", blocks.get(List.of(3, 1, 3)).getString("Name"));
            assertEquals("kindled", blocks.get(List.of(3, 1, 3)).getCompound("Properties").getString("blaze"));
            if (id.endsWith("automation")) {
                assertEquals("create:chute", blocks.get(List.of(3, 3, 3)).getString("Name"));
                assertEquals("down", blocks.get(List.of(3, 3, 3)).getCompound("Properties").getString("facing"));
                var funnel = blocks.get(List.of(3, 2, 2));
                assertEquals("create:andesite_funnel", funnel.getString("Name"));
                assertEquals("north", funnel.getCompound("Properties").getString("facing"));
                assertEquals("true", funnel.getCompound("Properties").getString("extracting"));
                assertEquals("minecraft:hopper", blocks.get(List.of(3, 1, 2)).getString("Name"));
                assertEquals("west", blocks.get(List.of(3, 1, 2)).getCompound("Properties").getString("facing"));
                assertEquals("minecraft:barrel", blocks.get(List.of(2, 1, 2)).getString("Name"));
                for (int x : List.of(0, 6)) assertEquals("create:fluid_tank", blocks.get(List.of(x, 1, 3)).getString("Name"));
                for (int x : List.of(1, 2, 4, 5)) {
                    // 冷热管路沿东西轴，避开北侧漏斗；底部热源上下直接相邻。
                    var pipe = blocks.get(List.of(x, 1, 3));
                    assertEquals("create:fluid_pipe", pipe.getString("Name"));
                    assertEquals("true", pipe.getCompound("Properties").getString("east"));
                    assertEquals("true", pipe.getCompound("Properties").getString("west"));
                }
            }
        }
    }

    @Test void paragraphsWaitForFadeAndUseOnlyClientSnapshotState() throws Exception {
        Path path = Path.of("src/main/java/com/iksxh/create_nuclear_industry/ponder/FuelSinteringPonderScenes.java");
        assertTrue(Files.exists(path), "缺少两幕实现");
        String source = Files.readString(path);
        assertTrue(source.contains("scene.idle(duration + 20)"), "正文必须包含淡出后10tick净空");
        assertTrue(source.contains("modifyBlockEntityNBT") && source.contains("FuelSintering"));
        assertFalse(source.contains("serverTick(") || source.contains(".state().tick(") || source.contains("HeatExchangerBasinBridge"));
        assertTrue(source.contains("setValue(NuclearHeatExchangerBlock.FACING, Direction.EAST)"), "冷热端口应避开正面出料");
    }
}
