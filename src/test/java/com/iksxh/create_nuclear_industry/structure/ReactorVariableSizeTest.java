package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.TreeMap;
import static org.junit.jupiter.api.Assertions.*;

/** 以独立长方体夹具验证可变边界、列图和共享容量，旧五格夹具保持原样。 */
class ReactorVariableSizeTest {
    @Test void rectangularEvenStructureUsesEveryActualColumnAndBody() {
        var scan = ReactorStructureDefinition.scan(fixture(6, 8, 7));
        assertTrue(scan.valid(), scan.failureReason());
        assertEquals(20, scan.columns().size());
        assertEquals(6, scan.columns().get(new CoreColumnPosition(0, 0)).bodyPositions().size());
        assertEquals(114_000L, ReactorCoolantCapacity.fromScan(scan, 1000).coolantCapacityMb());
    }

    @Test void maximumStructureFitsIntCapacityWithOneFuelComponentColumn() {
        var scan = ReactorStructureDefinition.scan(fixture(16, 16, 16));
        assertTrue(scan.valid(), scan.failureReason());
        assertEquals(196, scan.columns().size());
        assertEquals(2_730_000L, ReactorCoolantCapacity.fromScan(scan, 1000).coolantCapacityMb());
        assertNotNull(scan.columns().get(new CoreColumnPosition(13, 13)));
    }

    /** 最大实际几何可由既有协议承载，棒长和液体合法格消费同一高度，不代表视觉验收。 */
    @Test void maximumRuntimeEnvelopeAndClientRodUseFourteenBodyBlocks() {
        var blocks = fixture(16, 16, 16);
        blocks.put(new ReactorStructureDefinition.LocalPosition(14, 15, 14), id("control_rod_drive"));
        var scan = ReactorStructureDefinition.scan(blocks);
        assertTrue(scan.valid(), scan.failureReason());
        var origin = new net.minecraft.core.BlockPos(8, 30, 8);
        var max = origin.offset(15, 15, 15);
        var owner = origin.offset(1, 1, 0);
        var generation = new java.util.UUID(0, 42);
        var members = blocks.entrySet().stream().filter(e -> scan.dimensions().boundary(e.getKey()))
                .map(e -> {
                    var p = origin.offset(e.getKey().x(), e.getKey().y(), e.getKey().z());
                    return new ReactorSurfaceDescriptor.SurfaceMember(p, e.getValue(), ReactorSurfaceDescriptor.outwardFaces(p, origin, max));
                }).toList();
        var surface = new ReactorSurfaceDescriptor("minecraft:overworld", owner, generation, 1, true, origin, max, members);
        assertEquals(surface, ReactorSurfaceDescriptor.decode(surface.encode()).orElseThrow());
        var state = new com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot(Map.of(),
                Map.of(new CoreColumnPosition(13, 13), com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState.fullyInserted()),
                0, 0, 0, false, Map.of(), false, false, scan.dimensions());
        var input = new com.iksxh.create_nuclear_industry.reactor.ReactorServerTick.CoolantInput(
                com.iksxh.create_nuclear_industry.reactor.ReactorCoolantLedger.summarizePorts(java.util.List.of(), 128), 2_730_000, .5);
        var settled = com.iksxh.create_nuclear_industry.reactor.ReactorServerTick.advance(state,
                com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters.defaults(), input);
        var runtime = ReactorRuntimeDescriptorFactory.project(surface, scan, settled, input, 1, 1, 1);
        assertEquals(runtime, ReactorRuntimeDescriptor.decode(runtime.encode()).orElseThrow());
        assertEquals(196, runtime.columns().size());
        assertTrue(runtime.columns().stream().allMatch(c -> c.bodyPositions().size() == 14));
        assertEquals(2730, runtime.coolantSpace().size());
        var rod = (ReactorRuntimeDescriptor.ControlRodColumn) runtime.columns().stream()
                .filter(c -> c instanceof ReactorRuntimeDescriptor.ControlRodColumn).findFirst().orElseThrow();
        var identity = new com.iksxh.create_nuclear_industry.structure.client.ReactorAnimationVisualState.Identity(1, "minecraft:overworld", owner.asLong(), generation, 1);
        var pose = com.iksxh.create_nuclear_industry.structure.client.ReactorAnimationVisualState.rodPose(identity, rod, 0);
        assertEquals(14, pose.travel());
        assertEquals(origin.getY() + 1, pose.bottom(), 1e-12);
        assertEquals(origin.getY() + 15, pose.top(), 1e-12);
        com.iksxh.create_nuclear_industry.structure.client.ReactorAnimationVisualState.clear();
    }

    @Test void onlyCompleteFuelBodiesAndSupportedOuterDimensionsForm() {
        assertTrue(ReactorStructureDefinition.scan(fixture(5, 5, 5)).valid());
        assertFalse(ReactorStructureDefinition.scan(fixture(4, 5, 5)).valid());
        assertFalse(ReactorStructureDefinition.scan(fixture(17, 5, 5)).valid());
        var incomplete = fixture(6, 8, 7);
        incomplete.put(new ReactorStructureDefinition.LocalPosition(1, 6, 1), "minecraft:air");
        assertFalse(ReactorStructureDefinition.scan(incomplete).valid());
    }

    /** 独立夹具只装一列燃料，仪表偏向侧面边缘但不占棱边。 */
    static Map<ReactorStructureDefinition.LocalPosition, String> fixture(int width, int height, int depth) {
        var blocks = new TreeMap<ReactorStructureDefinition.LocalPosition, String>();
        for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) for (int z = 0; z < depth; z++) {
            boolean boundary = x == 0 || y == 0 || z == 0 || x == width - 1 || y == height - 1 || z == depth - 1;
            blocks.put(new ReactorStructureDefinition.LocalPosition(x, y, z), boundary ? id("reactor_casing") : "minecraft:air");
        }
        for (int y = 1; y < height - 1; y++) blocks.put(new ReactorStructureDefinition.LocalPosition(1, y, 1), id("reactor_fuel_rod"));
        blocks.put(new ReactorStructureDefinition.LocalPosition(1, height - 1, 1), id("reactor_refueling_port"));
        blocks.put(new ReactorStructureDefinition.LocalPosition(1, 1, 0), id("reactor_instrument_port"));
        blocks.put(new ReactorStructureDefinition.LocalPosition(width - 1, 1, 1), id("reactor_cold_port"));
        blocks.put(new ReactorStructureDefinition.LocalPosition(width - 1, 1, depth - 2), id("reactor_hot_port"));
        return blocks;
    }
    private static String id(String path) { return "create_nuclear_industry:" + path; }
}
