package com.omron.oss.domain.api;

import com.omron.oss.domain.common.constants.TopicNames;
import com.omron.oss.domain.common.model.NormalizedSignalMessage;
import com.omron.oss.domain.signal.pipeline.AsyncSignalProcessingResult;
import com.omron.oss.domain.signal.collection.SignalCollectionService;
import com.omron.oss.domain.signal.pipeline.AsyncSignalPublishingService;
import com.omron.oss.domain.signal.pipeline.SignalMessageFormatter;
import com.omron.oss.domain.signal.pipeline.SignalPublishingService;
import com.omron.oss.integration.jms.MessagePublisher;
import com.omron.oss.integration.mongodb.InMemorySignalRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncSignalRouteFacadeTest {

    @Test
    void shouldProcessMultiplePayloadsConcurrently() throws Exception {
        CapturingMessagePublisher publisher = new CapturingMessagePublisher();
        InMemorySignalRecordRepository repository = new InMemorySignalRecordRepository();
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        try {
            AsyncSignalRouteFacade routeFacade = new AsyncSignalRouteFacade(
                new SignalRawPayloadParser(),
                new SignalCollectionService(),
                new AsyncSignalPublishingService(
                    new SignalPublishingService(new SignalMessageFormatter(), publisher, repository),
                    executorService
                )
            );

            List<CompletableFuture<NormalizedSignalMessage>> futures = new ArrayList<CompletableFuture<NormalizedSignalMessage>>();
            for (int i = 0; i < 5; i++) {
                futures.add(routeFacade.ingestRawPayloadAsync(
                    "{\"machineCode\":\"edge-" + i + "\",\"frequency\":12000,\"values\":[0.1,0.2,0.3]}"
                ));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);

            assertEquals(5, repository.getSavedSamples().size());
            assertEquals(5, publisher.topicNames.size());
            assertTrue(publisher.topicNames.stream().allMatch(TopicNames.SOURCE_SIGNAL_CURVE::equals));
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void shouldReturnFailureResultForInvalidPayloadInSafeMode() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            AsyncSignalRouteFacade routeFacade = new AsyncSignalRouteFacade(
                new SignalRawPayloadParser(),
                new SignalCollectionService(),
                new AsyncSignalPublishingService(
                    new SignalPublishingService(new SignalMessageFormatter(), new CapturingMessagePublisher(), new InMemorySignalRecordRepository()),
                    executorService
                )
            );

            AsyncSignalProcessingResult result = routeFacade
                .ingestRawPayloadAsyncSafely("{\"machineCode\":\"broken-no-values\"}")
                .get(5, TimeUnit.SECONDS);

            assertFalse(result.isSuccess());
            assertTrue(result.getFailureReason().contains("values"));
        } finally {
            executorService.shutdownNow();
        }
    }

    private static final class CapturingMessagePublisher implements MessagePublisher {
        private final List<String> topicNames = new ArrayList<String>();

        @Override
        public synchronized void publish(String topicName, String payload) {
            this.topicNames.add(topicName);
        }
    }
}
