package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.slf4j.LoggerFactory;

/** 客户端唯一主控读取真实工序绘八活动分件；原机壳和Create底半轴沿现行渲染。 */
public final class ShieldedAssemblyRenderer extends KineticBlockEntityRenderer<ShieldedAssemblyBlockEntity> {
    /** 需在首次模型烘焙前由客户端渲染器注册事件读取，确保Flywheel能枚举并烘焙全部分件。 */
    static final List<PartialModel> PARTIAL_MODELS = ShieldedAssemblyAnimation.GROUP_NAMES.stream()
            .map(name -> partial("animation/" + name)).toList();
    private static final List<PartialModel> IDLE_MODELS = List.of(
            partial("partials/left_arm"), partial("partials/right_arm"), partial("partials/fixture"));
    private final ShieldedAssemblyAnimation animation;
    // 弱实体键与弱世界身份防止旧世界被缓存保活；每台/每个世界拥有独立的纯工时时钟。
    private record Timeline(WeakReference<Level> world, ShieldedAssemblyAnimationClock clock) {}
    private final Map<ShieldedAssemblyBlockEntity, Timeline> timelines = new WeakHashMap<>();
    private static PartialModel partial(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID,
                "block/shielded_assembly_station/" + path));
    }
    /** 原生dispatcher在资源重载时重建Renderer；只此客户端构造入口读取当前资源管理器。 */
    public ShieldedAssemblyRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        ShieldedAssemblyAnimation loaded = null;
        var id = ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "shielded_assembly/animation.json");
        try (var reader = Minecraft.getInstance().getResourceManager().getResource(id)
                .orElseThrow(() -> new IOException("缺少装配室动作资源")).openAsReader()) {
            loaded = ShieldedAssemblyAnimation.read(reader);
        } catch (IOException | IllegalArgumentException exception) {
            LoggerFactory.getLogger(ShieldedAssemblyRenderer.class).warn("屏蔽装配室动作数据加载失败，保持原三件待机机构", exception);
        }
        animation = loaded;
    }
    /** 按实际朝向包围完整八格，避免活动件跨出主格后被视锥过早裁掉。 */
    @Override public AABB getRenderBoundingBox(ShieldedAssemblyBlockEntity machine) {
        BlockPos origin = machine.getBlockPos();
        Direction facing = machine.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        int minX = origin.getX(), minZ = origin.getZ(), maxX = minX, maxZ = minZ;
        for (int part = 1; part < 8; part++) {
            BlockPos pos = ShieldedAssemblyLayout.position(origin, facing, part);
            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
        }
        return new AABB(minX, origin.getY(), minZ, maxX + 1, origin.getY() + 2, maxZ + 1);
    }
    @Override protected void renderSafe(ShieldedAssemblyBlockEntity machine, float partialTicks, PoseStack pose,
                                        MultiBufferSource buffers, int light, int overlay) {
        if (!machine.current()) { timelines.remove(machine); return; }
        if (machine.expanded()) {
            renderRotatingBuffer(machine, CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,
                    machine.getBlockState(), Direction.DOWN), pose, buffers.getBuffer(RenderType.solid()), light);
            Direction facing = machine.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            // Flywheel/JOML正角把(x,z)转为(z,-x)；方块state的east y=90对应反号，
            // 因此活动件绕主格中心用负角，才能与八格坐标及静态外壳保持同向。
            float angle = switch (facing) {
                case EAST -> (float) -Math.PI / 2;
                case SOUTH -> (float) Math.PI;
                case WEST -> (float) Math.PI / 2;
                default -> 0f;
            };
            if (animation == null) {
                for (PartialModel model : IDLE_MODELS)
                    CachedBuffers.partial(model, machine.getBlockState()).rotateCentered(angle, Direction.UP)
                            .light(light).renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
                return;
            }
            Level world = machine.getLevel();
            Timeline timeline = timelines.get(machine);
            if (timeline == null || timeline.world().get() != world) {
                timeline = new Timeline(new WeakReference<>(world), new ShieldedAssemblyAnimationClock());
                timelines.put(machine, timeline);
            }
            var state = machine.state();
            var sample = new ShieldedAssemblyAnimationClock.Sample(state.operation(), state.progress(), state.work(),
                    machine.getSpeed(), true, true, machine.getBlockState().getValue(ShieldedAssemblyBlock.WORKING),
                    machine.isOverStressed(), state.ready(), !state.outputEmpty());
            var frame = timeline.clock().update(sample, world.getGameTime() + (double) partialTicks,
                    Minecraft.getInstance().isPaused());
            var motion = animation.pose(state.operation(), frame.phase());
            for (int index = 0; index < PARTIAL_MODELS.size(); index++) {
                // partial保留全局整机原点；F*D已包含主控朝向与局部pivot，不再rotateCentered或补偏移。
                pose.pushPose();
                pose.mulPose(animation.matrix(motion, ShieldedAssemblyAnimation.GROUP_NAMES.get(index), angle));
                CachedBuffers.partial(PARTIAL_MODELS.get(index), machine.getBlockState()).light(light)
                        .renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
                pose.popPose();
            }
        } else timelines.remove(machine);
    }
}
