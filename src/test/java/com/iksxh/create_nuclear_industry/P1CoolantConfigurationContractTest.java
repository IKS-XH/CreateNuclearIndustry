package com.iksxh.create_nuclear_industry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class P1CoolantConfigurationContractTest {
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");

    @Test
    void serverConfigExposesIndependentThousandMillibucketBuffers() throws IOException {
        String source = readSource("com/iksxh/create_nuclear_industry/config/P1ServerConfig.java");

        assertTrue(source.contains("defineInRange(\"perPortFlowMbPerTick\", 128"));
        assertTrue(source.contains("defineInRange(\"coldInventoryCapacityMb\", 1_000"));
        assertTrue(source.contains("defineInRange(\"hotInventoryCapacityMb\", 1_000"));
        assertTrue(source.contains("coldInventoryCapacityMb"));
        assertTrue(source.contains("hotInventoryCapacityMb"));
    }

    @Test
    void formalFluidHandlerReadsFlowAndBothBufferValuesFromServerConfig() throws IOException {
        String source = readSource(
                "com/iksxh/create_nuclear_industry/reactor/ReactorCoolantFluidHandler.java");

        assertTrue(source.contains("P1ServerConfig.VALUES.perPortFlowMbPerTick.get()"));
        assertTrue(source.contains("P1ServerConfig.VALUES.coldInventoryCapacityMb.get()"));
        assertTrue(source.contains("P1ServerConfig.VALUES.hotInventoryCapacityMb.get()"));
        assertTrue(source.contains("ReactorCoolantPortFlowBudget"));
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(JAVA_SOURCES.resolve(relativePath));
    }
}
