package com.iksxh.create_nuclear_industry.p0probe.numeric;

/**
 * Minimal server-side adapter seam for P0. It owns one tick call and snapshot handoff,
 * while fake ports/control input remain plain data. It is not a block entity or registration.
 */
public final class P0IntegrationAdapter {
    private int tickCalls;

    public ReactorTickResult serverTick(ReactorSnapshot snapshot, ReactorParameters parameters,
                                        ReactorTickInput input) {
        tickCalls++;
        return ReactorModel.tick(snapshot, parameters, input);
    }

    public int tickCalls() {
        return tickCalls;
    }

    public ReactorSnapshot saveAndReload(ReactorSnapshot snapshot) {
        return ReactorSnapshotCodec.decode(ReactorSnapshotCodec.encode(snapshot));
    }
}
