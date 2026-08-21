package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition.LocalPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/** Adapts the loader-independent reactor contract to a live Minecraft level. */
public final class ReactorStructureScanner {
    private static final String INSTRUMENT_ID = id(P1ContentIds.REACTOR_INSTRUMENT_PORT_ID);

    private ReactorStructureScanner() {
    }

    /**
     * Finds the local origin from an instrument port and scans exactly one
     * fixed 5 x 5 x 5 candidate for every legal instrument slot.
     */
    public static WorldScanResult scanInstrumentPort(Level level, BlockPos instrumentPortPos) {
        if (level == null || instrumentPortPos == null) {
            return WorldScanResult.invalid("level and instrument position are required");
        }
        if (!INSTRUMENT_ID.equals(blockId(level, instrumentPortPos))) {
            return WorldScanResult.invalid("instrument port is not present at " + instrumentPortPos);
        }

        WorldScanResult firstFailure = null;
        for (LocalPosition instrumentSlot : ReactorStructureDefinition.sidePortSlots()) {
            BlockPos origin = instrumentPortPos.offset(-instrumentSlot.x(), -instrumentSlot.y(), -instrumentSlot.z());
            Map<LocalPosition, String> blocks = readCandidate(level, origin);
            ReactorStructureDefinition.ScanResult contract = ReactorStructureDefinition.scan(blocks);
            WorldScanResult result = new WorldScanResult(origin, contract);
            if (result.valid()) {
                return result;
            }
            if (firstFailure == null) {
                firstFailure = result;
            }
        }
        return firstFailure == null
                ? WorldScanResult.invalid("no legal instrument slot was tested")
                : firstFailure;
    }

    /** Scans every instrument port within the only distance a fixed structure can cover. */
    public static void rescanAround(Level level, BlockPos changedPos) {
        if (level == null || changedPos == null) {
            return;
        }
        for (int dx = -ReactorStructureDefinition.SIZE + 1;
             dx <= ReactorStructureDefinition.SIZE - 1; dx++) {
            for (int dy = -ReactorStructureDefinition.SIZE + 1;
                 dy <= ReactorStructureDefinition.SIZE - 1; dy++) {
                for (int dz = -ReactorStructureDefinition.SIZE + 1;
                     dz <= ReactorStructureDefinition.SIZE - 1; dz++) {
                    BlockPos candidate = changedPos.offset(dx, dy, dz);
                    if (INSTRUMENT_ID.equals(blockId(level, candidate))) {
                        BlockEntity blockEntity = level.getBlockEntity(candidate);
                        if (blockEntity instanceof ReactorInstrumentPortBlockEntity instrument) {
                            instrument.updateStructureCache(scanInstrumentPort(level, candidate));
                        }
                    }
                }
            }
        }
    }

    private static Map<LocalPosition, String> readCandidate(Level level, BlockPos origin) {
        Map<LocalPosition, String> blocks = new LinkedHashMap<>();
        for (int x = 0; x < ReactorStructureDefinition.SIZE; x++) {
            for (int y = 0; y < ReactorStructureDefinition.SIZE; y++) {
                for (int z = 0; z < ReactorStructureDefinition.SIZE; z++) {
                    LocalPosition local = new LocalPosition(x, y, z);
                    blocks.put(local, blockId(level, origin.offset(x, y, z)));
                }
            }
        }
        return blocks;
    }

    private static String blockId(Level level, BlockPos pos) {
        var key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        return key == null ? "" : key.toString();
    }

    private static String id(String path) {
        return "create_nuclear_industry:" + path;
    }

    public record WorldScanResult(
            BlockPos origin,
            ReactorStructureDefinition.ScanResult contract
    ) {
        public WorldScanResult {
            origin = origin == null ? null : origin.immutable();
            if (contract == null) {
                throw new IllegalArgumentException("structure scan contract result is required");
            }
        }

        public boolean valid() {
            return contract.valid();
        }

        public String failureReason() {
            return contract.failureReason();
        }

        private static WorldScanResult invalid(String reason) {
            return new WorldScanResult(null,
                    new ReactorStructureDefinition.ScanResult(false, reason, Map.of(), Map.of()));
        }
    }
}
