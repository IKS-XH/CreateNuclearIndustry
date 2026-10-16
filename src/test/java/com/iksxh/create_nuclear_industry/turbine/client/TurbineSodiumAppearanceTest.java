package com.iksxh.create_nuclear_industry.turbine.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.iksxh.create_nuclear_industry.turbine.client.TurbineCasingAppearanceModelTest.*;
import static com.iksxh.create_nuclear_industry.turbine.client.TurbineWindowConnectedModelTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 加载真实 Sodium LevelSlice，检查生产 atlas 和轴向连窗只消费本次切片显示数据。
 * 只初始化有限局部数组；不创建客户端/GPU，不代表 Sodium/Iris 联机视觉验收。
 */
class TurbineSodiumAppearanceTest {
    private static final BlockPos A = new BlockPos(4, 4, 4);
    private static URLClassLoader loader;
    private static Class<?> sliceClass;

    @BeforeAll
    static void loadActualSodiumClass() throws Exception {
        String directory = System.getProperty("cni.sodium.fixtureDir");
        assumeTrue(directory != null, "真实 Sodium 检查需显式指定已核哈希的夹具目录");
        try (var files = Files.list(Path.of(directory))) {
            URL[] jars = files.filter(path -> path.toString().endsWith(".jar")).map(path -> {
                try { return path.toUri().toURL(); }
                catch (Exception exception) { throw new AssertionError(exception); }
            }).toArray(URL[]::new);
            loader = new URLClassLoader(jars, TurbineSodiumAppearanceTest.class.getClassLoader());
        }
        sliceClass = Class.forName("net.caffeinemc.mods.sodium.client.world.LevelSlice", true, loader);
        assertSame(loader, sliceClass.getClassLoader(), "必须使用真实提取类，不能用仿制切片");
    }

    @AfterAll
    static void closeActualClassLoader() throws Exception {
        if (loader != null) loader.close();
    }

    @Test
    void actualSliceSelectsMediumAndLargeAtlasTilesWithoutReadingBoundWorld() throws Exception {
        try (var sprites = new Sprites()) {
            var model = new TurbineCasingAppearanceModel(new FixtureModel(List.of(quad(sprites.panel, 0, 1))), sprites.entry);
            var slice = identitySlice(level(p -> { throw new AssertionError("atlas不得查询绑定世界"); }));
            // medium z=3 是上排第二格，large z=-1 是下排第三格；切片不需要方块数组。
            int[] diameters = {5, 7};
            BlockPos[] positions = {new BlockPos(9, 60, 3), new BlockPos(0, 60, -1)};
            float[] wantU = {.25f, .5f}, wantV = {0, .5f};
            org.junit.jupiter.api.Assertions.assertAll(java.util.stream.IntStream.range(0, diameters.length).mapToObj(i -> () -> {
                var state = state("turbine_casing", diameters[i], Direction.NORTH, 0, diameters[i] / 2);
                var data = model.getModelData(slice, positions[i], state, ModelData.EMPTY);
                var output = model.getQuads(state, null, RandomSource.create(1), data, null).getFirst();
                assertSame(sprites.target, output.getSprite());
                assertEquals(wantU[i], u(output, 0), 1e-6);
                assertEquals(wantV[i], v(output, 0), 1e-6);
            }));
        }
    }

    @Test
    void actualCurrentAndOldSlicesConnectOnlyTheirOwnLocalNeighbors() throws Exception {
        try (var sprites = new WindowSprites()) {
            var state = state("turbine_window", 5, Direction.NORTH, 0, 2);
            var raw = obj("d5_middle_x0_y2", Direction.NORTH, sprites);
            var model = new TurbineWindowConnectedModel(new FixtureModel(raw));
            ClientLevel current = level(p -> { throw new AssertionError("不能读当前世界替代切片"); });
            ClientLevel old = level(p -> { throw new AssertionError("不能读旧世界替代切片"); });
            var linked = readableSlice(current, Map.of(A, state, A.north(), state, A.south(), state));
            var isolated = readableSlice(old, Map.of(A, state));
            var oneEnd = readableSlice(old, Map.of(A, state, A.south(), state));
            var data = model.getModelData(linked, A, state, ModelData.EMPTY);
            var output = model.getQuads(state, null, RandomSource.create(1), data, null);
            assertEquals(12, output.size(), "真实切片双连接必须撤下两端实体框");
            for (var q : output) {
                assertEquals(0, axialRange(q, Direction.NORTH)[0], 1e-6);
                assertEquals(1, axialRange(q, Direction.NORTH)[1], 1e-6);
            }
            var fallback = model.getModelData(isolated, A, state, data);
            assertSame(raw, model.getQuads(state, null, RandomSource.create(1), fallback, null));
            var partial = model.getModelData(oneEnd, A, state, ModelData.EMPTY);
            assertEquals(21, model.getQuads(state, null, RandomSource.create(1), partial, null).size(),
                    "旧世界切片仍可按自身局部邻窗连接，不借用当前世界");
        }
    }

    @Test
    void nativeAndUnknownContextsKeepTheirBoundaries() throws Exception {
        var world = allocate(ClientLevel.class);
        var region = allocate(RenderChunkRegion.class);
        put(region, "level", world);
        assertTrue(TurbineCasingAppearanceModel.realWorld(world));
        assertTrue(TurbineCasingAppearanceModel.realWorld(region));
        assertTrue(TurbineCasingAppearanceModel.realWorld(identitySlice(world)));
        assertFalse(TurbineCasingAppearanceModel.realWorld(identitySlice(null)));
        assertFalse(TurbineCasingAppearanceModel.realWorld(unknownWorld()));
        assertFalse(TurbineCasingAppearanceModel.realWorld(null));
    }

    @Test
    void smallUnknownAndMissingMaterialKeepOriginalQuads() throws Exception {
        try (var sprites = new Sprites()) {
            var q = quad(sprites.panel, 0, 1);
            var model = new TurbineCasingAppearanceModel(new FixtureModel(List.of(q)), sprites.entry);
            var slice = identitySlice(allocate(ClientLevel.class));
            var small = state("turbine_casing", 3, Direction.NORTH, 0, 1);
            var data = model.getModelData(slice, A, small, ModelData.EMPTY);
            assertSame(q, model.getQuads(small, null, RandomSource.create(1), data, null).getFirst());
            var medium = state("turbine_casing", 5, Direction.NORTH, 0, 2);
            data = model.getModelData(unknownWorld(), A, medium, ModelData.EMPTY);
            assertSame(q, model.getQuads(medium, null, RandomSource.create(1), data, null).getFirst());
            data = model.getModelData(slice, A, medium, ModelData.EMPTY);
            sprites.entry.target = null;
            assertSame(q, model.getQuads(medium, null, RandomSource.create(1), data, null).getFirst());
            sprites.entry.target = sprites.endcap;
            assertSame(q, model.getQuads(medium, null, RandomSource.create(1), data, null).getFirst());
        }
    }

    /** 分配外部边界而不启动完整客户端；不替代其 getBlockState 实现。 */
    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static void put(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static BlockAndTintGetter identitySlice(ClientLevel world) throws Exception {
        Object slice = allocate(sliceClass);
        put(slice, "level", world);
        return (BlockAndTintGetter) slice;
    }

    /** 数组索引调用真实 Sodium 方法；生产邻格读取仍经过真实 LevelSlice。 */
    private static BlockAndTintGetter readableSlice(ClientLevel world, Map<BlockPos, BlockState> states) throws Exception {
        var slice = identitySlice(world);
        BlockState[][] blocks = new BlockState[27][4096];
        for (var section : blocks) Arrays.fill(section, Blocks.AIR.defaultBlockState());
        var sectionIndex = sliceClass.getMethod("getLocalSectionIndex", int.class, int.class, int.class);
        var blockIndex = sliceClass.getMethod("getLocalBlockIndex", int.class, int.class, int.class);
        int section = (int) sectionIndex.invoke(null, 0, 0, 0);
        for (var entry : states.entrySet()) {
            var pos = entry.getKey();
            blocks[section][(int) blockIndex.invoke(null, pos.getX(), pos.getY(), pos.getZ())] = entry.getValue();
        }
        put(slice, "blockArrays", blocks);
        put(slice, "volume", new BoundingBox(0, 0, 0, 15, 15, 15));
        return slice;
    }
}
