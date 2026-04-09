package com.omron.oss.integration.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public final class PropertiesLoader {

    public Properties load(Path filePath) {
        Objects.requireNonNull(filePath, "filePath");
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load properties from " + filePath, ex);
        }
    }
}
