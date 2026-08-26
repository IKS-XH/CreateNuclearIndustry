package com.iksxh.create_nuclear_industry.content;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * P1 注册 ID 与所有权的静态契约。
 *
 * <p>本类故意不创建或注册 Minecraft 对象；正式注册层、资源契约和测试共同消费
 * 这些稳定 ID。模组自有 ID 必须保持未限定形式，Create 所有的外部 ID 则必须使用
 * {@code create:} 命名空间。</p>
 */
public final class P1ContentIds {
    public static final String EXPERIMENTAL_REACTOR_ID = "experimental_reactor";

    public static final String REACTOR_CASING_ID = "reactor_casing";
    public static final String REACTOR_WINDOW_ID = "reactor_window";
    public static final String REACTOR_INSTRUMENT_PORT_ID = "reactor_instrument_port";
    public static final String REACTOR_COLD_PORT_ID = "reactor_cold_port";
    public static final String REACTOR_HOT_PORT_ID = "reactor_hot_port";
    public static final String REACTOR_REFUELING_PORT_ID = "reactor_refueling_port";
    public static final String REACTOR_FUEL_ROD_ID = "reactor_fuel_rod";
    public static final String CONTROL_ROD_DRIVE_ID = "control_rod_drive";

    public static final String FRESH_FUEL_ASSEMBLY_ID = "fresh_fuel_assembly";
    public static final String COOLED_SPENT_FUEL_ASSEMBLY_ID = "cooled_spent_fuel_assembly";
    public static final String CONTROL_ROD_ID = "control_rod";
    public static final String STEEL_PLATE_ID = "steel_plate";

    public static final String COMPOUND_COOLANT_ID = "compound_coolant";
    public static final String HOT_COMPOUND_COOLANT_ID = "hot_compound_coolant";
    public static final String COMPOUND_COOLANT_BUCKET_ID = "compound_coolant_bucket";

    /** 该物品由 Create 所有，本模组只将其作为信息触发项消费。 */
    public static final String ENGINEER_GOGGLES_ID = "create:goggles";

    /** 旧样例内容与新的 P1 正式契约分开保留。 */
    public static final String EXISTING_SAMPLE_REACTOR_CASING_ID = "experimental_reactor_casing";

    /** 旧描述性名称不得重新成为第二个物品或别名。 */
    public static final String REMOVED_ALLOY_STEEL_PLATE_ALIAS = "alloy_steel_plate";

    public static final List<Entry> FORMAL_IDS = List.of(
            new Entry(EXPERIMENTAL_REACTOR_ID, Kind.MULTIBLOCK, Owner.MOD),
            new Entry(REACTOR_CASING_ID, Kind.BLOCK, Owner.MOD),
            new Entry(REACTOR_WINDOW_ID, Kind.BLOCK, Owner.MOD),
            new Entry(REACTOR_INSTRUMENT_PORT_ID, Kind.BLOCK, Owner.MOD),
            new Entry(REACTOR_COLD_PORT_ID, Kind.BLOCK, Owner.MOD),
            new Entry(REACTOR_HOT_PORT_ID, Kind.BLOCK, Owner.MOD),
            new Entry(REACTOR_REFUELING_PORT_ID, Kind.BLOCK, Owner.MOD),
            new Entry(REACTOR_FUEL_ROD_ID, Kind.BLOCK, Owner.MOD),
            new Entry(CONTROL_ROD_DRIVE_ID, Kind.BLOCK, Owner.MOD),
            new Entry(FRESH_FUEL_ASSEMBLY_ID, Kind.ITEM, Owner.MOD),
            new Entry(COOLED_SPENT_FUEL_ASSEMBLY_ID, Kind.ITEM, Owner.MOD),
            new Entry(CONTROL_ROD_ID, Kind.ITEM, Owner.MOD),
            new Entry(STEEL_PLATE_ID, Kind.ITEM, Owner.MOD),
            new Entry(COMPOUND_COOLANT_ID, Kind.FLUID, Owner.MOD),
            new Entry(HOT_COMPOUND_COOLANT_ID, Kind.FLUID, Owner.MOD),
            new Entry(COMPOUND_COOLANT_BUCKET_ID, Kind.ITEM, Owner.MOD),
            new Entry(ENGINEER_GOGGLES_ID, Kind.EXTERNAL_ITEM, Owner.CREATE)
    );

    /** 明确排除在 P1 注册契约之外的 ID。 */
    public static final Set<String> PROHIBITED_IDS = Set.of(
            "coolant_purifier",
            "contaminated_compound_coolant",
            "industrial_gauge",
            "industrial_instrument",
            "reactor_interlock",
            "scram_interlock",
            REMOVED_ALLOY_STEEL_PLATE_ALIAS
    );

    /** 注册条目的内容类别，用于资源与注册契约校验。 */
    public enum Kind {
        MULTIBLOCK,
        BLOCK,
        ITEM,
        FLUID,
        EXTERNAL_ITEM
    }

    /** 注册条目的所有者；外部条目不由本模组创建。 */
    public enum Owner {
        MOD,
        CREATE
    }

    /** 带有 ID、内容类别和所有权约束的不可变注册条目。 */
    public record Entry(String id, Kind kind, Owner owner) {
        public Entry {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("registration ID must not be blank");
            }
            if (kind == null || owner == null) {
                throw new IllegalArgumentException("registration ID kind and owner are required");
            }
            if (owner == Owner.MOD && id.indexOf(':') >= 0) {
                throw new IllegalArgumentException("local registration IDs must be unqualified: " + id);
            }
            if (owner == Owner.CREATE && !id.startsWith("create:")) {
                throw new IllegalArgumentException("Create integration IDs must use the Create namespace: " + id);
            }
        }
    }

    private P1ContentIds() {
    }

    /** 按大小写无关规则判断 ID 是否命中禁止注册名称或其保留关键词。 */
    public static boolean isProhibited(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        return PROHIBITED_IDS.contains(normalized)
                || normalized.contains("purif")
                || normalized.contains("contaminated_coolant")
                || normalized.contains("interlock");
    }
}
