package com.iksxh.create_nuclear_industry.turbine;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 按统一八棱几何把每格薄壳离散到十六分之一格，端盖与内壁均不占满方块。 */
final class TurbineCollision {
    private static final VoxelShape[][] CACHE = new VoxelShape[TurbineGeometry.MAX_PIECE + 1][4];
    private TurbineCollision() {}

    static VoxelShape shape(int pieceId, Direction facing) {
        if (pieceId <= 0 || pieceId > TurbineGeometry.MAX_PIECE) return Shapes.empty();
        int direction = facing.get2DDataValue();
        VoxelShape cached = CACHE[pieceId][direction];
        if (cached != null) return cached;
        synchronized (CACHE) {
            if (CACHE[pieceId][direction] == null)
                CACHE[pieceId][direction] = create(TurbineGeometry.piece(pieceId), facing);
            return CACHE[pieceId][direction];
        }
    }

    private static VoxelShape create(TurbineGeometry.Piece piece, Direction facing) {
        int diameter = piece.diameter();
        double half = diameter / 2D;
        double diagonal = 2 * half - diameter / (2 + Math.sqrt(2));
        double innerDiagonal = diagonal - 3 * Math.sqrt(2) / 16;
        VoxelShape result = Shapes.empty();
        for (int py = 0; py < 16; py++) {
            int start = -1, previous = 0;
            for (int px = 0; px <= 16; px++) {
                int type = 0;
                if (px < 16) {
                    double x = Math.abs(piece.x() + (px + .5) / 16 - .5);
                    double y = Math.abs(piece.y() + (py + .5) / 16 - .5);
                    boolean outer = x < half && y < half && x + y < diagonal;
                    boolean inner = x < half - 3D / 16 && y < half - 3D / 16
                            && x + y < innerDiagonal;
                    if (outer) type = inner && piece.section() != TurbineGeometry.Section.MIDDLE ? 1
                            : inner ? 0 : 2;
                }
                if (type != previous && start >= 0) {
                    double z0 = previous == 1 && piece.section() == TurbineGeometry.Section.REAR ? 13 : 0;
                    double z1 = previous == 1 && piece.section() == TurbineGeometry.Section.FRONT ? 3 : 16;
                    result = Shapes.or(result, TurbinePartBlock.boxRotated(start, py, z0,
                            px, py + 1, z1, facing));
                    start = -1;
                }
                if (type != 0 && start < 0) start = px;
                previous = type;
            }
        }
        return result.optimize();
    }
}
