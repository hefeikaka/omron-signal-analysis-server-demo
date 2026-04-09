package com.omron.oss.integration.config;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class SignalProcessingExecutorManager implements AutoCloseable {

    private final ExecutorService executorService;

    public SignalProcessingExecutorManager(ExecutorService executorService) {
        this.executorService = Objects.requireNonNull(executorService, "executorService");
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public boolean shutdownGracefully(long timeout, TimeUnit timeUnit) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(timeout, timeUnit)) {
                executorService.shutdownNow();
                return executorService.awaitTermination(timeout, timeUnit);
            }
            return true;
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void close() {
        shutdownGracefully(5, TimeUnit.SECONDS);
    }
}
