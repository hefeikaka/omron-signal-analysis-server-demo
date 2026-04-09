package com.omron.oss.integration.mysql;

import com.omron.oss.domain.common.model.FeatureTrendPoint;
import com.omron.oss.domain.common.model.SignalFeatureSnapshot;
import com.omron.oss.domain.common.model.SignalFeatureValue;
import com.omron.oss.integration.config.FeatureStoreProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class JdbcFeatureStoreRepository implements FeatureStoreRepository {

    private final FeatureStoreProperties properties;

    public JdbcFeatureStoreRepository(FeatureStoreProperties properties) {
        this.properties = properties;
    }

    @Override
    public void initialize() {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            Class.forName(properties.getDriverClass());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("MySQL driver was not found: " + properties.getDriverClass(), ex);
        }

        if (!properties.getDriverClass().toLowerCase().contains("h2")) {
            try (Connection connection = DriverManager.getConnection(
                properties.getAdminUrl(),
                properties.getUser(),
                properties.getPassword()
            )) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(
                        "CREATE DATABASE IF NOT EXISTS `" + properties.getDatabaseName() + "` " +
                            "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                    );
                    statement.execute(
                        "ALTER DATABASE `" + properties.getDatabaseName() + "` " +
                            "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                    );
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Failed to create database " + properties.getDatabaseName(), ex);
            }
        }

        try (Connection connection = openConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(
                    "CREATE TABLE IF NOT EXISTS feature_segment (" +
                        "segment_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                        "machine_id VARCHAR(128) NOT NULL," +
                        "collected_at BIGINT NOT NULL," +
                        "sampling_frequency INT NOT NULL," +
                        "sample_count INT NOT NULL," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "UNIQUE KEY uk_machine_collected (machine_id, collected_at)" +
                    ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                );
                statement.execute(
                    "CREATE TABLE IF NOT EXISTS feature_value (" +
                        "value_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                        "segment_id BIGINT NOT NULL," +
                        "feature_key VARCHAR(64) NOT NULL," +
                        "feature_label_zh VARCHAR(128) NOT NULL," +
                        "feature_label_en VARCHAR(128) NOT NULL," +
                        "feature_domain VARCHAR(32) NOT NULL," +
                        "feature_value DOUBLE NOT NULL," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "UNIQUE KEY uk_segment_feature (segment_id, feature_key)," +
                        "KEY idx_feature_lookup (feature_key, segment_id)," +
                        "CONSTRAINT fk_feature_segment FOREIGN KEY (segment_id) REFERENCES feature_segment(segment_id) ON DELETE CASCADE" +
                    ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                );
                statement.execute("ALTER TABLE feature_segment CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                statement.execute("ALTER TABLE feature_value CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize feature store tables", ex);
        }
    }

    @Override
    public void save(SignalFeatureSnapshot snapshot) {
        if (!properties.isEnabled()) {
            return;
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            long segmentId = upsertSegment(connection, snapshot);
            for (SignalFeatureValue featureValue : snapshot.getFeatureValues()) {
                upsertValue(connection, segmentId, featureValue);
            }
            connection.commit();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to store feature snapshot for " + snapshot.getMachineId(), ex);
        }
    }

    @Override
    public List<FeatureTrendPoint> findSeries(
        String machineId,
        String featureKey,
        long fromInclusive,
        long toInclusive,
        int limit
    ) {
        List<FeatureTrendPoint> points = new ArrayList<FeatureTrendPoint>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT s.collected_at, v.feature_value " +
                     "FROM feature_value v " +
                     "JOIN feature_segment s ON s.segment_id = v.segment_id " +
                     "WHERE s.machine_id = ? AND v.feature_key = ? AND s.collected_at BETWEEN ? AND ? " +
                     "ORDER BY s.collected_at ASC " +
                     "LIMIT ?"
             )) {
            statement.setString(1, machineId);
            statement.setString(2, featureKey);
            statement.setLong(3, fromInclusive);
            statement.setLong(4, toInclusive);
            statement.setInt(5, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    points.add(new FeatureTrendPoint(
                        resultSet.getLong(1),
                        resultSet.getDouble(2)
                    ));
                }
            }
            return points;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to query feature series for " + featureKey, ex);
        }
    }

    private long upsertSegment(Connection connection, SignalFeatureSnapshot snapshot) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
            "INSERT INTO feature_segment(machine_id, collected_at, sampling_frequency, sample_count) " +
                "VALUES(?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE sampling_frequency = VALUES(sampling_frequency), sample_count = VALUES(sample_count)",
            Statement.RETURN_GENERATED_KEYS
        )) {
            insert.setString(1, snapshot.getMachineId());
            insert.setLong(2, snapshot.getCollectedAtEpochMillis());
            insert.setInt(3, snapshot.getSamplingFrequency());
            insert.setInt(4, snapshot.getSampleCount());
            insert.executeUpdate();

            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        try (PreparedStatement select = connection.prepareStatement(
            "SELECT segment_id FROM feature_segment WHERE machine_id = ? AND collected_at = ?"
        )) {
            select.setString(1, snapshot.getMachineId());
            select.setLong(2, snapshot.getCollectedAtEpochMillis());
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to resolve segment id after insert.");
    }

    private void upsertValue(Connection connection, long segmentId, SignalFeatureValue featureValue) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO feature_value(segment_id, feature_key, feature_label_zh, feature_label_en, feature_domain, feature_value) " +
                "VALUES(?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE feature_value = VALUES(feature_value)"
        )) {
            statement.setLong(1, segmentId);
            statement.setString(2, featureValue.getDescriptor().getKey());
            statement.setString(3, featureValue.getDescriptor().getLabelZh());
            statement.setString(4, featureValue.getDescriptor().getLabelEn());
            statement.setString(5, featureValue.getDescriptor().getDomain().name());
            statement.setDouble(6, featureValue.getValue());
            statement.executeUpdate();
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(properties.getUrl(), properties.getUser(), properties.getPassword());
    }
}
