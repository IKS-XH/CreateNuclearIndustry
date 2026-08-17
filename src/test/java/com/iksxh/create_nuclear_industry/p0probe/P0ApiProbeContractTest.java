package com.iksxh.create_nuclear_industry.p0probe;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class P0ApiProbeContractTest {
    @Test
    void compileProbeNamesOnlyTheCurrentVersionEntrypoints() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "com", "iksxh",
                "create_nuclear_industry", "p0probe", "api", "P0CreateApiCompileProbe.java"));
        assertTrue(source.contains("Capabilities.FluidHandler.BLOCK"));
        assertTrue(source.contains("ScrollValueBehaviour"));
        assertTrue(source.contains("IHaveGoggleInformation"));
        assertTrue(source.contains("ArmInteractionPoint.create"));
        assertTrue(source.contains("PlayerInteractEvent.RightClickBlock"));
        assertTrue(source.contains("BlockEvent.BreakEvent"));
        assertTrue(source.contains("saveAdditional"));
        assertTrue(source.contains("loadAdditional"));
        assertTrue(source.contains("hasNeighborSignal"));
    }
}
