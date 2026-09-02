package com.iksxh.create_nuclear_industry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 COOL-05 的绑定生命周期、NeoForge capability 和 Create 管网恢复入口均已接入。 */
class P1Coolant05ContractTest {
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");

    @Test
    void lifecycleReconcilesBindingsAndReclaimsOnlyStaleOwners() throws IOException {
        String lifecycle = readSource(
                "com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java");
        String instrument = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java");
        String port = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorPortBlockEntity.java");

        assertTrue(lifecycle.contains("notifyFluidNetworkAround"));
        assertTrue(lifecycle.contains("FluidPropagator.propagateChangedPipe"));
        assertTrue(instrument.contains("reconcilePortBindings"));
        assertTrue(instrument.contains("public void invalidate()"));
        assertTrue(instrument.contains("detachPort"));
        assertTrue(port.contains("canReclaimBinding"));
        assertTrue(port.contains("previousOwner.detachPort(this)"));
        assertTrue(port.contains("level.invalidateCapabilities(worldPosition)"));
    }

    @Test
    void staleHandlersCheckBothWorldEntityAndOwnerLifecycle() throws IOException {
        String handler = readSource(
                "com/iksxh/create_nuclear_industry/reactor/ReactorCoolantFluidHandler.java");
        String gameTests = readSource(
                "com/iksxh/create_nuclear_industry/gametest/P1Coolant05GameTests.java");

        assertTrue(handler.contains("sourcePort.isRemoved()"));
        assertTrue(handler.contains("owner.isRemoved()"));
        assertTrue(handler.contains("getBlockEntity(sourcePort.getBlockPos()) != sourcePort"));
        assertTrue(gameTests.contains("replacingInstrumentRetainsPortAndCreateNetwork"));
        assertTrue(gameTests.contains("retainedHotPortRestoresCreateOutput"));
        assertTrue(gameTests.contains("retainedPortRecoversThroughFiveStructureCycles"));
        assertTrue(gameTests.contains("FluidPropagator.propagateChangedPipe"));
    }

    @Test
    void stableRescanOnlyReconcilesWithoutCreatingCapabilityEdges() throws IOException {
        String instrument = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java");
        String port = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorPortBlockEntity.java");

        assertTrue(instrument.contains("reconcilePortBindings()"));
        assertTrue(port.contains("boolean changed = !isBoundTo(owner, expectedType, column)"));
        assertTrue(port.contains("if (changed && level != null && !level.isClientSide)"));
        assertTrue(port.contains("invalidateCapabilityAndNetwork(previousBinding, expectedType)"));
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(JAVA_SOURCES.resolve(relativePath));
    }
}
