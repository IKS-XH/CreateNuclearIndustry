package com.iksxh.create_nuclear_industry;

import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.lang.reflect.Modifier;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 单页用法合同：真实模板、教学快照及字幕净空；不执行正式贮存事务。 */
class DryStoragePonderContractTest {
    private static final Path ASSETS=Path.of("src/main/resources/assets/create_nuclear_industry");
    private static final Path SCENE=Path.of("src/main/java/com/iksxh/create_nuclear_industry/ponder/DryStoragePonderScenes.java");
    private static final String PREFIX="create_nuclear_industry.ponder.dry_storage_usage.";
    private static final String NS="create_nuclear_industry:";
    private static final BlockPos RACK=new BlockPos(2,2,2), INPUT=new BlockPos(3,2,2), OUTPUT=new BlockPos(2,1,2);

    @Test void rackHasOneUsageEntryAndSixCompleteBilingualParagraphs() throws Exception {
        String plugin=Files.readString(Path.of("src/main/java/com/iksxh/create_nuclear_industry/ponder/P1PonderPlugin.java"));
        assertTrue(plugin.contains("componentId(\"dry_storage_rack\")"),"贮存架缺少直接入口");
        assertEquals(1,plugin.split("DryStoragePonderScenes::usage",-1).length-1);
        for(String locale:List.of("zh_cn","en_us")) {
            var lang=JsonParser.parseString(Files.readString(ASSETS.resolve("lang/"+locale+".json"))).getAsJsonObject();
            assertEquals(7,lang.keySet().stream().filter(k->k.startsWith(PREFIX)).count());
            for(String suffix:List.of("header","text_1","text_2","text_3","text_4","text_5","text_6"))
                assertFalse(lang.get(PREFIX+suffix).getAsString().isBlank());
        }
        var chinese=JsonParser.parseString(Files.readString(ASSETS.resolve("lang/zh_cn.json"))).getAsJsonObject();
        String text=String.join(" ",chinese.keySet().stream().filter(k->k.startsWith(PREFIX)).map(k->chinese.get(k).getAsString()).toList());
        assertTrue(text.contains("空手右击") && text.contains("默认16") && text.contains("底部只") && text.contains("上游") && text.contains("背包满"));
        for(String forbidden:List.of("潜行","配方","示意","再处理","永久安全","加工时间"))assertFalse(text.contains(forbidden));
    }

    /** 用MC原生NBT读入磁盘模板，检查物流真实方向、地台高度和完整组件载荷。 */
    @Test void actualTemplateHasSingleRackLegalHoppersAndValidPayloadSample() throws Exception {
        var template=template();var states=template.states();var data=template.data();
        assertEquals(List.of(5,5,5),ints(template.root().getList("size",Tag.TAG_INT)));
        assertEquals(1,states.values().stream().filter(s->s.getString("Name").equals(NS+"dry_storage_rack")).count());
        assertEquals("south",states.get(RACK).getCompound("Properties").getString("facing"));
        assertEquals("0",states.get(RACK).getCompound("Properties").getString("storage_level"));
        assertEquals(16,data.get(RACK).getInt("Capacity"));assertTrue(data.get(RACK).getCompound("DryStorage").isEmpty());
        assertEquals("minecraft:hopper",states.get(INPUT).getString("Name"));
        assertEquals("west",states.get(INPUT).getCompound("Properties").getString("facing"));
        assertEquals("minecraft:hopper",states.get(OUTPUT).getString("Name"));
        assertEquals("south",states.get(OUTPUT).getCompound("Properties").getString("facing"));
        assertEquals("minecraft:barrel",states.get(new BlockPos(2,1,3)).getString("Name"));
        assertPayload(sample(template));
        for(var entry:states.entrySet())assertTrue(entry.getKey().getY()>0 || entry.getValue().getString("Name").equals("minecraft:polished_andesite"));
    }

    @Test void fullTeachingSnapshotHasSixteenIndependentSingleCasksAndOnePublicStory() throws Exception {
        assertTrue(Files.exists(SCENE),"缺少单页场景");
        Class<?> type=Class.forName("com.iksxh.create_nuclear_industry.ponder.DryStoragePonderScenes");
        var stories=Arrays.stream(type.getDeclaredMethods()).filter(m->Modifier.isPublic(m.getModifiers())).map(m->m.getName()).toList();
        assertEquals(List.of("usage"),stories);
        var method=type.getDeclaredMethod("inventorySnapshot",int.class,CompoundTag.class);method.setAccessible(true);
        var sample=sample(template());
        var full=(CompoundTag)method.invoke(null,16,sample);var content=full.getCompound("DryStorage");
        assertEquals(16,full.getInt("Capacity"));assertEquals(16,content.getAllKeys().size());
        for(int slot=0;slot<16;slot++)assertPayload(content.getCompound("Slot"+slot));
        content.getCompound("Slot0").putInt("count",0);assertEquals(1,content.getCompound("Slot1").getInt("count"));assertEquals(1,sample.getInt("count"));
        assertEquals(15,((CompoundTag)method.invoke(null,15,sample)).getCompound("DryStorage").getAllKeys().size());
        assertTrue(((CompoundTag)method.invoke(null,0,sample)).getCompound("DryStorage").isEmpty());
    }

    @Test void sceneUsesClientSnapshotsFadeClearanceAndConservedWaitingCask() throws Exception {
        assertTrue(Files.exists(SCENE),"缺少单页场景");String source=Files.readString(SCENE);
        assertTrue(source.contains("scene.title(\"dry_storage_usage\""));assertTrue(source.contains("scene.configureBasePlate(0, 0, 5)"));
        // 锁定原生145°默认镜头和仅Y翻转后，180°增量使南向正面及东侧输入可见。
        assertTrue(source.contains("scene.rotateCameraY(180)"));
        assertTrue(source.contains("scene.idle(duration + 20)"));assertTrue(source.contains("scene.markAsFinished()"));
        assertTrue(source.contains("SpentFuelPayload.seal") && source.contains("rack.loadWithComponents"));
        assertTrue(source.contains("DryStorageBlock.STORAGE_LEVEL") && source.contains("snapshot(scene, 16)") && source.contains("snapshot(scene, 15)"));
        assertTrue(source.contains("waiting(scene, true)") && source.contains("waiting(scene, false)"));
        for(String forbidden:List.of("insertOne(","takeToPlayer(","serverTick(",".state().insert(",".state().extract(",".changed(",".whileSneaking()"))assertFalse(source.contains(forbidden));
    }

    private record Template(CompoundTag root,Map<BlockPos,CompoundTag> states,Map<BlockPos,CompoundTag> data) {}
    private static Template template() throws Exception {
        Path path=ASSETS.resolve("ponder/dry_storage_usage.nbt");assertTrue(Files.exists(path),"缺少真实模板");
        CompoundTag root=NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap());var palette=root.getList("palette",Tag.TAG_COMPOUND);
        Map<BlockPos,CompoundTag> states=new HashMap<>(),data=new HashMap<>();var entries=root.getList("blocks",Tag.TAG_COMPOUND);
        for(int i=0;i<entries.size();i++){var entry=entries.getCompound(i);var p=entry.getList("pos",Tag.TAG_INT);var pos=new BlockPos(p.getInt(0),p.getInt(1),p.getInt(2));
            assertNull(states.put(pos,palette.getCompound(entry.getInt("state"))));data.put(pos,entry.getCompound("nbt"));}
        return new Template(root,states,data);
    }
    private static List<Integer> ints(ListTag list) { return java.util.stream.IntStream.range(0,list.size()).mapToObj(list::getInt).toList(); }
    private static CompoundTag sample(Template template) { return template.data().get(new BlockPos(3,3,2)).getList("Items",Tag.TAG_COMPOUND).getCompound(0); }
    private static void assertPayload(CompoundTag cask) {
        assertEquals(NS+"sealed_spent_fuel_cask",cask.getString("id"));assertEquals(1,cask.getInt("count"));
        var contents=cask.getCompound("components").getList("minecraft:container",Tag.TAG_COMPOUND);assertEquals(1,contents.size());
        assertEquals(0,contents.getCompound(0).getInt("slot"));var spent=contents.getCompound(0).getCompound("item");
        assertEquals(NS+"cooled_spent_fuel_assembly",spent.getString("id"));assertEquals(1,spent.getInt("count"));
    }
}
