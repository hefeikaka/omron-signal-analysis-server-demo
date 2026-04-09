package com.omron.oss.integration.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalProcessingExecutorFactoryTest {

    @Test
    void shouldCreateNamedSignalWorkerThread() throws Exception {
        SignalProcessingExecutorFactory factory = new SignalProcessingExecutorFactory();
        ExecutorService executor = factory.create(new SignalProcessingProperties(2, 10));
        try {
            Future<String> future = executor.submit(() -> Thread.currentThread().getName());
            String threadName = future.get(3, TimeUnit.SECONDS);
            assertTrue(threadName.startsWith("signal-worker-"));
        } finally {
            executor.shutdownNow();
        }
    }
}
