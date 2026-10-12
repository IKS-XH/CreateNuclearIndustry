package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;

/**
 * 权威结构与纯模拟共同使用的不可变外尺寸，单位为方块；宽、高、深分别对应 X、Y、Z。
 * 三边严格限制为5～16，列只占内部水平截面，内部高度统一为外高减2。
 */
public record ReactorDimensions(int width, int height, int depth) {
    public static final int MIN_DIMENSION = 5;
    public static final int MAX_DIMENSION = 16;
    public static final ReactorDimensions DEFAULT = new ReactorDimensions(5, 5, 5);

    public ReactorDimensions {
        if (!validDimension(width) || !validDimension(height) || !validDimension(depth)) {
            throw new IllegalArgumentException("reactor dimensions must each be in [5, 16]");
        }
    }

    public int internalWidth() { return width - 2; }
    public int internalHeight() { return height - 2; }
    public int internalDepth() { return depth - 2; }
    public int columnCount() { return internalWidth() * internalDepth(); }
    public boolean containsColumn(CoreColumnPosition p) {
        return p != null && p.x() < internalWidth() && p.z() < internalDepth();
    }
    public boolean contains(ReactorStructureDefinition.LocalPosition p) {
        return p.x() >= 0 && p.x() < width && p.y() >= 0 && p.y() < height && p.z() >= 0 && p.z() < depth;
    }
    public boolean boundary(ReactorStructureDefinition.LocalPosition p) {
        return contains(p) && (p.x() == 0 || p.x() == width - 1 || p.y() == 0 || p.y() == height - 1 || p.z() == 0 || p.z() == depth - 1);
    }
    public boolean sideSlot(ReactorStructureDefinition.LocalPosition p) {
        return contains(p) && p.y() > 0 && p.y() < height - 1
                && ((p.x() == 0 || p.x() == width - 1) && p.z() > 0 && p.z() < depth - 1
                || (p.z() == 0 || p.z() == depth - 1) && p.x() > 0 && p.x() < width - 1);
    }
    public boolean columnCap(ReactorStructureDefinition.LocalPosition p) {
        return contains(p) && p.y() == height - 1 && p.x() > 0 && p.x() < width - 1 && p.z() > 0 && p.z() < depth - 1;
    }
    private static boolean validDimension(int value) { return value >= MIN_DIMENSION && value <= MAX_DIMENSION; }
}
