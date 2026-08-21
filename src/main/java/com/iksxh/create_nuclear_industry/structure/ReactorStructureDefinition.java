package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Loader-independent coordinate contract for the first P1 reactor.
 *
 * <p>The local origin is the lower north-west corner of the 5 x 5 x 5
 * structure. X increases east, Y increases upward and Z increases south.
 * This class deliberately operates on namespaced block IDs instead of
 * {@code BlockState}; the world-facing scanner in STRUCT-03 can therefore
 * adapt a level without changing this contract.</p>
 */
public final class ReactorStructureDefinition {
    public static final String STRUCTURE_ID = P1ContentIds.EXPERIMENTAL_REACTOR_ID;
    public static final String AIR_ID = "minecraft:air";

    public static final int SIZE = 5;
    public static final int INTERNAL_MIN = 1;
    public static final int INTERNAL_MAX = SIZE - 2;
    public static final int INTERNAL_HEIGHT = INTERNAL_MAX - INTERNAL_MIN + 1;

    /** Canonical template positions; scan results may contain ports at any legal side slot. */
    public static final LocalPosition DEFAULT_INSTRUMENT_PORT_POSITION = new LocalPosition(2, 2, 0);
    public static final LocalPosition DEFAULT_COLD_PORT_POSITION = new LocalPosition(1, 2, 4);
    public static final LocalPosition DEFAULT_HOT_PORT_POSITION = new LocalPosition(3, 2, 4);

    private static final Set<LocalPosition> DEFAULT_WINDOW_POSITIONS = Set.of(
            new LocalPosition(0, 2, 2),
            new LocalPosition(4, 2, 2),
            new LocalPosition(2, 2, 4)
    );

    private static final Set<LocalPosition> SIDE_PORT_SLOTS = buildSidePortSlots();
    private static final Set<LocalPosition> ALL_POSITIONS = allPositions();
    private static final Map<LocalPosition, String> FIXED_BLOCKS = fixedCasingBlocks();
    private static final Map<CoreColumnPosition, ColumnType> DEFAULT_COLUMNS = defaultColumns();

    private ReactorStructureDefinition() {
    }

    public static Set<LocalPosition> sidePortSlots() {
        return SIDE_PORT_SLOTS;
    }

    public static Set<LocalPosition> windowPositions() {
        return DEFAULT_WINDOW_POSITIONS;
    }

    public static Map<PortType, LocalPosition> defaultPortPositions() {
        EnumMap<PortType, LocalPosition> ports = new EnumMap<>(PortType.class);
        ports.put(PortType.INSTRUMENT, DEFAULT_INSTRUMENT_PORT_POSITION);
        ports.put(PortType.COLD_COOLANT, DEFAULT_COLD_PORT_POSITION);
        ports.put(PortType.HOT_COOLANT, DEFAULT_HOT_PORT_POSITION);
        return Collections.unmodifiableMap(ports);
    }

    public static Set<LocalPosition> allPositions() {
        TreeSet<LocalPosition> positions = new TreeSet<>();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    positions.add(new LocalPosition(x, y, z));
                }
            }
        }
        return Collections.unmodifiableSet(positions);
    }

    public static Map<CoreColumnPosition, ColumnType> defaultColumnLayout() {
        return DEFAULT_COLUMNS;
    }

    /**
     * Returns the documented balance-anchor layout: eight fuel columns around
     * one empty centre column. Control-rod columns are legal alternatives and
     * are covered by {@link #templateFor(Map)}.
     */
    public static Map<LocalPosition, String> canonicalTemplate() {
        return templateFor(DEFAULT_COLUMNS);
    }

    /**
     * Builds a complete test/template layout. Every 3 x 3 core coordinate
     * must be assigned exactly one role; no role is inferred from a missing
     * map entry.
     */
    public static Map<LocalPosition, String> templateFor(
            Map<CoreColumnPosition, ColumnType> columns
    ) {
        if (columns == null || columns.size() != CoreColumnPosition.GRID_SIZE * CoreColumnPosition.GRID_SIZE) {
            throw new IllegalArgumentException("all nine P1 core column roles are required");
        }

        TreeMap<CoreColumnPosition, ColumnType> orderedColumns = new TreeMap<>();
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                CoreColumnPosition position = new CoreColumnPosition(x, z);
                ColumnType type = columns.get(position);
                if (type == null) {
                    throw new IllegalArgumentException("missing P1 core column role at " + position);
                }
                orderedColumns.put(position, type);
            }
        }
        if (orderedColumns.size() != columns.size()) {
            throw new IllegalArgumentException("column roles contain an out-of-range coordinate");
        }

        Map<LocalPosition, String> blocks = new TreeMap<>();
        for (LocalPosition position : ALL_POSITIONS) {
            blocks.put(position, AIR_ID);
        }
        blocks.putAll(FIXED_BLOCKS);
        String casingId = id(P1ContentIds.REACTOR_CASING_ID);
        for (LocalPosition position : SIDE_PORT_SLOTS) {
            blocks.put(position, casingId);
        }
        for (LocalPosition position : DEFAULT_WINDOW_POSITIONS) {
            blocks.put(position, id(P1ContentIds.REACTOR_WINDOW_ID));
        }
        blocks.put(DEFAULT_INSTRUMENT_PORT_POSITION, id(P1ContentIds.REACTOR_INSTRUMENT_PORT_ID));
        blocks.put(DEFAULT_COLD_PORT_POSITION, id(P1ContentIds.REACTOR_COLD_PORT_ID));
        blocks.put(DEFAULT_HOT_PORT_POSITION, id(P1ContentIds.REACTOR_HOT_PORT_ID));

        for (Map.Entry<CoreColumnPosition, ColumnType> entry : orderedColumns.entrySet()) {
            CoreColumnPosition core = entry.getKey();
            int localX = core.x() + INTERNAL_MIN;
            int localZ = core.z() + INTERNAL_MIN;
            switch (entry.getValue()) {
                case FUEL -> {
                    for (int y = INTERNAL_MIN; y <= INTERNAL_MAX; y++) {
                        blocks.put(new LocalPosition(localX, y, localZ), id(P1ContentIds.REACTOR_FUEL_ROD_ID));
                    }
                    blocks.put(new LocalPosition(localX, SIZE - 1, localZ),
                            id(P1ContentIds.REACTOR_REFUELING_PORT_ID));
                }
                case CONTROL_ROD -> blocks.put(new LocalPosition(localX, SIZE - 1, localZ),
                        id(P1ContentIds.CONTROL_ROD_DRIVE_ID));
                case EMPTY -> blocks.put(new LocalPosition(localX, SIZE - 1, localZ),
                        id(P1ContentIds.REACTOR_CASING_ID));
            }
        }
        return Collections.unmodifiableMap(blocks);
    }

    /**
     * Scans a complete local 5 x 5 x 5 snapshot. Missing coordinates, keys
     * outside the volume, wrong casing/port/window placement and inconsistent
     * fuel/control-rod columns are all rejected.
     */
    public static ScanResult scan(Map<LocalPosition, String> blocks) {
        if (blocks == null) {
            return ScanResult.invalid(DiagnosticCode.GENERIC_FAILURE, "block snapshot is required");
        }

        TreeSet<LocalPosition> missing = new TreeSet<>(ALL_POSITIONS);
        missing.removeAll(blocks.keySet());
        if (!missing.isEmpty()) {
            return ScanResult.invalid(DiagnosticCode.STRUCTURE_BLOCKS,
                    "missing structure coordinate " + missing.first());
        }

        TreeSet<LocalPosition> extra = new TreeSet<>(blocks.keySet());
        extra.removeAll(ALL_POSITIONS);
        if (!extra.isEmpty()) {
            return ScanResult.invalid(DiagnosticCode.STRUCTURE_BLOCKS,
                    "extra structure coordinate " + extra.first());
        }

        for (Map.Entry<LocalPosition, String> entry : blocks.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                return ScanResult.invalid(DiagnosticCode.STRUCTURE_BLOCKS,
                        "blank block ID at " + entry.getKey());
            }
        }

        for (Map.Entry<LocalPosition, String> entry : blocks.entrySet()) {
            if (!SIDE_PORT_SLOTS.contains(entry.getKey()) && isPortId(entry.getValue())) {
                String expected = id(P1ContentIds.REACTOR_CASING_ID);
                DiagnosticCode code = entry.getValue().equals(id(P1ContentIds.REACTOR_INSTRUMENT_PORT_ID))
                        ? DiagnosticCode.INSTRUMENT_PORT
                        : DiagnosticCode.STRUCTURE_BLOCKS;
                return ScanResult.invalid(code, "expected " + expected + " at " + entry.getKey()
                        + ", found " + entry.getValue());
            }
        }

        for (Map.Entry<LocalPosition, String> entry : FIXED_BLOCKS.entrySet()) {
            String actual = blocks.get(entry.getKey());
            if (!entry.getValue().equals(actual)) {
                DiagnosticCode code = isPortId(actual)
                        && actual.equals(id(P1ContentIds.REACTOR_INSTRUMENT_PORT_ID))
                        ? DiagnosticCode.INSTRUMENT_PORT
                        : DiagnosticCode.STRUCTURE_BLOCKS;
                return ScanResult.invalid(code, "expected " + entry.getValue() + " at " + entry.getKey()
                        + ", found " + actual);
            }
        }

        EnumMap<PortType, List<LocalPosition>> ports = new EnumMap<>(PortType.class);
        for (PortType type : PortType.values()) {
            ports.put(type, new ArrayList<>());
        }
        for (LocalPosition position : SIDE_PORT_SLOTS) {
            String actual = blocks.get(position);
            if (actual.equals(id(P1ContentIds.REACTOR_INSTRUMENT_PORT_ID))) {
                ports.get(PortType.INSTRUMENT).add(position);
            } else if (actual.equals(id(P1ContentIds.REACTOR_COLD_PORT_ID))) {
                ports.get(PortType.COLD_COOLANT).add(position);
            } else if (actual.equals(id(P1ContentIds.REACTOR_HOT_PORT_ID))) {
                ports.get(PortType.HOT_COOLANT).add(position);
            } else if (!actual.equals(id(P1ContentIds.REACTOR_CASING_ID))
                    && !actual.equals(id(P1ContentIds.REACTOR_WINDOW_ID))) {
                return ScanResult.invalid(DiagnosticCode.STRUCTURE_BLOCKS,
                        "invalid side slot block " + actual + " at " + position);
            }
        }
        if (ports.get(PortType.INSTRUMENT).size() != 1) {
            return ScanResult.invalid(DiagnosticCode.INSTRUMENT_PORT,
                    "reactor requires exactly one instrument port");
        }
        if (ports.get(PortType.COLD_COOLANT).isEmpty()) {
            return ScanResult.invalid(DiagnosticCode.MISSING_COLD_PORT,
                    "reactor requires at least one cold coolant port");
        }
        if (ports.get(PortType.HOT_COOLANT).isEmpty()) {
            return ScanResult.invalid(DiagnosticCode.MISSING_HOT_PORT,
                    "reactor requires at least one hot coolant port");
        }

        TreeMap<CoreColumnPosition, ColumnMapping> columns = new TreeMap<>();
        int fuelCount = 0;
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                CoreColumnPosition core = new CoreColumnPosition(x, z);
                int localX = x + INTERNAL_MIN;
                int localZ = z + INTERNAL_MIN;
                LocalPosition cap = new LocalPosition(localX, SIZE - 1, localZ);
                String capId = blocks.get(cap);
                ColumnType type;
                String bodyId;
                switch (capId) {
                    case "create_nuclear_industry:reactor_refueling_port" -> {
                        type = ColumnType.FUEL;
                        bodyId = id(P1ContentIds.REACTOR_FUEL_ROD_ID);
                    }
                    case "create_nuclear_industry:control_rod_drive" -> {
                        type = ColumnType.CONTROL_ROD;
                        bodyId = AIR_ID;
                    }
                    case "create_nuclear_industry:reactor_casing" -> {
                        type = ColumnType.EMPTY;
                        bodyId = AIR_ID;
                    }
                    default -> {
                        return ScanResult.invalid(DiagnosticCode.COLUMN_LAYOUT,
                                "invalid core cap " + capId + " at " + cap);
                    }
                }

                List<LocalPosition> bodyPositions = new ArrayList<>(INTERNAL_HEIGHT);
                for (int y = INTERNAL_MIN; y <= INTERNAL_MAX; y++) {
                    LocalPosition body = new LocalPosition(localX, y, localZ);
                    bodyPositions.add(body);
                    if (!bodyId.equals(blocks.get(body))) {
                        return ScanResult.invalid(DiagnosticCode.COLUMN_LAYOUT,
                                "invalid " + type + " body at " + body
                                        + ", expected " + bodyId + ", found " + blocks.get(body));
                    }
                }
                if (type == ColumnType.FUEL) {
                    fuelCount++;
                }
                columns.put(core, new ColumnMapping(core, type, bodyPositions, cap));
            }
        }

        if (fuelCount == 0) {
            return ScanResult.invalid(DiagnosticCode.NO_FUEL,
                    "reactor must contain at least one fuel column");
        }
        return ScanResult.valid(columns, ports);
    }

    private static boolean isPortId(String id) {
        return id(P1ContentIds.REACTOR_INSTRUMENT_PORT_ID).equals(id)
                || id(P1ContentIds.REACTOR_COLD_PORT_ID).equals(id)
                || id(P1ContentIds.REACTOR_HOT_PORT_ID).equals(id);
    }

    private static String id(String path) {
        return "create_nuclear_industry:" + path;
    }

    private static Map<LocalPosition, String> fixedCasingBlocks() {
        Map<LocalPosition, String> fixed = new LinkedHashMap<>();
        for (LocalPosition position : ALL_POSITIONS) {
            if (position.isBoundary() && !isColumnCap(position) && !isSidePortSlot(position)) {
                fixed.put(position, id(P1ContentIds.REACTOR_CASING_ID));
            }
        }
        return Collections.unmodifiableMap(fixed);
    }

    private static Set<LocalPosition> buildSidePortSlots() {
        TreeSet<LocalPosition> slots = new TreeSet<>();
        for (int y = INTERNAL_MIN; y <= INTERNAL_MAX; y++) {
            for (int alongFace = INTERNAL_MIN; alongFace <= INTERNAL_MAX; alongFace++) {
                slots.add(new LocalPosition(0, y, alongFace));
                slots.add(new LocalPosition(SIZE - 1, y, alongFace));
                slots.add(new LocalPosition(alongFace, y, 0));
                slots.add(new LocalPosition(alongFace, y, SIZE - 1));
            }
        }
        return Collections.unmodifiableSet(slots);
    }

    private static boolean isSidePortSlot(LocalPosition position) {
        boolean xFace = (position.x() == 0 || position.x() == SIZE - 1)
                && position.z() >= INTERNAL_MIN && position.z() <= INTERNAL_MAX;
        boolean zFace = (position.z() == 0 || position.z() == SIZE - 1)
                && position.x() >= INTERNAL_MIN && position.x() <= INTERNAL_MAX;
        return position.y() >= INTERNAL_MIN && position.y() <= INTERNAL_MAX && (xFace || zFace);
    }

    private static boolean isColumnCap(LocalPosition position) {
        return position.y() == SIZE - 1
                && position.x() >= INTERNAL_MIN && position.x() <= INTERNAL_MAX
                && position.z() >= INTERNAL_MIN && position.z() <= INTERNAL_MAX;
    }

    private static Map<CoreColumnPosition, ColumnType> defaultColumns() {
        TreeMap<CoreColumnPosition, ColumnType> columns = new TreeMap<>();
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                columns.put(new CoreColumnPosition(x, z),
                        x == 1 && z == 1 ? ColumnType.EMPTY : ColumnType.FUEL);
            }
        }
        return Collections.unmodifiableMap(columns);
    }

    public enum PortType {
        INSTRUMENT,
        COLD_COOLANT,
        HOT_COOLANT
    }

    public enum DiagnosticCode {
        VALID("valid"),
        STRUCTURE_BLOCKS("structure_blocks"),
        INSTRUMENT_PORT("instrument_port"),
        MISSING_COLD_PORT("missing_cold_port"),
        MISSING_HOT_PORT("missing_hot_port"),
        COLUMN_LAYOUT("column_layout"),
        NO_FUEL("no_fuel"),
        GENERIC_FAILURE("generic_failure");

        private final String translationSuffix;

        DiagnosticCode(String translationSuffix) {
            this.translationSuffix = translationSuffix;
        }

        public String translationKey() {
            return "message.create_nuclear_industry.reactor_structure." + translationSuffix;
        }
    }

    public enum ColumnType {
        EMPTY,
        FUEL,
        CONTROL_ROD
    }

    public record LocalPosition(int x, int y, int z) implements Comparable<LocalPosition> {
        public boolean isInside() {
            return x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE;
        }

        public boolean isBoundary() {
            return isInside() && (x == 0 || x == SIZE - 1 || y == 0 || y == SIZE - 1
                    || z == 0 || z == SIZE - 1);
        }

        @Override
        public int compareTo(LocalPosition other) {
            int xOrder = Integer.compare(x, other.x);
            if (xOrder != 0) {
                return xOrder;
            }
            int yOrder = Integer.compare(y, other.y);
            return yOrder != 0 ? yOrder : Integer.compare(z, other.z);
        }
    }

    public record ColumnMapping(
            CoreColumnPosition corePosition,
            ColumnType type,
            List<LocalPosition> bodyPositions,
            LocalPosition capPosition
    ) {
        public ColumnMapping {
            bodyPositions = List.copyOf(bodyPositions);
        }
    }

    public record ScanResult(
            boolean valid,
            String failureReason,
            DiagnosticCode diagnosticCode,
            Map<CoreColumnPosition, ColumnMapping> columns,
            Map<PortType, List<LocalPosition>> ports
    ) {
        public ScanResult {
            if (diagnosticCode == null) {
                throw new IllegalArgumentException("structure diagnostic code is required");
            }
            columns = Collections.unmodifiableMap(new TreeMap<>(columns));
            EnumMap<PortType, List<LocalPosition>> copiedPorts = new EnumMap<>(PortType.class);
            for (Map.Entry<PortType, List<LocalPosition>> entry : ports.entrySet()) {
                copiedPorts.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            ports = Collections.unmodifiableMap(copiedPorts);
        }

        private static ScanResult valid(
                Map<CoreColumnPosition, ColumnMapping> columns,
                Map<PortType, List<LocalPosition>> ports
        ) {
            return new ScanResult(true, "", DiagnosticCode.VALID, columns, ports);
        }

        private static ScanResult invalid(DiagnosticCode diagnosticCode, String reason) {
            return new ScanResult(false, reason, diagnosticCode, Map.of(), Map.of());
        }

        public static ScanResult notScanned() {
            return invalid(DiagnosticCode.GENERIC_FAILURE, "structure has not been scanned");
        }
    }
}
