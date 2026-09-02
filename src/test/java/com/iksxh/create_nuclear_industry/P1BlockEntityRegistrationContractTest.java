package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 P1 方块实体类型注册及唯一仪表端口状态所有者约束。 */
class P1BlockEntityRegistrationContractTest {
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");

    @Test
    void registersTheInstrumentPortSharedPortsAndControlRodDriveTypes() throws IOException {
        String registration = readSource("com/iksxh/create_nuclear_industry/content/P1BlockEntities.java");
        String main = readSource("com/iksxh/create_nuclear_industry/CreateNuclearIndustry.java");

        assertTrue(registration.contains("P1ContentIds.REACTOR_INSTRUMENT_PORT_ID"));
        assertTrue(registration.contains("P1ContentIds.CONTROL_ROD_DRIVE_ID"));
        assertTrue(registration.contains("P1Blocks.REACTOR_COLD_PORT.get()"));
        assertTrue(registration.contains("P1Blocks.REACTOR_HOT_PORT.get()"));
        assertTrue(registration.contains("P1Blocks.REACTOR_REFUELING_PORT.get()"));
        assertTrue(registration.contains("ReactorInstrumentPortBlockEntity::new"));
        assertTrue(registration.contains("ReactorPortBlockEntity::new"));
        assertTrue(registration.contains("ControlRodDriveBlockEntity::new"));
        assertTrue(main.contains("P1BlockEntities.register(modEventBus)"));
    }

    @Test
    void onlyTheInstrumentPortIsMarkedAsTheFutureStateOwner() throws IOException {
        String instrument = readSource("com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java");
        String port = readSource("com/iksxh/create_nuclear_industry/blockentity/ReactorPortBlockEntity.java");
        String drive = readSource("com/iksxh/create_nuclear_industry/blockentity/ControlRodDriveBlockEntity.java");

        assertTrue(instrument.contains("Sole authoritative owner"));
        assertTrue(instrument.contains("private ReactorSnapshot snapshot"));
        assertTrue(instrument.contains("ReactorSnapshotNbtCodec"));
        assertTrue(instrument.contains("getUpdatePacket"));
        assertTrue(port.contains("readAuthoritativeSnapshot"));
        assertTrue(port.contains("FuelRefuelingTransaction"));
        assertTrue(port.contains("tryInsertFuel"));
        assertTrue(port.contains("tryExtractFuel"));
        assertFalse(port.contains("currentFissionHeatHu"), "端口不得接受调用方伪造的发热值");
        assertTrue(drive.contains("readAuthoritativeSnapshot"));
        assertFalse(port.contains("private FuelColumnState"), "fuel column state must stay in the instrument snapshot");
        for (String source : new String[]{drive}) {
            assertFalse(source.contains("Temperature"));
            assertFalse(source.contains("Coolant"));
            assertFalse(source.contains("Simulation"));
            assertFalse(source.contains("private ReactorSnapshot"));
            assertFalse(source.contains("private ControlRodColumnState"));
        }
    }

    @Test
    void formalEntityBlocksStayServerSideAndDataOnly() throws IOException {
        for (String path : new String[]{
                "com/iksxh/create_nuclear_industry/block/ReactorInstrumentPortBlock.java",
                "com/iksxh/create_nuclear_industry/block/ReactorPortBlock.java",
                "com/iksxh/create_nuclear_industry/block/ControlRodDriveBlock.java",
                "com/iksxh/create_nuclear_industry/blockentity/P1MinimalBlockEntity.java"
        }) {
            String source = readSource(path);
            assertFalse(source.contains("net.minecraft.client"), "client-only class leaked into " + path);
            assertFalse(source.contains("tick("), "runtime tick leaked into " + path);
        }
        assertTrue(readSource("com/iksxh/create_nuclear_industry/gametest/P1BlockEntityGameTests.java")
                .contains("formalBlockEntitiesLoadAndRoundTripOnDedicatedServer"));
        assertTrue(P1ContentIds.REACTOR_INSTRUMENT_PORT_ID.equals("reactor_instrument_port"));
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(JAVA_SOURCES.resolve(relativePath));
    }
}
