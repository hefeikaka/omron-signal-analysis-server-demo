package com.omron.oss.integration.config;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SignalMongoProperties {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private final String serverName;
    private final String host;
    private final int port;
    private final String database;
    private final String rawSignalCollection;

    public SignalMongoProperties(String serverName, String host, int port, String database, String rawSignalCollection) {
        this.serverName = serverName;
        this.host = host;
        this.port = port;
        this.database = database;
        this.rawSignalCollection = rawSignalCollection;
    }

    public static SignalMongoProperties from(Properties properties) {
        return new SignalMongoProperties(
            resolveProperty(properties, "serverName", "mongodb-datasource"),
            resolveProperty(properties, "url", "127.0.0.1"),
            Integer.parseInt(resolveProperty(properties, "port", "27017")),
            resolveProperty(properties, "database", "db_signal"),
            resolveProperty(properties, "rawSignalCollection", "rawdata_signal_curve")
        );
    }

    private static String resolveProperty(Properties properties, String key, String fallback) {
        String raw = properties.getProperty(key, fallback);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(raw);
        StringBuffer resolved = new StringBuffer();
        boolean replaced = false;
        while (matcher.find()) {
            String nestedKey = matcher.group(1);
            String nestedValue = properties.getProperty(nestedKey);
            if (nestedValue == null) {
                nestedValue = System.getProperty(nestedKey);
            }
            if (nestedValue == null) {
                nestedValue = fallback;
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(nestedValue));
            replaced = true;
        }
        if (replaced) {
            matcher.appendTail(resolved);
            return resolved.toString();
        }
        return raw;
    }

    public String getServerName() {
        return serverName;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getRawSignalCollection() {
        return rawSignalCollection;
    }
}
