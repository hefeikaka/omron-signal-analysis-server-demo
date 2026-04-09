package com.omron.oss.integration.config;

import java.util.Objects;
import java.util.Properties;

public final class FeatureStoreProperties {

    private final boolean enabled;
    private final String adminUrl;
    private final String url;
    private final String user;
    private final String password;
    private final String driverClass;
    private final String databaseName;

    public FeatureStoreProperties(
        boolean enabled,
        String adminUrl,
        String url,
        String user,
        String password,
        String driverClass,
        String databaseName
    ) {
        this.enabled = enabled;
        this.adminUrl = Objects.requireNonNull(adminUrl, "adminUrl");
        this.url = Objects.requireNonNull(url, "url");
        this.user = Objects.requireNonNull(user, "user");
        this.password = Objects.requireNonNull(password, "password");
        this.driverClass = Objects.requireNonNull(driverClass, "driverClass");
        this.databaseName = Objects.requireNonNull(databaseName, "databaseName");
    }

    public static FeatureStoreProperties from(Properties properties) {
        return new FeatureStoreProperties(
            Boolean.parseBoolean(properties.getProperty("enabled", "true")),
            properties.getProperty(
                "adminUrl",
                "jdbc:mysql://127.0.0.1:3306/?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci&serverTimezone=Asia/Shanghai"
            ),
            properties.getProperty(
                "url",
                "jdbc:mysql://127.0.0.1:3306/DB_SingalAys?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci&serverTimezone=Asia/Shanghai"
            ),
            properties.getProperty("user", "root"),
            properties.getProperty("password", "1234"),
            properties.getProperty("driverClass", "com.mysql.cj.jdbc.Driver"),
            properties.getProperty("databaseName", "DB_SingalAys")
        );
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getDatabaseName() {
        return databaseName;
    }
}
