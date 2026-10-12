package com.iksxh.create_nuclear_industry.reactor;

import java.util.ArrayList;
import java.util.List;

/**
 * 反应堆内部列网格中的零基坐标，工程最大内径为14。
 * 实际矩形边界由快照尺寸校验；默认三格常量只供原五格示例使用。
 * 世界坐标与结构局部坐标故意留给结构适配器处理。
 */
public record CoreColumnPosition(int x, int z) implements Comparable<CoreColumnPosition> {
    public static final int GRID_SIZE = 3;
    public static final int MAX_GRID_SIZE = 14;

    public CoreColumnPosition {
        if (x < 0 || x >= MAX_GRID_SIZE || z < 0 || z >= MAX_GRID_SIZE) {
            throw new IllegalArgumentException("core column coordinates must be in [0, 13]");
        }
    }

    /** 按北、南、西、东顺序返回网格内的四向邻列，不跨越边界。 */
    public List<CoreColumnPosition> cardinalNeighbours() {
        return cardinalNeighbours(com.iksxh.create_nuclear_industry.structure.ReactorDimensions.DEFAULT);
    }

    /** 按实际矩形边界生成四向邻列；邻接只按列一次，绝不跨行或跨结构。 */
    public List<CoreColumnPosition> cardinalNeighbours(com.iksxh.create_nuclear_industry.structure.ReactorDimensions dimensions) {
        List<CoreColumnPosition> neighbours = new ArrayList<>(4);
        addIfInside(neighbours, x, z - 1, dimensions);
        addIfInside(neighbours, x, z + 1, dimensions);
        addIfInside(neighbours, x - 1, z, dimensions);
        addIfInside(neighbours, x + 1, z, dimensions);
        return List.copyOf(neighbours);
    }

    @Override
    public int compareTo(CoreColumnPosition other) {
        int xOrder = Integer.compare(x, other.x);
        return xOrder != 0 ? xOrder : Integer.compare(z, other.z);
    }

    private static void addIfInside(List<CoreColumnPosition> positions, int candidateX, int candidateZ,
            com.iksxh.create_nuclear_industry.structure.ReactorDimensions dimensions) {
        if (candidateX >= 0 && candidateX < dimensions.internalWidth() && candidateZ >= 0 && candidateZ < dimensions.internalDepth()) {
            positions.add(new CoreColumnPosition(candidateX, candidateZ));
        }
    }
}
