package com.iksxh.create_nuclear_industry.control;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlRodSliderProtocolTest {
    @Test
    void wireCodesRoundTripAndUnknownValuesAreRejected() {
        for (ControlRodSliderPhase phase : ControlRodSliderPhase.values()) {
            assertEquals(phase, ControlRodSliderPhase.fromWireCode(phase.wireCode()));
        }
        assertEquals(null, ControlRodSliderPhase.fromWireCode(99));
        assertEquals(ControlRodSliderStatus.INVALID_PACKET,
                ControlRodSliderStatus.fromWireCode(999));
    }

    @Test
    void payloadCarriesTheServerValidationFields() {
        ControlRodSliderPayload payload = ControlRodSliderPayload.start(
                new BlockPos(3, 4, 5), 2, 1, 73, 42L);
        assertEquals(new BlockPos(3, 4, 5), payload.drivePos());
        assertEquals(2, payload.columnX());
        assertEquals(1, payload.columnZ());
        assertEquals(0, payload.row());
        assertEquals(73, payload.depthPercent());
        assertEquals(ControlRodSliderPhase.START, payload.phase());
        assertEquals(42L, payload.dragId());
    }

    @Test
    void responseAcceptsOnlyKnownSuccessfulStatuses() {
        ControlRodSliderResponsePayload accepted = new ControlRodSliderResponsePayload(
                new BlockPos(1, 2, 3), 0, 0, 50,
                ControlRodSliderPhase.PREVIEW.wireCode(),
                ControlRodSliderStatus.ACCEPTED.wireCode(), 7L, true);
        ControlRodSliderResponsePayload rejected = new ControlRodSliderResponsePayload(
                new BlockPos(1, 2, 3), 0, 0, 50,
                ControlRodSliderPhase.PREVIEW.wireCode(),
                ControlRodSliderStatus.INVALID_RANGE.wireCode(), 7L, false);
        assertTrue(accepted.accepted());
        assertTrue(accepted.hasAuthoritativeDepth());
        assertFalse(rejected.accepted());
        assertFalse(rejected.hasAuthoritativeDepth());
    }
}
