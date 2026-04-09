CREATE DATABASE IF NOT EXISTS `DB_SingalAys` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `DB_SingalAys`;

CREATE TABLE IF NOT EXISTS feature_segment (
    segment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id VARCHAR(128) NOT NULL,
    collected_at BIGINT NOT NULL,
    sampling_frequency INT NOT NULL,
    sample_count INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_machine_collected (machine_id, collected_at)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS feature_value (
    value_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment_id BIGINT NOT NULL,
    feature_key VARCHAR(64) NOT NULL,
    feature_label_zh VARCHAR(128) NOT NULL,
    feature_label_en VARCHAR(128) NOT NULL,
    feature_domain VARCHAR(32) NOT NULL,
    feature_value DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_segment_feature (segment_id, feature_key),
    KEY idx_feature_lookup (feature_key, segment_id),
    CONSTRAINT fk_feature_segment FOREIGN KEY (segment_id) REFERENCES feature_segment(segment_id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
