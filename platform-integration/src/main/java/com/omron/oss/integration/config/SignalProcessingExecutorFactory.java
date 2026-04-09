package com.omron.oss.integration.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class SignalProcessingExecutorFactory {

    public ExecutorService create(SignalProcessingProperties properties) {
        return new ThreadPoolExecutor(
            properties.getWorkerThreads(),
            properties.getWorkerThreads(),
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(properties.getQueueCapacity()),
            new SignalThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private static final class SignalThreadFactory implements ThreadFactory {
        private final AtomicInteger threadCounter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("signal-worker-" + threadCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
