package com.iksxh.create_nuclear_industry.reactor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Map;
import java.util.TreeMap;

/** P1 反应堆权威快照的版本化 NBT 适配器。 */
public final class ReactorSnapshotNbtCodec {
    public static final int FORMAT_VERSION = 3;

    private static final String FORMAT_VERSION_KEY = "FormatVersion";
    private static final String FUEL_COLUMNS_KEY = "FuelColumns";
    private static final String CONTROL_COLUMNS_KEY = "ControlRodColumns";
    private static final String SCRAM_SAVED_TARGETS_KEY = "ScramSavedTargets";

    private ReactorSnapshotNbtCodec() {
    }

    public static CompoundTag encode(ReactorSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("reactor snapshot is required");
        }
        CompoundTag root = new CompoundTag();
        root.putInt(FORMAT_VERSION_KEY, FORMAT_VERSION);
        root.putLong("ColdCoolantMb", snapshot.coldCoolantMb());
        root.putLong("HotCoolantMb", snapshot.hotCoolantMb());
        root.putLong("MeltdownProgressTicks", snapshot.meltdownProgressTicks());
        root.putBoolean("MeltdownCountdownStarted", snapshot.meltdownCountdownStarted());
        root.putBoolean("ScramRequested", snapshot.scramRequested());

        ListTag fuelColumns = new ListTag();
        snapshot.fuelColumns().forEach((position, state) -> {
            CompoundTag entry = positionTag(position);
            CompoundTag assembly = new CompoundTag();
            assembly.putBoolean("Present", state.fuelAssembly().present());
            assembly.putInt("Damage", state.fuelAssembly().damage());
            assembly.putInt("MaxDamage", state.fuelAssembly().maxDamage());
            entry.put("Assembly", assembly);
            entry.putDouble("Integrity", state.integrity());
            entry.putDouble("CachedHeatHu", state.cachedHeatHu());
            entry.putDouble("FuelBurnRemainder", state.fuelBurnRemainder());
            entry.putDouble("QuantizedHeatRemainderHu", state.quantizedHeatRemainderHu());
            fuelColumns.add(entry);
        });
        root.put(FUEL_COLUMNS_KEY, fuelColumns);

        ListTag controlColumns = new ListTag();
        snapshot.controlRodColumns().forEach((position, state) -> {
            CompoundTag entry = positionTag(position);
            entry.putDouble("Integrity", state.integrity());
            entry.putDouble("TargetDepth", state.targetDepth());
            entry.putDouble("ActualDepth", state.actualDepth());
            entry.putBoolean("Jammed", state.jammed());
            entry.putDouble("CachedHeatHu", state.cachedHeatHu());
            controlColumns.add(entry);
        });
        root.put(CONTROL_COLUMNS_KEY, controlColumns);

        ListTag scramTargets = new ListTag();
        snapshot.scramSavedTargetDepths().forEach((position, targetDepth) -> {
            CompoundTag entry = positionTag(position);
            entry.putDouble("TargetDepth", targetDepth);
            scramTargets.add(entry);
        });
        root.put(SCRAM_SAVED_TARGETS_KEY, scramTargets);
        return root;
    }

    public static ReactorSnapshot decode(CompoundTag root) {
        if (root == null) {
            return ReactorSnapshot.empty();
        }
        int formatVersion = root.contains(FORMAT_VERSION_KEY)
                ? root.getInt(FORMAT_VERSION_KEY) : 0;
        Map<CoreColumnPosition, FuelColumnState> fuelColumns = new TreeMap<>();
        ListTag fuelList = root.getList(FUEL_COLUMNS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < fuelList.size(); index++) {
            CompoundTag entry = fuelList.getCompound(index);
            CoreColumnPosition position = readPosition(entry);
            if (position == null) {
                continue;
            }
            CompoundTag assemblyTag = entry.getCompound("Assembly");
            int maxDamage = Math.max(0, assemblyTag.getInt("MaxDamage"));
            boolean present = assemblyTag.getBoolean("Present") || maxDamage > 0;
            FuelAssemblyState assembly;
            if (!present || maxDamage == 0) {
                assembly = FuelAssemblyState.empty();
            } else {
                int damage = Math.max(0, Math.min(maxDamage, assemblyTag.getInt("Damage")));
                assembly = FuelAssemblyState.installed(maxDamage, damage);
            }
            double cachedHeat = readNonNegative(entry, "CachedHeatHu", 0.0D);
            double quantizedHeat = formatVersion >= 3
                    ? Math.min(cachedHeat, readNonNegative(entry,
                    "QuantizedHeatRemainderHu", 0.0D)) : 0.0D;
            fuelColumns.put(position, new FuelColumnState(
                    assembly,
                    readUnit(entry, "Integrity", 1.0D),
                    cachedHeat,
                    readUnit(entry, "FuelBurnRemainder", 0.0D),
                    quantizedHeat
            ));
        }

        Map<CoreColumnPosition, ControlRodColumnState> controlColumns = new TreeMap<>();
        ListTag controlList = root.getList(CONTROL_COLUMNS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < controlList.size(); index++) {
            CompoundTag entry = controlList.getCompound(index);
            CoreColumnPosition position = readPosition(entry);
            if (position == null || fuelColumns.containsKey(position)) {
                continue;
            }
            controlColumns.put(position, new ControlRodColumnState(
                    readUnit(entry, "Integrity", 1.0D),
                    readUnit(entry, "TargetDepth", 1.0D),
                    readUnit(entry, "ActualDepth", 1.0D),
                    entry.getBoolean("Jammed"),
                    readNonNegative(entry, "CachedHeatHu", 0.0D)
            ));
        }

        long coldCoolant = Math.max(0L, root.getLong("ColdCoolantMb"));
        long hotCoolant = Math.max(0L, root.getLong("HotCoolantMb"));
        long progress = Math.max(0L, root.getLong("MeltdownProgressTicks"));
        boolean started = root.getBoolean("MeltdownCountdownStarted") || progress > 0L;
        boolean scramRequested = root.getBoolean("ScramRequested");
        Map<CoreColumnPosition, Double> scramSavedTargets = new TreeMap<>();
        ListTag scramTargetList = root.getList(SCRAM_SAVED_TARGETS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < scramTargetList.size(); index++) {
            CompoundTag entry = scramTargetList.getCompound(index);
            CoreColumnPosition position = readPosition(entry);
            if (position != null && controlColumns.containsKey(position)) {
                scramSavedTargets.put(position, readUnit(entry, "TargetDepth", 1.0D));
            }
        }
        if (!scramRequested) {
            scramSavedTargets.clear();
        }
        return new ReactorSnapshot(
                fuelColumns,
                controlColumns,
                coldCoolant,
                hotCoolant,
                progress,
                started,
                scramSavedTargets,
                scramRequested
        );
    }

    private static CompoundTag positionTag(CoreColumnPosition position) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("X", position.x());
        tag.putInt("Z", position.z());
        return tag;
    }

    private static CoreColumnPosition readPosition(CompoundTag tag) {
        if (!tag.contains("X") || !tag.contains("Z")) {
            return null;
        }
        try {
            return new CoreColumnPosition(tag.getInt("X"), tag.getInt("Z"));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static double readUnit(CompoundTag tag, String key, double defaultValue) {
        if (!tag.contains(key)) {
            return defaultValue;
        }
        double value = tag.getDouble(key);
        if (!Double.isFinite(value)) {
            return defaultValue;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static double readNonNegative(CompoundTag tag, String key, double defaultValue) {
        if (!tag.contains(key)) {
            return defaultValue;
        }
        double value = tag.getDouble(key);
        return Double.isFinite(value) ? Math.max(0.0D, value) : defaultValue;
    }
}
