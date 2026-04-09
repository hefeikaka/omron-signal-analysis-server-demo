package com.omron.oss.integration.runtime;

import com.omron.oss.integration.config.SignalProcessingExecutorManager;
import com.omron.oss.integration.mongodb.JsonLineSignalRecordRepository;
import com.omron.oss.integration.mongodb.SignalRecordRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenRuntimeBootstrapTest {

    @Test
    void shouldCreateExecutorManagerFromPropertiesFile() throws Exception {
        Path tempFile = Files.createTempFile("signal-processing", ".properties");
        try {
            Files.write(
                tempFile,
                ("signal.processing.workerThreads=2" + System.lineSeparator()
                    + "signal.processing.queueCapacity=50").getBytes(StandardCharsets.UTF_8)
            );
            OpenRuntimeBootstrap bootstrap = new OpenRuntimeBootstrap();

            SignalProcessingExecutorManager manager = bootstrap.createExecutorManager(tempFile);

            assertTrue(manager.getExecutorService() != null);
            manager.close();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void shouldUseJsonLineRepositoryWhenRequested() throws Exception {
        Path mongoFile = Files.createTempFile("mongo", ".cfg");
        Path storageFile = Files.createTempFile("signal-storage", ".jsonl");
        try {
            Files.write(
                mongoFile,
                ("url=127.0.0.1" + System.lineSeparator()
                    + "port=27017" + System.lineSeparator()
                    + "database=DB_SingalAys" + System.lineSeparator()
                    + "rawSignalCollection=rawdata_signal_curve").getBytes(StandardCharsets.UTF_8)
            );
            OpenRuntimeBootstrap bootstrap = new OpenRuntimeBootstrap();

            SignalRecordRepository repository = bootstrap.createSignalRecordRepository(mongoFile, "jsonl", storageFile);

            assertEquals(JsonLineSignalRecordRepository.class, repository.getClass());
        } finally {
            Files.deleteIfExists(mongoFile);
            Files.deleteIfExists(storageFile);
        }
    }
}
