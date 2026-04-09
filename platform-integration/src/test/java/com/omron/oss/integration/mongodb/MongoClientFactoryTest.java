package com.omron.oss.integration.mongodb;

import com.mongodb.client.MongoClient;
import com.omron.oss.integration.config.SignalMongoProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MongoClientFactoryTest {

    @Test
    void shouldCreateMongoClientFromProperties() {
        MongoClient client = new MongoClientFactory().create(
            new SignalMongoProperties("mongodb-datasource", "127.0.0.1", 27017, "db_signal", "rawdata_signal_curve")
        );
        try {
            assertNotNull(client);
        } finally {
            client.close();
        }
    }
}
