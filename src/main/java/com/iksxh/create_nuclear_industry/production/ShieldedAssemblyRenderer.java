package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.List;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/** 客户端由唯一主控绘制底部半轴与三件待机机构；静态八格机壳由方块模型负责。 */
public final class ShieldedAssemblyRenderer extends KineticBlockEntityRenderer<ShieldedAssemblyBlockEntity> {
    /** 需在首次模型烘焙前由客户端渲染器注册事件读取，确保Flywheel能枚举并烘焙全部分件。 */
    static final List<PartialModel> PARTIAL_MODELS = List.of(
            partial("left_arm"), partial("right_arm"), partial("fixture"));
    private static PartialModel partial(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID,
                "block/shielded_assembly_station/partials/" + name));
    }
    public ShieldedAssemblyRenderer(BlockEntityRendererProvider.Context context) { super(context); }
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
        if (!machine.current()) return;
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
            // partial自身已使用整机坐标，绕主格中心转向即可；本批保持待机位，不叠加pivot位移。
            for (PartialModel model : PARTIAL_MODELS)
                CachedBuffers.partial(model, machine.getBlockState()).rotateCentered(angle, Direction.UP)
                        .light(light).renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
        }
    }
}
