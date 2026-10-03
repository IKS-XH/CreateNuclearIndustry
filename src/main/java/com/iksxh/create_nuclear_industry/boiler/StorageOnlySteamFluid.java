package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * 只用于流体容器与 Create 管网的蒸汽身份；无世界方块、桶和流动状态。
 * 非 FlowingFluid 使开放管口拒绝向空气放置，避免把已付费蒸汽无声删除。
 */
public final class StorageOnlySteamFluid extends Fluid {
    @Override public FluidType getFluidType() { return BoilerContent.SUPERCRITICAL_TYPE.get(); }
    @Override public Item getBucket() { return Items.AIR; }
    @Override protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                                                   Fluid incoming, Direction direction) { return false; }
    @Override protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) { return Vec3.ZERO; }
    @Override public int getTickDelay(LevelReader level) { return 0; }
    @Override protected float getExplosionResistance() { return 0; }
    @Override public float getHeight(FluidState state, BlockGetter level, BlockPos pos) { return 0; }
    @Override public float getOwnHeight(FluidState state) { return 0; }
    @Override protected BlockState createLegacyBlock(FluidState state) { return Blocks.AIR.defaultBlockState(); }
    @Override public boolean isSource(FluidState state) { return true; }
    @Override public int getAmount(FluidState state) { return 8; }
    @Override public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
}
