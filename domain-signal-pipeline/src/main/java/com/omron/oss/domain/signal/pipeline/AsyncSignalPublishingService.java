package com.omron.oss.domain.signal.pipeline;

import com.omron.oss.domain.common.model.NormalizedSignalMessage;
import com.omron.oss.domain.common.model.SignalSample;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletionException;

public final class AsyncSignalPublishingService {

    private final SignalPublishingService signalPublishingService;
    private final Executor executor;

    public AsyncSignalPublishingService(SignalPublishingService signalPublishingService, Executor executor) {
        this.signalPublishingService = Objects.requireNonNull(signalPublishingService, "signalPublishingService");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public CompletableFuture<NormalizedSignalMessage> processAndPublishAsync(SignalSample sample) {
        return CompletableFuture.supplyAsync(() -> signalPublishingService.processAndPublish(sample), executor);
    }

    public CompletableFuture<AsyncSignalProcessingResult> processAndPublishAsyncSafely(SignalSample sample) {
        return processAndPublishAsync(sample)
            .thenApply(AsyncSignalProcessingResult::success)
            .exceptionally(this::toFailureResult);
    }

    private AsyncSignalProcessingResult toFailureResult(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
            ? throwable.getCause()
            : throwable;
        return AsyncSignalProcessingResult.failure(cause.getMessage());
    }
}
