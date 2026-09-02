package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证仪表端口静态摘要的字段来源、端口去重、容量覆盖和失效边界。 */
class ReactorInstrumentStructureSummaryTest {
    @Test
    void canonicalStructureProducesFixedDimensionsAndExpectedCounts() {
        ReactorInstrumentStructureSummary summary = ReactorInstrumentStructureSummary.from(
                scan(ReactorStructureDefinition.canonicalTemplate()), 1_000L, 1_000L);

        assertTrue(summary.valid());
        assertEquals("", summary.unavailableReason());
        assertEquals(5, summary.width());
        assertEquals(5, summary.height());
        assertEquals(5, summary.depth());
        assertEquals(8, summary.fuelColumnCount());
        assertEquals(0, summary.controlRodColumnCount());
        assertEquals(1, summary.coldPortCount());
        assertEquals(1, summary.hotPortCount());
        assertEquals(1_000L, summary.coldInventoryCapacityMb());
        assertEquals(1_000L, summary.hotInventoryCapacityMb());
        assertEquals(2_000L, summary.totalFluidCapacityMb());
    }

    @Test
    void controlRodAndAdditionalPortsAreCountedFromTheCachedLayout() {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout =
                new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        layout.put(new CoreColumnPosition(1, 0), ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        Map<ReactorStructureDefinition.LocalPosition, String> blocks =
                new HashMap<>(ReactorStructureDefinition.templateFor(layout));
        putPort(blocks, new ReactorStructureDefinition.LocalPosition(2, 1, 4),
                ReactorStructureDefinition.PortType.COLD_COOLANT);
        putPort(blocks, new ReactorStructureDefinition.LocalPosition(2, 3, 4),
                ReactorStructureDefinition.PortType.HOT_COOLANT);

        ReactorInstrumentStructureSummary summary = ReactorInstrumentStructureSummary.from(
                scan(blocks), 1_500L, 750L);

        assertTrue(summary.valid());
        assertEquals(7, summary.fuelColumnCount());
        assertEquals(1, summary.controlRodColumnCount());
        assertEquals(2, summary.coldPortCount());
        assertEquals(2, summary.hotPortCount());
        assertEquals(1_500L, summary.coldInventoryCapacityMb());
        assertEquals(750L, summary.hotInventoryCapacityMb());
        assertEquals(2_250L, summary.totalFluidCapacityMb());
    }

    @Test
    void duplicatePhysicalPortPositionsCountOnlyOnce() {
        ReactorStructureDefinition.ScanResult valid = scan(
                ReactorStructureDefinition.canonicalTemplate());
        Map<ReactorStructureDefinition.PortType, java.util.List<ReactorStructureDefinition.LocalPosition>> ports =
                new java.util.EnumMap<>(valid.ports());
        ReactorStructureDefinition.LocalPosition cold =
                ports.get(ReactorStructureDefinition.PortType.COLD_COOLANT).get(0);
        ReactorStructureDefinition.LocalPosition hot =
                ports.get(ReactorStructureDefinition.PortType.HOT_COOLANT).get(0);
        ports.put(ReactorStructureDefinition.PortType.COLD_COOLANT, java.util.List.of(cold, cold));
        ports.put(ReactorStructureDefinition.PortType.HOT_COOLANT, java.util.List.of(hot, hot));
        ReactorStructureDefinition.ScanResult duplicated = new ReactorStructureDefinition.ScanResult(
                true, "", ReactorStructureDefinition.DiagnosticCode.VALID,
                valid.columns(), ports);

        ReactorInstrumentStructureSummary summary = ReactorInstrumentStructureSummary.from(
                duplicated, 1_000L, 1_000L);

        assertEquals(1, summary.coldPortCount());
        assertEquals(1, summary.hotPortCount());
    }

    @Test
    void invalidStructureIsUnavailableInsteadOfAValidZeroSummary() {
        Map<ReactorStructureDefinition.LocalPosition, String> blocks =
                new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        blocks.put(ReactorStructureDefinition.DEFAULT_COLD_PORT_POSITION,
                "create_nuclear_industry:reactor_casing");

        ReactorInstrumentStructureSummary summary = ReactorInstrumentStructureSummary.from(
                scan(blocks), 1_000L, 1_000L);

        assertFalse(summary.valid());
        assertFalse(summary.unavailableReason().isBlank());
        assertEquals(0, summary.width());
        assertEquals(0L, summary.totalFluidCapacityMb());
    }

    @Test
    void capacityInputsMustBeNonNegative() {
        ReactorStructureDefinition.ScanResult scan = scan(
                ReactorStructureDefinition.canonicalTemplate());

        assertThrows(IllegalArgumentException.class,
                () -> ReactorInstrumentStructureSummary.from(scan, -1L, 1_000L));
        assertThrows(IllegalArgumentException.class,
                () -> ReactorInstrumentStructureSummary.from(scan, 1_000L, -1L));
    }

    @Test
    void validSummaryRoundTripsThroughClientUpdateTag() {
        ReactorInstrumentStructureSummary source = ReactorInstrumentStructureSummary.from(
                scan(ReactorStructureDefinition.canonicalTemplate()), 1_500L, 750L);

        ReactorInstrumentStructureSummary decoded =
                ReactorInstrumentStructureSummary.readSyncTag(source.writeSyncTag());

        assertEquals(source, decoded);
    }

    @Test
    void invalidOrMalformedClientUpdateCannotBecomeAValidZeroSummary() {
        ReactorInstrumentStructureSummary invalid =
                ReactorInstrumentStructureSummary.unavailable("not formed");
        assertFalse(ReactorInstrumentStructureSummary.readSyncTag(invalid.writeSyncTag()).valid());

        CompoundTag malformed = new CompoundTag();
        malformed.putBoolean("Valid", true);
        assertFalse(ReactorInstrumentStructureSummary.readSyncTag(malformed).valid());
    }

    private static ReactorStructureDefinition.ScanResult scan(
            Map<ReactorStructureDefinition.LocalPosition, String> blocks
    ) {
        return ReactorStructureDefinition.scan(blocks);
    }

    private static void putPort(
            Map<ReactorStructureDefinition.LocalPosition, String> blocks,
            ReactorStructureDefinition.LocalPosition position,
            ReactorStructureDefinition.PortType type
    ) {
        String id = switch (type) {
            case COLD_COOLANT -> "create_nuclear_industry:reactor_cold_port";
            case HOT_COOLANT -> "create_nuclear_industry:reactor_hot_port";
            case INSTRUMENT -> "create_nuclear_industry:reactor_instrument_port";
        };
        blocks.put(position, id);
    }
}
