package com.iksxh.create_nuclear_industry.turbine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** 三档结构和资源编号必须引用同一真实薄壳占格。 */
final class TurbineGeometryTest {
    @Test void sectionCountsAndDiagonalSlotsMatchTheApprovedMasks() {
        int[] diameters = {3, 5, 7};
        int[] expectedShell = {8, 16, 24};
        int[] expectedEnd = {9, 25, 45};
        int[] expectedAir = {0, 8, 20};
        int[] expectedCasing = {37, 141, 301};
        for (int i = 0; i < diameters.length; i++) {
            int diameter = diameters[i], radius = (diameter - 1) / 2;
            int shell = 0, end = 0, air = 0;
            for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++) {
                if (TurbineGeometry.footprint(diameter, x, y)) end++;
                if (TurbineGeometry.shellSlot(diameter, x, y)) shell++;
                if (TurbineGeometry.airSlot(diameter, x, y)) air++;
            }
            assertEquals(expectedShell[i], shell);
            assertEquals(expectedEnd[i], end);
            assertEquals(expectedAir[i], air);
            assertEquals(expectedCasing[i], 2 * (end - 1)
                    + (new int[]{3, 6, 9}[i]) * shell - 3);
        }
        assertTrue(TurbineGeometry.shellSlot(7, 2, 2));
        assertFalse(TurbineGeometry.footprint(7, 3, 3));
        assertEquals(206, TurbineGeometry.MAX_PIECE);
    }

    @Test void everyPieceHasUniqueStableModelNameAndOnlyFlatCentralWindows() {
        var names = new java.util.HashSet<String>();
        for (var piece : TurbineGeometry.pieces()) {
            assertTrue(names.add(piece.modelName()));
            assertEquals(piece, TurbineGeometry.piece(piece.id()));
            assertEquals(piece.id(), TurbineGeometry.pieceId(piece.diameter(), piece.section(),
                    piece.x(), piece.y()));
        }
        assertNull(TurbineGeometry.piece(0));
        assertEquals(0, TurbineGeometry.pieceId(7, TurbineGeometry.Section.MIDDLE, 3, 3));
        for (int diameter : new int[]{3, 5, 7}) {
            int radius = (diameter - 1) / 2;
            int windows = 0;
            for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++)
                if (TurbineGeometry.windowSlot(diameter, x, y)) windows++;
            assertEquals(4, windows);
            assertFalse(TurbineGeometry.windowSlot(diameter, radius, radius));
        }
    }
}
