package com.omron.oss.integration.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignalMongoPropertiesTest {

    @Test
    void shouldUseRc2SignalDatabaseAsDefaultDatabase() {
        SignalMongoProperties properties = SignalMongoProperties.from(new Properties());

        assertEquals("db_signal", properties.getDatabase());
        assertEquals("rawdata_signal_curve", properties.getRawSignalCollection());
    }
}
