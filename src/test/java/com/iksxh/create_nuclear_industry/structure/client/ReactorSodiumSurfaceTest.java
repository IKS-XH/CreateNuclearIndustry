package com.iksxh.create_nuclear_industry.structure.client;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 从只读提取的真实 Sodium JAR 加载 LevelSlice，贯穿生产快照、Create CT gather 和窗口共享面消费。
 * 仅初始化切片内存与世界身份，不创建区块或客户端；不代表完整 Sodium/Iris GPU 或联机验收。
 * 可选夹具目录由 cni.sodium.fixtureDir 指定，普通无 Sodium 构建不依赖或打包这些第三方类。
 */
class ReactorSodiumSurfaceTest {
    private static final String SLICE_CLASS = "net.caffeinemc.mods.sodium.client.world.LevelSlice";
    private static final BlockPos A = new BlockPos(4, 4, 4);
    private static final BlockPos B = A.east();
    private static URLClassLoader loader;
    private static Class<?> sliceClass;

    @BeforeAll
    static void loadActualSodiumClass() throws Exception {
        String directory = System.getProperty("cni.sodium.fixtureDir");
        assumeTrue(directory != null, "本批真实 Sodium 检查需显式指定已核哈希的夹具目录");
        try (var files = Files.list(Path.of(directory))) {
            URL[] jars = files.filter(path -> path.toString().endsWith(".jar"))
                    .map(path -> {
                        try { return path.toUri().toURL(); }
                        catch (Exception exception) { throw new AssertionError(exception); }
                    }).toArray(URL[]::new);
            loader = new URLClassLoader(jars, ReactorSodiumSurfaceTest.class.getClassLoader());
        }
        sliceClass = Class.forName(SLICE_CLASS, true, loader);
        assertSame(loader, sliceClass.getClassLoader(), "必须使用提取的真实类，不能用测试仿制类替代");
    }

    @AfterAll
    static void closeActualClassLoader() throws Exception {
        if (loader != null) loader.close();
    }

    @AfterEach
    void clearPublication() {
        ReactorSurfaceSnapshots.publish(null, ReactorSurfaceSnapshot.empty());
    }

    @Test
    void actualSliceCapturesOnlyItsOwnPublishedWorldWithoutReadingWorldData() throws Exception {
        ClientLevel current = allocate(ClientLevel.class);
        ClientLevel old = allocate(ClientLevel.class);
        var expected = snapshot("reactor_window");
        // 故意不初始化 blockArrays/volume；capture 若读取区块或方块就会失败。
        BlockAndTintGetter slice = identitySlice(current);
        BlockAndTintGetter stale = identitySlice(old);
        ReactorSurfaceSnapshots.publish(current, expected);
        assertSame(expected, ReactorSurfaceSnapshots.capture(slice));
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorSurfaceSnapshots.capture(stale));
        ReactorSurfaceSnapshots.publish(old, expected);
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorSurfaceSnapshots.capture(slice));
        assertSame(expected, ReactorSurfaceSnapshots.capture(stale));
        ReactorSurfaceSnapshots.publish(null, ReactorSurfaceSnapshot.empty());
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorSurfaceSnapshots.capture(stale));
    }

    @Test
    void actualSliceFeedsNativeCreateContextAndFinalModelDataForConnectedWindows() throws Exception {
        ClientLevel world = allocate(ClientLevel.class);
        BlockState window = registeredState("reactor_window");
        BlockAndTintGetter slice = readableSlice(world, window);
        ReactorSurfaceSnapshots.publish(world, snapshot("reactor_window"));
        var behaviour = new ReactorConnectedTextureBehaviour(id -> null);
        ReactorConnectedTextures.withCapturedSnapshot(slice, () -> {
            assertSame(AllCTTypes.OMNIDIRECTIONAL, behaviour.getDataType(slice, A, window, Direction.NORTH));
            var context = behaviour.buildContext(slice, A, window, Direction.NORTH,
                    AllCTTypes.OMNIDIRECTIONAL.getContextRequirement());
            assertTrue(context.left, "北面东侧邻窗是 Create 原生 left 方向");
            assertFalse(context.right);
            return null;
        });
        BakedModel model = ReactorConnectedTextures.wrapModel(new SixFaceModel(), behaviour);
        ModelData data = model.getModelData(slice, A, window, ModelData.EMPTY);
        assertTrue(model.getQuads(window, Direction.EAST, RandomSource.create(1), data, null).isEmpty(),
                "final getModelData 必须将同结构相邻窗口共享面写入本次模型数据");
        assertEquals(1, model.getQuads(window, Direction.WEST, RandomSource.create(1), data, null).size());
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorConnectedTextures.currentSnapshot(slice));
        // 同坐标旧世界切片不能借用当前信封；回退模型必须保留原共享面。
        BlockAndTintGetter stale = readableSlice(allocate(ClientLevel.class), window);
        ModelData fallback = model.getModelData(stale, A, window, ModelData.EMPTY);
        assertEquals(1, model.getQuads(window, Direction.EAST, RandomSource.create(1), fallback, null).size());
    }

    @Test
    void nativeRegionAndUnknownWrapperKeepTheirExistingIsolation() throws Exception {
        ClientLevel world = allocate(ClientLevel.class);
        var expected = snapshot("reactor_window");
        ReactorSurfaceSnapshots.publish(world, expected);
        RenderChunkRegion nativeRegion = allocate(RenderChunkRegion.class);
        put(nativeRegion, "level", world);
        assertSame(expected, ReactorSurfaceSnapshots.capture(nativeRegion));
        assertSame(expected, ReactorSurfaceSnapshots.capture(world));
        // 包装器即使暴露当前世界的光照接口，也不能据此取得全局快照；Ponder 同样走未知上下文降级。
        BlockAndTintGetter unknown = (BlockAndTintGetter) Proxy.newProxyInstance(
                BlockAndTintGetter.class.getClassLoader(), new Class<?>[]{BlockAndTintGetter.class},
                (proxy, method, args) -> { throw new AssertionError("快照入口不得调用包装器：" + method.getName()); });
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorSurfaceSnapshots.capture(unknown));
        assertSame(ReactorSurfaceSnapshot.empty(), ReactorSurfaceSnapshots.capture(null));
    }

    /** 分配外部对象而不执行需要完整游戏启动的构造器；仅用于测试边界。 */
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

    /** 实际切片 getBlockState 读取有限局部数组；数组索引由真实 Sodium 方法计算，不复制其算法。 */
    private static BlockAndTintGetter readableSlice(ClientLevel world, BlockState window) throws Exception {
        BlockAndTintGetter slice = identitySlice(world);
        BlockState[][] blocks = new BlockState[27][4096];
        for (BlockState[] section : blocks) Arrays.fill(section, Blocks.AIR.defaultBlockState());
        var sectionIndex = sliceClass.getMethod("getLocalSectionIndex", int.class, int.class, int.class);
        var blockIndex = sliceClass.getMethod("getLocalBlockIndex", int.class, int.class, int.class);
        int section = (int) sectionIndex.invoke(null, 0, 0, 0);
        for (BlockPos pos : List.of(A, B)) {
            blocks[section][(int) blockIndex.invoke(null, pos.getX(), pos.getY(), pos.getZ())] = window;
        }
        put(slice, "blockArrays", blocks);
        put(slice, "volume", new BoundingBox(0, 0, 0, 15, 15, 15));
        return slice;
    }

    private static BlockState registeredState(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", name);
        var block = BuiltInRegistries.BLOCK.get(id);
        assertEquals(id, BuiltInRegistries.BLOCK.getKey(block));
        return block.defaultBlockState();
    }

    private static ReactorSurfaceSnapshot snapshot(String block) {
        String id = "create_nuclear_industry:" + block;
        UUID generation = UUID.fromString("00000000-0000-0000-0000-000000000123");
        BlockPos origin = new BlockPos(3, 3, 4), max = new BlockPos(7, 7, 8), owner = new BlockPos(3, 4, 5);
        return new ReactorSurfaceSnapshot(Map.of(
                A, new ReactorSurfaceSnapshot.Member("minecraft:overworld", owner, generation, 1, origin, max, id, A, Set.of(Direction.NORTH)),
                B, new ReactorSurfaceSnapshot.Member("minecraft:overworld", owner, generation, 1, origin, max, id, B, Set.of(Direction.NORTH))));
    }

    /** 无 sprite 的原始六面夹具只观察共享面删除，不模拟 Create 的连接算法。 */
    private static final class SixFaceModel implements BakedModel {
        private final List<BakedQuad> quads = Arrays.stream(Direction.values())
                .map(face -> new BakedQuad(new int[32], -1, face, null, true)).toList();
        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
            return side == null ? quads : quads.stream().filter(quad -> quad.getDirection() == side).toList();
        }
        @Override public boolean useAmbientOcclusion() { return false; }
        @Override public boolean isGui3d() { return true; }
        @Override public boolean usesBlockLight() { return true; }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return null; }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }
}
