package com.iksxh.create_nuclear_industry.p0probe.numeric;

/** Stable horizontal coordinate used by the P0 prototype. */
public record ColumnKey(int x, int z) implements Comparable<ColumnKey> {
    @Override
    public int compareTo(ColumnKey other) {
        int xOrder = Integer.compare(x, other.x);
        return xOrder != 0 ? xOrder : Integer.compare(z, other.z);
    }
}
