package com.iksxh.create_nuclear_industry.reactor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证正式快照 NBT 字段、旧字段默认值、未知字段容忍和 SCRAM 目标往返。 */
class ReactorSnapshotNbtCodecTest {
    @Test
    void roundTripsCompleteDamagedPausedAndJammedSnapshot() {
        Map<CoreColumnPosition, FuelColumnState> fuels = new LinkedHashMap<>();
        Map<CoreColumnPosition, ControlRodColumnState> controls = new LinkedHashMap<>();
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                CoreColumnPosition position = new CoreColumnPosition(x, z);
                if ((x + z) % 2 == 0) {
                    fuels.put(position, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, (x * 3 + z) * 10_000),
                            Math.max(0.0D, 1.0D - (x * 3 + z) * 0.1D),
                            x * 10.0D + z
                    ));
                } else {
                    boolean jammed = x == 1 && z == 0;
                    controls.put(position, new ControlRodColumnState(
                            jammed ? 0.0D : 0.8D,
                            0.75D,
                            jammed ? 0.35D : 0.5D,
                            jammed,
                            x + z
                    ));
                }
            }
        }
        ReactorSnapshot expected = new ReactorSnapshot(fuels, controls, 7_500L, 2_500L, 321L, true);

        CompoundTag encoded = ReactorSnapshotNbtCodec.encode(expected);
        ReactorSnapshot decoded = ReactorSnapshotNbtCodec.decode(encoded);

        assertEquals(ReactorSnapshotNbtCodec.FORMAT_VERSION, encoded.getInt("FormatVersion"));
        assertEquals(expected, decoded);
        CompoundTag firstFuel = encoded.getList("FuelColumns", Tag.TAG_COMPOUND).getCompound(0);
        assertFalse(firstFuel.getCompound("Assembly").contains("Integrity"),
                "fuel assembly durability must not contain column integrity");
    }

    @Test
    void listOrderAndCompoundFieldOrderDoNotChangeDecodedSnapshot() {
        ReactorSnapshot expected = new ReactorSnapshot(
                Map.of(
                        new CoreColumnPosition(0, 0), fuel(0.8D, 2.0D),
                        new CoreColumnPosition(2, 2), fuel(0.6D, 4.0D)
                ),
                Map.of(new CoreColumnPosition(1, 1),
                        new ControlRodColumnState(0.5D, 0.9D, 0.4D, false, 3.0D)),
                100L,
                50L,
                10L,
                true
        );
        CompoundTag encoded = ReactorSnapshotNbtCodec.encode(expected);
        ListTag originalFuel = encoded.getList("FuelColumns", Tag.TAG_COMPOUND);
        ListTag reversedFuel = new ListTag();
        for (int index = originalFuel.size() - 1; index >= 0; index--) {
            reversedFuel.add(originalFuel.getCompound(index));
        }
        CompoundTag reordered = new CompoundTag();
        reordered.put("ControlRodColumns", encoded.getList("ControlRodColumns", Tag.TAG_COMPOUND));
        reordered.putBoolean("MeltdownCountdownStarted", true);
        reordered.putLong("MeltdownProgressTicks", 10L);
        reordered.putLong("HotCoolantMb", 50L);
        reordered.put("FuelColumns", reversedFuel);
        reordered.putLong("ColdCoolantMb", 100L);
        reordered.putInt("FormatVersion", ReactorSnapshotNbtCodec.FORMAT_VERSION);

        assertEquals(expected, ReactorSnapshotNbtCodec.decode(reordered));
    }

    @Test
    void missingFieldsUseSafeDefaultsAndUnknownFieldsAreIgnored() {
        CompoundTag root = new CompoundTag();
        root.putInt("FormatVersion", 0);
        root.putString("UnknownFutureField", "ignored");
        ListTag fuels = new ListTag();
        CompoundTag fuel = new CompoundTag();
        fuel.putInt("X", 0);
        fuel.putInt("Z", 0);
        fuel.putString("UnknownFuelField", "ignored");
        fuels.add(fuel);
        root.put("FuelColumns", fuels);
        ListTag controls = new ListTag();
        CompoundTag control = new CompoundTag();
        control.putInt("X", 1);
        control.putInt("Z", 0);
        controls.add(control);
        root.put("ControlRodColumns", controls);

        ReactorSnapshot decoded = ReactorSnapshotNbtCodec.decode(root);

        assertEquals(FuelColumnState.empty(), decoded.fuelColumns().get(new CoreColumnPosition(0, 0)));
        assertEquals(ControlRodColumnState.fullyInserted(),
                decoded.controlRodColumns().get(new CoreColumnPosition(1, 0)));
        assertEquals(0L, decoded.coldCoolantMb());
        assertEquals(0L, decoded.hotCoolantMb());
        assertEquals(0L, decoded.meltdownProgressTicks());
        assertFalse(decoded.meltdownCountdownStarted());
    }

    @Test
    void legacyFormatWithoutFuelBurnRemainderMigratesToZero() {
        CompoundTag root = new CompoundTag();
        root.putInt("FormatVersion", 1);
        ListTag fuels = new ListTag();
        CompoundTag fuel = new CompoundTag();
        fuel.putInt("X", 0);
        fuel.putInt("Z", 0);
        CompoundTag assembly = new CompoundTag();
        assembly.putBoolean("Present", true);
        assembly.putInt("MaxDamage", 216_000);
        assembly.putInt("Damage", 12);
        fuel.put("Assembly", assembly);
        fuel.putDouble("Integrity", 0.8D);
        fuel.putDouble("CachedHeatHu", 2.0D);
        fuels.add(fuel);
        root.put("FuelColumns", fuels);

        ReactorSnapshot decoded = ReactorSnapshotNbtCodec.decode(root);

        assertEquals(0.0D,
                decoded.fuelColumns().get(new CoreColumnPosition(0, 0)).fuelBurnRemainder(),
                1.0E-12D);
    }

    @Test
    void invalidLegacyValuesAreClampedOrSkippedSafely() {
        CompoundTag root = new CompoundTag();
        root.putLong("ColdCoolantMb", -100L);
        root.putLong("HotCoolantMb", -100L);
        root.putLong("MeltdownProgressTicks", 4L);
        ListTag fuels = new ListTag();
        CompoundTag valid = new CompoundTag();
        valid.putInt("X", 0);
        valid.putInt("Z", 0);
        valid.putDouble("Integrity", 2.0D);
        valid.putDouble("CachedHeatHu", -3.0D);
        CompoundTag assembly = new CompoundTag();
        assembly.putBoolean("Present", true);
        assembly.putInt("MaxDamage", 100);
        assembly.putInt("Damage", 200);
        valid.put("Assembly", assembly);
        fuels.add(valid);
        CompoundTag outside = new CompoundTag();
        outside.putInt("X", 9);
        outside.putInt("Z", 9);
        fuels.add(outside);
        root.put("FuelColumns", fuels);

        ReactorSnapshot decoded = ReactorSnapshotNbtCodec.decode(root);
        FuelColumnState column = decoded.fuelColumns().get(new CoreColumnPosition(0, 0));

        assertEquals(1.0D, column.integrity(), 1.0E-12D);
        assertEquals(0.0D, column.cachedHeatHu(), 1.0E-12D);
        assertTrue(column.fuelAssembly().exhausted());
        assertEquals(1, decoded.fuelColumns().size());
        assertEquals(0L, decoded.coldCoolantMb());
        assertEquals(0L, decoded.hotCoolantMb());
        assertEquals(4L, decoded.meltdownProgressTicks());
        assertTrue(decoded.meltdownCountdownStarted(), "positive legacy progress implies a started countdown");
    }

    @Test
    void scramRequestAndSavedTargetsRoundTripWithoutCreatingASecondStateOwner() {
        CoreColumnPosition position = new CoreColumnPosition(1, 1);
        ReactorSnapshot expected = new ReactorSnapshot(
                Map.of(),
                Map.of(position, new ControlRodColumnState(1.0D, 1.0D, 1.0D, false, 0.0D)),
                0L,
                0L,
                0L,
                false,
                Map.of(position, 0.35D),
                true
        );

        ReactorSnapshot decoded = ReactorSnapshotNbtCodec.decode(ReactorSnapshotNbtCodec.encode(expected));

        assertTrue(decoded.scramRequested());
        assertTrue(decoded.scramActive());
        assertEquals(Map.of(position, 0.35D), decoded.scramSavedTargetDepths());
    }

    private static FuelColumnState fuel(double integrity, double cachedHeat) {
        return new FuelColumnState(FuelAssemblyState.installed(216_000, 10_000), integrity, cachedHeat);
    }
}
