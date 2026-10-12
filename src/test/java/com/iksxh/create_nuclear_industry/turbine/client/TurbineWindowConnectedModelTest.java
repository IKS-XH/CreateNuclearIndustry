package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import static com.iksxh.create_nuclear_industry.turbine.client.TurbineCasingAppearanceModelTest.*;
import static org.junit.jupiter.api.Assertions.*;

/** 原OBJ实际quad进入生产包装，检查轴向邻接、完整撤框和共享玻璃端面。 */
class TurbineWindowConnectedModelTest {
    @Test void twoSharedEndsRemoveWholeBarsAndExtendGlassWithOriginalThickness() throws Exception {
        try (var sprites = new WindowSprites()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                var state = state("turbine_window", 5, facing, 0, 2);
                var original = obj("d5_middle_x0_y2", facing, sprites);
                var copy = original.stream().map(q -> q.getVertices().clone()).toList();
                var model = new TurbineWindowConnectedModel(new FixtureModel(original));
                var data = model.getModelData(level(p -> state), BlockPos.ZERO, state, ModelData.EMPTY);
                var output = model.getQuads(state, null, RandomSource.create(), data, null);
                assertEquals(12, output.size(), "双连接只保留长框和连续玻璃");
                assertEquals(4, output.stream().filter(q -> q.getSprite() == sprites.glass).count());
                for (var q : output) {
                    float[] range = axialRange(q, facing);
                    assertEquals(0, range[0], 1e-6);
                    assertEquals(1, range[1], 1e-6);
                    if (q.getSprite() == sprites.glass)
                        for (int i = 0; i < 4; i++) {
                            float y = Float.intBitsToFloat(q.getVertices()[8 * i + 1]);
                            assertTrue(Math.abs(y - .96) < 1e-6 || Math.abs(y - .92) < 1e-6);
                            assertEquals(0x87FFFFFF, q.getVertices()[8 * i + 3]);
                        }
                }
                assertGlassSampling(output, sprites, 1);
                for (int i = 0; i < original.size(); i++) assertArrayEquals(copy.get(i), original.get(i).getVertices());
                assertEquals(30, original.size());
            }
            saveMeshEvidence(sprites);
        }
    }

    @Test void singleConnectedEndKeepsOuterEndAndAllFourRealWindowPlanes() throws Exception {
        try (var sprites = new WindowSprites()) {
            for (int[] xy : new int[][]{{0, 2}, {0, -2}, {2, 0}, {-2, 0}}) {
                var state = state("turbine_window", 5, Direction.NORTH, xy[0], xy[1]);
                var pos = new BlockPos(10, 60, 30);
                var model = new TurbineWindowConnectedModel(new FixtureModel(obj("d5_middle_x" + xy[0] + "_y" + xy[1], Direction.NORTH, sprites)));
                var data = model.getModelData(level(p -> p.equals(pos.south()) ? state : Blocks.AIR.defaultBlockState()), pos, state, ModelData.EMPTY);
                var output = model.getQuads(state, null, RandomSource.create(), data, null);
                assertEquals(21, output.size());
                assertTrue(output.stream().anyMatch(q -> axialRange(q, Direction.NORTH)[1] <= .140001));
                assertFalse(output.stream().anyMatch(q -> axialRange(q, Direction.NORTH)[0] >= .859999));
                var glasses = output.stream().filter(q -> q.getSprite() == sprites.glass).toList();
                assertEquals(5, glasses.size());
                assertGlassSampling(output, sprites, xy[1] != 0 ? 1 : 0);
                assertTrue(glasses.stream().anyMatch(q -> Math.abs(axialRange(q, Direction.NORTH)[0] - .14) < 1e-6 && Math.abs(axialRange(q, Direction.NORTH)[1] - 1) < 1e-6));
            }
        }
    }

    @Test void invalidNeighborsAndEndcapsNeverJoin() throws Exception {
        try (var sprites = new WindowSprites()) {
            var state = state("turbine_window", 5, Direction.NORTH, 0, 2);
            var raw = obj("d5_middle_x0_y2", Direction.NORTH, sprites);
            var model = new TurbineWindowConnectedModel(new FixtureModel(raw));
            List<BlockState> rejected = List.of(
                    state("turbine_casing", 5, Direction.NORTH, 0, 2),
                    state("turbine_window", 7, Direction.NORTH, 0, 3),
                    state("turbine_window", 5, Direction.EAST, 0, 2),
                    state("turbine_window", 5, Direction.NORTH, 2, 0),
                    state.setValue(TurbinePartBlock.LOCATED, false),
                    state.setValue(TurbinePartBlock.FORMED, false),
                    state.setValue(TurbinePartBlock.PIECE, 0),
                    state.setValue(TurbinePartBlock.PIECE, com.iksxh.create_nuclear_industry.turbine.TurbineGeometry.pieceId(5, com.iksxh.create_nuclear_industry.turbine.TurbineGeometry.Section.FRONT, 0, 2)));
            for (var neighbor : rejected) {
                var data = model.getModelData(level(p -> neighbor), BlockPos.ZERO, state, ModelData.EMPTY);
                assertSame(raw, model.getQuads(state, null, RandomSource.create(), data, null));
            }
            var data = model.getModelData(level(p -> p.equals(BlockPos.ZERO.east()) ? state : Blocks.AIR.defaultBlockState()), BlockPos.ZERO, state, ModelData.EMPTY);
            assertSame(raw, model.getQuads(state, null, RandomSource.create(), data, null));
        }
    }

    @Test void missingDataUnknownContextAndIndependentWindowKeepOriginal() throws Exception {
        try (var sprites = new WindowSprites()) {
            var state = state("turbine_window", 5, Direction.NORTH, 0, 2);
            var raw = obj("d5_middle_x0_y2", Direction.NORTH, sprites);
            var model = new TurbineWindowConnectedModel(new FixtureModel(raw));
            assertSame(raw, model.getQuads(state, null, RandomSource.create(), ModelData.EMPTY, null));
            var data = model.getModelData(unknownWorld(), BlockPos.ZERO, state, ModelData.EMPTY);
            assertSame(raw, model.getQuads(state, null, RandomSource.create(), data, null));
            var loose = state.setValue(TurbinePartBlock.LOCATED, false);
            data = model.getModelData(level(p -> loose), BlockPos.ZERO, loose, ModelData.EMPTY);
            assertSame(raw, model.getQuads(loose, null, RandomSource.create(), data, null));
            assertSame(raw, model.getQuads(null, null, RandomSource.create()));
        }
    }

    /** 用实际四顶点的恒定深度识别两张大面，不依赖OBJ共用的sprite或错误边面法向。 */
    private static void assertGlassSampling(List<BakedQuad> quads, WindowSprites sprites, int depthIndex) {
        int largeFaces = 0;
        for (var q : quads) {
            if (q.getSprite() != sprites.glass) continue;
            float depth = Float.intBitsToFloat(q.getVertices()[depthIndex]);
            boolean plane = true;
            for (int i = 1; i < 4; i++)
                plane &= Math.abs(Float.intBitsToFloat(q.getVertices()[8 * i + depthIndex]) - depth) < 1e-6;
            if (!plane) {
                // 幸存玻璃边面仍保留原全幅UV，不能随两张大面一起裁采样。
                assertTrue(java.util.stream.IntStream.range(0, 4).anyMatch(i -> Math.abs(u(q, i)) < 1e-6 || Math.abs(u(q, i) - 1) < 1e-6));
                continue;
            }
            largeFaces++;
            float minU = 1, maxU = 0, minV = 1, maxV = 0;
            for (int i = 0; i < 4; i++) {
                minU = Math.min(minU, u(q, i)); maxU = Math.max(maxU, u(q, i));
                minV = Math.min(minV, v(q, i)); maxV = Math.max(maxV, v(q, i));
            }
            assertEquals(4.5 / 16, minU, 1e-6);
            assertEquals(11.5 / 16, maxU, 1e-6);
            assertEquals(3.5 / 16, minV, 1e-6);
            assertEquals(11.5 / 16, maxV, 1e-6);
        }
        assertEquals(2, largeFaces, "只裁采样前后两张玻璃大面");
    }

    /** 夹具只解码现有OBJ和原生模型旋转；不实现连接、撤框、扩玻璃或相位算法。 */
    static List<BakedQuad> obj(String name, Direction facing, WindowSprites sprites) throws Exception {
        var vertices = new ArrayList<float[]>(); var uv = new ArrayList<float[]>(); var normals = new ArrayList<float[]>();
        var quads = new ArrayList<BakedQuad>(); String material = "";
        int yaw = switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
        var rotation = BlockModelRotation.by(0, yaw).getRotation().getMatrix();
        for (String line : Files.readAllLines(Path.of("src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/window_" + name + ".obj"))) {
            String[] fields = line.split(" ");
            switch (fields[0]) {
                case "v", "vt", "vn" -> {
                    float[] values = new float[fields.length - 1];
                    for (int i = 1; i < fields.length; i++) values[i - 1] = Float.parseFloat(fields[i]);
                    (fields[0].equals("v") ? vertices : fields[0].equals("vt") ? uv : normals).add(values);
                }
                case "usemtl" -> material = fields[1];
                case "f" -> {
                    boolean glass = material.startsWith("glass");
                    var sprite = glass ? sprites.glass : sprites.steel;
                    int[] data = new int[32]; Direction normal = null;
                    for (int i = 0; i < 4; i++) {
                        String[] indices = fields[i + 1].split("/");
                        float[] v = vertices.get(Integer.parseInt(indices[0]) - 1);
                        var point = new Vector4f(v[0] - .5f, v[1] - .5f, v[2] - .5f, 1).mul(rotation);
                        data[i * 8] = Float.floatToRawIntBits(point.x + .5f);
                        data[i * 8 + 1] = Float.floatToRawIntBits(point.y + .5f);
                        data[i * 8 + 2] = Float.floatToRawIntBits(point.z + .5f);
                        data[i * 8 + 3] = glass ? 0x87FFFFFF : -1;
                        float[] texture = uv.get(Integer.parseInt(indices[1]) - 1);
                        data[i * 8 + 4] = Float.floatToRawIntBits(sprite.getU(texture[0]));
                        data[i * 8 + 5] = Float.floatToRawIntBits(sprite.getV(texture[1]));
                        float[] n = normals.get(Integer.parseInt(indices[2]) - 1);
                        var vector = new Vector4f(n[0], n[1], n[2], 0).mul(rotation);
                        normal = Direction.getNearest(vector.x, vector.y, vector.z);
                        data[i * 8 + 7] = (Math.round(vector.x * 127) & 255)
                                | (Math.round(vector.y * 127) & 255) << 8 | (Math.round(vector.z * 127) & 255) << 16;
                    }
                    quads.add(new BakedQuad(data, -1, normal, sprite, true, true));
                }
                default -> { }
            }
        }
        return List.copyOf(quads);
    }

    static float[] axialRange(BakedQuad q, Direction facing) {
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        var axis = facing.getOpposite();
        for (int i = 0; i < 4; i++) {
            float x = Float.intBitsToFloat(q.getVertices()[8 * i]) - .5f;
            float z = Float.intBitsToFloat(q.getVertices()[8 * i + 2]) - .5f;
            float value = x * axis.getStepX() + z * axis.getStepZ() + .5f;
            min = Math.min(min, value); max = Math.max(max, value);
        }
        return new float[]{min, max};
    }

    /** 离线预览只消费这里实际生产输出；不在Python端另算撤框、玻璃扩展或连接判断。 */
    private static void saveMeshEvidence(WindowSprites sprites) throws Exception {
        var state = state("turbine_window", 5, Direction.NORTH, 0, 2);
        var input = obj("d5_middle_x0_y2", Direction.NORTH, sprites);
        var scenes = new ArrayList<Object>();
        for (int length : new int[]{1, 3}) {
            var states = new java.util.HashMap<BlockPos, BlockState>();
            for (int z = 0; z < length; z++) states.put(new BlockPos(0, 0, z), state);
            var world = level(p -> states.getOrDefault(p, Blocks.AIR.defaultBlockState()));
            var model = new TurbineWindowConnectedModel(new FixtureModel(input));
            var cells = new ArrayList<Object>();
            for (int z = 0; z < length; z++) {
                var data = model.getModelData(world, new BlockPos(0, 0, z), state, ModelData.EMPTY);
                var output = model.getQuads(state, null, RandomSource.create(), data, null);
                assertEquals(length == 1 ? 30 : z == 1 ? 12 : 21, output.size());
                var faces = new ArrayList<Object>();
                for (var q : output) {
                    var vertices = new ArrayList<Object>();
                    for (int i = 0; i < 4; i++) {
                        int[] raw = q.getVertices(); int index = i * 8;
                        vertices.add(Map.of("position", List.of(Float.intBitsToFloat(raw[index]), Float.intBitsToFloat(raw[index + 1]), Float.intBitsToFloat(raw[index + 2])),
                                "uv", List.of(u(q, i), v(q, i)), "color_abgr", Integer.toUnsignedLong(raw[index + 3]),
                                "light", raw[index + 6], "normal_packed", raw[index + 7]));
                    }
                    faces.add(Map.of("sprite", q.getSprite().contents().name().toString(), "direction", q.getDirection().name(),
                            "shade", q.isShade(), "ambient_occlusion", q.hasAmbientOcclusion(), "vertices", vertices));
                }
                cells.add(Map.of("offset", List.of(0, 0, z), "quad_count", output.size(), "faces", faces));
            }
            scenes.add(Map.of("name", length == 1 ? "single" : "three_connected", "cells", cells));
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("source_obj", "src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/window_d5_middle_x0_y2.obj");
        result.put("basis", "原OBJ解码及原生BakedQuad/BlockModelRotation进入生产getModelData/getQuads；离线几何证据，不是客户端完整OBJ bake/GPU证明");
        result.put("material_mtl", "src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/turbine.mtl");
        result.put("glass_png", "src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_window_glass.png");
        result.put("glass_vertex_alpha", 135); result.put("glass_png_alpha", "原PNG非零区域bbox(4,3)-(12,12)，最大136；生产仅连接大面UV裁采样至有色texel中心，边面/单窗保持全幅；预览使用这里的实际UV");
        result.put("scenes", scenes);
        Path path = Path.of("build/reports/art/ART-TURBINE-07B-R2/consumer/window-mesh-glass-uv.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(result) + "\n");
    }

    static final class WindowSprites implements AutoCloseable {
        final TestSprite steel = new TestSprite(id("block/turbine/turbine_casing_panel"), 16, 16);
        final TestSprite glass = new TestSprite(id("block/turbine/turbine_window_glass"), 16, 16);
        @Override public void close() { steel.contents().close(); glass.contents().close(); }
    }
}
