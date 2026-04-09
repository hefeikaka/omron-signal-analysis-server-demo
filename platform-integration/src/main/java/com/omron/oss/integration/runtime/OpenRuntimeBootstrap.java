package com.omron.oss.integration.runtime;

import com.mongodb.client.MongoClient;
import com.omron.oss.integration.acquisition.DirectAcquisitionService;
import com.omron.oss.integration.config.FeatureStoreProperties;
import com.omron.oss.integration.config.PropertiesLoader;
import com.omron.oss.integration.config.SignalMongoProperties;
import com.omron.oss.integration.config.SignalProcessingExecutorFactory;
import com.omron.oss.integration.config.SignalProcessingExecutorManager;
import com.omron.oss.integration.config.SignalProcessingProperties;
import com.omron.oss.integration.mysql.FeatureStoreRepository;
import com.omron.oss.integration.mysql.JdbcFeatureStoreRepository;
import com.omron.oss.integration.mongodb.JsonLineSignalRecordRepository;
import com.omron.oss.integration.mongodb.MongoClientFactory;
import com.omron.oss.integration.mongodb.MongoSignalRecordRepository;
import com.omron.oss.integration.mongodb.SignalRecordRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

public final class OpenRuntimeBootstrap {

    private final PropertiesLoader propertiesLoader;
    private final SignalProcessingExecutorFactory executorFactory;
    private final MongoClientFactory mongoClientFactory;

    public OpenRuntimeBootstrap() {
        this(new PropertiesLoader(), new SignalProcessingExecutorFactory(), new MongoClientFactory());
    }

    OpenRuntimeBootstrap(
        PropertiesLoader propertiesLoader,
        SignalProcessingExecutorFactory executorFactory,
        MongoClientFactory mongoClientFactory
    ) {
        this.propertiesLoader = Objects.requireNonNull(propertiesLoader, "propertiesLoader");
        this.executorFactory = Objects.requireNonNull(executorFactory, "executorFactory");
        this.mongoClientFactory = Objects.requireNonNull(mongoClientFactory, "mongoClientFactory");
    }

    public SignalProcessingExecutorManager createExecutorManager(Path signalProcessingPropertiesPath) {
        Properties properties = propertiesLoader.load(signalProcessingPropertiesPath);
        SignalProcessingProperties processingProperties = SignalProcessingProperties.from(properties);
        ExecutorService executorService = executorFactory.create(processingProperties);
        return new SignalProcessingExecutorManager(executorService);
    }

    public SignalRecordRepository createSignalRecordRepository(
        Path mongoPropertiesPath,
        String storageMode,
        Path localStorageFile
    ) {
        if ("jsonl".equalsIgnoreCase(storageMode)) {
            return new JsonLineSignalRecordRepository(localStorageFile);
        }

        SignalMongoProperties properties = SignalMongoProperties.from(propertiesLoader.load(mongoPropertiesPath));
        MongoClient mongoClient = mongoClientFactory.create(properties);
        return new MongoSignalRecordRepository(
            mongoClient,
            properties.getDatabase(),
            properties.getRawSignalCollection()
        );
    }

    public DirectAcquisitionService createDirectAcquisitionService(Path directAcquisitionConfigPath, Path driverDirectory) {
        return new DirectAcquisitionService(directAcquisitionConfigPath, driverDirectory);
    }

    public FeatureStoreRepository createFeatureStoreRepository(Path featureStorePropertiesPath) {
        return new JdbcFeatureStoreRepository(
            FeatureStoreProperties.from(propertiesLoader.load(featureStorePropertiesPath))
        );
    }

    public static Path defaultMongoConfigPath(Path etcDirectory) {
        return etcDirectory.resolve("com.omron.gc.cm.mongodb-signal.cfg");
    }

    public static Path defaultSignalProcessingConfigPath(Path etcDirectory) {
        return etcDirectory.resolve("signal-processing.properties");
    }

    public static Path defaultDirectAcquisitionConfigPath(Path etcDirectory) {
        return etcDirectory.resolve("direct-acquisition.properties");
    }

    public static Path defaultFeatureStoreConfigPath(Path etcDirectory) {
        return etcDirectory.resolve("mysql-feature-store.properties");
    }

    public static Path defaultJsonLineStoragePath(Path runtimeBaseDirectory) {
        return runtimeBaseDirectory.resolve(Paths.get("storage", "rawdata_signal_curve.jsonl"));
    }
}
