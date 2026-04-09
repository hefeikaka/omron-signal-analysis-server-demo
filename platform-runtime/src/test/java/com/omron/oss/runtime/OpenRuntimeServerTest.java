package com.omron.oss.runtime;

import com.omron.oss.domain.common.model.SignalSample;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenRuntimeServerTest {

    @Test
    void shouldResolveRuntimeHomeFromSystemProperty() {
        String original = System.getProperty("open.runtime.home");
        try {
            System.setProperty("open.runtime.home", "D:\\runtime-home");
            Path resolved = OpenRuntimeServer.resolveRuntimeHome();
            assertEquals(Paths.get("D:\\runtime-home"), resolved);
        } finally {
            if (original == null) {
                System.clearProperty("open.runtime.home");
            } else {
                System.setProperty("open.runtime.home", original);
            }
        }
    }

    @Test
    void shouldExposeEdgeAsDefaultLandingPath() {
        assertEquals("/edge/", OpenRuntimeServer.defaultLandingPath());
    }

    @Test
    void shouldBuildRealtimeMonitorResponse() {
        String response = OpenRuntimeServer.buildRealtimeMonitorResponse(Collections.singletonList(
            new SignalSample("machine-01", 1712640000000L, 10000, Arrays.asList(1.2d, 3.4d, 2.1d))
        ));

        assertTrue(response.contains("\"count\":1"));
        assertTrue(response.contains("\"machineId\":\"machine-01\""));
        assertTrue(response.contains("\"latestValue\":2.1"));
        assertTrue(response.contains("\"previewValues\":[1.2,3.4,2.1]"));
    }
}
