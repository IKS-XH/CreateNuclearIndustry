package com.iksxh.create_nuclear_industry.p0probe.numeric;

/** P0 原型使用的稳定水平列坐标；不与正式 P1 的坐标类型混用。 */
public record ColumnKey(int x, int z) implements Comparable<ColumnKey> {
    @Override
    public int compareTo(ColumnKey other) {
        int xOrder = Integer.compare(x, other.x);
        return xOrder != 0 ? xOrder : Integer.compare(z, other.z);
    }
}
