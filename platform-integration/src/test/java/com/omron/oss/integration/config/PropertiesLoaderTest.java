package com.omron.oss.integration.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertiesLoaderTest {

    @Test
    void shouldLoadPropertiesFile() throws Exception {
        Path tempFile = Files.createTempFile("signal-processing", ".properties");
        try {
            Files.write(tempFile, "signal.processing.workerThreads=6".getBytes(StandardCharsets.UTF_8));
            Properties properties = new PropertiesLoader().load(tempFile);
            assertEquals("6", properties.getProperty("signal.processing.workerThreads"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
