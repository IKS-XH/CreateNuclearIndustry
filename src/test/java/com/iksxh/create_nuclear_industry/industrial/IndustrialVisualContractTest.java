package com.iksxh.create_nuclear_industry.industrial;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import com.iksxh.create_nuclear_industry.content.IndustrialFluidContent;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** 解析实际依赖及生产资源；连接、方向与首行只证明自动合同，不替代GPU和鼠标复看。 */
class IndustrialVisualContractTest {
    static final Path ROOT = Path.of("src/main/resources");
    static final Path CREATE = Path.of(System.getProperty("user.home"), ".gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/92471e8fc5ed4e3c2279001d77ebcec6fd38bcab/create-1.21.1-6.0.10-280-slim.jar");
    static final Path MC = Path.of(System.getProperty("user.home"), ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar");
    static final List<String> IDS = List.of("reinforced_fluid_pipe", "ceramic_fluid_pipe", "industrial_fluid_pump", "reinforced_fluid_valve", "ceramic_fluid_valve");
    static byte[] resource(String namespace, String path) throws Exception {
        String entry = "assets/" + namespace + "/" + path;
        if (namespace.equals("create_nuclear_industry")) return Files.readAllBytes(ROOT.resolve(entry));
        try (var zip = new ZipFile((namespace.equals("create") ? CREATE : MC).toFile())) {
            var found = zip.getEntry(entry); assertNotNull(found, "缺少实际资源 " + entry);
            return zip.getInputStream(found).readAllBytes();
        }
    }
    static JsonObject json(String namespace, String path) throws Exception { return JsonParser.parseString(new String(resource(namespace, path), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject(); }
    static String[] id(String name) { return name.contains(":") ? name.split(":", 2) : new String[]{"minecraft", name}; }
    /** 父模型先解析，再由子模型覆盖纹理；面片继承遵守Minecraft整组替代语义。 */
    static JsonObject resolved(String name, Set<String> active) throws Exception {
        assertTrue(active.add(name), "父模型循环 " + name);
        var key = id(name); var own = json(key[0], "models/" + key[1] + ".json"); var result = new JsonObject();
        if (own.has("parent")) result = resolved(own.get("parent").getAsString(), active);
        for (var entry : own.entrySet()) {
            if (entry.getKey().equals("textures")) {
                var textures = result.has("textures") ? result.getAsJsonObject("textures").deepCopy() : new JsonObject();
                for (var texture : entry.getValue().getAsJsonObject().entrySet()) textures.add(texture.getKey(), texture.getValue());
                result.add("textures", textures);
            } else result.add(entry.getKey(), entry.getValue());
        }
        active.remove(name); return result;
    }
    static String texture(String value, JsonObject textures) {
        Set<String> seen = new HashSet<>();
        while (value.startsWith("#")) { assertTrue(seen.add(value), "纹理变量循环 " + value); assertTrue(textures.has(value.substring(1)), "悬空纹理变量 " + value); value = textures.get(value.substring(1)).getAsString(); }
        return value;
    }
    static void validateModel(String name) throws Exception {
        var model = resolved(name, new HashSet<>()); var textures = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
        Set<String> refs = new HashSet<>();
        for (var entry : textures.entrySet()) refs.add(texture(entry.getValue().getAsString(), textures));
        if (model.has("elements")) for (var element : model.getAsJsonArray("elements")) for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) refs.add(texture(face.getValue().getAsJsonObject().get("texture").getAsString(), textures));
        for (String ref : refs) { var key = id(ref); assertNotNull(ImageIO.read(new ByteArrayInputStream(resource(key[0], "textures/" + key[1] + ".png"))), "PNG无法解码 " + ref); }
    }
    @Test void everyModelParentFaceParticleAndItemResolvesToRealPng() throws Exception {
        for (String name : IDS) {
            validateModel("create_nuclear_industry:item/" + name);
            var state = json("create_nuclear_industry", "blockstates/" + name + ".json");
            for (var application : applications(state)) validateModel(application.get("model").getAsString());
        }
        Path extras = ROOT.resolve("assets/create_nuclear_industry/models/block/industrial_fluid_visual_01");
        if (Files.exists(extras)) try (var paths = Files.walk(extras)) { for (Path path : paths.filter(p -> p.toString().endsWith(".json")).toList()) validateModel("create_nuclear_industry:block/industrial_fluid_visual_01/" + extras.relativize(path).toString().replace('\\', '/').replace(".json", "")); }
    }
    static List<JsonObject> applications(JsonObject state) {
        List<JsonObject> result = new ArrayList<>();
        if (state.has("variants")) for (var entry : state.getAsJsonObject("variants").entrySet()) result.add(entry.getValue().getAsJsonObject());
        if (state.has("multipart")) for (var entry : state.getAsJsonArray("multipart")) result.add(entry.getAsJsonObject().getAsJsonObject("apply"));
        return result;
    }
    static boolean matches(JsonObject condition, Map<String, String> state) {
        if (condition.has("OR")) return condition.getAsJsonArray("OR").asList().stream().anyMatch(c -> matches(c.getAsJsonObject(), state));
        if (condition.has("AND")) return condition.getAsJsonArray("AND").asList().stream().allMatch(c -> matches(c.getAsJsonObject(), state));
        return condition.entrySet().stream().allMatch(e -> Arrays.asList(e.getValue().getAsString().split("\\|" )).contains(state.get(e.getKey())));
    }
    static List<String> selected(JsonObject blockstate, Map<String, String> state) {
        List<String> models = new ArrayList<>();
        for (var entry : blockstate.getAsJsonArray("multipart")) { var part = entry.getAsJsonObject(); if (!part.has("when") || matches(part.getAsJsonObject("when"), state)) models.add(part.getAsJsonObject("apply").get("model").getAsString()); }
        return models;
    }
    @Test void pumpSixFacingsReuseCreateRotationsAndBody() throws Exception {
        var expected = json("create", "blockstates/mechanical_pump.json").getAsJsonObject("variants");
        var actual = json("create_nuclear_industry", "blockstates/industrial_fluid_pump.json").getAsJsonObject("variants");
        assertEquals(expected.keySet(), actual.keySet());
        for (String key : expected.keySet()) {
            for (String rotation : List.of("x", "y")) assertEquals(expected.getAsJsonObject(key).get(rotation), actual.getAsJsonObject(key).get(rotation));
            assertEquals(resolved("create:block/mechanical_pump/block", new HashSet<>()).get("elements"), resolved(actual.getAsJsonObject(key).get("model").getAsString(), new HashSet<>()).get("elements"));
        }
    }
    @Test void valveAxesKeepOriginalModelsWithoutUnsupportedStateProperties() throws Exception {
        for (String valve : List.of("reinforced_fluid_valve", "ceramic_fluid_valve")) {
            var actual = json("create_nuclear_industry", "blockstates/" + valve + ".json");
            for (String axis : List.of("x", "y", "z")) for (int mask = 0; mask < 64; mask++) {
                Map<String, String> state = new HashMap<>(); state.put("axis", axis);
                String[] dirs = {"down", "up", "north", "south", "west", "east"};
                for (int i = 0; i < 6; i++) state.put(dirs[i], Boolean.toString((mask & (1 << i)) != 0));
                var selected = selected(actual, state); assertEquals(1, selected.size());
                String original = "create:block/fluid_valve/" + (axis.equals("y") ? "block_horizontal_open" : "block_vertical_open");
                assertEquals(resolved(original, new HashSet<>()).get("elements"), resolved(selected.getFirst(), new HashSet<>()).get("elements"));
            }
        }
    }
    @Test void arrowTransformPointsAtEveryDisplayedDirection() throws Exception {
        Class<?> type = Class.forName("com.iksxh.create_nuclear_industry.industrial.client.IndustrialVisualOrientation");
        for (Direction direction : Direction.values()) {
            int x = (int) type.getMethod("xRotation", Direction.class).invoke(null, direction);
            int y = (int) type.getMethod("yRotation", Direction.class).invoke(null, direction);
            var vector = new org.joml.Matrix4f().rotateY((float)Math.toRadians(y)).rotateX((float)Math.toRadians(x)).transformDirection(new org.joml.Vector3f(0, 1, 0));
            assertEquals(direction.getStepX(), vector.x, 0.00001); assertEquals(direction.getStepY(), vector.y, 0.00001); assertEquals(direction.getStepZ(), vector.z, 0.00001);
        }
    }
    @Test void valveBodyWindowsUseTheRegisteredCutoutLayer() throws Exception {
        for (String tone : List.of("steel", "ceramic")) for (String name : List.of("block_vertical_open", "block_horizontal_open")) {
            var model = resolved("create_nuclear_industry:block/industrial_fluid_visual_01/" + tone + "/fluid_valve/" + name, new HashSet<>());
            assertTrue(model.has("render_type"), "透明阀窗口缺少cutout层");
            assertEquals("minecraft:cutout_mipped", model.get("render_type").getAsString(), "锁定NeoForge内建命名空间");
        }
    }
    @Test void valvePointerCoordinatesFaceTheActualTransparentWindowsAtBothEnds() throws Exception {
        var orientation = Class.forName("com.iksxh.create_nuclear_industry.industrial.client.IndustrialVisualOrientation");
        java.lang.reflect.Method yaw, tilt, angle;
        try {
            yaw = orientation.getMethod("valvePointerYaw", Direction.Axis.class);
            tilt = orientation.getMethod("valvePointerTilt");
            angle = orientation.getMethod("valvePointerAngle", Direction.Axis.class, int.class);
        } catch (NoSuchMethodException exception) { fail("缺少精确窗口面指针矩阵入口"); return; }
        var bodyTexture = ImageIO.read(new ByteArrayInputStream(resource("create", "textures/block/fluid_valve.png")));
        var pointer = resolved("create:block/fluid_valve/pointer", new HashSet<>()).getAsJsonArray("elements");
        for (Direction.Axis axis : Direction.Axis.values()) for (int opening : List.of(0, 100)) {
            var matrix = new org.joml.Matrix4f().translate(8, 8, 8)
                    .rotateY((float)Math.toRadians(((Number)yaw.invoke(null, axis)).floatValue()))
                    .rotateX((float)Math.toRadians(((Number)tilt.invoke(null)).floatValue()))
                    .rotateY((float)Math.toRadians(((Number)angle.invoke(null, axis, opening)).floatValue())).translate(-8, -8, -8);
            var bodyInverse = new org.joml.Matrix4f().translate(8,8,8).rotateY(axis == Direction.Axis.Z ? (float)Math.PI / 2 : 0).translate(-8,-8,-8);
            for (var element : pointer) {
                var from = element.getAsJsonObject().getAsJsonArray("from"); var to = element.getAsJsonObject().getAsJsonArray("to");
                // 取真实needle两个端点和中心；不是用被测矩阵生成预期窗口。
                for (float fraction : List.of(0f, .5f, 1f)) {
                    float x = (from.get(0).getAsFloat() + to.get(0).getAsFloat()) / 2;
                    float y = from.get(1).getAsFloat(); float z = from.get(2).getAsFloat() + fraction * (to.get(2).getAsFloat() - from.get(2).getAsFloat());
                    var world = matrix.transformPosition(new org.joml.Vector3f(x,y,z));
                    var local = bodyInverse.transformPosition(new org.joml.Vector3f(world));
                    assertTrue(Math.abs(local.z - 2.5f) < .001 || Math.abs(local.z - 13.5f) < .001, "指针必须在z=2.5/13.5窗口内侧，轴=" + axis);
                    // 原窗口主壳UV[1,1,7,7]（horizontal旋转90不改变圆孔）；其外表面z=2/14要真正透明。
                    int u = (int)((1 + (local.x - 2) / 2) * 2);
                    int v = (int)((1 + (14 - local.y) / 2) * 2);
                    assertEquals(0, (bodyTexture.getRGB(u,v) >>> 24), "指针端点投影被实心壳遮挡，轴=" + axis);
                }
            }
        }
    }
    @Test void bothTooltipFirstLinesUseExistingIconIndent() throws Exception {
        for (String name : List.of("IndustrialPumpBlockEntity", "IndustrialValveBlockEntity")) {
            String source = Files.readString(Path.of("src/main/java/com/iksxh/create_nuclear_industry/industrial/" + name + ".java"));
            assertTrue(source.substring(source.indexOf("boolean addToGoggleTooltip")).contains("tooltip.add(GoggleTooltip.indentFirstLine("), name + "首行缺Create图标避让");
        }
    }
    @Test void displayedOutputUsesActualPumpDirectionAndSuppressesStoppedAndOverstressed() throws Exception {
        Class<?> adapter;
        try { adapter = Class.forName("com.iksxh.create_nuclear_industry.industrial.client.IndustrialVisualOrientation"); }
        catch (ClassNotFoundException exception) { fail("缺少读取真实泵方向的只读视觉入口"); return; }
        var method = adapter.getMethod("displayedOutput", IndustrialPumpBlockEntity.class);
        var pump = new IndustrialPumpBlockEntity(BlockPos.ZERO, IndustrialFluidContent.PUMP.get().defaultBlockState());
        for (Direction facing : Direction.values()) {
            pump.setBlockState(pump.getBlockState().setValue(IndustrialPumpBlock.FACING, facing));
            pump.setSpeed(256); assertEquals(pump.outputDirection(), method.invoke(null, pump)); assertEquals(facing, method.invoke(null, pump));
            pump.setSpeed(-256); assertEquals(pump.outputDirection(), method.invoke(null, pump)); assertEquals(facing.getOpposite(), method.invoke(null, pump));
            pump.setSpeed(0); assertNull(method.invoke(null, pump));
            pump.setSpeed(256); var stress = com.simibubi.create.content.kinetics.base.KineticBlockEntity.class.getDeclaredField("overStressed"); stress.setAccessible(true); stress.setBoolean(pump, true); assertNull(method.invoke(null, pump)); stress.setBoolean(pump, false);
        }
    }
}
