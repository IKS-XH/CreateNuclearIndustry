package com.iksxh.create_nuclear_industry.p0probe.numeric;

import java.util.Map;
import java.util.TreeMap;

/** N-14 使用的纯 Java 确定性快照编解码器；NBT 适配器独立存在，均属 P0 回归依据。 */
public final class ReactorSnapshotCodec {
    private ReactorSnapshotCodec() {
    }

    /** 将 P0 快照编码为稳定行格式字符串。 */
    public static String encode(ReactorSnapshot snapshot) {
        StringBuilder result = new StringBuilder()
                .append(snapshot.meltdownProgress()).append('|')
                .append(snapshot.meltdownTriggered()).append('|')
                .append(snapshot.tick()).append('\n');
        snapshot.columns().values().forEach(column -> result.append(column.key().x()).append(',')
                .append(column.key().z()).append(',').append(column.kind()).append(',')
                .append(column.fuelRemaining()).append(',').append(column.fuelColumnIntegrity()).append(',')
                .append(column.controlRodColumnIntegrity()).append(',').append(column.controlRodDepth()).append(',')
                .append(column.jammedDepth()).append(',').append(column.cachedHeat()).append(',')
                .append(column.jammed()).append('\n'));
        return result.toString();
    }

    /** 从稳定行格式字符串恢复 P0 快照。 */
    public static ReactorSnapshot decode(String encoded) {
        String[] lines = encoded.split("\\n");
        String[] header = lines[0].split("\\|");
        Map<ColumnKey, ColumnState> columns = new TreeMap<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String[] fields = lines[i].split(",");
            ColumnKey key = new ColumnKey(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]));
            ColumnKind kind = ColumnKind.valueOf(fields[2]);
            ColumnState state = kind == ColumnKind.FUEL
                    ? ColumnState.fuel(key, Double.parseDouble(fields[3]), Double.parseDouble(fields[4]),
                    Double.parseDouble(fields[8]))
                    : kind == ColumnKind.CONTROL_ROD
                    ? new ColumnState(key, kind, 0, 0, Double.parseDouble(fields[5]),
                    Double.parseDouble(fields[6]), Double.parseDouble(fields[7]),
                    Double.parseDouble(fields[8]), Boolean.parseBoolean(fields[9]))
                    : ColumnState.empty(key);
            columns.put(key, state);
        }
        return new ReactorSnapshot(columns, Double.parseDouble(header[0]), Boolean.parseBoolean(header[1]),
                Long.parseLong(header[2]));
    }
}
