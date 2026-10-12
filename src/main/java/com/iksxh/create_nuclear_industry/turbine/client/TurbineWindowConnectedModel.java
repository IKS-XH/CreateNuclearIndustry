package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.turbine.TurbineGeometry;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * 只根据已有显示状态连接轴向窗口；不扫描结构或核验运行owner。
 * 对原OBJ已烘焙quad撤共享端完整实体框并延伸玻璃，保留材质、厚度、光照及透明RenderType。
 */
public final class TurbineWindowConnectedModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Connection> CONNECTION = new ModelProperty<>();
    private static final double INSET = .14;
    private static final double EPSILON = 1e-5;

    /** 原模型继续负责非连接形态、物品变换与RenderType，连接只改变可见mesh。 */
    public TurbineWindowConnectedModel(BakedModel original) {
        super(original);
    }

    @Override public ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData data) {
        ModelData original = super.getModelData(world, pos, state, data);
        Connection connection = null;
        if (TurbineCasingAppearanceModel.realWorld(world) && validWindow(state)) {
            Direction facing = state.getValue(TurbinePartBlock.MACHINE_FACING);
            Direction axial = facing.getOpposite();
            // 只读长轴正负各一格。普通壳/端盖/功能口天然截断，绝不调用TurbineAssembly或owner扫描。
            int mask = (matches(state, world.getBlockState(pos.relative(axial.getOpposite()))) ? 1 : 0)
                    | (matches(state, world.getBlockState(pos.relative(axial))) ? 2 : 0);
            connection = new Connection(state.getValue(TurbinePartBlock.PIECE), facing,
                    state.getValue(TurbinePartBlock.FORMED), mask);
        }
        return original.derive().with(CONNECTION, connection).build();
    }

    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType renderType) {
        List<BakedQuad> original = super.getQuads(state, side, random, data, renderType);
        Connection connection = data.get(CONNECTION);
        if (connection == null || connection.mask() == 0 || !validWindow(state)
                || connection.piece() != state.getValue(TurbinePartBlock.PIECE)
                || connection.facing() != state.getValue(TurbinePartBlock.MACHINE_FACING)
                || connection.formed() != state.getValue(TurbinePartBlock.FORMED)) return original;
        var output = new ArrayList<BakedQuad>(original.size());
        Direction axial = connection.facing().getOpposite();
        var piece = TurbineGeometry.piece(connection.piece());
        Direction outward = piece.y() != 0 ? (piece.y() > 0 ? Direction.UP : Direction.DOWN)
                : (piece.x() > 0 ? connection.facing().getClockWise() : connection.facing().getCounterClockWise());
        boolean before = (connection.mask() & 1) != 0, after = (connection.mask() & 2) != 0;
        for (var quad : original) {
            var sprite = quad.getSprite();
            if (sprite == null || !knownSprite(sprite.contents().name().toString())) { output.add(quad); continue; }
            int[] source = quad.getVertices();
            int stride = source.length / 4;
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                double coordinate = axialCoordinate(source, i * stride, axial);
                min = Math.min(min, coordinate); max = Math.max(max, coordinate);
            }
            // 整个quad位于端条范围就撤下：包括横框前后面、四侧壁、长框端盖和玻璃端面。
            if (before && max <= INSET + EPSILON || after && min >= 1 - INSET - EPSILON) continue;
            int[] vertices = source.clone();
            boolean changed = false;
            boolean glassPlane = sprite.contents().name().toString().equals("create_nuclear_industry:block/turbine/turbine_window_glass")
                    && glassPlane(source, stride, outward);
            for (int i = 0; i < 4; i++) {
                int index = i * stride;
                if (glassPlane) {
                    // 原PNG透明边距不能在连窗逐格重复；只裁两张大面，玻璃边面仍使用原UV。
                    float u = sprite.getUOffset(Float.intBitsToFloat(source[index + 4]));
                    float v = sprite.getVOffset(Float.intBitsToFloat(source[index + 5]));
                    vertices[index + 4] = Float.floatToRawIntBits(sprite.getU((4.5f + 7 * u) / 16));
                    vertices[index + 5] = Float.floatToRawIntBits(sprite.getV((3.5f + 8 * v) / 16));
                    changed = true;
                }
                double coordinate = axialCoordinate(source, index, axial);
                double delta = before && Math.abs(coordinate - INSET) < EPSILON ? -INSET
                        : after && Math.abs(coordinate - (1 - INSET)) < EPSILON ? INSET : 0;
                if (delta != 0) {
                    // 只延伸长轴端点；深度、横向厚框和玻璃0.04/0.08内缩保持原OBJ值。
                    vertices[index] = Float.floatToRawIntBits((float) (Float.intBitsToFloat(source[index]) + delta * axial.getStepX()));
                    vertices[index + 2] = Float.floatToRawIntBits((float) (Float.intBitsToFloat(source[index + 2]) + delta * axial.getStepZ()));
                    changed = true;
                }
            }
            output.add(changed ? new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), sprite,
                    quad.isShade(), quad.hasAmbientOcclusion()) : quad);
        }
        return List.copyOf(output);
    }

    /** 原OBJ边面也共用glass sprite且法向可能同外面；按四顶点实际深度平面选0.04/0.08内缩的大面。 */
    private static boolean glassPlane(int[] vertices, int stride, Direction outward) {
        double first = outwardCoordinate(vertices, 0, outward);
        if (Math.abs(first - .96) > EPSILON && Math.abs(first - .92) > EPSILON) return false;
        for (int i = 1; i < 4; i++)
            if (Math.abs(outwardCoordinate(vertices, i * stride, outward) - first) > EPSILON) return false;
        return true;
    }

    private static double outwardCoordinate(int[] vertices, int index, Direction outward) {
        return (Float.intBitsToFloat(vertices[index]) - .5) * outward.getStepX()
                + (Float.intBitsToFloat(vertices[index + 1]) - .5) * outward.getStepY()
                + (Float.intBitsToFloat(vertices[index + 2]) - .5) * outward.getStepZ() + .5;
    }

    private static double axialCoordinate(int[] vertices, int index, Direction axial) {
        return (Float.intBitsToFloat(vertices[index]) - .5) * axial.getStepX()
                + (Float.intBitsToFloat(vertices[index + 2]) - .5) * axial.getStepZ() + .5;
    }

    private static boolean knownSprite(String name) {
        return name.equals("create_nuclear_industry:block/turbine/turbine_casing_panel")
                || name.equals("create_nuclear_industry:block/turbine/turbine_window_glass");
    }

    private static boolean validWindow(BlockState state) {
        if (state == null || !(state.getBlock() instanceof TurbinePartBlock.Window) || !state.getValue(TurbinePartBlock.LOCATED)) return false;
        var piece = TurbineGeometry.piece(state.getValue(TurbinePartBlock.PIECE));
        return piece != null && piece.section() == TurbineGeometry.Section.MIDDLE
                && TurbineGeometry.windowSlot(piece.diameter(), piece.x(), piece.y());
    }

    private static boolean matches(BlockState from, BlockState to) {
        return validWindow(to) && from.getValue(TurbinePartBlock.PIECE).equals(to.getValue(TurbinePartBlock.PIECE))
                && from.getValue(TurbinePartBlock.MACHINE_FACING) == to.getValue(TurbinePartBlock.MACHINE_FACING)
                && from.getValue(TurbinePartBlock.FORMED).equals(to.getValue(TurbinePartBlock.FORMED));
    }

    /** mask位0/1对应局部长轴前/后邻窗；与getQuads共享不可变数据，不传入世界引用。 */
    private record Connection(int piece, Direction facing, boolean formed, int mask) { }
}
