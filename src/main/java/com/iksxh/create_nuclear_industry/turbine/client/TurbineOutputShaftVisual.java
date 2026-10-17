package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.turbine.TurbineOutputShaftBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineShaftBlock;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 客户端输出轴实例：复用原partial和累计相位，仅把每帧顶点回放改为单个实例矩阵更新。
 * 原BER继续承担不支持Flywheel的上下文；本类不生成网速、不触碰服务端或持久化状态。
 */
public final class TurbineOutputShaftVisual extends AbstractBlockEntityVisual<TurbineOutputShaftBlockEntity>
        implements SimpleDynamicVisual {
    private static final RendererReloadCache<PartialModel, Model> MODELS = new RendererReloadCache<>(partial ->
            new BakedModelBuilder(partial.get()).materialFunc((type, shaded, ao) -> shaded
                    ? Materials.CUTOUT_MIPPED_BLOCK : Materials.CUTOUT_MIPPED_UNSHADED_BLOCK).build());
    private TransformedInstance instance;
    private PartialModel partial;
    private boolean visible;

    public TurbineOutputShaftVisual(VisualizationContext context, TurbineOutputShaftBlockEntity shaft, float partialTick) {
        super(context, shaft, partialTick);
    }

    /** 缓存随Flywheel资源重载统一释放；强制沿用BER的cutoutMipped，避免模型默认solid填住透明孔。 */
    static Model model(PartialModel partial) { return MODELS.get(partial); }

    @Override public void beginFrame(Context context) {
        var state = blockEntity.getBlockState();
        if (blockEntity.isRemoved() || !state.getValue(TurbinePartBlock.LOCATED)
                || !withinDistance(pos, context.camera().getPosition())
                || !context.frustum().testAab(visualPos.getX(), visualPos.getY(), visualPos.getZ(),
                visualPos.getX() + 1, visualPos.getY() + 1, visualPos.getZ() + 1)) {
            hide();
            return;
        }
        PartialModel next = TurbineOutputShaftRenderer.partialFor(state.getValue(TurbineShaftBlock.END));
        if (next != partial) {
            var instancer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, model(next));
            if (instance == null) instance = instancer.createInstance();
            else instancer.stealInstance(instance);
            partial = next;
            relight(instance);
        }
        transform(instance.pose, visualPos, state.getValue(TurbinePartBlock.MACHINE_FACING),
                TurbineClientEvents.outputAngle(blockEntity, context.partialTick()));
        instance.setChanged();
        visible = true;
    }

    /** 保持原BlockEntityRenderer的中心点严格64格门槛，不使用Flywheel默认的远距降频。 */
    static boolean withinDistance(BlockPos pos, Vec3 camera) { return pos.distToCenterSqr(camera) < 64 * 64; }

    /** 角度单位度；矩阵右乘为平移*中心Yaw*中心Roll，与原SBB先局部旋转再映射世界朝向一致。 */
    static Matrix4f transform(Matrix4f matrix, Vec3i offset, Direction facing, double degrees) {
        float yaw = switch (facing) {
            case EAST -> -90;
            case SOUTH -> -180;
            case WEST -> -270;
            default -> 0;
        };
        return matrix.translation(offset.getX(), offset.getY(), offset.getZ())
                .translate(.5f, .5f, .5f).rotateY((float) Math.toRadians(yaw))
                .rotateZ((float) Math.toRadians(degrees)).translate(-.5f, -.5f, -.5f);
    }

    private void hide() {
        if (instance != null && visible) {
            instance.setZeroTransform().setChanged();
            visible = false;
        }
    }

    @Override public void updateLight(float partialTick) { relight(instance); }

    @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
        if (instance != null && visible) consumer.accept(instance);
    }

    @Override protected void _delete() {
        if (instance != null) instance.delete();
        instance = null;
        partial = null;
        visible = false;
    }
}
