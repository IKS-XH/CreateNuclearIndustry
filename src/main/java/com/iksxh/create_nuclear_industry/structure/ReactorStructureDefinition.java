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
 * 首个 P1 反应堆的加载器无关结构坐标契约。
 *
 * <p>局部原点是实际长方体结构的西北下角；X 向东递增、Y 向上递增、Z 向南递增。
 * 本类只处理带命名空间的方块 ID，不直接依赖 {@code BlockState}，因此世界扫描器
 * 可以适配 Minecraft 世界而不改变坐标契约。</p>
 */
public final class ReactorStructureDefinition {
    public static final String STRUCTURE_ID = P1ContentIds.EXPERIMENTAL_REACTOR_ID;
    public static final String AIR_ID = "minecraft:air";

    public static final int SIZE = 5;
    public static final int INTERNAL_MIN = 1;
    public static final int INTERNAL_MAX = SIZE - 2;
    public static final int INTERNAL_HEIGHT = INTERNAL_MAX - INTERNAL_MIN + 1;

    /** 标准模板位置；实际扫描结果允许端口出现在任一合法侧面槽位。 */
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

    /** 返回所有可放置侧面端口的局部坐标，结果不可变且按坐标排序。 */
    public static Set<LocalPosition> sidePortSlots() {
        return SIDE_PORT_SLOTS;
    }

    /** 返回标准模板中的观察窗位置，结果不可变。 */
    public static Set<LocalPosition> windowPositions() {
        return DEFAULT_WINDOW_POSITIONS;
    }

    /** 返回仪表、冷却剂输入和热冷却剂输出的标准端口位置。 */
    public static Map<PortType, LocalPosition> defaultPortPositions() {
        EnumMap<PortType, LocalPosition> ports = new EnumMap<>(PortType.class);
        ports.put(PortType.INSTRUMENT, DEFAULT_INSTRUMENT_PORT_POSITION);
        ports.put(PortType.COLD_COOLANT, DEFAULT_COLD_PORT_POSITION);
        ports.put(PortType.HOT_COOLANT, DEFAULT_HOT_PORT_POSITION);
        return Collections.unmodifiableMap(ports);
    }

    /** 枚举完整 5×5×5 体积内的全部局部坐标。 */
    public static Set<LocalPosition> allPositions() { return allPositions(ReactorDimensions.DEFAULT); }

    /** 枚举实际包围盒，最多读取16³格。 */
    public static Set<LocalPosition> allPositions(ReactorDimensions dimensions) {
        TreeSet<LocalPosition> positions = new TreeSet<>();
        for (int x = 0; x < dimensions.width(); x++) {
            for (int y = 0; y < dimensions.height(); y++) {
                for (int z = 0; z < dimensions.depth(); z++) {
                    positions.add(new LocalPosition(x, y, z));
                }
            }
        }
        return Collections.unmodifiableSet(positions);
    }

    /** 返回八根燃料列围绕一个中心空列的平衡锚点布局。 */
    public static Map<CoreColumnPosition, ColumnType> defaultColumnLayout() {
        return DEFAULT_COLUMNS;
    }

    /** 返回文档定义的平衡锚点布局；控制棒列由 {@link #templateFor(Map)} 支持。 */
    public static Map<LocalPosition, String> canonicalTemplate() {
        return templateFor(DEFAULT_COLUMNS);
    }

    /**
     * 构造完整的测试/模板布局。3×3 堆芯坐标必须各自明确指定一种角色，缺少的
     * 映射不会被推断成空列。
     */
    public static Map<LocalPosition, String> templateFor(
            Map<CoreColumnPosition, ColumnType> columns
    ) {
        ReactorDimensions dimensions = ReactorDimensions.DEFAULT;
        if (columns == null || columns.size() != CoreColumnPosition.GRID_SIZE * CoreColumnPosition.GRID_SIZE) {
            throw new IllegalArgumentException("all nine P1 core column roles are required");
        }

        TreeMap<CoreColumnPosition, ColumnType> orderedColumns = new TreeMap<>();
        for (int x = 0; x < dimensions.internalWidth(); x++) {
            for (int z = 0; z < dimensions.internalDepth(); z++) {
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
                    for (int y = INTERNAL_MIN; y <= dimensions.internalHeight(); y++) {
                        blocks.put(new LocalPosition(localX, y, localZ), id(P1ContentIds.REACTOR_FUEL_ROD_ID));
                    }
                    blocks.put(new LocalPosition(localX, dimensions.height() - 1, localZ),
                            id(P1ContentIds.REACTOR_REFUELING_PORT_ID));
                }
                case CONTROL_ROD -> blocks.put(new LocalPosition(localX, dimensions.height() - 1, localZ),
                        id(P1ContentIds.CONTROL_ROD_DRIVE_ID));
                case EMPTY -> blocks.put(new LocalPosition(localX, dimensions.height() - 1, localZ),
                        id(P1ContentIds.REACTOR_CASING_ID));
            }
        }
        return Collections.unmodifiableMap(blocks);
    }

    /**
     * 从完整局部快照的最大坐标取得三边尺寸，再验证边界和列。缺格、负坐标以及
     * 不一致列都会被拒绝；已知外尺寸的世界扫描器应使用显式尺寸重载。
     */
    public static ScanResult scan(Map<LocalPosition, String> blocks) {
        if (blocks == null || blocks.isEmpty()) return ScanResult.invalid(DiagnosticCode.STRUCTURE_BLOCKS, "block snapshot is required");
        try {
            ReactorDimensions dimensions = new ReactorDimensions(
                    blocks.keySet().stream().mapToInt(LocalPosition::x).max().orElse(-1) + 1,
                    blocks.keySet().stream().mapToInt(LocalPosition::y).max().orElse(-1) + 1,
                    blocks.keySet().stream().mapToInt(LocalPosition::z).max().orElse(-1) + 1);
            return scan(dimensions, blocks);
        } catch (IllegalArgumentException exception) {
            return ScanResult.invalid(DiagnosticCode.STRUCTURE_BLOCKS, exception.getMessage());
        }
    }

    /** 根据确定的实际外尺寸验证完整列和边界，不从内部空气或燃料猜测外墙。 */
    public static ScanResult scan(ReactorDimensions dimensions, Map<LocalPosition, String> blocks) {
        Set<LocalPosition> ALL_POSITIONS = allPositions(dimensions);
        Set<LocalPosition> SIDE_PORT_SLOTS = sidePortSlots(dimensions);
        Map<LocalPosition, String> FIXED_BLOCKS = fixedCasingBlocks(dimensions);
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
            if (!SIDE_PORT_SLOTS.contains(entry.getKey()) && entry.getValue().equals(id("reactor_automation_port"))) {
                var pos=entry.getKey();
                return ScanResult.invalid(DiagnosticCode.AUTOMATION_SLOT,pos.x()+", "+pos.y()+", "+pos.z());
            }
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
            } else if (actual.equals(id("reactor_automation_port"))) {
                ports.get(PortType.AUTOMATION).add(position);
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

        if (ports.get(PortType.AUTOMATION).size() > 1) {
            var pos=ports.get(PortType.AUTOMATION).get(1);
            return ScanResult.invalid(DiagnosticCode.AUTOMATION_COUNT,pos.x()+", "+pos.y()+", "+pos.z());
        }
        TreeMap<CoreColumnPosition, ColumnMapping> columns = new TreeMap<>();
        int fuelCount = 0;
        for (int x = 0; x < dimensions.internalWidth(); x++) {
            for (int z = 0; z < dimensions.internalDepth(); z++) {
                CoreColumnPosition core = new CoreColumnPosition(x, z);
                int localX = x + INTERNAL_MIN;
                int localZ = z + INTERNAL_MIN;
                LocalPosition cap = new LocalPosition(localX, dimensions.height() - 1, localZ);
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

                List<LocalPosition> bodyPositions = new ArrayList<>(dimensions.internalHeight());
                for (int y = INTERNAL_MIN; y <= dimensions.internalHeight(); y++) {
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
        return ScanResult.valid(dimensions, columns, ports);
    }

    private static boolean isPortId(String id) {
        return id(P1ContentIds.REACTOR_INSTRUMENT_PORT_ID).equals(id)
                || id(P1ContentIds.REACTOR_COLD_PORT_ID).equals(id)
                || id(P1ContentIds.REACTOR_HOT_PORT_ID).equals(id)
                || id("reactor_automation_port").equals(id);
    }

    private static String id(String path) {
        return "create_nuclear_industry:" + path;
    }

    private static Map<LocalPosition, String> fixedCasingBlocks() { return fixedCasingBlocks(ReactorDimensions.DEFAULT); }
    private static Map<LocalPosition, String> fixedCasingBlocks(ReactorDimensions dimensions) {
        Map<LocalPosition, String> fixed = new LinkedHashMap<>();
        for (LocalPosition position : allPositions(dimensions)) {
            if (dimensions.boundary(position) && !dimensions.columnCap(position) && !dimensions.sideSlot(position)) {
                fixed.put(position, id(P1ContentIds.REACTOR_CASING_ID));
            }
        }
        return Collections.unmodifiableMap(fixed);
    }

    private static Set<LocalPosition> buildSidePortSlots() { return sidePortSlots(ReactorDimensions.DEFAULT); }
    /** 返回实际四个侧面的非棱边槽位，顶部列帽另行验证。 */
    public static Set<LocalPosition> sidePortSlots(ReactorDimensions dimensions) {
        TreeSet<LocalPosition> slots = new TreeSet<>();
        for (LocalPosition position : allPositions(dimensions)) {
            if (dimensions.sideSlot(position)) slots.add(position);
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
        ReactorDimensions dimensions = ReactorDimensions.DEFAULT;
        TreeMap<CoreColumnPosition, ColumnType> columns = new TreeMap<>();
        for (int x = 0; x < dimensions.internalWidth(); x++) {
            for (int z = 0; z < dimensions.internalDepth(); z++) {
                columns.put(new CoreColumnPosition(x, z),
                        x == 1 && z == 1 ? ColumnType.EMPTY : ColumnType.FUEL);
            }
        }
        return Collections.unmodifiableMap(columns);
    }

    /** 结构端口的逻辑所有权类别。 */
    public enum PortType {
        INSTRUMENT,
        COLD_COOLANT,
        HOT_COOLANT,
        AUTOMATION
    }

    /** 扫描失败或成功结果的稳定诊断类别。 */
    public enum DiagnosticCode {
        VALID("valid"),
        STRUCTURE_BLOCKS("structure_blocks"),
        INSTRUMENT_PORT("instrument_port"),
        MISSING_COLD_PORT("missing_cold_port"),
        MISSING_HOT_PORT("missing_hot_port"),
        COLUMN_LAYOUT("column_layout"),
        NO_FUEL("no_fuel"),
        DIMENSIONS("dimensions"),
        CHUNKS_UNAVAILABLE("chunks_unavailable"),
        AUTOMATION_SLOT("automation_slot"),
        AUTOMATION_COUNT("automation_count"),
        GENERIC_FAILURE("generic_failure");

        private final String translationSuffix;

        DiagnosticCode(String translationSuffix) {
            this.translationSuffix = translationSuffix;
        }

        /** 返回用于玩家可见消息的语言键，不改变诊断类别本身。 */
        public String translationKey() {
            return "message.create_nuclear_industry.reactor_structure." + translationSuffix;
        }
    }

    /** 堆芯列帽及内部主体所表达的列角色。 */
    public enum ColumnType {
        EMPTY,
        FUEL,
        CONTROL_ROD
    }

    /** 结构内部的不可变局部坐标，排序顺序为 X、Y、Z。 */
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

    /** 将堆芯列角色映射到主体坐标和列帽坐标。 */
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

    /** 结构契约扫描结果；有效结果才携带可用于后续模拟的列和端口映射。 */
    public record ScanResult(
            boolean valid,
            String failureReason,
            DiagnosticCode diagnosticCode,
            Map<CoreColumnPosition, ColumnMapping> columns,
            Map<PortType, List<LocalPosition>> ports,
            ReactorDimensions dimensions
    ) {
        public ScanResult {
            if (dimensions == null) throw new IllegalArgumentException("structure dimensions are required");
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

        /** 原五格调用形式只用于固定示例与尚未成型的诊断信封。 */
        public ScanResult(boolean valid, String reason, DiagnosticCode code,
                Map<CoreColumnPosition, ColumnMapping> columns, Map<PortType, List<LocalPosition>> ports) {
            this(valid, reason, code, columns, ports, ReactorDimensions.DEFAULT);
        }

        private static ScanResult valid(
                ReactorDimensions dimensions,
                Map<CoreColumnPosition, ColumnMapping> columns,
                Map<PortType, List<LocalPosition>> ports
        ) {
            return new ScanResult(true, "", DiagnosticCode.VALID, columns, ports, dimensions);
        }

        private static ScanResult invalid(DiagnosticCode diagnosticCode, String reason) {
            return new ScanResult(false, reason, diagnosticCode, Map.of(), Map.of(), ReactorDimensions.DEFAULT);
        }

        public static ScanResult notScanned() {
            return invalid(DiagnosticCode.GENERIC_FAILURE, "structure has not been scanned");
        }
    }
}
