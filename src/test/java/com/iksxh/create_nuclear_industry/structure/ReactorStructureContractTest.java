package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 5×5×5 结构坐标、端口槽位、九列角色和所有无效结构诊断。 */
class ReactorStructureContractTest {
    private static final Path TEMPLATE = Path.of(
            "src", "main", "resources", "data", "create_nuclear_industry", "structures",
            ReactorStructureDefinition.STRUCTURE_ID + ".nbt"
    );

    @Test
    void canonicalTemplateUsesFixedDimensionsPortsWindowsAndBalanceLayout() {
        assertEquals(5, ReactorStructureDefinition.SIZE);
        assertEquals(3, ReactorStructureDefinition.INTERNAL_HEIGHT);
        assertEquals(new ReactorStructureDefinition.LocalPosition(2, 2, 0),
                ReactorStructureDefinition.DEFAULT_INSTRUMENT_PORT_POSITION);
        assertEquals(new ReactorStructureDefinition.LocalPosition(1, 2, 4),
                ReactorStructureDefinition.DEFAULT_COLD_PORT_POSITION);
        assertEquals(new ReactorStructureDefinition.LocalPosition(3, 2, 4),
                ReactorStructureDefinition.DEFAULT_HOT_PORT_POSITION);
        assertEquals(36, ReactorStructureDefinition.sidePortSlots().size());
        assertEquals(3, ReactorStructureDefinition.windowPositions().size());

        ReactorStructureDefinition.ScanResult result = ReactorStructureDefinition.scan(
                ReactorStructureDefinition.canonicalTemplate());

        assertTrue(result.valid(), result.failureReason());
        assertEquals(9, result.columns().size());
        assertEquals(8, result.columns().values().stream()
                .filter(column -> column.type() == ReactorStructureDefinition.ColumnType.FUEL)
                .count());
        assertEquals(ReactorStructureDefinition.ColumnType.EMPTY,
                result.columns().get(new CoreColumnPosition(1, 1)).type());
        assertEquals("create_nuclear_industry:reactor_casing",
                ReactorStructureDefinition.canonicalTemplate().get(
                        new ReactorStructureDefinition.LocalPosition(2, 4, 2)));
        assertEquals(3, result.columns().get(new CoreColumnPosition(0, 0)).bodyPositions().size());
        assertEquals(1, result.ports().get(ReactorStructureDefinition.PortType.INSTRUMENT).size());
        assertEquals(1, result.ports().get(ReactorStructureDefinition.PortType.COLD_COOLANT).size());
        assertEquals(1, result.ports().get(ReactorStructureDefinition.PortType.HOT_COOLANT).size());
    }

    @Test
    void acceptsOneTwoAndThreeColdAndHotPortsInLegalSideSlots() {
        Map<ReactorStructureDefinition.LocalPosition, String> oneGroup =
                ReactorStructureDefinition.canonicalTemplate();
        assertValidWithPortCounts(oneGroup, 1, 1);

        Map<ReactorStructureDefinition.LocalPosition, String> twoGroups =
                new HashMap<>(oneGroup);
        putPort(twoGroups, new ReactorStructureDefinition.LocalPosition(2, 1, 4),
                ReactorStructureDefinition.PortType.COLD_COOLANT);
        putPort(twoGroups, new ReactorStructureDefinition.LocalPosition(2, 3, 4),
                ReactorStructureDefinition.PortType.HOT_COOLANT);
        assertValidWithPortCounts(twoGroups, 2, 2);

        Map<ReactorStructureDefinition.LocalPosition, String> threeGroups =
                new HashMap<>(twoGroups);
        putPort(threeGroups, new ReactorStructureDefinition.LocalPosition(0, 1, 2),
                ReactorStructureDefinition.PortType.COLD_COOLANT);
        putPort(threeGroups, new ReactorStructureDefinition.LocalPosition(4, 3, 2),
                ReactorStructureDefinition.PortType.HOT_COOLANT);
        assertValidWithPortCounts(threeGroups, 3, 3);
    }

    @Test
    void controlRodColumnMapsToAnAirBodyAndDriveCap() {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout =
                new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        CoreColumnPosition control = new CoreColumnPosition(1, 0);
        layout.put(control, ReactorStructureDefinition.ColumnType.CONTROL_ROD);

        ReactorStructureDefinition.ScanResult result = ReactorStructureDefinition.scan(
                ReactorStructureDefinition.templateFor(layout));

        assertTrue(result.valid(), result.failureReason());
        ReactorStructureDefinition.ColumnMapping mapping = result.columns().get(control);
        assertEquals(ReactorStructureDefinition.ColumnType.CONTROL_ROD, mapping.type());
        assertEquals(new ReactorStructureDefinition.LocalPosition(2, 4, 1), mapping.capPosition());
        assertEquals(3, mapping.bodyPositions().size());
    }

    @Test
    void rejectsMissingBlockWrongBlockExtraBlockAndMisplacedPort() {
        Map<ReactorStructureDefinition.LocalPosition, String> canonical =
                ReactorStructureDefinition.canonicalTemplate();

        Map<ReactorStructureDefinition.LocalPosition, String> missing = new HashMap<>(canonical);
        missing.remove(ReactorStructureDefinition.DEFAULT_INSTRUMENT_PORT_POSITION);
        assertInvalid(missing, "missing structure coordinate");

        Map<ReactorStructureDefinition.LocalPosition, String> wrong = new HashMap<>(canonical);
        wrong.put(new ReactorStructureDefinition.LocalPosition(1, 1, 1),
                "create_nuclear_industry:reactor_casing");
        assertInvalid(wrong, "invalid FUEL body");

        Map<ReactorStructureDefinition.LocalPosition, String> extraReplacement = new HashMap<>(canonical);
        extraReplacement.put(new ReactorStructureDefinition.LocalPosition(1, 1, 1),
                "create_nuclear_industry:reactor_window");
        assertInvalid(extraReplacement, "invalid FUEL body");

        Map<ReactorStructureDefinition.LocalPosition, String> misplacedPort = new HashMap<>(canonical);
        misplacedPort.put(new ReactorStructureDefinition.LocalPosition(0, 0, 2),
                "create_nuclear_industry:reactor_cold_port");
        assertInvalid(misplacedPort, "expected create_nuclear_industry:reactor_casing");

        Map<ReactorStructureDefinition.LocalPosition, String> cornerPort = new HashMap<>(canonical);
        cornerPort.put(new ReactorStructureDefinition.LocalPosition(0, 2, 0),
                "create_nuclear_industry:reactor_instrument_port");
        assertInvalid(cornerPort, "expected create_nuclear_industry:reactor_casing");

        Map<ReactorStructureDefinition.LocalPosition, String> outside = new HashMap<>(canonical);
        outside.put(new ReactorStructureDefinition.LocalPosition(5, 2, 2),
                "create_nuclear_industry:reactor_casing");
        assertInvalid(outside, "extra structure coordinate");
    }

    @Test
    void rejectsAColumnWithTheWrongTopCapOrNonAirControlRodBody() {
        Map<ReactorStructureDefinition.LocalPosition, String> wrongCap =
                new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        wrongCap.put(new ReactorStructureDefinition.LocalPosition(2, 4, 2),
                ReactorStructureDefinition.AIR_ID);
        assertInvalid(wrongCap, "invalid core cap minecraft:air");

        Map<ReactorStructureDefinition.LocalPosition, String> wrongControlCap =
                new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        wrongControlCap.put(new ReactorStructureDefinition.LocalPosition(1, 4, 1),
                "create_nuclear_industry:control_rod_drive");
        assertInvalid(wrongControlCap, "invalid CONTROL_ROD body");

        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout =
                new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        CoreColumnPosition control = new CoreColumnPosition(1, 0);
        layout.put(control, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        Map<ReactorStructureDefinition.LocalPosition, String> wrongBody =
                new HashMap<>(ReactorStructureDefinition.templateFor(layout));
        wrongBody.put(new ReactorStructureDefinition.LocalPosition(2, 1, 1),
                "create_nuclear_industry:reactor_fuel_rod");
        assertInvalid(wrongBody, "invalid CONTROL_ROD body");
    }

    @Test
    void rejectsMissingCoolantPortDuplicateInstrumentAndIncompleteSideShell() {
        Map<ReactorStructureDefinition.LocalPosition, String> missingCold =
                new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        missingCold.put(ReactorStructureDefinition.DEFAULT_COLD_PORT_POSITION,
                "create_nuclear_industry:reactor_casing");
        assertInvalid(missingCold, "at least one cold coolant port");

        Map<ReactorStructureDefinition.LocalPosition, String> missingHot =
                new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        missingHot.put(ReactorStructureDefinition.DEFAULT_HOT_PORT_POSITION,
                "create_nuclear_industry:reactor_casing");
        assertInvalid(missingHot, "at least one hot coolant port");

        Map<ReactorStructureDefinition.LocalPosition, String> duplicateInstrument =
                new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        duplicateInstrument.put(new ReactorStructureDefinition.LocalPosition(0, 1, 1),
                "create_nuclear_industry:reactor_instrument_port");
        assertInvalid(duplicateInstrument, "exactly one instrument port");

        Map<ReactorStructureDefinition.LocalPosition, String> missingSideShell =
                new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        missingSideShell.put(new ReactorStructureDefinition.LocalPosition(0, 1, 1),
                ReactorStructureDefinition.AIR_ID);
        assertInvalid(missingSideShell, "invalid side slot block minecraft:air");
    }

    @Test
    void commitsACompressedGameTestStructureTemplate() throws IOException {
        assertTrue(Files.isRegularFile(TEMPLATE), "missing experimental reactor GameTest template");
        assertTrue(Files.size(TEMPLATE) > 32, "experimental reactor template is unexpectedly empty");
        byte[] header = Files.readAllBytes(TEMPLATE);
        assertEquals((byte) 0x1f, header[0], "structure template must be gzip-compressed NBT");
        assertEquals((byte) 0x8b, header[1], "structure template must be gzip-compressed NBT");

        CompoundTag root = NbtIo.readCompressed(TEMPLATE, NbtAccounter.unlimitedHeap());
        assertArrayEquals(new int[]{5, 5, 5}, root.getIntArray("size"));
        assertEquals(8, root.getList("palette", Tag.TAG_COMPOUND).size());
        assertEquals(122, root.getList("blocks", Tag.TAG_COMPOUND).size());
        assertEquals(0, root.getList("entities", Tag.TAG_COMPOUND).size());

        ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
        ListTag blockList = root.getList("blocks", Tag.TAG_COMPOUND);
        Map<ReactorStructureDefinition.LocalPosition, String> blocks = new HashMap<>();
        for (ReactorStructureDefinition.LocalPosition position : ReactorStructureDefinition.allPositions()) {
            blocks.put(position, ReactorStructureDefinition.AIR_ID);
        }
        for (int index = 0; index < blockList.size(); index++) {
            CompoundTag block = blockList.getCompound(index);
            int[] coordinates = block.getIntArray("pos");
            assertEquals(3, coordinates.length, "structure block position must have three coordinates");
            ReactorStructureDefinition.LocalPosition position =
                    new ReactorStructureDefinition.LocalPosition(coordinates[0], coordinates[1], coordinates[2]);
            assertTrue(position.isInside(), "structure block lies outside the declared volume");
            String previous = blocks.put(position, palette.getCompound(block.getInt("state")).getString("Name"));
            assertEquals(ReactorStructureDefinition.AIR_ID, previous,
                    "structure template contains duplicate non-air coordinates");
        }
        ReactorStructureDefinition.ScanResult scan = ReactorStructureDefinition.scan(blocks);
        assertTrue(scan.valid(), scan.failureReason());
    }

    private static void assertInvalid(Map<ReactorStructureDefinition.LocalPosition, String> blocks,
                                      String expectedReason) {
        ReactorStructureDefinition.ScanResult result = ReactorStructureDefinition.scan(blocks);
        assertFalse(result.valid(), "expected invalid structure");
        assertTrue(result.failureReason().contains(expectedReason),
                () -> "expected reason containing '" + expectedReason + "', got '"
                        + result.failureReason() + "'");
    }

    private static void assertValidWithPortCounts(
            Map<ReactorStructureDefinition.LocalPosition, String> blocks,
            int coldPorts,
            int hotPorts
    ) {
        ReactorStructureDefinition.ScanResult result = ReactorStructureDefinition.scan(blocks);
        assertTrue(result.valid(), result.failureReason());
        assertEquals(1, result.ports().get(ReactorStructureDefinition.PortType.INSTRUMENT).size());
        assertEquals(coldPorts, result.ports().get(ReactorStructureDefinition.PortType.COLD_COOLANT).size());
        assertEquals(hotPorts, result.ports().get(ReactorStructureDefinition.PortType.HOT_COOLANT).size());
    }

    private static void putPort(
            Map<ReactorStructureDefinition.LocalPosition, String> blocks,
            ReactorStructureDefinition.LocalPosition position,
            ReactorStructureDefinition.PortType type
    ) {
        String id = switch (type) {
            case INSTRUMENT -> "create_nuclear_industry:reactor_instrument_port";
            case COLD_COOLANT -> "create_nuclear_industry:reactor_cold_port";
            case HOT_COOLANT -> "create_nuclear_industry:reactor_hot_port";
        };
        blocks.put(position, id);
    }
}
