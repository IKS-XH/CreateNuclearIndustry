package com.iksxh.create_nuclear_industry.industrial.client;

import com.iksxh.create_nuclear_industry.industrial.IndustrialPumpBlockEntity;
import com.iksxh.create_nuclear_industry.industrial.IndustrialPumpBlock;
import net.minecraft.core.Direction;
import net.createmod.catnip.render.SuperByteBuffer;

/** 工业视觉的纯朝向适配；只读取现有同步泵速/方向，角度单位为度，不写服务器状态。 */
public final class IndustrialVisualOrientation {
    private IndustrialVisualOrientation() {}
    /**
     * 客户端箭头始终表示预定运输方向；同步理论轴速单位为RPM，零速按FACING、负速按反向。
     * 超载/冻结保留理论轴速符号，但不改变真实动力或服务端运输；运行时仍与outputDirection一致。
     */
    public static Direction displayedOutput(IndustrialPumpBlockEntity pump) {
        Direction facing = pump.getBlockState().getValue(IndustrialPumpBlock.FACING);
        return pump.getTheoreticalSpeed() < 0 ? facing.getOpposite() : facing;
    }
    /** 原泵body以UP为基准；Minecraft模型角与JOML右手旋转相反，按Y后X应用负角。 */
    public static int xRotation(Direction direction) { return direction == Direction.UP ? 0 : direction == Direction.DOWN ? -180 : -90; }
    public static int yRotation(Direction direction) { return switch (direction) { case EAST -> -90; case SOUTH -> -180; case WEST -> -270; default -> 0; }; }
    /** 真实SuperByteBuffer绘制入口；将完整UV下的实际UP glyph朝向显示方向，保持服务器流向不变。 */
    public static SuperByteBuffer orientArrow(SuperByteBuffer buffer, Direction output) {
        return buffer.center().rotateYDegrees(yRotation(output)).rotateXDegrees(xRotation(output)).uncenter();
    }
    /** 三种body窗口均沿局部Z；原pointer位于局部Y平面，统一正X90度转向真实窗口。 */
    public static int valvePointerTilt() { return 90; }
    /** 仅Z管轴的body绕Y负90度；指针首先应用相同的整体朝向。 */
    public static int valvePointerYaw(Direction.Axis axis) { return axis == Direction.Axis.Z ? -90 : 0; }
    /** 开度单位百分比；竖直管轴复用原Create90度基准补偿，再沿局部Y应用0..-90度开度。 */
    public static float valvePointerAngle(Direction.Axis axis, int opening) { return (axis == Direction.Axis.Y ? 90 : 0) - 90f * opening / 100f; }
}
