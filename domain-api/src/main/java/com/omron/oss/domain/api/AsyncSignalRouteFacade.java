package com.omron.oss.domain.api;

import com.omron.oss.domain.common.model.NormalizedSignalMessage;
import com.omron.oss.domain.signal.collection.SignalCollectionService;
import com.omron.oss.domain.signal.pipeline.AsyncSignalProcessingResult;
import com.omron.oss.domain.signal.pipeline.AsyncSignalPublishingService;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class AsyncSignalRouteFacade {

    private final SignalRawPayloadParser rawPayloadParser;
    private final SignalCollectionService signalCollectionService;
    private final AsyncSignalPublishingService asyncSignalPublishingService;

    public AsyncSignalRouteFacade(
        SignalRawPayloadParser rawPayloadParser,
        SignalCollectionService signalCollectionService,
        AsyncSignalPublishingService asyncSignalPublishingService
    ) {
        this.rawPayloadParser = Objects.requireNonNull(rawPayloadParser, "rawPayloadParser");
        this.signalCollectionService = Objects.requireNonNull(signalCollectionService, "signalCollectionService");
        this.asyncSignalPublishingService = Objects.requireNonNull(asyncSignalPublishingService, "asyncSignalPublishingService");
    }

    public CompletableFuture<NormalizedSignalMessage> ingestRawPayloadAsync(String rawJson) {
        SignalIngestRequest request = rawPayloadParser.parse(rawJson);
        return asyncSignalPublishingService.processAndPublishAsync(
            signalCollectionService.collect(
                new com.omron.oss.domain.signal.collection.CollectedSignalRequest(
                    request.getMachineId(),
                    request.getSamplingFrequency(),
                    request.getValues()
                )
            )
        );
    }

    public CompletableFuture<AsyncSignalProcessingResult> ingestRawPayloadAsyncSafely(String rawJson) {
        try {
            SignalIngestRequest request = rawPayloadParser.parse(rawJson);
            return asyncSignalPublishingService.processAndPublishAsyncSafely(
                signalCollectionService.collect(
                    new com.omron.oss.domain.signal.collection.CollectedSignalRequest(
                        request.getMachineId(),
                        request.getSamplingFrequency(),
                        request.getValues()
                    )
                )
            );
        } catch (RuntimeException ex) {
            return CompletableFuture.completedFuture(AsyncSignalProcessingResult.failure(ex.getMessage()));
        }
    }
}
