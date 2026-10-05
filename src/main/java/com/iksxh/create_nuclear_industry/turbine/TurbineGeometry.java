package com.iksxh.create_nuclear_industry.turbine;

import java.util.ArrayList;
import java.util.List;

/**
 * 三档八棱截面的唯一占格和模型编号表。局部 x、y 以轴心为零，z 从前端向后端；
 * 前后端盖占外轮廓所有正面积格，中段只占外环，轴心与内腔由调用方单独处理。
 */
public final class TurbineGeometry {
    public enum Section { FRONT, MIDDLE, REAR }
    public record Piece(int id, int diameter, Section section, int x, int y) {
        public String modelName() {
            return "d" + diameter + "_" + section.name().toLowerCase(java.util.Locale.ROOT)
                    + "_x" + x + "_y" + y;
        }
    }

    private static final List<Piece> PIECES = enumerate();
    public static final int MAX_PIECE = PIECES.size();

    private TurbineGeometry() {}

    private static List<Piece> enumerate() {
        List<Piece> result = new ArrayList<>();
        for (int diameter : new int[]{3, 5, 7}) {
            int radius = (diameter - 1) / 2;
            for (Section section : Section.values()) {
                for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++) {
                    if (!footprint(diameter, x, y)
                            || section == Section.MIDDLE && !shellSlot(diameter, x, y))
                        continue;
                    result.add(new Piece(result.size() + 1, diameter, section, x, y));
                }
            }
        }
        return List.copyOf(result);
    }

    /** 仅当方格与八棱外轮廓的交集有正面积才要求放结构件；切点不算占格。 */
    public static boolean footprint(int diameter, int x, int y) {
        if (!supportedDiameter(diameter)) return false;
        double half = diameter / 2D;
        if (Math.abs(x) - .5 >= half || Math.abs(y) - .5 >= half) return false;
        double nearX = Math.max(0, Math.abs(x) - .5);
        double nearY = Math.max(0, Math.abs(y) - .5);
        return nearX + nearY < 2 * half - diameter / (2 + Math.sqrt(2));
    }

    public static boolean supportedDiameter(int diameter) {
        return diameter == 3 || diameter == 5 || diameter == 7;
    }

    public static boolean shellSlot(int diameter, int x, int y) {
        if (!footprint(diameter, x, y)) return false;
        double half = diameter / 2D;
        double maxX = Math.abs(x) + .5, maxY = Math.abs(y) + .5;
        double innerDiagonal = 2 * half - diameter / (2 + Math.sqrt(2))
                - 3 * Math.sqrt(2) / 16;
        return maxX > half - 3D / 16 || maxY > half - 3D / 16
                || maxX + maxY > innerDiagonal;
    }

    public static boolean airSlot(int diameter, int x, int y) {
        return footprint(diameter, x, y) && !shellSlot(diameter, x, y)
                && (x != 0 || y != 0);
    }

    public static boolean windowSlot(int diameter, int x, int y) {
        int radius = (diameter - 1) / 2;
        return supportedDiameter(diameter) && (x == 0 && Math.abs(y) == radius
                || y == 0 && Math.abs(x) == radius);
    }

    public static boolean sideSlot(int diameter, int x, int y) {
        return windowSlot(diameter, x, y);
    }

    public static int pieceId(int diameter, Section section, int x, int y) {
        for (Piece piece : PIECES)
            if (piece.diameter() == diameter && piece.section() == section
                    && piece.x() == x && piece.y() == y) return piece.id();
        return 0;
    }

    public static Piece piece(int id) {
        return id >= 1 && id <= MAX_PIECE ? PIECES.get(id - 1) : null;
    }

    public static List<Piece> pieces() { return PIECES; }
}
