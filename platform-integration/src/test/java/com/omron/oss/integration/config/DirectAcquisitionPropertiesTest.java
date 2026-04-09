package com.omron.oss.integration.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectAcquisitionPropertiesTest {

    @Test
    void shouldUseRc2AlignedDefaults() {
        DirectAcquisitionProperties properties = DirectAcquisitionProperties.from(new Properties());

        assertTrue(properties.isEnabled());
        assertEquals("VK70xNMC_DAQ2.dll", properties.getDriverName());
        assertEquals(8234, properties.getTcpPort());
        assertEquals("M001-1", properties.getEquipmentNo());
        assertEquals(5000, properties.getSamplingFrequency());
        assertEquals(1000L, properties.getAcquisitionDurationMillis());
        assertEquals(10000L, properties.getAcquisitionIntervalMillis());
        assertEquals(5000, properties.computeSamplingNum());
    }
}
