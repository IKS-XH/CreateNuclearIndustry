package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.turbine.TurbineGeometry;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * 仅客户端的机壳装饰包装：按现有PIECE尺寸和世界轴向格坐标选择16像素atlas格。
 * 不查询机主、轴列或运行状态；d3、独立板、物品和未知上下文完整沿用原模型。
 */
public final class TurbineCasingAppearanceModel extends BakedModelWrapper<BakedModel> {
    static final ResourceLocation SOURCE = ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", "block/turbine/turbine_casing_07_panel");
    static final ResourceLocation TARGET = ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", "block/turbine/turbine_casing_07_sparse");
    private static final ModelProperty<Appearance> APPEARANCE = new ModelProperty<>();
    private final SpriteShiftEntry shift;

    /** 原模型保留几何/RenderType；entry由Catnip每次atlas重载更新，不保存跨重载sprite。 */
    public TurbineCasingAppearanceModel(BakedModel original, SpriteShiftEntry shift) {
        super(original);
        this.shift = shift;
    }

    @Override public ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData data) {
        ModelData original = super.getModelData(world, pos, state, data);
        Appearance appearance = null;
        if (realWorld(world) && validCasing(state)) {
            var piece = TurbineGeometry.piece(state.getValue(TurbinePartBlock.PIECE));
            if (piece != null && piece.diameter() != 3) {
                Direction facing = state.getValue(TurbinePartBlock.MACHINE_FACING);
                Direction axial = facing.getOpposite();
                // 相位只取世界坐标投影，搬移后允许变化；long避免负向投影的整数溢出。
                long coordinate = (long) pos.getX() * axial.getStepX() + (long) pos.getZ() * axial.getStepZ();
                int phase = (int) Math.floorMod(coordinate, piece.diameter() == 5 ? 2L : 3L);
                appearance = new Appearance(piece.id(), facing, piece.diameter(), phase);
            }
        }
        return original.derive().with(APPEARANCE, appearance).build();
    }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
        List<BakedQuad> original = super.getQuads(state, side, random, data, renderType);
        Appearance appearance = data.get(APPEARANCE);
        if (appearance == null || !validCasing(state) || appearance.piece() != state.getValue(TurbinePartBlock.PIECE)
                || appearance.facing() != state.getValue(TurbinePartBlock.MACHINE_FACING)) return original;
        var target = shift.getTarget();
        // 新图尚未安装或资源包规格不符时保留原材质，不能把missing sprite套成稀疏atlas。
        if (target == null || target.contents().width() != 64 || target.contents().height() != 32) return original;
        var output = new ArrayList<BakedQuad>(original.size());
        int row = appearance.diameter() == 5 ? 0 : 1;
        for (var quad : original) {
            var sprite = quad.getSprite();
            if (sprite == null || !SOURCE.equals(sprite.contents().name())) { output.add(quad); continue; }
            int[] vertices = quad.getVertices().clone();
            int stride = vertices.length / 4;
            for (int i = 0; i < 4; i++) {
                int index = i * stride;
                // 锁定1.21.1的getUOffset/getVOffset为0..1；一个tile仍精确对应16×16像素。
                float u = sprite.getUOffset(Float.intBitsToFloat(vertices[index + 4]));
                float v = sprite.getVOffset(Float.intBitsToFloat(vertices[index + 5]));
                vertices[index + 4] = Float.floatToRawIntBits(target.getU((appearance.phase() + u) / 4f));
                vertices[index + 5] = Float.floatToRawIntBits(target.getV((row + v) / 2f));
            }
            output.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), target, quad.isShade(), quad.hasAmbientOcclusion()));
        }
        return List.copyOf(output);
    }

    static boolean realWorld(BlockAndTintGetter world) {
        // 原生区段和真实客户端世界可读现有显示状态；Ponder/未知包装禁止自行寻找机组。
        return world instanceof ClientLevel || world != null && world.getClass() == RenderChunkRegion.class;
    }

    private static boolean validCasing(BlockState state) {
        return state != null && state.getBlock() instanceof TurbinePartBlock.Casing && state.getValue(TurbinePartBlock.LOCATED);
    }

    /** 不可变装饰数据；piece防止区段重建间把旧尺寸数据用于另一种模型。 */
    private record Appearance(int piece, Direction facing, int diameter, int phase) { }
}
