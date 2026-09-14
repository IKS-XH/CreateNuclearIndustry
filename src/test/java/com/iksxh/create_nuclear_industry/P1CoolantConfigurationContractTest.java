package com.iksxh.create_nuclear_industry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** 验证布局派生共享容量和服务端流量配置的读取边界。 */
class P1CoolantConfigurationContractTest {
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");

    @Test
    void serverConfigExposesPerSpaceBlockSharedCapacity() throws IOException {
        String source = readSource("com/iksxh/create_nuclear_industry/config/P1ServerConfig.java");

        assertTrue(source.contains("defineInRange(\"perPortFlowMbPerTick\", 128"));
        assertTrue(source.contains("defineInRange(\"coolantCapacityPerEmptyBlockMb\""));
        assertTrue(source.contains("ReactorCoolantCapacity.DEFAULT_CAPACITY_PER_EMPTY_BLOCK_MB"));
        assertTrue(source.contains("ReactorCoolantCapacity.MAX_CAPACITY_PER_EMPTY_BLOCK_MB"));
        assertTrue(source.contains("0, ReactorCoolantCapacity.MAX_CAPACITY_PER_EMPTY_BLOCK_MB"));
        assertFalse(source.contains("coldInventoryCapacityMb"));
        assertFalse(source.contains("hotInventoryCapacityMb"));
    }

    @Test
    void formalFluidHandlerReadsFlowAndBothBufferValuesFromServerConfig() throws IOException {
        String source = readSource(
                "com/iksxh/create_nuclear_industry/reactor/ReactorCoolantFluidHandler.java");

        assertTrue(source.contains("P1ServerConfig.VALUES.perPortFlowMbPerTick.get()"));
        assertTrue(source.contains("owner.coolantCapacityMb()"));
        assertFalse(source.contains("coldInventoryCapacityMb"));
        assertFalse(source.contains("hotInventoryCapacityMb"));
        assertTrue(source.contains("ReactorCoolantPortFlowBudget"));
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(JAVA_SOURCES.resolve(relativePath));
    }
}
