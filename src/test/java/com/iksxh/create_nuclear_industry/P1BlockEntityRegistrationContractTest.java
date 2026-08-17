package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertTrue(instrument.contains("Sole future owner"));
        for (String source : new String[]{port, drive}) {
            assertFalse(source.contains("Fuel"));
            assertFalse(source.contains("Integrity"));
            assertFalse(source.contains("Temperature"));
            assertFalse(source.contains("Coolant"));
            assertFalse(source.contains("Simulation"));
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
