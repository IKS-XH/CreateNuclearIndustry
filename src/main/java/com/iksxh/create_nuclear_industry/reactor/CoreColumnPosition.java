package com.iksxh.create_nuclear_industry.reactor;

import java.util.ArrayList;
import java.util.List;

/**
 * Zero-based position in the fixed P1 reactor's 3 x 3 interior column grid.
 * World and structure coordinates are intentionally left to the structure adapter.
 */
public record CoreColumnPosition(int x, int z) implements Comparable<CoreColumnPosition> {
    public static final int GRID_SIZE = 3;

    public CoreColumnPosition {
        if (x < 0 || x >= GRID_SIZE || z < 0 || z >= GRID_SIZE) {
            throw new IllegalArgumentException("core column coordinates must be in [0, 2]");
        }
    }

    public List<CoreColumnPosition> cardinalNeighbours() {
        List<CoreColumnPosition> neighbours = new ArrayList<>(4);
        addIfInside(neighbours, x, z - 1);
        addIfInside(neighbours, x, z + 1);
        addIfInside(neighbours, x - 1, z);
        addIfInside(neighbours, x + 1, z);
        return List.copyOf(neighbours);
    }

    @Override
    public int compareTo(CoreColumnPosition other) {
        int xOrder = Integer.compare(x, other.x);
        return xOrder != 0 ? xOrder : Integer.compare(z, other.z);
    }

    private static void addIfInside(List<CoreColumnPosition> positions, int candidateX, int candidateZ) {
        if (candidateX >= 0 && candidateX < GRID_SIZE && candidateZ >= 0 && candidateZ < GRID_SIZE) {
            positions.add(new CoreColumnPosition(candidateX, candidateZ));
        }
    }
}
