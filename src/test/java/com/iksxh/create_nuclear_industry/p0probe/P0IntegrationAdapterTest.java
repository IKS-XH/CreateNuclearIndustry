package com.iksxh.create_nuclear_industry.p0probe;

import com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnKey;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ColumnState;
import com.iksxh.create_nuclear_industry.p0probe.numeric.CoolantPort;
import com.iksxh.create_nuclear_industry.p0probe.numeric.P0IntegrationAdapter;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorParameters;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorTickInput;
import com.iksxh.create_nuclear_industry.p0probe.numeric.ReactorTickResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class P0IntegrationAdapterTest {
    @Test
    void serverAdapterTicksOnceDeduplicatesPortsAndCanResumeFromSnapshot() {
        ColumnKey key = new ColumnKey(0, 0);
        ReactorSnapshot initial = new ReactorSnapshot(
                Map.of(key, ColumnState.fuel(key, 1, 1, 0)), 0, false, 0);
        ReactorTickInput input = new ReactorTickInput(List.of(
                CoolantPort.cold("cold", 1), CoolantPort.cold("cold", 1),
                CoolantPort.hot("hot", 1), CoolantPort.hot("hot", 1)), Map.of(), 1, false);
        P0IntegrationAdapter adapter = new P0IntegrationAdapter();
        ReactorTickResult first = adapter.serverTick(initial, ReactorParameters.defaults(), input);
        assertEquals(1, adapter.tickCalls());
        assertEquals(2, first.duplicatePortCount());
        ReactorSnapshot reloaded = adapter.saveAndReload(first.next());
        ReactorTickResult resumed = adapter.serverTick(reloaded, ReactorParameters.defaults(), input);
        assertEquals(2, adapter.tickCalls());
        assertEquals(first.next().tick() + 1, resumed.next().tick());
        assertEquals(first.next().columns().get(key).fuelColumnIntegrity(),
                resumed.next().columns().get(key).fuelColumnIntegrity(), 1e-12);
    }
}
