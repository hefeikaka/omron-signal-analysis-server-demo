package com.omron.oss.integration.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.omron.oss.integration.config.SignalMongoProperties;

public final class MongoClientFactory {

    public MongoClient create(SignalMongoProperties properties) {
        String connectionString = "mongodb://" + properties.getHost() + ":" + properties.getPort();
        return MongoClients.create(connectionString);
    }
}
