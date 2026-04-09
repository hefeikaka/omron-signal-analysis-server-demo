package com.omron.oss.domain.api;

import com.omron.oss.domain.common.model.NormalizedSignalMessage;
import com.omron.oss.domain.signal.collection.CollectedSignalRequest;
import com.omron.oss.domain.signal.collection.SignalCollectionService;
import com.omron.oss.domain.signal.pipeline.SignalPublishingService;

public final class SignalIngestFacade {

    private final SignalCollectionService collectionService;
    private final SignalPublishingService publishingService;

    public SignalIngestFacade(SignalCollectionService collectionService, SignalPublishingService publishingService) {
        this.collectionService = collectionService;
        this.publishingService = publishingService;
    }

    public NormalizedSignalMessage ingest(SignalIngestRequest request) {
        return publishingService.processAndPublish(
            collectionService.collect(
                new CollectedSignalRequest(request.getMachineId(), request.getSamplingFrequency(), request.getValues())
            )
        );
    }
}
