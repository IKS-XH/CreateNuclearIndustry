package com.iksxh.create_nuclear_industry.control;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** 验证 P1-CONTROL-04 的生产代码边界，防止网络响应重新接回 Create 游标反射。 */
class P1Control04ContractTest {
    private static final Path CONTROL_SOURCE = Path.of(
            "src/main/java/com/iksxh/create_nuclear_industry/control");

    @Test
    void responsePathHasNoReflectiveScreenSynchronizer() throws Exception {
        assertFalse(Files.exists(CONTROL_SOURCE.resolve("ControlRodSliderScreenSync.java")));
        String handler = Files.readString(CONTROL_SOURCE.resolve("ControlRodSliderClientHandler.java"));
        String adapter = Files.readString(CONTROL_SOURCE.resolve("ControlRodSliderClientAdapter.java"));
        assertFalse(handler.contains("ControlRodSliderScreenSync"));
        assertFalse(handler.contains("setCursor"));
        assertFalse(adapter.contains("ControlRodSliderPayload.preview("));
    }
}
