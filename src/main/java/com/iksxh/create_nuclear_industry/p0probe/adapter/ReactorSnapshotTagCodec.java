package com.iksxh.create_nuclear_industry.p0probe.adapter;

import com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnKey;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnKind;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnState;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Map;
import java.util.TreeMap;

/** P0-only NBT adapter; the numeric model itself has no Minecraft dependency. */
public final class ReactorSnapshotTagCodec {
    private ReactorSnapshotTagCodec() {
    }

    public static CompoundTag save(ReactorSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("MeltdownProgress", snapshot.meltdownProgress());
        tag.putBoolean("MeltdownTriggered", snapshot.meltdownTriggered());
        tag.putLong("Tick", snapshot.tick());
        ListTag columns = new ListTag();
        snapshot.columns().values().forEach(column -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", column.key().x());
            entry.putInt("Z", column.key().z());
            entry.putString("Kind", column.kind().name());
            entry.putDouble("FuelRemaining", column.fuelRemaining());
            entry.putDouble("FuelIntegrity", column.fuelColumnIntegrity());
            entry.putDouble("ControlIntegrity", column.controlRodColumnIntegrity());
            entry.putDouble("ControlDepth", column.controlRodDepth());
            entry.putDouble("JammedDepth", column.jammedDepth());
            entry.putDouble("CachedHeat", column.cachedHeat());
            entry.putBoolean("Jammed", column.jammed());
            columns.add(entry);
        });
        tag.put("Columns", columns);
        return tag;
    }

    public static ReactorSnapshot load(CompoundTag tag) {
        Map<ColumnKey, ColumnState> columns = new TreeMap<>();
        ListTag list = tag.getList("Columns", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ColumnKey key = new ColumnKey(entry.getInt("X"), entry.getInt("Z"));
            ColumnKind kind = ColumnKind.valueOf(entry.getString("Kind"));
            ColumnState state;
            if (kind == ColumnKind.FUEL) {
                state = ColumnState.fuel(key, entry.getDouble("FuelRemaining"),
                        entry.getDouble("FuelIntegrity"), entry.getDouble("CachedHeat"));
            } else if (kind == ColumnKind.CONTROL_ROD) {
                state = new ColumnState(key, kind, 0, 0,
                        entry.getDouble("ControlIntegrity"), entry.getDouble("ControlDepth"),
                        entry.getDouble("JammedDepth"), entry.getDouble("CachedHeat"),
                        entry.getBoolean("Jammed"));
            } else {
                state = ColumnState.empty(key);
            }
            columns.put(key, state);
        }
        return new ReactorSnapshot(columns, tag.getDouble("MeltdownProgress"),
                tag.getBoolean("MeltdownTriggered"), tag.getLong("Tick"));
    }
}
