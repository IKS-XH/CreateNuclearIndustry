package com.iksxh.create_nuclear_industry.radiation.client;

import com.iksxh.create_nuclear_industry.content.RadiationContent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * 保留20B入口光照、override和display链，只为本机实体四手持上下文替换无针body并叠加独立针。
 * 所有实体判断都在resolve运行时完成；不在共享模型记录当前玩家，也不重建104元素的静态quad。
 */
public final class RadiationMeterBakedModel extends BakedModelWrapper<BakedModel> {
    public static final double PIVOT_X_PIXELS = 7.78125;
    public static final double PIVOT_Y_PIXELS = 9.40625;
    public static final double PIVOT_Z_PIXELS = 5.8125;
    public static final int ROTATION_SIGN = 1;
    private final BakedModel body;
    private final BakedModel needle;
    private final DoubleSupplier angle;
    private final ItemOverrides overrides;

    public RadiationMeterBakedModel(BakedModel original, BakedModel body, BakedModel needle, DoubleSupplier angle) {
        super(original);
        this.body = body;
        this.needle = needle;
        this.angle = angle;
        overrides = new MeterOverrides();
    }

    @Override public ItemOverrides getOverrides() { return overrides; }

    private final class MeterOverrides extends ItemOverrides {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            BakedModel resolved = originalModel.getOverrides().resolve(originalModel, stack, level, entity, seed);
            if (resolved == null) resolved = originalModel;
            Minecraft minecraft = Minecraft.getInstance();
            boolean owner = stack.is(RadiationContent.METER.get()) && entity != null && entity == minecraft.player
                    && level != null && level == minecraft.level;
            return new ContextModel(resolved, owner);
        }
    }

    private final class ContextModel extends BakedModelWrapper<BakedModel> {
        private final boolean owner;
        private ContextModel(BakedModel original, boolean owner) { super(original); this.owner = owner; }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poses, boolean leftHand) {
            // 先让SeparateTransforms选GUI/held并应用原display一次，再选择手持动画载荷。
            BakedModel transformed = originalModel.applyTransform(context, poses, leftHand);
            boolean hand = switch (context) {
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> true;
                default -> false;
            };
            return owner && hand ? new AnimatedPasses(body, new RotatingNeedle(needle, angle.getAsDouble())) : transformed;
        }
    }

    private static final class AnimatedPasses extends BakedModelWrapper<BakedModel> {
        private final BakedModel needle;
        private final List<BakedModel> passes;
        private AnimatedPasses(BakedModel body, BakedModel needle) {
            super(body); this.needle = needle; passes = List.of(body, needle);
        }
        // 原生ItemRenderer逐pass调用；不能转发父级pass列表而丢掉针件。
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return passes; }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return new QuadView(originalModel.getQuads(state, side, random), needle.getQuads(state, side, random));
        }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data, @Nullable RenderType type) {
            return new QuadView(originalModel.getQuads(state, side, random, data, type), needle.getQuads(state, side, random, data, type));
        }
    }

    /** 只复制少量north针面顶点；绕+Z旋转不改变north法线、UV、颜色或光照属性。 */
    private static final class RotatingNeedle extends BakedModelWrapper<BakedModel> {
        private final double cosine;
        private final double sine;
        private RotatingNeedle(BakedModel needle, double degrees) {
            super(needle);
            double radians = Math.toRadians(degrees * ROTATION_SIGN);
            cosine = Math.cos(radians); sine = Math.sin(radians);
        }
        @Override public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return rotate(originalModel.getQuads(state, side, random));
        }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data, @Nullable RenderType type) {
            return rotate(originalModel.getQuads(state, side, random, data, type));
        }
        private List<BakedQuad> rotate(List<BakedQuad> source) {
            List<BakedQuad> result = new ArrayList<>(source.size());
            int stride = DefaultVertexFormat.BLOCK.getVertexSize() / Integer.BYTES;
            for (BakedQuad quad : source) {
                int[] vertices = quad.getVertices().clone();
                for (int offset = 0; offset < vertices.length; offset += stride) {
                    double x = Float.intBitsToFloat(vertices[offset]) - PIVOT_X_PIXELS / 16.0;
                    double y = Float.intBitsToFloat(vertices[offset + 1]) - PIVOT_Y_PIXELS / 16.0;
                    vertices[offset] = Float.floatToRawIntBits((float)(PIVOT_X_PIXELS / 16.0 + x * cosine - y * sine));
                    vertices[offset + 1] = Float.floatToRawIntBits((float)(PIVOT_Y_PIXELS / 16.0 + x * sine + y * cosine));
                }
                result.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
            }
            return result;
        }
    }

    /** 合并视图供非原生pass消费者读取，不复制静态body的quad列表或顶点。 */
    private static final class QuadView extends AbstractList<BakedQuad> implements RandomAccess {
        private final List<BakedQuad> first;
        private final List<BakedQuad> second;
        private QuadView(List<BakedQuad> first, List<BakedQuad> second) { this.first = first; this.second = second; }
        @Override public BakedQuad get(int index) { return index < first.size() ? first.get(index) : second.get(index - first.size()); }
        @Override public int size() { return first.size() + second.size(); }
    }
}
