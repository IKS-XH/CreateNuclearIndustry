package com.iksxh.create_nuclear_industry.content;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * P1 registration contract only. This class deliberately does not register
 * Minecraft objects; the follow-up data tasks consume these stable IDs.
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

    /** Create owns this item; this mod only consumes it as an information trigger. */
    public static final String ENGINEER_GOGGLES_ID = "create:goggles";

    /** Existing sample content is retained separately from the new P1 contract. */
    public static final String EXISTING_SAMPLE_REACTOR_CASING_ID = "experimental_reactor_casing";

    /** The old descriptive name must never become a second item or alias. */
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

    /** IDs that are explicitly excluded from the P1 registration contract. */
    public static final Set<String> PROHIBITED_IDS = Set.of(
            "coolant_purifier",
            "contaminated_compound_coolant",
            "industrial_gauge",
            "industrial_instrument",
            "reactor_interlock",
            "scram_interlock",
            REMOVED_ALLOY_STEEL_PLATE_ALIAS
    );

    public enum Kind {
        MULTIBLOCK,
        BLOCK,
        ITEM,
        FLUID,
        EXTERNAL_ITEM
    }

    public enum Owner {
        MOD,
        CREATE
    }

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

    public static boolean isProhibited(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        return PROHIBITED_IDS.contains(normalized)
                || normalized.contains("purif")
                || normalized.contains("contaminated_coolant")
                || normalized.contains("interlock");
    }
}
