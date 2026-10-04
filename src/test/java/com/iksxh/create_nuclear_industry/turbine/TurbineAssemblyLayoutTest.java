package com.iksxh.create_nuclear_industry.turbine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** 三档及自定义有效轴长在四个水平朝向下共用同一局部坐标系。 */
final class TurbineAssemblyLayoutTest {
    @Test void fourDirectionsAndNonDefaultLengthPreserveDistinctCrossAndAxialCoordinates() {
        BlockPos front = new BlockPos(101, 70, -47);
        for (Direction facing : Direction.Plane.HORIZONTAL)
            for (int[] size : new int[][]{{3, 5}, {5, 8}, {7, 11}, {5, 9}}) {
                var layout = new TurbineAssembly.Layout(front, facing, size[1], size[0]);
                int radius = layout.radius();
                for (int z : new int[]{0, size[1] / 2, size[1] - 1})
                    for (int x : new int[]{-radius, 0, radius}) {
                        BlockPos pos = layout.at(x, 0, z);
                        assertEquals(z, layout.axial(pos));
                        assertEquals(x, layout.cross(pos));
                        assertTrue(layout.contains(pos));
                    }
                assertFalse(layout.contains(layout.at(0, 0, size[1])));
                assertFalse(layout.contains(layout.at(radius + 1, 0, 1)));
            }
    }

    @Test void independentPanelFacesKeepLegacyTopAndSeparateWorldDirections() {
        assertEquals(0, TurbinePartBlock.independentPiece(Direction.UP));
        for (Direction face : Direction.values())
            assertEquals(face, TurbinePartBlock.independentFace(
                    TurbinePartBlock.independentPiece(face)));
        assertEquals(206, TurbineGeometry.MAX_PIECE);
        assertEquals(211, TurbinePartBlock.LAST_INDEPENDENT_PIECE);
    }
}
