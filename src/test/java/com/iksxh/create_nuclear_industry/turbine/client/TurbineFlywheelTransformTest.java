package com.iksxh.create_nuclear_industry.turbine.client;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 将实例的真实矩阵入口与原BER提交逐点对照，覆盖世界原点偏移、四向和正负/零角。 */
class TurbineFlywheelTransformTest {
    private static Method entry(String name, Class<?>... parameters) throws Exception {
        Class<?> visual = Class.forName("com.iksxh.create_nuclear_industry.turbine.client.TurbineOutputShaftVisual");
        Method method = visual.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    @Test void instanceMatrixMatchesOriginalSubmissionAcrossFacingsAndSignedAngles() throws Exception {
        Method transform = entry("transform", Matrix4f.class, Vec3i.class, Direction.class, double.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (double degrees : new double[]{0, 90, -90, 359.75, -720.125}) {
                var original = new TurbineAnimationRendererTest.BufferRecord();
                TurbineOutputShaftRenderer.renderPiece(original.buffer(), facing, degrees,
                        new com.mojang.blaze3d.vertex.PoseStack(), null, 15728880, 655360);
                var offset = new BlockPos(-120, 7, 399);
                Matrix4f matrix = new Matrix4f();
                assertSame(matrix, transform.invoke(null, matrix, offset, facing, degrees));
                for (Vector3f point : new Vector3f[]{new Vector3f(.5f), new Vector3f(.1f, .3f, -.6f),
                        new Vector3f(3.9f, -2.9f, .7f)}) {
                    Vector3f expected = original.local.last().pose().transformPosition(new Vector3f(point))
                            .add(offset.getX(), offset.getY(), offset.getZ());
                    Vector3f actual = matrix.transformPosition(new Vector3f(point));
                    assertEquals(expected.x, actual.x, 4e-5);
                    assertEquals(expected.y, actual.y, 4e-5);
                    assertEquals(expected.z, actual.z, 4e-5);
                }
                Vector3f expectedNormal = original.local.last().pose().transformDirection(new Vector3f(.1f, .8f, .2f));
                Vector3f actualNormal = matrix.transformDirection(new Vector3f(.1f, .8f, .2f));
                assertTrue(expectedNormal.distance(actualNormal) < 1e-5);
            }
        }
    }

    @Test void original64BlockCenterDistanceHasStrictBoundary() throws Exception {
        Method distance = entry("withinDistance", BlockPos.class, Vec3.class);
        var pos = new BlockPos(-16, -60, 0);
        Vec3 center = Vec3.atCenterOf(pos);
        assertEquals(true, distance.invoke(null, pos, center.add(63.999, 0, 0)));
        assertEquals(false, distance.invoke(null, pos, center.add(64, 0, 0)));
        assertEquals(false, distance.invoke(null, pos, center.add(0, 0, 64.001)));
        assertEquals(true, distance.invoke(null, pos, center.add(-32, -32, -32)));
    }
}
