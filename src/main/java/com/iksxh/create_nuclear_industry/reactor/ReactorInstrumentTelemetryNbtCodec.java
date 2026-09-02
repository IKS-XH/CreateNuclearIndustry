package com.iksxh.create_nuclear_industry.reactor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 动态遥测的方块实体更新数据编解码器。
 *
 * <p>该格式只服务于客户端更新包，不参与方块实体持久化，也不改变
 * {@link ReactorSnapshotNbtCodec} 的版本和字段。解码任何缺失、重复或非法值时都返回不可用
 * 遥测，防止客户端把损坏包解释成有效运行状态。</p>
 */
public final class ReactorInstrumentTelemetryNbtCodec {
    public static final int FORMAT_VERSION = 1;

    private static final String FORMAT_VERSION_KEY = "FormatVersion";
    private static final String AVAILABLE_KEY = "Available";
    private static final String COLD_COOLANT_MB_KEY = "ColdCoolantMb";
    private static final String HOT_COOLANT_MB_KEY = "HotCoolantMb";
    private static final String FUEL_COLUMNS_KEY = "FuelColumns";
    private static final String CONTROL_ROD_COLUMNS_KEY = "ControlRodColumns";
    private static final String TOTAL_GENERATED_HEAT_KEY = "TotalGeneratedFissionHeatHuPerTick";
    private static final String CONVERTED_COOLANT_KEY = "ConvertedCoolantMbPerTick";

    private ReactorInstrumentTelemetryNbtCodec() {
    }

    /** 编码只读遥测更新数据；不可用状态不携带任何动态数值。 */
    public static CompoundTag encode(ReactorInstrumentTelemetry telemetry) {
        if (telemetry == null) {
            throw new IllegalArgumentException("reactor instrument telemetry is required");
        }
        CompoundTag root = new CompoundTag();
        root.putInt(FORMAT_VERSION_KEY, FORMAT_VERSION);
        root.putBoolean(AVAILABLE_KEY, telemetry.available());
        if (!telemetry.available()) {
            return root;
        }

        root.putLong(COLD_COOLANT_MB_KEY, telemetry.coldCoolantMb());
        root.putLong(HOT_COOLANT_MB_KEY, telemetry.hotCoolantMb());
        root.putDouble(TOTAL_GENERATED_HEAT_KEY, telemetry.totalGeneratedFissionHeatHuPerTick());
        root.putDouble(CONVERTED_COOLANT_KEY, telemetry.convertedCoolantMbPerTick());

        ListTag fuelColumns = new ListTag();
        for (ReactorInstrumentTelemetry.FuelColumnTelemetry column : telemetry.fuelColumns()) {
            CompoundTag entry = positionTag(column.position());
            entry.putDouble("Integrity", column.fuelColumnIntegrity());
            entry.putDouble("GeneratedFissionHeatHuPerTick",
                    column.generatedFissionHeatHuPerTick());
            fuelColumns.add(entry);
        }
        root.put(FUEL_COLUMNS_KEY, fuelColumns);

        ListTag controlRodColumns = new ListTag();
        for (ReactorInstrumentTelemetry.ControlRodColumnTelemetry column
                : telemetry.controlRodColumns()) {
            CompoundTag entry = positionTag(column.position());
            entry.putDouble("Integrity", column.controlRodColumnIntegrity());
            controlRodColumns.add(entry);
        }
        root.put(CONTROL_ROD_COLUMNS_KEY, controlRodColumns);
        return root;
    }

    /** 解码客户端更新数据；格式不兼容或数据非法时返回明确不可用状态。 */
    public static ReactorInstrumentTelemetry decode(CompoundTag root) {
        if (root == null || !root.getBoolean(AVAILABLE_KEY)
                || root.getInt(FORMAT_VERSION_KEY) != FORMAT_VERSION) {
            return ReactorInstrumentTelemetry.unavailable("telemetry is not available");
        }
        try {
            if (!root.contains(COLD_COOLANT_MB_KEY, Tag.TAG_LONG)
                    || !root.contains(HOT_COOLANT_MB_KEY, Tag.TAG_LONG)
                    || !root.contains(TOTAL_GENERATED_HEAT_KEY, Tag.TAG_DOUBLE)
                    || !root.contains(CONVERTED_COOLANT_KEY, Tag.TAG_DOUBLE)
                    || !root.contains(FUEL_COLUMNS_KEY, Tag.TAG_LIST)
                    || !root.contains(CONTROL_ROD_COLUMNS_KEY, Tag.TAG_LIST)) {
                return ReactorInstrumentTelemetry.unavailable("telemetry data is incomplete");
            }

            List<ReactorInstrumentTelemetry.FuelColumnTelemetry> fuelColumns = new ArrayList<>();
            Set<CoreColumnPosition> fuelPositions = new HashSet<>();
            ListTag fuelList = root.getList(FUEL_COLUMNS_KEY, Tag.TAG_COMPOUND);
            for (int index = 0; index < fuelList.size(); index++) {
                CompoundTag entry = fuelList.getCompound(index);
                CoreColumnPosition position = readPosition(entry);
                if (position == null || !fuelPositions.add(position)
                        || !entry.contains("Integrity", Tag.TAG_DOUBLE)
                        || !entry.contains("GeneratedFissionHeatHuPerTick", Tag.TAG_DOUBLE)) {
                    return ReactorInstrumentTelemetry.unavailable("fuel telemetry data is invalid");
                }
                fuelColumns.add(new ReactorInstrumentTelemetry.FuelColumnTelemetry(
                        position,
                        entry.getDouble("Integrity"),
                        entry.getDouble("GeneratedFissionHeatHuPerTick")
                ));
            }

            List<ReactorInstrumentTelemetry.ControlRodColumnTelemetry> controlRodColumns =
                    new ArrayList<>();
            Set<CoreColumnPosition> controlPositions = new HashSet<>();
            ListTag controlList = root.getList(CONTROL_ROD_COLUMNS_KEY, Tag.TAG_COMPOUND);
            for (int index = 0; index < controlList.size(); index++) {
                CompoundTag entry = controlList.getCompound(index);
                CoreColumnPosition position = readPosition(entry);
                if (position == null || !controlPositions.add(position)
                        || fuelPositions.contains(position)
                        || !entry.contains("Integrity", Tag.TAG_DOUBLE)) {
                    return ReactorInstrumentTelemetry.unavailable("control rod telemetry data is invalid");
                }
                controlRodColumns.add(new ReactorInstrumentTelemetry.ControlRodColumnTelemetry(
                        position, entry.getDouble("Integrity")));
            }

            return new ReactorInstrumentTelemetry(
                    true,
                    "",
                    readNonNegativeLong(root, COLD_COOLANT_MB_KEY),
                    readNonNegativeLong(root, HOT_COOLANT_MB_KEY),
                    fuelColumns,
                    controlRodColumns,
                    root.getDouble(TOTAL_GENERATED_HEAT_KEY),
                    root.getDouble(CONVERTED_COOLANT_KEY)
            );
        } catch (RuntimeException exception) {
            return ReactorInstrumentTelemetry.unavailable("telemetry synchronization data is invalid");
        }
    }

    private static CompoundTag positionTag(CoreColumnPosition position) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", position.x());
        tag.putInt("Z", position.z());
        return tag;
    }

    private static CoreColumnPosition readPosition(CompoundTag tag) {
        if (!tag.contains("X", Tag.TAG_INT) || !tag.contains("Z", Tag.TAG_INT)) {
            return null;
        }
        try {
            return new CoreColumnPosition(tag.getInt("X"), tag.getInt("Z"));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static long readNonNegativeLong(CompoundTag tag, String key) {
        long value = tag.getLong(key);
        if (value < 0L) {
            throw new IllegalArgumentException("telemetry inventory must be non-negative");
        }
        return value;
    }
}
