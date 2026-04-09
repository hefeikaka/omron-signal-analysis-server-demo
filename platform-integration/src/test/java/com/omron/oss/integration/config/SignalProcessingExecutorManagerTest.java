package com.omron.oss.integration.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalProcessingExecutorManagerTest {

    @Test
    void shouldShutdownExecutorGracefully() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        SignalProcessingExecutorManager manager = new SignalProcessingExecutorManager(executor);

        boolean shutdown = manager.shutdownGracefully(2, TimeUnit.SECONDS);

        assertTrue(shutdown);
        assertTrue(executor.isShutdown());
    }
}
