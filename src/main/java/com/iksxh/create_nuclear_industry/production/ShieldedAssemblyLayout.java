package com.iksxh.create_nuclear_industry.production;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 八格布局的唯一坐标约定。主控为北向局部原点，part=x+2*z+4*y；
 * 水平旋转始终围绕主控格中心，返回的是世界方块坐标而非渲染像素。
 */
public final class ShieldedAssemblyLayout {
    private ShieldedAssemblyLayout() {}

    public static BlockPos position(BlockPos master, Direction facing, int part) {
        int x = part & 1;
        int z = (part >> 1) & 1;
        int y = (part >> 2) & 1;
        return switch (facing) {
            case EAST -> master.offset(-z, y, x);
            case SOUTH -> master.offset(-x, y, -z);
            case WEST -> master.offset(z, y, -x);
            default -> master.offset(x, y, z);
        };
    }

    public static int partAt(BlockPos master, Direction facing, BlockPos target) {
        for (int part = 0; part < 8; part++)
            if (position(master, facing, part).equals(target)) return part;
        return -1;
    }

    public static boolean exterior(int part, Direction facing, Direction side) {
        int x = part & 1;
        int z = (part >> 1) & 1;
        if (side == Direction.UP) return (part & 4) != 0;
        if (side == Direction.DOWN) return false;
        BlockPos center = position(BlockPos.ZERO, facing, part);
        return partAt(BlockPos.ZERO, facing, center.relative(side)) < 0;
    }
}
