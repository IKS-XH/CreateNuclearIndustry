package com.iksxh.create_nuclear_industry.turbine;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
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

/** 普通蒸汽只有容器/管网身份；没有桶、世界方块或落地流动能力。 */
public final class StorageOnlyOrdinarySteamFluid extends Fluid {
    @Override public FluidType getFluidType() { return TurbineContent.STEAM_TYPE.get(); }
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
