package com.iksxh.create_nuclear_industry.p0probe;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 P0 编译探针只引用当前 NeoForge/Create API 入口，不形成正式玩法契约。 */
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
