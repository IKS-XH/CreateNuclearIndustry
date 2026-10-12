package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.turbine.TurbineGeometry;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.mojang.blaze3d.platform.NativeImage;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/** 实际包装输出的UV、原quad保留与资源替换边界，不复写生产相位算法。 */
class TurbineCasingAppearanceModelTest {
    @Test void mediumUsesSecondUpperTileWithoutChangingOtherSpritesOrVertices() {
        try (var sprites = new Sprites()) {
            var panel = quad(sprites.panel, 0, 1);
            var end = quad(sprites.endcap, 0, 1);
            var input = List.of(panel, end);
            var model = new TurbineCasingAppearanceModel(new FixtureModel(input), sprites.entry);
            var state = state("turbine_casing", 5, Direction.NORTH, 0, 2);
            var data = model.getModelData(level(p -> state), new BlockPos(9, 60, 3), state, ModelData.EMPTY);
            var output = model.getQuads(state, null, RandomSource.create(), data, null);
            assertSame(sprites.target, output.getFirst().getSprite());
            assertEquals(.25f, u(output.getFirst(), 0), 1e-6);
            assertEquals(.5f, u(output.getFirst(), 1), 1e-6);
            assertEquals(.5f, v(output.getFirst(), 2), 1e-6);
            assertSame(end, output.get(1));
            assertEquals(0f, u(panel, 0));
            assertNotSame(panel.getVertices(), output.getFirst().getVertices());
            for (int i = 0; i < 4; i++) for (int k : new int[]{0, 1, 2, 3, 6, 7})
                assertEquals(panel.getVertices()[8 * i + k], output.getFirst().getVertices()[8 * i + k]);
            assertEquals(panel.hasAmbientOcclusion(), output.getFirst().hasAmbientOcclusion());
            assertEquals(panel.isShade(), output.getFirst().isShade());
        }
    }

    @Test void largeNegativeWorldPositionUsesThirdLowerTile() {
        try (var sprites = new Sprites()) {
            var model = new TurbineCasingAppearanceModel(new FixtureModel(List.of(quad(sprites.panel, 0, 1))), sprites.entry);
            var state = state("turbine_casing", 7, Direction.NORTH, 0, 3);
            var data = model.getModelData(level(p -> state), new BlockPos(0, 60, -1), state, ModelData.EMPTY);
            var q = model.getQuads(state, null, RandomSource.create(), data, null).getFirst();
            assertSame(sprites.target, q.getSprite());
            assertEquals(.5f, u(q, 0), 1e-6);
            assertEquals(.75f, u(q, 1), 1e-6);
            assertEquals(.5f, v(q, 0), 1e-6);
            assertEquals(1f, v(q, 2), 1e-6);
        }
    }

    @Test void smallMissingDataAndUnknownWorldKeepTheExactOriginalQuad() {
        try (var sprites = new Sprites()) {
            var q = quad(sprites.panel, 0, 1);
            var model = new TurbineCasingAppearanceModel(new FixtureModel(List.of(q)), sprites.entry);
            var small = state("turbine_casing", 3, Direction.NORTH, 0, 1);
            var data = model.getModelData(level(p -> small), BlockPos.ZERO, small, ModelData.EMPTY);
            assertSame(q, model.getQuads(small, null, RandomSource.create(), data, null).getFirst());
            var medium = state("turbine_casing", 5, Direction.NORTH, 0, 2);
            assertSame(q, model.getQuads(medium, null, RandomSource.create(), ModelData.EMPTY, null).getFirst());
            var unknown = model.getModelData(unknownWorld(), BlockPos.ZERO, medium, ModelData.EMPTY);
            assertSame(q, model.getQuads(medium, null, RandomSource.create(), unknown, null).getFirst());
            assertSame(q, model.getQuads(null, null, RandomSource.create()).getFirst());
        }
    }

    @Test void fourFacingsAndReloadUseCurrentEntryInsteadOfAnOldAtlasObject() {
        try (var sprites = new Sprites()) {
            var model = new TurbineCasingAppearanceModel(new FixtureModel(List.of(quad(sprites.panel, 0, 1))), sprites.entry);
            var pos = new BlockPos(4, 60, 3);
            int[] want = {0, 2, 0, 1};
            Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            for (int i = 0; i < directions.length; i++) {
                var state = state("turbine_casing", 7, directions[i], 0, 3);
                var data = model.getModelData(level(p -> state), pos, state, ModelData.EMPTY);
                var q = model.getQuads(state, null, RandomSource.create(), data, null).getFirst();
                assertEquals(want[i] / 4f, u(q, 0), 1e-6);
            }
            sprites.entry.target = sprites.reloaded;
            var state = state("turbine_casing", 5, Direction.NORTH, 0, 2);
            var data = model.getModelData(level(p -> state), pos, state, ModelData.EMPTY);
            assertSame(sprites.reloaded, model.getQuads(state, null, RandomSource.create(), data, null).getFirst().getSprite());
        }
    }

    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", path); }

    static BlockState state(String block, int diameter, Direction facing, int x, int y) {
        var b = BuiltInRegistries.BLOCK.get(id(block));
        assertEquals(id(block), BuiltInRegistries.BLOCK.getKey(b));
        return b.defaultBlockState().setValue(TurbinePartBlock.LOCATED, true)
                .setValue(TurbinePartBlock.FORMED, true).setValue(TurbinePartBlock.MACHINE_FACING, facing)
                .setValue(TurbinePartBlock.PIECE, TurbineGeometry.pieceId(diameter, TurbineGeometry.Section.MIDDLE, x, y));
    }

    static BakedQuad quad(TextureAtlasSprite sprite, float low, float high) {
        int[] vertices = new int[32];
        for (int i = 0; i < 4; i++) {
            vertices[i * 8] = Float.floatToRawIntBits(i == 1 || i == 2 ? 1 : 0);
            vertices[i * 8 + 1] = Float.floatToRawIntBits(1);
            vertices[i * 8 + 2] = Float.floatToRawIntBits(i > 1 ? 1 : 0);
            vertices[i * 8 + 3] = -1;
            vertices[i * 8 + 4] = Float.floatToRawIntBits(sprite.getU(i == 1 || i == 2 ? high : low));
            vertices[i * 8 + 5] = Float.floatToRawIntBits(sprite.getV(i > 1 ? high : low));
        }
        return new BakedQuad(vertices, -1, Direction.UP, sprite, true, false);
    }

    static float u(BakedQuad q, int vertex) { return q.getSprite().getUOffset(Float.intBitsToFloat(q.getVertices()[vertex * 8 + 4])); }
    static float v(BakedQuad q, int vertex) { return q.getSprite().getVOffset(Float.intBitsToFloat(q.getVertices()[vertex * 8 + 5])); }

    /** 仅替代外部已加载世界的两个邻格读取；不伪造连接判断或几何输出。 */
    static final class FixtureLevel extends ClientLevel {
        Function<BlockPos, BlockState> reader;
        private FixtureLevel() { super(null, null, null, null, 0, 0, null, null, false, 0L); throw new AssertionError(); }
        @Override public BlockState getBlockState(BlockPos pos) { return reader.apply(pos); }
    }

    static FixtureLevel level(Function<BlockPos, BlockState> reader) {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
            var level = (FixtureLevel) ((Unsafe) f.get(null)).allocateInstance(FixtureLevel.class);
            level.reader = reader;
            return level;
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    static BlockAndTintGetter unknownWorld() {
        return (BlockAndTintGetter) Proxy.newProxyInstance(BlockAndTintGetter.class.getClassLoader(), new Class<?>[]{BlockAndTintGetter.class},
                (proxy, method, args) -> { throw new AssertionError("未知/Ponder上下文不得读世界"); });
    }

    /** 原模型夹具只提供不可变原quad，全部转换由生产包装执行。 */
    static class FixtureModel implements BakedModel {
        final List<BakedQuad> quads;
        FixtureModel(List<BakedQuad> quads) { this.quads = quads; }
        @Override public List<BakedQuad> getQuads(BlockState s, Direction side, RandomSource r) { return quads; }
        @Override public boolean useAmbientOcclusion() { return true; }
        @Override public boolean isGui3d() { return true; }
        @Override public boolean usesBlockLight() { return true; }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return quads.getFirst().getSprite(); }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }

    static final class TestSprite extends TextureAtlasSprite {
        TestSprite(ResourceLocation id, int width, int height) {
            super(ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png"),
                    new SpriteContents(id, new FrameSize(width, height), new NativeImage(width, height, false), ResourceMetadata.EMPTY), width, height, 0, 0);
        }
    }

    /** atlas获取边界可切换，真实TextureAtlasSprite负责归一化UV；不替代生产转换公式。 */
    static final class Entry extends SpriteShiftEntry {
        final TextureAtlasSprite original;
        TextureAtlasSprite target;
        Entry(TextureAtlasSprite original, TextureAtlasSprite target) { this.original = original; this.target = target; }
        @Override public TextureAtlasSprite getOriginal() { return original; }
        @Override public TextureAtlasSprite getTarget() { return target; }
    }

    static final class Sprites implements AutoCloseable {
        final TestSprite panel = new TestSprite(id("block/turbine/turbine_casing_07_panel"), 16, 16);
        final TestSprite endcap = new TestSprite(id("block/turbine/turbine_casing_07_endcap"), 128, 256);
        final TestSprite target = new TestSprite(id("block/turbine/turbine_casing_07_sparse"), 64, 32);
        final TestSprite reloaded = new TestSprite(id("block/turbine/turbine_casing_07_sparse"), 64, 32);
        final Entry entry = new Entry(panel, target);
        @Override public void close() { for (var s : List.of(panel, endcap, target, reloaded)) s.contents().close(); }
    }
}
