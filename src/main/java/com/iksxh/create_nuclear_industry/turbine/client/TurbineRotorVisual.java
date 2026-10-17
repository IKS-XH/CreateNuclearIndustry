package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineRotorBlockEntity;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;

/**
 * 客户端转子实例；同次rotorPose提供可信几何与相位，租约失效仍按当前合法外观静止绘制。
 * 每个BE只持一个GPU实例，换直径复用实例，删除/卸载/重载由Flywheel生命周期释放。
 */
public final class TurbineRotorVisual extends AbstractBlockEntityVisual<TurbineRotorBlockEntity>
        implements SimpleDynamicVisual {
    private TransformedInstance instance;
    private PartialModel partial;
    private boolean visible;

    public TurbineRotorVisual(VisualizationContext context, TurbineRotorBlockEntity rotor, float partialTick) {
        super(context, rotor, partialTick);
    }

    @Override public void beginFrame(Context context) {
        var state = blockEntity.getBlockState();
        // 与原BER扩展包围盒逐轴一致，叶片可见而轴心离开视锥时仍须绘制。
        float extra = state.getValue(TurbinePartBlock.DIAMETER).blocks() / 2f;
        if (blockEntity.isRemoved() || !state.getValue(TurbinePartBlock.LOCATED)
                || !TurbineOutputShaftVisual.withinDistance(pos, context.camera().getPosition())
                || !context.frustum().testAab(visualPos.getX() - extra, visualPos.getY() - extra, visualPos.getZ() - extra,
                visualPos.getX() + 1 + extra, visualPos.getY() + 1 + extra, visualPos.getZ() + 1 + extra)) {
            hide();
            return;
        }
        var display = TurbineClientEvents.rotorPose(blockEntity, context.partialTick());
        int diameter = display.sample().map(sample -> sample.diameter())
                .orElse(state.getValue(TurbinePartBlock.DIAMETER).blocks());
        PartialModel next = TurbineRotorRenderer.BLADES[switch (diameter) {
            case 5 -> 1;
            case 7 -> 2;
            default -> 0;
        }];
        if (next != partial) {
            var instancer = instancerProvider().instancer(InstanceTypes.TRANSFORMED, TurbineOutputShaftVisual.model(next));
            if (instance == null) instance = instancer.createInstance();
            else instancer.stealInstance(instance);
            partial = next;
            relight(instance);
        }
        var facing = display.sample().map(sample -> sample.facing())
                .orElse(state.getValue(TurbinePartBlock.MACHINE_FACING));
        TurbineOutputShaftVisual.transform(instance.pose, visualPos, facing, display.degrees());
        instance.setChanged();
        visible = true;
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
