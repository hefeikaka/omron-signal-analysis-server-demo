package com.omron.oss.integration.config;

import java.util.Properties;

public final class SignalProcessingProperties {

    private final int workerThreads;
    private final int queueCapacity;

    public SignalProcessingProperties(int workerThreads, int queueCapacity) {
        this.workerThreads = workerThreads;
        this.queueCapacity = queueCapacity;
    }

    public static SignalProcessingProperties from(Properties properties) {
        return new SignalProcessingProperties(
            Integer.parseInt(properties.getProperty("signal.processing.workerThreads", "4")),
            Integer.parseInt(properties.getProperty("signal.processing.queueCapacity", "200"))
        );
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }
}
